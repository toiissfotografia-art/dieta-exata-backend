package com.dietaexata.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dietaexata.model.Refeicao;

@Repository
public interface RefeicaoRepository extends JpaRepository<Refeicao, Long> {
    
    // Este método permite buscar as refeições de um usuário diretamente pelo banco de dados
    // O Spring Boot criará a consulta SQL automaticamente para você.
    List<Refeicao> findByUsuarioEmail(String usuarioEmail);
}