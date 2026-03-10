package com.calitrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "sets")
@Getter
@Setter
public class Set {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workoutId;

    @Column(nullable = false)
    private Long exerciseId;

    private Integer reps;
    private Double weight;
    private Double rpe;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
