<?php
/**
 * DIETA EXATA - API BRIDGE PRO
 * Finalidade: Ponte de comunicação segura entre Frontend e Backend Railway.
 * Mantém a integridade da rede MMN e conexões de saldo.
 */

// Configurações de Cabeçalho para evitar erros de CORS no Navegador
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json; charset=UTF-8");

// Trata requisições preflight do navegador
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// URL do seu Backend no Railway (Ajuste se o host mudar)
$backend_url = "https://dieta-exata-production.up.railway.app/";

// Captura o caminho enviado pelo HTML (ex: api/usuarios/processar-upgrade)
$path = isset($_GET['path']) ? $_GET['path'] : '';

if (empty($path)) {
    echo json_encode(["error" => "Caminho da API não especificado."]);
    exit;
}

$full_url = $backend_url . $path;

// Inicializa o CURL para fazer a ponte
$ch = curl_init($full_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false); // Garante conexão com HTTPS do Railway

// Se for uma requisição POST (como o Upgrade), repassa o corpo da mensagem
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json_data = file_get_contents('php://input');
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $json_data);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
}

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    http_response_code(500);
    echo json_encode(["error" => "Falha na ponte: " . curl_error($ch)]);
} else {
    http_response_code($http_code);
    echo $response;
}

curl_close($ch);
?>