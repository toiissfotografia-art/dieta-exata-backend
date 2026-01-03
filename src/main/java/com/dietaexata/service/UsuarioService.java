package com.dietaexata.service;

import com.dietaexata.model.Usuario;
import com.dietaexata.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Busca o usuário no seu banco pelo e-mail
        Usuario usuario = repository.findByEmail(email);
        
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email);
        }

        // Retorna o usuário para o Spring Security validar a senha
        return new User(usuario.getEmail(), usuario.getSenha(), new ArrayList<>());
    }
}