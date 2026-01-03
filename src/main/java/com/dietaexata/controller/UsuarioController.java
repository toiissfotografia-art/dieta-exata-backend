package com.dietaexata.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dietaexata.model.Usuario;
import com.dietaexata.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario user) {
        if ("toiiss@dietaexata.com.br".equals(user.getEmail()) && "Acesso@123dieta".equals(user.getSenha())) {
            Usuario admin = repository.findAll().stream()
                .filter(u -> u.getEmail().equals(user.getEmail())).findFirst().orElse(null);
            if (admin == null) {
                admin = new Usuario();
                admin.setNome("ADMINISTRADOR MESTRE");
                admin.setEmail("toiiss@dietaexata.com.br");
                admin.setSenha("Acesso@123dieta");
                admin.setPlano("ADMIN");
                admin.setDataExpiracao(LocalDateTime.now().plusYears(99));
                repository.save(admin);
            }
            return ResponseEntity.ok(admin);
        }

        Optional<Usuario> found = repository.findAll().stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(user.getEmail()) && u.getSenha().equals(user.getSenha()))
            .findFirst();

        if (found.isPresent()) {
            Usuario u = found.get();
            if (u.getDataExpiracao() == null || u.getDataExpiracao().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(403).body("Sua conta aguarda ativação. Realize o PIX e envie o comprovante no grupo!");
            }
            return ResponseEntity.ok(u);
        }
        return ResponseEntity.status(401).body("Invalido");
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody Usuario usuario) {
        try {
            boolean emailExiste = repository.findAll().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(usuario.getEmail()));

            if (emailExiste) {
                return ResponseEntity.status(400).body("Este e-mail já está cadastrado!");
            }

            usuario.setDataUltimaDieta(LocalDate.now());
            usuario.setSaldoDisponivel(0.0);
            usuario.setSaldoSolicitado(0.0);
            usuario.setGanhosDiretos(0.0);
            usuario.setGanhosIndiretos(0.0);
            usuario.setNivel1count(0);
            usuario.setNivel2count(0);
            usuario.setNivel3count(0);
            
            if (usuario.getPlano() == null) usuario.setPlano("BRONZE");
            usuario.setDataExpiracao(LocalDateTime.now().minusDays(1));

            Usuario salvo = repository.save(usuario);
            return ResponseEntity.ok(salvo);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Erro ao salvar no banco: " + e.getMessage());
        }
    }

    @PostMapping("/processar-upgrade")
    public ResponseEntity<?> processarUpgrade(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String metodo = payload.get("metodo");

            Usuario u = repository.findAll().stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst().orElse(null);

            if (u == null) return ResponseEntity.status(404).body("{\"erro\":\"Usuário não encontrado.\"}");

            if ("ADMIN_MANUAL".equalsIgnoreCase(metodo)) {
                u.setPlano("OURO");
                u.setDataExpiracao(LocalDateTime.now().plusDays(30));
                repository.save(u);
                processarGanhosRede(u); // Dispara a lógica de bônus do seu código
                return ResponseEntity.ok(Map.of("mensagem", "Usuário ativado via Admin!"));
            }

            if ("SALDO".equalsIgnoreCase(metodo)) {
                double saldo = Optional.ofNullable(u.getSaldoDisponivel()).orElse(0.0);
                if (saldo < 197.0) return ResponseEntity.badRequest().body("{\"erro\":\"Saldo insuficiente.\"}");
                
                u.setSaldoDisponivel(saldo - 197.0);
                u.setPlano("OURO");
                u.setDataExpiracao(LocalDateTime.now().plusDays(30));
                repository.save(u);
                processarGanhosRede(u); 
                return ResponseEntity.ok("{\"mensagem\":\"Upgrade realizado!\"}");
            }
            
            return ResponseEntity.badRequest().body("{\"erro\":\"Método não suportado.\"}");
        } catch (NullPointerException | IllegalArgumentException e) {
            return ResponseEntity.status(400).body("{\"erro\":\"Dados inválidos.\"}");
        }
    }

    @PostMapping("/ajustar-saldo")
    public ResponseEntity<?> ajustarSaldo(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");
            double valor = Double.parseDouble(payload.get("valor").toString());
            
            Usuario u = repository.findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);

            if (u != null) {
                u.setSaldoDisponivel(valor);
                repository.save(u);
                return ResponseEntity.ok("{\"mensagem\":\"Saldo Ajustado!\"}");
            }
            return ResponseEntity.status(404).build();
        } catch (NumberFormatException | NullPointerException e) {
            return ResponseEntity.status(400).body("{\"erro\":\"Formato de valor inválido.\"}");
        }
    }

    public void processarBonusPercentualPeloEmail(String email, double valorPago) {
        renovarPlanoAdmin(email);
    }

    public void renovarPlanoAdmin(String email) {
        Usuario u = repository.findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
        if (u != null) {
            u.setDataExpiracao(LocalDateTime.now().plusDays(30));
            if (u.getPlano() == null || u.getPlano().isEmpty()) u.setPlano("BRONZE");
            repository.save(u);
            processarGanhosRede(u);
        }
    }

    @PostMapping("/estornar")
    public ResponseEntity<?> estornar(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");
            Object valorObj = payload.get("valor");
            double valorEstorno = (valorObj != null) ? Double.parseDouble(valorObj.toString()) : 0.0;

            Usuario u = repository.findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);

            if (u == null) return ResponseEntity.status(404).body("Usuário não encontrado.");

            double saldoAtual = Optional.ofNullable(u.getSaldoDisponivel()).orElse(0.0);
            u.setSaldoDisponivel(saldoAtual - valorEstorno);
            
            repository.save(u);
            return ResponseEntity.ok("Saldo atualizado após estorno.");
        } catch (NumberFormatException | NullPointerException e) {
            return ResponseEntity.status(400).body("Erro no valor de estorno.");
        }
    }

    @PostMapping("/transferir")
    public ResponseEntity<?> transferirSaldo(@RequestBody Map<String, Object> payload) {
        try {
            String emailOrigem = (String) payload.get("remetenteEmail");
            String emailDestino = (String) payload.get("destinatarioEmail");
            
            if (payload.get("valor") == null) return ResponseEntity.badRequest().body("Valor não informado.");
            double valor = Double.parseDouble(payload.get("valor").toString());

            if (emailOrigem.equalsIgnoreCase(emailDestino)) {
                return ResponseEntity.badRequest().body("Você não pode transferir para si mesmo.");
            }

            Usuario origem = repository.findByEmail(emailOrigem);
            Usuario destino = repository.findByEmail(emailDestino);

            if (origem == null) return ResponseEntity.status(404).body("Sua conta não foi encontrada.");
            if (destino == null) return ResponseEntity.status(404).body("Destinatário não encontrado no sistema.");

            double saldoAtual = Optional.ofNullable(origem.getSaldoDisponivel()).orElse(0.0);
            if (saldoAtual < valor) return ResponseEntity.badRequest().body("Saldo insuficiente.");

            origem.setSaldoDisponivel(saldoAtual - valor);
            destino.setSaldoDisponivel(Optional.ofNullable(destino.getSaldoDisponivel()).orElse(0.0) + valor);

            repository.save(origem);
            repository.save(destino);

            return ResponseEntity.ok("Transferência concluída!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro no processamento: " + e.getMessage());
        }
    }

    @PostMapping("/solicitar-saque/{email}")
    public ResponseEntity<?> solicitarSaque(@PathVariable String email, @RequestBody Map<String, String> payload) {
        Usuario u = repository.findAll().stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        if (u != null) {
            String pix = payload.get("chavePix");
            if (pix == null || pix.isEmpty()) return ResponseEntity.status(400).body("PIX obrigatório.");

            double saldoDisp = Optional.ofNullable(u.getSaldoDisponivel()).orElse(0.0);
            if (saldoDisp > 0) {
                u.setChavePix(pix);
                u.setSaldoSolicitado(Optional.ofNullable(u.getSaldoSolicitado()).orElse(0.0) + saldoDisp);
                u.setSaldoDisponivel(0.0);
                repository.save(u);
                return ResponseEntity.ok("Sucesso!");
            }
        }
        return ResponseEntity.status(404).build();
    }

    // --- RESTAURAÇÃO DA SUA LÓGICA DE MMN ORIGINAL (COM SEUS VALORES) ---
    private void processarGanhosRede(Usuario novo) {
        if (novo.getIndicadoPor() == null || novo.getIndicadoPor().isEmpty()) return;

        double valorPago;
        String planoStr = (novo.getPlano() != null) ? novo.getPlano().toUpperCase() : "BRONZE";
        
        // Mantendo os valores de referência do seu switch original
        switch (planoStr) {
            case "BRONZE" -> valorPago = 19.99;
            case "PRATA"  -> valorPago = 49.99;
            case "OURO"   -> valorPago = 79.99;
            default       -> valorPago = 0.0;
        }
        
        if (valorPago == 0) return;

        Usuario pai = repository.findAll().stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(novo.getIndicadoPor()))
            .findFirst().orElse(null);

        if (pai != null) {
            String planoPai = (pai.getPlano() != null) ? pai.getPlano().toUpperCase() : "BRONZE";
            
            // Lógica de bônus percentual que estava no seu código original
            double bonusN1 = switch (planoPai) {
                case "OURO"   -> valorPago * 0.50;
                case "PRATA"  -> valorPago * 0.25;
                default       -> valorPago * 0.10;
            };
            
            creditarValor(pai, bonusN1, true);
            pai.setNivel1count(Optional.ofNullable(pai.getNivel1count()).orElse(0) + 1);
            repository.save(pai);

            if (pai.getIndicadoPor() != null && !pai.getIndicadoPor().isEmpty()) {
                Usuario avo = repository.findAll().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(pai.getIndicadoPor()))
                    .findFirst().orElse(null);
                
                if (avo != null) {
                    String planoAvo = (avo.getPlano() != null) ? avo.getPlano().toUpperCase() : "BRONZE";
                    
                    // Lógica de bônus fixo Nível 2 que estava no seu código original
                    double bonusN2 = switch (planoAvo) {
                        case "OURO"  -> 2.50;
                        case "PRATA" -> 1.50;
                        default      -> 1.00;
                    };
                    
                    creditarValor(avo, bonusN2, false);
                    avo.setNivel2count(Optional.ofNullable(avo.getNivel2count()).orElse(0) + 1);
                    repository.save(avo);
                }
            }
        }
    }

    private void creditarValor(Usuario u, double valor, boolean isDireto) {
        if (u == null) return;
        u.setSaldoDisponivel(Optional.ofNullable(u.getSaldoDisponivel()).orElse(0.0) + valor);
        if (isDireto) u.setGanhosDiretos(Optional.ofNullable(u.getGanhosDiretos()).orElse(0.0) + valor);
        else u.setGanhosIndiretos(Optional.ofNullable(u.getGanhosIndiretos()).orElse(0.0) + valor);
    }

    @GetMapping("/todos")
    public List<Usuario> listarTodos() { return repository.findAll(); }

    @DeleteMapping("/deletar/{email}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable String email) {
        if ("toiiss@dietaexata.com.br".equals(email)) return ResponseEntity.status(403).build();
        Usuario u = repository.findAll().stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        if (u != null) {
            repository.delete(u);
            return ResponseEntity.ok("Removido");
        }
        return ResponseEntity.status(404).build();
    }

    @PostMapping("/zerar-saldo/{email}")
    public ResponseEntity<?> zerarSaldoSolicitado(@PathVariable String email) {
        Usuario u = repository.findAll().stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        if (u != null) {
            u.setSaldoSolicitado(0.0);
            repository.save(u);
            return ResponseEntity.ok("Sucesso");
        }
        return ResponseEntity.status(404).build();
    }

    @PostMapping("/estornar-pix/{email}")
    public ResponseEntity<?> estornarPix(@PathVariable String email) {
        Usuario u = repository.findAll().stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        if (u != null) {
            double valorRetorno = Optional.ofNullable(u.getSaldoSolicitado()).orElse(0.0);
            u.setSaldoDisponivel(Optional.ofNullable(u.getSaldoDisponivel()).orElse(0.0) + valorRetorno);
            u.setSaldoSolicitado(0.0);
            u.setAlertaMensagem("⚠️ ERRO NO PIX: Verifique sua chave. Valor estornado.");
            repository.save(u);
            return ResponseEntity.ok("Estornado");
        }
        return ResponseEntity.status(404).build();
    }

    @PostMapping("/atualizar-dieta")
    public ResponseEntity<?> atualizarDieta(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String dieta = payload.get("dietaAtual");

        Usuario u = repository.findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);

        if (u != null) {
            u.setDietaAtual(dieta);
            u.setDataUltimaDieta(LocalDate.now());
            repository.save(u);
            return ResponseEntity.ok("Dieta salva com sucesso!");
        }
        return ResponseEntity.status(404).body("Usuário não encontrado.");
    }
}