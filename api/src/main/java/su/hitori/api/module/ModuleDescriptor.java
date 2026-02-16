package su.hitori.api.module;

import net.kyori.adventure.key.Keyed;

import java.io.File;

/**
 * Descriptor of module. Performing all load, enable and reload logic of module. Also stores active instance.
 */
public interface ModuleDescriptor extends Keyed {

    /**
     * Active instance of module
     * @return module instance
     */
    Module getInstance();

    boolean isEnabling();

    /**
     * Returns if module is enabled
     * @return is module enabled
     */
    boolean isEnabled();

    /**
     * Returns personal folder of module
     * @return personal module folder
     */
    File getFolder();

    ClassLoader classLoader();

}
