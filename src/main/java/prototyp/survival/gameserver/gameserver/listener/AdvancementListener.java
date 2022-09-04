package prototyp.survival.gameserver.gameserver.listener;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import io.papermc.paper.advancement.AdvancementDisplay;
import lombok.AllArgsConstructor;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
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

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Optional<Gruppe> current = gameServer.getGruppe(event.getPlayer());
        if (current.isEmpty()) return;
        Gruppe gruppe = current.get();
        if (!gruppe.getFinishedAdvancements().add(event.getAdvancement())) {
            event.message(null);
        }
        AdvancementDisplay advancementDisplay = event.getAdvancement().getDisplay();
        if (advancementDisplay == null) return;
        AdvancementDisplay.Frame frame = advancementDisplay.frame();
        event.getPlayer().setLevel(event.getPlayer().getLevel() + switch (frame) {
            case TASK -> 1;
            case CHALLENGE -> 5;
            case GOAL -> 16;
        });
    }
}
