package prototyp.survival.gameserver.gameserver;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import prototyp.survival.gameserver.gameserver.command.JoinCommand;
import prototyp.survival.gameserver.gameserver.command.StartCommand;
import prototyp.survival.gameserver.gameserver.data.GameState;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.listener.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Getter
public final class GameServer extends JavaPlugin {

    private World gameworld;
    private int round = 0;
    @Setter
    private GameState state = GameState.LOBBY;

    private World lobbyWorld;

    private Set<Gruppe> gruppes = new HashSet<>();
    private Set<Player> blocked = new HashSet<>();

    public Optional<Gruppe> getGruppe(Player player) {
        for (Gruppe gruppe : gruppes) {
            if (gruppe.getPlayers().contains(player)) return Optional.of(gruppe);
        }
        return Optional.empty();
    }

    public Optional<Gruppe> getGruppe(Block block) {
        if (block.getType() != Material.BEACON) return Optional.empty();
        if (!block.getWorld().equals(gameworld)) return Optional.empty();
        for (Gruppe gruppe : gruppes) {
            if (gruppe.getSpawn() == null) continue;
            if (block.getLocation().distanceSquared(gruppe.getSpawn()) > 2.25) continue;
            if (gruppe.getSpawn().clone().add(-1, 0, 0).equals(block.getLocation())) return Optional.of(gruppe);
            if (gruppe.getSpawn().clone().add(-1, 0, -1).equals(block.getLocation())) return Optional.of(gruppe);
            if (gruppe.getSpawn().clone().add(0, 0, -1).equals(block.getLocation())) return Optional.of(gruppe);
            if (gruppe.getSpawn().clone().add(0, 0, 0).equals(block.getLocation())) return Optional.of(gruppe);
        }
        return Optional.empty();
    }


    @Override
    public void onEnable() {
        if (Bukkit.getWorld("lobby") != null) {
            lobbyWorld = Bukkit.getWorld("lobby");
        } else {
            lobbyWorld = Bukkit.createWorld(new WorldCreator("lobby"));
        }

        Bukkit.getPluginManager().registerEvents(new MoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AdvancementListener(this), this);
        Bukkit.getPluginCommand("join").setExecutor(new JoinCommand(this));
        StartCommand startCommand = new StartCommand(this);
        Bukkit.getPluginCommand("start").setExecutor(startCommand);
        Bukkit.getPluginCommand("skip").setExecutor(startCommand);
    }

    public void discardOldWorld() {
        Bukkit.unloadWorld(oldWorld, false);
        try {
            Files.walk(oldWorld.getWorldFolder().toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void generateWorld() {
        Random random = new Random();
        gameworld = new WorldCreator("world")
//                .environment(World.Environment.values()[random.nextInt(3)])
                .type(WorldType.values()[random.nextInt(WorldType.values().length)])
                .createWorld();
    }

    public void copyChunks() throws WorldEditException {
        round++;
        BukkitWorld bukkitWorld = new BukkitWorld(gameworld);
        for (Gruppe gruppe : gruppes) {
            CuboidRegion region = new CuboidRegion(
                    BlockVector3.at(gruppe.getSpawn().getX() - 16, -64, gruppe.getSpawn().getZ() - 16),
                    BlockVector3.at(gruppe.getSpawn().getX() + 15, 320, gruppe.getSpawn().getZ() + 15));
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    bukkitWorld, region, clipboard, region.getMinimumPoint()
            );
            // configure here
            Operations.complete(forwardExtentCopy);
            gruppe.setClipboard(clipboard);
        }
        Random random = new Random();
        oldWorld = gameworld;
        gameworld = new WorldCreator("gameworld_round_" + round + "_" + random.nextInt())
//                .environment(World.Environment.values()[random.nextInt(3)])
                .type(getValue(random))
                .createWorld();


    }

    private WorldType getValue(Random random) {
        int i;
        WorldType value;
        do {
            i = random.nextInt(WorldType.values().length + 10);
            value = WorldType.values()[i % WorldType.values().length];
        } while (i > 4 && value == WorldType.FLAT);
        return value;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
