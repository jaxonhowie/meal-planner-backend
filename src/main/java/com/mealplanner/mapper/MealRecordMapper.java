package com.mealplanner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealplanner.entity.MealRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MealRecordMapper extends BaseMapper<MealRecord> {
}
