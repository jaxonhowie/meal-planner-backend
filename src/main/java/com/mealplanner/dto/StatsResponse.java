package com.mealplanner.dto;

import lombok.Data;

import java.util.List;

@Data
public class StatsResponse {
    private int totalCheckins;
    private int totalDishKinds;
    private List<DishStat> topByRating;
    private List<DishStat> topByFrequency;
    /** 近30天每日打卡数量，用于前端趋势图 */
    private List<DayCount> checkinTrend;
}
