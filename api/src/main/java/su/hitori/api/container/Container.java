package su.hitori.api.container;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Macro for custom menus to handle events. Framework will redirect all events to methods below.
 * There's no need to register listeners - just implement this class.
 */
public interface Container extends InventoryHolder {

    void click(InventoryClickEvent event);

    void drag(InventoryDragEvent event);

    default void close(InventoryCloseEvent event) {}

}
