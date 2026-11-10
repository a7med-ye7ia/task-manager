package com.taskmanagesys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${app.base-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String additionalOrigins;

    private List<String> allowedOrigins;

    @PostConstruct
    void initialize() {
        allowedOrigins = configuredOrigins();
        log.info("CORS allowed origins: {}", allowedOrigins);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins != null ? allowedOrigins : configuredOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> configuredOrigins() {
        List<String> origins = new ArrayList<>();
        Stream.concat(Stream.of(frontendUrl), Stream.ofNullable(additionalOrigins)
                        .flatMap(value -> Stream.of(value.split(","))))
                .map(this::normalizeOrigin)
                .filter(origin -> !origin.isBlank() && !"*".equals(origin))
                .distinct()
                .forEach(origins::add);
        return origins;
    }

    private String normalizeOrigin(String origin) {
        String normalized = origin == null ? "" : origin.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
