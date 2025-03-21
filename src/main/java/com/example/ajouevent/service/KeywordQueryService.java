package com.example.ajouevent.service;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.dto.KeywordResponse;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordQueryService {
	private final MemberRepository memberRepository;
	private final KeywordMemberRepository keywordMemberRepository;

	@Transactional(readOnly = true)
	public List<KeywordResponse> getUserKeyword(Principal principal) {
		String memberEmail = principal.getName();
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<KeywordMember> keywordMembers = keywordMemberRepository.findByMemberWithKeywordAndTopic(member);

		return keywordMembers.stream()
			.map(km -> KeywordResponse.builder()
				.encodedKeyword(km.getKeyword().getEncodedKeyword())
				.koreanKeyword(km.getKeyword().getKoreanKeyword())
				.searchKeyword(km.getKeyword().getSearchKeyword())
				.topicName(km.getKeyword().getTopic().getKoreanTopic())
				.isRead(km.isRead())
				.lastReadAt(km.getLastReadAt())
				.build())
			.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Keyword> getSubscribedKeywords(Member member) {
		return keywordMemberRepository.findByMemberWithKeyword(member).stream()
			.map(KeywordMember::getKeyword)
			.distinct()
			.toList();
	}

}