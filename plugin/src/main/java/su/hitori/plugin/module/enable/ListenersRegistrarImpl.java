package su.hitori.plugin.module.enable;

import org.bukkit.event.Listener;
import su.hitori.api.module.enable.ListenersRegistrar;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ListenersRegistrarImpl implements ListenersRegistrar {

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

    private Listener createInstance(Class<? extends Listener> clazz) {
        Listener listener;
        try {
            Constructor<? extends Listener> constructor = clazz.getConstructor();
            listener = constructor.newInstance();
        }
        catch (Throwable ex) {
            ex.printStackTrace();
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
