package su.hitori.api.logging;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;
import java.util.logging.Logger;

public interface LoggerFactory {

    static LoggerFactory instance() {
        return Optional.ofNullable(Bukkit.getServer().getServicesManager().getRegistration(LoggerFactory.class))
                .map(RegisteredServiceProvider::getProvider)
                .orElse(null);
    }

    Logger create();

    Logger create(Class<?> clazz);

    Logger create(String name);

}
