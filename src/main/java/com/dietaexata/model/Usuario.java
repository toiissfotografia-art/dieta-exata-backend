package com.dietaexata.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    private String senha;
    private String plano = "BRONZE";
    private LocalDateTime dataExpiracao;
    private String indicadoPor;

    // Campos de Saldo e Financeiro (Conforme usado no Controller)
    private Double saldoDisponivel = 0.0;
    private Double saldoSolicitado = 0.0;
    private Double ganhosDiretos = 0.0;
    private Double ganhosIndiretos = 0.0;
    private Double saldo = 0.0; // Campo de apoio

    // Contadores de Rede MMN
    private Integer nivel1count = 0;
    private Integer nivel2count = 0;
    private Integer nivel3count = 0;

    // Campos de Dieta e Mensagens
    private String dietaAtual;
    private LocalDate dataUltimaDieta;
    private String alertaMensagem;
    private String chavePix;

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

    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDateTime dataExpiracao) { this.dataExpiracao = dataExpiracao; }

    public String getIndicadoPor() { return indicadoPor; }
    public void setIndicadoPor(String indicadoPor) { this.indicadoPor = indicadoPor; }

    public Double getSaldoDisponivel() { return saldoDisponivel; }
    public void setSaldoDisponivel(Double saldoDisponivel) { this.saldoDisponivel = saldoDisponivel; }

    public Double getSaldoSolicitado() { return saldoSolicitado; }
    public void setSaldoSolicitado(Double saldoSolicitado) { this.saldoSolicitado = saldoSolicitado; }

    public Double getGanhosDiretos() { return ganhosDiretos; }
    public void setGanhosDiretos(Double ganhosDiretos) { this.ganhosDiretos = ganhosDiretos; }

    public Double getGanhosIndiretos() { return ganhosIndiretos; }
    public void setGanhosIndiretos(Double ganhosIndiretos) { this.ganhosIndiretos = ganhosIndiretos; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    public Integer getNivel1count() { return nivel1count; }
    public void setNivel1count(Integer nivel1count) { this.nivel1count = nivel1count; }

    public Integer getNivel2count() { return nivel2count; }
    public void setNivel2count(Integer nivel2count) { this.nivel2count = nivel2count; }

    public Integer getNivel3count() { return nivel3count; }
    public void setNivel3count(Integer nivel3count) { this.nivel3count = nivel3count; }

    public String getDietaAtual() { return dietaAtual; }
    public void setDietaAtual(String dietaAtual) { this.dietaAtual = dietaAtual; }

    public LocalDate getDataUltimaDieta() { return dataUltimaDieta; }
    public void setDataUltimaDieta(LocalDate dataUltimaDieta) { this.dataUltimaDieta = dataUltimaDieta; }

    public String getAlertaMensagem() { return alertaMensagem; }
    public void setAlertaMensagem(String alertaMensagem) { this.alertaMensagem = alertaMensagem; }

    public String getChavePix() { return chavePix; }
    public void setChavePix(String chavePix) { this.chavePix = chavePix; }
}