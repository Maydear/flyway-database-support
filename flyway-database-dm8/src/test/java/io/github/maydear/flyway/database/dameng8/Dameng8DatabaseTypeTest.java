package io.github.maydear.flyway.database.dameng8;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Dameng8DatabaseType} 的单元测试：URL 匹配、产品名匹配与驱动类解析。
 */
class Dameng8DatabaseTypeTest {

    private final Dameng8DatabaseType type = new Dameng8DatabaseType();

    @Test
    void nameIsDm() {
        assertThat(type.getName()).isEqualTo("DM");
    }

    @Test
    void nullTypeIsVarchar() {
        assertThat(type.getNullType()).isEqualTo(Types.VARCHAR);
    }

    @Test
    void handlesJdbcUrl_matchesDmUrls() {
        assertThat(type.handlesJDBCUrl("jdbc:dm://localhost:5236/SYSDBA")).isTrue();
        assertThat(type.handlesJDBCUrl("jdbc:dm://10.0.0.1:5236")).isTrue();
    }

    @Test
    void handlesJdbcUrl_rejectsNonDmUrls() {
        assertThat(type.handlesJDBCUrl("jdbc:oracle:thin:@localhost:1521:xe")).isFalse();
        assertThat(type.handlesJDBCUrl("jdbc:postgresql://localhost/db")).isFalse();
        assertThat(type.handlesJDBCUrl("jdbc:mysql://localhost/db")).isFalse();
    }

    @Test
    void driverClassIsDmDriver() {
        assertThat(type.getDriverClass("jdbc:dm://localhost:5236", getClass().getClassLoader()))
            .isEqualTo("dm.jdbc.driver.DmDriver");
    }

    @Test
    void handlesProductName_matchesDmDbms() {
        assertThat(type.handlesDatabaseProductNameAndVersion("DM DBMS", "8.1.3.140", null)).isTrue();
        assertThat(type.handlesDatabaseProductNameAndVersion("DM", "8.1", null)).isTrue();
    }

    @Test
    void handlesProductName_rejectsOthers() {
        assertThat(type.handlesDatabaseProductNameAndVersion("Oracle", "18c", null)).isFalse();
        assertThat(type.handlesDatabaseProductNameAndVersion("PostgreSQL", "16", null)).isFalse();
    }
}
