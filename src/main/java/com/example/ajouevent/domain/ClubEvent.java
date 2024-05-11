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

    @Column
    private String content;

    @Column
    private String writer;

    @Column
    private LocalDateTime createdAt;

    @Column
    private String subject;

    @Column
    private String url;

    @Column
    private String major;

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

}
