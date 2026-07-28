package su.hitori.api;

import net.kyori.adventure.key.Key;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.registry.RegistryAccess;
import su.hitori.api.registry.RegistryKey;
import su.hitori.api.util.UnsafeUtil;

public interface HitoriRegistryAccess extends RegistryAccess {

    RegistryKey<HitoriConfiguration<?>> CONFIGURATION = new RegistryKey<>(
            Key.key("hitori", "configuration"),
            UnsafeUtil.cast(HitoriConfiguration.class)
    );

}
