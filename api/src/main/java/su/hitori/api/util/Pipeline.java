package su.hitori.api.util;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Ordered map with function to add objects between each other.
 * @param <T>
 */
public class Pipeline<T> implements Iterable<T> {

    private final Map<Key, T> objects;
    private final List<Key> order;

    public Pipeline() {
        objects = new HashMap<>();
        order = new ArrayList<>();
    }

    public void addLast(Key key, T newObject) {
        addAfter(null, key, newObject);
    }

    public void addAfter(Key baseObject, @NotNull Key newObject, @NotNull T object) {
        if(order.contains(newObject)) return;

        int baseIndex = order.indexOf(baseObject);
        int newIndex;
        if(baseIndex == -1 || baseIndex >= objects.size()) newIndex = objects.size();
        else newIndex = baseIndex + 1;

        objects.put(newObject, object);
        order.add(newIndex, newObject);
    }

    /**
     * Adds an object before another object. If baseObject is null, adds object to the start of the pipeline.
     * @param baseObject key of the base object
     * @param newObject key of the new object
     * @param object new object
     */
    public void addBefore(Key baseObject, @NotNull Key newObject, @NotNull T object) {
        if(order.contains(newObject)) return;

        int baseIndex = order.indexOf(baseObject);
        int newIndex;
        if(baseIndex == -1 || baseIndex >= objects.size()) newIndex = 0;
        else newIndex = baseIndex;

        objects.put(newObject, object);
        order.add(newIndex, newObject);
    }

    public void remove(Key name) {
        objects.remove(name);
        order.remove(name);
    }

    public boolean containsKey(Key name) {
        return objects.containsKey(name);
    }

    /**
     * @return a copy of order list
     */
    public List<Key> order() {
        return List.copyOf(order);
    }

    /**
     * forEach based on order
     */
    @Override
    public void forEach(Consumer<? super T> consumer) {
        for (Key id : order) {
            T object = objects.get(id);
            if(object != null) consumer.accept(object);
        }
    }

    public @Nullable T get(Key name) {
        return objects.get(name);
    }

    public T get(int index) {
        return objects.get(order.get(index));
    }

    public int size() {
        return order.size();
    }

    public void sort(Comparator<T> comparator) {
        order.sort((first, second) -> comparator.compare(get(first), get(second)));
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return stream().iterator();
    }

    public Stream<T> stream() {
        return order().stream().map(objects::get);
    }

    public Stream<T> parallelStream() {
        return stream().parallel();
    }

}
