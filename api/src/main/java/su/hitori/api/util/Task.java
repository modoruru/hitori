package su.hitori.api.util;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.Hitori;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author StreamVersus
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public final class Task {

    private static final boolean runningFolia = Hitori.instance().serverCoreInfo().isFolia();

    private final @Nullable BukkitTask bukkitTask;
    private final @Nullable ScheduledTask scheduledTask;

    private Task(BukkitTask bukkitTask) {
        if(runningFolia) throw new IllegalStateException("Creating bukkit task on folia server");
        this.bukkitTask = bukkitTask;
        this.scheduledTask = null;
    }

    private Task(@Nullable ScheduledTask scheduledTask) {
        if(!runningFolia) throw new IllegalStateException("Creating folia task on bukkit server");
        if(scheduledTask == null) throw new IllegalArgumentException("Entity is already removed so task can't be created.");
        this.bukkitTask = null;
        this.scheduledTask = scheduledTask;
    }

    @ApiStatus.Internal
    public @Nullable BukkitTask bukkitTask() {
        return bukkitTask;
    }

    @ApiStatus.Internal
    public @Nullable ScheduledTask foliaTask() {
        return scheduledTask;
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

    private static BukkitTask runBukkit(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin(), runnable, delay);
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

        if(sync) Bukkit.getGlobalRegionScheduler().run(plugin(), _ -> runnable.run());
        else Bukkit.getAsyncScheduler().runNow(plugin(), _ -> runnable.run());
    }

    private static Task create(Supplier<BukkitTask> bukkit, Supplier<@Nullable ScheduledTask> folia) {
        return runningFolia ? new Task(folia.get()) : new Task(bukkit.get());
    }

    private static long fixupDelay(long delay) {
        if(runningFolia && delay < 1L) return 1L;
        return delay;
    }

    public static Task runGlobally(Runnable runnable, long delay) {
        return create(
                () -> runBukkit(runnable, delay),
                () -> server().getGlobalRegionScheduler().runDelayed(plugin(), (_) -> runnable.run(), fixupDelay(delay))
        );
    }

    public static Task runLocation(Location location, Runnable runnable, long delay) {
        return create(
                () -> runBukkit(runnable, delay),
                () -> server().getRegionScheduler().runDelayed(plugin(), location, (_) -> runnable.run(), fixupDelay(delay))
        );
    }

    public static Task runEntity(Entity entity, Runnable runnable, long delay) {
        return create(
                () -> runBukkit(runnable, delay),
                () -> entity.getScheduler().runDelayed(plugin(), (_) -> runnable.run(), null, fixupDelay(delay))
        );
    }

    public static Task async(Runnable runnable, long delay) {
        return create(
                () -> Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), runnable, delay),
                () -> server().getAsyncScheduler().runDelayed(plugin(), (_) -> runnable.run(), fixupDelay(delay) * 50L, TimeUnit.MILLISECONDS)
        );
    }

    private static BukkitTask runTaskTimerBukkit(Runnable runnable, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin(),  runnable, delay, period);
    }

    public static Task runTaskTimerGlobally(Runnable runnable, long delay, long period) {
        return create(
                () -> runTaskTimerBukkit(runnable, delay, period),
                () -> server().getGlobalRegionScheduler().runAtFixedRate(plugin(), (_) -> runnable.run(), fixupDelay(delay), period)
        );
    }

    public static Task runTaskTimerLocation(Location location, Runnable runnable, long delay, long period) {
        return create(
                () -> runTaskTimerBukkit(runnable, delay, period),
                () -> server().getRegionScheduler().runAtFixedRate(plugin(), location, (_) -> runnable.run(), fixupDelay(delay), period)
        );
    }

    public static Task runTaskTimerEntity(Entity entity, Runnable runnable, long delay, long period) {
        return create(
                () -> runTaskTimerBukkit(runnable, delay, period),
                () -> entity.getScheduler().runAtFixedRate(plugin(), (_) -> runnable.run(), null, fixupDelay(delay), period)
        );
    }

    public static Task runTaskTimerAsync(Runnable runnable, long delay, long period) {
        return create(
                () -> Bukkit.getScheduler().runTaskTimerAsynchronously(plugin(), runnable, delay, period),
                () -> server().getAsyncScheduler().runAtFixedRate(plugin(), (_) -> runnable.run(), fixupDelay(delay) * 50L, period * 50L, TimeUnit.MILLISECONDS)
        );
    }

    private static Server server() {
        return Bukkit.getServer();
    }

    private static Plugin plugin() {
        return Hitori.instance().plugin();
    }

}
