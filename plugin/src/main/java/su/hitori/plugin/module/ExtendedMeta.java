package su.hitori.plugin.module;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import su.hitori.api.Version;
import su.hitori.api.module.ModuleMeta;
import su.hitori.api.util.JSONUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public record ExtendedMeta(String mainClass, @Nullable String bootstrapClass, Set<String> packages, ModuleMeta moduleMeta) {

    private static <E> E wrapRequiredGet(String nodeName, Function<String, E> getFunction) throws MetaReadError {
        try {
            return getFunction.apply(nodeName);
        }
        catch (JSONException _) {
            throw MetaReadError.missingField(nodeName);
        }
    }

    @SuppressWarnings("PatternValidation")
    public static ExtendedMeta readMetaFromJar(File jar) throws MetaReadError {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("hitori.module.json");
            if(entry == null) {
                if(jarFile.getJarEntry("hitori.properties") != null)
                    throw MetaReadError.OLD_FORMAT_ERROR;

                throw MetaReadError.MISSING_MODULE_JSON;
            }

            try (InputStream is = jarFile.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                JSONObject hitoriModuleBody = JSONUtil.read(reader);

                // Required
                String key = wrapRequiredGet("key", hitoriModuleBody::getString);
                String version = wrapRequiredGet("version", hitoriModuleBody::getString);
                String main = wrapRequiredGet("main", hitoriModuleBody::getString);
                JSONArray packages = wrapRequiredGet("packages", hitoriModuleBody::getJSONArray);

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
        catch (IOException exception) {
            throw MetaReadError.io(exception);
        }
    }

    public static final class MetaReadError extends Exception {

        public static final MetaReadError
                OLD_FORMAT_ERROR = new MetaReadError(Type.OLD_FORMAT, null, null),
                MISSING_MODULE_JSON = new MetaReadError(Type.MISSING_MODULE_JSON, null, null);

        public final Type type;
        public final @Nullable String missingField;
        public final @Nullable IOException ioException;

        private MetaReadError(Type type, @Nullable String missingField, @Nullable IOException ioException) {
            this.type = type;
            this.missingField = missingField;
            this.ioException = ioException;
        }

        public static MetaReadError missingField(String missingField) {
            return new MetaReadError(Type.MISSING_FIELD, missingField, null);
        }

        public static MetaReadError io(IOException ioException) {
            return new MetaReadError(Type.IO, null, ioException);
        }

        public enum Type {
            OLD_FORMAT,
            MISSING_FIELD,
            MISSING_MODULE_JSON,
            IO
        }

    }

}
