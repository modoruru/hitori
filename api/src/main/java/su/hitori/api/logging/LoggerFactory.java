package su.hitori.api.logging;

import java.util.logging.Logger;

public interface LoggerFactory {

    @SuppressWarnings("DataFlowIssue")
    static LoggerFactory instance() {
        return LoggerFactoryHolder.instance;
    }

    Logger create();

    Logger create(Class<?> clazz);

    Logger create(String name);

}
