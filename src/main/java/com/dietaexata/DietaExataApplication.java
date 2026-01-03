package com.dietaexata;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DietaExataApplication {

    public static void main(String[] args) {
        SpringApplication.run(DietaExataApplication.class, args);
    }

    @Bean
    public CommandLineRunner toiiss() {
        return args -> {
            System.out.println("\n\n" + "#".repeat(40));
            System.out.println("   TOIISS. SITE ON! 🚀");
            System.out.println("   PLANOS: BRONZE, PRATA E OURO ATIVOS");
            System.out.println("#".repeat(40) + "\n");
        };
    }
}