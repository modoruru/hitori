package su.hitori.plugin.module;

import su.hitori.api.module.Module;
import su.hitori.api.module.ModuleDescriptor;
import su.hitori.api.module.ModuleInitializer;
import su.hitori.api.module.ModuleMeta;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

final class ModuleClassLoader extends URLClassLoader implements ModuleInitializer {

    private final ExtendedMeta extendedMeta;
    private final ModuleDescriptorImpl moduleDescriptor;
    private final ModuleRepositoryImpl moduleRepository;
    private final Set<ModuleDescriptorImpl> injected;

    public ModuleClassLoader(URL[] urls, ClassLoader parent, ExtendedMeta extendedMeta, ModuleDescriptorImpl moduleDescriptor, ModuleRepositoryImpl moduleRepository) {
        super(urls, parent);
        this.extendedMeta = extendedMeta;
        this.moduleDescriptor = moduleDescriptor;
        this.moduleRepository = moduleRepository;
        this.injected = new HashSet<>();
    }

    @Override
    public ModuleMeta getModuleMeta() {
        return extendedMeta.toModuleMeta();
    }

    @Override
    public ModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }

    public Set<ModuleDescriptorImpl> getInjectedModules() {
        return injected;
    }

    Module create() {
        try {
            Class<?> mainClass = loadClass(extendedMeta.mainClass());
            Constructor<?> constructor = mainClass.getConstructor();
            Object instance = constructor.newInstance();
            if(!(instance instanceof Module module)) throw new IllegalStateException("created instance is not a Module");
            return module;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Class<?> loadClass(ModuleDescriptorImpl descriptor, String name, boolean resolve) throws ClassNotFoundException {
        if(descriptor == null || descriptor == moduleDescriptor) throw new IllegalArgumentException("illegal descriptor present");
        injected.add(descriptor);
        return loadClass(name, resolve);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name.startsWith(extendedMeta.packageName())) return super.loadClass(name, resolve);

        Class<?> moduleSpecific = moduleRepository.loadModuleSpecificClass(moduleDescriptor, name, resolve);
        if(moduleSpecific != null) return moduleSpecific;

        return super.loadClass(name, resolve);
    }

}
