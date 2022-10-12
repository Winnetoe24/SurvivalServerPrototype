package prototyp.survival.gameserver.gameserver.listener;

import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
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
                    } else {
                        event.setTargetWorld(gameServer.getGameworldNether());
                    }

                } else {

                    event.setTargetWorld(gameServer.getGameworld());


                }
            }
            case ENDER -> {
                if (event.getEntity().getWorld().equals(gameServer.getGameworld()))

                    if (gameServer.getGameworldEnd() == null) {
                        gameServer.generateEnd();
                        event.setCancelled(true);
                    } else {
                        event.setTargetWorld(gameServer.getGameworldEnd());
                    }
                else
                    event.setTargetWorld(gameServer.getGameworld());
            }
        }
        System.out.println(event);
    }

    @EventHandler()
    public void onEntityPortal(EntityPortalEvent event) {
        System.out.println(event);
        System.out.println(event.getEventName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTP(PlayerTeleportEvent event) {
        Location to = null;
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (event.getPlayer().getWorld().equals(gameServer.getGameworld()))

                if (gameServer.getGameworldEnd() == null) {

                    gameServer.generateEnd();
                    event.setCancelled(true);
                } else {
                    to = event.getTo();
                    to.setWorld(gameServer.getGameworldEnd());
                    Location under = to.clone().add(0, -2, 0);
                    if (under.getBlock().getType() == Material.AIR) {
                        under.getBlock().setType(Material.OBSIDIAN);
                        under = to.add(0, 0, 1);
                        under.getBlock().setType(Material.OBSIDIAN);
                        under = to.add(0, 0, -2);
                        under.getBlock().setType(Material.OBSIDIAN);

                        under = to.add(1, 0, 0);
                        under.getBlock().setType(Material.OBSIDIAN);
                        under = to.add(0, 0, 1);
                        under.getBlock().setType(Material.OBSIDIAN);
                        under = to.add(0, 0, 1);
                        under.getBlock().setType(Material.OBSIDIAN);

                        under = to.add(-2, 0, 0);
                        under.getBlock().setType(Material.OBSIDIAN);
                        under = to.add(0, 0, -1);
                        under.getBlock().setType(Material.OBSIDIAN);
                        under = to.add(0, 0, -1);
                        under.getBlock().setType(Material.OBSIDIAN);
                    }
                    event.setTo(to);
                }
            else {
                to = event.getTo();
                to.setWorld(gameServer.getGameworld());
                event.setTo(to);
            }


        }
    }

}
