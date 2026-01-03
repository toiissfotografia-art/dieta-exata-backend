const CONFIG = {
    // A BASE_URL deve ser vazia para usar o caminho relativo ao domínio onde o site está hospedado
    BASE_URL: '', 
    
    // Simplificamos os caminhos. O HTML chamará api_bridge.php passando apenas o endpoint final.
    API_LOGIN: 'api_bridge.php?path=api/usuarios/login',
    API_USUARIOS: 'api_bridge.php?path=api/usuarios',
    API_CADASTRO: 'api_bridge.php?path=api/usuarios/cadastrar',
    
    API_PIX: 'api_bridge.php?path=api/pagamentos/pix',
    API_PLANOS: 'api_bridge.php?path=api/usuarios/todos',

    NOME_SISTEMA: 'DIETA EXATA - OFICIAL'
};