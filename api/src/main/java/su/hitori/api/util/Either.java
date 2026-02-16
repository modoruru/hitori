package su.hitori.api.util;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * Class that holds A or B object. Can be used to pass choose of what argument to use
 * @param <A>
 * @param <B>
 */
public final class Either<A, B> {

    private final A first;
    private final B second;

    private Either(A first, B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns if first object is present.
     * @return if first object is present
     */
    public boolean firstPresent() {
        return first != null;
    }

    /**
     * Returns if second object is present.
     * @return if second object is present
     */
    public boolean secondPresent() {
        return second != null;
    }

    /**
     * Returns first object as optional
     * @return optional with first object or empty
     */
    public Optional<A> firstOptional() {
        return Optional.ofNullable(first);
    }

    /**
     * Returns second object as optional
     * @return optional with second object or empty
     */
    public Optional<B> secondOptional() {
        return Optional.ofNullable(second);
    }

    /**
     * If first object is present, returns it, otherwise throws an exception.
     * @return first object
     * @throws java.util.NoSuchElementException if no first is present
     */
    public A first() {
        return firstOptional().orElseThrow();
    }

    /**
     * If second object is present, returns it, otherwise throws an exception.
     * @return second object
     * @throws java.util.NoSuchElementException if no second is present
     */
    public B second() {
        return secondOptional().orElseThrow();
    }

    /**
     * Maps both objects to another Either (if present)
     * @param aMapper mapper for first (A) object
     * @param bMapper mapper for second (B) object
     * @return returns another Either with objects mapped
     * @param <A2> new type of first (A) object
     * @param <B2> new type of second (B) object
     */
    public <A2, B2> Either<A2, B2> map(Function<A, A2> aMapper, Function<B, B2> bMapper) {
        return new Either<>(
                firstOptional().map(aMapper).orElse(null),
                secondOptional().map(bMapper).orElse(null)
        );
    }

    /**
     * Returns Either containing first object
     * @param first not null first object
     * @return new Either holding first object
     * @param <A> first (A) object type
     * @param <B> second (B) object type
     */
    public static <A, B> Either<A, B> ofFirst(@NotNull A first) {
        return new Either<>(first, null);
    }

    /**
     * Returns Either containing second object
     * @param second not null second object
     * @return new Either holding second object
     * @param <A> first (A) object type
     * @param <B> second (B) object type
     */
    public static <A, B> Either<A, B> ofSecond(@NotNull B second) {
        return new Either<>(null, second);
    }

}
