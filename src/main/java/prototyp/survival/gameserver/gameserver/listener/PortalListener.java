package prototyp.survival.gameserver.gameserver.listener;

import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import lombok.AllArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import prototyp.survival.gameserver.gameserver.GameServer;

@AllArgsConstructor
public class PortalListener implements Listener {
    private GameServer gameServer;
    @EventHandler(ignoreCancelled = true)
    public void onEntityPortalReady(EntityPortalReadyEvent event) {
        switch (event.getPortalType()) {
            case NETHER -> {
                if (event.getEntity().getWorld().equals(gameServer.getGameworld())) {
                    if (gameServer.getGameworldNether() == null) {
                        gameServer.generateNether();
                        event.setCancelled(true);
                    }else {
                        event.setTargetWorld(gameServer.getGameworldNether());
                    }

                }
                else{

                        event.setTargetWorld(gameServer.getGameworld());


                }
            }
            case ENDER -> {
                if (event.getEntity().getWorld().equals(gameServer.getGameworld()))

                    if (gameServer.getGameworldEnd() == null) {
                        gameServer.generateEnd();
                        event.setCancelled(true);
                    }else {
                        event.setTargetWorld(gameServer.getGameworldEnd());
                    }
                else
                    event.setTargetWorld(gameServer.getGameworld());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {

    }
}
