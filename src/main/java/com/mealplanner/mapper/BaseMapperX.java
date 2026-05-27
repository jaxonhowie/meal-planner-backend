package com.mealplanner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface BaseMapperX<T> extends BaseMapper<T> {

    int insertBatchSomeColumn(@Param("list") Collection<T> entityList);
}
