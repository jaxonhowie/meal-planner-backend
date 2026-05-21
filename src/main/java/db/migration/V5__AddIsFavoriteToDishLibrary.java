package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;

/**
 * V5: 幂等地为 dish_library 添加 is_favorite 字段。
 */
public class V5__AddIsFavoriteToDishLibrary extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String catalog = conn.getCatalog();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(catalog, null, "dish_library", "is_favorite")) {
            if (!rs.next()) {
                try (Statement s = conn.createStatement()) {
                    s.execute(
                        "ALTER TABLE `dish_library` " +
                        "ADD COLUMN `is_favorite` TINYINT(1) NOT NULL DEFAULT 0 " +
                        "COMMENT '是否收藏' AFTER `tags`"
                    );
                }
            }
        }
    }
}
