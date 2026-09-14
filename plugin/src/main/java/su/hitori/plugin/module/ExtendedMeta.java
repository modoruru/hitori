package su.hitori.plugin.module;

import net.kyori.adventure.key.Key;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Version;
import su.hitori.api.module.ModuleMeta;
import su.hitori.api.util.JSONUtil;
import su.hitori.api.util.SafeUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public record ExtendedMeta(String mainClass, @Nullable String bootstrapClass, Set<String> packages, ModuleMeta moduleMeta, Dependency<Integer> javaDependency, Dependency<Version> hitoriDependency, Map<Key, Dependency<Version>> modulesDependencies) {

    private static <E> E wrapRequiredGet(String nodeName, Function<String, E> getFunction) throws MetaReadError {
        try {
            return getFunction.apply(nodeName);
        }
        catch (JSONException _) {
            throw MetaReadError.missingField(nodeName);
        }
    }

    private static <E extends Comparable<E>> Dependency<E> parseDependency(String dependencyName, String dependency, String representationTip, Function<String, @Nullable E> baseParseFunction) throws MetaReadError {
        Dependency.Operator operator = Dependency.readOperator(dependency);
        if(operator == null) throw MetaReadError.format("Unable to read operator for \"" + dependencyName + "\" dependency.");
        E dependencyBase = baseParseFunction.apply(dependency.substring(operator.symbols));
        if(dependencyBase == null) throw MetaReadError.format(String.format(
                "Unable to read java version dependency: \"%s\" version should be represented as %s.",
                dependencyName,
                representationTip
        ));

        return new Dependency<>(dependencyBase, operator);
    }

    private static @Nullable Version parseVersion(String raw) {
        return SafeUtil.wrapParse(Version::new, raw);
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
                JSONObject depends = wrapRequiredGet("depends", hitoriModuleBody::getJSONObject);
                String dependsJava = wrapRequiredGet("java", depends::getString);
                String dependsHitori = wrapRequiredGet("hitori", depends::getString);

                // Optional
                String description = hitoriModuleBody.optString("description");
                String bootstrap = hitoriModuleBody.optString("bootstrap", null);
                JSONObject build = hitoriModuleBody.optJSONObject("build");

                Set<String> convertedPackages = new HashSet<>(packages.length());
                for (Object aPackage : packages) {
                    if(aPackage instanceof String packageAsString) convertedPackages.add(packageAsString);
                }

                Dependency.Operator javaDependencyOperator = Dependency.readOperator(dependsJava);
                if(javaDependencyOperator == null) throw MetaReadError.format("Unable to read operator for java dependency.");
                Integer javaDependencyBase = SafeUtil.parseInt(dependsJava.substring(javaDependencyOperator.symbols));
                if(javaDependencyBase == null) throw MetaReadError.format("Unable to read java version dependency: java version should be represented as single integer.");

                Dependency<Integer> javaDependency = parseDependency("java", dependsJava, "single integer", SafeUtil::parseInt);
                Dependency<Version> hitoriDependency = parseDependency("hitori", dependsHitori, "SemVer 2.0.0 string", ExtendedMeta::parseVersion);

                Map<Key, Dependency<Version>> modulesDependencies = new HashMap<>();
                for (String dependencyKey : depends.keySet()) {
                    if(dependencyKey.equalsIgnoreCase("hitori") || dependencyKey.equalsIgnoreCase("java")) continue;
                    if(dependencyKey.indexOf(':') == -1) throw MetaReadError.format("Unknown dependency type: " + dependencyKey + "\". Allowed types are: java, hitori and module key (for example, hitori:template)");

                    String value = depends.optString(dependencyKey, null);
                    if(value == null) throw MetaReadError.format(dependencyKey + " value is not a string.");

                    Key moduleKey = SafeUtil.wrapParse(Key::key, dependencyKey);
                    if(moduleKey == null) throw MetaReadError.format("Unable to parse \"" + dependencyKey + "\" key.");

                    Dependency<Version> moduleDependency = parseDependency(dependencyKey, value, "SemVer 2.0.0 string", ExtendedMeta::parseVersion);
                    modulesDependencies.put(moduleKey, moduleDependency);
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
                                description
                        ),
                        javaDependency,
                        hitoriDependency,
                        modulesDependencies
                );
            }
        }
        catch (IOException exception) {
            throw MetaReadError.io(exception);
        }
    }

    public static final class MetaReadError extends Exception {

        public static final MetaReadError
                OLD_FORMAT_ERROR = new MetaReadError(Type.OLD_FORMAT, null, null, null),
                MISSING_MODULE_JSON = new MetaReadError(Type.MISSING_MODULE_JSON, null, null, null);

        public final Type type;
        public final @Nullable String missingField;
        public final @Nullable IOException ioException;
        public final @Nullable String formatMessage;

        private MetaReadError(Type type, @Nullable String missingField, @Nullable IOException ioException, @Nullable String formatMessage) {
            this.type = type;
            this.missingField = missingField;
            this.ioException = ioException;
            this.formatMessage = formatMessage;
        }

        public static MetaReadError missingField(String missingField) {
            return new MetaReadError(Type.MISSING_FIELD, missingField, null, null);
        }

        public static MetaReadError io(IOException ioException) {
            return new MetaReadError(Type.IO, null, ioException, null);
        }

        public static MetaReadError format(String formatMessage) {
            return new MetaReadError(Type.FORMAT, null, null, formatMessage);
        }

        public enum Type {
            OLD_FORMAT,
            MISSING_FIELD,
            MISSING_MODULE_JSON,
            IO,
            FORMAT
        }

    }

    public static final class Dependency<V extends Comparable<V>> {

        public final V base;
        public final Operator operator;

        public Dependency(V base, Operator operator) {
            this.base = base;
            this.operator = operator;
        }

        public static @Nullable Operator readOperator(String string) {
            char firstCharacter = string.charAt(0);
            if(firstCharacter == '=') return Operator.EQUALS;

            char secondCharacter = string.charAt(1);
            if(firstCharacter == '>') return secondCharacter == '=' ? Operator.GREATER_OR_EQUALS : Operator.GREATER;

            return null;
        }

        public boolean compatible(V other) {
            int difference = base.compareTo(other);
            return switch (operator) {
                case EQUALS -> difference == 0;
                case GREATER_OR_EQUALS -> difference >= 0;
                case GREATER -> difference > 0;
            };
        }

        public enum Operator {
            EQUALS(1),
            GREATER_OR_EQUALS(2),
            GREATER(1);

            public final int symbols;

            Operator(int symbols) {
                this.symbols = symbols;
            }

        }

    }

}
