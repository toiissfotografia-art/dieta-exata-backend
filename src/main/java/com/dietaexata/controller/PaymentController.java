package com.dietaexata.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagamentos")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PaymentController {

    @Autowired
    private UsuarioController usuarioController;

    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarPagamento(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");
            Object valorObj = payload.get("valor");
            double valor = (valorObj != null) ? Double.parseDouble(valorObj.toString()) : 0.0;

            if (email != null) {
                // Chama os métodos do UsuarioController para processar a ativação e o MMN
                usuarioController.renovarPlanoAdmin(email);
                usuarioController.processarBonusPercentualPeloEmail(email, valor);
                
                return ResponseEntity.ok(Map.of("mensagem", "Pagamento e bônus processados!"));
            }
            return ResponseEntity.badRequest().body("Email não informado.");
            
        } catch (NumberFormatException e) {
            return ResponseEntity.status(400).body("Erro no formato do valor do pagamento.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro interno ao confirmar pagamento.");
        }
    }

    @PostMapping("/estornar-pagamento")
    public ResponseEntity<?> solicitarEstorno(@RequestBody Map<String, Object> payload) {
        try {
            // Repassa a requisição de estorno para a lógica central no UsuarioController
            return usuarioController.estornar(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Dados de estorno inválidos.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao processar estorno.");
        }
    }
}