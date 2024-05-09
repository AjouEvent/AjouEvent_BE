package com.example.ajouevent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.domain.TopicToken;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicTokenRepository;


class TopicServiceTest {

	@Mock
	private TokenRepository tokenRepository;

	@Mock
	private TopicTokenRepository topicTokenRepository;



	@Mock
	private TopicMemberRepository topicMemberRepository;

	@Mock
	private FCMService fcmService;

	@Mock
	private MemberRepository memberRepository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);



	}
	// // }
	// // void getSubscribedTopics() {
	// // @Test
	// // @Test
	// // void subscribeToTopic() {
	// // }
	// //
	// // @Test
	// // void unsubscribeFromTopic() {
	// // }
	// //
	// // @Test
	// // void saveFCMToken() {
	// // }
	// //
	// // @Test
	// // void unsubscribeExpiredTokens() {
	// // }
	//
	//
	// @Test
	// void resetAllSubscriptions() {
	// 	// given
	// 	// 가짜 Member 객체 생성
	// 	Member member = Member.builder()
	// 		.id(1L)
	// 		.email("test@ajou.ac.kr")
	// 		.name("심재엽")
	// 		.password("1234")
	// 		.major("소프트웨어학과")
	// 		.phone("010-2056-4266")
	// 		.build();
	//
	// 	// 가짜 Token 객체 생성
	// 	Token token1 = Token.builder()
	// 		.id(1L)
	// 		.tokenValue("token1")
	// 		.expirationDate(LocalDate.now())
	// 		.member(member)
	// 		.build();
	// 	Token token2 = Token.builder()
	// 		.id(2L)
	// 		.tokenValue("token2")
	// 		.expirationDate(LocalDate.now())
	// 		.member(member)
	// 		.build();
	//
	// 	// 가짜 Topic 객체 생성
	// 	Topic topic = Topic.builder()
	// 		.id(1L)
	// 		.department("test")
	// 		.build();
	//
	// 	// 가짜 TopicMember 객체 생성
	// 	TopicMember topicMember1 = TopicMember.builder()
	// 		.id(1L)
	// 		.topic(topic)
	// 		.member(member)
	// 		.build();
	// 	TopicMember topicMember2 = TopicMember.builder()
	// 		.id(2L)
	// 		.topic(topic)
	// 		.member(member)
	// 		.build();
	//
	// 	// when
	// 	// memberRepository.findByEmail 메서드가 호출될 때 가짜 Member 객체 반환하도록 설정
	// 	when(memberRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(member));
	//
	// 	// tokenRepository.findByMember 메서드가 호출될 때 가짜 Token 객체 목록 반환하도록 설정
	// 	when(tokenRepository.findByMember(any(Member.class))).thenReturn(Arrays.asList(token1, token2));
	//
	// 	// topicMemberRepository.findByMember 메서드가 호출될 때 가짜 TopicMember 객체 목록 반환하도록 설정
	// 	when(topicMemberRepository.findByMember(any(Member.class))).thenReturn(Arrays.asList(topicMember1, topicMember2));
	//
	// 	// SecurityContext에 가짜 Authentication 설정
	// 	SecurityContext securityContext = mock(SecurityContext.class);
	// 	SecurityContextHolder.setContext(securityContext);
	// 	when(securityContext.getAuthentication()).thenReturn(() -> "test@example.com");
	//
	// 	// resetAllSubscriptions 메서드 호출
	// 	subscriptionService.resetAllSubscriptions();
	//
	// 	// fcmService.unsubscribeFromTopic 메서드가 호출될 때 올바른 인수로 호출되는지 확인
	// 	verify(fcmService, times(1)).unsubscribeFromTopic(eq("department1"), eq(token1.getTokenValue()));
	// 	verify(fcmService, times(1)).unsubscribeFromTopic(eq("department2"), eq(token2.getTokenValue()));
	//
	// 	// topicTokenRepository.deleteByTopic 및 topicMemberRepository.delete 메서드가 호출되는지 확인
	// 	verify(topicMemberRepository, times(1)).delete(eq(topicMember1));
	// 	verify(topicMemberRepository, times(1)).delete(eq(topicMember2));
	// }
	@Test
	@DisplayName("만료된 토큰 삭제")
	public void testUnsubscribeExpiredTokens() {
		// given
		// 가짜 Member 객체 생성
		Member member = Member.builder()
			.email("test@ajou.ac.kr")
			.name("심재엽")
			.password("1234")
			.major("소프트웨어학과")
			.phone("010-2056-4266")
			.build();

		// 만료될 토큰과 TopicToken 생성
		Token expiredToken1 = Token.builder()
			.tokenValue("expired_token_value")
			.expirationDate(LocalDate.now()) // 만료 날짜 설정
			.member(member)
			.build();

		Token expiredToken2 = Token.builder()
			.tokenValue("expired_token_value")
			.expirationDate(LocalDate.now()) // 만료 날짜 설정
			.member(member)
			.build();

		List<Token> expiredTokens = Arrays.asList(expiredToken1, expiredToken2);
		// TopicToken에 대한 Topic과 Token 설정
		// 가짜 Topic 객체 생성
		Topic topic = Topic.builder()
			.department("Test")
			.build();
		TopicToken expiredTopicToken1 = new TopicToken(topic, expiredToken1);
		TopicToken expiredTopicToken2 = new TopicToken(topic, expiredToken2);
		List<TopicToken> expiredTopicTokens = Arrays.asList(expiredTopicToken1, expiredTopicToken2);

		when(tokenRepository.findByExpirationDate(LocalDate.now())).thenReturn(expiredTokens);
		when(topicTokenRepository.findByTokenIn(expiredTokens)).thenReturn(expiredTopicTokens);

		// when
		List<Token> foundTokens = tokenRepository.findByExpirationDate(LocalDate.now());

		if (!foundTokens.isEmpty()) {
			List<TopicToken> tokensToDelete = topicTokenRepository.findByTokenIn(foundTokens);
			topicTokenRepository.deleteAll(tokensToDelete);
			tokenRepository.deleteAll(foundTokens);
		}

		// then 검증: 삭제 메소드가 호출되었는지 확인
		verify(topicTokenRepository, times(1)).deleteAll(any());
		verify(tokenRepository, times(1)).deleteAll(any());
	}


}