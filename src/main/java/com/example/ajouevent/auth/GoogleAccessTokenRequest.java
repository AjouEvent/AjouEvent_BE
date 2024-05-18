package com.example.ajouevent.auth;

import lombok.Data;

@Data
public class GoogleAccessTokenRequest {
    private String code;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String grantType;

    public GoogleAccessTokenRequest(String decodedCode, String clientId, String clientSecret, String redirectUri, String grantType) {
        this.code = decodedCode;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.grantType = grantType;
    }
}
