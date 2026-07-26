package su.hitori.api.configuration.listener;

import org.jetbrains.annotations.ApiStatus;
import su.hitori.api.module.ModuleDescriptor;

public record RegisteredFieldListener(ModuleDescriptor registrar, FieldListener<?> listener) {

    @ApiStatus.Internal
    public RegisteredFieldListener {
    }

    public void unregister() {

    }

}
