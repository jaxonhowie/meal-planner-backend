package com.mealplanner.controller;

import com.mealplanner.dto.ApiResponse;
import com.mealplanner.entity.DishLibrary;
import com.mealplanner.service.DishLibraryService;
import com.mealplanner.util.SecurityUtils;
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

    @GetMapping
    public ApiResponse<List<DishLibrary>> list(@RequestParam(required = false) String mealType) {
        return ApiResponse.success(dishLibraryService.listForUser(SecurityUtils.uid(), mealType));
    }

    @GetMapping("/random")
    public ApiResponse<List<String>> random(
            @RequestParam(required = false) String mealType,
            @RequestParam(defaultValue = "2") int count) {
        return ApiResponse.success(dishLibraryService.random(SecurityUtils.uid(), mealType, count));
    }

    @PostMapping
    public ApiResponse<DishLibrary> add(@RequestBody Map<String, String> body) {
        return ApiResponse.success(
            dishLibraryService.add(SecurityUtils.uid(), body.get("name"), body.get("mealType"), body.get("tags"))
        );
    }

    @PutMapping("/{id}/tags")
    public ApiResponse<DishLibrary> updateTags(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.success(dishLibraryService.updateTags(id, body.get("tags")));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        dishLibraryService.delete(SecurityUtils.uid(), id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/image")
    public ApiResponse<DishLibrary> updateImage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.success(dishLibraryService.setImageUrl(id, body.get("imageUrl")));
    }

    @DeleteMapping("/{id}/image")
    public ApiResponse<DishLibrary> clearImage(@PathVariable Long id) {
        return ApiResponse.success(dishLibraryService.setImageUrl(id, null));
    }

    @PutMapping("/{id}/favorite")
    public ApiResponse<DishLibrary> toggleFavorite(@PathVariable Long id) {
        return ApiResponse.success(dishLibraryService.toggleFavorite(id));
    }
}
