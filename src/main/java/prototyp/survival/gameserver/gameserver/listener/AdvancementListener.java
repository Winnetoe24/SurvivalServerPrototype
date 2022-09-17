package prototyp.survival.gameserver.gameserver.listener;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import com.google.common.collect.Lists;
import io.papermc.paper.advancement.AdvancementDisplay;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.command.StartCommand;
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
            case GOAL -> 5;
            case CHALLENGE -> 16;
        });
        if (frame == AdvancementDisplay.Frame.CHALLENGE) {
            event.getPlayer().getInventory().addItem(genBall());
        }
    }

    private ItemStack genBall() {
        ItemStack itemStack = new ItemStack(Material.STICK,1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Magic 8 Ball", StartCommand.PURPLE));
        itemMeta.setUnbreakable(true);
        itemMeta.lore(BallListener.NOT_USED_LORE);
        itemMeta.setCustomModelData(1);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
