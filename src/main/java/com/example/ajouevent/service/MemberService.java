package com.example.ajouevent.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import com.example.ajouevent.auth.JwtUtil;
import com.example.ajouevent.auth.OAuth;
import com.example.ajouevent.auth.OAuthDto;
import com.example.ajouevent.auth.UserInfoGetDto;
import com.example.ajouevent.discord.DiscordMessageProvider;
import com.example.ajouevent.domain.EmailCheck;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.*;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.EmailCheckRedisRepository;
import com.google.api.client.auth.oauth2.TokenResponse;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
	private final JavaMailSender javaMailSender;
	private final EmailCheckRedisRepository emailCheckRedisRepository;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	private static final String REDIS_HASH = "EmailCheck";
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
				.email(member.getEmail())
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

	public LoginResponse socialLogin (OAuthDto oAuthDto) throws GeneralSecurityException, IOException {
		TokenResponse googleToken = oAuth.requestGoogleAccessToken(oAuthDto);
		UserInfoGetDto userInfoGetDto = oAuth.printUserResource(googleToken);
		if (googleToken.getRefreshToken() != null)
			oAuth.addCalendarCredentials(googleToken, userInfoGetDto.getEmail());

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

	public boolean duplicateEmail (String email) {
		return !memberRepository.existsByEmail(email);
	}

	@Transactional
	public String EmailCheckRequest(String email) {
		String authCode = this.createCode();
		EmailCheck existingEmailCheck = emailCheckRedisRepository.findByEmail(email);
		if (existingEmailCheck != null) {
			// 이미 해당 이메일이 Redis에 저장되어 있는 경우
			existingEmailCheck.setCode(authCode);
			emailCheckRedisRepository.save(existingEmailCheck);
		} else {
			EmailCheck emailCheck = new EmailCheck(email, authCode);
			emailCheck.setId(UUID.randomUUID().toString());

			emailCheckRedisRepository.save(emailCheck);
		}

		try {
			SMTPMsgDto smtpMsgDto = SMTPMsgDto.builder()
					.address(email)
					.title(email + "님의 [ajouevent] 이메일 인증 안내 이메일 입니다.")
					.message("안녕하세요. [ajouevent] 이메일 인증 안내 관련 이메일 입니다. \n" + "[" + email + "]" + "님의 코드는 "
							+ authCode + " 입니다.").build();
			SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
			simpleMailMessage.setTo(smtpMsgDto.getAddress());
			simpleMailMessage.setSubject(smtpMsgDto.getTitle());
			simpleMailMessage.setText(smtpMsgDto.getMessage());
			javaMailSender.send(simpleMailMessage);
		} catch (Exception exception) {
			log.error("이메일 인증 ::{} ", exception.getMessage());
			throw new CustomException(CustomErrorCode.EMAIL_CHECK_FAILED);
		}
		return "이메일 전송 완료";
	}

	private String createCode() {
		int lenth = 6;
		try {
			Random random = SecureRandom.getInstanceStrong();
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < lenth; i++) {
				builder.append(random.nextInt(10));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			log.debug("MemberService.createCode() exception occur");
			throw new CustomException(CustomErrorCode.NO_SUCH_ALGORITHM);
		}
	}

	public String EmailCheck(String email, String code) {
		EmailCheck emailCheck = emailCheckRedisRepository.findByEmail(email);
		if (emailCheck == null) throw new CustomException(CustomErrorCode.USER_NOT_FOUND);
		if (!Objects.equals(emailCheck.getCode(), code))
			throw new CustomException(CustomErrorCode.CODE_FAILED);
		return "인증 성공";
	}

}
