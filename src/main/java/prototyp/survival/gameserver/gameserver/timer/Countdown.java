package prototyp.survival.gameserver.gameserver.timer;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import prototyp.survival.gameserver.gameserver.GameServer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Countdown {
    private GameServer gameServer;
    private final int maxRounds;
    private long ticksBetween;
    private long ticksBefore;
    private int round = 0;
    private Set<Consumer<Integer>> callbacks = new HashSet<>();
    private BukkitTask bukkitTask;

    public Countdown(GameServer gameServer, int timeBetween, TimeUnit timeUnit, int timeBefore, TimeUnit timeUnitBefore, int maxRounds) {
        this.gameServer = gameServer;
        this.maxRounds = maxRounds;
        ticksBetween = timeUnit.toMillis(timeBetween) / 50L;
        ticksBefore = timeUnitBefore.toMillis(timeBefore) / 50L;
    }

    public Countdown(GameServer gameServer, int timeBetween, TimeUnit timeUnit, int maxRounds) {
        this(gameServer, timeBetween, timeUnit, timeBetween, timeUnit, maxRounds);
    }

    public void start() {
        if (bukkitTask != null) bukkitTask.cancel();
        bukkitTask = Bukkit.getScheduler().runTaskTimer(gameServer, () -> {
            bukkitTask = null;
            round++;
            callbacks.forEach(integerConsumer -> integerConsumer.accept(round));
            if (round >= maxRounds) {
                end();
            }
        }, ticksBefore, ticksBetween);
    }

    public void add(Consumer<Integer> consumer) {
        callbacks.add(consumer);
    }

    public void remove(Consumer<Integer> consumer) {
        callbacks.remove(consumer);
    }

    public void end() {
        if (bukkitTask != null) bukkitTask.cancel();
        callbacks.clear();
    }
}
