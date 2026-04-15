package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;

/**
 * V4 Java Migration：幂等地为 dish_library 添加 updated_at 字段。
 * 若列已存在（如旧数据库通过其他方式添加过）则跳过，避免重复执行报错。
 */
public class V4__AddUpdatedAtToDishLibrary extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String catalog = conn.getCatalog();
        DatabaseMetaData meta = conn.getMetaData();

        // 幂等：updated_at 不存在时才添加
        try (ResultSet rs = meta.getColumns(catalog, null, "dish_library", "updated_at")) {
            if (!rs.next()) {
                try (Statement s = conn.createStatement()) {
                    s.execute(
                        "ALTER TABLE `dish_library` " +
                        "ADD COLUMN `updated_at` DATETIME NULL DEFAULT NULL " +
                        "COMMENT '最后更新时间' AFTER `created_at`"
                    );
                }
                // 回填：用 created_at 初始化 updated_at
                try (Statement s = conn.createStatement()) {
                    s.execute(
                        "UPDATE `dish_library` SET `updated_at` = `created_at` " +
                        "WHERE `updated_at` IS NULL"
                    );
                }
            }
        }
    }
}
