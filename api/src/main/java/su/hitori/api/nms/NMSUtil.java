package su.hitori.api.nms;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class NMSUtil {

    private NMSUtil() {}

    public static ServerPlayer asNMS(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

}
