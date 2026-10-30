package com.authservice.config.oauth;

import com.authservice.domain.User;
import com.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        User user = resolveUser(getOAuth2UserInfo(registrationId, attributes));
        return new PrincipalDetails(user, attributes);
    }

    // 소셜 계정을 서비스 계정에 연결한다.
    // 1) 같은 제공자의 같은 사용자 ID로 가입한 계정이 있으면 그 계정으로 로그인한다.
    // 2) 같은 이메일의 계정이 있으면, 제공자가 이메일 소유를 확인했고 기존 계정의 이메일도 확인된 경우에만 연결한다.
    //    확인되지 않은 이메일을 가진 소셜 계정으로 남의 계정에 들어가는 계정 탈취를 막기 위해서다.
    // 3) 둘 다 없으면 새 계정을 만든다.
    User resolveUser(OAuth2UserInfo userInfo) {
        User user = userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .map(linked -> linked.update(userInfo.getName()))
                .orElseGet(() -> linkByEmailOrCreate(userInfo));
        userRepository.save(user);
        return user;
    }

    private User linkByEmailOrCreate(OAuth2UserInfo userInfo) {
        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_required"),
                    "소셜 계정의 이메일 제공에 동의해야 로그인할 수 있습니다.");
        }

        Optional<User> sameEmail = userRepository.findByEmail(email);
        if (sameEmail.isEmpty()) {
            // 새로운 사용자 생성
            return User.builder()
                    .email(email)
                    .name(userInfo.getName())
                    .provider(userInfo.getProvider())
                    .providerId(userInfo.getProviderId())
                    .password(null) // OAuth2 로그인은 비밀번호 불필요
                    .emailVerified(userInfo.isEmailVerified())
                    .build();
        }

        User existing = sameEmail.get();
        if (!userInfo.isEmailVerified() || !existing.isEmailVerified()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_link_denied"),
                    "같은 이메일로 가입된 계정이 있습니다. 처음 가입한 방법으로 로그인해 주세요.");
        }
        return existing.update(userInfo.getName());
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equals("google")) {
            return new GoogleUserInfo(attributes);
        } else if (registrationId.equals("kakao")) {
            return new KakaoUserInfo(attributes);
        } else if (registrationId.equals("naver")) {
            return new NaverUserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 로그인 서비스입니다: " + registrationId);
        }
    }
}
