package com.example.ajouevent.auth;

import lombok.Data;

@Data
public class GoogleAccessTokenResponse {
    private String accessToken;
    private String expiresIn;
    private String scope;
    private String tokenType;
    private String idToken;
}
