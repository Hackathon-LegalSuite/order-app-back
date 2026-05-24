package com.restaurant.order_app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Beans de infraestructura compartidos por toda la aplicación. */
@Configuration
public class AppConfig {

    /** RestTemplate para llamadas HTTP salientes (ej: Gemini API). */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /** ObjectMapper para serialización/deserialización JSON en servicios de la aplicación. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
