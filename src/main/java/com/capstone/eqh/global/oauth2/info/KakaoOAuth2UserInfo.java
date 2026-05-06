package com.capstone.eqh.global.oauth2.info;

import java.util.Map;

/**
 * 카카오 OpenID Connect claims 파싱.
 * OIDC 표준 claims(sub, nickname)를 사용하므로
 * 비즈앱 전환 없이도 유저 식별이 가능하다.
 */
public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return String.valueOf(attributes.get("sub"));
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("nickname");
    }
}
