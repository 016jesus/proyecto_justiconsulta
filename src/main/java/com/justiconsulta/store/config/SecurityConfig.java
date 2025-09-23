package com.justiconsulta.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Deshabilitar completamente la seguridad (solo para desarrollo)
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(sm -> sm.disable())
            .httpBasic(hb -> hb.disable())
            .formLogin(fl -> fl.disable())
            .logout(lo -> lo.disable())
            .oauth2Login(oauth -> oauth.disable());

        // Si tienes filtros custom como ApiKeyAuthFilter/JwtAuthFilter, evita registrarlos aquí.

        return http.build();
    }
}
