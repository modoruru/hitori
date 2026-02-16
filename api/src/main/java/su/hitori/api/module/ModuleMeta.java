package su.hitori.api.module;

import net.kyori.adventure.key.Key;
import su.hitori.api.Version;

/**
 * Metadata of module
 * @param key module key
 * @param version module version
 * @param description module description or empty string if not present
 */
public record ModuleMeta(Key key, Version version, String description) {
}
