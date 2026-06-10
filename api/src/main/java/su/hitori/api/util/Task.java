package su.hitori.api.util;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.Hitori;

import java.util.concurrent.TimeUnit;

/**
 * @author StreamVersus
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public final class Task {

    private static final boolean runningFolia = Hitori.instance().serverCoreInfo().isFolia();

    private final @Nullable BukkitTask bukkitTask;
    private final @Nullable ScheduledTask scheduledTask;

    private Task(@NotNull BukkitTask bukkitTask) {
        if(runningFolia) throw new IllegalStateException("Creating bukkit task on folia server");
        this.bukkitTask = bukkitTask;
        this.scheduledTask = null;
    }

    private Task(@NotNull ScheduledTask scheduledTask) {
        if(!runningFolia) throw new IllegalStateException("Creating folia task on bukkit server");
        this.bukkitTask = null;
        this.scheduledTask = scheduledTask;
    }

    public void cancel() {
        if(runningFolia) {
            assert scheduledTask != null;
            scheduledTask.cancel();
        }
        else {
            assert bukkitTask != null;
            bukkitTask.cancel();
        }
    }

    private static Task runBukkit(Runnable runnable, long delay) {
        return new Task(Bukkit.getScheduler().runTaskLater(plugin(), runnable, delay));
    }

    public static void ensureSync(Runnable runnable) {
        doTask(runnable, true);
    }

    public static void ensureAsync(Runnable runnable) {
        doTask(runnable, false);
    }

    private static void doTask(Runnable runnable, boolean sync) {
        if((Thread.currentThread() instanceof TickThread) == sync) {
            runnable.run();
            return;
        }

        if(sync) Bukkit.getScheduler().runTask(plugin(), runnable);
        else Bukkit.getScheduler().runTaskAsynchronously(plugin(), runnable);
    }

    public static Task runGlobally(Runnable runnable, long delay) {
        return runningFolia
                ? new Task(server().getGlobalRegionScheduler().runDelayed(plugin(), (_) -> runnable.run(), delay))
                : runBukkit(runnable, delay);
    }

    public static Task runLocation(Location location, Runnable runnable, long wait) {
        return runningFolia
                ? new Task(server().getRegionScheduler().runDelayed(plugin(), location, (_) -> runnable.run(), wait))
                : runBukkit(runnable, wait);
    }

    public static Task runEntity(Entity entity, Runnable runnable, long delay) {
        if(runningFolia) {
            ScheduledTask scheduledTask = entity.getScheduler().runDelayed(plugin(), (_) -> runnable.run(), null, delay);
            assert scheduledTask != null;
            return new Task(scheduledTask);
        }

        return runBukkit(runnable, delay);
    }

    public static Task async(Runnable runnable, long delay) {
        return runningFolia
                ? new Task(server().getAsyncScheduler().runDelayed(plugin(), (_) -> runnable.run(), delay * 50L, TimeUnit.MILLISECONDS))
                : new Task(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), runnable, delay));
    }

    private static Task runTaskTimerBukkit(Runnable runnable, long delay, long period) {
        return new Task(Bukkit.getScheduler().runTaskTimer(plugin(),  runnable, delay, period));
    }

    public static Task runTaskTimerGlobally(Runnable runnable, long delay, long period) {
        return runningFolia
                ? new Task(server().getGlobalRegionScheduler().runAtFixedRate(plugin(), (_) -> runnable.run(), delay, period))
                : runTaskTimerBukkit(runnable, delay, period);
    }

    public static Task runTaskTimerLocation(Location location, Runnable runnable, long delay, long period) {
        return runningFolia
                ? new Task(server().getRegionScheduler().runAtFixedRate(plugin(), location, (_) -> runnable.run(), delay, period))
                : runTaskTimerBukkit(runnable, delay, period);
    }

    public static Task runTaskTimerEntity(Entity entity, Runnable runnable, long delay, long period) {
        if(runningFolia) {
            ScheduledTask scheduledTask = entity.getScheduler().runAtFixedRate(plugin(), (_) -> runnable.run(), null, delay, period);
            assert scheduledTask != null;
            return new Task(scheduledTask);
        }

        return runTaskTimerBukkit(runnable, delay, period);
    }

    public static Task runTaskTimerAsync(Runnable runnable, long delay, long period) {
        return runningFolia
                ? new Task(server().getAsyncScheduler().runAtFixedRate(plugin(), (_) -> runnable.run(), delay * 50L, period, TimeUnit.MILLISECONDS))
                : new Task(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin(), runnable, delay, period));
    }

    private static Server server() {
        return Bukkit.getServer();
    }

    private static Plugin plugin() {
        return Hitori.instance().plugin();
    }

}
