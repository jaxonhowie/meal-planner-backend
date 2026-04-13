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

-- 菜品库（系统预设，user_id=0 表示系统菜品）
CREATE TABLE IF NOT EXISTS `dish_library` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `user_id`       BIGINT       NOT NULL DEFAULT 0     COMMENT '用户ID，0=系统预设',
  `name`          VARCHAR(200) NOT NULL                COMMENT '菜名',
  `meal_type`     VARCHAR(20)                          COMMENT '适用餐次: breakfast/lunch/dinner，null=通用',
  `image_url`     VARCHAR(500)                         COMMENT '示例图片（取自最近一次打卡）',
  `checkin_count` INT          NOT NULL DEFAULT 0      COMMENT '打卡次数',
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_meal_type` (`user_id`, `meal_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品库';

-- 预设菜品（系统通用，INSERT IGNORE 保证幂等）
INSERT IGNORE INTO `dish_library` (id, user_id, name, meal_type) VALUES
-- 早餐
(1,  0, '豆浆油条',     'breakfast'),
(2,  0, '皮蛋瘦肉粥',   'breakfast'),
(3,  0, '煎饼果子',     'breakfast'),
(4,  0, '小笼包',       'breakfast'),
(5,  0, '阳春面',       'breakfast'),
(6,  0, '荷包蛋拌面',   'breakfast'),
(7,  0, '番茄鸡蛋面',   'breakfast'),
(8,  0, '馒头配咸菜',   'breakfast'),
-- 午餐
(9,  0, '红烧肉',       'lunch'),
(10, 0, '宫保鸡丁',     'lunch'),
(11, 0, '麻婆豆腐',     'lunch'),
(12, 0, '鱼香肉丝',     'lunch'),
(13, 0, '糖醋排骨',     'lunch'),
(14, 0, '回锅肉',       'lunch'),
(15, 0, '扬州炒饭',     'lunch'),
(16, 0, '番茄炒蛋盖饭', 'lunch'),
-- 晚餐
(17, 0, '清蒸鱼',       'dinner'),
(18, 0, '白切鸡',       'dinner'),
(19, 0, '蒜蓉虾',       'dinner'),
(20, 0, '水煮肉片',     'dinner'),
(21, 0, '红烧豆腐',     'dinner'),
(22, 0, '清炒时蔬',     'dinner'),
(23, 0, '砂锅粥',       'dinner'),
(24, 0, '酸辣汤',       'dinner'),
-- 通用
(25, 0, '番茄炒蛋',     null),
(26, 0, '炒青菜',       null),
(27, 0, '蒸蛋',         null),
(28, 0, '泡面加蛋',     null);

-- 测试数据
INSERT IGNORE INTO `user` (username, password) VALUES ('demo', '$2a$10$demohashedpassword');
