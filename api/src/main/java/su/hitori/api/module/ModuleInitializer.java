package su.hitori.api.module;

/**
 * Class used to extend ClassLoader using to load module.
 * Transfers basic data of module to instance.
 */
public interface ModuleInitializer {

    ModuleMeta getModuleMeta();

    ModuleDescriptor getModuleDescriptor();

}
