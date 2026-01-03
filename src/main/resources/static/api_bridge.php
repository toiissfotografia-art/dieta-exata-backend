<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS, DELETE, PUT");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') exit(0);

// ENDEREÇO DO RENDER
$backend_url = "https://dieta-exata-backend-1.onrender.com/"; 

$path = isset($_GET['path']) ? $_GET['path'] : '';
$url = $backend_url . $path;

// Captura o JSON enviado pelo Admin
$input = file_get_contents("php://input");
$method = $_SERVER['REQUEST_METHOD'];

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30); // Aumentado para o Render acordar

// Montagem dos cabeçalhos corrigida
$headers = array('Content-Type: application/json');
if (!empty($input)) {
    $headers[] = 'Content-Length: ' . strlen($input);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $input);
}

curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    $error_msg = curl_error($ch);
    echo json_encode([
        "erro" => "Terminal Central (Render) Inacessível",
        "detalhe" => $error_msg
    ]);
    http_response_code(502);
} else {
    // Se o Java retornar erro, o PHP repassa o código e a mensagem
    http_response_code($http_code);
    echo $response;
}
curl_close($ch);