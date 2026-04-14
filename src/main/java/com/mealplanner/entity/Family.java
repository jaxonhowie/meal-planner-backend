package com.mealplanner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("family")
public class Family {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 邀请码（8位随机字符串，唯一） */
    private String inviteCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
