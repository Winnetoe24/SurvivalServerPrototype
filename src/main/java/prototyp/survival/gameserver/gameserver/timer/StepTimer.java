package prototyp.survival.gameserver.gameserver.timer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import prototyp.survival.gameserver.gameserver.GameServer;

public class StepTimer {
    protected int round = -1;
    protected final GameServer gameServer;

    @Setter(AccessLevel.PROTECTED)
    private Runnable[] runnables;
    @Getter(AccessLevel.PROTECTED)
    private BukkitTask bukkitTask;

    public StepTimer(GameServer gameServer, Runnable... runnables) {
        this.gameServer = gameServer;
        this.runnables = runnables;
    }

    public void start() {
        if (bukkitTask != null) bukkitTask.cancel();

        round = -1;

        schedule();
    }

    private void schedule() {
        bukkitTask = Bukkit.getScheduler().runTaskTimer(gameServer, () -> {
            long l = System.currentTimeMillis();
            round++;
            System.out.println("round:"+round);
            if (round >= runnables.length) {
                bukkitTask.cancel();
                return;
            }
            runnables[round].run();
            Bukkit.getLogger().info("Zeit für Schritt:"+(System.currentTimeMillis()-l));


        }, 2L, 2L);
    }


    public void pause() {
        if (bukkitTask != null){
            bukkitTask.cancel();
            bukkitTask = null;
        }
    }

    public void resume() {
        if (bukkitTask == null)
            schedule();
    }

    public void end() {
        if (bukkitTask != null) bukkitTask.cancel();
    }
}
