package com.example.ajouevent.service;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.EventLike;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.EventLikeRepository;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventLikeService {
	private final EventLikeRepository eventLikeRepository;
	private final EventRepository eventRepository;
	private final MemberRepository memberRepository;

	// 게시글 찜하기
	@Transactional
	public ResponseEntity<ResponseDto> likeEvent(Long eventId, Principal principal) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null || SecurityContextHolder.getContext().getAuthentication() == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName(); // 현재 로그인한 사용자의 이메일 가져오기

		// 이벤트 조회
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		// 사용자 조회
		Member member = memberRepository.findByEmail(userEmail).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 이미 찜한 이벤튼지 확인
		if (eventLikeRepository.existsByMemberAndClubEvent(member, clubEvent)) {
			return ResponseEntity.ok().body(ResponseDto.builder()
				.successStatus(HttpStatus.OK)
				.successContent("이미 찜한 이벤트입니다.")
				.build()
			);
		}

		// 이벤트를 사용자의 찜 목록에 추가
		EventLike eventLike = EventLike.builder()
			.clubEvent(clubEvent)
			.member(member)
			.build();

		// 게시글의 좋아요 수 증가
		clubEvent.incrementLikes();

		eventLikeRepository.save(eventLike);

		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.CREATED)
			.successContent("게시글을 찜했습니다.")
			.build()
		);

	}

	// 게시글 찜 취소 하기
	@Transactional
	public ResponseEntity<ResponseDto> cancelLikeEvent(Long eventId, Principal principal) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName(); // 현재 로그인한 사용자의 이메일 가져오기

		// 이벤트 조회
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		// 사용자 조회
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 찜한 이벤트인지 확인
		EventLike eventLike = eventLikeRepository.findByClubEventAndMember(clubEvent, member).orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_LIKED));
		if (eventLike == null) {
			throw new CustomException(CustomErrorCode.EVENT_NOT_LIKED);
		}

		// 게시글의 저장수 감소
		clubEvent.decreaseLikes();

		// 이벤트 찜 취소
		eventLikeRepository.delete(eventLike);

		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글 찜하기를 취소했습니다.")
			.build()
		);
	}

	@Transactional
	public void deleteAllLikesByMember(Member member) {
		List<EventLike> eventLikes = eventLikeRepository.findByMember(member);
		List<Long> eventLikeIds = eventLikes.stream()
			.map(EventLike::getEventLikeId)
			.toList();

		if (!eventLikeIds.isEmpty()) {
			eventLikeRepository.deleteAllByIds(eventLikeIds);
		}
	}

	// 사용자가 찜한 이벤트 ID 목록 조회
	@Transactional(readOnly = true)
	public List<Long> getLikedEventIds(Member member) {
		return eventLikeRepository.findByMemberWithClubEvent(member).stream()
			.map(eventLike -> eventLike.getClubEvent().getEventId())
			.collect(Collectors.toList());
	}

	// 사용자의 찜한 이벤트를 Map<Long, Boolean> 형태로 변환
	@Transactional(readOnly = true)
	public Map<Long, Boolean> getLikedEventMap(Member member) {
		List<Long> likedEventIds = getLikedEventIds(member);
		return likedEventIds.stream().collect(Collectors.toMap(id -> id, id -> true));
	}

	// 사용자의 찜한 이벤트 목록을 반환 (이벤트 전체 객체)
	@Transactional(readOnly = true)
	public List<EventLike> getLikedEvents(Member member) {
		return eventLikeRepository.findByMemberWithClubEvent(member);
	}
}