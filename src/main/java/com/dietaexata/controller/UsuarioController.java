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
                return ResponseEntity.status(403).body("Sua conta aguarda ativação. Realize o PIX!");
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

            if (usuario.getIndicadoPor() != null && !usuario.getIndicadoPor().isEmpty()) {
                Usuario indicador = repository.findByEmail(usuario.getIndicadoPor().toLowerCase().trim());
                if (indicador != null) {
                    usuario.setIndicadoPor(indicador.getEmail());
                }
            }

            Usuario salvo = repository.save(usuario);
            return ResponseEntity.ok(salvo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    // --- MÉTODOS SOLICITADOS PELO PAYMENTCONTROLLER (CORREÇÃO DE ERROS) ---

    public void renovarPlanoAdmin(String email) {
        Usuario u = repository.findByEmail(email);
        if (u != null) {
            // Se o usuário pagou e está como Bronze, sobe para Ouro automaticamente
            if (u.getPlano() == null || u.getPlano().equalsIgnoreCase("BRONZE")) {
                u.setPlano("OURO");
            }
            u.setDataExpiracao(LocalDateTime.now().plusDays(30));
            repository.saveAndFlush(u);
            System.out.println("Plano renovado via Admin/Pagamento para: " + email);
        }
    }

    public void processarBonusPercentualPeloEmail(String email, double valorPago) {
        Usuario u = repository.findByEmail(email);
        if (u != null) {
            // MANTENDO LÓGICA MMN: Aciona a rede de ganhos
            processarGanhosRede(u);
        }
    }

    public void estornar(Map<String, Object> payload) {
        String email = (String) payload.get("external_reference");
        Usuario u = repository.findByEmail(email);
        if (u != null) {
            u.setDataExpiracao(LocalDateTime.now().minusDays(1)); // Inativa a conta
            repository.saveAndFlush(u);
            System.out.println("Pagamento estornado/cancelado para: " + email);
        }
    }

    // ---------------------------------------------------------------------

    @PostMapping("/pagamentos/webhook")
    public ResponseEntity<?> webhookPagamento(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("external_reference"); 
            if (email != null) {
                Usuario u = repository.findByEmail(email);
                if (u != null) {
                    // Garante a subida de plano para evitar o erro do "Bronze"
                    if (u.getPlano() == null || u.getPlano().equalsIgnoreCase("BRONZE")) {
                        u.setPlano("OURO"); 
                    }
                    u.setDataExpiracao(LocalDateTime.now().plusDays(30));
                    repository.saveAndFlush(u);
                    processarGanhosRede(u); 
                    return ResponseEntity.ok("{\"mensagem\":\"Sucesso!\"}");
                }
            }
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/processar-upgrade")
    public ResponseEntity<?> processarUpgrade(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String metodo = payload.get("metodo");
            
            Usuario u = repository.findByEmail(email);
            if (u == null) return ResponseEntity.status(404).body("{\"erro\":\"Usuário não encontrado.\"}");

            if ("ADMIN_MANUAL".equalsIgnoreCase(metodo)) {
                if(u.getPlano() == null || u.getPlano().equalsIgnoreCase("BRONZE")) {
                    u.setPlano("OURO"); 
                }
                u.setDataExpiracao(LocalDateTime.now().plusDays(30));
                repository.saveAndFlush(u);
                processarGanhosRede(u); 
                return ResponseEntity.ok(Map.of("mensagem", "Ativado com sucesso!"));
            }

            if ("SALDO".equalsIgnoreCase(metodo)) {
                String novoPlano = payload.get("plano") != null ? payload.get("plano").toUpperCase() : "OURO";
                double custo = novoPlano.equals("OURO") ? 79.99 : (novoPlano.equals("PRATA") ? 49.99 : 19.99);

                double saldo = Optional.ofNullable(u.getSaldoDisponivel()).orElse(0.0);
                if (saldo < custo) return ResponseEntity.badRequest().body("{\"erro\":\"Saldo insuficiente.\"}");
                
                u.setSaldoDisponivel(saldo - custo);
                u.setPlano(novoPlano);
                u.setDataExpiracao(LocalDateTime.now().plusDays(30));
                repository.saveAndFlush(u);
                processarGanhosRede(u);
                return ResponseEntity.ok("{\"mensagem\":\"Upgrade realizado!\"}");
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
    }

    // LÓGICA DE MMN (Multi-Level Marketing)
    private void processarGanhosRede(Usuario novo) {
        if (novo.getIndicadoPor() == null || novo.getIndicadoPor().isEmpty()) return;

        double valorBaseMMN = 0.0;
        String planoVendido = (novo.getPlano() != null) ? novo.getPlano().toUpperCase() : "BRONZE";
        
        if (planoVendido.equals("OURO")) {
            valorBaseMMN = 79.99;
        } else if (planoVendido.equals("PRATA")) {
            valorBaseMMN = 49.99;
        } else {
            valorBaseMMN = 19.99;
        }

        Usuario pai = repository.findByEmail(novo.getIndicadoPor().toLowerCase().trim());
        if (pai != null) {
            String planoPai = (pai.getPlano() != null) ? pai.getPlano().toUpperCase() : "BRONZE";
            
            double bonusN1 = switch (planoPai) {
                case "OURO"   -> valorBaseMMN * 0.50; 
                case "PRATA"  -> valorBaseMMN * 0.25; 
                default       -> valorBaseMMN * 0.10; 
            };
            
            creditarValor(pai, bonusN1, true);
            pai.setNivel1count(Optional.ofNullable(pai.getNivel1count()).orElse(0) + 1);
            repository.save(pai);

            if (pai.getIndicadoPor() != null && !pai.getIndicadoPor().isEmpty()) {
                Usuario avo = repository.findByEmail(pai.getIndicadoPor().toLowerCase().trim());
                if (avo != null) {
                    String planoAvo = (avo.getPlano() != null) ? avo.getPlano().toUpperCase() : "BRONZE";
                    
                    double bonusN2 = switch (planoAvo) {
                        case "OURO"  -> 5.00;
                        case "PRATA" -> 2.50;
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
        Usuario u = repository.findByEmail(email);
        if (u != null) { repository.delete(u); return ResponseEntity.ok("Removido"); }
        return ResponseEntity.status(404).build();
    }
}