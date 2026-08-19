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

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.internal.database.base.Schema;
import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.util.StringUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * 达梦 Schema 实现。在达梦中，Schema 即用户。
 *
 * @author kelvin.liang
 */
public class Dameng8Schema extends Schema<Dameng8Database, Dameng8Table> {

    /**
     * 创建达梦 Schema 句柄。
     *
     * @param jdbcTemplate 向数据库执行语句的模板
     * @param database     所属的达梦数据库适配器
     * @param name         Schema（用户）名
     */
    Dameng8Schema(JdbcTemplate jdbcTemplate, Dameng8Database database, String name) {
        super(jdbcTemplate, database, name);
    }

    /**
     * 检查该 Schema 对应的用户是否存在。
     *
     * @return 在 {@code ALL_USERS} 中命中记录时为 {@code true}
     * @throws SQLException 查询失败时抛出
     */
    @Override
    protected boolean doExists() throws SQLException {
        return StringUtils.hasText(jdbcTemplate.queryForString(
            "SELECT USERNAME FROM ALL_USERS WHERE USERNAME = ?", name)); // [VERIFY] 达梦 ALL_USERS 视图待验证
    }

    /**
     * 检查该 Schema 是否不含任何对象。
     *
     * @return 该 Schema 在 {@code ALL_OBJECTS} 中无对象时为 {@code true}
     * @throws SQLException 查询失败时抛出
     */
    @Override
    protected boolean doEmpty() throws SQLException {
        return !StringUtils.hasText(jdbcTemplate.queryForString(
            "SELECT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER = ? AND ROWNUM = 1", name)); // [VERIFY] 待验证
    }

    /**
     * 通过创建达梦用户建立 Schema，并授予 RESOURCE 权限使其可持有对象。
     *
     * @throws SQLException 创建用户或授权失败时抛出
     */
    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE USER " + database.quote(name)
            + " IDENTIFIED BY " + database.quote("FFllyywwaayy00!!")); // [VERIFY] 达梦 CREATE USER/IDENTIFIED BY 引号处理待验证
        jdbcTemplate.execute("GRANT RESOURCE TO " + database.quote(name));
    }

    /**
     * 通过 {@code CASCADE} 删除用户来删除 Schema，将一并移除其持有的全部对象。
     *
     * @throws SQLException 删除失败时抛出
     */
    @Override
    protected void doDrop() throws SQLException {
        jdbcTemplate.execute("DROP USER " + database.quote(name) + " CASCADE");
    }

    /**
     * 按依赖友好的顺序删除该 Schema 持有的全部对象：先删表（级联约束），
     * 再删视图、序列，最后删 PL/SQL 对象。
     *
     * @throws SQLException 任一删除失败时抛出
     */
    @Override
    protected void doClean() throws SQLException {
        // 拒绝清理达梦系统 Schema——删除其对象将导致实例瘫痪。
        if (database.getSystemSchemas().contains(name)) {
            throw new FlywayException("Clean not allowed on Dameng system schema: " + name);
        }

        // 专用字典视图已按对象类型过滤。
        dropObjects("ALL_TABLES", "TABLE_NAME", "DROP TABLE", " CASCADE CONSTRAINTS");
        dropObjects("ALL_VIEWS", "VIEW_NAME", "DROP VIEW", "");
        dropObjects("ALL_SEQUENCES", "SEQUENCE_NAME", "DROP SEQUENCE", "");

        // PL/SQL 对象：按 ALL_OBJECTS 的 OBJECT_TYPE 过滤。
        // [VERIFY] Oracle 中物化视图的 OBJECT_TYPE 报告为 'TABLE'，因此物化视图这一轮可能为空操作，
        // 其容器表也可能出现在上面的 ALL_TABLES 轮次中。需在集成测试（任务 7）中对照真实 DM8 实例
        // 确认字典行为（以及达梦是否使用 ALL_MVIEWS 视图）。
        for (String type : new String[]{"SYNONYM", "TRIGGER", "TYPE", "PROCEDURE", "FUNCTION",
            "PACKAGE", "MATERIALIZED VIEW"}) {
            String dropVerb = type.equals("MATERIALIZED VIEW") ? "DROP MATERIALIZED VIEW" : "DROP " + type;
            dropObjectsByType(type, dropVerb);
        }
    }

    /**
     * 删除专用字典视图（如 {@code ALL_TABLES}）列出的全部对象，此类视图本身已按
     * 单一对象类型过滤。
     *
     * @param view       待查询的字典视图
     * @param nameColumn 视图中对象名所在列
     * @param dropVerb   DROP 语句前缀，如 {@code "DROP TABLE"}
     * @param suffix     追加到每条 DROP 之后的子句，如 {@code " CASCADE CONSTRAINTS"}
     * @throws SQLException 查询或删除失败时抛出
     */
    private void dropObjects(String view, String nameColumn, String dropVerb, String suffix) throws SQLException {
        List<String> names = jdbcTemplate.queryForStringList(
            "SELECT " + nameColumn + " FROM " + view + " WHERE OWNER = ?", name); // [VERIFY] 字典视图待验证
        for (String objectName : names) {
            jdbcTemplate.execute(dropVerb + " " + database.quote(name, objectName) + suffix);
        }
    }

    /**
     * 删除该 Schema 持有的某一类型的全部对象，经 {@code ALL_OBJECTS.OBJECT_TYPE} 解析。
     *
     * @param objectType OBJECT_TYPE 过滤值，如 {@code "TRIGGER"}
     * @param dropVerb   DROP 语句前缀，如 {@code "DROP TRIGGER"}
     * @throws SQLException 查询或删除失败时抛出
     */
    private void dropObjectsByType(String objectType, String dropVerb) throws SQLException {
        List<String> names = jdbcTemplate.queryForStringList(
            "SELECT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER = ? AND OBJECT_TYPE = ?", name, objectType); // [VERIFY] 待验证
        for (String objectName : names) {
            jdbcTemplate.execute(dropVerb + " " + database.quote(name, objectName));
        }
    }

    /**
     * 列出该 Schema 持有的全部表。
     *
     * @return 从 {@code ALL_TABLES} 解析出的表数组
     * @throws SQLException 查询失败时抛出
     */
    @Override
    protected Dameng8Table[] doAllTables() throws SQLException {
        List<String> names = jdbcTemplate.queryForStringList(
            "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = ?", name); // [VERIFY] 待验证
        Dameng8Table[] tables = new Dameng8Table[names.size()];
        for (int i = 0; i < names.size(); i++) {
            tables[i] = new Dameng8Table(jdbcTemplate, database, this, names.get(i));
        }
        return tables;
    }

    /**
     * 为该 Schema 中的某张表创建句柄。
     *
     * @param tableName 表名
     * @return 新的 {@link Dameng8Table}
     */
    @Override
    public Table getTable(String tableName) {
        return new Dameng8Table(jdbcTemplate, database, this, tableName);
    }
}
