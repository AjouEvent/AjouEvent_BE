package com.example.ajouevent.auth;

import lombok.Data;

@Data
public class OAuthDto {
    private String authorizationCode;
    private String fcmToken;
}
