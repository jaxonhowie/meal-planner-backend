package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;

/**
 * V2 Java Migration：幂等地为旧版生产数据库补全后续添加的字段。
 * 使用 DatabaseMetaData 检查列是否存在，避免 MySQL 方言 / 权限问题。
 * 对新建数据库（V1 已创建完整 schema）同样安全。
 */
public class V2__PatchMissingColumns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String catalog = conn.getCatalog();
        DatabaseMetaData meta = conn.getMetaData();

        // ── user 表 ──────────────────────────────────────────────────────────
        addColumnIfMissing(conn, meta, catalog, "user", "nickname",
            "ALTER TABLE `user` ADD COLUMN `nickname` VARCHAR(50) NULL AFTER `password`");

        addColumnIfMissing(conn, meta, catalog, "user", "family_id",
            "ALTER TABLE `user` ADD COLUMN `family_id` BIGINT NULL AFTER `nickname`");

        // ── family 表（若不存在则创建）───────────────────────────────────────
        try (Statement s = conn.createStatement()) {
            s.execute(
                "CREATE TABLE IF NOT EXISTS `family` (" +
                "  `id`          BIGINT       NOT NULL AUTO_INCREMENT," +
                "  `name`        VARCHAR(100) NOT NULL," +
                "  `invite_code` VARCHAR(10)  NOT NULL," +
                "  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (`id`)," +
                "  UNIQUE KEY `uq_invite_code` (`invite_code`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }

        // ── dish_library 表 ──────────────────────────────────────────────────
        addColumnIfMissing(conn, meta, catalog, "dish_library", "image_url",
            "ALTER TABLE `dish_library` ADD COLUMN `image_url` VARCHAR(500) NULL AFTER `meal_type`");

        addColumnIfMissing(conn, meta, catalog, "dish_library", "checkin_count",
            "ALTER TABLE `dish_library` ADD COLUMN `checkin_count` INT NOT NULL DEFAULT 0 AFTER `image_url`");

        addColumnIfMissing(conn, meta, catalog, "dish_library", "tags",
            "ALTER TABLE `dish_library` ADD COLUMN `tags` VARCHAR(200) NULL AFTER `checkin_count`");

        // ── 为系统预设菜品补充 tags（仅更新 tags 为 NULL 的行，幂等）──────────
        try (Statement s = conn.createStatement()) {
            s.execute("UPDATE `dish_library` SET tags = '素'       WHERE user_id = 0 AND tags IS NULL " +
                      "AND name IN ('豆浆油条','煎饼果子','阳春面','荷包蛋拌面','番茄鸡蛋面','馒头配咸菜','番茄炒蛋盖饭','扬州炒饭','红烧豆腐','番茄炒蛋','炒青菜','蒸蛋','泡面加蛋')");
            s.execute("UPDATE `dish_library` SET tags = '荤'       WHERE user_id = 0 AND tags IS NULL " +
                      "AND name IN ('皮蛋瘦肉粥','小笼包','红烧肉','糖醋排骨')");
            s.execute("UPDATE `dish_library` SET tags = '荤,辣'   WHERE user_id = 0 AND tags IS NULL " +
                      "AND name IN ('宫保鸡丁','鱼香肉丝','回锅肉','水煮肉片')");
            s.execute("UPDATE `dish_library` SET tags = '素,辣'   WHERE user_id = 0 AND tags IS NULL " +
                      "AND name IN ('麻婆豆腐','酸辣汤')");
            s.execute("UPDATE `dish_library` SET tags = '荤,清淡' WHERE user_id = 0 AND tags IS NULL " +
                      "AND name IN ('清蒸鱼','白切鸡','蒜蓉虾')");
            s.execute("UPDATE `dish_library` SET tags = '素,清淡' WHERE user_id = 0 AND tags IS NULL " +
                      "AND name IN ('清炒时蔬','砂锅粥','炒青菜','蒸蛋')");
        }
    }

    private void addColumnIfMissing(Connection conn, DatabaseMetaData meta,
                                     String catalog, String table, String column,
                                     String alterSql) throws SQLException {
        try (ResultSet rs = meta.getColumns(catalog, null, table, column)) {
            if (!rs.next()) {
                try (Statement s = conn.createStatement()) {
                    s.execute(alterSql);
                }
            }
        }
    }
}
