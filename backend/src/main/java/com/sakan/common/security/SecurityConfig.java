package com.sakan.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
// TODO(SKN-18): added by M3 for SCRUM-94 so @PreAuthorize on ViewingSlotController is enforced.
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // TODO(SKN-15): added by M3 for SCRUM-94; without it anonymous requests get 403 instead of 401.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // TODO(SKN-18): added by M3 for SCRUM-94. Checked before body validation, so a BUYER_TENANT gets 403, not 400.
                .requestMatchers(HttpMethod.POST, "/api/properties/*/slots").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/properties/*/slots/*").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.GET, "/api/properties/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
