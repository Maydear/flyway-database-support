package io.github.maydear.flyway.database.dameng8.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 针对真实 DM8 实例的集成测试。以系统属性 {@code dameng8.url} 门控，未设置时跳过，
 * 因而不影响日常构建。
 * <p>
 * 运行：mvn verify -Ddameng8.url=jdbc:dm://host:5236 -Ddameng8.user=SYSDBA \
 * -Ddameng8.password=xxx -Ddameng8.schema=TEST
 */
@Tag("integration")
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(
    named = "dameng8.url", matches = ".+")
class Dameng8IntegrationTest {

    private Flyway flyway() {
        return Flyway.configure()
            .dataSource(
                System.getProperty("dameng8.url"),
                System.getProperty("dameng8.user"),
                System.getProperty("dameng8.password"))
            .defaultSchema(System.getProperty("dameng8.schema"))
            .locations("classpath:migration/dameng8")
            .cleanDisabled(false)
            .load();
    }

    @Test
    void migrate_appliesAllMigrationsAndIsIdempotent() {
        Flyway flyway = flyway();
        flyway.clean();

        MigrateResult first = flyway.migrate();
        assertThat(first.success).isTrue();
        assertThat(first.migrationsExecuted).isEqualTo(2);

        // 幂等：再次执行不会产生新迁移。
        MigrateResult second = flyway.migrate();
        assertThat(second.migrationsExecuted).isZero();
    }

    @Test
    void validate_passesAfterMigrate() {
        Flyway flyway = flyway();
        flyway.clean();
        flyway.migrate();
        flyway.validate();
    }

    @Test
    void clean_removesSchemaHistoryAndTables() {
        Flyway flyway = flyway();
        flyway.migrate();
        flyway.clean();

        // 清理后再次迁移将全部从头执行。
        MigrateResult result = flyway.migrate();
        assertThat(result.migrationsExecuted).isEqualTo(2);
    }
}
