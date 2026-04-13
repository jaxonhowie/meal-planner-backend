package com.mealplanner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dish_library")
public class DishLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** breakfast / lunch / dinner，null 表示通用 */
    private String mealType;

    /** 示例图片（取自最近一次打卡图片） */
    private String imageUrl;

    /** 打卡次数，用于加权推荐 */
    private Integer checkinCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
