package com.calitrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "coach_recommendations")
@Getter
@Setter
public class CoachRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, unique = true)
    private Long workoutId;

    @Column(nullable = false, columnDefinition = "text")
    private String recommendation;

    @Column(nullable = false, columnDefinition = "text")
    private String focusArea;

    @Column(nullable = false, columnDefinition = "text")
    private String nextExercise;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
