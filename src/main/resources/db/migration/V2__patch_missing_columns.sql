-- V2: 为已有数据库补全后续添加的字段（幂等，使用 IF NOT EXISTS 条件逻辑）
-- 对新建数据库（V1 已创建完整 schema）同样安全：条件不满足时执行 SELECT 1 跳过

-- ── user 表 ─────────────────────────────────────────────────────────────────

-- user.nickname
SET @exist = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'nickname');
PREPARE _s FROM IF(@exist = 0,
  'ALTER TABLE `user` ADD COLUMN `nickname` VARCHAR(50) NULL AFTER `password`',
  'SELECT 1');
EXECUTE _s; DEALLOCATE PREPARE _s;

-- user.family_id
SET @exist = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'family_id');
PREPARE _s FROM IF(@exist = 0,
  'ALTER TABLE `user` ADD COLUMN `family_id` BIGINT NULL AFTER `nickname`',
  'SELECT 1');
EXECUTE _s; DEALLOCATE PREPARE _s;

-- ── family 表（若整张表不存在则创建）────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `family` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '家庭ID',
  `name`        VARCHAR(100) NOT NULL                COMMENT '家庭名称',
  `invite_code` VARCHAR(10)  NOT NULL                COMMENT '邀请码（8位）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭表';

-- ── dish_library 表 ──────────────────────────────────────────────────────────

-- dish_library.image_url
SET @exist = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_library' AND COLUMN_NAME = 'image_url');
PREPARE _s FROM IF(@exist = 0,
  'ALTER TABLE `dish_library` ADD COLUMN `image_url` VARCHAR(500) NULL AFTER `meal_type`',
  'SELECT 1');
EXECUTE _s; DEALLOCATE PREPARE _s;

-- dish_library.checkin_count
SET @exist = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_library' AND COLUMN_NAME = 'checkin_count');
PREPARE _s FROM IF(@exist = 0,
  'ALTER TABLE `dish_library` ADD COLUMN `checkin_count` INT NOT NULL DEFAULT 0 AFTER `image_url`',
  'SELECT 1');
EXECUTE _s; DEALLOCATE PREPARE _s;

-- dish_library.tags（本次新增功能）
SET @exist = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish_library' AND COLUMN_NAME = 'tags');
PREPARE _s FROM IF(@exist = 0,
  'ALTER TABLE `dish_library` ADD COLUMN `tags` VARCHAR(200) NULL AFTER `checkin_count`',
  'SELECT 1');
EXECUTE _s; DEALLOCATE PREPARE _s;

-- ── 为已有系统预设菜品补充 tags（UPDATE IGNORE 只更新 tags 为 NULL 的行）────

UPDATE `dish_library` SET tags = '素'       WHERE user_id = 0 AND tags IS NULL AND name IN ('豆浆油条','煎饼果子','阳春面','荷包蛋拌面','番茄鸡蛋面','馒头配咸菜','番茄炒蛋盖饭','扬州炒饭','红烧豆腐','清炒时蔬','砂锅粥','番茄炒蛋','炒青菜','蒸蛋','泡面加蛋');
UPDATE `dish_library` SET tags = '荤'       WHERE user_id = 0 AND tags IS NULL AND name IN ('皮蛋瘦肉粥','小笼包','红烧肉','糖醋排骨');
UPDATE `dish_library` SET tags = '荤,辣'   WHERE user_id = 0 AND tags IS NULL AND name IN ('宫保鸡丁','鱼香肉丝','回锅肉','水煮肉片');
UPDATE `dish_library` SET tags = '素,辣'   WHERE user_id = 0 AND tags IS NULL AND name IN ('麻婆豆腐','酸辣汤');
UPDATE `dish_library` SET tags = '荤,清淡' WHERE user_id = 0 AND tags IS NULL AND name IN ('清蒸鱼','白切鸡','蒜蓉虾');
UPDATE `dish_library` SET tags = '素,清淡' WHERE user_id = 0 AND tags IS NULL AND name IN ('清炒时蔬','砂锅粥','炒青菜','蒸蛋');
