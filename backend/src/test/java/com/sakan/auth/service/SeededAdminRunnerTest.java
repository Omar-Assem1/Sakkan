package com.sakan.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sakan.common.config.AdminSeedProperties;
import com.sakan.support.tags.RegressionTest;
import com.sakan.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RegressionTest
@ExtendWith(MockitoExtension.class)
class SeededAdminRunnerTest {

    private static final String USERNAME = "seed-admin-user";
    private static final String PASSWORD = "Sup3rSecretSeedPass";
    private static final String EMAIL = "admin@sakan.test";

    @Mock private UserService userService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachLogAppender() {
        logger = (Logger) LoggerFactory.getLogger(SeededAdminRunner.class);
        logger.setLevel(Level.TRACE);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachLogAppender() {
        logger.detachAppender(appender);
    }

    private SeededAdminRunner runner(String username, String password, String email) {
        return new SeededAdminRunner(userService, encoder, new AdminSeedProperties(username, password, email));
    }

    private void run(SeededAdminRunner runner) throws Exception {
        runner.run(new DefaultApplicationArguments());
    }

    private void assertNoSecretsLogged() {
        List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).isNotEmpty();
        assertThat(messages).noneMatch(m -> m.contains(USERNAME) || m.contains(PASSWORD) || m.contains("$2"));
    }

    @Test
    @DisplayName("T01 creates the admin with a BCrypt hash when none exists")
    void createsAdminWhenNoneExists() throws Exception {
        when(userService.existsSeededAdmin()).thenReturn(false);

        run(runner(USERNAME, PASSWORD, EMAIL));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(userService).createSeededAdmin(eq(USERNAME), eq(EMAIL), hash.capture());
        assertThat(hash.getValue()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, hash.getValue())).isTrue();
    }

    @Test
    @DisplayName("T02 does nothing when a seeded admin already exists")
    void doesNothingWhenSeededAdminExists() throws Exception {
        when(userService.existsSeededAdmin()).thenReturn(true);

        run(runner(USERNAME, PASSWORD, EMAIL));

        verify(userService, never()).createSeededAdmin(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("T03 running twice creates only one admin")
    void runningTwiceCreatesOneAdmin() throws Exception {
        when(userService.existsSeededAdmin()).thenReturn(false, true);
        SeededAdminRunner runner = runner(USERNAME, PASSWORD, EMAIL);

        run(runner);
        run(runner);

        verify(userService, times(1)).createSeededAdmin(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "T04a blank username [{0}] fails fast")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankUsernameFailsFast(String username) {
        assertThatThrownBy(() -> run(runner(username, PASSWORD, EMAIL)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_USERNAME")
                .hasMessageNotContaining(PASSWORD);
        verifyNoInteractions(userService);
    }

    @ParameterizedTest(name = "T04b blank password [{0}] fails fast")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankPasswordFailsFast(String password) {
        assertThatThrownBy(() -> run(runner(USERNAME, password, EMAIL)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageNotContaining(USERNAME);
        verifyNoInteractions(userService);
    }

    @ParameterizedTest(name = "T05 missing email [{0}] uses the default")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void missingEmailUsesDefault(String email) throws Exception {
        when(userService.existsSeededAdmin()).thenReturn(false);

        run(runner(USERNAME, PASSWORD, email));

        verify(userService).createSeededAdmin(eq(USERNAME), eq("admin@sakan.local"), anyString());
    }

    @Test
    @DisplayName("T06a nothing secret is logged when the admin is created")
    void noSecretsLoggedOnCreate() throws Exception {
        when(userService.existsSeededAdmin()).thenReturn(false);

        run(runner(USERNAME, PASSWORD, EMAIL));

        assertNoSecretsLogged();
    }

    @Test
    @DisplayName("T06b nothing secret is logged when the admin already exists")
    void noSecretsLoggedWhenSkipped() throws Exception {
        when(userService.existsSeededAdmin()).thenReturn(true);

        run(runner(USERNAME, PASSWORD, EMAIL));

        assertNoSecretsLogged();
    }

    @Test
    @DisplayName("E1 username already used by a normal user: fail, never convert the account")
    void usernameTakenByNormalUserFails() {
        when(userService.existsSeededAdmin()).thenReturn(false);
        when(userService.existsByUsername(USERNAME)).thenReturn(true);

        assertThatThrownBy(() -> run(runner(USERNAME, PASSWORD, EMAIL)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_USERNAME");
        verify(userService, never()).createSeededAdmin(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("E2 email already used by a normal user: fail, never convert the account")
    void emailTakenByNormalUserFails() {
        when(userService.existsSeededAdmin()).thenReturn(false);
        when(userService.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> run(runner(USERNAME, PASSWORD, EMAIL)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");
        verify(userService, never()).createSeededAdmin(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("E3 email is trimmed and lower-cased like registration does")
    void emailIsNormalised() throws Exception {
        when(userService.existsSeededAdmin()).thenReturn(false);

        run(runner(USERNAME, PASSWORD, "  Admin@Sakan.TEST "));

        verify(userService).createSeededAdmin(eq(USERNAME), eq(EMAIL), anyString());
    }
}