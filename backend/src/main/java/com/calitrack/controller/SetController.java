package com.calitrack.controller;

import com.calitrack.dto.CreateSetRequest;
import com.calitrack.dto.SetResponse;
import com.calitrack.dto.UpdateSetRequest;
import com.calitrack.service.CurrentUserService;
import com.calitrack.service.SetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workouts/{workoutId}/sets")
public class SetController {

    private final SetService setService;
    private final CurrentUserService currentUserService;

    public SetController(SetService setService, CurrentUserService currentUserService) {
        this.setService = setService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SetResponse addSet(
            @PathVariable Long workoutId,
            @Valid @RequestBody CreateSetRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return setService.addSet(workoutId, currentUserService.getUserId(jwt), request);
    }

    @GetMapping
    public List<SetResponse> getSets(
            @PathVariable Long workoutId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return setService.getSets(workoutId, currentUserService.getUserId(jwt));
    }

    @PatchMapping("/{setId}")
    public SetResponse updateSet(
            @PathVariable Long workoutId,
            @PathVariable Long setId,
            @Valid @RequestBody UpdateSetRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return setService.updateSet(workoutId, setId, currentUserService.getUserId(jwt), request);
    }

    @DeleteMapping("/{setId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSet(
            @PathVariable Long workoutId,
            @PathVariable Long setId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        setService.deleteSet(workoutId, setId, currentUserService.getUserId(jwt));
    }
}
