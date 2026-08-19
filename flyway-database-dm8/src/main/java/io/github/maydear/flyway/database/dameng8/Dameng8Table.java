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

import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;

import java.sql.SQLException;

/**
 * 达梦的表级操作：存在性检查、删除与锁定。
 *
 * @author kelvin.liang
 */
public class Dameng8Table extends Table<Dameng8Database, Dameng8Schema> {

    /**
     * 创建达梦表句柄。
     *
     * @param jdbcTemplate 向数据库执行语句的模板
     * @param database     所属的达梦数据库适配器
     * @param schema       所属 Schema
     * @param name         表名
     */
    Dameng8Table(JdbcTemplate jdbcTemplate, Dameng8Database database, Dameng8Schema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    /**
     * 删除表并连带删除其依赖约束。
     *
     * @throws SQLException 删除失败时抛出
     */
    @Override
    protected void doDrop() throws SQLException {
        jdbcTemplate.execute("DROP TABLE " + database.quote(schema.getName(), name) + " CASCADE CONSTRAINTS");
    }

    /**
     * 检查表在其 Schema 中是否存在。
     *
     * @return 表存在时为 {@code true}
     * @throws SQLException 检查失败时抛出
     */
    @Override
    protected boolean doExists() throws SQLException {
        return exists(null, schema, name);
    }

    /**
     * 在迁移期间以独占模式锁定该表。
     *
     * @throws SQLException 加锁失败时抛出
     */
    @Override
    protected void doLock() throws SQLException {
        jdbcTemplate.execute("LOCK TABLE " + this + " IN EXCLUSIVE MODE"); // [VERIFY] 达梦锁定语法待验证
    }
}
