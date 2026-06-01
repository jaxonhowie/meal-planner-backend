-- V3: 添加 dish_library 表的 (user_id, name) 索引
-- 优化 recordCheckin 和 add 方法中的 user_id + name 查询

ALTER TABLE `dish_library` ADD INDEX `idx_user_name` (`user_id`, `name`);
