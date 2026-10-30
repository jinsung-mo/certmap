package com.authservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 보안 설정과 함께 서버를 띄워, 필수 환경 변수만 있으면 시작되고 코드 교환 API가 열려 있는지 확인한다
@SpringBootTest(properties = {
        "JWT_SECRET_KEY=test-only-secret-key-for-unit-tests-0123456789",
        "GOOGLE_CLIENT_ID=test", "GOOGLE_CLIENT_SECRET=test",
        "KAKAO_CLIENT_ID=test", "KAKAO_CLIENT_SECRET=test",
        "NAVER_CLIENT_ID=test", "NAVER_CLIENT_SECRET=test"
})
@AutoConfigureMockMvc
class OAuth2TokenEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("코드 교환 API는 로그인 없이 호출할 수 있고, 발급하지 않은 코드에는 401을 준다")
    void tokenEndpointIsPublicAndRejectsUnknownCode() throws Exception {
        mockMvc.perform(post("/api/auth/oauth2/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"not-issued\"}"))
                .andExpect(status().isUnauthorized());
    }
}
