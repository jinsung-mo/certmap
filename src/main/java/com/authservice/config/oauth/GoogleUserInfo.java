package com.authservice.config.oauth;

import java.util.Map;

public class GoogleUserInfo extends OAuth2UserInfo {

    public GoogleUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    // Google userinfo의 email_verified가 true일 때만 이메일 소유가 확인된 것으로 본다
    @Override
    public boolean isEmailVerified() {
        Object verified = attributes.get("email_verified");
        return Boolean.TRUE.equals(verified) || "true".equals(verified);
    }
}
