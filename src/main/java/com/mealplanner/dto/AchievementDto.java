package com.mealplanner.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AchievementDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Integer threshold;
    private boolean unlocked;
    private LocalDateTime unlockedAt;
    private int currentValue;
}
