package com.example.ajouevent.auth;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.Map;

public class OAuth2UserRequest {

    private final OAuth2AccessToken accessToken;

    private final Map<String, Object> additionalParameters;

    public OAuth2UserRequest(OAuth2AccessToken accessToken, Map<String, Object> additionalParameters) {
        this.accessToken = accessToken;
        this.additionalParameters = additionalParameters;
    }

}
