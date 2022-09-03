package prototyp.survival.gameserver.gameserver.timer;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import prototyp.survival.gameserver.gameserver.GameServer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Timer {
    private GameServer gameServer;
    private long ticksToWait;
    private BukkitTask bukkitTask;

    private Set<Runnable> callbacks = new HashSet<>();

    public Timer(GameServer gameServer, int duration, TimeUnit timeUnit) {
        this.gameServer = gameServer;
        ticksToWait = timeUnit.toMillis(duration) / 50L;
    }

    public BukkitTask start() {
        if (bukkitTask != null) bukkitTask.cancel();
        bukkitTask = Bukkit.getScheduler().runTaskLater(gameServer, () -> {
            bukkitTask = null;
            callbacks.forEach(Runnable::run);
            callbacks.clear();
        },ticksToWait);
        return bukkitTask;
    }

    public void add(Runnable runnable) {
        callbacks.add(runnable);
    }

    public void remove(Runnable runnable) {
        callbacks.remove(runnable);
    }

    public void end() {
        if (bukkitTask != null) bukkitTask.cancel();
        callbacks.clear();
    }
}
