package su.hitori.api.configuration.listener;

@FunctionalInterface
public interface FieldListener<T> {

    void handle(T oldValue, T newValue, ChangeCause changeCause);

}
