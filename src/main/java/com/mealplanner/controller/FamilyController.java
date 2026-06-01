package com.mealplanner.controller;

import com.mealplanner.dto.ApiResponse;
import com.mealplanner.dto.FamilyDto;
import com.mealplanner.service.FamilyService;
import com.mealplanner.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/family")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FamilyController {

    private final FamilyService familyService;

    /** GET /api/v1/family — 查询当前家庭 */
    @GetMapping
    public ApiResponse<FamilyDto> get() {
        return ApiResponse.success(familyService.getFamily(SecurityUtils.uid()));
    }

    /** POST /api/v1/family — 创建家庭 { "name": "张家" } */
    @PostMapping
    public ApiResponse<FamilyDto> create(@RequestBody Map<String, String> body) {
        return ApiResponse.success(familyService.create(SecurityUtils.uid(), body.get("name")));
    }

    /** POST /api/v1/family/join — 加入家庭 { "inviteCode": "ABCD1234" } */
    @PostMapping("/join")
    public ApiResponse<FamilyDto> join(@RequestBody Map<String, String> body) {
        return ApiResponse.success(familyService.join(SecurityUtils.uid(), body.get("inviteCode")));
    }

    /** DELETE /api/v1/family/leave — 退出家庭 */
    @DeleteMapping("/leave")
    public ApiResponse<?> leave() {
        familyService.leave(SecurityUtils.uid());
        return ApiResponse.success();
    }
}
