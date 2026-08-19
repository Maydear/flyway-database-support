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

import org.flywaydb.core.internal.database.base.Connection;
import org.flywaydb.core.internal.database.base.Schema;

import java.sql.SQLException;

/**
 * 达梦的连接层适配：解析当前 Schema 并支持切换。
 *
 * @author kelvin.liang
 */
public class Dameng8Connection extends Connection<Dameng8Database> {

    /**
     * 创建达梦连接适配器。
     *
     * @param database   所属的达梦数据库适配器
     * @param connection 原始 JDBC 连接
     */
    Dameng8Connection(Dameng8Database database, java.sql.Connection connection) {
        super(database, connection);
    }

    /**
     * 解析当前 Schema；达梦中当前 Schema 即登录用户。
     *
     * @return 当前用户名
     * @throws SQLException 查询失败时抛出
     */
    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        return jdbcTemplate.queryForString("SELECT USER FROM DUAL");
    }

    /**
     * 切换会话的当前 Schema。
     *
     * @param schema 目标 Schema 名
     * @throws SQLException 切换失败时抛出
     */
    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) throws SQLException {
        jdbcTemplate.execute("SET SCHEMA " + database.quote(schema)); // [VERIFY] 达梦 SET SCHEMA 语法待验证
    }

    /**
     * 创建达梦 Schema 句柄。
     *
     * @param name Schema（用户）名
     * @return 新的 {@link Dameng8Schema}
     */
    @Override
    public Schema getSchema(String name) {
        return new Dameng8Schema(jdbcTemplate, database, name);
    }
}
