package com.dietaexata.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dietaexata.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Já existente: usado no Login e no Pagamento para achar o pagador
    Usuario findByEmail(String email);

    // Novo: Útil para o sistema de rede/indicados que você tem na Model
    List<Usuario> findByIndicadoPor(String emailIndicador);
}