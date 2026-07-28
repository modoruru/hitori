package su.hitori.api.configuration.listener;

import org.jetbrains.annotations.ApiStatus;
import su.hitori.api.module.ModuleDescriptor;

public final class RegisteredFieldListener {

    private final ModuleDescriptor registrar;
    private final FieldListener<?> listener;
    private final Runnable unregisterMethod;

    private boolean registered;

    @ApiStatus.Internal
    public RegisteredFieldListener(ModuleDescriptor registrar, FieldListener<?> listener, Runnable unregisterMethod) {
        this.registrar = registrar;
        this.listener = listener;
        this.unregisterMethod = unregisterMethod;
    }

    public ModuleDescriptor registrar() {
        return registrar;
    }

    public FieldListener<?> listener() {
        return listener;
    }

    public void unregister() {
        if(!registered) return;
        registered = false;

        unregisterMethod.run();
    }

}
