package com.mealplanner.service;

import com.mealplanner.dto.FamilyLeaderboardDto;
import com.mealplanner.dto.StatsResponse;
import com.mealplanner.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.mealplanner.dto.DayCount;
import com.mealplanner.dto.WeeklyRating;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    @Cacheable(value = "userStats", key = "#userId")
    public StatsResponse getStats(Long userId) {
        StatsResponse stats = new StatsResponse();
        stats.setTotalCheckins(statsMapper.countCheckins(userId));
        stats.setTotalDishKinds(statsMapper.countDishKinds(userId));
        stats.setOverallAvgRating(statsMapper.overallAvgRating(userId));
        stats.setTopByRating(statsMapper.topByRating(userId));
        stats.setTopByFrequency(statsMapper.topByFrequency(userId));
        stats.setCheckinTrend(fillDayCount(statsMapper.checkinTrendLast30Days(userId), 30));
        stats.setHeatmapData(fillDayCount(statsMapper.checkinHeatmap91Days(userId), 91));
        stats.setMealTypeDistribution(statsMapper.mealTypeDistribution(userId));
        stats.setRatingTrend(fillWeeklyRating(statsMapper.ratingTrendLast8Weeks(userId), 8));

        List<String> allDates = statsMapper.allCheckinDates(userId);
        int[] streaks = computeStreaks(allDates);
        stats.setCurrentStreak(streaks[0]);
        stats.setBestStreak(streaks[1]);

        return stats;
    }

    private List<DayCount> fillDayCount(List<DayCount> sparse, int days) {
        Map<String, Integer> lookup = sparse.stream()
                .collect(Collectors.toMap(DayCount::getDate, DayCount::getCount));
        LocalDate today = LocalDate.now();
        List<DayCount> dense = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String dateStr = today.minusDays(i).toString();
            DayCount dc = new DayCount();
            dc.setDate(dateStr);
            dc.setCount(lookup.getOrDefault(dateStr, 0));
            dense.add(dc);
        }
        return dense;
    }

    private List<WeeklyRating> fillWeeklyRating(List<WeeklyRating> sparse, int weeks) {
        Map<String, WeeklyRating> lookup = sparse.stream()
                .collect(Collectors.toMap(WeeklyRating::getWeekLabel, wr -> wr));
        LocalDate today = LocalDate.now();
        // 与 MySQL WEEKDAY() 对齐：DayOfWeek.getValue() 1=Monday … 7=Sunday
        LocalDate thisMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        List<WeeklyRating> dense = new ArrayList<>(weeks);
        for (int i = weeks - 1; i >= 0; i--) {
            String label = thisMonday.minusWeeks(i).format(fmt);
            dense.add(lookup.getOrDefault(label, emptyWeek(label)));
        }
        return dense;
    }

    private WeeklyRating emptyWeek(String label) {
        WeeklyRating wr = new WeeklyRating();
        wr.setWeekLabel(label);
        wr.setAvgRating(0.0);
        wr.setCheckinCount(0);
        return wr;
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

    @Cacheable(value = "familyLeaderboard", key = "#familyId")
    public FamilyLeaderboardDto getFamilyLeaderboard(Long familyId) {
        FamilyLeaderboardDto dto = new FamilyLeaderboardDto();
        dto.setByCheckinDays(statsMapper.familyCheckinDays(familyId));
        dto.setByDishVariety(statsMapper.familyDishVariety(familyId));
        dto.setByAvgRating(statsMapper.familyAvgRating(familyId));
        return dto;
    }
}
