package com.dietaexata.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(length = 20)
    private String plano = "BRONZE"; // Valor padrão inicial

    @Column(name = "indicado_por")
    private String indicadoPor = "direto";

    @Column(name = "chave_pix")
    private String chavePix;

    // Ajustado para permitir null no mapeamento, mas garantindo 0.0 no objeto
    @Column(nullable = false)
    private Double saldoDisponivel = 0.0;

    @Column(nullable = false)
    private Double saldoSolicitado = 0.0;

    @Column(nullable = false)
    private Double ganhosDiretos = 0.0;

    @Column(nullable = false)
    private Double ganhosIndiretos = 0.0;

    @Column(nullable = false)
    private Integer nivel1count = 0;

    @Column(nullable = false)
    private Integer nivel2count = 0;

    @Column(nullable = false)
    private Integer nivel3count = 0;

    @Column(name = "data_expiracao")
    private LocalDateTime dataExpiracao;

    @Column(name = "data_ultima_dieta")
    private LocalDate dataUltimaDieta;
    
    @Column(name = "dieta_atual", columnDefinition = "TEXT")
    private String dietaAtual;

    @Column(name = "alerta_mensagem", columnDefinition = "TEXT")
    private String alertaMensagem;

    // --- NOVO: MÉTODO DE PROTEÇÃO PRÉ-SALVAMENTO ---
    // Isso garante que antes de enviar para a Locaweb, nada vá nulo
    @PrePersist
    protected void onCreate() {
        if (this.saldoDisponivel == null) this.saldoDisponivel = 0.0;
        if (this.saldoSolicitado == null) this.saldoSolicitado = 0.0;
        if (this.ganhosDiretos == null) this.ganhosDiretos = 0.0;
        if (this.ganhosIndiretos == null) this.ganhosIndiretos = 0.0;
        if (this.nivel1count == null) this.nivel1count = 0;
        if (this.nivel2count == null) this.nivel2count = 0;
        if (this.nivel3count == null) this.nivel3count = 0;
        if (this.plano == null) this.plano = "BRONZE";
        if (this.indicadoPor == null) this.indicadoPor = "direto";
        if (this.dataUltimaDieta == null) this.dataUltimaDieta = LocalDate.now();
        if (this.dataExpiracao == null) this.dataExpiracao = LocalDateTime.now().minusDays(1);
    }

    // --- CONSTRUTORES ---
    public Usuario() {
    }

    // --- GETTERS E SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }

    public String getIndicadoPor() { return indicadoPor; }
    public void setIndicadoPor(String indicadoPor) { this.indicadoPor = indicadoPor; }

    public String getChavePix() { return chavePix; }
    public void setChavePix(String chavePix) { this.chavePix = chavePix; }

    public Double getSaldoDisponivel() { return saldoDisponivel; }
    public void setSaldoDisponivel(Double saldoDisponivel) { this.saldoDisponivel = (saldoDisponivel != null) ? saldoDisponivel : 0.0; }

    public Double getSaldoSolicitado() { return saldoSolicitado; }
    public void setSaldoSolicitado(Double saldoSolicitado) { this.saldoSolicitado = (saldoSolicitado != null) ? saldoSolicitado : 0.0; }

    public Double getGanhosDiretos() { return ganhosDiretos; }
    public void setGanhosDiretos(Double ganhosDiretos) { this.ganhosDiretos = (ganhosDiretos != null) ? ganhosDiretos : 0.0; }

    public Double getGanhosIndiretos() { return ganhosIndiretos; }
    public void setGanhosIndiretos(Double ganhosIndiretos) { this.ganhosIndiretos = (ganhosIndiretos != null) ? ganhosIndiretos : 0.0; }

    public Integer getNivel1count() { return nivel1count; }
    public void setNivel1count(Integer nivel1count) { this.nivel1count = (nivel1count != null) ? nivel1count : 0; }

    public Integer getNivel2count() { return nivel2count; }
    public void setNivel2count(Integer nivel2count) { this.nivel2count = (nivel2count != null) ? nivel2count : 0; }

    public Integer getNivel3count() { return nivel3count; }
    public void setNivel3count(Integer nivel3count) { this.nivel3count = (nivel3count != null) ? nivel3count : 0; }

    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDateTime dataExpiracao) { this.dataExpiracao = dataExpiracao; }

    public LocalDate getDataUltimaDieta() { return dataUltimaDieta; }
    public void setDataUltimaDieta(LocalDate dataUltimaDieta) { this.dataUltimaDieta = dataUltimaDieta; }

    public String getDietaAtual() { return dietaAtual; }
    public void setDietaAtual(String dietaAtual) { this.dietaAtual = dietaAtual; }

    public String getAlertaMensagem() { return alertaMensagem; }
    public void setAlertaMensagem(String alertaMensagem) { this.alertaMensagem = alertaMensagem; }
}