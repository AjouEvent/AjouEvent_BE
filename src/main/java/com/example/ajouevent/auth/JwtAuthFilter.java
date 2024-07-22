package com.example.ajouevent.auth;

import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.util.JwtUtil;

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

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter { // 한 번 실행 보장
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtil jwtUtil;

    // JWT 토큰 검증 필 수행
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");

            // JWT가 헤더에 있는 경우
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);

                // JWT 유효성 검증
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserId(token);

                    // 유저와 토큰 일치 시 userDetails 생성
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(userId.toString());

                    if (userDetails != null) {
                        // UserDetails, Password, Role -> 접근 권한 인증 token 생성
                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        // 현재 Request의 Security Context에 접근권한 설정
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    }
                }
            }

            filterChain.doFilter(request, response);
    } catch (CustomException ex) {
        SecurityContextHolder.clearContext();
        request.setAttribute("exception", ex);
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
    } catch (Exception ex) {
        throw new ServletException("Authentication error", ex);
    }
    }

}
