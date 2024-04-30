package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

import org.joda.time.DateTime;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Alarm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmId;

    @Column
    private LocalDateTime alarmDateTime;

    @Column
    private String date;

    @Column
    private String title;

    @Column
    private String content;

    @Column
    private String url;

    @Column
    private String writer;

    @Column
    private DateTime eventDate;

    @Column
    private String subject;

    @Column
    private String target;

    @Column(length = 50000)
    @Enumerated(value = EnumType.STRING)
    private Type type;

    @OneToMany(mappedBy = "alarm", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @ToString.Exclude
    private List<AlarmImage> alarmImageList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "member_id")
    @ToString.Exclude
    private Member member;
}
