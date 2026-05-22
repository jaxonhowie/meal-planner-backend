package com.mealplanner.service;

import com.mealplanner.dto.FamilyLeaderboardDto;
import com.mealplanner.dto.StatsResponse;
import com.mealplanner.mapper.StatsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsMapper statsMapper;

    @InjectMocks
    private StatsService statsService;

    @Test
    void getStats_allFieldsPopulated() {
        Long userId = 1L;

        when(statsMapper.countCheckins(userId)).thenReturn(50);
        when(statsMapper.countDishKinds(userId)).thenReturn(20);
        when(statsMapper.overallAvgRating(userId)).thenReturn(4.2);
        when(statsMapper.topByRating(userId)).thenReturn(List.of());
        when(statsMapper.topByFrequency(userId)).thenReturn(List.of());
        when(statsMapper.checkinTrendLast30Days(userId)).thenReturn(List.of());
        when(statsMapper.checkinHeatmap91Days(userId)).thenReturn(List.of());
        when(statsMapper.mealTypeDistribution(userId)).thenReturn(List.of());
        when(statsMapper.ratingTrendLast8Weeks(userId)).thenReturn(List.of());
        when(statsMapper.allCheckinDates(userId)).thenReturn(List.of());

        StatsResponse result = statsService.getStats(userId);

        assertThat(result.getTotalCheckins()).isEqualTo(50);
        assertThat(result.getTotalDishKinds()).isEqualTo(20);
        assertThat(result.getOverallAvgRating()).isEqualTo(4.2);
        assertThat(result.getCurrentStreak()).isEqualTo(0);
        assertThat(result.getBestStreak()).isEqualTo(0);
    }

    @Test
    void getStats_computeStreaks_consecutiveDays() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        when(statsMapper.countCheckins(userId)).thenReturn(5);
        when(statsMapper.countDishKinds(anyLong())).thenReturn(5);
        when(statsMapper.overallAvgRating(anyLong())).thenReturn(4.0);
        when(statsMapper.topByRating(anyLong())).thenReturn(List.of());
        when(statsMapper.topByFrequency(anyLong())).thenReturn(List.of());
        when(statsMapper.checkinTrendLast30Days(anyLong())).thenReturn(List.of());
        when(statsMapper.checkinHeatmap91Days(anyLong())).thenReturn(List.of());
        when(statsMapper.mealTypeDistribution(anyLong())).thenReturn(List.of());
        when(statsMapper.ratingTrendLast8Weeks(anyLong())).thenReturn(List.of());

        // 5 consecutive days ending today
        when(statsMapper.allCheckinDates(userId)).thenReturn(List.of(
            today.toString(),
            today.minusDays(1).toString(),
            today.minusDays(2).toString(),
            today.minusDays(3).toString(),
            today.minusDays(4).toString()
        ));

        StatsResponse result = statsService.getStats(userId);

        assertThat(result.getCurrentStreak()).isEqualTo(5);
        assertThat(result.getBestStreak()).isEqualTo(5);
    }

    @Test
    void getStats_computeStreaks_gapInStreak() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        when(statsMapper.countCheckins(anyLong())).thenReturn(0);
        when(statsMapper.countDishKinds(anyLong())).thenReturn(0);
        when(statsMapper.overallAvgRating(anyLong())).thenReturn(0.0);
        when(statsMapper.topByRating(anyLong())).thenReturn(List.of());
        when(statsMapper.topByFrequency(anyLong())).thenReturn(List.of());
        when(statsMapper.checkinTrendLast30Days(anyLong())).thenReturn(List.of());
        when(statsMapper.checkinHeatmap91Days(anyLong())).thenReturn(List.of());
        when(statsMapper.mealTypeDistribution(anyLong())).thenReturn(List.of());
        when(statsMapper.ratingTrendLast8Weeks(anyLong())).thenReturn(List.of());

        // today, yesterday, then a gap, then 2 more days
        when(statsMapper.allCheckinDates(userId)).thenReturn(List.of(
            today.toString(),
            today.minusDays(1).toString(),
            today.minusDays(3).toString(),
            today.minusDays(4).toString()
        ));

        StatsResponse result = statsService.getStats(userId);

        assertThat(result.getCurrentStreak()).isEqualTo(2);
        assertThat(result.getBestStreak()).isEqualTo(2);
    }

    @Test
    void getFamilyLeaderboard_success() {
        Long familyId = 10L;

        when(statsMapper.familyCheckinDays(familyId)).thenReturn(List.of());
        when(statsMapper.familyDishVariety(familyId)).thenReturn(List.of());
        when(statsMapper.familyAvgRating(familyId)).thenReturn(List.of());

        FamilyLeaderboardDto result = statsService.getFamilyLeaderboard(familyId);

        assertThat(result).isNotNull();
        assertThat(result.getByCheckinDays()).isEmpty();
    }
}
