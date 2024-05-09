package com.example.ajouevent.service;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.example.ajouevent.auth.JwtUtil;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.dto.*;
import jakarta.validation.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
	private final MemberRepository memberRepository;
	private final TokenRepository tokenRepository;
	private final PasswordEncoder encoder;
	private final JwtUtil jwtUtil;
	private final BCryptPasswordEncoder BCryptEncoder;

	@Transactional
	public String register(RegisterRequest registerRequest) throws IOException {
		Optional<Member> member = memberRepository.findByEmail(registerRequest.getEmail());
		if (member.isPresent()) {
			throw new IOException("This member email is already exist." + registerRequest.getEmail());
		}

		String password = BCryptEncoder.encode(registerRequest.getPassword());

		Member newMember = Member.builder()
				.email(registerRequest.getEmail())
				.major(registerRequest.getMajor())
				.phone(registerRequest.getPhone())
				.name(registerRequest.getName())
				.password(password)
				.build();

		memberRepository.save(newMember);

		return "가입 완료"; // -> 수정 필요
	}

	@Transactional
	public ResponseEntity<LoginResponse> login(MemberDto.LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("이메일이 존재하지 않습니다."));

		if (!encoder.matches(password, member.getPassword())) {
			throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
		}

		MemberDto.MemberInfoDto memberInfoDto = MemberDto.MemberInfoDto.builder()
				.memberId(member.getId())
				.email(member.getEmail())
				.password(member.getPassword())
				.role(member.getRole())
				.build();

		String accessToken = jwtUtil.createAccessToken(memberInfoDto);
		String refreshToken = jwtUtil.createRefreshToken(memberInfoDto);


		// member = memberRepository.findByEmail(loginRequest.getEmail()).orElseThrow(NoSuchElementException::new);


		// 로그인을 하고 토큰이 이미 존재한다면, token의 만료 기간을 현재 날짜에서 2달 늘려줌
		// Optional<Token> existingToken = tokenRepository.findByTokenValueAndMember(loginRequest.getFcmToken(), member);
		// if (existingToken.isPresent()) {
		// 	Token token = existingToken.get();
		// 	log.info("이미 존재하는 토큰: " + existingToken.get().getTokenValue());
		// 	token.setExpirationDate(LocalDate.now().plusMonths(2));
		// 	tokenRepository.save(token);
		// }

		LoginResponse loginResponse = LoginResponse.builder()
				.id(member.getId())
				.grantType("Authorization")
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.name(member.getName())
				.major(member.getMajor())
				.build();

		return ResponseEntity.ok().body(loginResponse);
	}


	public LoginResponse reissueAccessToken(ReissueTokenDto refreshToken) {
		String token = refreshToken.getRefreshToken();
		if (!jwtUtil.validateToken(token)) {
			throw new ValidationException("refresh token이 유효하지 않습니다.");
		}
		Long memberId = jwtUtil.getUserId(token);

		Member member = memberRepository.findById(memberId).orElseThrow();

			MemberDto.MemberInfoDto memberInfoDto = MemberDto.MemberInfoDto.builder()
				.memberId(member.getId())
				.email(member.getEmail())
				.password(member.getPassword())
				.role(member.getRole())
				.build();

		String accessToken = jwtUtil.createAccessToken(memberInfoDto);

		return LoginResponse.builder()
				.id(member.getId())
				.grantType("Authorization")
				.accessToken(accessToken)
				.refreshToken(token)
				.build();
	}

	public MemberGetDto getMemberInfo(Principal principal) {
		Member member = memberRepository.findByEmail(principal.getName()).orElseThrow();

		return MemberGetDto.builder()
				.name(member.getName())
				.email(member.getEmail())
				.major(member.getMajor())
				.phone(member.getPhone())
				.build();
	}
}
