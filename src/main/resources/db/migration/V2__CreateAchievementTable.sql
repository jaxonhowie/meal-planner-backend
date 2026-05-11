CREATE TABLE `achievement` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(50)  NOT NULL COMMENT '成就编码',
  `name`        VARCHAR(100) NOT NULL COMMENT '成就名称',
  `description` VARCHAR(200) NOT NULL COMMENT '成就描述',
  `icon`        VARCHAR(50)  NOT NULL COMMENT '图标 emoji',
  `category`    VARCHAR(30)  NOT NULL COMMENT '分类: streak / variety / rating / milestone',
  `threshold`   INT          NOT NULL DEFAULT 1 COMMENT '解锁阈值',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_achievement` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT   NOT NULL,
  `achievement_id` BIGINT   NOT NULL,
  `unlocked_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_achievement` (`user_id`, `achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 种子数据
INSERT INTO `achievement` (`code`, `name`, `description`, `icon`, `category`, `threshold`, `sort_order`) VALUES
('first_checkin',    '初出茅庐',   '完成第一次打卡',           '🌱', 'milestone', 1,   1),
('streak_7',         '一周坚持',   '连续打卡 7 天',            '🔥', 'streak',    7,   2),
('streak_30',        '月度达人',   '连续打卡 30 天',           '💪', 'streak',    30,  3),
('streak_100',       '百日如一',   '连续打卡 100 天',          '🏆', 'streak',    100, 4),
('variety_10',       '尝鲜派',     '打卡 10 种不同菜品',       '🍽️', 'variety',   10,  5),
('variety_30',       '美食家',     '打卡 30 种不同菜品',       '👨‍🍳', 'variety',   30,  6),
('variety_50',       '百味人生',   '打卡 50 种不同菜品',       '🌈', 'variety',   50,  7),
('rating_5star',     '五星好评',   '给出第一次 5 星评分',      '⭐', 'rating',    1,   8),
('checkin_50',       '半个世纪',   '累计打卡 50 次',           '🎯', 'milestone', 50,  9),
('checkin_100',      '百次打卡',   '累计打卡 100 次',          '💯', 'milestone', 100, 10),
('checkin_200',      '打卡狂人',   '累计打卡 200 次',          '🦸', 'milestone', 200, 11),
('all_meals_day',    '三餐齐全',   '同一天完成早中晚三餐打卡', '☀️', 'milestone', 1,   12);
