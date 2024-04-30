package com.example.ajouevent.config;

import com.example.ajouevent.auth.*;
import com.example.ajouevent.auth.CustomOAuth2UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class WebSecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtil jwtUtil;
    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final MyAuthenticationSuccessHandler myAuthenticationSuccessHandler;
    private final MyAuthenticationFailureHandler myAuthenticationFailureHandler;

    private static final String[] AUTH_WHITELIST = {
            "swagger-ui/**", "/users/register", "/users/login", "/users/reissueToken",  // 여기에 포함되지 않은 모든 요청은 인증 필요
            "/send/registeration-token", "/set", "/get/{key}", "login",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF, CORS
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(withDefaults());

        http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
        ));
        // formLogin, basicHttp 비활성화
        http.formLogin(withDefaults());
        http.httpBasic(AbstractHttpConfigurer::disable);

        // JwtAuthFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
        http.addFilterBefore(new JwtAuthFilter(customUserDetailsService, jwtUtil), UsernamePasswordAuthenticationFilter.class);

        http.exceptionHandling((exceptionHandling) -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );

        // 권한 규칙 작성
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(AUTH_WHITELIST).permitAll()
                //@PreAuthorization을 사용하는 경우 모든 경로에 대한 인증 처리는 permit
                .anyRequest().permitAll()
                )
                .oauth2Login(
                        oauth2 -> oauth2
                                .userInfoEndpoint(
                                        endpoint -> endpoint
                                                .userService(customOAuth2UserService)
                                )

                                .failureHandler(myAuthenticationFailureHandler)  // OAuth2 로그인 실패 시 처리할 핸들러 지정
                                .successHandler(myAuthenticationSuccessHandler) // OAuth2 로그인 성공 시 처리할 핸들러 지정

                );
        return http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class).build();
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
