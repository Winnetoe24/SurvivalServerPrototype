package prototyp.survival.gameserver.gameserver.listener;

import lombok.AllArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.Gruppe;

import java.util.Optional;

@AllArgsConstructor
public class MoveListener implements Listener {
    private GameServer gameServer;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Block block = event.getTo().clone().add(0, -0.1, 0).getBlock();
        Optional<Gruppe> own = gameServer.getGruppe(event.getPlayer());
        Optional<Gruppe> over = gameServer.getGruppe(block);
        if (over.isEmpty()) return;
        if (own.isEmpty()) return;
        if (own.get() != over.get()) {
            over.get().getPlayers().forEach(player -> {
                if (!gameServer.getBlocked().contains(player)) {
                    player.damage(100000000, event.getPlayer());
                }
            });
        }
    }
}
