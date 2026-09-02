package su.hitori.plugin.module;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import su.hitori.api.Version;
import su.hitori.api.module.ModuleMeta;
import su.hitori.api.util.JSONUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

record ExtendedMeta(String mainClass, @Nullable String bootstrapClass, Set<String> packages, ModuleMeta moduleMeta) {

    @SuppressWarnings("PatternValidation")
    public static ExtendedMeta readMetaFromJar(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("hitori.module.json");
            if(entry == null) {
                if(jarFile.getJarEntry("hitori.properties") != null)
                    throw new IllegalStateException(""); // todo: add info about migrating

                throw new IllegalStateException("hitori.module.json not found");
            }

            try (InputStream is = jarFile.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                JSONObject hitoriModuleBody = JSONUtil.read(reader);

                // Required
                String key = hitoriModuleBody.getString("key");
                String version = hitoriModuleBody.getString("version");
                String main = hitoriModuleBody.getString("main");
                JSONArray packages = hitoriModuleBody.getJSONArray("packages");

                // Optional
                String description = hitoriModuleBody.optString("description");
                String bootstrap = hitoriModuleBody.optString("bootstrap", null);
                JSONObject build = hitoriModuleBody.optJSONObject("build");
                JSONArray dependencies = hitoriModuleBody.optJSONArray("dependencies");

                Set<String> convertedPackages = new HashSet<>(packages.length());
                for (Object aPackage : packages) {
                    if(aPackage instanceof String packageAsString) convertedPackages.add(packageAsString);
                }

                Set<Key> convertedDependencies = new HashSet<>(dependencies.length());
                for (Object dependency : dependencies) {
                    if(dependency instanceof String dependencyAsString) convertedDependencies.add(Key.key(dependencyAsString));
                }

                return new ExtendedMeta(
                        main,
                        bootstrap,
                        convertedPackages,
                        new ModuleMeta(
                                Key.key(key),
                                new Version(version),
                                Optional.ofNullable(build)
                                        .map(json -> {
                                            if(json.optBoolean("ide", false)) return ModuleMeta.BuildInfo.ideBuild();
                                            return ModuleMeta.BuildInfo.create(json.getString("commit"));
                                        })
                                        .orElse(null),
                                description,
                                convertedDependencies
                        )
                );
            }
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
