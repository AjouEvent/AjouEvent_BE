package com.example.ajouevent.auth;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.MemberDto;
import com.example.ajouevent.repository.MemberRepository;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // OAuth2User로 캐스팅하여 인증된 사용자 정보를 가져온다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        // 사용자 이메일을 가져온다.
        String email = oAuth2User.getAttribute("email");

        // CustomOAuth2UserService에서 셋팅한 로그인한 회원 존재 여부를 가져온다.
        boolean isExist = oAuth2User.getAttribute("exist");
        // OAuth2User로 부터 Role을 얻어온다.
        String role = oAuth2User.getAuthorities().stream()
                .findFirst() // 첫번째 Role을 찾아온다.
                .orElseThrow(IllegalAccessError::new) // 존재하지 않을 시 예외를 던진다.
                .getAuthority(); // Role을 가져온다.

        // 회원이 존재할경우
        if (isExist) {
            // 회원이 존재하면 jwt token 발행을 시작한다.
            Member member = memberRepository.findByEmail(email).orElseThrow();

            MemberDto.MemberInfoDto Dto = MemberDto.MemberInfoDto.builder().memberId(member.getId()).email(email).role(role).build();
            String accessToken = jwtUtil.createAccessToken(Dto);
            String refreshToken = jwtUtil.createRefreshToken(Dto);
            log.info(String.valueOf(Dto));

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("accessToken", accessToken);
            jsonResponse.addProperty("refreshToken", refreshToken);
            jsonResponse.addProperty("id", Dto.getMemberId());
            jsonResponse.addProperty("email", Dto.getEmail());
            jsonResponse.addProperty("name", member.getName());
            jsonResponse.addProperty("major", member.getMajor());
            PrintWriter out = response.getWriter();
            out.print(jsonResponse.toString());
            out.flush();

        } else {

            // 회원이 존재하지 않을경우, email을 쿼리스트링으로 전달하는 url을 만들어준다.
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/register.html")
                    .queryParam("email", (String) oAuth2User.getAttribute("email"))
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
            // 회원가입 페이지로 리다이렉트 시킨다.
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }

}