package com.calitrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    // Where the set comes in its workout, from 0.
    @Column(nullable = false)
    private Integer position;
}
