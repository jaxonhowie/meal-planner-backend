package com.mealplanner.mapper;

import com.mealplanner.dto.DayCount;
import com.mealplanner.dto.DishStat;
import com.mealplanner.dto.MealTypeCount;
import com.mealplanner.dto.WeeklyRating;
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

    @Select("SELECT ROUND(AVG(rating), 1) FROM meal_record WHERE user_id = #{userId}")
    Double overallAvgRating(@Param("userId") Long userId);

    /**
     * 按平均评分取 Top5（通过 plan_dish → meal_plan → meal_record 关联）
     */
    @Select("SELECT pd.dish_name AS dishName, " +
            "AVG(mr.rating) AS avgRating, COUNT(DISTINCT mr.id) AS checkinCount " +
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
            "LIMIT 8")
    List<DishStat> topByFrequency(@Param("userId") Long userId);

    /**
     * 近30天每日打卡次数（稀疏：无打卡的日期不返回）
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM meal_record " +
            "WHERE user_id = #{userId} " +
            "  AND created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d') " +
            "ORDER BY date ASC")
    List<DayCount> checkinTrendLast30Days(@Param("userId") Long userId);

    /**
     * 近91天每日打卡次数，用于热力图（稀疏数据）
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM meal_record " +
            "WHERE user_id = #{userId} " +
            "  AND created_at >= DATE_SUB(CURDATE(), INTERVAL 91 DAY) " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d') " +
            "ORDER BY date ASC")
    List<DayCount> checkinHeatmap91Days(@Param("userId") Long userId);

    /**
     * 所有打卡的不重复日期（降序），用于计算连续打卡天数
     */
    @Select("SELECT DISTINCT DATE_FORMAT(created_at, '%Y-%m-%d') AS date " +
            "FROM meal_record " +
            "WHERE user_id = #{userId} " +
            "ORDER BY date DESC")
    List<String> allCheckinDates(@Param("userId") Long userId);

    /**
     * 餐次分布（早/午/晚各打卡多少次）
     */
    @Select("SELECT mp.meal_type AS mealType, COUNT(*) AS count " +
            "FROM meal_record mr " +
            "JOIN meal_plan mp ON mr.plan_id = mp.id " +
            "WHERE mr.user_id = #{userId} " +
            "GROUP BY mp.meal_type")
    List<MealTypeCount> mealTypeDistribution(@Param("userId") Long userId);

    /**
     * 近8周每周平均评分（按周一分组）
     */
    @Select("SELECT " +
            "  DATE_FORMAT(DATE_SUB(created_at, INTERVAL WEEKDAY(created_at) DAY), '%m/%d') AS weekLabel, " +
            "  ROUND(AVG(rating), 1) AS avgRating, " +
            "  COUNT(*) AS checkinCount " +
            "FROM meal_record " +
            "WHERE user_id = #{userId} " +
            "  AND created_at >= DATE_SUB(CURDATE(), INTERVAL 8 WEEK) " +
            "GROUP BY DATE(DATE_SUB(created_at, INTERVAL WEEKDAY(created_at) DAY)) " +
            "ORDER BY MIN(created_at) ASC")
    List<WeeklyRating> ratingTrendLast8Weeks(@Param("userId") Long userId);
}
