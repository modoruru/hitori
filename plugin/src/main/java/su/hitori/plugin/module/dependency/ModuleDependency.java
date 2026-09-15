package su.hitori.plugin.module.dependency;

import su.hitori.api.Version;

public final class ModuleDependency extends Dependency<Version> {

    public final Type type;

    public ModuleDependency(Version base, Operator operator, Type type) {
        super(base, operator);
        this.type = type;
    }

    public enum Type {
        SOFT, HARD
    }

}
