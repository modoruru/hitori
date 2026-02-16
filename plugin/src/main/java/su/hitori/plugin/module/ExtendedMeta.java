package su.hitori.plugin.module;

import net.kyori.adventure.key.Key;
import su.hitori.api.Version;
import su.hitori.api.module.ModuleMeta;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

record ExtendedMeta(String mainClass, String packageName, Key key, Version version, String description) {

    public ModuleMeta toModuleMeta() {
        return new ModuleMeta(key, version, description);
    }

    public static ExtendedMeta readMetaFromJar(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("hitori.properties");
            if(entry == null) {
                throw new IllegalStateException("hitori.properties not found");
            }

            try (InputStream is = jarFile.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                Properties properties = new Properties();
                properties.load(reader);

                String key = properties.getProperty("key");
                String version = properties.getProperty("version");
                String description = properties.getProperty("description", "");
                String mainClass = properties.getProperty("main");
                String packageName = properties.getProperty("package");

                if(key == null) throw new IllegalStateException("hitori.properties missing \"key\" key!");
                if(version == null) throw new IllegalStateException("hitori.properties missing \"version\" key!");
                if(mainClass == null) throw new IllegalStateException("hitori.properties missing \"main\" key!");
                if(packageName == null) throw new IllegalStateException("hitori.properties missing \"package\" key!");

                return new ExtendedMeta(mainClass, packageName, Key.key(key), new Version(version), description);
            }
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
