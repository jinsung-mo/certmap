# 개발 로그 (Development Log)

이 문서는 프로젝트의 모든 개발 과정과 변경사항을 기록합니다.
새로운 기능 추가나 수정이 있을 때마다 이 문서를 업데이트합니다.

---

## 2025-10-14: 프로젝트 초기 설정 및 소셜 로그인 기능 추가

### 1. 초기 프로젝트 구조
- **프레임워크**: Spring Boot 3.5.4
- **Java 버전**: 17
- **빌드 도구**: Gradle
- **데이터베이스**: H2 (인메모리)
- **인증 방식**: JWT (JSON Web Token)

### 2. 기본 인증 시스템
- JWT 기반 인증/인가 시스템 구현
- 회원가입/로그인 API
- BCrypt 패스워드 암호화

### 3. 소셜 로그인 기능 추가

#### 3.1 추가된 의존성
```gradle
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

#### 3.2 User 엔티티 확장
**파일**: `src/main/java/com/authservice/domain/User.java`

추가된 필드:
- `name`: String - 사용자 이름
- `provider`: String - 소셜 로그인 제공자 (google, kakao, naver)
- `providerId`: String - 제공자의 사용자 고유 ID

추가된 메소드:
- `update(String name)`: 사용자 정보 업데이트

#### 3.3 OAuth2 관련 클래스 구조

**추상 클래스 및 인터페이스**:
- `OAuth2UserInfo` (추상 클래스): 모든 OAuth2 제공자의 공통 인터페이스
  - 위치: `src/main/java/com/authservice/config/oauth/OAuth2UserInfo.java`
  - 메소드: `getProviderId()`, `getProvider()`, `getEmail()`, `getName()`

**구현 클래스**:
1. `GoogleUserInfo`: Google OAuth2 응답 처리
   - 위치: `src/main/java/com/authservice/config/oauth/GoogleUserInfo.java`
   - 주요 속성: `sub` (providerId), `email`, `name`

2. `KakaoUserInfo`: Kakao OAuth2 응답 처리
   - 위치: `src/main/java/com/authservice/config/oauth/KakaoUserInfo.java`
   - 주요 속성: `id` (providerId), `kakao_account.email`, `properties.nickname`

3. `NaverUserInfo`: Naver OAuth2 응답 처리
   - 위치: `src/main/java/com/authservice/config/oauth/NaverUserInfo.java`
   - 주요 속성: `response.id` (providerId), `response.email`, `response.name`

**통합 인증 클래스**:
- `PrincipalDetails`: `UserDetails`와 `OAuth2User` 인터페이스를 모두 구현
  - 위치: `src/main/java/com/authservice/config/oauth/PrincipalDetails.java`
  - 목적: 일반 로그인과 OAuth2 로그인 통합 처리

#### 3.4 OAuth2 서비스 레이어

**CustomOAuth2UserService**:
- 위치: `src/main/java/com/authservice/config/oauth/CustomOAuth2UserService.java`
- 역할: OAuth2 로그인 시 사용자 정보 처리
- 주요 로직:
  1. OAuth2 제공자로부터 사용자 정보 수신
  2. 제공자별로 적절한 UserInfo 객체 생성
  3. 이메일로 기존 사용자 조회
  4. 신규 사용자면 DB에 저장, 기존 사용자면 정보 업데이트
  5. `PrincipalDetails` 객체 반환

**OAuth2SuccessHandler**:
- 위치: `src/main/java/com/authservice/config/oauth/OAuth2SuccessHandler.java`
- 역할: OAuth2 로그인 성공 시 후처리
- 주요 로직:
  1. 인증된 사용자 정보로 JWT 토큰 생성
  2. 프론트엔드 리다이렉트 URL에 토큰 추가
  3. 리다이렉트 실행

#### 3.5 보안 설정 업데이트

**WebSecurityConfig**:
- 위치: `src/main/java/com/authservice/config/WebSecurityConfig.java`
- 변경사항:
  1. `CustomOAuth2UserService`와 `OAuth2SuccessHandler` 의존성 주입
  2. OAuth2 경로 허용: `/login/**`, `/oauth2/**`
  3. OAuth2 로그인 설정 추가:
     ```java
     .oauth2Login(oauth2 -> oauth2
         .userInfoEndpoint(userInfo -> userInfo
             .userService(customOAuth2UserService))
         .successHandler(oAuth2SuccessHandler))
     ```

#### 3.6 설정 파일

**application.yml**:
- 위치: `src/main/resources/application.yml`
- OAuth2 설정 추가:
  - Google, Kakao, Naver 각각의 client-id, client-secret, scope 설정
  - Kakao, Naver는 커스텀 provider 설정 (authorization-uri, token-uri, user-info-uri)
  - Redirect URI 설정

### 4. API 엔드포인트

#### 기존 API (일반 로그인)
- `POST /api/auth/signup`: 회원가입
- `POST /api/auth/login`: 로그인

#### 소셜 로그인 시작 URL
- Google: `GET /oauth2/authorization/google`
- Kakao: `GET /oauth2/authorization/kakao`
- Naver: `GET /oauth2/authorization/naver`

#### 콜백 URL (자동 처리)
- Google: `/login/oauth2/code/google`
- Kakao: `/login/oauth2/code/kakao`
- Naver: `/login/oauth2/code/naver`

#### 성공 시 리다이렉트
- URL: `http://localhost:3000/oauth2/redirect?token={JWT_TOKEN}`

### 5. 데이터베이스 스키마 변경

**users 테이블**:
```sql
ALTER TABLE users ADD COLUMN name VARCHAR(255);
ALTER TABLE users ADD COLUMN provider VARCHAR(50);
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);
```

### 6. 설정 필요 사항

각 소셜 플랫폼에서 OAuth2 애플리케이션 등록 필요:

1. **Google Cloud Console** (https://console.cloud.google.com)
   - OAuth 2.0 클라이언트 ID 생성
   - Redirect URI: `http://localhost:8080/login/oauth2/code/google`

2. **Kakao Developers** (https://developers.kakao.com)
   - 애플리케이션 추가
   - Redirect URI: `http://localhost:8080/login/oauth2/code/kakao`
   - 동의 항목: profile_nickname, account_email

3. **Naver Developers** (https://developers.naver.com)
   - 애플리케이션 등록
   - Callback URL: `http://localhost:8080/login/oauth2/code/naver`
   - 사용 API: 회원이름, 이메일 주소

발급받은 Client ID와 Secret을 `application.yml`에 입력해야 합니다.

### 7. 보안 고려사항

- 소셜 로그인 사용자는 password 필드가 null입니다
- provider와 providerId로 소셜 로그인 사용자 구분
- 이메일 중복 시 기존 계정에 소셜 로그인 연동 (같은 이메일 사용)
- JWT 토큰 만료 시간: 24시간

---

## 다음 개발 계획 (TODO)

- [ ] 리프레시 토큰 기능 추가
- [ ] 이메일 인증 기능
- [ ] 비밀번호 찾기/재설정 기능
- [ ] 프로필 관리 API
- [ ] 계정 연동/해제 기능 (일반 로그인 ↔ 소셜 로그인)
- [ ] 운영 DB 연동 (PostgreSQL/MySQL)
- [ ] 로깅 및 모니터링 설정
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] 테스트 코드 작성

---

## 문서 업데이트 가이드

새로운 기능을 추가하거나 수정할 때마다 다음 형식으로 이 문서를 업데이트하세요:

```markdown
## YYYY-MM-DD: [변경사항 제목]

### 1. [카테고리]
- 변경된 파일 경로
- 주요 변경사항
- 이유 및 목적

### 2. API 변경사항 (있는 경우)
- 추가/수정/삭제된 엔드포인트

### 3. 데이터베이스 변경사항 (있는 경우)
- 스키마 변경 내용

### 4. 설정 변경사항 (있는 경우)
- application.yml 등 설정 파일 변경

---
```

**README.md도 함께 업데이트**하여 프로젝트 개요와 빠른 시작 가이드를 최신 상태로 유지하세요.
