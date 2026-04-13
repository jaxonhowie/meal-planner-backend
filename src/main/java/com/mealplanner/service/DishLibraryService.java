package com.mealplanner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealplanner.entity.DishLibrary;
import com.mealplanner.mapper.DishLibraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishLibraryService {

    private final DishLibraryMapper dishLibraryMapper;

    /**
     * 查询菜库（系统预设 + 用户自定义，按餐次过滤）
     */
    public List<DishLibrary> list(Long userId, String mealType) {
        return dishLibraryMapper.selectList(
            new LambdaQueryWrapper<DishLibrary>()
                .in(DishLibrary::getUserId, 0L, userId)
                .and(mealType != null, w -> w
                    .eq(DishLibrary::getMealType, mealType)
                    .or()
                    .isNull(DishLibrary::getMealType))
                .orderByAsc(DishLibrary::getId)
        );
    }

    /**
     * 加权随机推荐（Efraimidis-Spirakis 算法）
     * 打卡次数越多权重越高，权重 = checkinCount + 1（保证未吃过的菜也有机会）
     */
    public List<String> random(Long userId, String mealType, int count) {
        List<DishLibrary> candidates = list(userId, mealType);
        if (candidates.isEmpty()) return List.of();

        // 加权随机无重复抽样：key = -ln(U) / weight，取 key 最大的 count 个
        return candidates.stream()
            .sorted(Comparator.comparingDouble(d ->
                Math.log(ThreadLocalRandom.current().nextDouble()) / (d.getCheckinCount() + 1)))
            .limit(count)
            .map(DishLibrary::getName)
            .collect(Collectors.toList());
    }

    /**
     * 打卡时同步菜品到菜库：已有则打卡次数+1，没有则新建
     * 若本次打卡有图片，同步更新 imageUrl
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
     * 新增自定义菜品
     */
    public DishLibrary add(Long userId, String name, String mealType) {
        DishLibrary dish = new DishLibrary();
        dish.setUserId(userId);
        dish.setName(name);
        dish.setMealType(mealType);
        dish.setCheckinCount(0);
        dishLibraryMapper.insert(dish);
        return dish;
    }

    /**
     * 删除自定义菜品（只能删自己的，不能删系统预设）
     */
    public void delete(Long userId, Long id) {
        dishLibraryMapper.delete(
            new LambdaQueryWrapper<DishLibrary>()
                .eq(DishLibrary::getId, id)
                .eq(DishLibrary::getUserId, userId)
        );
    }
}
