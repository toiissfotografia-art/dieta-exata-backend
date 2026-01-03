package com.dietaexata.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    // --- LISTAR TODOS (PARA O ADMIN) ---
    @GetMapping("/todos")
    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    // --- LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String senha = payload.get("senha");

        Usuario u = repository.findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email) && user.getSenha().equals(senha))
                .findFirst().orElse(null);

        if (u != null) {
            return ResponseEntity.ok(u);
        }
        return ResponseEntity.status(401).body("{\"erro\":\"Credenciais inválidas.\"}");
    }

    // --- CADASTRO ---
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody Usuario novo) {
        if (repository.findAll().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(novo.getEmail()))) {
            return ResponseEntity.badRequest().body("{\"erro\":\"Email já cadastrado.\"}");
        }
        Usuario salvo = repository.save(novo);
        return ResponseEntity.ok(salvo);
    }

    // --- PROCESSO DE UPGRADE (BOTÃO ATIVAR DO ADMIN) ---
    @PostMapping("/processar-upgrade")
    public ResponseEntity<?> processarUpgrade(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String metodo = payload.get("metodo");

            Usuario u = repository.findAll().stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst().orElse(null);

            if (u == null) {
                return ResponseEntity.status(404).body("{\"erro\":\"Usuário não encontrado.\"}");
            }

            // Ação disparada pelo botão ATIVAR do Admin
            if ("ADMIN_MANUAL".equalsIgnoreCase(metodo)) {
                u.setPlano("OURO");
                u.setDataExpiracao(LocalDateTime.now().plusDays(30));
                
                // Salva primeiro para garantir o status
                repository.save(u);

                // Tenta processar ganhos da rede
                try {
                    processarGanhosRede(u);
                } catch (Exception e) {
                    System.out.println("Aviso: Falha ao processar bônus de rede: " + e.getMessage());
                }

                return ResponseEntity.ok("{\"mensagem\":\"Usuário ativado com sucesso!\"}");
            }

            // Caso use upgrade por saldo interno
            if ("SALDO".equalsIgnoreCase(metodo)) {
                Double saldo = u.getSaldoDisponivel();
                if (saldo < 197.0) {
                    return ResponseEntity.badRequest().body("{\"erro\":\"Saldo insuficiente.\"}");
                }
                u.setSaldoDisponivel(saldo - 197.0);
                u.setPlano("OURO");
                u.setDataExpiracao(LocalDateTime.now().plusDays(30));
                repository.save(u);
                return ResponseEntity.ok("{\"mensagem\":\"Upgrade realizado via saldo!\"}");
            }

            return ResponseEntity.badRequest().body("{\"erro\":\"Método não suportado.\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"erro\":\"Erro interno: " + e.getMessage() + "\"}");
        }
    }

    // --- AJUSTAR SALDO (MODAL DO ADMIN) ---
    @PostMapping("/ajustar-saldo")
    public ResponseEntity<?> ajustarSaldo(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");
            Double valor = Double.parseDouble(payload.get("valor").toString());

            Usuario u = repository.findAll().stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst().orElse(null);

            if (u != null) {
                u.setSaldoDisponivel(valor);
                repository.save(u);
                return ResponseEntity.ok("{\"mensagem\":\"Saldo atualizado!\"}");
            }
            return ResponseEntity.status(404).body("{\"erro\":\"Usuário não encontrado.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"erro\":\"Falha ao ajustar saldo.\"}");
        }
    }

    // --- DELETAR USUÁRIO ---
    @DeleteMapping("/deletar/{email}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable String email) {
        try {
            Usuario u = repository.findAll().stream()
                    .filter(user -> user.getEmail().equalsIgnoreCase(email))
                    .findFirst().orElse(null);

            if (u != null) {
                repository.delete(u);
                return ResponseEntity.ok("{\"mensagem\":\"Usuário removido.\"}");
            }
            return ResponseEntity.status(404).body("{\"erro\":\"Usuário não encontrado.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"erro\":\"Erro ao deletar: " + e.getMessage() + "\"}");
        }
    }

    // --- LÓGICA DE GANHOS DE REDE ---
    private void processarGanhosRede(Usuario u) {
        if (u.getIndicadoPor() == null || u.getIndicadoPor().equalsIgnoreCase("direto")) return;

        // Bônus Nível 1 (Quem indicou o U)
        Usuario pai = repository.findAll().stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(u.getIndicadoPor()))
                .findFirst().orElse(null);

        if (pai != null && "OURO".equalsIgnoreCase(pai.getPlano())) {
            pai.setSaldoDisponivel(pai.getSaldoDisponivel() + 70.0);
            pai.setGanhosDiretos(pai.getGanhosDiretos() + 70.0);
            pai.setNivel1count(pai.getNivel1count() + 1);
            repository.save(pai);

            // Bônus Nível 2 (Quem indicou o pai)
            if (pai.getIndicadoPor() != null && !pai.getIndicadoPor().equalsIgnoreCase("direto")) {
                Usuario avo = repository.findAll().stream()
                        .filter(a -> a.getEmail().equalsIgnoreCase(pai.getIndicadoPor()))
                        .findFirst().orElse(null);
                
                if (avo != null && "OURO".equalsIgnoreCase(avo.getPlano())) {
                    avo.setSaldoDisponivel(avo.getSaldoDisponivel() + 15.0);
                    avo.setGanhosIndiretos(avo.getGanhosIndiretos() + 15.0);
                    avo.setNivel2count(avo.getNivel2count() + 1);
                    repository.save(avo);
                }
            }
        }
    }
}