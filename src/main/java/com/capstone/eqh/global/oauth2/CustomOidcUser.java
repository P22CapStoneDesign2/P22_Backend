package com.capstone.eqh.global.oauth2;

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DB 유저 정보(dbUserId, dbUserRole)를
 * OIDC claims 위에 덧붙여 SuccessHandler에 전달하는 래퍼.
 */
public class CustomOidcUser extends DefaultOidcUser {

    private final Map<String, Object> mergedAttributes;

    public CustomOidcUser(OidcUser delegate, Map<String, Object> additionalAttributes) {
        super(delegate.getAuthorities(), delegate.getIdToken(), delegate.getUserInfo());
        Map<String, Object> merged = new HashMap<>(super.getAttributes());
        merged.putAll(additionalAttributes);
        this.mergedAttributes = Collections.unmodifiableMap(merged);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return mergedAttributes;
    }
}
