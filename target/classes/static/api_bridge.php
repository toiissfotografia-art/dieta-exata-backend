<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

// Configurações de arquivos locais
$arquivoUsuarios = 'todos.json'; 
$arquivoSaques = 'saques_gerais.json'; 
$backend_url = "https://dieta-exata-backend-1.onrender.com/"; 

$path = $_GET['path'] ?? '';

// --- LÓGICA DE INTERCEPTAÇÃO E SUBTRAÇÃO DE SALDO ---

if ($path === 'api/usuarios/admin/solicitar-saque' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $inputRaw = file_get_contents("php://input");
    $data = json_decode($inputRaw, true);

    if (!$data) {
        echo json_encode(['status' => 'error', 'message' => 'Nenhum dado recebido']);
        exit;
    }

    $emailUsuario = strtolower(trim($data['emailUsuario']));
    $valorSaque = floatval($data['valor']);
    $chavePix = $data['chavePix'];

    // 1. PROCESSO DE SUBTRAÇÃO NO BANCO LOCAL (todos.json)
    if (file_exists($arquivoUsuarios)) {
        $usuariosRaw = file_get_contents($arquivoUsuarios);
        $usuarios = json_decode($usuariosRaw, true);
        $sucessoSubtracao = false;

        // Itera para encontrar o usuário
        foreach ($usuarios as $key => $u) {
            if (isset($u['email']) && strtolower(trim($u['email'])) === $emailUsuario) {
                $saldoAtual = floatval($u['saldoDisponivel'] ?? 0);
                
                if ($saldoAtual >= $valorSaque) {
                    // Executa a subtração
                    $usuarios[$key]['saldoDisponivel'] = $saldoAtual - $valorSaque;
                    $usuarios[$key]['ultimoSaqueSolicitado'] = $valorSaque;
                    $usuarios[$key]['chavePixSaque'] = $chavePix; // Salva a chave usada no saque
                    
                    $sucessoSubtracao = true;
                    break;
                } else {
                    echo json_encode(['status' => 'error', 'message' => 'Saldo insuficiente no sistema (Saldo: '.$saldoAtual.')']);
                    exit;
                }
            }
        }

        if ($sucessoSubtracao) {
            // SALVAMENTO CRÍTICO
            file_put_contents($arquivoUsuarios, json_encode($usuarios, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Usuário não encontrado no todos.json']);
            exit;
        }
    }

    // 2. REGISTRO PARA O PAINEL ADMIN MASTER (saques_gerais.json)
    $historicoSaques = file_exists($arquivoSaques) ? json_decode(file_get_contents($arquivoSaques), true) : [];
    if (!is_array($historicoSaques)) $historicoSaques = [];

    $novoRegistro = [
        'id' => uniqid(),
        'dataSolicitacao' => date('Y-m-d H:i:s'), // Ajustado para compatibilidade com o front
        'emailUsuario' => $emailUsuario, // Ajustado nome da chave
        'nomeUsuario' => $data['nomeUsuario'] ?? 'Usuário', 
        'valor' => $valorSaque,
        'chavePix' => $chavePix,
        'status' => 'pendente'
    ];
    $historicoSaques[] = $novoRegistro;
    file_put_contents($arquivoSaques, json_encode($historicoSaques, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));

    echo json_encode(['status' => 'success', 'message' => 'Saque processado e saldo atualizado!']);
    exit;
}

// 2. Lógica para o Master ler os saques pendentes
if ($path === 'api/usuarios/admin/saques-pendentes') {
    if (file_exists($arquivoSaques)) {
        echo file_get_contents($arquivoSaques);
    } else {
        echo json_encode([]);
    }
    exit;
}

// 3. Admin finalizando (Zera o valor solicitado e remove da fila)
if ($path === 'api/usuarios/admin/finalizar-saque' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents("php://input"), true);
    // Captura o email vindo do front-end
    $emailParaLimpar = isset($input['emailUsuario']) ? strtolower(trim($input['emailUsuario'])) : '';

    if (empty($emailParaLimpar)) {
        echo json_encode(["status" => "error", "message" => "E-mail não fornecido"]);
        exit;
    }

    // A. Remove do saques_gerais.json (Limpa a lista de pendentes)
    if (file_exists($arquivoSaques)) {
        $saques = json_decode(file_get_contents($arquivoSaques), true) ?: [];
        $novos_saques = array_filter($saques, function($s) use ($emailParaLimpar) {
            // Mantém apenas saques que NÃO pertencem ao e-mail pago
            $emailNoSaque = $s['emailUsuario'] ?? ($s['email'] ?? '');
            return strtolower(trim($emailNoSaque)) !== $emailParaLimpar;
        });
        file_put_contents($arquivoSaques, json_encode(array_values($novos_saques), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
    }

    // B. Limpa o valor em "ultimoSaqueSolicitado" no todos.json para o front atualizar
    if (file_exists($arquivoUsuarios)) {
        $usuarios = json_decode(file_get_contents($arquivoUsuarios), true);
        $alterado = false;
        foreach ($usuarios as $key => $u) {
            if (isset($u['email']) && strtolower(trim($u['email'])) === $emailParaLimpar) {
                $usuarios[$key]['ultimoSaqueSolicitado'] = 0;
                $alterado = true;
                break;
            }
        }
        if ($alterado) {
            file_put_contents($arquivoUsuarios, json_encode($usuarios, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
        }
    }

    echo json_encode(["status" => "success", "message" => "Pagamento baixado com sucesso"]);
    exit;
}

// --- FLUXO PADRÃO PARA OUTRAS ROTAS (CURL) ---
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
echo $response;
curl_close($ch);
?>