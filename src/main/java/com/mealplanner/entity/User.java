package com.mealplanner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密码，查询时默认不返回 */
    @TableField(select = false)
    private String password;

    private String nickname;

    private Long familyId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
