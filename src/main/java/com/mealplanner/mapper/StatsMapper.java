package com.mealplanner.mapper;

import com.mealplanner.dto.DishStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM meal_record WHERE user_id = #{userId}")
    int countCheckins(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM dish_library WHERE user_id = #{userId} AND checkin_count > 0")
    int countDishKinds(@Param("userId") Long userId);

    /**
     * 按平均评分取 Top5（通过 plan_dish → meal_plan → meal_record 关联）
     */
    @Select("SELECT pd.dish_name AS dishName, " +
            "AVG(mr.rating) AS avgRating, COUNT(*) AS checkinCount " +
            "FROM meal_record mr " +
            "JOIN meal_plan mp ON mr.plan_id = mp.id " +
            "JOIN plan_dish pd ON pd.plan_id = mp.id " +
            "WHERE mr.user_id = #{userId} " +
            "GROUP BY pd.dish_name " +
            "ORDER BY avgRating DESC, checkinCount DESC " +
            "LIMIT 5")
    List<DishStat> topByRating(@Param("userId") Long userId);

    /**
     * 按打卡频次取 Top5（直接读 dish_library）
     */
    @Select("SELECT name AS dishName, checkin_count AS checkinCount, 0.0 AS avgRating " +
            "FROM dish_library " +
            "WHERE user_id = #{userId} AND checkin_count > 0 " +
            "ORDER BY checkin_count DESC " +
            "LIMIT 5")
    List<DishStat> topByFrequency(@Param("userId") Long userId);
}
