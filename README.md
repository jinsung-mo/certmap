# Auth Service - JWT 기반 인증 서비스

Spring Boot 기반의 독립적인 JWT 인증 서비스입니다. 일반 로그인과 소셜 로그인(Google, Kakao, Naver)을 지원하며, 프론트엔드와 협업하기 쉽도록 RESTful API로 구현되었습니다.

## 목차

- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [API 명세](#api-명세)
- [소셜 로그인 설정](#소셜-로그인-설정)
- [프론트엔드 통합 가이드](#프론트엔드-통합-가이드)
- [운영 환경 배포](#운영-환경-배포)
- [트러블슈팅](#트러블슈팅)

## 기술 스택

- **Spring Boot 3.5.4**
- **Java 17**
- **Spring Security + JWT + OAuth2**
- **Spring Data JPA**
- **H2 Database** (개발용, 운영환경에서는 PostgreSQL/MySQL 권장)
- **Gradle**

## 주요 기능

### 인증/인가
- ✅ 회원가입 (이메일/비밀번호)
- ✅ 로그인 (JWT 토큰 발급)
- ✅ JWT 토큰 검증
- ✅ 비밀번호 암호화 (BCrypt)

### 소셜 로그인
- ✅ Google OAuth2 로그인
- ✅ Kakao OAuth2 로그인
- ✅ Naver OAuth2 로그인
- ✅ 소셜 계정 연동 (같은 이메일이어도 제공자가 이메일 소유를 확인한 계정끼리만 연결)

### 기타
- ✅ CORS 설정 (프론트엔드 협업용)
- ✅ H2 콘솔 (개발 모드)

### 보안 보완 (프로젝트 종료 후, 2026-10)
- JWT 비밀키의 기본값을 없앴습니다. `JWT_SECRET_KEY`가 없거나 32바이트보다 짧으면 서버가 시작되지 않습니다.
- 소셜 로그인 후 JWT를 URL에 싣지 않습니다. 60초 동안 한 번만 쓸 수 있는 코드를 보내고, 프론트엔드가 `POST /api/auth/oauth2/token`으로 토큰과 바꿉니다. 개발용 로컬 파일 경로로 보내던 리다이렉트는 `OAUTH2_REDIRECT_URI` 설정으로 바꿨습니다.
- 제공자가 이메일 소유를 확인하지 않은 소셜 계정은 같은 이메일의 기존 계정에 연결하지 않습니다. 소셜 로그인은 제공자와 제공자 사용자 ID로 먼저 찾습니다.

## 프로젝트 구조

```
Backend_service/
├── src/main/java/com/authservice/
│   ├── config/
│   │   ├── jwt/
│   │   │   ├── TokenProvider.java              # JWT 토큰 생성/검증
│   │   │   └── JwtAuthenticationFilter.java    # JWT 인증 필터
│   │   ├── oauth/
│   │   │   ├── OAuth2UserInfo.java             # OAuth2 사용자 정보 추상 클래스
│   │   │   ├── GoogleUserInfo.java             # Google 사용자 정보 처리
│   │   │   ├── KakaoUserInfo.java              # Kakao 사용자 정보 처리
│   │   │   ├── NaverUserInfo.java              # Naver 사용자 정보 처리
│   │   │   ├── PrincipalDetails.java           # 통합 인증 정보
│   │   │   ├── CustomOAuth2UserService.java    # OAuth2 사용자 처리 서비스
│   │   │   ├── OAuth2LoginCodeService.java     # 소셜 로그인 1회용 코드 발급과 교환
│   │   │   └── OAuth2SuccessHandler.java       # OAuth2 성공 핸들러 (1회용 코드로 리다이렉트)
│   │   └── WebSecurityConfig.java              # Spring Security 설정
│   ├── controller/
│   │   └── AuthController.java                 # 인증 API 컨트롤러
│   ├── domain/
│   │   └── User.java                           # 사용자 엔티티
│   ├── dto/
│   │   ├── AddUserRequest.java                 # 회원가입 요청 DTO
│   │   ├── LoginRequest.java                   # 로그인 요청 DTO
│   │   ├── OAuth2CodeRequest.java              # 소셜 로그인 코드 교환 요청 DTO
│   │   └── LoginResponse.java                  # 로그인 응답 DTO
│   ├── repository/
│   │   └── UserRepository.java                 # 사용자 Repository
│   ├── service/
│   │   ├── UserService.java                    # 사용자 서비스
│   │   └── UserDetailService.java              # Spring Security 사용자 인증
│   └── AuthServiceApplication.java             # 메인 애플리케이션
├── src/main/resources/
│   └── application.yml                         # 설정 파일
├── src/test/java/com/authservice/              # 보안 보완 단위 테스트와 통합 테스트
├── DEVELOPMENT_LOG.md                          # 개발 로그 (상세 기록)
└── README.md                                   # 프로젝트 개요 (현재 문서)
```

## 시작하기

### 1. 프로젝트 클론 및 빌드

```bash
cd certmap

# 필수: JWT 서명 키 (32바이트 이상, 기본값 없음)
export JWT_SECRET_KEY="$(openssl rand -base64 48)"

# 소셜 로그인 키 (각 플랫폼에서 발급)
export GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=...
export KAKAO_CLIENT_ID=... KAKAO_CLIENT_SECRET=...
export NAVER_CLIENT_ID=... NAVER_CLIENT_SECRET=...

# 선택: 소셜 로그인 후 돌아갈 프론트엔드 주소 (기본값 http://localhost:3000/oauth2/redirect)
export OAUTH2_REDIRECT_URI="http://localhost:3000/oauth2/redirect"

# Gradle 빌드 (테스트 포함)
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

### 2. H2 콘솔 접속 (개발 모드)

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:authdb`
- Username: `sa`
- Password: (비어있음)

## API 명세

### 일반 로그인 API

#### 1. 회원가입

**Endpoint:** `POST /api/auth/signup`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `201 Created`

**cURL 예제:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

---

#### 2. 로그인

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWQiOjEsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsImlhdCI6MTY4MDAwMDAwMCwiZXhwIjoxNjgwMDg2NDAwfQ.xxxxx"
}
```

**cURL 예제:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

---

#### 3. 토큰 검증

**Endpoint:** `GET /api/auth/validate`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
- `200 OK` - 토큰이 유효함
- `401 Unauthorized` - 토큰이 유효하지 않음

**cURL 예제:**
```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 소셜 로그인 API

#### 소셜 로그인 시작 URL

프론트엔드에서 다음 URL로 리다이렉트하면 소셜 로그인이 시작됩니다:

- **Google**: `http://localhost:8080/oauth2/authorization/google`
- **Kakao**: `http://localhost:8080/oauth2/authorization/kakao`
- **Naver**: `http://localhost:8080/oauth2/authorization/naver`

#### 로그인 성공 후 리다이렉트와 코드 교환

소셜 로그인에 성공하면 `OAUTH2_REDIRECT_URI`(기본값 `http://localhost:3000/oauth2/redirect`)로 돌아갑니다. JWT는 URL에 싣지 않고, 60초 동안 한 번만 쓸 수 있는 코드만 붙입니다.

```
http://localhost:3000/oauth2/redirect?code={1회용 코드}
```

프론트엔드는 이 코드를 JWT로 바꿉니다.

**Endpoint:** `POST /api/auth/oauth2/token`

**Request Body:**
```json
{
  "code": "{1회용 코드}"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

코드가 없거나, 이미 썼거나, 60초가 지났으면 `401 Unauthorized`를 돌려줍니다. 토큰을 URL에 넣으면 브라우저 기록, 서버 로그, Referer 헤더에 남기 때문에 이렇게 바꿨습니다.

## 소셜 로그인 설정

### 1. OAuth2 애플리케이션 등록

각 소셜 플랫폼의 개발자 콘솔에서 OAuth2 애플리케이션을 등록해야 합니다:

#### Google
1. [Google Cloud Console](https://console.cloud.google.com) 접속
2. 프로젝트 생성 또는 선택
3. API 및 서비스 > OAuth 동의 화면 설정
4. 사용자 인증 정보 > OAuth 2.0 클라이언트 ID 생성
5. **승인된 리디렉션 URI**: `http://localhost:8080/login/oauth2/code/google`
6. Client ID와 Client Secret 복사

#### Kakao
1. [Kakao Developers](https://developers.kakao.com) 접속
2. 애플리케이션 추가
3. 내 애플리케이션 > 앱 설정 > 요약 정보에서 REST API 키 확인
4. 제품 설정 > 카카오 로그인 활성화
5. **Redirect URI**: `http://localhost:8080/login/oauth2/code/kakao`
6. 동의 항목 설정: 닉네임, 이메일 필수 동의
7. Client Secret 발급 (선택사항이지만 권장)

#### Naver
1. [Naver Developers](https://developers.naver.com) 접속
2. 애플리케이션 등록
3. 사용 API: 네이버 로그인
4. **서비스 URL**: `http://localhost:8080`
5. **Callback URL**: `http://localhost:8080/login/oauth2/code/naver`
6. 제공 정보: 이메일, 이름 선택
7. Client ID와 Client Secret 복사

### 2. application.yml 설정

발급받은 Client ID와 Secret은 `application.yml`에 직접 적지 말고 환경 변수로 넣습니다. `application.yml`은 아래처럼 환경 변수를 읽습니다:

```yaml
spring.security.oauth2:
  client:
    registration:
      google:
        client-id: ${GOOGLE_CLIENT_ID}
        client-secret: ${GOOGLE_CLIENT_SECRET}

      kakao:
        client-id: ${KAKAO_CLIENT_ID}
        client-secret: ${KAKAO_CLIENT_SECRET}

      naver:
        client-id: ${NAVER_CLIENT_ID}
        client-secret: ${NAVER_CLIENT_SECRET}
```

## 프론트엔드 통합 가이드

### React 예제 (일반 로그인 + 소셜 로그인)

```javascript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/auth';

// Axios 인스턴스 생성
const authAPI = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 1. 일반 회원가입
export const signup = async (email, password) => {
  return await authAPI.post('/signup', { email, password });
};

// 2. 일반 로그인
export const login = async (email, password) => {
  const response = await authAPI.post('/login', { email, password });
  const { accessToken } = response.data;

  localStorage.setItem('accessToken', accessToken);
  return accessToken;
};

// 3. 소셜 로그인 (Google, Kakao, Naver)
export const socialLogin = (provider) => {
  // provider: 'google', 'kakao', 'naver'
  window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
};

// 4. OAuth2 리다이렉트 처리 (React Router 사용)
// /oauth2/redirect 경로에서 1회용 코드를 JWT로 바꾼다
export const handleOAuth2Redirect = async () => {
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');
  // 코드가 주소창과 방문 기록에 남지 않게 바로 지운다
  window.history.replaceState({}, document.title, window.location.pathname);

  if (code) {
    const response = await authAPI.post('/oauth2/token', { code });
    localStorage.setItem('accessToken', response.data.accessToken);
    // 메인 페이지로 이동
    window.location.href = '/';
  }
};

// 5. 인증 헤더 자동 추가 인터셉터
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 6. 로그아웃
export const logout = () => {
  localStorage.removeItem('accessToken');
  window.location.href = '/login';
};
```

### React 컴포넌트 예제

```jsx
import React from 'react';
import { socialLogin } from './authService';

function LoginPage() {
  return (
    <div>
      <h1>로그인</h1>

      {/* 일반 로그인 폼 */}
      <form onSubmit={handleSubmit}>
        <input type="email" placeholder="이메일" />
        <input type="password" placeholder="비밀번호" />
        <button type="submit">로그인</button>
      </form>

      <hr />

      {/* 소셜 로그인 버튼 */}
      <h2>소셜 로그인</h2>
      <button onClick={() => socialLogin('google')}>
        Google로 로그인
      </button>
      <button onClick={() => socialLogin('kakao')}>
        Kakao로 로그인
      </button>
      <button onClick={() => socialLogin('naver')}>
        Naver로 로그인
      </button>
    </div>
  );
}

// OAuth2 리다이렉트 처리 컴포넌트
function OAuth2RedirectHandler() {
  React.useEffect(() => {
    handleOAuth2Redirect();
  }, []);

  return <div>로그인 처리 중...</div>;
}

export { LoginPage, OAuth2RedirectHandler };
```

### Vue.js 예제

```javascript
// authService.js
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/auth';

export const authService = {
  // 일반 로그인
  async login(email, password) {
    const response = await axios.post(`${API_BASE_URL}/login`, {
      email,
      password
    });
    const { accessToken } = response.data;
    localStorage.setItem('accessToken', accessToken);
    return accessToken;
  },

  // 소셜 로그인
  socialLogin(provider) {
    window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
  },

  // 소셜 로그인 후 돌아온 페이지에서 1회용 코드를 JWT로 바꾼다
  async exchangeOAuth2Code(code) {
    const response = await axios.post(`${API_BASE_URL}/oauth2/token`, { code });
    localStorage.setItem('accessToken', response.data.accessToken);
    return response.data.accessToken;
  },

  // 로그아웃
  logout() {
    localStorage.removeItem('accessToken');
  }
};
```

## 운영 환경 배포

### 1. JWT Secret Key 설정

`jwt.secret-key`는 기본값 없이 `JWT_SECRET_KEY` 환경 변수만 읽습니다. 값이 없거나 32바이트보다 짧으면 서버가 시작되지 않습니다. 저장소에 공개된 기본 키로 서버가 떠서 누구나 같은 키로 토큰을 위조할 수 있던 문제를 막기 위해서입니다.

```bash
export JWT_SECRET_KEY="$(openssl rand -base64 48)"
```

### 2. 데이터베이스 변경 (PostgreSQL 예시)

#### build.gradle 수정
```gradle
dependencies {
    // runtimeOnly 'com.h2database:h2'  // 주석 처리
    runtimeOnly 'org.postgresql:postgresql'
}
```

#### application.yml 수정
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # 운영에서는 validate 사용
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

### 3. CORS 설정 업데이트

운영 도메인을 추가합니다:

```yaml
cors:
  allowed-origins: https://yourdomain.com,https://www.yourdomain.com
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
```

### 4. OAuth2 Redirect URI 업데이트

각 소셜 플랫폼의 개발자 콘솔에서 운영 환경 Redirect URI를 추가합니다:

- Google: `https://yourdomain.com/login/oauth2/code/google`
- Kakao: `https://yourdomain.com/login/oauth2/code/kakao`
- Naver: `https://yourdomain.com/login/oauth2/code/naver`

application.yml에서도 redirect-uri를 업데이트합니다:

```yaml
spring.security.oauth2:
  client:
    registration:
      kakao:
        redirect-uri: https://yourdomain.com/login/oauth2/code/kakao
      naver:
        redirect-uri: https://yourdomain.com/login/oauth2/code/naver
```

로그인 후 돌아갈 프론트엔드 주소는 `OAUTH2_REDIRECT_URI`로 바꿉니다. http, https 주소만 허용합니다.

```bash
export OAUTH2_REDIRECT_URI="https://yourdomain.com/oauth2/redirect"
```

### 5. HTTPS 사용

JWT 토큰 전송 시 반드시 HTTPS를 사용해야 합니다. SSL 인증서를 설정하세요.

## 트러블슈팅

### 서버가 시작되지 않는 경우
- `JWT_SECRET_KEY` 환경 변수가 있는지, 32바이트 이상인지 확인
- `OAUTH2_REDIRECT_URI`가 http 또는 https 주소인지 확인

### CORS 에러가 발생하는 경우
- `application.yml`의 `cors.allowed-origins`에 프론트엔드 URL이 추가되어 있는지 확인
- 프론트엔드에서 `credentials: 'include'` 옵션 사용 시 `allow-credentials: true` 설정 확인

### 토큰 검증 실패
- 토큰 만료 여부 확인 (기본 24시간)
- `Authorization` 헤더 형식 확인: `Bearer <token>`
- JWT secret key가 서버와 일치하는지 확인

### 소셜 로그인 실패
- OAuth2 Client ID/Secret이 올바르게 설정되었는지 확인
- Redirect URI가 정확히 일치하는지 확인 (포트 번호 포함)
- 각 플랫폼의 콘솔에서 애플리케이션이 활성화되어 있는지 확인
- 요청하는 scope가 플랫폼에서 허용되는지 확인
- `account_link_denied`: 같은 이메일로 가입된 계정이 있지만, 이메일 인증이 확인되지 않아 연결하지 않은 경우입니다. 처음 가입한 방법으로 로그인하세요.
- `email_required`: 소셜 계정의 이메일 제공에 동의해야 합니다.
- 코드 교환이 401이면 코드가 60초 안에 한 번만 쓰였는지 확인

### H2 콘솔 접속 불가
- `application.yml`에서 `spring.h2.console.enabled: true` 확인
- `/h2-console` 경로가 Security에서 허용되어 있는지 확인

## 참고 문서

- **상세 개발 로그**: [DEVELOPMENT_LOG.md](./DEVELOPMENT_LOG.md) - 모든 개발 과정과 기술적 세부사항
- **Spring Security OAuth2**: https://spring.io/guides/tutorials/spring-boot-oauth2/
- **JWT 공식 문서**: https://jwt.io/

## 라이선스

MIT License

## 문의

문제가 발생하거나 질문이 있으면 이슈를 등록해주세요.

---

**마지막 업데이트**: 2025-10-14
**버전**: 1.0.0
