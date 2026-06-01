package com.mealplanner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealplanner.dto.AchievementDto;
import com.mealplanner.entity.Achievement;
import com.mealplanner.entity.UserAchievement;
import com.mealplanner.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final StatsMapper statsMapper;
    private final MealRecordMapper mealRecordMapper;

    /** 获取所有成就 + 当前用户的解锁状态和进度 */
    @Cacheable(value = "userAchievements", key = "#userId")
    public List<AchievementDto> getUserAchievements(Long userId) {
        List<Achievement> all = achievementMapper.selectList(
            new LambdaQueryWrapper<Achievement>().orderByAsc(Achievement::getSortOrder)
        );
        Map<Long, UserAchievement> unlocked = userAchievementMapper.selectList(
            new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId)
        ).stream().collect(Collectors.toMap(UserAchievement::getAchievementId, ua -> ua));

        UserStats stats = computeUserStats(userId);

        List<AchievementDto> result = new ArrayList<>();
        for (Achievement a : all) {
            AchievementDto dto = toDto(a);

            UserAchievement ua = unlocked.get(a.getId());
            if (ua != null) {
                dto.setUnlocked(true);
                dto.setUnlockedAt(ua.getUnlockedAt());
                dto.setCurrentValue(a.getThreshold());
            } else {
                dto.setUnlocked(false);
                dto.setCurrentValue(computeProgress(a.getCode(), stats));
            }
            result.add(dto);
        }
        return result;
    }

    /** 幂等解锁检查（同步版本，供 Controller 使用，返回新解锁列表） */
    @Transactional
    @CacheEvict(value = "userAchievements", key = "#userId")
    public List<AchievementDto> checkAndUnlock(Long userId) {
        List<Achievement> all = achievementMapper.selectList(null);
        Set<Long> alreadyUnlocked = userAchievementMapper.selectList(
            new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId)
        ).stream().map(UserAchievement::getAchievementId).collect(Collectors.toSet());

        UserStats stats = computeUserStats(userId);

        List<AchievementDto> newlyUnlocked = new ArrayList<>();
        for (Achievement a : all) {
            if (alreadyUnlocked.contains(a.getId())) continue;
            if (shouldUnlock(a.getCode(), stats)) {
                UserAchievement ua = new UserAchievement();
                ua.setUserId(userId);
                ua.setAchievementId(a.getId());
                ua.setUnlockedAt(LocalDateTime.now());
                userAchievementMapper.insert(ua);

                AchievementDto dto = toDto(a);
                dto.setUnlocked(true);
                dto.setUnlockedAt(ua.getUnlockedAt());
                dto.setCurrentValue(a.getThreshold());
                newlyUnlocked.add(dto);

                log.info("User {} unlocked achievement: {}", userId, a.getCode());
            }
        }
        return newlyUnlocked;
    }

    /** 异步版本，供打卡流程使用（不阻塞主流程） */
    @Async("asyncExecutor")
    public void checkAndUnlockAsync(Long userId) {
        try {
            checkAndUnlock(userId);
        } catch (Exception e) {
            log.warn("异步成就检查失败: {}", e.getMessage());
        }
    }

    private AchievementDto toDto(Achievement a) {
        AchievementDto dto = new AchievementDto();
        dto.setId(a.getId());
        dto.setCode(a.getCode());
        dto.setName(a.getName());
        dto.setDescription(a.getDescription());
        dto.setIcon(a.getIcon());
        dto.setCategory(a.getCategory());
        dto.setThreshold(a.getThreshold());
        return dto;
    }

    private UserStats computeUserStats(Long userId) {
        return new UserStats(
            statsMapper.countCheckins(userId),
            statsMapper.countDishKinds(userId),
            computeCurrentStreak(userId),
            hasRating(userId, 5),
            hasAllMealsInOneDay(userId)
        );
    }

    private record UserStats(int totalCheckins, int dishKinds, int currentStreak,
                              boolean hasFiveStar, boolean allMealsDay) {}

    private boolean shouldUnlock(String code, UserStats stats) {
        return switch (code) {
            case "first_checkin" -> stats.totalCheckins() >= 1;
            case "streak_7"      -> stats.currentStreak() >= 7;
            case "streak_30"     -> stats.currentStreak() >= 30;
            case "streak_100"    -> stats.currentStreak() >= 100;
            case "variety_10"    -> stats.dishKinds() >= 10;
            case "variety_30"    -> stats.dishKinds() >= 30;
            case "variety_50"    -> stats.dishKinds() >= 50;
            case "rating_5star"  -> stats.hasFiveStar();
            case "checkin_50"    -> stats.totalCheckins() >= 50;
            case "checkin_100"   -> stats.totalCheckins() >= 100;
            case "checkin_200"   -> stats.totalCheckins() >= 200;
            case "all_meals_day" -> stats.allMealsDay();
            default -> false;
        };
    }

    private int computeProgress(String code, UserStats stats) {
        return switch (code) {
            case "first_checkin" -> Math.min(stats.totalCheckins(), 1);
            case "streak_7"      -> Math.min(stats.currentStreak(), 7);
            case "streak_30"     -> Math.min(stats.currentStreak(), 30);
            case "streak_100"    -> Math.min(stats.currentStreak(), 100);
            case "variety_10"    -> Math.min(stats.dishKinds(), 10);
            case "variety_30"    -> Math.min(stats.dishKinds(), 30);
            case "variety_50"    -> Math.min(stats.dishKinds(), 50);
            case "rating_5star"  -> stats.hasFiveStar() ? 1 : 0;
            case "checkin_50"    -> Math.min(stats.totalCheckins(), 50);
            case "checkin_100"   -> Math.min(stats.totalCheckins(), 100);
            case "checkin_200"   -> Math.min(stats.totalCheckins(), 200);
            case "all_meals_day" -> stats.allMealsDay() ? 1 : 0;
            default -> 0;
        };
    }

    private int computeCurrentStreak(Long userId) {
        List<String> dates = statsMapper.allCheckinDates(userId);
        if (dates.isEmpty()) return 0;
        // allCheckinDates returns DESC order
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate first = java.time.LocalDate.parse(dates.get(0));
        if (!first.equals(today) && !first.equals(today.minusDays(1))) return 0;

        int streak = 1;
        for (int i = 1; i < dates.size(); i++) {
            java.time.LocalDate curr = java.time.LocalDate.parse(dates.get(i));
            java.time.LocalDate prev = java.time.LocalDate.parse(dates.get(i - 1));
            if (prev.minusDays(1).equals(curr)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private boolean hasRating(Long userId, int targetRating) {
        return mealRecordMapper.selectCount(
            new LambdaQueryWrapper<com.mealplanner.entity.MealRecord>()
                .eq(com.mealplanner.entity.MealRecord::getUserId, userId)
                .eq(com.mealplanner.entity.MealRecord::getRating, targetRating)
        ) > 0;
    }

    private boolean hasAllMealsInOneDay(Long userId) {
        return statsMapper.countDaysWithAllMeals(userId) > 0;
    }
}
