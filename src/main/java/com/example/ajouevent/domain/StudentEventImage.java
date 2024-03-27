package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentEventImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @Column
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "student_event_id")
    @ToString.Exclude
    private StudentEvent studentEvent;
}
