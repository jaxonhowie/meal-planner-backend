-- 为 dish_library 添加 updated_at 字段，用于按更新时间排序
ALTER TABLE `dish_library`
  ADD COLUMN `updated_at` DATETIME NULL DEFAULT NULL COMMENT '最后更新时间'
  AFTER `created_at`;

-- 存量数据：初始化为 created_at
UPDATE `dish_library` SET `updated_at` = `created_at` WHERE `updated_at` IS NULL;
