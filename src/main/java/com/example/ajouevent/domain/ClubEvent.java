package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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
    private String date;

    @Column
    private String subject;

    @OneToMany(mappedBy = "clubEvent", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @ToString.Exclude
    private List<ClubEventImage> clubEventImageList;

}
