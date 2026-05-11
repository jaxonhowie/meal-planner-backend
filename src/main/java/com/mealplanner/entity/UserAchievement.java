package com.mealplanner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_achievement")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long achievementId;
    private LocalDateTime unlockedAt;
}
