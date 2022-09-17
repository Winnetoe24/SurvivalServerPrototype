package prototyp.survival.gameserver.gameserver;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Getter
public final class GameServer extends JavaPlugin {

    private World gameworld;
    private World gameworldNether;
    private World gameworldEnd;


    private Audience audience = Audience.empty();
    private BossBar bossBar = BossBar.bossBar(Component.text("In der Lobby"), 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

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
        audience.showBossBar(bossBar);
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
        Bukkit.getPluginManager().registerEvents(new BallListener(this), this);
        Bukkit.getPluginCommand("join").setExecutor(new JoinCommand(this));
        StartCommand startCommand = new StartCommand(this);
        Bukkit.getPluginCommand("start").setExecutor(startCommand);
        Bukkit.getPluginCommand("skip").setExecutor(startCommand);
        startCommand.broadcastLobby();

        loadChunks();
    }

    public void discardWorld() {
        unloadWorld(gameworld);
        unloadWorld(gameworldNether);
        unloadWorld(gameworldEnd);
        gameworldNether = null;
        gameworldEnd = null;
        System.out.println("World discarded");
    }

    public void regenerateWorld() {
        Random random = new Random();
        String worldname = "gameworld_round_" + round + "_" + random.nextInt();
        audience.sendActionBar(Component.text("Erstelle Overworld...", StartCommand.YELLOW));
        long l = System.currentTimeMillis();
        gameworld = new WorldCreator(worldname)
                .environment(World.Environment.NORMAL)
                .type(getWorldType(random))
                .createWorld();
        System.out.println("Worldgen:" + (System.currentTimeMillis() - l));
        audience.sendActionBar(Component.text("Fertig stellen...", StartCommand.YELLOW));


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
//            try {
//                Files.walk(world.getWorldFolder().toPath())
//                        .sorted(Comparator.reverseOrder())
//                        .map(Path::toFile)
//                        .forEach(File::delete);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
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
        saveChunks();
    }


    private void loadChunks() {
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".chunks"));
        if (files == null) return;
        for (File file : files) {
            loadChunk(file);
        }
    }

    public void loadCunks(Gruppe gruppe) {
        loadCunks(new File(this.getDataFolder(), gruppe.getGroupID() + ".chunks"), gruppe);
    }


    private void loadChunk(File file) {
        String groupName = file.getName().replace(".chunks", "");
        Gruppe gruppe = gruppes.stream().filter(lGruppe -> lGruppe.getGroupID().equals(groupName)).findAny().orElse(null);
        loadCunks(file, gruppe);
    }

    private void loadCunks(File file, Gruppe gruppe) {
        if (gruppe == null) return;
        if (!file.exists()) return;
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            gruppe.setClipboard(reader.read());
            gruppe.setRecalculateY(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveChunks() {
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        for (Gruppe gruppe : gruppes) {
            saveChunks(gruppe);
        }
    }




    public void saveChunks(Gruppe gruppe) {
        if (gruppe == null) return;
        File file = new File(this.getDataFolder(), gruppe.getGroupID()+".chunks");
        if (gruppe.getClipboard() == null) return;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
                writer.write(gruppe.getClipboard());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
