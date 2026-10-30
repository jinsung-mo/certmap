package com.authservice.controller;

import lombok.RequiredArgsConstructor;
import com.authservice.config.jwt.TokenProvider;
import com.authservice.config.oauth.OAuth2LoginCodeService;
import com.authservice.domain.User;
import com.authservice.dto.AddUserRequest;
import com.authservice.dto.LoginRequest;
import com.authservice.dto.LoginResponse;
import com.authservice.dto.OAuth2CodeRequest;
import com.authservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final OAuth2LoginCodeService loginCodeService;

    /**
     * 회원가입 API
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody AddUserRequest request){
        userService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 로그인 API
     * POST /api/auth/login
     *
     * 성공 시 JWT 토큰 반환
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 사용자 인증
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 인증된 사용자 정보 가져오기
        User user = userService.findByEmail(request.getEmail());

        // JWT 토큰 생성 (24시간 유효)
        String token = tokenProvider.generateToken(user, Duration.ofHours(24));

        return ResponseEntity.ok(new LoginResponse(token));
    }

    /**
     * 소셜 로그인 코드 교환 API
     * POST /api/auth/oauth2/token
     *
     * 소셜 로그인 성공 후 리다이렉트 주소에 붙은 1회용 코드를 JWT로 바꿔 준다.
     * 코드는 60초 동안 한 번만 쓸 수 있고, 없거나 만료된 코드에는 401을 돌려준다.
     */
    @PostMapping("/oauth2/token")
    public ResponseEntity<LoginResponse> exchangeOAuth2Code(@RequestBody OAuth2CodeRequest request) {
        return loginCodeService.consume(request.getCode())
                .map(userService::findById)
                .map(user -> ResponseEntity.ok(new LoginResponse(tokenProvider.generateToken(user, Duration.ofHours(24)))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * 토큰 검증 API
     * GET /api/auth/validate
     *
     * 헤더의 Authorization 토큰이 유효한지 확인
     */
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenProvider.validToken(token)) {
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
