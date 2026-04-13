-- MealPlanner 数据库初始化脚本（幂等，安全重复执行）

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名',
  `password`   VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt加密）',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 每日餐食计划
CREATE TABLE IF NOT EXISTS `meal_plan` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id`    BIGINT       NOT NULL                COMMENT '用户ID',
  `date`       DATE         NOT NULL                COMMENT '日期',
  `meal_type`  VARCHAR(20)  NOT NULL                COMMENT '餐次: breakfast/lunch/dinner',
  `status`     VARCHAR(20)  NOT NULL DEFAULT 'planned' COMMENT '状态: planned/done/skipped',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_date_meal` (`user_id`, `date`, `meal_type`),
  KEY `idx_user_date` (`user_id`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日餐食计划';

-- 计划菜品（与 meal_plan 一对多）
CREATE TABLE IF NOT EXISTS `plan_dish` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `plan_id`    BIGINT       NOT NULL                COMMENT '关联计划ID',
  `dish_name`  VARCHAR(200) NOT NULL                COMMENT '菜名',
  `remark`     VARCHAR(500)                         COMMENT '备注（如：不辣、少放油）',
  `sort_order` INT          NOT NULL DEFAULT 0      COMMENT '排序序号',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划菜品';

-- 打卡记录
CREATE TABLE IF NOT EXISTS `meal_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `plan_id`     BIGINT       NOT NULL                COMMENT '关联计划ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
  `description` TEXT                                 COMMENT '打卡描述',
  `rating`      TINYINT      NOT NULL DEFAULT 3      COMMENT '评分 1-5',
  `image_url`   VARCHAR(255)                         COMMENT '图片链接',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_user_date` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录';

-- 测试数据
INSERT IGNORE INTO `user` (username, password) VALUES ('demo', '$2a$10$demohashedpassword');
