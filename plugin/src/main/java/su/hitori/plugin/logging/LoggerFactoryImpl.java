package su.hitori.plugin.logging;

import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleInitializer;

import java.util.logging.Logger;

public final class LoggerFactoryImpl implements LoggerFactory {

    private static final String UNKNOWN = "unknown";
    private static final String HITORI = "hitori";

    @Override
    public Logger create() {
        var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stream -> stream
                .map(StackWalker.StackFrame::getDeclaringClass)
                .skip(1)
                .findFirst()
        );

        return caller.map(this::create).orElseGet(() -> create(UNKNOWN));
    }

    @Override
    public Logger create(Class<?> clazz) {
        String namespace;

        if(clazz.getClassLoader() instanceof ModuleInitializer moduleInitializer) namespace = moduleInitializer.getModuleMeta().key().value();
        else if(clazz.getName().startsWith("su.hitori.plugin")) namespace = HITORI;
        else namespace = UNKNOWN;

        StringBuilder builder = new StringBuilder(namespace);
        if(!clazz.isAssignableFrom(Module.class)) {
            builder.append('/').append(clazz.getSimpleName());
        }

        return create(builder.toString());
    }

    @Override
    public Logger create(String name) {
        return Logger.getLogger(name);
    }

}
