package com.mealplanner.controller;

import com.mealplanner.dto.ApiResponse;
import com.mealplanner.dto.FamilyLeaderboardDto;
import com.mealplanner.dto.StatsResponse;
import com.mealplanner.dto.UpdateDishesRequest;
import com.mealplanner.entity.MealPlan;
import com.mealplanner.entity.MealRecord;
import com.mealplanner.entity.User;
import com.mealplanner.mapper.UserMapper;
import com.mealplanner.service.MealPlanService;
import com.mealplanner.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final StatsService statsService;
    private final UserMapper userMapper;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Long familyId() {
        User user = userMapper.selectById(uid());
        if (user == null || user.getFamilyId() == null)
            throw new RuntimeException("请先加入或创建家庭");
        return user.getFamilyId();
    }

    @GetMapping("/meal/today")
    public ApiResponse<List<MealPlan>> getToday(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ApiResponse.success(mealPlanService.getDailyPlan(familyId(), target));
    }

    @PostMapping("/meal/generate")
    public ApiResponse<List<MealPlan>> generate(@RequestBody Map<String, String> body) {
        LocalDate date = body.containsKey("date")
            ? LocalDate.parse(body.get("date")) : LocalDate.now();
        return ApiResponse.success(mealPlanService.generateDailyPlan(familyId(), uid(), date));
    }

    @PostMapping("/meal/add-single")
    public ApiResponse<MealPlan> addSingle(@RequestBody Map<String, String> body) {
        LocalDate date = body.containsKey("date")
            ? LocalDate.parse(body.get("date")) : LocalDate.now();
        String mealType = body.get("mealType");
        return ApiResponse.success(mealPlanService.createSinglePlan(familyId(), uid(), date, mealType));
    }

    @PutMapping("/meal/{id}/dishes")
    public ApiResponse<MealPlan> updateDishes(
            @PathVariable Long id,
            @RequestBody UpdateDishesRequest request) {
        return ApiResponse.success(mealPlanService.updateDishes(id, request.getDishes()));
    }

    @PutMapping("/meal/{id}/status")
    public ApiResponse<MealPlan> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.success(mealPlanService.updateStatus(id, body.get("status")));
    }

    @PostMapping("/record")
    public ApiResponse<MealRecord> addRecord(@RequestBody Map<String, Object> body) {
        Long planId  = Long.parseLong(body.get("planId").toString());
        String desc  = (String) body.getOrDefault("description", "");
        Integer rating = body.containsKey("rating")
            ? Integer.parseInt(body.get("rating").toString()) : 3;
        String imageUrl = (String) body.getOrDefault("imageUrl", null);
        return ApiResponse.success(mealPlanService.addRecord(planId, uid(), desc, rating, imageUrl));
    }

    @GetMapping("/record/plan/{planId}")
    public ApiResponse<List<MealRecord>> getRecordsByPlan(@PathVariable Long planId) {
        return ApiResponse.success(mealPlanService.getRecordsByPlanId(planId));
    }

    @PutMapping("/record/{id}")
    public ApiResponse<MealRecord> updateRecord(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String desc = (String) body.getOrDefault("description", "");
        Integer rating = body.containsKey("rating")
            ? Integer.parseInt(body.get("rating").toString()) : null;
        String imageUrl = (String) body.getOrDefault("imageUrl", null);
        return ApiResponse.success(mealPlanService.updateRecord(id, uid(), desc, rating, imageUrl));
    }

    @GetMapping("/record")
    public ApiResponse<List<MealRecord>> getRecords(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ApiResponse.success(mealPlanService.getRecords(familyId(), target));
    }

    @GetMapping("/record/range")
    public ApiResponse<List<MealRecord>> getRecordsRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.success(mealPlanService.getRecordsForRange(familyId(), start, end));
    }

    @GetMapping("/meal/week")
    public ApiResponse<List<MealPlan>> getWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.success(mealPlanService.getWeeklyPlans(familyId(), start, end));
    }

    @GetMapping("/stats")
    public ApiResponse<StatsResponse> getStats() {
        return ApiResponse.success(statsService.getStats(uid()));
    }

    @GetMapping("/stats/family-leaderboard")
    public ApiResponse<FamilyLeaderboardDto> getFamilyLeaderboard() {
        return ApiResponse.success(statsService.getFamilyLeaderboard(familyId()));
    }
}
