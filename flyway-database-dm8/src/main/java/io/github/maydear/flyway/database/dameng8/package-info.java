/**
 * Flyway 达梦数据库（DM8）支持包。
 *
 * <p>本包实现 Flyway 的数据库插件 SPI。当 {@code flyway-core}、本构件与达梦 JDBC 驱动
 * （{@code com.dameng:DmJdbcDriver11} 或兼容版本）同时位于类路径时，插件将被自动发现。
 *
 * <p>用法示例：
 * <pre>{@code
 * Flyway flyway = Flyway.configure()
 *     .dataSource("jdbc:dm://host:5236", "SYSDBA", "password")
 *     .load();
 * flyway.migrate();
 * }</pre>
 *
 * @since 0.1.0
 */
package io.github.maydear.flyway.database.dameng8;
