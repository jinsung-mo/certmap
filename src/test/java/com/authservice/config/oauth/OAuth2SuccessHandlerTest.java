package com.authservice.config.oauth;

import com.authservice.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2SuccessHandlerTest {

    private static OAuth2SuccessHandler handlerWith(OAuth2LoginCodeService codes, String redirectUri) {
        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(codes);
        ReflectionTestUtils.setField(handler, "redirectUri", redirectUri);
        handler.validateRedirectUri();
        return handler;
    }

    @Test
    @DisplayName("로그인 성공 시 JWT 대신 1회용 코드만 붙여 설정된 주소로 보낸다")
    void redirectsWithOneTimeCodeInsteadOfToken() throws Exception {
        OAuth2LoginCodeService codes = new OAuth2LoginCodeService();
        OAuth2SuccessHandler handler = handlerWith(codes, "http://localhost:3000/oauth2/redirect");
        User user = User.builder().email("user@example.com").build();
        ReflectionTestUtils.setField(user, "id", 42L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new TestingAuthenticationToken(new PrincipalDetails(user, Map.of()), null));

        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).startsWith("http://localhost:3000/oauth2/redirect?code=")
                .doesNotContain("token=");
        String code = UriComponentsBuilder.fromUriString(redirectedUrl).build().getQueryParams().getFirst("code");
        assertThat(codes.consume(code)).contains(42L);
    }

    @Test
    @DisplayName("http, https가 아닌 리다이렉트 주소로는 서버가 시작되지 않는다")
    void nonHttpRedirectUriStopsStartup() {
        OAuth2LoginCodeService codes = new OAuth2LoginCodeService();

        assertThatThrownBy(() -> handlerWith(codes, "file:///Users/someone/test_social_login.html"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAUTH2_REDIRECT_URI");
    }
}
