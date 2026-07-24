package su.hitori.plugin.module.enable;

import net.kyori.adventure.key.Key;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.module.enable.ConfigurationsRegistrar;
import su.hitori.api.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public final class ConfigurationsRegistrarImpl implements ConfigurationsRegistrar {

    public final Registry<HitoriConfiguration<?>> registry;
    public final Map<Key, HitoriConfiguration<?>> configurations;
    public boolean frozen;

    public ConfigurationsRegistrarImpl(Registry<HitoriConfiguration<?>> registry) {
        this.registry = registry;
        this.configurations = new HashMap<>();
    }

    @Override
    public ConfigurationsRegistrar register(HitoriConfiguration<?> configuration) {
        if(frozen) return this;
        Key key = configuration.key();
        if(registry.hasKey(key) || configurations.containsKey(key)) throw new IllegalArgumentException(String.format(
                "Configuration under key %s is already registered.",
                key.asString()
        ));

        configurations.put(key, configuration);

        return this;
    }

}
