package su.hitori.test;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.logging.LoggerFactoryHolder;

import java.util.logging.Logger;

public final class LoggerInitializerListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        LoggerFactoryHolder.set(new LoggerFactory() {
            @Override
            public Logger create() {
                return create("unknown");
            }

            @Override
            public Logger create(Class<?> clazz) {
                return create(clazz.getSimpleName());
            }

            @Override
            public Logger create(String name) {
                return Logger.getLogger(name);
            }
        });
    }

}
