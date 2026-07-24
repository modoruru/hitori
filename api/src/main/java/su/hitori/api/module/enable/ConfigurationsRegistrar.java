package su.hitori.api.module.enable;

import net.kyori.adventure.key.Key;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.SectionScheme;

public interface ConfigurationsRegistrar {

    ConfigurationsRegistrar register(HitoriConfiguration<?> configuration);

    default <RootScheme extends SectionScheme> HitoriConfiguration<RootScheme> register(Key key, RootScheme rootScheme) {
        HitoriConfiguration<RootScheme> configuration = HitoriConfiguration.create(key, rootScheme);
        register(configuration);
        return configuration;
    }

}
