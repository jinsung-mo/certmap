package com.authservice.config.oauth;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final OAuth2LoginCodeService loginCodeService;

    // 로그인 후 돌아갈 프론트엔드 주소 (OAUTH2_REDIRECT_URI)
    // 요청 값이 아니라 서버 설정으로만 정해서, 로그인 결과를 임의의 주소로 보내지 않는다.
    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @PostConstruct
    void validateRedirectUri() {
        URI uri = URI.create(redirectUri);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme) || uri.getHost() == null) {
            throw new IllegalStateException("OAUTH2_REDIRECT_URI는 http 또는 https 주소여야 합니다: " + redirectUri);
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        // JWT 대신 1회용 코드만 붙여 보낸다. 프론트엔드는 POST /api/auth/oauth2/token 으로 코드를 토큰과 바꾼다.
        String code = loginCodeService.issue(principalDetails.getUser().getId());
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
