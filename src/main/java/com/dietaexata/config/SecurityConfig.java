package com.dietaexata.config;

import java.util.Arrays;

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
            // 1. Ativa a configuração de CORS definida abaixo
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. Desativa o CSRF (Crucial para o POST do PIX e Webhook funcionarem)
            .csrf(csrf -> csrf.disable())
            
            // 3. Define as regras de acesso
            .authorizeHttpRequests(auth -> auth
                // Libera totalmente as rotas de usuários
                .requestMatchers("/api/usuarios/**").permitAll()
                
                // Libera totalmente as rotas de pagamento (Onde estava o erro de conexão)
                .requestMatchers("/api/pagamentos/**").permitAll()
                .requestMatchers("/api/pagamentos/pix").permitAll()
                .requestMatchers("/api/pagamentos/webhook").permitAll()
                
                // Permite qualquer outra requisição para não travar o sistema em produção
                .anyRequest().permitAll()
            )
            
            // 4. CORREÇÃO: Alterado de .disable() para STATELESS 
            // Isso permite que a API receba múltiplas chamadas sem criar estado, mas sem recusar a conexão.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Lista completa de origens (Netlify + Domínios Locaweb)
        configuration.setAllowedOrigins(Arrays.asList(
            "https://celebrated-peony-751b18.netlify.app", 
            "https://dietaexata.com.br", 
            "https://www.dietaexata.com.br",
            "https://hilarious-heliotrope-b814dd.netlify.app",
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "http://localhost:8081",
            "http://127.0.0.1:8081"
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Importante: Permitir todos os headers para que o 'Content-Type: application/json' passe
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}