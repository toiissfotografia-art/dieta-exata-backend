<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

// URL DO SEU BACKEND NO RENDER
$backend_url = "https://dieta-exata-backend-1.onrender.com/"; 
$path = $_GET['path'] ?? '';
$full_url = $backend_url . $path;

$method = $_SERVER['REQUEST_METHOD'];
$input = file_get_contents("php://input");

$ch = curl_init($full_url);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
curl_setopt($ch, CURLOPT_POSTFIELDS, $input);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Content-Length: ' . strlen($input)
]);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    echo json_encode(["error" => "Erro na ponte: " . curl_error($ch)]);
} else {
    if ($http_code == 404) {
        // SE A ROTA NÃO EXISTIR NO JAVA, RETORNA UM ERRO CLARO
        echo json_encode([
            "error" => "Rota não encontrada no Java",
            "path_tentado" => $path,
            "status" => 404
        ]);
    } else {
        echo $response;
    }
}
curl_close($ch);
?>