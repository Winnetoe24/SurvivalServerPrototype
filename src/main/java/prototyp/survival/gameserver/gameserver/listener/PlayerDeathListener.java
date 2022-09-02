package prototyp.survival.gameserver.gameserver.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import prototyp.survival.gameserver.gameserver.GameServer;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PlayerDeathListener implements Listener {
    private final GameServer gameServer;
    private List<Player> blocked = new ArrayList<>();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getPlayer().getKiller() != null) {
            gameServer.getGruppe(event.getPlayer()).ifPresent(gruppe -> gruppe.setPoints(gruppe.getPoints() - 100));
            gameServer.getGruppe(event.getPlayer().getKiller()).ifPresent(gruppe -> gruppe.setPoints(gruppe.getPoints() + 100));
            blocked.add(event.getEntity());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (blocked.contains(event.getPlayer())) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        } else {
             gameServer.getGruppe(event.getPlayer()).ifPresent(gruppe -> event.setRespawnLocation(gruppe.getSpawn()));
        }
    }
}
