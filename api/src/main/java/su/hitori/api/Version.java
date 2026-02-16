package su.hitori.api;

import io.papermc.paper.ServerBuildInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Describes version based on three numbers: global, major and minor sub-versions
 */
public final class Version implements Comparable<Version> {

    private final int global, major, minor;

    public Version(int global, int major) {
        this(global, major, 0);
    }

    public Version(int global, int major, int minor) {
        if(global < 0) throw new IllegalArgumentException("global version can't be below zero");
        if(major < 0) throw new IllegalArgumentException("major version can't be below zero");
        if(minor < 0) throw new IllegalArgumentException("minor version can't be below zero");
        this.global = global;
        this.major = major;
        this.minor = minor;
    }

    /**
     * Parses version from string
     * @param raw string with version format
     * @throws IllegalArgumentException if malformed string is passed
     */
    public Version(@NotNull String raw) throws IllegalArgumentException {
        if(raw.isEmpty()) throw new IllegalArgumentException("empty string");
        String[] unboxed = raw.split("\\.");

        int global, major, minor = 0;
        if(unboxed.length < 2 || unboxed.length > 3) throw new IllegalArgumentException("too many/few parts");

        try {
            global = Integer.parseInt(unboxed[0]);
            major = Integer.parseInt(unboxed[1]);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("malformed numbers: \"" + raw + "\"");
        }

        if(unboxed.length == 3) {
            try {
                minor = Integer.parseInt(unboxed[2]);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("malformed numbers: \"" + raw + "\"");
            }
        }

        this.global = global;
        this.major = major;
        this.minor = minor;
    }

    public int global() {
        return global;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Version version)) return false;
        return version.global == this.global
                && version.major == major
                && version.minor == this.minor;
    }

    /**
     * Formats version as string. If minor version equals zero, only global and major versions are added.
     * @return string in version format
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(global).append(".").append(major);
        if(minor > 0) builder.append(".").append(minor);
        return builder.toString();
    }

    @Override
    public int compareTo(@NotNull Version version) {
        if (this.global != version.global) return Integer.compare(this.global, version.global);
        else if (this.major != version.major) return Integer.compare(this.major, version.major);
        return Integer.compare(this.minor, version.minor);
    }

    public static Version getMinecraftVersion() {
        return new Version(ServerBuildInfo.buildInfo().minecraftVersionId());
    }

}
