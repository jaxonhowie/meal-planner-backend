package com.mealplanner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_dish")
public class PlanDish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private String dishName;

    private String remark;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
