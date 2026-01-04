package com.dietaexata.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;

@RestController
@RequestMapping("/api/pagamentos")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.GET, RequestMethod.OPTIONS})
public class PaymentController {

    @Autowired
    private UsuarioController usuarioController; 

    static {
        // Seu Access Token do Mercado Pago
        MercadoPagoConfig.setAccessToken("APP_USR-1937482038911257-122401-7f038287c775e1a3842dca7eaa72520a-1312092772");
    }

    @PostMapping("/pix")
    public ResponseEntity<?> gerarPix(@RequestBody Map<String, String> dados) {
        String emailUsuario = dados.get("email");
        if (emailUsuario == null || emailUsuario.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "O e-mail do pagador é obrigatório."));
        }

        String plano = Optional.ofNullable(dados.get("plano")).orElse("BRONZE").toUpperCase();
        
        BigDecimal valor;
        try {
            if (dados.containsKey("valor")) {
                valor = new BigDecimal(dados.get("valor"));
            } else {
                valor = switch (plano) {
                    case "PRATA" -> new BigDecimal("49.99");
                    case "OURO"  -> new BigDecimal("79.99");
                    default      -> new BigDecimal("19.99");
                };
            }
        } catch (Exception e) {
            valor = new BigDecimal("19.99"); 
        }

        try {
            PaymentClient client = new PaymentClient();

            PaymentCreateRequest paymentCreateRequest =
                PaymentCreateRequest.builder()
                    .transactionAmount(valor)
                    .description("Plano " + plano + " - " + emailUsuario)
                    .installments(1)
                    .paymentMethodId("pix")
                    // CONFIGURAÇÃO 1: Esta URL deve ser o endereço da sua api_bridge.php apontando para o webhook
                    .notificationUrl("https://dietaexata.com.br/api_bridge.php?path=api/pagamentos/webhook") 
                    .payer(PaymentPayerRequest.builder()
                        .email(emailUsuario)
                        .build())
                    .build();

            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("X-Idempotency-Key", UUID.randomUUID().toString());

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .customHeaders(customHeaders)
                    .build();

            Payment payment = client.create(paymentCreateRequest, requestOptions);

            Map<String, Object> resposta = new HashMap<>();
            resposta.put("id", payment.getId());
            resposta.put("status", payment.getStatus());
            resposta.put("valor", valor.doubleValue());
            
            if (payment.getPointOfInteraction() != null && 
                payment.getPointOfInteraction().getTransactionData() != null) {
                // Aqui garantimos que o nome do campo seja o mesmo que o JS procura
                resposta.put("pixCopiaEcola", payment.getPointOfInteraction().getTransactionData().getQrCode());
                resposta.put("qrCodeBase64", payment.getPointOfInteraction().getTransactionData().getQrCodeBase64());
            }
            
            return ResponseEntity.ok(resposta);

        } catch (MPApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                 .body(Map.of("erro", "Erro na API do Mercado Pago: " + e.getApiResponse().getContent()));
        } catch (MPException | RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("erro", "Erro ao gerar PIX: " + e.getMessage()));
        }
    }

    // CONFIGURAÇÃO 2: O MÉTODO WEBHOOK
    @PostMapping("/webhook")
    public ResponseEntity<?> receberNotificacao(@RequestBody Map<String, Object> payload) {
        try {
            // Log para você depurar no Render se o Mercado Pago está chamando
            System.out.println("Webhook recebido: " + payload);

            String action = (String) payload.get("action");
            // Se for apenas teste de criação, respondemos OK
            if (action != null && action.equals("payment.created")) return ResponseEntity.ok().build(); 

            Object dataObj = payload.get("data");
            if (!(dataObj instanceof Map)) return ResponseEntity.ok().build();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            String paymentIdStr = String.valueOf(data.get("id"));
            
            if (paymentIdStr == null || paymentIdStr.equals("null")) return ResponseEntity.ok().build();

            long paymentId = Long.parseLong(paymentIdStr);

            // Buscamos o status real do pagamento no Mercado Pago
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(paymentId);

            // CONFIGURAÇÃO 3: A LIBERAÇÃO AUTOMÁTICA E MMN
            if ("approved".equals(payment.getStatus())) {
                String descricao = payment.getDescription(); 
                if (descricao != null && descricao.contains(" - ")) {
                    String email = descricao.split(" - ")[1]; 
                    double valorPago = payment.getTransactionAmount().doubleValue();

                    // Ativa o plano do usuário
                    usuarioController.renovarPlanoAdmin(email);
                    // Processa os bônus da rede (MMN)
                    usuarioController.processarBonusPercentualPeloEmail(email, valorPago);
                    
                    System.out.println("Pagamento APROVADO e MMN Processado para: " + email);
                }
            }
            // Importante: Sempre retornar 200 OK para o Mercado Pago não tentar enviar de novo
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok().build(); 
        }
    }

    @PostMapping("/estornar-mercado-pago")
    public ResponseEntity<?> estornarPagamento(@RequestBody Map<String, Object> payload) {
        try {
            String paymentIdStr = String.valueOf(payload.get("paymentId"));
            if (paymentIdStr == null || paymentIdStr.equals("null")) {
                return ResponseEntity.badRequest().body(Map.of("erro", "ID do pagamento é necessário."));
            }

            long paymentId = Long.parseLong(paymentIdStr);
            PaymentRefundClient refundClient = new PaymentRefundClient();

            PaymentRefund refund = refundClient.refund(paymentId);

            usuarioController.estornar(payload);

            return ResponseEntity.ok(Map.of(
                "status", "refunded",
                "id_estorno", refund.getId()
            ));

        } catch (MPApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("erro", "Erro MP: " + e.getApiResponse().getContent()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", "Falha no estorno: " + e.getMessage()));
        }
    }
}