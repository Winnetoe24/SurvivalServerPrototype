package prototyp.survival.gameserver.gameserver.data;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@RequiredArgsConstructor
public class Gruppe {
    private final String groupID;
    private List<Player> players = new ArrayList<>();
    private Location spawn;
    private int[][] chunks;
    private long points = 0;
    private Clipboard clipboard;
    private SpawnPositionState spawnPositionState = SpawnPositionState.NONE;
    private boolean recalculateY = false;
    private Set<Advancement> finishedAdvancements = new HashSet<>();

    public void disableBeacons() {
        World world = this.getSpawn().getWorld();
        for (int x = -2; x < 2; x++) {
            for (int z = -2; z < 2; z++) {
                world.getBlockAt(this.getSpawn().clone().add(x, -1, z)).setType(Material.BEDROCK);

            }
        }


    }
    public void enableBeacons() {
        World world = this.getSpawn().getWorld();
        for (int x = -2; x < 2; x++) {
            for (int z = -2; z < 2; z++) {
                world.getBlockAt(this.getSpawn().clone().add(x, -1, z)).setType(Material.IRON_BLOCK);

            }
        }

    }

    public boolean inside(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        if (chunks == null) return false;
        for (int[] ints : chunks) {
            if (chunk.getX() == ints[0] && chunk.getZ() == ints[1]) return true;
        }
        return false;
    }
}
