package com.authservice.config.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginCodeServiceTest {

    @Test
    @DisplayName("코드는 한 번만 사용자 ID로 바꿀 수 있다")
    void codeCanBeConsumedOnlyOnce() {
        OAuth2LoginCodeService service = new OAuth2LoginCodeService();
        String code = service.issue(7L);

        assertThat(service.consume(code)).contains(7L);
        assertThat(service.consume(code)).isEmpty();
    }

    @Test
    @DisplayName("60초가 지난 코드는 거절한다")
    void expiredCodeIsRejected() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-30T00:00:00Z"));
        OAuth2LoginCodeService service = new OAuth2LoginCodeService(clock, Duration.ofSeconds(60));
        String code = service.issue(7L);

        clock.advance(Duration.ofSeconds(60));

        assertThat(service.consume(code)).isEmpty();
    }

    @Test
    @DisplayName("발급하지 않은 코드나 빈 코드는 거절한다")
    void unknownOrBlankCodeIsRejected() {
        OAuth2LoginCodeService service = new OAuth2LoginCodeService();

        assertThat(service.consume("not-issued")).isEmpty();
        assertThat(service.consume("")).isEmpty();
        assertThat(service.consume(null)).isEmpty();
    }

    @Test
    @DisplayName("코드는 매번 다르고 URL에 그대로 넣을 수 있다")
    void codesAreUniqueAndUrlSafe() {
        OAuth2LoginCodeService service = new OAuth2LoginCodeService();

        String first = service.issue(1L);
        String second = service.issue(1L);

        assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{43}");
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
