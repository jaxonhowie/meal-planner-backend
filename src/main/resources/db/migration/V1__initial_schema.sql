-- V1: 初始化完整 schema（含 tags 列）
-- Flyway 保证此脚本只执行一次；不包含任何 DROP 语句

-- 家庭表
CREATE TABLE IF NOT EXISTS `family` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '家庭ID',
  `name`        VARCHAR(100) NOT NULL                COMMENT '家庭名称',
  `invite_code` VARCHAR(10)  NOT NULL                COMMENT '邀请码（8位）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭表';

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名',
  `password`   VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt加密）',
  `nickname`   VARCHAR(50)                          COMMENT '昵称',
  `family_id`  BIGINT                               COMMENT '所属家庭ID',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_username` (`username`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 每日餐食计划
CREATE TABLE IF NOT EXISTS `meal_plan` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `family_id`  BIGINT       NOT NULL                COMMENT '家庭ID',
  `user_id`    BIGINT       NOT NULL                COMMENT '创建人ID',
  `date`       DATE         NOT NULL                COMMENT '日期',
  `meal_type`  VARCHAR(20)  NOT NULL                COMMENT '餐次: breakfast/lunch/dinner',
  `status`     VARCHAR(20)  NOT NULL DEFAULT 'planned' COMMENT '状态: planned/done/skipped',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_family_date_meal` (`family_id`, `date`, `meal_type`),
  KEY `idx_family_date` (`family_id`, `date`)
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
  `user_id`     BIGINT       NOT NULL                COMMENT '打卡人ID',
  `description` TEXT                                 COMMENT '打卡描述',
  `rating`      TINYINT      NOT NULL DEFAULT 3      COMMENT '评分 1-5',
  `image_url`   VARCHAR(500)                         COMMENT '图片链接',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_user_date` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录';

-- 菜品库（user_id=0 表示系统预设）
CREATE TABLE IF NOT EXISTS `dish_library` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `user_id`       BIGINT       NOT NULL DEFAULT 0     COMMENT '用户ID，0=系统预设',
  `name`          VARCHAR(200) NOT NULL                COMMENT '菜名',
  `meal_type`     VARCHAR(20)                          COMMENT '适用餐次: breakfast/lunch/dinner，null=通用',
  `image_url`     VARCHAR(500)                         COMMENT '示例图片',
  `checkin_count` INT          NOT NULL DEFAULT 0      COMMENT '打卡次数',
  `tags`          VARCHAR(200)                         COMMENT '标签，逗号分隔，如：荤,辣',
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_meal_type` (`user_id`, `meal_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品库';

-- 系统预设菜品（INSERT IGNORE：已存在则跳过，不重复插入）
INSERT IGNORE INTO `dish_library` (id, user_id, name, meal_type, tags) VALUES
-- 早餐
(1,  0, '豆浆油条',     'breakfast', '素'),
(2,  0, '皮蛋瘦肉粥',   'breakfast', '荤'),
(3,  0, '煎饼果子',     'breakfast', '素'),
(4,  0, '小笼包',       'breakfast', '荤'),
(5,  0, '阳春面',       'breakfast', '素'),
(6,  0, '荷包蛋拌面',   'breakfast', '素'),
(7,  0, '番茄鸡蛋面',   'breakfast', '素'),
(8,  0, '馒头配咸菜',   'breakfast', '素'),
-- 午餐
(9,  0, '红烧肉',       'lunch',     '荤'),
(10, 0, '宫保鸡丁',     'lunch',     '荤,辣'),
(11, 0, '麻婆豆腐',     'lunch',     '素,辣'),
(12, 0, '鱼香肉丝',     'lunch',     '荤,辣'),
(13, 0, '糖醋排骨',     'lunch',     '荤'),
(14, 0, '回锅肉',       'lunch',     '荤,辣'),
(15, 0, '扬州炒饭',     'lunch',     '荤'),
(16, 0, '番茄炒蛋盖饭', 'lunch',     '素'),
-- 晚餐
(17, 0, '清蒸鱼',       'dinner',    '荤,清淡'),
(18, 0, '白切鸡',       'dinner',    '荤,清淡'),
(19, 0, '蒜蓉虾',       'dinner',    '荤,清淡'),
(20, 0, '水煮肉片',     'dinner',    '荤,辣'),
(21, 0, '红烧豆腐',     'dinner',    '素'),
(22, 0, '清炒时蔬',     'dinner',    '素,清淡'),
(23, 0, '砂锅粥',       'dinner',    '素,清淡'),
(24, 0, '酸辣汤',       'dinner',    '素,辣'),
-- 通用
(25, 0, '番茄炒蛋',     null,        '素'),
(26, 0, '炒青菜',       null,        '素,清淡'),
(27, 0, '蒸蛋',         null,        '素,清淡'),
(28, 0, '泡面加蛋',     null,        '素');
