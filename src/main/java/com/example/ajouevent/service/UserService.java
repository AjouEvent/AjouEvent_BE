package com.example.ajouevent.service;

import java.io.IOException;
import java.util.Optional;

import com.example.ajouevent.auth.JwtUtil;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.*;
import jakarta.validation.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final JwtUtil jwtUtil;
	private final BCryptPasswordEncoder BCryptEncoder;

	@Transactional
	public String register(RegisterRequest registerRequest) throws IOException {

		Optional<Member> member = userRepository.findByEmail(registerRequest.getEmail());
		if (member.isPresent()) {
			throw new IOException("This member email is already exist." + registerRequest.getEmail());
		}

		String password = BCryptEncoder.encode(registerRequest.getPassword());

		Member newMember = Member.builder()
				.email(registerRequest.getEmail())
				.major(registerRequest.getMajor())
				.phone(registerRequest.getPhone())
				.password(password)
				.build();

		userRepository.save(newMember);

		return "가입 완료"; // -> 수정 필요
	}

	@Transactional
	public ResponseEntity<LoginResponse> login(UserDTO.LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();
		Optional<Member> optionalMember = userRepository.findByEmail(email);

		if (optionalMember.isEmpty()) {
			throw new UsernameNotFoundException("이메일이 존재하지 않습니다.");
		}


		Member member = optionalMember.get();
		if (!encoder.matches(password, member.getPassword())) {
			throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
		}

		UserDTO.UserInfoDto userInfoDto = UserDTO.UserInfoDto.builder()
				.memberId(member.getId())
				.email(member.getEmail())
				.password(member.getPassword())
				.role(member.getRole())
				.build();

		String accessToken = jwtUtil.createAccessToken(userInfoDto);
		String refreshToken = jwtUtil.createRefreshToken(userInfoDto);

		LoginResponse loginResponse = LoginResponse.builder()
				.grantType("Authorization")
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();

		return ResponseEntity.ok().body(loginResponse);
	}

	public LoginResponse reissueAccessToken(ReissueTokenDto refreshToken) {
		String token = refreshToken.getRefreshToken();
		if (!jwtUtil.validateToken(token)) {
			throw new ValidationException("refresh token이 유효하지 않습니다.");
		}
		Long memberId = jwtUtil.getUserId(token);

		Member member = userRepository.findById(memberId).orElseThrow();

		UserDTO.UserInfoDto userInfoDto = UserDTO.UserInfoDto.builder()
				.memberId(member.getId())
				.email(member.getEmail())
				.password(member.getPassword())
				.role(member.getRole())
				.build();

		String accessToken = jwtUtil.createAccessToken(userInfoDto);

		return LoginResponse.builder()
				.grantType("Authorization")
				.accessToken(accessToken)
				.refreshToken(token)
				.build();
	}
}
