package prototyp.survival.gameserver.gameserver.command;

import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.Gruppe;

@AllArgsConstructor
public class JoinCommand implements CommandExecutor {
    private GameServer gameServer;
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (gameServer.getGruppes().stream().noneMatch(gruppe -> gruppe.getGroupID().equals(args[0]))) {
            gameServer.getGruppes().add(new Gruppe(args[0]));
        }
        if (gameServer.getGruppe(player).isPresent()) {
            player.sendMessage("Du bist schon in einer Gruppe");
            return false;
        }
        Gruppe gruppe1 = gameServer.getGruppes().stream().filter(gruppe -> gruppe.getGroupID().equals(args[0])).findAny().get();
        gruppe1.getPlayers().add(player);
        player.sendMessage("Zur Gruppe "+args[0]+" hinzugefügt");
        return false;
    }
}
