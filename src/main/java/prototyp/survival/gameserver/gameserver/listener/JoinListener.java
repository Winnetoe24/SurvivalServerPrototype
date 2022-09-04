package prototyp.survival.gameserver.gameserver.listener;

import lombok.AllArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.GameState;

@AllArgsConstructor
public class JoinListener implements Listener {

    private GameServer gameServer;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        if (gameServer.getState() == GameState.LOBBY) {
            Location location = event.getPlayer().getLocation().clone();
            location.setWorld(gameServer.getLobbyWorld());
            event.getPlayer().teleport(location);
        }
    }
}
