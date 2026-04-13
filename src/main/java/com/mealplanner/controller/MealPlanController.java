package com.mealplanner.controller;

import com.mealplanner.dto.ApiResponse;
import com.mealplanner.dto.UpdateDishesRequest;
import com.mealplanner.entity.MealPlan;
import com.mealplanner.entity.MealRecord;
import com.mealplanner.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    /**
     * GET /api/v1/meal/today?userId=1
     * 查询今日菜单
     */
    @GetMapping("/meal/today")
    public ApiResponse<List<MealPlan>> getToday(
            @RequestParam Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ApiResponse.success(mealPlanService.getDailyPlan(userId, target));
    }

    /**
     * POST /api/v1/meal/generate
     * Body: { "userId": 1, "date": "2026-04-10" }
     * 生成今日菜单
     */
    @PostMapping("/meal/generate")
    public ApiResponse<List<MealPlan>> generate(@RequestBody Map<String, String> body) {
        Long userId = Long.parseLong(body.get("userId"));
        LocalDate date = body.containsKey("date")
            ? LocalDate.parse(body.get("date"))
            : LocalDate.now();
        return ApiResponse.success(mealPlanService.generateDailyPlan(userId, date));
    }

    /**
     * PUT /api/v1/meal/{id}/dishes
     * Body: { "dishes": [{"dishName":"红烧肉","remark":"少放油"}, ...] }
     * 更新计划菜品
     */
    @PutMapping("/meal/{id}/dishes")
    public ApiResponse<MealPlan> updateDishes(
            @PathVariable Long id,
            @RequestBody UpdateDishesRequest request) {
        return ApiResponse.success(mealPlanService.updateDishes(id, request.getDishes()));
    }

    /**
     * PUT /api/v1/meal/{id}/status
     * Body: { "status": "done" }
     * 更新状态
     */
    @PutMapping("/meal/{id}/status")
    public ApiResponse<MealPlan> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.success(mealPlanService.updateStatus(id, body.get("status")));
    }

    /**
     * POST /api/v1/record
     * Body: { "planId":1, "userId":1, "description":"...", "rating":5, "imageUrl":"..." }
     * 新增打卡记录
     */
    @PostMapping("/record")
    public ApiResponse<MealRecord> addRecord(@RequestBody Map<String, Object> body) {
        Long planId     = Long.parseLong(body.get("planId").toString());
        Long userId     = Long.parseLong(body.get("userId").toString());
        String desc     = (String) body.getOrDefault("description", "");
        Integer rating  = body.containsKey("rating") ? Integer.parseInt(body.get("rating").toString()) : 3;
        String imageUrl = (String) body.getOrDefault("imageUrl", null);
        return ApiResponse.success(mealPlanService.addRecord(planId, userId, desc, rating, imageUrl));
    }

    /**
     * GET /api/v1/record?userId=1&date=2026-04-10
     * 查询打卡记录
     */
    @GetMapping("/record")
    public ApiResponse<List<MealRecord>> getRecords(
            @RequestParam Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ApiResponse.success(mealPlanService.getRecords(userId, target));
    }
}
