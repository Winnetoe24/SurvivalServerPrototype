package prototyp.survival.gameserver.gameserver.listener;

import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import prototyp.survival.gameserver.gameserver.GameServer;

@AllArgsConstructor
public class PortalListener implements Listener {
    private GameServer gameServer;
    @EventHandler(ignoreCancelled = true)
    public void onEntityPortalReady(EntityPortalReadyEvent event) {
        switch (event.getPortalType()) {
            case NETHER -> event.setTargetWorld(gameServer.getGameworldNether());
            case ENDER -> event.setTargetWorld(gameServer.getGameworldEnd());
        }
    }
}
