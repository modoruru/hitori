package su.hitori.api.module.enable;

import org.bukkit.event.Listener;

import java.util.Collection;

/**
 * Module specific listeners registrar used in enable phase of module
 */
public interface ListenersRegistrar {

    /**
     * Creates listeners from classes using empty constructors, then register them from this module
     * @param clazz first listener class to create
     * @param listeners an array containing classes of listeners to create
     * @return that ListenersRegistrar
     */
    ListenersRegistrar createThenRegister(Class<? extends Listener> clazz, Class<? extends Listener>... listeners);

    /**
     * Registers from this module a collection of listeners
     * @param listeners collection containing listeners to register
     * @return that ListenersRegistrar
     */
    ListenersRegistrar register(Collection<? extends Listener> listeners);

    /**
     * Registers from this module an array of listeners
     * @param listeners collection containing listeners to register
     * @return that ListenersRegistrar
     */
    ListenersRegistrar register(Listener... listeners);

}
