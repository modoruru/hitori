package su.hitori.plugin.container;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import su.hitori.api.container.Container;

public final class ContainerListener implements Listener {

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof Container container)
            container.click(event);
    }

    @EventHandler
    private void onInventoryDrag(InventoryDragEvent event) {
        if(event.getInventory().getHolder() instanceof Container container)
            container.drag(event);
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof Container container)
            container.close(event);
    }

}
