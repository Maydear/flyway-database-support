package io.github.maydear.flyway.database.dameng8;

import org.flywaydb.core.extensibility.Plugin;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证插件 SPI 注册项可经 {@link ServiceLoader} 发现，与 Flyway 自身的加载方式一致。
 */
class Dameng8PluginDiscoveryTest {

    @Test
    void damengDatabaseTypeIsDiscoverableViaServiceLoader() {
        ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class);

        assertThat(plugins)
            .anySatisfy(plugin -> assertThat(plugin).isInstanceOf(Dameng8DatabaseType.class));

        assertThat(plugins)
            .anySatisfy(plugin -> assertThat(plugin).isInstanceOf(Dameng8ConfigurationExtension.class));
    }
}
