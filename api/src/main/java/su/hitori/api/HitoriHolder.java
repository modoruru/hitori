package su.hitori.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class HitoriHolder {

    static @Nullable Hitori instance;

    public static void set(Hitori hitori) {
        if(instance != null) return;
        instance = hitori;
    }

}
