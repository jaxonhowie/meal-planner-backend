package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;

/**
 * V3 Java Migration：补全 meal_plan 和 plan_dish 表中后续添加的字段。
 * 幂等：使用 DatabaseMetaData 检查列是否存在，安全运行在新旧数据库上。
 */
public class V3__PatchMealTables extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String catalog = conn.getCatalog();
        DatabaseMetaData meta = conn.getMetaData();

        // ── meal_plan 表 ──────────────────────────────────────────────────────

        // meal_plan.family_id（家庭共享菜单功能所需）
        addColumnIfMissing(conn, meta, catalog, "meal_plan", "family_id",
            "ALTER TABLE `meal_plan` ADD COLUMN `family_id` BIGINT NOT NULL DEFAULT 0 AFTER `id`");

        // 为 family_id 补充索引（若已存在则忽略）
        addIndexIfMissing(conn, catalog, "meal_plan", "idx_family_date",
            "ALTER TABLE `meal_plan` ADD KEY `idx_family_date` (`family_id`, `date`)");

        // ── plan_dish 表 ──────────────────────────────────────────────────────

        // plan_dish.remark（菜品备注，如：不辣、少放油）
        addColumnIfMissing(conn, meta, catalog, "plan_dish", "remark",
            "ALTER TABLE `plan_dish` ADD COLUMN `remark` VARCHAR(500) NULL AFTER `dish_name`");

        // plan_dish.sort_order（排序序号）
        addColumnIfMissing(conn, meta, catalog, "plan_dish", "sort_order",
            "ALTER TABLE `plan_dish` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 AFTER `remark`");
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

    private void addIndexIfMissing(Connection conn, String catalog,
                                    String table, String indexName,
                                    String alterSql) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, catalog);
            ps.setString(2, table);
            ps.setString(3, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (Statement s = conn.createStatement()) {
                        s.execute(alterSql);
                    }
                }
            }
        }
    }
}
