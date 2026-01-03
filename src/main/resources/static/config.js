const CONFIG = {
    // A BASE_URL vazia é correta para quem usa api_bridge.php no mesmo servidor do HTML
    BASE_URL: '', 
    
    // Rotas de Autenticação e Usuário
    API_LOGIN: 'api_bridge.php?path=api/usuarios/login',
    API_USUARIOS: 'api_bridge.php?path=api/usuarios',
    API_CADASTRO: 'api_bridge.php?path=api/usuarios/cadastrar',
    
    // --- NOVAS ROTAS ADICIONADAS PARA O ADMIN NÃO TRAVAR ---
    API_UPGRADE: 'api_bridge.php?path=api/usuarios/processar-upgrade',
    API_DELETAR: 'api_bridge.php?path=api/usuarios/deletar', 
    API_AJUSTAR_SALDO: 'api_bridge.php?path=api/usuarios/ajustar-saldo',
    
    // Rotas de Pagamento e Planos
    API_PIX: 'api_bridge.php?path=api/pagamentos/pix',
    API_PLANOS: 'api_bridge.php?path=api/usuarios/todos',

    NOME_SISTEMA: 'DIETA EXATA - OFICIAL'
};