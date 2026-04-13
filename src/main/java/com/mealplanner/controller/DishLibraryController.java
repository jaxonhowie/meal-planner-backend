package com.mealplanner.controller;

import com.mealplanner.dto.ApiResponse;
import com.mealplanner.entity.DishLibrary;
import com.mealplanner.service.DishLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dish-library")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DishLibraryController {

    private final DishLibraryService dishLibraryService;

    /**
     * GET /api/v1/dish-library?userId=1&mealType=dinner
     * 查询菜库列表
     */
    @GetMapping
    public ApiResponse<List<DishLibrary>> list(
            @RequestParam Long userId,
            @RequestParam(required = false) String mealType) {
        return ApiResponse.success(dishLibraryService.list(userId, mealType));
    }

    /**
     * GET /api/v1/dish-library/random?userId=1&mealType=dinner&count=2
     * 随机取菜名列表
     */
    @GetMapping("/random")
    public ApiResponse<List<String>> random(
            @RequestParam Long userId,
            @RequestParam(required = false) String mealType,
            @RequestParam(defaultValue = "2") int count) {
        return ApiResponse.success(dishLibraryService.random(userId, mealType, count));
    }

    /**
     * POST /api/v1/dish-library
     * Body: { "userId":1, "name":"红烧肉", "mealType":"dinner" }
     * 新增菜品
     */
    @PostMapping
    public ApiResponse<DishLibrary> add(@RequestBody Map<String, String> body) {
        Long userId = Long.parseLong(body.get("userId"));
        return ApiResponse.success(
            dishLibraryService.add(userId, body.get("name"), body.get("mealType"))
        );
    }

    /**
     * DELETE /api/v1/dish-library/{id}?userId=1
     * 删除自定义菜品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(
            @PathVariable Long id,
            @RequestParam Long userId) {
        dishLibraryService.delete(userId, id);
        return ApiResponse.success();
    }
}
