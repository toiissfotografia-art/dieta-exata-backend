package com.dietaexata.config;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Ativa a configuração de CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. Desativa o CSRF (Essencial para POST/DELETE sem token)
            .csrf(csrf -> csrf.disable())
            
            // 3. Define as regras de acesso
            .authorizeHttpRequests(auth -> auth
                // Libera totalmente as rotas de usuários e pagamentos
                .requestMatchers("/api/usuarios/**").permitAll()
                .requestMatchers("/api/pagamentos/**").permitAll()
                
                // Permite qualquer outra requisição para garantir fluidez
                .anyRequest().permitAll()
            )
            
            // 4. Sessão STATELESS (Melhor performance para Render/Heroku)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // CORREÇÃO: Usamos setAllowedOriginPatterns para ser mais flexível com subdomínios
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        
        // Garante que todos os verbos HTTP sejam aceitos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        
        // CORREÇÃO: Liberamos todos os headers para evitar erro de 'Preflight'
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Importante para manter a compatibilidade com cookies se necessário
        configuration.setAllowCredentials(true);
        
        // Expõe headers que podem ser úteis para o frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}