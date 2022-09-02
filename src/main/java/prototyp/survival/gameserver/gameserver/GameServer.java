package prototyp.survival.gameserver.gameserver;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import prototyp.survival.gameserver.gameserver.command.JoinCommand;
import prototyp.survival.gameserver.gameserver.command.StartCommand;
import prototyp.survival.gameserver.gameserver.data.GameState;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.listener.MoveListener;
import prototyp.survival.gameserver.gameserver.listener.PlayerDeathListener;

import java.util.*;

@Getter
public final class GameServer extends JavaPlugin {


    @Setter
    private GameState state = GameState.LOBBY;

    private Set<Gruppe> gruppes = new HashSet<>();

    public Optional<Gruppe> getGruppe(Player player) {
        for (Gruppe gruppe : gruppes) {
            if (gruppe.getPlayers().contains(player)) return Optional.of(gruppe);
        }
        return Optional.empty();
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new MoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginCommand("join").setExecutor(new JoinCommand(this));
        Bukkit.getPluginCommand("start").setExecutor(new StartCommand(this));

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
