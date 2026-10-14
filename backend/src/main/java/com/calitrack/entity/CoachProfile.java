package com.calitrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "coach_profiles")
@Getter
@Setter
public class CoachProfile {

    @Id
    private String userId;

    @Column(nullable = false, length = 200)
    private String goals;

    @Column(nullable = false, length = 50)
    private String fitnessLevel;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
