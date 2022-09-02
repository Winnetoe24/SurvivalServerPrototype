package prototyp.survival.gameserver.gameserver.listener;

import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.GameState;

@AllArgsConstructor
public class MoveListener implements Listener {
    private GameServer gameServer;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        event.setCancelled(gameServer.getState() != GameState.RUNNING);
    }
}
