/*
 * Copyright 2026 Maydear
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.maydear.flyway.database.dameng8;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.util.StringUtils;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Flyway 的达梦（DM8）数据库适配层。
 *
 * @author kelvin.liang
 */
public class Dameng8Database extends Database<Dameng8Connection> {

    /**
     * 已知的达梦系统 Schema（用户）名单，严禁对其执行清理。
     */
    private static final Set<String> SYSTEM_SCHEMAS = new HashSet<>(Arrays.asList(
        "SYS", "SYSDBA", "SYSAUDITOR", "SYSSSO", "CTISYS", "SYSJOB"));

    /**
     * 创建达梦数据库适配器。
     *
     * @param configuration         Flyway 配置
     * @param jdbcConnectionFactory 持有 JDBC 连接的工厂
     * @param statementInterceptor  语句拦截器，可为 null
     */
    public Dameng8Database(Configuration configuration,
                           JdbcConnectionFactory jdbcConnectionFactory,
                           StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    /**
     * 构建达梦方言的 {@code flyway_schema_history} DDL。包私有以便在无真实数据库连接时
     * 进行单元测试（见 Dameng8DatabaseTest）。
     *
     * @param tableSql          完全限定且已加引号的表标识（{@code table.toString()}）
     * @param tableName         未限定的表名
     * @param schemaName        Schema 名
     * @param tablespace        可选表空间；为 null 或空时省略该子句
     * @param baselineStatement 基线记录的 INSERT 语句，非基线时为空字符串
     * @return 完整的 DDL 脚本
     */
    static String buildHistoryTableDdl(String tableSql, String tableName, String schemaName,
                                       String tablespace, String baselineStatement) {
        // TABLESPACE 子句可选：仅在配置了表空间时追加。
        String tablespaceClause = StringUtils.hasText(tablespace)
            ? " TABLESPACE \"" + tablespace + "\""
            : "";

        // 列布局与标准 flyway_schema_history 一致；"success" 用 NUMBER(1)，
        // 因为达梦无原生布尔类型，以 1/0 存储标志位。
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableSql).append(" (\n")
            .append("    \"installed_rank\" INT NOT NULL,\n")
            .append("    \"version\" VARCHAR(50),\n")
            .append("    \"description\" VARCHAR(200) NOT NULL,\n")
            .append("    \"type\" VARCHAR(20) NOT NULL,\n")
            .append("    \"script\" VARCHAR(1000) NOT NULL,\n")
            .append("    \"checksum\" INT,\n")
            .append("    \"installed_by\" VARCHAR(100) NOT NULL,\n")
            .append("    \"installed_on\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,\n")
            .append("    \"execution_time\" INT NOT NULL,\n")
            .append("    \"success\" NUMBER(1) NOT NULL,\n")
            .append("    CONSTRAINT \"").append(tableName).append("_pk\" PRIMARY KEY (\"installed_rank\")\n")
            .append(")").append(tablespaceClause).append(";\n");

        // 基线记录须位于 CREATE TABLE 之后、索引之前，保证脚本可一次性顺序执行。
        if (StringUtils.hasText(baselineStatement)) {
            sb.append(baselineStatement).append(";\n");
        }

        // 为 "success" 建索引以加速待执行/失败迁移的查询。
        sb.append("CREATE INDEX \"").append(schemaName).append("\".\"").append(tableName)
            .append("_s_idx\" ON ").append(tableSql).append(" (\"success\");\n");
        return sb.toString();
    }

    /**
     * 将原始 JDBC 连接包装为达梦连接适配器。
     *
     * @param connection 原始 JDBC 连接
     * @return 新的 {@link Dameng8Connection}
     */
    @Override
    protected Dameng8Connection doGetConnection(java.sql.Connection connection) {
        return new Dameng8Connection(this, connection);
    }

    /**
     * 确认所连服务器为受支持的达梦版本。
     *
     * @param configuration Flyway 配置
     */
    @Override
    public void ensureSupported(Configuration configuration) {
        ensureDatabaseIsRecentEnough("8");
    }

    /**
     * 构建以达梦方言创建历史表的脚本。
     *
     * @param table    目标历史表
     * @param baseline 为 {@code true} 时追加基线记录的 INSERT
     * @return 完整的 DDL 脚本
     */
    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        // 先解析基线 INSERT，再委托包私有的构建方法，使 DDL 本身无需真实连接即可单元测试。
        String baselineStatement = baseline ? getBaselineStatement(table) : "";
        return buildHistoryTableDdl(
            table.toString(),
            table.getName(),
            table.getSchema().getName(),
            configuration.getTablespace(),
            baselineStatement);
    }

    /**
     * 获取当前会话登录的用户。
     *
     * @return 服务器上的 {@code USER} 值
     * @throws SQLException 查询失败时抛出
     */
    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainConnection().getJdbcTemplate().queryForString("SELECT USER FROM DUAL");
    }

    /**
     * 达梦不支持事务性 DDL。
     *
     * @return {@code false}
     */
    @Override
    public boolean supportsDdlTransactions() {
        return false;
    }

    /**
     * 返回 {@code true} 的字面量；达梦以数字表示布尔值。
     *
     * @return {@code "1"}
     */
    @Override
    public String getBooleanTrue() {
        return "1";
    }

    /**
     * 返回 {@code false} 的字面量；达梦以数字表示布尔值。
     *
     * @return {@code "0"}
     */
    @Override
    public String getBooleanFalse() {
        return "0";
    }

    /**
     * 达梦中 catalog 并非 Schema；Schema 即用户。
     *
     * @return {@code false}
     */
    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    /**
     * 不支持空迁移描述：达梦将空字符串转为 NULL，而历史表的 {@code description} 列为
     * NOT NULL。
     *
     * @return {@code false}
     */
    @Override
    public boolean supportsEmptyMigrationDescription() {
        // 达梦将空字符串转为 NULL，而 description 列为 NOT NULL。
        return false;
    }

    /**
     * 返回达梦系统 Schema 集合。并非重写——Flyway 基类 Database 无此钩子；
     * 由 Dameng8Schema 在拒绝清理系统 Schema 时查询。
     *
     * @return 受保护的系统 Schema 名集合（大写）
     */
    Set<String> getSystemSchemas() {
        return SYSTEM_SCHEMAS;
    }
}
