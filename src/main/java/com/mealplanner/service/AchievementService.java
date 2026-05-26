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

        // Pre-compute values needed for progress
        int totalCheckins = statsMapper.countCheckins(userId);
        int dishKinds = statsMapper.countDishKinds(userId);
        int currentStreak = computeCurrentStreak(userId);
        boolean hasFiveStar = hasRating(userId, 5);
        boolean allMealsDay = hasAllMealsInOneDay(userId);

        List<AchievementDto> result = new ArrayList<>();
        for (Achievement a : all) {
            AchievementDto dto = new AchievementDto();
            dto.setId(a.getId());
            dto.setCode(a.getCode());
            dto.setName(a.getName());
            dto.setDescription(a.getDescription());
            dto.setIcon(a.getIcon());
            dto.setCategory(a.getCategory());
            dto.setThreshold(a.getThreshold());

            UserAchievement ua = unlocked.get(a.getId());
            if (ua != null) {
                dto.setUnlocked(true);
                dto.setUnlockedAt(ua.getUnlockedAt());
                dto.setCurrentValue(a.getThreshold());
            } else {
                dto.setUnlocked(false);
                dto.setCurrentValue(computeProgress(a.getCode(), totalCheckins, dishKinds, currentStreak, hasFiveStar, allMealsDay));
            }
            result.add(dto);
        }
        return result;
    }

    /** 幂等解锁检查，返回新解锁的成就列表 */
    @Transactional
    @CacheEvict(value = "userAchievements", key = "#userId")
    public List<AchievementDto> checkAndUnlock(Long userId) {
        List<Achievement> all = achievementMapper.selectList(null);
        Set<Long> alreadyUnlocked = userAchievementMapper.selectList(
            new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId)
        ).stream().map(UserAchievement::getAchievementId).collect(Collectors.toSet());

        int totalCheckins = statsMapper.countCheckins(userId);
        int dishKinds = statsMapper.countDishKinds(userId);
        int currentStreak = computeCurrentStreak(userId);
        boolean hasFiveStar = hasRating(userId, 5);
        boolean allMealsDay = hasAllMealsInOneDay(userId);

        List<AchievementDto> newlyUnlocked = new ArrayList<>();
        for (Achievement a : all) {
            if (alreadyUnlocked.contains(a.getId())) continue;
            if (shouldUnlock(a.getCode(), totalCheckins, dishKinds, currentStreak, hasFiveStar, allMealsDay)) {
                UserAchievement ua = new UserAchievement();
                ua.setUserId(userId);
                ua.setAchievementId(a.getId());
                ua.setUnlockedAt(LocalDateTime.now());
                userAchievementMapper.insert(ua);

                AchievementDto dto = new AchievementDto();
                dto.setId(a.getId());
                dto.setCode(a.getCode());
                dto.setName(a.getName());
                dto.setDescription(a.getDescription());
                dto.setIcon(a.getIcon());
                dto.setCategory(a.getCategory());
                dto.setThreshold(a.getThreshold());
                dto.setUnlocked(true);
                dto.setUnlockedAt(ua.getUnlockedAt());
                dto.setCurrentValue(a.getThreshold());
                newlyUnlocked.add(dto);

                log.info("User {} unlocked achievement: {}", userId, a.getCode());
            }
        }
        return newlyUnlocked;
    }

    private boolean shouldUnlock(String code, int totalCheckins, int dishKinds,
                                  int currentStreak, boolean hasFiveStar, boolean allMealsDay) {
        return switch (code) {
            case "first_checkin" -> totalCheckins >= 1;
            case "streak_7"      -> currentStreak >= 7;
            case "streak_30"     -> currentStreak >= 30;
            case "streak_100"    -> currentStreak >= 100;
            case "variety_10"    -> dishKinds >= 10;
            case "variety_30"    -> dishKinds >= 30;
            case "variety_50"    -> dishKinds >= 50;
            case "rating_5star"  -> hasFiveStar;
            case "checkin_50"    -> totalCheckins >= 50;
            case "checkin_100"   -> totalCheckins >= 100;
            case "checkin_200"   -> totalCheckins >= 200;
            case "all_meals_day" -> allMealsDay;
            default -> false;
        };
    }

    private int computeProgress(String code, int totalCheckins, int dishKinds,
                                 int currentStreak, boolean hasFiveStar, boolean allMealsDay) {
        return switch (code) {
            case "first_checkin" -> Math.min(totalCheckins, 1);
            case "streak_7"      -> Math.min(currentStreak, 7);
            case "streak_30"     -> Math.min(currentStreak, 30);
            case "streak_100"    -> Math.min(currentStreak, 100);
            case "variety_10"    -> Math.min(dishKinds, 10);
            case "variety_30"    -> Math.min(dishKinds, 30);
            case "variety_50"    -> Math.min(dishKinds, 50);
            case "rating_5star"  -> hasFiveStar ? 1 : 0;
            case "checkin_50"    -> Math.min(totalCheckins, 50);
            case "checkin_100"   -> Math.min(totalCheckins, 100);
            case "checkin_200"   -> Math.min(totalCheckins, 200);
            case "all_meals_day" -> allMealsDay ? 1 : 0;
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
