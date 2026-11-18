package com.sakan.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sakan.support.tags.RegressionTest;

@RegressionTest
class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void find_withoutAuthentication_isEmpty() {
        assertThat(provider.find()).isEmpty();
    }

    @Test
    void find_anonymousToken_isEmpty() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(provider.find()).isEmpty();
    }

    @Test
    void find_authenticatedUser_readsIdAndStripsRolePrefix() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "42", null, List.of(new SimpleGrantedAuthority("ROLE_OWNER"), new SimpleGrantedAuthority("AGENT"))));

        AuthenticatedUser user = provider.find().orElseThrow();

        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.roles()).containsExactlyInAnyOrder("OWNER", "AGENT");
        assertThat(user.canManageListings()).isTrue();
    }

    @Test
    void find_nonNumericName_isEmpty() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "mohamed", null, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        assertThat(provider.find()).isEmpty();
    }

    @Test
    void require_withoutAuthentication_throws() {
        assertThatThrownBy(provider::require).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void authenticatedUser_buyerCannotManageListings() {
        AuthenticatedUser buyer = new AuthenticatedUser(1L, java.util.Set.of("BUYER_TENANT"));

        assertThat(buyer.canManageListings()).isFalse();
        assertThat(buyer.hasRole(AuthenticatedUser.ADMIN)).isFalse();
    }
}
