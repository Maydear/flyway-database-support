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

import org.flywaydb.core.extensibility.ConfigurationExtension;

/**
 * 达梦的配置扩展点。为未来的达梦专有配置项保留命名空间，当前未暴露任何参数。
 *
 * @author kelvin.liang
 */
public class Dameng8ConfigurationExtension implements ConfigurationExtension {

    /**
     * 返回为未来达梦专有配置项保留的命名空间。
     *
     * @return {@code "dameng8"}
     */
    @Override
    public String getNamespace() {
        return "dameng8";
    }

    /**
     * 从环境变量解析配置参数；当前尚未定义任何映射。
     *
     * @param environmentVariable 环境变量名
     * @return 恒为 {@code null}
     */
    @Override
    public String getConfigurationParameterFromEnvironmentVariable(String environmentVariable) {
        return null;
    }
}
