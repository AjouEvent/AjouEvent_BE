package com.example.ajouevent.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.security.auth.login.LoginException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


@Component
@Slf4j
public class OAuth {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;


    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.authorization-grant-type}")
    private String grantType;


    public TokenResponse requestGoogleAccessToken(OAuthDto oAuthDto) throws LoginException {
        String code = oAuthDto.getAuthorizationCode();

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty");
        }
        String url = "https://oauth2.googleapis.com/token";
        String decode = URLDecoder.decode(code, StandardCharsets.UTF_8);

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("code", decode);
        parameters.add("grant_type", grantType);
        parameters.add("redirect_uri", oAuthDto.getRedirectUri());


        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, headers);

        // RestTemplate 객체 생성
        RestTemplate restTemplate = new RestTemplate();

        // POST 요청 보내기
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
// 응답 본문 체크
        if (responseEntity.getBody() == null) {
            throw new LoginException("Response body is null");
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken(rootNode.get("access_token").asText());
            tokenResponse.setExpiresInSeconds(Long.valueOf(rootNode.get("expires_in").asText()));
            tokenResponse.setTokenType(rootNode.get("token_type").asText());

            if (rootNode.has("refresh_token") && rootNode.get("refresh_token").asText() != null)
                tokenResponse.setRefreshToken(rootNode.get("refresh_token").asText());
            else
                tokenResponse.setRefreshToken(null);

            tokenResponse.setScope(rootNode.get("scope").asText());

            return tokenResponse;
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }
        return null;

    }

    public UserInfoGetDto printUserResource(TokenResponse googleToken) {
        String GOOGLE_USERINFO_REQUEST_URL = "https://www.googleapis.com/oauth2/v1/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + googleToken.getAccessToken());
        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                GOOGLE_USERINFO_REQUEST_URL,
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode responseBody = response.getBody();

            if (responseBody == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Response body is null");
            }

            if (responseBody.has("email")) {
                return UserInfoGetDto.builder()
                    .id(responseBody.get("id").asText())
                    .email(responseBody.get("email").asText())
                    .verifiedEmail(responseBody.get("verified_email").asBoolean())
                    .name(responseBody.get("name").asText())
                    .givenName(responseBody.get("given_name").asText())
                    .familyName(responseBody.get("family_name").asText())
                    .picture(responseBody.get("picture").asText())
                    .hd(responseBody.get("hd").asText())
                    .build();
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 이메일을 찾을 수 없습니다.");
            }
        } else {
            System.err.println("Failed to fetch user resource: " + response.getStatusCode());
        }
        return null;
    }



}
