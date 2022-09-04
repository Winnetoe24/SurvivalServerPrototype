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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import prototyp.survival.gameserver.gameserver.command.JoinCommand;
import prototyp.survival.gameserver.gameserver.command.StartCommand;
import prototyp.survival.gameserver.gameserver.data.GameState;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.listener.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Getter
public final class GameServer extends JavaPlugin {

    private World gameworld;
    private World gameworldNether;
    private World gameworldEnd;

    @Setter
    private Audience audience = Audience.empty();
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
        audience = Audience.audience(Bukkit.getOnlinePlayers());
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
        Bukkit.getPluginManager().registerEvents(new PortalListener(this), this);
        Bukkit.getPluginCommand("join").setExecutor(new JoinCommand(this));
        StartCommand startCommand = new StartCommand(this);
        Bukkit.getPluginCommand("start").setExecutor(startCommand);
        Bukkit.getPluginCommand("skip").setExecutor(startCommand);
        startCommand.broadcastLobby();
    }

    public void discardWorld() {
        unloadWorld(gameworld);
        unloadWorld(gameworldNether);
        unloadWorld(gameworldEnd);
        gameworldNether = null;
        gameworldEnd = null;
        System.out.println("World discarded");
    }

    public void regenerateWorld(Runnable runAfter) {


        Random random = new Random();
        String worldname = "gameworld_round_" + round + "_" + random.nextInt();
        audience.sendActionBar(Component.text("Erstelle Overworld...", StartCommand.YELLOW));
        gameworld = new WorldCreator(worldname)
                .environment(World.Environment.NORMAL)
                .type(getWorldType(random))
                .createWorld();
        audience.sendActionBar(Component.text("Fertig stellen...", StartCommand.YELLOW));

        Bukkit.getScheduler().runTaskLater(this, runAfter, 10L);
    }

    public void generateNether() {
        audience.sendActionBar(Component.text("Erstelle Nether...", StartCommand.YELLOW));
        gameworldNether = new WorldCreator(gameworld.getName() + "_nether")
                .environment(World.Environment.NETHER)
                .createWorld();

    }

    public void generateEnd() {
        audience.sendActionBar(Component.text("Erstelle End...", StartCommand.YELLOW));
        gameworldEnd = new WorldCreator(gameworld.getName() + "_end")
                .environment(World.Environment.THE_END)
                .createWorld();
    }

    private void unloadWorld(World world) {
        if (world != null) {
            Bukkit.unloadWorld(world, false);
            try {
                Files.walk(world.getWorldFolder().toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    }

    private WorldType getWorldType(Random random) {
        int i = random.nextInt(10);
        int length = WorldType.values().length;
        if (i > length && WorldType.values()[i % length] == WorldType.FLAT) {
            i += random.nextInt(2) - 1;
        }
        return WorldType.values()[i % length];
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
