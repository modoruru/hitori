package su.hitori.api.module;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;

public interface ModuleBootstrap {

    /**
     * Called during Bootstrap phase of Hitori plugin. No calls other than to BootstrapContext should be done there.
     * @param context bootstrap context
     */
    @SuppressWarnings("UnstableApiUsage")
    void bootstrap(BootstrapContext context);

}
