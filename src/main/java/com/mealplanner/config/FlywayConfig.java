package com.mealplanner.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * 启动时先 repair() 清除历史失败记录，再执行 migrate()。
     * 保证部署失败后重新部署能自动恢复，而不是卡在 "failed migration" 状态。
     */
    @Bean
    public FlywayMigrationStrategy repairAndMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
