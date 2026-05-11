package com.mealplanner.controller;

import com.mealplanner.dto.AchievementDto;
import com.mealplanner.dto.ApiResponse;
import com.mealplanner.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AchievementController {

    private final AchievementService achievementService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public ApiResponse<List<AchievementDto>> getAchievements() {
        return ApiResponse.success(achievementService.getUserAchievements(uid()));
    }

    @PostMapping("/check")
    public ApiResponse<List<AchievementDto>> checkAchievements() {
        return ApiResponse.success(achievementService.checkAndUnlock(uid()));
    }
}
