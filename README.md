# Flyway 达梦数据库支持（DM8）

![License](https://img.shields.io/badge/License-Apache--2.0-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Flyway](https://img.shields.io/badge/Flyway-12.11.0-red)
![Database](https://img.shields.io/badge/%E8%BE%BE%E6%A2%A6-DM8-blue)
![Build](https://img.shields.io/badge/Build-Maven-c71a36)

> 为 [Flyway](https://flywaydb.org/) 提供**达梦数据库（DM8）**的第三方插件支持。基于 Flyway 12 插件 SPI 实现，引入依赖后即可被
> Flyway 自动发现，开箱即用。

---

## 目录

- [简介](#简介)
- [核心特性](#核心特性)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [迁移脚本示例](#迁移脚本示例)
- [工作原理](#工作原理)
- [配置说明](#配置说明)
- [已知限制](#已知限制)
- [本地构建与测试](#本地构建与测试)
- [项目结构](#项目结构)
- [版本兼容矩阵](#版本兼容矩阵)
- [反馈与贡献](#反馈与贡献)
- [许可证](#许可证)

---

## 简介

自 Flyway 10 起，官方将各数据库的支持能力拆分为独立的 `flyway-database-*` 插件，并要求通过 SPI 在运行时发现。达梦数据库（达梦数据
DM8）并不在 Flyway 官方支持列表中，本组件用于补齐这一空白。

本组件实现了 Flyway 的数据库插件 SPI（`org.flywaydb.core.extensibility.Plugin`），覆盖以下能力：

- 识别 `jdbc:dm:` 连接字符串与 `dm.jdbc.driver.DmDriver` 驱动；
- 按达梦方言生成并维护 `flyway_schema_history` 历史表；
- 支持 Schema（在达梦中即 User）的创建、清理与探查；
- 支持 baseline（基线）迁移；
- 对达梦系统 Schema 提供清理安全防护，避免误删系统对象。

只需将 `flyway-core`、本组件以及达梦 JDBC 驱动同时置于类路径，Flyway 即可在运行时自动加载本插件，无需额外的手工注册。

---

## 核心特性

- **零配置接入**：通过标准 SPI 机制被 Flyway 自动发现，无需手动声明 DatabaseType。
- **达梦方言适配**：历史表 DDL 采用达梦语法（如 `NUMBER(1)` 表示布尔、双引号标识符、可选 `TABLESPACE`）。
- **连接凭证脱敏**：识别 `jdbc:dm://user:password@host` 形式的内嵌凭证并在日志中自动脱敏。
- **清理安全防护**：执行 `clean` 时拒绝清理 `SYS`、`SYSDBA`、`SYSAUDITOR`、`SYSSSO`、`CTISYS`、`SYSJOB` 等系统 Schema。
- **基线支持**：支持对已存在历史的数据库建立基线。
- **Spring Boot 友好**：在 Spring Boot 环境下，由 `FlywayAutoConfiguration` 自动加载，引入依赖即生效。

---

## 环境要求

| 组件           | 版本要求                                                     |
|----------------|--------------------------------------------------------------|
| JDK            | 17 及以上                                                    |
| Flyway Core    | 12.11.0（与本项目对齐）                                      |
| 达梦数据库     | DM8                                                          |
| 达梦 JDBC 驱动 | `com.dameng:DmJdbcDriver11` 8.1.5.45（需自行引入，详见下文） |

> 驱动 Jar 可从[达梦官网](https://www.dameng.com/list_103.html)下载，数据库安装目录的 `jdbc` 子目录下亦有携带，或通过中央仓库拉取。
> `provided`/`optional` 方式依赖它，运行期由使用方提供。安装方式：

```xml
<!-- Source: https://mvnrepository.com/artifact/com.dameng/DmJdbcDriver11 -->
<dependency>
    <groupId>com.dameng</groupId>
    <artifactId>DmJdbcDriver11</artifactId>
    <version>8.1.5.45</version>
    <scope>compile</scope>
</dependency>
```

---

## 快速开始

### 1. 添加依赖

在 `pom.xml` 中引入 Flyway Core、本插件，以及达梦 JDBC 驱动：

```xml

<dependencies>
    <!-- Flyway 核心 -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
        <version>12.11.0</version>
    </dependency>

    <!-- 达梦数据库 Flyway 支持（本组件） -->
    <dependency>
        <groupId>io.github.maydear.flyway</groupId>
        <artifactId>flyway-database-dm8</artifactId>
        <version>12.11.0-SNAPSHOT</version>
    </dependency>

    <!-- 达梦 JDBC 驱动（需自行安装至仓库） -->
    <dependency>
        <groupId>com.dameng</groupId>
        <artifactId>DmJdbcDriver11</artifactId>
        <version>8.1.5.45</version>
    </dependency>
</dependencies>
```

### 2. 编写迁移脚本

在 `src/main/resources/db/migration` 下放置版本化脚本，例如 `V1__create_person.sql`：

```sql
CREATE TABLE "person"
(
    "id"   INT PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL
);
```

### 3. 执行迁移

**纯 Flyway API 方式：**

```java
import org.flywaydb.core.Flyway;

Flyway flyway = Flyway.configure()
    .dataSource("jdbc:dm://localhost:5236", "SYSDBA", "your-password")
    .load();

flyway.

migrate();
```

**Spring Boot 方式：**

在 `application.yml` 中配置数据源与 Flyway 即可，依赖引入后由 Spring Boot 自动触发迁移：

```yaml
spring:
    datasource:
        url: jdbc:dm://localhost:5236
        username: SYSDBA
        password: your-password
        driver-class-name: dm.jdbc.driver.DmDriver
    flyway:
        enabled: true
        locations: classpath:db/migration
```

---

## 迁移脚本示例

标准分号结尾的 DDL/DML 均可正常解析（`CREATE TABLE`、`ALTER`、`INSERT`、`CREATE INDEX` 等）。下方为本组件测试套件中使用的两段示例：

```sql
-- V1__create_person.sql
CREATE TABLE "person"
(
    "id"   INT PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL
);

-- V2__add_email.sql
ALTER TABLE "person" ADD ("email" VARCHAR(200));
```

---

## 工作原理

Flyway 通过 Java SPI（`META-INF/services/org.flywaydb.core.extensibility.Plugin`）在类路径上发现插件。本组件注册了以下扩展点：

| 扩展点                          | 职责                                                                                                          |
|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| `Dameng8DatabaseType`           | 识别 `jdbc:dm:` 连接、返回驱动类 `dm.jdbc.driver.DmDriver`、匹配产品名以 `DM` 开头，并创建 Database 与 Parser |
| `Dameng8Database`               | 数据库抽象层；生成达梦方言的历史表 DDL，定义系统 Schema 集合                                                  |
| `Dameng8Connection`             | 连接层适配                                                                                                    |
| `Dameng8Schema`                 | Schema（即 User）的探查、创建、清理；`clean` 时拒绝清理系统 Schema                                            |
| `Dameng8Table`                  | 表级别的元数据与操作                                                                                          |
| `Dameng8Parser`                 | SQL 脚本解析（当前委托 Flyway 基础 Parser）                                                                   |
| `Dameng8ConfigurationExtension` | 配置扩展点                                                                                                    |

当 `flyway-core`、`flyway-database-dm8` 与达梦 JDBC 驱动三者同时存在于类路径时，Flyway 启动时会自动选用本插件处理达梦连接。

---

## 配置说明

本组件遵循 Flyway 标准的 `flyway.*` 配置体系，无需额外专有配置。常用项如下：

| 配置项                                                | 说明                                                        |
|-------------------------------------------------------|-------------------------------------------------------------|
| `flyway.url` / `flyway.user` / `flyway.password`      | 达梦连接信息，URL 形如 `jdbc:dm://host:5236`                |
| `flyway.schemas`                                      | 待迁移的 Schema（达梦中即 User）                            |
| `flyway.defaultSchema`                                | 默认 Schema                                                 |
| `flyway.table`                                        | 历史表名，默认 `flyway_schema_history`                      |
| `flyway.tablespace`                                   | 历史表所在表空间（可选，设置后 DDL 追加 `TABLESPACE` 子句） |
| `flyway.baselineOnMigrate` / `flyway.baselineVersion` | 对已有数据库建立基线                                        |

> 提示：达梦会将空字符串转为 `NULL`，而历史表的 `description` 列为 `NOT NULL`，因此本组件 **不支持空迁移描述**（
> `supportsEmptyMigrationDescription()` 返回 `false`）。

---

## 已知限制

本组件当前处于早期开发阶段（`12.11.0-SNAPSHOT`），存在以下限制，使用前请知悉：

- **过程化对象解析**：暂不支持 PL/SQL 风格的匿名块（`BEGIN ... END;`）与 `/`
  语句终止符（用于存储过程、函数、包、触发器）。此类过程化迁移在当前版本需拆分为单语句或按需切分。后续版本规划支持。
- **布尔类型**：达梦以 `1`/`0` 表示布尔值，迁移脚本中涉及布尔字面量时请注意。
- **集成测试依赖真实实例**：本仓库的集成测试需要一台真实的 DM8 实例，详见[本地构建与测试](#本地构建与测试)。
- **字典视图待验证**：`clean` 等操作依赖达梦数据字典视图（`ALL_USERS`、`ALL_OBJECTS`、`ALL_TABLES` 等），其行为与 Oracle
  存在细微差异，部分分支仍待集成测试进一步确认。

---

## 本地构建与测试

### 编译与单元测试

```bash
./mvnw clean install
```

单元测试不依赖真实数据库，默认随构建执行。

### 集成测试

集成测试位于 `**/integration/**` 包下，通过 `@EnabledIfSystemProperty` 以系统属性 `dameng8.url` 门控——未设置该属性时自动跳过，
普通构建不受影响。运行集成测试需一台可访问的 DM8 实例，并通过 `-D` 系统属性传入连接信息：

```bash
./mvnw verify \
    -Ddameng8.url="jdbc:dm://localhost:5236" \
    -Ddameng8.user="SYSDBA" \
    -Ddameng8.password="your-password" \
    -Ddameng8.schema="TEST"
```

### 发布

构件发布至公司私服（`repository.tubsoft.com`）：`SNAPSHOT` 版本发布至 `maven-snapshots` 仓库，正式版发布至 `maven-releases`
仓库。私服凭证存放于本地 `settings.xml`，不入库。日常快照发布可直接执行根目录的 `deploy.sh`（等价于
`mvn clean package deploy -DskipTests`）：

```bash
./deploy.sh
```

---

## 项目结构

```
flyway-database-support/                # 父 POM 聚合工程
├── pom.xml                             # 聚合 + dependencyManagement
└── flyway-database-dm8/                # 达梦支持核心模块
    ├── pom.xml
    └── src/
        ├── main/java/.../dameng8/
        │   ├── Dameng8DatabaseType.java            # SPI 入口：DatabaseType
        │   ├── Dameng8Database.java                # 数据库抽象 + 历史表 DDL
        │   ├── Dameng8Connection.java              # 连接适配
        │   ├── Dameng8Schema.java                  # Schema(User) 操作 + 清理防护
        │   ├── Dameng8Table.java                   # 表操作
        │   ├── Dameng8Parser.java                  # SQL 解析
        │   └── Dameng8ConfigurationExtension.java  # 配置扩展
        ├── main/resources/META-INF/services/
        │   └── org.flywaydb.core.extensibility.Plugin
        └── test/
            ├── java/.../dameng8/                   # 单元测试
            │   └── integration/                    # 集成测试（需真实实例）
            └── resources/migration/dameng8/        # 示例迁移脚本
```

---

## 版本兼容矩阵

| flyway-database-dm8        | Flyway Core | 达梦 JDBC               | JDK |
|----------------------------|-------------|-------------------------|-----|
| 12.11.0-SNAPSHOT（开发中） | 12.11.0     | DmJdbcDriver11 8.1.5.45 | 17+ |

---

## 反馈与贡献

- 问题反馈与功能建议：[Issues](https://git.onloch.com/maydear/flyway-database-support/issues)
- 源码仓库：[git.onloch.com/maydear/flyway-database-support](https://git.onloch.com/maydear/flyway-database-support)

欢迎提交 Pull Request。提交前请确保：

1. 代码通过静态检查（不引入 Block 级别以上问题）；
2. 单元测试通过；
3. 如涉及数据库行为变更，补充/更新对应集成测试。

---

## 许可证

本项目基于 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源。

Copyright &copy; 2026 Maydear
