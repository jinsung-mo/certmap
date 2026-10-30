package com.authservice.controller;

import com.authservice.config.jwt.TokenProvider;
import com.authservice.config.oauth.OAuth2LoginCodeService;
import com.authservice.domain.User;
import com.authservice.service.UserService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerOAuth2TokenTest {

    private static final String TEST_SECRET = "test-only-secret-key-for-unit-tests-0123456789";

    private MockMvc mockMvc;
    private OAuth2LoginCodeService codes;
    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        User user = User.builder().email("user@example.com").build();
        ReflectionTestUtils.setField(user, "id", 42L);
        UserService userService = mock(UserService.class);
        when(userService.findById(42L)).thenReturn(user);

        tokenProvider = new TokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "expirationHours", 24L);
        ReflectionTestUtils.invokeMethod(tokenProvider, "init");

        codes = new OAuth2LoginCodeService();
        AuthController controller = new AuthController(userService, tokenProvider, mock(AuthenticationManager.class), codes);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("1회용 코드는 한 번만 JWT로 바꿀 수 있다")
    void exchangesCodeForTokenOnlyOnce() throws Exception {
        String body = "{\"code\":\"" + codes.issue(42L) + "\"}";

        String json = mockMvc.perform(post("/api/auth/oauth2/token").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(json, "$.accessToken");
        assertThat(tokenProvider.getUserEmail(accessToken)).isEqualTo("user@example.com");

        mockMvc.perform(post("/api/auth/oauth2/token").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
