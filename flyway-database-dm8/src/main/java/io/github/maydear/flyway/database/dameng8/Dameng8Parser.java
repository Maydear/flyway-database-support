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
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;

/**
 * 达梦的 SQL 脚本解析器。
 *
 * <p>当前版本委托给 Flyway 基础 {@link Parser}，其可正确处理以分号结尾的标准
 * DDL/DML 语句、字符串字面量与注释——覆盖达梦迁移的常见场景
 * （{@code CREATE TABLE}、{@code ALTER}、{@code INSERT}、{@code CREATE INDEX} 等）。
 *
 * <p>暂不支持 PL/SQL 风格的匿名块（{@code BEGIN…END;}）与 {@code /} 语句终止符
 * （用于存储过程、函数、包、触发器），规划于未来版本支持。在此之前，过程化迁移
 * 应编写为单语句或按需拆分。
 *
 * @author kelvin.liang
 */
public class Dameng8Parser extends Parser {

    /**
     * 创建达梦解析器。
     *
     * @param configuration  Flyway 配置
     * @param parsingContext 共享的解析上下文
     */
    public Dameng8Parser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 3);
    }
}
