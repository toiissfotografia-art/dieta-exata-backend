package com.dietaexata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Incluindo todas as variações do seu domínio próprio
                .allowedOrigins(
                    "https://dietaexata.com.br",
                    "https://www.dietaexata.com.br",
                    "http://dietaexata.com.br",
                    "http://www.dietaexata.com.br",
                    "http://localhost:5500",
                    "http://127.0.0.1:5500"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}