package com.authservice.config.jwt;

import com.authservice.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenProviderTest {

    private static final String TEST_SECRET = "test-only-secret-key-for-unit-tests-0123456789";

    private static TokenProvider providerWith(String secret) {
        TokenProvider provider = new TokenProvider();
        ReflectionTestUtils.setField(provider, "secretKey", secret);
        ReflectionTestUtils.setField(provider, "expirationHours", 24L);
        return provider;
    }

    @Test
    @DisplayName("비밀키가 없으면 서버가 시작되지 않는다")
    void missingSecretStopsStartup() {
        assertThatThrownBy(() -> providerWith(null).init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_KEY");
        assertThatThrownBy(() -> providerWith("  ").init())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("32바이트보다 짧은 비밀키는 거부한다")
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> providerWith("short-secret").init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
    }

    @Test
    @DisplayName("설정한 비밀키로 서명한 토큰만 유효하다")
    void onlyTokensSignedWithConfiguredSecretAreValid() {
        TokenProvider provider = providerWith(TEST_SECRET);
        provider.init();
        User user = User.builder().email("user@example.com").build();

        String token = provider.generateToken(user, Duration.ofMinutes(5));

        assertThat(provider.validToken(token)).isTrue();
        assertThat(provider.getUserEmail(token)).isEqualTo("user@example.com");

        TokenProvider otherServer = providerWith(TEST_SECRET + "-other");
        otherServer.init();
        assertThat(otherServer.validToken(token)).isFalse();
    }
}
