package su.hitori.api.container;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractContainer implements Container {

    public static final int
            ONE_ROW = 9,
            TWO_ROWS = ONE_ROW * 2,
            THREE_ROWS = ONE_ROW * 3,
            FOUR_ROWS = ONE_ROW * 4,
            FIVE_ROWS = ONE_ROW * 5,
            SIX_ROWS = ONE_ROW * 6;

    protected final Player player;
    protected Inventory inventory;

    protected AbstractContainer(Player player) {
        this.player = player;
    }

    public abstract Inventory create();

    @Override
    public @NotNull Inventory getInventory() {
        if(inventory == null) inventory = create();
        return inventory;
    }

}
