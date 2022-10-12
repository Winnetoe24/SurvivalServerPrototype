package prototyp.survival.gameserver.gameserver.listener;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.Gruppe;

@AllArgsConstructor
public class SpawnProtectionListener implements Listener {

    private GameServer gameServer;

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkEvent(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        checkEvent(event);
    }

    private void checkEvent(BlockEvent event) {
        if (gameServer.getGameworld() == null) return;
        if (!gameServer.getGameworld().equals(event.getBlock().getWorld())) return;
        for (Gruppe gruppe : gameServer.getGruppes()) {
            if (isSpawnBlock(event.getBlock().getLocation(), gruppe)) {
                ((Cancellable) event).setCancelled(true);
                return;
            }
        }
    }

    private boolean isSpawnBlock(Location location, Gruppe gruppe) {
        int divX = gruppe.getSpawn().getBlockX() - location.getBlockX();
        int divY = gruppe.getSpawn().getBlockY() - location.getBlockY();
        int divZ = gruppe.getSpawn().getBlockZ() - location.getBlockZ();

        if (divY < 2) {
            //Beacons and Above Beacons
            if ((divX == 0 || divX == 1) && (divZ == 0 || divZ == 1)) return true;
            //Beacon Base
            return divY == 1 && divX > -2 && divX < 3 && divZ > -2 && divZ < 3;
        }
        return false;
    }
}
