package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.example.ajouevent.dto.UpdateEventRequest;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClubEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column
    private String title;

    @Column(length = 50000)
    private String content;

    @Column // 게시글 작성자(작성 기관)
    private String writer;

    @Column // 게시글 생성 시간
    private LocalDateTime createdAt;

    @Column // 게시글 분류(topic) - 아주대학교 - 일반, 소프트웨어학과, 동아리
    private String subject;

    @Column // 원래 공지사항 url
    private String url;

    @Column // 찜한 수 (default는 0)
    private Long likesCount;

    @Column // 조회 수 (default는 0)
    private Long viewCount;

    @Column(length = 50000)
    @Enumerated(value = EnumType.STRING)
    private Type type;

    @BatchSize(size=100) //
    @OneToMany(mappedBy = "clubEvent", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @ToString.Exclude
    private List<ClubEventImage> clubEventImageList;

    public void updateEvent(UpdateEventRequest request) {
        if (request.getTitle() != null) {
            this.title = request.getTitle();
        }
        if (request.getContent() != null) {
            this.content = request.getContent();
        }
        if (request.getWriter() != null) {
            this.writer = request.getWriter();
        }
        if (request.getSubject() != null) {
            this.subject = request.getSubject();
        }
        if (request.getUrl() != null) {
            this.url = request.getUrl();
        }
        if (request.getType() != null) {
            this.type = request.getType();
        }
        // date는 일반적으로 업데이트 요청 시 현재 시간으로 설정하는 것이 일반적이므로 주석 처리
        this.createdAt = LocalDateTime.now();
    }

    // 게시글의 저장수 증가
    public void incrementLikes() {
        this.likesCount++;
    }

    // 게시글의 저장수 감소
    public void decreaseLikes() {
        this.likesCount--;
    }

    // 게시글의 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }


}
