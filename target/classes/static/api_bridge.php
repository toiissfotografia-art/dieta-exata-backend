<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json");

// URL DO SEU BACKEND NO RENDER
$backend_url = "https://dieta-exata-backend-1.onrender.com/"; 

// Captura o path base
$path = $_GET['path'] ?? '';

// --- INCLUSÃO: MANTER PARÂMETROS DE MMN (Email, Ref, etc) ---
// Esta parte garante que se a URL for api_bridge.php?path=api/usuarios&email=teste@teste.com
// o email não seja perdido no caminho para o Render.
$queryParams = $_GET;
unset($queryParams['path']); // Removemos o 'path' para não duplicar
$queryString = http_build_query($queryParams);

$full_url = $backend_url . $path;
if (!empty($queryString)) {
    $full_url .= (strpos($full_url, '?') === false ? '?' : '&') . $queryString;
}
// -----------------------------------------------------------

$method = $_SERVER['REQUEST_METHOD'];
$input = file_get_contents("php://input");

$ch = curl_init($full_url);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);

// Garante que o corpo do POST/PUT seja enviado corretamente para o Render
if (!empty($input)) {
    curl_setopt($ch, CURLOPT_POSTFIELDS, $input);
}

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
            "error" => "Rota não encontrada no Java (Render)",
            "path_tentado" => $path,
            "full_url_final" => $full_url,
            "status" => 404
        ]);
    } else {
        // Retorna a resposta real do backend (onde deve vir o plano atualizado)
        echo $response;
    }
}
curl_close($ch);
?>