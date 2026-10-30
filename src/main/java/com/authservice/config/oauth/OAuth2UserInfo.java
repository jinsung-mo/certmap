package com.authservice.config.oauth;

import java.util.Map;

public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public abstract String getProviderId();
    public abstract String getProvider();
    public abstract String getEmail();
    public abstract String getName();

    // 소셜 제공자가 이메일 소유를 확인했는지. 확인되지 않은 이메일로는 기존 계정과 연결하지 않는다.
    public abstract boolean isEmailVerified();
}
