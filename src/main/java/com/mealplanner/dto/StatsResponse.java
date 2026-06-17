package com.mealplanner.dto;

import lombok.Data;

import java.util.List;

@Data
public class StatsResponse {
    private int totalCheckins;
    private int totalDishKinds;
    private Double overallAvgRating;
    private List<DishStat> topByRating;
    private List<DishStat> topByFrequency;
    /** 近30天每日打卡数量，用于历史页趋势图 */
    private List<DayCount> checkinTrend;

    /** 当前连续打卡天数 */
    private int currentStreak;
    /** 历史最长连续打卡天数 */
    private int bestStreak;
    /** 餐次分布（早/午/晚各打卡次数） */
    private List<MealTypeCount> mealTypeDistribution;
    /** 近8周每周平均评分趋势 */
    private List<WeeklyRating> ratingTrend;
    /** 近91天每日打卡数量，用于热力图 */
    private List<DayCount> heatmapData;
}
