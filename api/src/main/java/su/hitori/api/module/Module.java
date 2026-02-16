package su.hitori.api.module;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import su.hitori.api.Version;
import su.hitori.api.module.compatibility.CompatibilityLayer;
import su.hitori.api.module.enable.EnableContext;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * A main class of every Module in hitori framework
 */
public abstract class Module implements Keyed {

    private final ModuleMeta moduleMeta;
    private final ModuleDescriptor moduleDescriptor;
    private final File folder;

    /**
     * Constructor of module
     * @throws IllegalStateException if module is created outside the hitori framework module loader
     */
    public Module() {
        if(!(getClass().getClassLoader() instanceof ModuleInitializer moduleInitializer))
            throw new IllegalStateException("Module created not from ModuleInitializer");

        this.moduleMeta = moduleInitializer.getModuleMeta();
        this.moduleDescriptor = moduleInitializer.getModuleDescriptor();
        this.folder = moduleDescriptor.getFolder();
    }

    /**
     * Meta of this module
     * @return the meta
     */
    public final ModuleMeta moduleMeta() {
        return moduleMeta;
    }

    /**
     * ModuleDescriptor for this module
     * @return moduleDescriptor
     */
    public final ModuleDescriptor moduleDescriptor() {
        return moduleDescriptor;
    }

    /**
     * Path to personal folder of this module. Can be used for configs, assets etc.
     * @return personal folder of module
     */
    public final Path folder() {
        return folder.toPath();
    }

    /**
     * Path to default config file located in module folder
     */
    public final Path defaultConfig() {
        File folder = this.folder;
        folder.mkdirs();
        return folder().resolve("config.yml");
    }

    /**
     * Key of this module
     */
    @Override
    public final @NotNull Key key() {
        return moduleMeta.key();
    }

    /**
     * Version of this Module from {@link #moduleMeta()}
     * @return version object
     */
    public final Version version() {
        return moduleMeta.version();
    }

    /**
     * Description of this Module from {@link #moduleMeta()}
     * @return description or empty string if description is not specified
     */
    public final String description() {
        return moduleMeta.description();
    }

    /**
     * Setups compatibility with others modules, called before enabling to determine which module are required for this one to run.
     * <p>
     * Does nothing by default.
     * @param compatibilityLayer compatibilityLayer to setup compatibility
     */
    public void setupCompatibility(CompatibilityLayer compatibilityLayer) {
    }

    public abstract void enable(EnableContext context);

    public abstract void disable();

    public InputStream getResourceAsStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

}
