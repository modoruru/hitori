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
import su.hitori.plugin.module.exception.MetaReadError;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public record ExtendedMeta(String mainClass, @Nullable String bootstrapClass, Set<String> packages, ModuleMeta moduleMeta, Dependency<Integer> javaDependency, Dependency<Version> hitoriDependency, Map<Key, ModuleDependency> modulesDependencies) {

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
                "Unable to read %s dependency version: \"%s\" version should be represented as %s.",
                dependencyName,
                dependency,
                representationTip
        ));

        return new Dependency<>(dependencyBase, operator);
    }

    private static @Nullable Version parseVersion(String raw) {
        return SafeUtil.wrapParse(Version::new, raw);
    }

    private static ModuleDependency parseModuleDependency(String module, JSONObject object) throws MetaReadError {
        String dependency = object.optString("version", null);
        String rawType = object.optString("type", null);
        if(dependency == null || rawType == null)
            throw MetaReadError.format("Version and/or type strings are missing for the " + module + " module dependency.");

        ModuleDependency.Type type = SafeUtil.enumValueOf(ModuleDependency.Type.class, rawType.toUpperCase());
        if(type == null)
            throw MetaReadError.format("Unknown dependency type for the " + module + " module dependency.");

        Dependency.Operator operator = Dependency.readOperator(dependency);
        if(operator == null) throw MetaReadError.format("Unable to read operator for \"" + module + "\" dependency.");
        Version dependencyBase = parseVersion(dependency.substring(operator.symbols));
        if(dependencyBase == null) throw MetaReadError.format(String.format(
                "Unable to read module %s dependency version: \"%s\" version should be represented as SemVer 2.0.0 string",
                module,
                dependency
        ));

        return new ModuleDependency(dependencyBase, operator, type);
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

                Dependency<Integer> javaDependency = parseDependency("java", dependsJava, "single integer", SafeUtil::parseInt);
                Dependency<Version> hitoriDependency = parseDependency("hitori", dependsHitori, "SemVer 2.0.0 string", ExtendedMeta::parseVersion);

                Map<Key, ModuleDependency> modulesDependencies = new HashMap<>();
                for (String dependencyKey : depends.keySet()) {
                    if(dependencyKey.equalsIgnoreCase("hitori") || dependencyKey.equalsIgnoreCase("java")) continue;
                    if(dependencyKey.indexOf(':') == -1) throw MetaReadError.format("Unknown dependency type: " + dependencyKey + "\". Allowed types are: java, hitori and module key (for example, hitori:template)");

                    JSONObject value = depends.optJSONObject(dependencyKey, null);
                    if(value == null) throw MetaReadError.format(dependencyKey + " value is not a json object.");

                    Key moduleKey = SafeUtil.wrapParse(Key::key, dependencyKey);
                    if(moduleKey == null) throw MetaReadError.format("Unable to parse \"" + dependencyKey + "\" key.");

                    modulesDependencies.put(moduleKey, parseModuleDependency(dependencyKey, value));
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

    public static final class ModuleDependency extends Dependency<Version> {

        public final Type type;

        public ModuleDependency(Version base, Operator operator, Type type) {
            super(base, operator);
            this.type = type;
        }

        public enum Type {
            SOFT, HARD
        }

    }

    public static sealed class Dependency<V extends Comparable<V>> permits ModuleDependency {

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
            int difference = other.compareTo(base);
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

        @Override
        public String toString() {
            return switch (operator) {
                case EQUALS -> "=";
                case GREATER_OR_EQUALS -> ">=";
                case GREATER -> ">";
            } + base;
        }
    }

}
