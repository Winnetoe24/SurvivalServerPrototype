package prototyp.survival.gameserver.gameserver;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
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
import org.bukkit.util.BlockVector;
import prototyp.survival.gameserver.gameserver.command.JoinCommand;
import prototyp.survival.gameserver.gameserver.command.StartCommand;
import prototyp.survival.gameserver.gameserver.data.GameState;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.listener.JoinListener;
import prototyp.survival.gameserver.gameserver.listener.MoveListener;
import prototyp.survival.gameserver.gameserver.listener.PlayerDeathListener;
import prototyp.survival.gameserver.gameserver.listener.QuitListener;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Getter
public final class GameServer extends JavaPlugin {

    private World gameworld;
    private int round = 0;
    @Setter
    private GameState state = GameState.LOBBY;

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
        for (Gruppe gruppe : gruppes) {
            if (gruppe.getSpawn().clone().add(-1, 0, 0).equals(block.getLocation())) return Optional.of(gruppe);
            if (gruppe.getSpawn().clone().add(-1, 0, -1).equals(block.getLocation())) return Optional.of(gruppe);
            if (gruppe.getSpawn().clone().add(0, 0, -1).equals(block.getLocation())) return Optional.of(gruppe);
            if (gruppe.getSpawn().clone().add(0, 0, 0).equals(block.getLocation())) return Optional.of(gruppe);
        }
        return Optional.empty();
    }

    @Override
    public void onEnable() {
        gameworld = Bukkit.getWorlds().get(0);
        Bukkit.getPluginManager().registerEvents(new MoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginCommand("join").setExecutor(new JoinCommand(this));
        Bukkit.getPluginCommand("start").setExecutor(new StartCommand(this));
    }

    public void zurNeuenWelt() throws WorldEditException {
        round++;
        BukkitWorld bukkitWorld = new BukkitWorld(gameworld);
        for (Gruppe gruppe : gruppes) {
            CuboidRegion region = new CuboidRegion(
                    BlockVector3.at(gruppe.getSpawn().getX()-16, -64, gruppe.getSpawn().getZ()-16),
                    BlockVector3.at(gruppe.getSpawn().getX()+15, 320, gruppe.getSpawn().getZ()+15));
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    bukkitWorld, region, clipboard, region.getMinimumPoint()
            );
            // configure here
            Operations.complete(forwardExtentCopy);
            gruppe.setClipboard(clipboard);
            gruppe.setSpawn(null);
        }
        Random random = new Random();
        gameworld= new WorldCreator("gameworld_round_" + round)
                .environment(World.Environment.values()[random.nextInt(3)])
                .type(WorldType.values()[random.nextInt(WorldType.values().length)])
                .createWorld();



    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
