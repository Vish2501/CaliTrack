package com.calitrack.service;

import com.calitrack.dto.CoachProfileResponse;
import com.calitrack.dto.UpdateCoachProfileRequest;
import com.calitrack.entity.CoachProfile;
import com.calitrack.repository.CoachProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class CoachProfileService {

    static final String DEFAULT_GOALS = "general fitness";
    static final String DEFAULT_FITNESS_LEVEL = "intermediate";

    private final CoachProfileRepository profileRepository;

    public CoachProfileService(CoachProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /** The user's saved profile, or the defaults if they haven't set one. */
    public CoachProfileResponse getProfile(String userId) {
        return profileRepository.findById(userId)
            .map(profile -> new CoachProfileResponse(profile.getGoals(), profile.getFitnessLevel()))
            .orElse(new CoachProfileResponse(DEFAULT_GOALS, DEFAULT_FITNESS_LEVEL));
    }

    public CoachProfileResponse updateProfile(String userId, UpdateCoachProfileRequest request) {
        CoachProfile profile = profileRepository.findById(userId).orElseGet(() -> {
            CoachProfile created = new CoachProfile();
            created.setUserId(userId);
            return created;
        });
        profile.setGoals(request.goals().trim());
        profile.setFitnessLevel(request.fitnessLevel().trim());
        CoachProfile saved = profileRepository.save(profile);
        return new CoachProfileResponse(saved.getGoals(), saved.getFitnessLevel());
    }
}
