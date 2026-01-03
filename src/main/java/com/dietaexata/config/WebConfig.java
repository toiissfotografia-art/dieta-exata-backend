package com.dietaexata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:5500",    // Porta comum do VS Code Live Server
                    "http://127.0.0.1:5500",
                    "http://localhost:8081",
                    "http://127.0.0.1:8081",
                    // --- ADICIONADO: DOMÍNIOS DE PRODUÇÃO PARA EVITAR ERRO DE CONEXÃO ---
                    "https://dietaexata.com.br",
                    "https://www.dietaexata.com.br",
                    "https://celebrated-peony-751b18.netlify.app",
                    "https://hilarious-heliotrope-b814dd.netlify.app"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}