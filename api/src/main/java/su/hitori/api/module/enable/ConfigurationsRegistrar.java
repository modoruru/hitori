package su.hitori.api.module.enable;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import su.hitori.api.configuration.ConfigurationSource;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.SectionScheme;

public interface ConfigurationsRegistrar {

    ConfigurationsRegistrar register(HitoriConfiguration<?> configuration);

    default <RootScheme extends SectionScheme> HitoriConfiguration<RootScheme> register(Key key, RootScheme rootScheme, @Nullable ConfigurationSource configurationSource) {
        HitoriConfiguration<RootScheme> configuration = HitoriConfiguration.create(key, rootScheme, configurationSource);
        register(configuration);
        return configuration;
    }

}
