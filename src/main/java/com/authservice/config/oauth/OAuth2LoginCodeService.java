package com.authservice.config.oauth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 소셜 로그인 성공 후 프론트엔드에 넘길 1회용 코드를 발급하고 교환한다.
 *
 * JWT를 리다이렉트 URL에 그대로 붙이면 브라우저 기록, 서버 로그, Referer 헤더에 토큰이 남는다.
 * 그래서 URL에는 60초 동안 한 번만 쓸 수 있는 코드만 싣고, 프론트엔드가 POST 요청으로 토큰과 바꾸게 한다.
 * 코드는 서버 메모리에 두므로 서버가 여러 대가 되면 Redis 같은 공유 저장소로 옮겨야 한다.
 */
@Component
public class OAuth2LoginCodeService {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);
    private static final int CODE_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    public OAuth2LoginCodeService() {
        this(Clock.systemUTC(), DEFAULT_TTL);
    }

    OAuth2LoginCodeService(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    // 사용자 ID에 묶인 1회용 코드를 만든다
    public String issue(Long userId) {
        removeExpired();
        byte[] bytes = new byte[CODE_BYTES];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendingLogins.put(code, new PendingLogin(userId, clock.instant().plus(ttl)));
        return code;
    }

    // 코드를 꺼내면서 지워 한 번만 쓰게 한다. 없거나 만료된 코드면 빈 값을 돌려준다.
    public Optional<Long> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        PendingLogin login = pendingLogins.remove(code);
        if (login == null || !clock.instant().isBefore(login.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(login.userId());
    }

    private void removeExpired() {
        Instant now = clock.instant();
        pendingLogins.values().removeIf(login -> !now.isBefore(login.expiresAt()));
    }

    private record PendingLogin(Long userId, Instant expiresAt) {
    }
}
