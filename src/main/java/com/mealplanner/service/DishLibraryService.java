package com.mealplanner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealplanner.entity.DishLibrary;
import com.mealplanner.entity.User;
import com.mealplanner.mapper.DishLibraryMapper;
import com.mealplanner.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DishLibraryService {

    private final DishLibraryMapper dishLibraryMapper;
    private final UserMapper userMapper;

    /**
     * 查询菜库（系统预设 + 家庭成员自定义）
     */
    @Cacheable(value = "dishLibrary", key = "T(String).valueOf(#userId).concat(':').concat(#mealType ?: 'all')")
    public List<DishLibrary> listForUser(Long userId, String mealType) {
        List<Long> userIds = familyUserIds(userId);
        return dishLibraryMapper.selectList(
            new LambdaQueryWrapper<DishLibrary>()
                .in(DishLibrary::getUserId, userIds)
                .and(mealType != null, w -> w
                    .eq(DishLibrary::getMealType, mealType)
                    .or()
                    .isNull(DishLibrary::getMealType))
                .orderByDesc(DishLibrary::getUpdatedAt)
                .orderByDesc(DishLibrary::getId)
        );
    }

    /**
     * 加权随机推荐（Efraimidis-Spirakis）
     */
    public List<String> random(Long userId, String mealType, int count) {
        List<DishLibrary> candidates = listForUser(userId, mealType);
        return weightedRandom(candidates, count);
    }

    /**
     * 按家庭随机推荐（用于自动填充菜单）
     */
    public List<String> randomForFamily(Long familyId, String mealType, int count) {
        return randomForFamily(familyId, mealType, count, Collections.emptySet());
    }

    /**
     * 按家庭随机推荐，排除近期已吃菜品
     * 若排除后候选不足 count，回退到全量候选（防菜库过小）
     */
    public List<String> randomForFamily(Long familyId, String mealType, int count, Set<String> excludeNames) {
        List<Long> userIds = familyUserIdsByFamilyId(familyId);
        List<DishLibrary> candidates = dishLibraryMapper.selectList(
            new LambdaQueryWrapper<DishLibrary>()
                .in(DishLibrary::getUserId, userIds)
                .and(mealType != null, w -> w
                    .eq(DishLibrary::getMealType, mealType)
                    .or()
                    .isNull(DishLibrary::getMealType))
        );
        List<DishLibrary> filtered = candidates.stream()
            .filter(d -> excludeNames == null || !excludeNames.contains(d.getName()))
            .collect(Collectors.toList());
        List<DishLibrary> pool = filtered.size() >= count ? filtered : candidates;
        return weightedRandom(pool, count);
    }

    /**
     * 打卡时同步菜品到菜库
     */
    public void recordCheckin(Long userId, String dishName, String mealType, String imageUrl) {
        DishLibrary existing = dishLibraryMapper.selectOne(
            new LambdaQueryWrapper<DishLibrary>()
                .eq(DishLibrary::getUserId, userId)
                .eq(DishLibrary::getName, dishName)
                .last("LIMIT 1")
        );
        if (existing != null) {
            existing.setCheckinCount(existing.getCheckinCount() + 1);
            if (imageUrl != null) existing.setImageUrl(imageUrl);
            dishLibraryMapper.updateById(existing);
        } else {
            DishLibrary dish = new DishLibrary();
            dish.setUserId(userId);
            dish.setName(dishName);
            dish.setMealType(mealType);
            dish.setCheckinCount(1);
            dish.setImageUrl(imageUrl);
            dishLibraryMapper.insert(dish);
        }
    }

    /**
     * 批量打卡同步菜品到菜库（消除 N+1 查询）
     * 原来每道菜 2 条 SQL，现在 N 道菜只需 2-3 条 SQL
     */
    public void batchRecordCheckin(Long userId, List<String> dishNames, String mealType, String imageUrl) {
        if (dishNames == null || dishNames.isEmpty()) return;

        // 1. 一次查询所有已存在的菜品
        List<DishLibrary> existingList = dishLibraryMapper.selectList(
            new LambdaQueryWrapper<DishLibrary>()
                .eq(DishLibrary::getUserId, userId)
                .in(DishLibrary::getName, dishNames)
        );
        Map<String, DishLibrary> existingMap = existingList.stream()
            .collect(Collectors.toMap(DishLibrary::getName, d -> d, (a, b) -> a));

        // 2. 区分更新和新增
        List<DishLibrary> toUpdate = new ArrayList<>();
        List<DishLibrary> toInsert = new ArrayList<>();
        for (String name : dishNames) {
            DishLibrary existing = existingMap.get(name);
            if (existing != null) {
                existing.setCheckinCount(existing.getCheckinCount() + 1);
                if (imageUrl != null) existing.setImageUrl(imageUrl);
                toUpdate.add(existing);
            } else {
                DishLibrary dish = new DishLibrary();
                dish.setUserId(userId);
                dish.setName(name);
                dish.setMealType(mealType);
                dish.setCheckinCount(1);
                dish.setImageUrl(imageUrl);
                toInsert.add(dish);
            }
        }

        // 3. 批量更新
        for (DishLibrary d : toUpdate) {
            dishLibraryMapper.updateById(d);
        }
        // 4. 批量插入
        if (!toInsert.isEmpty()) {
            dishLibraryMapper.insertBatchSomeColumn(toInsert);
        }
    }

    /**
     * 新增自定义菜品（名称去重：系统库 + 本人已有的均不允许重名）
     */
    @CacheEvict(value = "dishLibrary", allEntries = true)
    public DishLibrary add(Long userId, String name, String mealType, String tags) {
        List<Long> userIds = familyUserIds(userId);
        long count = dishLibraryMapper.selectCount(
            new LambdaQueryWrapper<DishLibrary>()
                .in(DishLibrary::getUserId, userIds)
                .eq(DishLibrary::getName, name)
        );
        if (count > 0) throw new RuntimeException("菜品已存在: " + name);

        DishLibrary dish = new DishLibrary();
        dish.setUserId(userId);
        dish.setName(name);
        dish.setMealType(mealType);
        dish.setCheckinCount(0);
        dish.setTags(tags);
        dishLibraryMapper.insert(dish);
        return dish;
    }

    /** 更新菜品标签 */
    @CacheEvict(value = "dishLibrary", allEntries = true)
    public DishLibrary updateTags(Long id, String tags) {
        DishLibrary dish = dishLibraryMapper.selectById(id);
        if (dish == null) throw new RuntimeException("菜品不存在: " + id);
        dish.setTags(tags);
        dishLibraryMapper.updateById(dish);
        return dish;
    }

    /** 删除自定义菜品（只能删自己的） */
    @CacheEvict(value = "dishLibrary", allEntries = true)
    public void delete(Long userId, Long id) {
        dishLibraryMapper.delete(
            new LambdaQueryWrapper<DishLibrary>()
                .eq(DishLibrary::getId, id)
                .eq(DishLibrary::getUserId, userId)
        );
    }

    /** 更新菜品图片 */
    @CacheEvict(value = "dishLibrary", allEntries = true)
    public DishLibrary updateImage(Long id, String imageUrl) {
        DishLibrary dish = dishLibraryMapper.selectById(id);
        if (dish == null) throw new RuntimeException("菜品不存在: " + id);
        dish.setImageUrl(imageUrl);
        dishLibraryMapper.updateById(dish);
        return dish;
    }

    /** 删除菜品图片 */
    @CacheEvict(value = "dishLibrary", allEntries = true)
    public DishLibrary clearImage(Long id) {
        DishLibrary dish = dishLibraryMapper.selectById(id);
        if (dish == null) throw new RuntimeException("菜品不存在: " + id);
        dish.setImageUrl(null);
        dishLibraryMapper.updateById(dish);
        return dish;
    }

    /** 切换收藏状态 */
    @CacheEvict(value = "dishLibrary", allEntries = true)
    public DishLibrary toggleFavorite(Long id) {
        DishLibrary dish = dishLibraryMapper.selectById(id);
        if (dish == null) throw new RuntimeException("菜品不存在: " + id);
        dish.setIsFavorite(!Boolean.TRUE.equals(dish.getIsFavorite()));
        dishLibraryMapper.updateById(dish);
        return dish;
    }

    // ── 工具方法 ──────────────────────────────

    /** 系统(0) + 家庭成员 userId 列表 */
    private List<Long> familyUserIds(Long userId) {
        User user = userMapper.selectById(userId);
        return familyUserIdsByFamilyId(user != null ? user.getFamilyId() : null);
    }

    private List<Long> familyUserIdsByFamilyId(Long familyId) {
        List<Long> ids = new ArrayList<>();
        ids.add(0L); // 系统预设
        if (familyId != null) {
            userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getFamilyId, familyId)
                    .select(User::getId))
                .forEach(u -> ids.add(u.getId()));
        }
        return ids;
    }

    private List<String> weightedRandom(List<DishLibrary> candidates, int count) {
        if (candidates.isEmpty()) return List.of();
        return candidates.stream()
            .sorted(Comparator.comparingDouble(d -> {
                double weight = d.getCheckinCount() + 1;
                if (Boolean.TRUE.equals(d.getIsFavorite())) weight *= 2;
                return Math.log(ThreadLocalRandom.current().nextDouble()) / weight;
            }))
            .limit(count)
            .map(DishLibrary::getName)
            .collect(Collectors.toList());
    }
}
