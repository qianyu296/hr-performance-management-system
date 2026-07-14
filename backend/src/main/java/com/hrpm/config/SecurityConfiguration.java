package com.hrpm.config;


import com.hrpm.security.PermissionResolver;
import com.hrpm.security.SessionValidator;
import com.hrpm.security.TokenAuthenticationFilter;
import com.hrpm.service.TokenService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    WebSecurityCustomizer openApiWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(request -> {
            String uri = request.getRequestURI();
            return uri.contains("/v3/api-docs") || uri.contains("/swagger-ui");
        });
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/actuator/health", "/auth/login", "/auth/refresh",
                                "/v3/api-docs/**", "/api/v1/v3/api-docs/**",
                                "/swagger-ui/**", "/api/v1/swagger-ui/**", "/swagger-ui.html", "/api/v1/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    TokenService tokenService(
            @Value("${app.security.jwt-signing-key}") String signingKey,
            @Value("${app.security.access-token-ttl:PT15M}") Duration accessTokenTtl,
            @Value("${app.security.refresh-token-ttl:P14D}") Duration refreshTokenTtl) {
        return new TokenService(signingKey, accessTokenTtl, refreshTokenTtl, Clock.systemUTC());
    }

    @Bean
    TokenAuthenticationFilter tokenAuthenticationFilter(
            TokenService tokenService,
            SessionValidator sessionValidator,
            PermissionResolver permissionResolver,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            ObjectProvider<com.hrpm.mapper.UserAccountMapper> userAccountMapperProvider) {
        return new TokenAuthenticationFilter(tokenService, sessionValidator, permissionResolver, objectMapper, userAccountMapperProvider.getIfAvailable());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:${app.cors.allowed-origin:http://localhost:5173}}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
