package com.mealplanner.controller;

import com.mealplanner.dto.AchievementDto;
import com.mealplanner.dto.ApiResponse;
import com.mealplanner.service.AchievementService;
import com.mealplanner.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ApiResponse<List<AchievementDto>> getAchievements() {
        return ApiResponse.success(achievementService.getUserAchievements(SecurityUtils.uid()));
    }

    @PostMapping("/check")
    public ApiResponse<List<AchievementDto>> checkAchievements() {
        return ApiResponse.success(achievementService.checkAndUnlock(SecurityUtils.uid()));
    }
}
