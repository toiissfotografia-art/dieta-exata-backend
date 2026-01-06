<?php
// Remove qualquer saída acidental antes do JSON
ob_start();

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

// Configurações de arquivos locais - Mantendo consistência com seu sistema
$arquivoUsuarios = 'todos.json'; 
$arquivoSaques = 'saques_gerais.json'; 
$backend_url = "https://dieta-exata-backend-1.onrender.com/"; 

$path = $_GET['path'] ?? '';

// --- 1. LÓGICA DE WEBHOOK (MERCADO PAGO) ---
if ($path === 'api/pagamentos/webhook' || $path === 'webhook-pix') {
    $inputRaw = file_get_contents("php://input");
    $payload = json_decode($inputRaw, true);
    
    $chRepasse = curl_init($backend_url . "api/pagamentos/webhook");
    curl_setopt($chRepasse, CURLOPT_CUSTOMREQUEST, "POST");
    curl_setopt($chRepasse, CURLOPT_POSTFIELDS, $inputRaw);
    curl_setopt($chRepasse, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($chRepasse, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    $responseRender = curl_exec($chRepasse);
    curl_close($chRepasse);

    $payment_id = $_GET['id'] ?? ($payload['data']['id'] ?? null);

    if ($payment_id) {
        $access_token = "APP_USR-1937482038911257-122401-7f038287c775e1a3842dca7eaa72520a-1312092772";
        $chMp = curl_init("https://api.mercadopago.com/v1/payments/$payment_id");
        curl_setopt($chMp, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($chMp, CURLOPT_HTTPHEADER, ["Authorization: Bearer $access_token"]);
        $payment_info = json_decode(curl_exec($chMp), true);
        curl_close($chMp);

        if (isset($payment_info['status']) && $payment_info['status'] === 'approved') {
            $descricao = $payment_info['description'] ?? '';
            $emailPagador = "";
            $planoAtivado = "BRONZE";

            if (strpos($descricao, " - ") !== false) {
                $partes = explode(" - ", $descricao);
                $emailPagador = strtolower(trim($partes[1]));
                $planoAtivado = str_replace("Plano ", "", strtoupper($partes[0]));
            } else {
                $emailPagador = strtolower(trim($payment_info['external_reference'] ?? ''));
                $valorPago = floatval($payment_info['transaction_amount'] ?? 0);
                if ($valorPago > 70) $planoAtivado = 'OURO';
                else if ($valorPago > 40) $planoAtivado = 'PRATA';
            }

            if (!empty($emailPagador) && file_exists($arquivoUsuarios)) {
                $usuarios = json_decode(file_get_contents($arquivoUsuarios), true);
                $alterado = false;
                foreach ($usuarios as $key => $u) {
                    if (isset($u['email']) && strtolower(trim($u['email'])) === $emailPagador) {
                        $usuarios[$key]['plano'] = $planoAtivado;
                        $usuarios[$key]['status_conta'] = 'ATIVO';
                        $usuarios[$key]['dataAtivacao'] = date('Y-m-d H:i:s');
                        $alterado = true;
                        break;
                    }
                }
                if ($alterado) {
                    file_put_contents($arquivoUsuarios, json_encode($usuarios, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
                }
            }
        }
    }
    ob_end_clean(); // Limpa lixo de saída
    echo json_encode(['status' => 'webhook_processed']);
    exit;
}

// --- 2. LÓGICA DE CONSULTA DE STATUS ---
if (strpos($path, 'api/usuarios/status-pagamento/') !== false) {
    $emailBusca = strtolower(trim(str_replace('api/usuarios/status-pagamento/', '', $path)));
    if (file_exists($arquivoUsuarios)) {
        $usuarios = json_decode(file_get_contents($arquivoUsuarios), true);
        foreach ($usuarios as $u) {
            if (isset($u['email']) && strtolower(trim($u['email'])) === $emailBusca) {
                $ativo = (isset($u['status_conta']) && ($u['status_conta'] === 'ATIVO' || $u['status_conta'] === 'active'));
                ob_end_clean();
                echo json_encode(['planoAtivo' => $ativo, 'plano' => $u['plano'] ?? 'NENHUM']);
                exit;
            }
        }
    }
    ob_end_clean();
    echo json_encode(['planoAtivo' => false, 'plano' => 'NENHUM']);
    exit;
}

// --- 3. LÓGICA DE SAQUE (ZERAMENTO DE SALDO) ---
if ($path === 'api/usuarios/admin/solicitar-saque' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $inputRaw = file_get_contents("php://input");
    $data = json_decode($inputRaw, true);

    if (!$data || !isset($data['emailUsuario'])) {
        ob_end_clean();
        echo json_encode(['status' => 'error', 'message' => 'Nenhum dado recebido']);
        exit;
    }

    $emailUsuario = strtolower(trim($data['emailUsuario']));
    $chavePix = $data['chavePix'] ?? 'Não informada';
    $valorFinalSacado = 0;

    if (file_exists($arquivoUsuarios)) {
        $usuarios = json_decode(file_get_contents($arquivoUsuarios), true);
        $sucessoSubtracao = false;

        foreach ($usuarios as $key => $u) {
            if (isset($u['email']) && strtolower(trim($u['email'])) === $emailUsuario) {
                $saldoAtual = floatval($u['saldoDisponivel'] ?? 0);
                
                if ($saldoAtual >= 50) { // Validação de segurança mínima
                    $valorFinalSacado = $saldoAtual;
                    $usuarios[$key]['saldoDisponivel'] = 0; 
                    $usuarios[$key]['ultimoSaqueSolicitado'] = $valorFinalSacado;
                    $usuarios[$key]['chavePixSaque'] = $chavePix;
                    $sucessoSubtracao = true;
                    break;
                } else {
                    ob_end_clean();
                    echo json_encode(['status' => 'error', 'message' => 'Saldo insuficiente ou abaixo do mínimo (R$ 50)']);
                    exit;
                }
            }
        }

        if ($sucessoSubtracao) {
            file_put_contents($arquivoUsuarios, json_encode($usuarios, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
            
            $historicoSaques = file_exists($arquivoSaques) ? json_decode(file_get_contents($arquivoSaques), true) : [];
            if (!is_array($historicoSaques)) $historicoSaques = [];

            $historicoSaques[] = [
                'id' => uniqid(),
                'dataSolicitacao' => date('Y-m-d H:i:s'),
                'emailUsuario' => $emailUsuario,
                'nomeUsuario' => $data['nomeUsuario'] ?? 'Usuário', 
                'valor' => $valorFinalSacado,
                'chavePix' => $chavePix,
                'status' => 'pendente'
            ];
            file_put_contents($arquivoSaques, json_encode($historicoSaques, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));

            ob_end_clean();
            echo json_encode(['status' => 'success', 'message' => 'Saque processado com sucesso!']);
        } else {
            ob_end_clean();
            echo json_encode(['status' => 'error', 'message' => 'Usuário não encontrado']);
        }
    }
    exit;
}

// --- 4. LER SAQUES PENDENTES ---
if ($path === 'api/usuarios/admin/saques-pendentes') {
    ob_end_clean();
    if (file_exists($arquivoSaques)) {
        echo file_get_contents($arquivoSaques);
    } else {
        echo json_encode([]);
    }
    exit;
}

// --- 5. FINALIZAR SAQUE (BAIXA) ---
if ($path === 'api/usuarios/admin/finalizar-saque' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents("php://input"), true);
    $emailParaLimpar = isset($input['emailUsuario']) ? strtolower(trim($input['emailUsuario'])) : '';

    if (!empty($emailParaLimpar)) {
        if (file_exists($arquivoSaques)) {
            $saques = json_decode(file_get_contents($arquivoSaques), true) ?: [];
            $novos_saques = array_filter($saques, function($s) use ($emailParaLimpar) {
                $emailNoSaque = $s['emailUsuario'] ?? ($s['email'] ?? '');
                return strtolower(trim($emailNoSaque)) !== $emailParaLimpar;
            });
            file_put_contents($arquivoSaques, json_encode(array_values($novos_saques), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        }

        if (file_exists($arquivoUsuarios)) {
            $usuarios = json_decode(file_get_contents($arquivoUsuarios), true);
            foreach ($usuarios as $key => $u) {
                if (isset($u['email']) && strtolower(trim($u['email'])) === $emailParaLimpar) {
                    $usuarios[$key]['ultimoSaqueSolicitado'] = 0;
                    break;
                }
            }
            file_put_contents($arquivoUsuarios, json_encode($usuarios, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        }
    }
    ob_end_clean();
    echo json_encode(["status" => "success"]);
    exit;
}

// --- FLUXO PADRÃO (CURL PARA O BACKEND JAVA) ---
$queryParams = $_GET;
unset($queryParams['path']); 
$queryString = http_build_query($queryParams);
$full_url = $backend_url . $path . (!empty($queryString) ? '?' . $queryString : '');

$method = $_SERVER['REQUEST_METHOD'];
$input = file_get_contents("php://input");
$ch = curl_init($full_url);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
if (!empty($input)) { curl_setopt($ch, CURLOPT_POSTFIELDS, $input); }
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);

$response = curl_exec($ch);
curl_close($ch);

ob_end_clean(); // Garante que nenhum erro PHP sujou a saída
echo $response;
exit;