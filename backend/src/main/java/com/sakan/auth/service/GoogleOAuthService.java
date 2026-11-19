package com.sakan.auth.service;

import com.sakan.auth.dto.GoogleExchangeResponse;
import com.sakan.auth.dto.GoogleRegistrationRequest;
import com.sakan.auth.dto.LoginResponse;
import com.sakan.common.exception.DuplicateUsernameException;
import com.sakan.common.exception.InvalidCredentialsException;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sakan.common.exception.DuplicateEmailException;

/** Resolves Google identities and exchanges short-lived opaque codes for Sakan JWTs. */
@Service
public class GoogleOAuthService {

    private static final long HANDOFF_LIFETIME_SECONDS = 300;
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final RoleRepository roles;
    private final JwtService jwtService;

    public GoogleOAuthService(JdbcTemplate jdbc, UserRepository users, RoleRepository roles, JwtService jwtService) {
        this.jdbc = jdbc;
        this.users = users;
        this.roles = roles;
        this.jwtService = jwtService;
    }

    @Transactional
    public String createHandoff(String subject, String email, boolean emailVerified, String hostedDomain) {
        jdbc.update("delete from google_login_handoffs where expires_at <= ?", Timestamp.from(Instant.now()));
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("insert into google_login_handoffs "
                        + "(code_hash, google_sub, email, email_verified, hosted_domain, expires_at) "
                        + "values (?, ?, ?, ?, ?, ?)",
                hash(code), subject, normalizeEmail(email), emailVerified, hostedDomain,
                Timestamp.from(Instant.now().plusSeconds(HANDOFF_LIFETIME_SECONDS)));
        return code;
    }

    @Transactional
    public GoogleExchangeResponse exchange(String code) {
        Handoff handoff = lockHandoff(code);
        requireVerifiedEmail(handoff);

        User existing = users.findByGoogleSub(handoff.googleSub()).orElse(null);
        if (existing != null) {
            if (!existing.isEnabled()) {
                throw new InvalidCredentialsException();
            }
            deleteHandoff(code);
            return GoogleExchangeResponse.authenticated(jwtService.generateToken(existing));
        }

        User byEmail = users.findByEmailIgnoreCase(handoff.email()).orElse(null);
        if (byEmail != null) {
            if (!byEmail.isEnabled() || byEmail.getAuthProvider() != AuthProvider.LOCAL
                    || !isGoogleAuthoritative(handoff)) {
                throw new DuplicateEmailException();
            }
            byEmail.setGoogleSub(handoff.googleSub());
            User linked = users.saveAndFlush(byEmail);
            deleteHandoff(code);
            return GoogleExchangeResponse.authenticated(jwtService.generateToken(linked));
        }

        return GoogleExchangeResponse.registration(handoff.email());
    }

    @Transactional
    public LoginResponse complete(GoogleRegistrationRequest request) {
        if (!USERNAME.matcher(request.username()).matches()) {
            throw new IllegalArgumentException("Username must be 3-50 characters using letters, digits, dot, dash or underscore");
        }
        if (users.existsByUsername(request.username())) {
            throw new DuplicateUsernameException();
        }

        Handoff handoff = lockHandoff(request.code());
        requireVerifiedEmail(handoff);
        if (users.findByGoogleSub(handoff.googleSub()).isPresent()
                || users.findByEmailIgnoreCase(handoff.email()).isPresent()) {
            throw new DuplicateEmailException();
        }

        Role buyer = roles.findByName(RoleName.BUYER_TENANT)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: BUYER_TENANT"));
        User created = User.builder()
                .username(request.username())
                .email(handoff.email())
                .passwordHash(null)
                .phone(null)
                .authProvider(AuthProvider.GOOGLE)
                .googleSub(handoff.googleSub())
                .enabled(true)
                .roles(new HashSet<>(Set.of(buyer)))
                .build();
        try {
            users.saveAndFlush(created);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUsernameException();
        }
        deleteHandoff(request.code());
        return LoginResponse.bearer(jwtService.generateToken(created));
    }

    private Handoff lockHandoff(String code) {
        if (code == null || code.isBlank() || code.length() > 128) {
            throw new InvalidCredentialsException();
        }
        return jdbc.query("select google_sub, email, email_verified, hosted_domain, expires_at "
                        + "from google_login_handoffs where code_hash = ? for update",
                (rs, row) -> map(rs), hash(code)).stream().findFirst()
                .filter(handoff -> handoff.expiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    private static Handoff map(ResultSet rs) throws SQLException {
        return new Handoff(rs.getString("google_sub"), rs.getString("email"), rs.getBoolean("email_verified"),
                rs.getString("hosted_domain"), rs.getTimestamp("expires_at").toInstant());
    }

    private void requireVerifiedEmail(Handoff handoff) {
        if (!handoff.emailVerified()) {
            throw new InvalidCredentialsException();
        }
    }

    private boolean isGoogleAuthoritative(Handoff handoff) {
        String domain = emailDomain(handoff.email());
        if ("gmail.com".equals(domain)) {
            return true;
        }
        return handoff.hostedDomain() != null
                && handoff.hostedDomain().equalsIgnoreCase(domain);
    }

    private void deleteHandoff(String code) {
        jdbc.update("delete from google_login_handoffs where code_hash = ?", hash(code));
    }

    private static String emailDomain(String email) {
        int at = email.lastIndexOf('@');
        return at < 0 ? "" : email.substring(at + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record Handoff(String googleSub, String email, boolean emailVerified, String hostedDomain,
            Instant expiresAt) {
    }
}
