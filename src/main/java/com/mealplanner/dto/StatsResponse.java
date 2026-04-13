package com.mealplanner.dto;

import lombok.Data;

import java.util.List;

@Data
public class StatsResponse {
    private int totalCheckins;
    private int totalDishKinds;
    private List<DishStat> topByRating;
    private List<DishStat> topByFrequency;
}
