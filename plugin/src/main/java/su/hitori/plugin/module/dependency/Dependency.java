package su.hitori.plugin.module.dependency;

import org.jspecify.annotations.Nullable;

public sealed class Dependency<V extends Comparable<V>> permits ModuleDependency {

    public final V base;
    public final Operator operator;

    public Dependency(V base, Operator operator) {
        this.base = base;
        this.operator = operator;
    }

    public static @Nullable Operator readOperator(String string) {
        char firstCharacter = string.charAt(0);
        if (firstCharacter == '=') return Operator.EQUALS;

        char secondCharacter = string.charAt(1);
        if (firstCharacter == '>') return secondCharacter == '=' ? Operator.GREATER_OR_EQUALS : Operator.GREATER;

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
