package com.sakan.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.sakan.common.config.JwtProperties;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    private JwtService jwtService;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(24)));
        decoder = decoderFor(SECRET);
    }

    private static NimbusJwtDecoder decoderFor(String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static User user() {
        return User.builder()
                .id(42L)
                .username("omar")
                .email("omar@sakan.com")
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .roles(Set.of(Role.builder().id(2L).name(RoleName.OWNER).build()))
                .build();
    }

    // T13
    @Test
    void token_isSignedWithHs256() {
        Jwt jwt = decoder.decode(jwtService.generateToken(user()));

        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
    }

    // T13
    @Test
    void token_expiresAfter24Hours() {
        Jwt jwt = decoder.decode(jwtService.generateToken(user()));

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
                .isEqualTo(Duration.ofHours(24));
    }

    // T16
    @Test
    void token_containsUserIdUsernameAndRoles() {
        Jwt jwt = decoder.decode(jwtService.generateToken(user()));

        assertThat(jwt.getSubject()).isEqualTo("42");                  // user id, read by CurrentUserProvider
        assertThat(jwt.getClaimAsString("username")).isEqualTo("omar");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("OWNER");
    }

    // T16 (security): the token must never carry the password hash
    @Test
    void token_neverContainsPasswordData() {
        User u = user();
        u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");

        Jwt jwt = decoder.decode(jwtService.generateToken(u));

        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash", "password_hash");
    }

    // T14 (unit level)
    // T14 (unit level): an expired token cannot be made through JwtService, so build one by hand
    @Test
    void expiredToken_isRejectedByDecoder() {
        Instant now = Instant.now();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("42")
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .build();
        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    // T15 (unit level)
    @Test
    void tamperedSignature_isRejectedByDecoder() {
        String token = jwtService.generateToken(user());
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = String.join(".", parts);

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
    }

    // T15 (unit level)
    @Test
    void tokenSignedWithOtherSecret_isRejectedByDecoder() {
        JwtService other = new JwtService(
                new JwtProperties("another-secret-another-secret-0123456789", Duration.ofHours(24)));

        assertThatThrownBy(() -> decoder.decode(other.generateToken(user())))
                .isInstanceOf(JwtException.class);
    }

    // E01: a weak secret must stop the application, not weaken tokens silently
    @Test
    void secretShorterThan32Bytes_isRejected() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("short", Duration.ofHours(24))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}