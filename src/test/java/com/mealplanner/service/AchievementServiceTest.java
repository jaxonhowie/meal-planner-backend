package com.mealplanner.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mealplanner.dto.AchievementDto;
import com.mealplanner.entity.Achievement;
import com.mealplanner.entity.MealRecord;
import com.mealplanner.entity.UserAchievement;
import com.mealplanner.mapper.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private AchievementMapper achievementMapper;
    @Mock
    private UserAchievementMapper userAchievementMapper;
    @Mock
    private StatsMapper statsMapper;
    @Mock
    private MealRecordMapper mealRecordMapper;

    @InjectMocks
    private AchievementService achievementService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Achievement.class);
        TableInfoHelper.initTableInfo(assistant, UserAchievement.class);
        TableInfoHelper.initTableInfo(assistant, MealRecord.class);
    }

    private Achievement createAchievement(Long id, String code, String name) {
        Achievement a = new Achievement();
        a.setId(id);
        a.setCode(code);
        a.setName(name);
        a.setDescription("desc");
        a.setIcon("icon");
        a.setCategory("checkin");
        a.setThreshold(1);
        a.setSortOrder(0);
        return a;
    }

    @Test
    void getUserAchievements_allLocked() {
        Achievement a1 = createAchievement(1L, "first_checkin", "首次打卡");
        Achievement a2 = createAchievement(2L, "streak_7", "连续7天");

        when(achievementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a1, a2));
        when(userAchievementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(statsMapper.countCheckins(anyLong())).thenReturn(0);
        when(statsMapper.countDishKinds(anyLong())).thenReturn(0);
        when(statsMapper.allCheckinDates(anyLong())).thenReturn(List.of());
        when(mealRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        List<AchievementDto> result = achievementService.getUserAchievements(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isUnlocked()).isFalse();
        assertThat(result.get(1).isUnlocked()).isFalse();
    }

    @Test
    void getUserAchievements_someUnlocked() {
        Achievement a1 = createAchievement(1L, "first_checkin", "首次打卡");

        UserAchievement ua = new UserAchievement();
        ua.setUserId(1L);
        ua.setAchievementId(1L);

        when(achievementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a1));
        when(userAchievementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ua));
        when(statsMapper.countCheckins(anyLong())).thenReturn(5);
        when(statsMapper.countDishKinds(anyLong())).thenReturn(3);
        when(statsMapper.allCheckinDates(anyLong())).thenReturn(List.of());
        when(mealRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        List<AchievementDto> result = achievementService.getUserAchievements(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isUnlocked()).isTrue();
        assertThat(result.get(0).getCurrentValue()).isEqualTo(1);
    }

    @Test
    void checkAndUnlock_firstCheckin() {
        Achievement a1 = createAchievement(1L, "first_checkin", "首次打卡");

        when(achievementMapper.selectList(any())).thenReturn(List.of(a1));
        when(userAchievementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(statsMapper.countCheckins(anyLong())).thenReturn(1);
        when(statsMapper.countDishKinds(anyLong())).thenReturn(1);
        when(statsMapper.allCheckinDates(anyLong())).thenReturn(List.of("2026-05-22"));
        when(mealRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userAchievementMapper.insert(any(UserAchievement.class))).thenReturn(1);

        List<AchievementDto> result = achievementService.checkAndUnlock(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("first_checkin");
        verify(userAchievementMapper).insert(any(UserAchievement.class));
    }

    @Test
    void checkAndUnlock_alreadyUnlocked() {
        Achievement a1 = createAchievement(1L, "first_checkin", "首次打卡");

        UserAchievement ua = new UserAchievement();
        ua.setAchievementId(1L);

        when(achievementMapper.selectList(any())).thenReturn(List.of(a1));
        when(userAchievementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ua));
        when(statsMapper.countCheckins(anyLong())).thenReturn(5);
        when(statsMapper.countDishKinds(anyLong())).thenReturn(3);
        when(statsMapper.allCheckinDates(anyLong())).thenReturn(List.of());
        when(mealRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        List<AchievementDto> result = achievementService.checkAndUnlock(1L);

        assertThat(result).isEmpty();
        verify(userAchievementMapper, never()).insert(any(UserAchievement.class));
    }

    @Test
    void checkAndUnlock_noQualify() {
        Achievement a1 = createAchievement(1L, "streak_7", "连续7天");

        when(achievementMapper.selectList(any())).thenReturn(List.of(a1));
        when(userAchievementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(statsMapper.countCheckins(anyLong())).thenReturn(0);
        when(statsMapper.countDishKinds(anyLong())).thenReturn(0);
        when(statsMapper.allCheckinDates(anyLong())).thenReturn(List.of());
        when(mealRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        List<AchievementDto> result = achievementService.checkAndUnlock(1L);

        assertThat(result).isEmpty();
        verify(userAchievementMapper, never()).insert(any(UserAchievement.class));
    }
}
