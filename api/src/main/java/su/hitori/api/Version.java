package su.hitori.api;

import io.papermc.paper.ServerBuildInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object representation of <a href="https://semver.org/">Semantic Versioning 2.0.0/</a> standard.
 */
public final class Version implements Comparable<Version> {

    private static final Pattern
            PRE_RELEASE_AND_METADATA_PATTERN = Pattern.compile("[0-9A-Za-z-]+"),
            RAW_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[A-Za-z-][0-9A-Za-z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[A-Za-z-][0-9A-Za-z-]*))*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

    private final int major, minor, patch;
    private final @Nullable String preRelease, metadata;

    public Version(int major, int minor) {
        this(major, minor, 0, null, null);
    }

    public Version(int major, int minor, int patch, @Nullable String preRelease, @Nullable String metadata) {
        if(major < 0) throw new IllegalArgumentException("global version can't be below zero");
        if(minor < 0) throw new IllegalArgumentException("major version can't be below zero");
        if(patch < 0) throw new IllegalArgumentException("minor version can't be below zero");
        if(preRelease != null && !PRE_RELEASE_AND_METADATA_PATTERN.matcher(preRelease).find()) throw new IllegalArgumentException("Pre-release appendix should be [0-9A-Za-z-]+");
        if(metadata != null && !PRE_RELEASE_AND_METADATA_PATTERN.matcher(metadata).find()) throw new IllegalArgumentException("Metadata appendix should be [0-9A-Za-z-]+");
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.metadata = metadata;
    }

    /**
     * Parses version from string
     * @param raw string with version format
     * @throws IllegalArgumentException if malformed string is passed
     */
    public Version(String raw) throws IllegalArgumentException {
        if(raw.isEmpty()) throw new IllegalArgumentException("empty string");

        Matcher matcher = RAW_PATTERN.matcher(raw);
        if(!matcher.matches()) throw new IllegalArgumentException("Value is not valid SemVer string representation.");

        major = Integer.parseInt(matcher.group(1));
        minor = Integer.parseInt(matcher.group(2));

        String rawPatch = matcher.group(3);
        if(rawPatch != null) patch = Integer.parseInt(rawPatch);
        else patch = 0;

        preRelease = matcher.group(4);
        metadata = matcher.group(5);
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public @Nullable String preRelease() {
        return preRelease;
    }

    public @Nullable String metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Version version)) return false;
        return version.major == this.major
                && version.minor == minor
                && version.patch == this.patch;
    }

    /**
     * Formats version as string.
     * @return string in SemVer format
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(major).append('.').append(minor).append('.').append(patch);
        if(preRelease != null) builder.append("-").append(preRelease);
        if(metadata != null) builder.append("+").append(metadata);
        return builder.toString();
    }

    @Override
    public int compareTo(Version version) {
        if (this.major != version.major) return Integer.compare(this.major, version.major);
        else if (this.minor != version.minor) return Integer.compare(this.minor, version.minor);
        return Integer.compare(this.patch, version.patch);
    }

    @ApiStatus.Obsolete
    public static Version getMinecraftVersion() {
        return new Version(ServerBuildInfo.buildInfo().minecraftVersionId());
    }

}
