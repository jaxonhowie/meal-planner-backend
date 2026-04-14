-- MealPlanner v2 migration：为 dish_library 添加 tags 列
-- 已有数据库实例执行此脚本（新环境通过 init.sql 重建无需此步骤）

ALTER TABLE `dish_library`
  ADD COLUMN `tags` VARCHAR(200) NULL COMMENT '标签，逗号分隔，如：荤,辣' AFTER `checkin_count`;

-- 为系统预设菜品补充标签
UPDATE `dish_library` SET tags = '素'       WHERE user_id = 0 AND name IN ('豆浆油条', '煎饼果子', '阳春面', '荷包蛋拌面', '番茄鸡蛋面', '馒头配咸菜', '番茄炒蛋盖饭', '麻婆豆腐', '扬州炒饭', '红烧豆腐', '清炒时蔬', '砂锅粥', '酸辣汤', '番茄炒蛋', '炒青菜', '蒸蛋', '泡面加蛋');
UPDATE `dish_library` SET tags = '荤'       WHERE user_id = 0 AND name IN ('皮蛋瘦肉粥', '小笼包', '红烧肉', '糖醋排骨', '扬州炒饭');
UPDATE `dish_library` SET tags = '荤,辣'   WHERE user_id = 0 AND name IN ('宫保鸡丁', '鱼香肉丝', '回锅肉', '水煮肉片');
UPDATE `dish_library` SET tags = '素,辣'   WHERE user_id = 0 AND name IN ('麻婆豆腐', '酸辣汤');
UPDATE `dish_library` SET tags = '荤,清淡' WHERE user_id = 0 AND name IN ('清蒸鱼', '白切鸡', '蒜蓉虾');
UPDATE `dish_library` SET tags = '素,清淡' WHERE user_id = 0 AND name IN ('清炒时蔬', '砂锅粥', '炒青菜', '蒸蛋');
