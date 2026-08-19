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

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.base.BaseDatabaseType;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;

import java.sql.Connection;
import java.sql.Types;
import java.util.regex.Pattern;

/**
 * 达梦数据库（DM8）的 Flyway 数据库类型。
 *
 * <p>通过 {@code META-INF/services/org.flywaydb.core.extensibility.Plugin} 注册，
 * 由 Flyway 在运行时自动发现。
 *
 * @author kelvin.liang
 */
public class Dameng8DatabaseType extends BaseDatabaseType {

    /**
     * 匹配内嵌凭证的达梦 JDBC URL：{@code jdbc:dm://user:password@host} 或
     * {@code jdbc:dm:user:password@host} 形式。
     * 分组 1 捕获密码，供 Flyway 在日志中脱敏。
     */
    private static final Pattern URL_CREDENTIALS_PATTERN =
        Pattern.compile("^jdbc:dm:(?://)?[^:@/]+:([^@]+)@.*$");

    /**
     * 返回 Flyway 用于标识达梦的数据库类型名。
     *
     * @return {@code "DM"}
     */
    @Override
    public String getName() {
        return "DM";
    }

    /**
     * 返回绑定 NULL 值时使用的 JDBC 类型。
     *
     * @return {@link Types#VARCHAR}
     */
    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    /**
     * 判断给定的 JDBC URL 是否指向达梦数据库。
     *
     * @param url 待检查的 JDBC URL，可为 null
     * @return 仅当 URL 以 {@code jdbc:dm:} 开头时为 {@code true}
     */
    @Override
    public boolean handlesJDBCUrl(String url) {
        return url != null && url.startsWith("jdbc:dm:");
    }

    /**
     * 返回用于检测达梦 JDBC URL 内嵌凭证的正则，供 Flyway 在日志中脱敏。
     *
     * @return 匹配 {@code jdbc:dm://user:password@host} 形式 URL 的正则
     */
    @Override
    public Pattern getJDBCCredentialsPattern() {
        return URL_CREDENTIALS_PATTERN;
    }

    /**
     * 无论 URL 形式如何，均返回达梦 JDBC 驱动类名。
     *
     * @param url         JDBC URL（未使用，所有 DM URL 共用一个驱动）
     * @param classLoader 探测驱动所用的类加载器（未使用）
     * @return {@code "dm.jdbc.driver.DmDriver"}
     */
    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        return "dm.jdbc.driver.DmDriver";
    }

    /**
     * 判断 JDBC 驱动上报的产品名是否为达梦服务器。
     *
     * @param databaseProductName    产品名，如 {@code "DM DBMS"}
     * @param databaseProductVersion 产品版本，不参与匹配
     * @param connection             活动的 JDBC 连接，不参与匹配
     * @return 仅当产品名以 {@code "DM"} 开头时为 {@code true}
     */
    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName,
                                                        String databaseProductVersion,
                                                        Connection connection) {
        return databaseProductName != null && databaseProductName.startsWith("DM");
    }

    /**
     * 为正在打开的连接创建达梦数据库适配器。
     *
     * @param configuration         Flyway 配置
     * @param jdbcConnectionFactory 持有 JDBC 连接的工厂
     * @param statementInterceptor  语句拦截器，可为 null
     * @return 新的 {@link Dameng8Database}
     */
    @Override
    public Database createDatabase(Configuration configuration,
                                   JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        return new Dameng8Database(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    /**
     * 创建达梦 SQL 脚本解析器。
     *
     * @param configuration    Flyway 配置
     * @param resourceProvider 迁移脚本资源提供器
     * @param parsingContext   共享的解析上下文
     * @return 新的 {@link Dameng8Parser}
     */
    @Override
    public Parser createParser(Configuration configuration,
                               ResourceProvider resourceProvider,
                               ParsingContext parsingContext) {
        return new Dameng8Parser(configuration, parsingContext);
    }
}
