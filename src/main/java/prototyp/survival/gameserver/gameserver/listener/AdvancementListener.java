package prototyp.survival.gameserver.gameserver.listener;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.Gruppe;

import java.util.Optional;

@AllArgsConstructor
public class AdvancementListener implements Listener {

    private GameServer gameServer;

    @EventHandler
    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        Optional<Gruppe> current = gameServer.getGruppe(event.getPlayer());
        if (current.isEmpty()) return;
        Gruppe gruppe = current.get();
        gruppe.getPlayers().forEach(player -> {
            player.getAdvancementProgress(event.getAdvancement()).awardCriteria(event.getCriterion());
        });
    }
}
