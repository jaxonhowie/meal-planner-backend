package com.mealplanner.service;

import com.mealplanner.dto.StatsResponse;
import com.mealplanner.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public StatsResponse getStats(Long userId) {
        StatsResponse stats = new StatsResponse();
        stats.setTotalCheckins(statsMapper.countCheckins(userId));
        stats.setTotalDishKinds(statsMapper.countDishKinds(userId));
        stats.setOverallAvgRating(statsMapper.overallAvgRating(userId));
        stats.setTopByRating(statsMapper.topByRating(userId));
        stats.setTopByFrequency(statsMapper.topByFrequency(userId));
        stats.setCheckinTrend(statsMapper.checkinTrendLast30Days(userId));
        stats.setHeatmapData(statsMapper.checkinHeatmap91Days(userId));
        stats.setMealTypeDistribution(statsMapper.mealTypeDistribution(userId));
        stats.setRatingTrend(statsMapper.ratingTrendLast8Weeks(userId));

        List<String> allDates = statsMapper.allCheckinDates(userId);
        int[] streaks = computeStreaks(allDates);
        stats.setCurrentStreak(streaks[0]);
        stats.setBestStreak(streaks[1]);

        return stats;
    }

    /**
     * 计算当前连续打卡天数和历史最长连续打卡天数。
     * 若今天已打卡则从今天往前数；否则从昨天往前数（允许当天还没打卡不中断）。
     *
     * @param dateStrings 所有打卡日期字符串（格式 "yyyy-MM-dd"），降序
     * @return int[]{currentStreak, bestStreak}
     */
    private int[] computeStreaks(List<String> dateStrings) {
        if (dateStrings.isEmpty()) return new int[]{0, 0};

        Set<LocalDate> dateSet = dateStrings.stream()
                .map(LocalDate::parse)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();

        // 当前连续：今天有打卡则从今天算，否则从昨天算
        int current = 0;
        LocalDate cursor = dateSet.contains(today) ? today : today.minusDays(1);
        while (dateSet.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }

        // 历史最长连续
        List<LocalDate> sorted = new ArrayList<>(dateSet);
        Collections.sort(sorted);
        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : sorted) {
            if (prev != null && d.equals(prev.plusDays(1))) {
                run++;
            } else {
                run = 1;
            }
            if (run > best) best = run;
            prev = d;
        }

        return new int[]{current, best};
    }
}
