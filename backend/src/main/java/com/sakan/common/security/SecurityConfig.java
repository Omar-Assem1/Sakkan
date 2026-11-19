package com.sakan.common.security;
import com.sakan.common.config.AdminSeedProperties;
import com.sakan.common.config.JwtProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, AdminSeedProperties.class})
// TODO(SKN-18): added by M3 for SCRUM-94 so @PreAuthorize on ViewingSlotController is enforced.
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/admin/login").permitAll()
                // Checked before body validation, so disallowed roles get 403, not 400.
                .requestMatchers(HttpMethod.POST, "/api/properties/*/slots").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/properties/*/slots/*").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.GET, "/api/viewings/my").hasAnyRole("BUYER_TENANT", "OWNER", "AGENT")
                .requestMatchers(HttpMethod.POST, "/api/properties/*/images").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.PUT, "/api/properties/*/images/order").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/properties/*/images/*").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.GET, "/api/property-images/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/viewings").hasAnyRole("BUYER_TENANT", "OWNER", "AGENT")
                .requestMatchers(HttpMethod.PATCH, "/api/viewings/*/confirm", "/api/viewings/*/complete").hasAnyRole("OWNER", "AGENT")
                .requestMatchers(HttpMethod.PATCH, "/api/viewings/*/cancel").hasAnyRole("BUYER_TENANT", "OWNER", "AGENT")
                .requestMatchers(HttpMethod.GET, "/api/properties/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** HS256 only: tokens signed with any other algorithm or key are rejected. */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        byte[] key = properties.secret().getBytes(StandardCharsets.UTF_8);
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** roles claim becomes ROLE_<name>, which SecurityContextCurrentUserProvider strips again. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
