package su.hitori.api.logging;

import org.jetbrains.annotations.Nullable;

public final class LoggerFactoryHolder {

    static @Nullable LoggerFactory instance;

    public static void set(LoggerFactory loggerFactory) {
        if(instance == null && loggerFactory != null)
            instance = loggerFactory;
    }

}
