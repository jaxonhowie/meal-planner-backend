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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanMapper mealPlanMapper;
    private final MealRecordMapper mealRecordMapper;
    private final PlanDishMapper planDishMapper;

    /**
     * 查询指定用户某日的菜单
     */
    public List<MealPlan> getDailyPlan(Long userId, LocalDate date) {
        List<MealPlan> plans = mealPlanMapper.selectList(
            new LambdaQueryWrapper<MealPlan>()
                .eq(MealPlan::getUserId, userId)
                .eq(MealPlan::getDate, date)
                .orderByAsc(MealPlan::getMealType)
        );
        attachDishes(plans);
        return plans;
    }

    /**
     * 生成当日菜单（工作日仅晚餐，周末三餐）
     * 使用数据库唯一约束防重，避免并发问题
     */
    @Transactional
    public List<MealPlan> generateDailyPlan(Long userId, LocalDate date) {
        List<String> meals = isWeekendOrHoliday(date)
            ? List.of("breakfast", "lunch", "dinner")
            : List.of("dinner");

        List<MealPlan> created = new ArrayList<>();
        for (String meal : meals) {
            MealPlan plan = new MealPlan();
            plan.setUserId(userId);
            plan.setDate(date);
            plan.setMealType(meal);
            plan.setStatus("planned");
            try {
                mealPlanMapper.insert(plan);
                created.add(plan);
            } catch (DuplicateKeyException e) {
                log.debug("Plan already exists for userId={} date={} meal={}", userId, date, meal);
            }
        }
        // 返回该日完整菜单（无论是否新建）
        return getDailyPlan(userId, date);
    }

    /**
     * 更新计划关联的菜品列表（先删后增）
     */
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

    /**
     * 更新计划状态 (planned / done / skipped)
     */
    public MealPlan updateStatus(Long planId, String status) {
        MealPlan plan = mealPlanMapper.selectById(planId);
        if (plan == null) throw new RuntimeException("计划不存在: " + planId);
        plan.setStatus(status);
        mealPlanMapper.updateById(plan);
        attachDishes(List.of(plan));
        return plan;
    }

    /**
     * 新增打卡记录
     */
    @Transactional
    public MealRecord addRecord(Long planId, Long userId, String description, Integer rating, String imageUrl) {
        MealPlan plan = mealPlanMapper.selectById(planId);
        if (plan == null) throw new RuntimeException("计划不存在: " + planId);

        MealRecord record = new MealRecord();
        record.setPlanId(planId);
        record.setUserId(userId);
        record.setDescription(description);
        record.setRating(rating != null ? rating : 3);
        record.setImageUrl(imageUrl);
        mealRecordMapper.insert(record);

        // 同步更新计划状态为 done
        plan.setStatus("done");
        mealPlanMapper.updateById(plan);

        return record;
    }

    /**
     * 查询某日打卡记录
     */
    public List<MealRecord> getRecords(Long userId, LocalDate date) {
        // 先找该日所有 planId
        List<MealPlan> plans = getDailyPlan(userId, date);
        if (plans.isEmpty()) return List.of();

        List<Long> planIds = plans.stream().map(MealPlan::getId).toList();
        return mealRecordMapper.selectList(
            new LambdaQueryWrapper<MealRecord>()
                .in(MealRecord::getPlanId, planIds)
                .orderByDesc(MealRecord::getCreatedAt)
        );
    }

    // ── 私有方法 ──────────────────────────────────────────

    /**
     * 为计划列表批量填充关联菜品
     */
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

    /**
     * 判断是否为周末（后续可扩展为节假日 API）
     */
    private boolean isWeekendOrHoliday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
