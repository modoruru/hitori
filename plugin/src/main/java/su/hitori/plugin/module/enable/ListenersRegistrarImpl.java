package su.hitori.plugin.module.enable;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.enable.ListenersRegistrar;
import su.hitori.api.util.LoggerUtil;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public final class ListenersRegistrarImpl implements ListenersRegistrar {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    public final Set<Listener> listeners = new HashSet<>();
    public boolean frozen = false;

    @SafeVarargs
    @Override
    public final ListenersRegistrar createThenRegister(Class<? extends Listener> clazz, Class<? extends Listener>... listeners) {
        if(frozen) return this;

        Listener listener = createInstance(clazz);
        if(listener != null) this.listeners.add(listener);

        for (Class<? extends Listener> aClass : listeners) {
            Listener listener1 = createInstance(aClass);
            if(listener1 != null) this.listeners.add(listener1);
        }

        return this;
    }

    private @Nullable Listener createInstance(Class<? extends Listener> clazz) {
        Listener listener;
        try {
            Constructor<? extends Listener> constructor = clazz.getConstructor();
            listener = constructor.newInstance();
        }
        catch (Throwable exception) {
            LOGGER.warning(LoggerUtil.exceptionToString(exception));
            return null;
        }
        return listener;
    }

    @Override
    public ListenersRegistrar register(Collection<? extends Listener> listeners) {
        if(frozen) return this;
        this.listeners.addAll(listeners);
        return this;
    }

    @Override
    public ListenersRegistrar register(Listener... listeners) {
        if(frozen) return this;
        Collections.addAll(this.listeners, listeners);
        return this;
    }

}
