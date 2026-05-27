package com.mealplanner.mapper;

import com.mealplanner.mapper.BaseMapperX;
import com.mealplanner.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapperX<User> {
}
