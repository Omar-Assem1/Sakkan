package com.sakan.auth.security;

import com.sakan.auth.service.GoogleOAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private final GoogleOAuthService googleOAuthService;

    public GoogleOAuthSuccessHandler(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            response.sendRedirect("/auth/sign-in?googleError=1");
            return;
        }

        Map<String, Object> claims = oidcUser.getClaims();
        String subject = oidcUser.getSubject();
        String email = claims.get("email") instanceof String value ? value : null;
        boolean verified = Boolean.TRUE.equals(claims.get("email_verified"));
        String hostedDomain = claims.get("hd") instanceof String value ? value : null;
        if (subject == null || email == null || email.isBlank()) {
            response.sendRedirect("/auth/sign-in?googleError=1");
            return;
        }

        String code = googleOAuthService.createHandoff(subject, email, verified, hostedDomain);
        String callback = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/google/callback")
                .queryParam("code", code)
                .build().encode().toUriString();
        response.sendRedirect(callback);
    }
}
