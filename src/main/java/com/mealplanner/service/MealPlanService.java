package com.mealplanner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealplanner.dto.DishItem;
import com.mealplanner.entity.MealPlan;
import com.mealplanner.entity.MealRecord;
import com.mealplanner.entity.PlanDish;
import com.mealplanner.mapper.MealPlanMapper;
import com.mealplanner.mapper.MealRecordMapper;
import com.mealplanner.mapper.PlanDishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanMapper mealPlanMapper;
    private final MealRecordMapper mealRecordMapper;
    private final PlanDishMapper planDishMapper;
    private final DishLibraryService dishLibraryService;
    private final AchievementService achievementService;

    /** 查询指定家庭某日的菜单 */
    public List<MealPlan> getDailyPlan(Long familyId, LocalDate date) {
        List<MealPlan> plans = mealPlanMapper.selectList(
            new LambdaQueryWrapper<MealPlan>()
                .eq(MealPlan::getFamilyId, familyId)
                .eq(MealPlan::getDate, date)
                .orderByAsc(MealPlan::getMealType)
        );
        attachDishes(plans);
        return plans;
    }

    /** 生成当日菜单
     *  - 三餐均创建计划（保证所有卡片可编辑）
     *  - 工作日仅对晚餐自动填充菜品，早/午餐创建空计划
     *  - 周末三餐均自动填充菜品
     *  - 已打卡（done）的餐次保持不动
     *  - 未打卡的已有计划：清空旧菜品后重新推荐
     */
    @Transactional
    public List<MealPlan> generateDailyPlan(Long familyId, Long userId, LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;

        // 查询近7天已出现的菜品名，推荐时排除
        List<Long> recentPlanIds = mealPlanMapper.selectList(
            new LambdaQueryWrapper<MealPlan>()
                .eq(MealPlan::getFamilyId, familyId)
                .between(MealPlan::getDate, date.minusDays(7), date.minusDays(1)))
            .stream().map(MealPlan::getId).toList();
        Set<String> excludeNames = recentPlanIds.isEmpty() ? Collections.emptySet()
            : planDishMapper.selectList(new LambdaQueryWrapper<PlanDish>()
                .in(PlanDish::getPlanId, recentPlanIds))
              .stream().map(PlanDish::getDishName).collect(Collectors.toSet());

        for (String meal : List.of("breakfast", "lunch", "dinner")) {
            // 工作日早/午餐：只建空计划，不自动填菜
            boolean autoFill = isWeekend || "dinner".equals(meal);

            MealPlan existing = mealPlanMapper.selectOne(
                new LambdaQueryWrapper<MealPlan>()
                    .eq(MealPlan::getFamilyId, familyId)
                    .eq(MealPlan::getDate, date)
                    .eq(MealPlan::getMealType, meal)
            );
            if (existing != null) {
                // 已打卡：保留不动
                if ("done".equals(existing.getStatus())) continue;
                // 未打卡：仅在需要自动填充时重新生成菜品
                if (autoFill) {
                    planDishMapper.delete(
                        new LambdaQueryWrapper<PlanDish>().eq(PlanDish::getPlanId, existing.getId())
                    );
                    autoFillDishes(existing, familyId, meal, excludeNames);
                }
            } else {
                MealPlan plan = new MealPlan();
                plan.setFamilyId(familyId);
                plan.setUserId(userId);
                plan.setDate(date);
                plan.setMealType(meal);
                plan.setStatus("planned");
                mealPlanMapper.insert(plan);
                if (autoFill) {
                    autoFillDishes(plan, familyId, meal, excludeNames);
                }
            }
        }
        return getDailyPlan(familyId, date);
    }

    /** 手动创建单餐空计划（无初始菜品，幂等：已存在则直接返回） */
    @Transactional
    public MealPlan createSinglePlan(Long familyId, Long userId, LocalDate date, String mealType) {
        MealPlan plan = new MealPlan();
        plan.setFamilyId(familyId);
        plan.setUserId(userId);
        plan.setDate(date);
        plan.setMealType(mealType);
        plan.setStatus("planned");
        try {
            mealPlanMapper.insert(plan);
        } catch (DuplicateKeyException e) {
            plan = mealPlanMapper.selectOne(
                new LambdaQueryWrapper<MealPlan>()
                    .eq(MealPlan::getFamilyId, familyId)
                    .eq(MealPlan::getDate, date)
                    .eq(MealPlan::getMealType, mealType)
            );
        }
        attachDishes(List.of(plan));
        return plan;
    }

    /** 更新计划关联的菜品列表（先删后增） */
    @Transactional
    public MealPlan updateDishes(Long planId, List<DishItem> dishItems) {
        MealPlan plan = mealPlanMapper.selectById(planId);
        if (plan == null) throw new RuntimeException("计划不存在: " + planId);

        planDishMapper.delete(
            new LambdaQueryWrapper<PlanDish>().eq(PlanDish::getPlanId, planId)
        );
        for (int i = 0; i < dishItems.size(); i++) {
            DishItem item = dishItems.get(i);
            PlanDish dish = new PlanDish();
            dish.setPlanId(planId);
            dish.setDishName(item.getDishName());
            dish.setRemark(item.getRemark());
            dish.setSortOrder(i);
            planDishMapper.insert(dish);
        }
        attachDishes(List.of(plan));
        return plan;
    }

    /** 更新计划状态 */
    public MealPlan updateStatus(Long planId, String status) {
        MealPlan plan = mealPlanMapper.selectById(planId);
        if (plan == null) throw new RuntimeException("计划不存在: " + planId);
        plan.setStatus(status);
        mealPlanMapper.updateById(plan);
        attachDishes(List.of(plan));
        return plan;
    }

    /** 新增打卡记录，并将本餐菜品同步到菜库 */
    @Transactional
    public MealRecord addRecord(Long planId, Long userId, String description,
                                Integer rating, String imageUrl) {
        MealPlan plan = mealPlanMapper.selectById(planId);
        if (plan == null) throw new RuntimeException("计划不存在: " + planId);

        MealRecord record = new MealRecord();
        record.setPlanId(planId);
        record.setUserId(userId);
        record.setDescription(description);
        record.setRating(rating != null ? rating : 3);
        record.setImageUrl(imageUrl);
        mealRecordMapper.insert(record);

        List<PlanDish> dishes = planDishMapper.selectList(
            new LambdaQueryWrapper<PlanDish>().eq(PlanDish::getPlanId, planId)
        );
        for (PlanDish dish : dishes) {
            dishLibraryService.recordCheckin(userId, dish.getDishName(),
                    plan.getMealType(), imageUrl);
        }

        plan.setStatus("done");
        mealPlanMapper.updateById(plan);

        // 触发成就解锁检查（非阻塞，失败不影响打卡）
        try {
            achievementService.checkAndUnlock(userId);
        } catch (Exception e) {
            log.warn("成就检查失败: {}", e.getMessage());
        }

        return record;
    }

    /** 查询指定家庭某周期间的所有菜单 */
    public List<MealPlan> getWeeklyPlans(Long familyId, LocalDate start, LocalDate end) {
        List<MealPlan> plans = mealPlanMapper.selectList(
            new LambdaQueryWrapper<MealPlan>()
                .eq(MealPlan::getFamilyId, familyId)
                .between(MealPlan::getDate, start, end)
                .orderByAsc(MealPlan::getDate)
                .orderByAsc(MealPlan::getMealType)
        );
        attachDishes(plans);
        return plans;
    }

    /** 查询某计划的打卡记录（最新的在前） */
    public List<MealRecord> getRecordsByPlanId(Long planId) {
        return mealRecordMapper.selectList(
            new LambdaQueryWrapper<MealRecord>()
                .eq(MealRecord::getPlanId, planId)
                .orderByDesc(MealRecord::getCreatedAt)
        );
    }

    /** 更新打卡记录（评分、描述、图片） */
    public MealRecord updateRecord(Long id, Long userId, String description, Integer rating, String imageUrl) {
        MealRecord record = mealRecordMapper.selectById(id);
        if (record == null) throw new RuntimeException("记录不存在: " + id);
        if (!record.getUserId().equals(userId)) throw new RuntimeException("无权限修改");
        record.setDescription(description);
        record.setRating(rating != null ? rating : record.getRating());
        record.setImageUrl(imageUrl);
        mealRecordMapper.updateById(record);
        return record;
    }

    /** 查询某日打卡记录（家庭所有成员） */
    public List<MealRecord> getRecords(Long familyId, LocalDate date) {
        List<MealPlan> plans = getDailyPlan(familyId, date);
        if (plans.isEmpty()) return List.of();

        List<Long> planIds = plans.stream().map(MealPlan::getId).toList();
        return mealRecordMapper.selectList(
            new LambdaQueryWrapper<MealRecord>()
                .in(MealRecord::getPlanId, planIds)
                .orderByDesc(MealRecord::getCreatedAt)
        );
    }

    /** 查询某段时间的打卡记录（家庭所有成员） */
    public List<MealRecord> getRecordsForRange(Long familyId, LocalDate start, LocalDate end) {
        List<MealPlan> plans = mealPlanMapper.selectList(
            new LambdaQueryWrapper<MealPlan>()
                .eq(MealPlan::getFamilyId, familyId)
                .between(MealPlan::getDate, start, end)
        );
        if (plans.isEmpty()) return List.of();
        List<Long> planIds = plans.stream().map(MealPlan::getId).toList();
        return mealRecordMapper.selectList(
            new LambdaQueryWrapper<MealRecord>()
                .in(MealRecord::getPlanId, planIds)
        );
    }

    // ── 私有方法 ──────────────────────────────

    private void attachDishes(List<MealPlan> plans) {
        if (plans.isEmpty()) return;
        List<Long> planIds = plans.stream().map(MealPlan::getId).toList();
        List<PlanDish> allDishes = planDishMapper.selectList(
            new LambdaQueryWrapper<PlanDish>()
                .in(PlanDish::getPlanId, planIds)
                .orderByAsc(PlanDish::getSortOrder)
        );
        Map<Long, List<PlanDish>> dishMap = allDishes.stream()
            .collect(Collectors.groupingBy(PlanDish::getPlanId));
        plans.forEach(p -> p.setDishes(dishMap.getOrDefault(p.getId(), List.of())));
    }

    private void autoFillDishes(MealPlan plan, Long familyId, String mealType, Set<String> excludeNames) {
        int count = "breakfast".equals(mealType) ? 1 : 2;
        List<String> names = dishLibraryService.randomForFamily(familyId, mealType, count, excludeNames);
        for (int i = 0; i < names.size(); i++) {
            PlanDish dish = new PlanDish();
            dish.setPlanId(plan.getId());
            dish.setDishName(names.get(i));
            dish.setSortOrder(i);
            planDishMapper.insert(dish);
        }
    }

}
