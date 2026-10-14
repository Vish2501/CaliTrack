package com.calitrack.repository;

import com.calitrack.entity.CoachProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachProfileRepository extends JpaRepository<CoachProfile, String> {
}
