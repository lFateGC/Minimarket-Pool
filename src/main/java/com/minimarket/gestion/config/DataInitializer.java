package com.minimarket.gestion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("Sistema iniciado correctamente.");
    }
}