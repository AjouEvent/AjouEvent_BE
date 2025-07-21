package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "club_event_image")
public class ClubEventImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @Column
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "club_event_id")
    @ToString.Exclude
    private ClubEvent clubEvent;

    @Builder
    private ClubEventImage(String url, ClubEvent clubEvent) {
        this.url = url;
        this.clubEvent = clubEvent;
    }

    public static ClubEventImage create(String url, ClubEvent clubEvent) {
        return ClubEventImage.builder()
            .url(url)
            .clubEvent(clubEvent)
            .build();
    }
}
