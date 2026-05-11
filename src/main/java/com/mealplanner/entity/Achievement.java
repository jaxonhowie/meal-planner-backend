package com.mealplanner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("achievement")
public class Achievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Integer threshold;
    private Integer sortOrder;
}
