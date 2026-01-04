package com.dietaexata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dietaexata.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Esta conexão é vital para o MMN localizar o Pai e o Avô na rede
    Usuario findByEmail(String email);
    
    // Caso precise verificar se o e-mail existe antes de cadastrar
    boolean existsByEmail(String email);
}