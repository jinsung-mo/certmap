package com.authservice.config.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2UserInfoTest {

    private static KakaoUserInfo kakao(boolean emailValid, boolean emailVerified) {
        return new KakaoUserInfo(Map.of("id", 1L, "kakao_account",
                Map.of("email", "user@kakao.com", "is_email_valid", emailValid, "is_email_verified", emailVerified)));
    }

    @Test
    @DisplayName("구글은 email_verified가 true일 때만 인증된 이메일로 본다")
    void googleRequiresEmailVerified() {
        assertThat(new GoogleUserInfo(Map.of("email_verified", true)).isEmailVerified()).isTrue();
        assertThat(new GoogleUserInfo(Map.of("email_verified", false)).isEmailVerified()).isFalse();
        assertThat(new GoogleUserInfo(Map.of()).isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("카카오는 유효하고 인증된 이메일만 믿는다")
    void kakaoRequiresValidAndVerifiedEmail() {
        assertThat(kakao(true, true).isEmailVerified()).isTrue();
        assertThat(kakao(true, false).isEmailVerified()).isFalse();
        assertThat(kakao(false, true).isEmailVerified()).isFalse();
        assertThat(new KakaoUserInfo(Map.of("id", 1L)).isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("네이버는 인증 여부를 알려 주지 않으므로 미인증으로 본다")
    void naverIsTreatedAsUnverified() {
        NaverUserInfo naver = new NaverUserInfo(Map.of("response", Map.of("id", "naver-1", "email", "user@naver.com")));

        assertThat(naver.isEmailVerified()).isFalse();
    }
}
