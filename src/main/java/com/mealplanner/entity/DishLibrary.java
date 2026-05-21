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

    /** 标签，逗号分隔，如："荤,辣"；null 表示未设置 */
    private String tags;

    /** 是否收藏 */
    private Boolean isFavorite;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
