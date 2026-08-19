package io.github.maydear.flyway.database.dameng8;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Dameng8Database#buildHistoryTableDdl} 的单元测试：列布局、主键、表空间子句、
 * 基线记录与 success 索引。
 */
class Dameng8DatabaseTest {

    private static final String TABLE_SQL = "\"TEST\".\"FLYWAY_SCHEMA_HISTORY\"";
    private static final String TABLE_NAME = "FLYWAY_SCHEMA_HISTORY";
    private static final String SCHEMA_NAME = "TEST";

    @Test
    void buildHistoryTableDdl_containsAllColumnsAndPrimaryKey() {
        String ddl = Dameng8Database.buildHistoryTableDdl(TABLE_SQL, TABLE_NAME, SCHEMA_NAME, null, "");

        assertThat(ddl).contains("CREATE TABLE " + TABLE_SQL);
        assertThat(ddl).contains("\"installed_rank\" INT NOT NULL");
        assertThat(ddl).contains("\"version\" VARCHAR(50)");
        assertThat(ddl).contains("\"description\" VARCHAR(200) NOT NULL");
        assertThat(ddl).contains("\"type\" VARCHAR(20) NOT NULL");
        assertThat(ddl).contains("\"script\" VARCHAR(1000) NOT NULL");
        assertThat(ddl).contains("\"checksum\" INT");
        assertThat(ddl).contains("\"installed_by\" VARCHAR(100) NOT NULL");
        assertThat(ddl).contains("\"installed_on\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL");
        assertThat(ddl).contains("\"execution_time\" INT NOT NULL");
        assertThat(ddl).contains("\"success\" NUMBER(1) NOT NULL");
        assertThat(ddl).contains("CONSTRAINT \"FLYWAY_SCHEMA_HISTORY_pk\" PRIMARY KEY (\"installed_rank\")");
    }

    @Test
    void buildHistoryTableDdl_createsSuccessIndexWithoutTablespace() {
        String ddl = Dameng8Database.buildHistoryTableDdl(TABLE_SQL, TABLE_NAME, SCHEMA_NAME, null, "");

        assertThat(ddl).contains("CREATE INDEX \"TEST\".\"FLYWAY_SCHEMA_HISTORY_s_idx\" ON " + TABLE_SQL + " (\"success\")");
        assertThat(ddl).doesNotContain("TABLESPACE");
    }

    @Test
    void buildHistoryTableDdl_addsTablespaceWhenProvided() {
        String ddl = Dameng8Database.buildHistoryTableDdl(TABLE_SQL, TABLE_NAME, SCHEMA_NAME, "MY_TS", "");

        assertThat(ddl).contains("TABLESPACE \"MY_TS\"");
    }

    @Test
    void buildHistoryTableDdl_includesBaselineStatementWhenProvided() {
        String baseline = "INSERT INTO \"TEST\".\"FLYWAY_SCHEMA_HISTORY\""
            + " (\"installed_rank\",\"version\",\"description\",\"type\",\"script\","
            + "\"checksum\",\"installed_by\",\"execution_time\",\"success\")"
            + " VALUES (1, NULL, '<< Flyway Baseline >>', 'BASELINE', '', NULL, 'SYSDBA', 0, 1)";
        String ddl = Dameng8Database.buildHistoryTableDdl(TABLE_SQL, TABLE_NAME, SCHEMA_NAME, null, baseline);

        // 基线 INSERT 应位于 CREATE TABLE 之后、CREATE INDEX 之前
        int createTableEnd = ddl.indexOf(");\n") + ");\n".length();
        int baselinePos = ddl.indexOf(baseline);
        int indexPos = ddl.indexOf("CREATE INDEX");
        // 基线语句紧随 CREATE TABLE 的 ");\n" 之后，且位于 CREATE INDEX 之前。
        assertThat(baselinePos).isGreaterThanOrEqualTo(createTableEnd);
        assertThat(indexPos).isGreaterThan(baselinePos);
    }

    @Test
    void buildHistoryTableDdl_omitsBaselineWhenEmpty() {
        String ddl = Dameng8Database.buildHistoryTableDdl(TABLE_SQL, TABLE_NAME, SCHEMA_NAME, null, "");

        assertThat(ddl).doesNotContain("INSERT INTO");
    }
}
