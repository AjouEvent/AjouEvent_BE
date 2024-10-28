package com.example.ajouevent.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.example.ajouevent.domain.EventLike;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.repository.EventLikeRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.util.JwtUtil;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ajouevent.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
	private final MemberRepository memberRepository;
	private final PasswordEncoder encoder;
	private final JwtUtil jwtUtil;
	private final BCryptPasswordEncoder BCryptEncoder;
	private final OAuth oAuth;
	private final TopicService topicService;
	private final DiscordMessageProvider discordMessageProvider;
	private final JavaMailSender javaMailSender;
	private final EmailCheckRedisRepository emailCheckRedisRepository;
	private final RedisTemplate<String, EmailCheck> redisEmailCheckTemplate;

	private static final String REDIS_HASH = "EmailCheck";
	private final TokenRepository tokenRepository;
	private final EventLikeRepository eventLikeRepository;

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
				// .phone(registerRequest.getPhone())
				.name(registerRequest.getName())
				.password(password)
				.build();

		memberRepository.save(newMember);

		// 회원가입이 완료되면 트리거 된다.
		String registerMessage = newMember.getId() + "번째 유저 " + registerRequest.getName() + " 님이 회원가입했습니다!\n";
		discordMessageProvider.sendMessage(registerMessage);
		return "가입 완료"; // -> 수정 필요
	}

	@Transactional
	public RegisterResponse registerInfo(RegisterMemberInfoRequest registerMemberInfoRequest, Principal principal) throws IOException {
		Member member = memberRepository.findByEmail(principal.getName())
			.orElseThrow(() -> new CustomException(CustomErrorCode.LOGIN_FAILED));

		if (registerMemberInfoRequest.getMajor() != null) member.setMajor(registerMemberInfoRequest.getMajor());


		memberRepository.save(member);

		String registerMessage = member.getId() + "번째 유저 " + member.getName() + " 님이 회원가입했습니다!\n";
		discordMessageProvider.sendMessage(registerMessage);
		return RegisterResponse.builder()
			.email(member.getEmail())
			.name(member.getName())
			.build();
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

		// 해당 멤버의 찜한 게시글(EventLike) 목록 삭제
		List<EventLike> eventLikes = eventLikeRepository.findByMember(member);
		List<Long> eventLikeIds = eventLikes.stream()
			.map(EventLike::getEventLikeId)
			.toList();
		eventLikeRepository.deleteAllByIds(eventLikeIds);

		// 해당 멤버의 토큰 삭제
		List<Token> memberTokens = tokenRepository.findByMember(member);
		List<Long> memberTokensIds = memberTokens.stream()
			.map(Token::getId)
			.toList();
		tokenRepository.deleteAllByTokenIds(memberTokensIds);

		memberRepository.delete(member);
		return "삭제 완료";
	}

	public LoginResponse socialLogin (OAuthDto oAuthDto) throws GeneralSecurityException, IOException {
		UserInfoGetDto userInfoGetDto = connectCalendar(oAuthDto);

		Boolean isNewMember = false; // 새로 가입한 회원 여부

		// // 캘린더 연동을 선택한 경우
		// if (isCalendarLinked) {
		// 	userInfoGetDto = connectCalendar(oAuthDto);
		// } else {
		// 	// 캘린더 연동을 선택하지 않은 경우, 구글 로그인만 처리
		// 	TokenResponse googleToken = oAuth.requestGoogleAccessToken(oAuthDto);
		// 	userInfoGetDto = oAuth.printUserResource(googleToken);
		// }

		Optional<Member> memberOptional = memberRepository.findByEmail(userInfoGetDto.getEmail());

		Member member;

		if (memberOptional.isPresent()) {
			member = memberOptional.get();
		} else {
			member = Member.builder()
				.email(userInfoGetDto.getEmail())
				.name(userInfoGetDto.getName())
				.build();

			memberRepository.save(member);
			isNewMember = true; // 새로 가입한 회원으로 설정
		}


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
				.isNewMember(isNewMember)
				.build();
	}

	// 캘린더 연동
	public UserInfoGetDto connectCalendar(OAuthDto oAuthDto) throws GeneralSecurityException, IOException {
		TokenResponse googleToken = oAuth.requestGoogleAccessToken(oAuthDto);

		System.out.println("Access Token: " + googleToken.getAccessToken());
		System.out.println("Refresh Token: " + googleToken.getRefreshToken());
		System.out.println("Token Type: " + googleToken.getTokenType());
		System.out.println("Expires In: " + googleToken.getExpiresInSeconds());

		UserInfoGetDto userInfoGetDto = oAuth.printUserResource(googleToken);
		log.info("hello");
		if (googleToken.getRefreshToken() != null) {
			log.info("helloworld");
			CalendarService.getCredentials(googleToken, userInfoGetDto.getEmail());
		}
		return userInfoGetDto;
	}

	@Transactional
	public boolean duplicateEmail (String email) {
		return !memberRepository.existsByEmail(email);
	}

	@Transactional
	public boolean emailExists (String email) {
		return memberRepository.existsByEmail(email);
	}

	@Transactional
	public boolean accountExists(String email, String name) {
		return memberRepository.existsByEmailAndName(email, name);
	}

	@Transactional
	public boolean verifyCurrentPassword(CurrentPasswordDto currentPasswordDto, Principal principal) {
		Member member = memberRepository.findByEmail(principal.getName())
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		if (!BCryptEncoder.matches(currentPasswordDto.getCurrentPassword(), member.getPassword())) {
			throw new CustomException(CustomErrorCode.PASSWORD_FAILED);
		}

		return true; // 비밀번호가 일치하면 true 반환
	}

	public String EmailCheckRequest(String email) {
		String authCode = this.createCode();
		EmailCheck existingEmailCheck = emailCheckRedisRepository.findByEmail(email);
		if (existingEmailCheck != null) {
			// 이미 해당 이메일이 Redis에 저장되어 있는 경우
			existingEmailCheck.setCode(authCode);
			existingEmailCheck = emailCheckRedisRepository.save(existingEmailCheck);
		} else {
			EmailCheck emailCheck = new EmailCheck(email, authCode);
			emailCheck.setId(UUID.randomUUID().toString());

			existingEmailCheck = emailCheckRedisRepository.save(emailCheck);
		}

		redisEmailCheckTemplate.expire(REDIS_HASH, 300, TimeUnit.SECONDS);
		redisEmailCheckTemplate.expire(REDIS_HASH + ":" + existingEmailCheck.getId(), 300, TimeUnit.SECONDS);
		redisEmailCheckTemplate.expire(REDIS_HASH + ":email:" + email, 300, TimeUnit.SECONDS);
		redisEmailCheckTemplate.expire(REDIS_HASH + ":" + existingEmailCheck.getId() + ":idx", 300, TimeUnit.SECONDS);

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


	@Transactional
	public String changePassword (PasswordDto passwordDto, Principal principal) {
		Member member = memberRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		if (!BCryptEncoder.matches(passwordDto.getPassword(), member.getPassword())) {
			throw new CustomException(CustomErrorCode.PASSWORD_FAILED);
		}

		String newPassword = BCryptEncoder.encode(passwordDto.getNewPassword());
		member.setPassword(newPassword);
		memberRepository.save(member);
		return "비밀번호 변경 완료";
	}

	@Transactional
	public String resetPassword(ResetPasswordDto resetPasswordDto) {
		Member member = memberRepository.findByEmail(resetPasswordDto.getEmail())
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		String newPassword = BCryptEncoder.encode(resetPasswordDto.getNewPassword());
		member.setPassword(newPassword);
		memberRepository.save(member);
		return "비밀번호 재설정 완료";
	}

	@Transactional
	public String reissuePassword(ReissuePasswordDto reissuePasswordDto) throws Exception {
		String newPw = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		Member member = memberRepository.findMemberByEmailAndPhone(reissuePasswordDto.getEmail(), reissuePasswordDto.getPhone());
		if (member == null)
			throw new CustomException(CustomErrorCode.EVENT_NOT_FOUND);

		member.setPassword(BCryptEncoder.encode(newPw));

		try {
			SMTPMsgDto smtpMsgDto = SMTPMsgDto.builder()
					.address(member.getEmail())
					.title(member.getName() + "님의 [ajou event] 임시비밀번호 안내 이메일 입니다.")
					.message("안녕하세요. [ajou event] 임시 비밀번호 안내 관련 이메일 입니다. \n" + "[" + member.getName() + "]" + "님의 임시 비밀번호는 "
							+ newPw + " 입니다.").build();
			SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
			simpleMailMessage.setTo(smtpMsgDto.getAddress());
			simpleMailMessage.setSubject(smtpMsgDto.getTitle());
			simpleMailMessage.setText(smtpMsgDto.getMessage());
			javaMailSender.send(simpleMailMessage);
		} catch (Exception exception) {
			log.error("PW Reissue ::{} ", exception.getMessage());
			throw new CustomException(CustomErrorCode.REISSUE_PASSWORD_FAILED);
		}
		return "비밀번호 재설정 완료";
	}

}
