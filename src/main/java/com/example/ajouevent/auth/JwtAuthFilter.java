package com.example.ajouevent.auth;

import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter { // 한 번 실행 보장
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtil jwtUtil;

    // JWT 토큰 검증 필 수행
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        // JWT가 헤더에 있는 경우
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            // JWT 유효성 검증
            try {
                // JWT 유효성 검증
                jwtUtil.validateToken(token);
                    Long userId = jwtUtil.getUserId(token);

                    // 유저와 토큰 일치 시 userDetails 생성
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(userId.toString());

                    if (userDetails != null) {
                        // UserDetails, Password, Role -> 접근 권한 인증 token 생성
                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        // 현재 Request의 Security Context에 접근권한 설정
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    }
            } catch (Exception e) {
                // 토큰이 유효하지 않거나 만료된 경우
                if (e instanceof CustomException) {// 정의한 error에 속할 경우
                    CustomException customException = (CustomException) e;
                    handleAuthenticationException(response, customException.getMessage());
                }else{
                    handleAuthenticationException(response, "internal server error"); // 전체 internal로 처리
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void handleAuthenticationException(HttpServletResponse response,String message ) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "FAILED");
        responseBody.put("message", message);

        PrintWriter writer = response.getWriter();
        writer.write(new ObjectMapper().writeValueAsString(responseBody));
        writer.flush();
        writer.close();
    }
}
