package com.mealplanner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealplanner.entity.MealPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MealPlanMapper extends BaseMapper<MealPlan> {
}
