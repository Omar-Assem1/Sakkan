package com.sakan.auth.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sakan.common.config.JwtProperties;
import com.sakan.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtEncoder encoder;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        byte[] key = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (key.length < MIN_SECRET_BYTES) {
            // never include the secret in the message
            throw new IllegalArgumentException("JWT secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
        this.expiration = properties.expiration();
    }

    public String generateToken(User user) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        List<String> roles = user.getRoles().stream().map(r -> r.getName().name()).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))   // CurrentUserProvider parses sub as the user id
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}