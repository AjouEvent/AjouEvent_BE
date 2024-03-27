package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmImageId;

    @Column
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "alarm_id")
    @ToString.Exclude
    private Alarm alarm;
}
