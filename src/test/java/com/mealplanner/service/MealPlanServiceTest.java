package com.mealplanner.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mealplanner.dto.DishItem;
import com.mealplanner.entity.MealPlan;
import com.mealplanner.entity.MealRecord;
import com.mealplanner.entity.PlanDish;
import com.mealplanner.mapper.MealPlanMapper;
import com.mealplanner.mapper.MealRecordMapper;
import com.mealplanner.mapper.PlanDishMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

    @Mock
    private MealPlanMapper mealPlanMapper;
    @Mock
    private MealRecordMapper mealRecordMapper;
    @Mock
    private PlanDishMapper planDishMapper;
    @Mock
    private DishLibraryService dishLibraryService;
    @Mock
    private AchievementService achievementService;

    @InjectMocks
    private MealPlanService mealPlanService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, MealPlan.class);
        TableInfoHelper.initTableInfo(assistant, MealRecord.class);
        TableInfoHelper.initTableInfo(assistant, PlanDish.class);
    }

    @Test
    void getDailyPlan_success() {
        MealPlan plan = new MealPlan();
        plan.setId(1L);
        plan.setFamilyId(10L);
        plan.setDate(LocalDate.now());
        plan.setMealType("dinner");

        PlanDish dish = new PlanDish();
        dish.setPlanId(1L);
        dish.setDishName("红烧肉");
        dish.setSortOrder(0);

        when(mealPlanMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(plan));
        when(planDishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(dish));

        List<MealPlan> result = mealPlanService.getDailyPlan(10L, LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDishes()).hasSize(1);
        assertThat(result.get(0).getDishes().get(0).getDishName()).isEqualTo("红烧肉");
    }

    @Test
    void getDailyPlan_empty() {
        when(mealPlanMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<MealPlan> result = mealPlanService.getDailyPlan(10L, LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    void updateDishes_success() {
        MealPlan plan = new MealPlan();
        plan.setId(1L);

        when(mealPlanMapper.selectById(1L)).thenReturn(plan);
        when(planDishMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        when(planDishMapper.insert(any(PlanDish.class))).thenReturn(1);
        when(planDishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        DishItem item = new DishItem();
        item.setDishName("红烧肉");
        item.setRemark("少放盐");

        MealPlan result = mealPlanService.updateDishes(1L, List.of(item));

        assertThat(result).isNotNull();
        verify(planDishMapper).delete(any(LambdaQueryWrapper.class));
        verify(planDishMapper).insert(any(PlanDish.class));
    }

    @Test
    void updateDishes_planNotFound() {
        when(mealPlanMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> mealPlanService.updateDishes(999L, List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("计划不存在: 999");
    }

    @Test
    void updateStatus_success() {
        MealPlan plan = new MealPlan();
        plan.setId(1L);
        plan.setStatus("planned");

        when(mealPlanMapper.selectById(1L)).thenReturn(plan);
        when(mealPlanMapper.updateById(any(MealPlan.class))).thenReturn(1);
        when(planDishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        MealPlan result = mealPlanService.updateStatus(1L, "done");

        assertThat(result.getStatus()).isEqualTo("done");
        verify(mealPlanMapper).updateById(plan);
    }

    @Test
    void updateStatus_planNotFound() {
        when(mealPlanMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> mealPlanService.updateStatus(999L, "done"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("计划不存在: 999");
    }

    @Test
    void addRecord_success() {
        MealPlan plan = new MealPlan();
        plan.setId(1L);
        plan.setMealType("dinner");

        PlanDish dish = new PlanDish();
        dish.setDishName("红烧肉");

        when(mealPlanMapper.selectById(1L)).thenReturn(plan);
        when(mealRecordMapper.insert(any(MealRecord.class))).thenReturn(1);
        when(planDishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(dish));
        when(mealPlanMapper.updateById(any(MealPlan.class))).thenReturn(1);

        MealRecord result = mealPlanService.addRecord(1L, 1L, "好吃", 5, "img.jpg");

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getDescription()).isEqualTo("好吃");
        assertThat(plan.getStatus()).isEqualTo("done");
        verify(dishLibraryService).recordCheckin(1L, "红烧肉", "dinner", "img.jpg");
        verify(achievementService).checkAndUnlock(1L);
    }

    @Test
    void addRecord_defaultRating() {
        MealPlan plan = new MealPlan();
        plan.setId(1L);
        plan.setMealType("dinner");

        when(mealPlanMapper.selectById(1L)).thenReturn(plan);
        when(mealRecordMapper.insert(any(MealRecord.class))).thenReturn(1);
        when(planDishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(mealPlanMapper.updateById(any(MealPlan.class))).thenReturn(1);

        MealRecord result = mealPlanService.addRecord(1L, 1L, null, null, null);

        assertThat(result.getRating()).isEqualTo(3);
    }

    @Test
    void addRecord_planNotFound() {
        when(mealPlanMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> mealPlanService.addRecord(999L, 1L, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("计划不存在: 999");
    }

    @Test
    void addRecord_achievementCheckFails() {
        MealPlan plan = new MealPlan();
        plan.setId(1L);
        plan.setMealType("dinner");

        when(mealPlanMapper.selectById(1L)).thenReturn(plan);
        when(mealRecordMapper.insert(any(MealRecord.class))).thenReturn(1);
        when(planDishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(mealPlanMapper.updateById(any(MealPlan.class))).thenReturn(1);
        doThrow(new RuntimeException("成就服务异常")).when(achievementService).checkAndUnlock(1L);

        MealRecord result = mealPlanService.addRecord(1L, 1L, null, 3, null);

        assertThat(result).isNotNull();
        assertThat(plan.getStatus()).isEqualTo("done");
    }

    @Test
    void updateRecord_success() {
        MealRecord record = new MealRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setRating(3);

        when(mealRecordMapper.selectById(1L)).thenReturn(record);
        when(mealRecordMapper.updateById(any(MealRecord.class))).thenReturn(1);

        MealRecord result = mealPlanService.updateRecord(1L, 1L, "新描述", 5, "new.jpg");

        assertThat(result.getDescription()).isEqualTo("新描述");
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getImageUrl()).isEqualTo("new.jpg");
    }

    @Test
    void updateRecord_notFound() {
        when(mealRecordMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> mealPlanService.updateRecord(999L, 1L, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("记录不存在: 999");
    }

    @Test
    void updateRecord_wrongUser() {
        MealRecord record = new MealRecord();
        record.setId(1L);
        record.setUserId(1L);

        when(mealRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> mealPlanService.updateRecord(1L, 2L, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权限修改");
    }
}
