package su.hitori.api.module;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Version;

import java.util.Set;

/**
 * Metadata of module
 * @param key module key
 * @param version module version
 * @param description module description or empty string if not present
 */
public record ModuleMeta(Key key, Version version, @Nullable BuildInfo buildInfo, String description, Set<Key> hardDependencies) {

    public static final class BuildInfo {

        private static final BuildInfo IDE_BUILD = new BuildInfo(true, null);

        private final boolean ide;
        private final @Nullable String commit;

        private BuildInfo(boolean ide, @Nullable String commit) {
            this.ide = ide;
            this.commit = commit;
        }

        public boolean ide() {
            return ide;
        }

        public @Nullable String commit() {
            return commit;
        }

        public static BuildInfo ideBuild() {
            return IDE_BUILD;
        }

        public static BuildInfo create(String commit) {
            return new BuildInfo(false, commit);
        }

    }

}
