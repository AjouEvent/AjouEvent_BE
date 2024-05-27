package com.example.ajouevent.service;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import com.example.ajouevent.auth.JwtUtil;
import com.example.ajouevent.auth.OAuth;
import com.example.ajouevent.auth.OAuthDto;
import com.example.ajouevent.auth.UserInfoGetDto;
import com.example.ajouevent.discord.DiscordMessageProvider;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.*;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import javax.security.auth.login.LoginException;


@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
	private final MemberRepository memberRepository;
	private final TokenRepository tokenRepository;
	private final PasswordEncoder encoder;
	private final JwtUtil jwtUtil;
	private final BCryptPasswordEncoder BCryptEncoder;
	private final OAuth oAuth;
	private final TopicService topicService;
	private final DiscordMessageProvider discordMessageProvider;

	@Transactional
	public String register(RegisterRequest registerRequest) throws IOException {
		Optional<Member> member = memberRepository.findByEmail(registerRequest.getEmail());
		if (member.isPresent()) {
			throw new CustomException(CustomErrorCode.DUPLICATED_EMAIL);
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

		// 회원가입이 완료되면 트리거 된다.
		String registerMessage = newMember.getId() + "번째 유저" + registerRequest.getName() + " 님이 회원가입했습니다!\n";
		discordMessageProvider.sendMessage(registerMessage);
		return "가입 완료"; // -> 수정 필요
	}

	@Transactional
	public ResponseEntity<LoginResponse> login(MemberDto.LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.LOGIN_FAILED));

		if (!encoder.matches(password, member.getPassword())) {
			throw new CustomException(CustomErrorCode.LOGIN_FAILED);
		}

		MemberDto.MemberInfoDto memberInfoDto = MemberDto.MemberInfoDto.builder()
				.memberId(member.getId())
				.email(member.getEmail())
				.password(member.getPassword())
				.role(member.getRole())
				.build();

		String accessToken = jwtUtil.createAccessToken(memberInfoDto);
		String refreshToken = jwtUtil.createRefreshToken(memberInfoDto);

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
			throw new CustomException(CustomErrorCode.UNAUTHORIZED);
		}

		Long memberId = jwtUtil.getUserId(token);

		Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

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
		Member member = memberRepository.findByEmail(principal.getName()).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		return MemberGetDto.builder()
				.name(member.getName())
				.email(member.getEmail())
				.major(member.getMajor())
				.phone(member.getPhone())
				.build();
	}

	@Transactional
	public String updateMemberInfo (MemberUpdateDto memberUpdateDto, Principal principal) {
		Member member = memberRepository.findByEmail(principal.getName()).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		if (memberUpdateDto.getMajor() != null) member.setMajor(memberUpdateDto.getMajor());
		if (memberUpdateDto.getName() != null) member.setName(memberUpdateDto.getName());
		if (memberUpdateDto.getPhone() != null) member.setPhone(memberUpdateDto.getPhone());
		memberRepository.save(member);
		return "수정 완료";
	}

	@Transactional
	public String deleteMember (Principal principal) {
		Member member = memberRepository.findByEmail(principal.getName()).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
		memberRepository.delete(member);
		return "삭제 완료";
	}

	public LoginResponse socialLogin (OAuthDto oAuthDto) throws LoginException {
		String googleAccessToken = oAuth.requestGoogleAccessToken(oAuthDto.getAuthorizationCode());
		UserInfoGetDto userInfoGetDto = oAuth.printUserResource(googleAccessToken);
		Member member = memberRepository.findByEmail(userInfoGetDto.getEmail()).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		MemberDto.MemberInfoDto memberInfoDto = MemberDto.MemberInfoDto.builder()
				.memberId(member.getId())
				.email(member.getEmail())
				.password(member.getPassword())
				.role(member.getRole())
				.build();

		String accessToken = jwtUtil.createAccessToken(memberInfoDto);
		String refreshToken = jwtUtil.createRefreshToken(memberInfoDto);

		MemberDto.LoginRequest loginRequest = MemberDto.LoginRequest.builder()
				.email(member.getEmail())
				.password(member.getPassword())
				.fcmToken(oAuthDto.getFcmToken())
				.build();

		if (loginRequest.getFcmToken() != null) {
			topicService.saveFCMToken(loginRequest);
		} else {
			log.info("가져온 LoginRequest의 FcmToken이 null 입니다.");
		}

        return LoginResponse.builder()
				.id(member.getId())
				.grantType("Authorization")
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.name(member.getName())
				.major(member.getMajor())
				.email(member.getEmail())
				.build();
	}


}
