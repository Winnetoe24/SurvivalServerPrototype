package prototyp.survival.gameserver.gameserver.listener;

import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import prototyp.survival.gameserver.gameserver.GameServer;

@AllArgsConstructor
public class QuitListener implements Listener {

    private GameServer gameServer;


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameServer.getBlocked().remove(event.getPlayer());
        gameServer.getGruppe(event.getPlayer()).ifPresent(gruppe -> {
            gruppe.getPlayers().remove(event.getPlayer());
            if (gruppe.getPlayers().isEmpty()) {
                gameServer.getGruppes().remove(gruppe);
            }
        });
    }
}
