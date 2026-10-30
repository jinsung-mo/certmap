package com.authservice.config.oauth;

import com.authservice.domain.User;
import com.authservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService service;

    private static User account(String email, String provider, String providerId, boolean emailVerified) {
        return User.builder()
                .email(email)
                .name("기존 이름")
                .provider(provider)
                .providerId(providerId)
                .emailVerified(emailVerified)
                .build();
    }

    private static GoogleUserInfo google(String email, boolean emailVerified) {
        return new GoogleUserInfo(Map.of("sub", "google-1", "email", email, "name", "구글 사용자",
                "email_verified", emailVerified));
    }

    private static NaverUserInfo naver(String email) {
        return new NaverUserInfo(Map.of("response", Map.of("id", "naver-1", "email", email, "name", "네이버 사용자")));
    }

    private static String errorCode(Throwable e) {
        return ((OAuth2AuthenticationException) e).getError().getErrorCode();
    }

    @Test
    @DisplayName("같은 제공자의 같은 사용자 ID면 이메일과 상관없이 그 계정으로 로그인한다")
    void sameProviderIdLogsIntoLinkedAccount() {
        User linked = account("user@example.com", "google", "google-1", true);
        when(userRepository.findByProviderAndProviderId("google", "google-1")).thenReturn(Optional.of(linked));

        assertThat(service.resolveUser(google("user@example.com", true))).isSameAs(linked);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("이메일 인증이 확인되지 않은 소셜 계정으로는 같은 이메일의 남의 계정에 들어갈 수 없다")
    void unverifiedSocialEmailCannotTakeOverAccount() {
        when(userRepository.findByProviderAndProviderId("naver", "naver-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("victim@example.com"))
                .thenReturn(Optional.of(account("victim@example.com", "google", "google-1", true)));

        assertThatThrownBy(() -> service.resolveUser(naver("victim@example.com")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("account_link_denied"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일 인증이 없는 일반 가입 계정에는 소셜 계정을 자동으로 연결하지 않는다")
    void verifiedSocialEmailDoesNotJoinUnverifiedLocalAccount() {
        when(userRepository.findByProviderAndProviderId("google", "google-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("someone@example.com"))
                .thenReturn(Optional.of(account("someone@example.com", null, null, false)));

        assertThatThrownBy(() -> service.resolveUser(google("someone@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("account_link_denied"));
    }

    @Test
    @DisplayName("양쪽 모두 이메일 인증이 확인되면 같은 이메일 계정으로 로그인한다")
    void verifiedEmailsOnBothSidesAreLinked() {
        User existing = account("user@example.com", "kakao", "kakao-1", true);
        when(userRepository.findByProviderAndProviderId("google", "google-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

        assertThat(service.resolveUser(google("user@example.com", true))).isSameAs(existing);
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("처음 보는 이메일이면 제공자의 인증 여부를 기록해 새 계정을 만든다")
    void newEmailCreatesAccountWithVerificationFlag() {
        when(userRepository.findByProviderAndProviderId("naver", "naver-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        User created = service.resolveUser(naver("new@example.com"));

        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getProvider()).isEqualTo("naver");
        assertThat(created.getProviderId()).isEqualTo("naver-1");
        assertThat(created.isEmailVerified()).isFalse();
        verify(userRepository).save(created);
    }

    @Test
    @DisplayName("소셜 계정에서 이메일을 받지 못하면 로그인하지 않는다")
    void missingEmailIsRejected() {
        when(userRepository.findByProviderAndProviderId("google", "google-1")).thenReturn(Optional.empty());
        GoogleUserInfo withoutEmail = new GoogleUserInfo(Map.of("sub", "google-1", "name", "이메일 없음"));

        assertThatThrownBy(() -> service.resolveUser(withoutEmail))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("email_required"));
    }
}
