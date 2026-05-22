-- H2-compatible schema for integration tests
-- Converted from MySQL migrations, removing ENGINE=InnoDB and CHARSET=utf8mb4

CREATE TABLE IF NOT EXISTS family (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100) NOT NULL,
  invite_code VARCHAR(10)  NOT NULL,
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (invite_code)
);

CREATE TABLE IF NOT EXISTS user (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  username   VARCHAR(50)  NOT NULL,
  password   VARCHAR(100) NOT NULL,
  nickname   VARCHAR(50),
  family_id  BIGINT,
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
  deleted    INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS meal_plan (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  family_id  BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL,
  date       DATE         NOT NULL,
  meal_type  VARCHAR(20)  NOT NULL,
  status     VARCHAR(20)  NOT NULL DEFAULT 'planned',
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (family_id, date, meal_type)
);

CREATE TABLE IF NOT EXISTS plan_dish (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  plan_id    BIGINT       NOT NULL,
  dish_name  VARCHAR(200) NOT NULL,
  remark     VARCHAR(500),
  sort_order INT          NOT NULL DEFAULT 0,
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS meal_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  plan_id     BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  description TEXT,
  rating      INT          NOT NULL DEFAULT 3,
  image_url   VARCHAR(500),
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS dish_library (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL DEFAULT 0,
  name          VARCHAR(200) NOT NULL,
  meal_type     VARCHAR(20),
  image_url     VARCHAR(500),
  checkin_count INT          NOT NULL DEFAULT 0,
  tags          VARCHAR(200),
  is_favorite   BOOLEAN      DEFAULT FALSE,
  created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  deleted       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS achievement (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(50)  NOT NULL,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(200) NOT NULL,
  icon        VARCHAR(50)  NOT NULL,
  category    VARCHAR(30)  NOT NULL,
  threshold   INT          NOT NULL DEFAULT 1,
  sort_order  INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS user_achievement (
  id             BIGINT   NOT NULL AUTO_INCREMENT,
  user_id        BIGINT   NOT NULL,
  achievement_id BIGINT   NOT NULL,
  unlocked_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (user_id, achievement_id)
);

CREATE TABLE IF NOT EXISTS stats (
  id BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (id)
);

-- Seed achievement data (using MERGE INTO to avoid duplicate key errors)
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('first_checkin', '初出茅庐', '完成第一次打卡', '🌱', 'milestone', 1, 1);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('streak_7', '一周坚持', '连续打卡 7 天', '🔥', 'streak', 7, 2);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('streak_30', '月度达人', '连续打卡 30 天', '💪', 'streak', 30, 3);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('streak_100', '百日如一', '连续打卡 100 天', '🏆', 'streak', 100, 4);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('variety_10', '尝鲜派', '打卡 10 种不同菜品', '🍽️', 'variety', 10, 5);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('variety_30', '美食家', '打卡 30 种不同菜品', '👨‍🍳', 'variety', 30, 6);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('variety_50', '百味人生', '打卡 50 种不同菜品', '🌈', 'variety', 50, 7);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('rating_5star', '五星好评', '给出第一次 5 星评分', '⭐', 'rating', 1, 8);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('checkin_50', '半个世纪', '累计打卡 50 次', '🎯', 'milestone', 50, 9);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('checkin_100', '百次打卡', '累计打卡 100 次', '💯', 'milestone', 100, 10);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('checkin_200', '打卡狂人', '累计打卡 200 次', '🦸', 'milestone', 200, 11);
MERGE INTO achievement (code, name, description, icon, category, threshold, sort_order) KEY(code) VALUES
('all_meals_day', '三餐齐全', '同一天完成早中晚三餐打卡', '☀️', 'milestone', 1, 12);
