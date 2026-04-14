package com.mealplanner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("meal_plan")
public class MealPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属家庭（家庭共享菜单） */
    private Long familyId;

    /** 创建人 */
    private Long userId;

    private LocalDate date;

    /** breakfast / lunch / dinner */
    private String mealType;

    /** planned / done / skipped */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 关联菜品列表（非数据库字段） */
    @TableField(exist = false)
    private List<PlanDish> dishes;
}
