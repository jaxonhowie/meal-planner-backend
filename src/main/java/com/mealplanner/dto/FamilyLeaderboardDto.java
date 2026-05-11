package com.mealplanner.dto;

import lombok.Data;

import java.util.List;

@Data
public class FamilyLeaderboardDto {
    private List<LeaderboardEntry> byCheckinDays;
    private List<LeaderboardEntry> byDishVariety;
    private List<LeaderboardEntry> byAvgRating;

    @Data
    public static class LeaderboardEntry {
        private Long userId;
        private String username;
        private String nickname;
        private double value;
    }
}
