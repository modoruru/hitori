package su.hitori.api.util;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;
import su.hitori.api.Hitori;
import net.kyori.adventure.key.Key;

/**
 * utils for {@link Key}
 */
@ApiStatus.Obsolete
public final class KeyUtil {

    private KeyUtil() {}

    /**
     * Creates Key under hitori framework namespace
     * @param value value of key
     * @return created Key under {@link NamespacedKey} implementation
     */
    @ApiStatus.Obsolete
    public static NamespacedKey create(String value) {
        return new NamespacedKey(Hitori.instance().plugin(), value);
    }

}
