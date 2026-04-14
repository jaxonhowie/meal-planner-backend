package com.mealplanner.service;

import com.mealplanner.dto.StatsResponse;
import com.mealplanner.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public StatsResponse getStats(Long userId) {
        StatsResponse stats = new StatsResponse();
        stats.setTotalCheckins(statsMapper.countCheckins(userId));
        stats.setTotalDishKinds(statsMapper.countDishKinds(userId));
        stats.setTopByRating(statsMapper.topByRating(userId));
        stats.setTopByFrequency(statsMapper.topByFrequency(userId));
        stats.setCheckinTrend(statsMapper.checkinTrendLast30Days(userId));
        return stats;
    }
}
