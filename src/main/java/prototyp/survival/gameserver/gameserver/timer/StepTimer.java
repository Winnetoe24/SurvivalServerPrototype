package prototyp.survival.gameserver.gameserver.timer;

import lombok.AccessLevel;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import prototyp.survival.gameserver.gameserver.GameServer;

public class StepTimer {
    protected int round = 0;
    protected final GameServer gameServer;

    @Setter(AccessLevel.PROTECTED)
    private Runnable[] runnables;
    private BukkitTask bukkitTask;

    public StepTimer(GameServer gameServer, Runnable... runnables) {
        this.gameServer = gameServer;
        this.runnables = runnables;
    }

    public void start() {
        if (bukkitTask != null) bukkitTask.cancel();

        round = 0;
        bukkitTask = Bukkit.getScheduler().runTaskTimer(gameServer, () -> {
            long l = System.currentTimeMillis();
            runnables[round].run();
            Bukkit.getLogger().info("Zeit für Schritt:"+(System.currentTimeMillis()-l));
            round++;
            System.out.println("round:"+round);
            if (round >= runnables.length) {
                bukkitTask.cancel();
            }
        }, 2L, 2L);
    }

    public void end() {
        if (bukkitTask != null) bukkitTask.cancel();
    }
}
