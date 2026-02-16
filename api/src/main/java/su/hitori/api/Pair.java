package su.hitori.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

/**
 * Pair holding two non-null objects
 * @param first first object
 * @param second second object
 * @param <A> first object type
 * @param <B> second object type
 */
public record Pair<A, B>(@NotNull A first, @NotNull B second) {

    /**
     * Maps both objects to another Pair
     * @param firstMapper mapper for first object
     * @param secondMapper mapper for second object
     * @return returns new Pair with objects mapped
     * @param <A2> new type of first object
     * @param <B2> new type of second object
     */
    public <A2, B2> Pair<A2, B2> map(Function<A, A2> firstMapper, Function<B, B2> secondMapper) {
        return Pair.of(firstMapper.apply(first), secondMapper.apply(second));
    }

    @Override
    public @NotNull String toString() {
        return first + " " + second;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair<?, ?>(Object first1, Object second1))) return false;
        return first1 == first && second1 == second;
    }

    /**
     * Creates a Pair of two objects
     * @param a first object
     * @param b second object
     * @return Pair holding first and second objects
     * @param <A> first object type
     * @param <B> second object type
     */
    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    /**
     * Create a Pair from map entry where key stored as first and value as second
     * @param entry map entry
     * @return Pair holding key and value from entry
     * @param <A> first (key) object type
     * @param <B> second (value) object type
     */
    public static <A, B> Pair<A, B> fromMapEntry(Map.Entry<A, B> entry) {
        return of(entry.getKey(), entry.getValue());
    }

}
