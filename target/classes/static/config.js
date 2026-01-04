const CONFIG = {
    // A BASE_URL vazia é correta para quem usa api_bridge.php no mesmo servidor do HTML
    BASE_URL: '', 
    
    // Rotas de Autenticação e Usuário
    API_LOGIN: 'api_bridge.php?path=api/usuarios/login',
    API_USUARIOS: 'api_bridge.php?path=api/usuarios',
    // Corrigido para /registrar que é o padrão comum em Java Spring
    API_CADASTRO: 'api_bridge.php?path=api/usuarios/registrar', 
    
    // --- ROTAS DE ADMIN ---
    API_UPGRADE: 'api_bridge.php?path=api/usuarios/processar-upgrade',
    API_DELETAR: 'api_bridge.php?path=api/usuarios/deletar', 
    API_AJUSTAR_SALDO: 'api_bridge.php?path=api/usuarios/ajustar-saldo',
    
    // --- ROTAS DE SAQUE E FINANCEIRO (INCLUÍDAS PARA CONEXÃO MMN) ---
    // Esta rota dispara a subtração no todos.json e gravação no saques_gerais.json
    API_SOLICITAR_SAQUE: 'api_bridge.php?path=api/usuarios/admin/solicitar-saque',
    
    // Rotas para o Painel do Admin Master gerir os pagamentos
    API_LISTAR_SAQUES: 'api_bridge.php?path=api/usuarios/admin/saques-pendentes',
    API_FINALIZAR_SAQUE: 'api_bridge.php?path=api/usuarios/admin/finalizar-saque',

    // Rotas de Pagamento e Planos
    API_PIX: 'api_bridge.php?path=api/pagamentos/pix',
    API_PLANOS: 'api_bridge.php?path=api/usuarios/todos',
    API_URL: "https://seusite.com.br/api_bridge.php?path=",

    NOME_SISTEMA: 'DIETA EXATA - OFICIAL'
};