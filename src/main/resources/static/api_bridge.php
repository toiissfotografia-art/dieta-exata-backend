<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS, DELETE, PUT");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') exit(0);

// ENDEREÇO DO RENDER (Certifique-se que termina com /)
$backend_url = "https://dieta-exata-backend-1.onrender.com/"; 

// Pega o caminho e reconstrói a query string (importante para deletar por email na URL)
$path = isset($_GET['path']) ? $_GET['path'] : '';
unset($_GET['path']); // Remove o path para não duplicar na reconstrução
$query_string = http_build_query($_GET);

$url = $backend_url . $path . ($query_string ? '?' . $query_string : '');

// Captura o JSON enviado pelo Frontend
$input = file_get_contents("php://input");
$method = $_SERVER['REQUEST_METHOD'];

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 60); // Aumentado para 60s (Render Free demora a subir)
curl_setopt($ch, CURLOPT_TIMEOUT, 60);

// Cabeçalhos para o Java entender a requisição
$headers = array(
    'Content-Type: application/json',
    'Accept: application/json'
);

if (!empty($input)) {
    curl_setopt($ch, CURLOPT_POSTFIELDS, $input);
    $headers[] = 'Content-Length: ' . strlen($input);
}

curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    $error_msg = curl_error($ch);
    echo json_encode([
        "erro" => "Conexão com Servidor Render Falhou",
        "detalhe" => $error_msg,
        "url_tentada" => $url
    ]);
    http_response_code(502);
} else {
    // Repassa exatamente o que o Java respondeu
    http_response_code($http_code);
    echo $response;
}
curl_close($ch);