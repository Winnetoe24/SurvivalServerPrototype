package prototyp.survival.gameserver.gameserver.listener;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.command.StartCommand;
import prototyp.survival.gameserver.gameserver.data.Gruppe;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class BallListener implements Listener {

    public static final List<Component> NOT_USED_LORE = Lists.newArrayList(Component.text("Nutze ihn um, die geringste Entfernung"), Component.text("zwischen deinem Spawn und dem eines Gegners zu bestimmen"));

    private GameServer gameServer;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        if (!item.getType().equals(Material.STICK)) return;
        if (item.getItemMeta().getCustomModelData() != 1) return;
        List<Component> lore = item.lore();
        if (lore == null) return;
        if (!lore.containsAll(NOT_USED_LORE)) return;

        Optional<Gruppe> optGruppe = gameServer.getGruppe(event.getPlayer());
        if (optGruppe.isEmpty()) return;
        Gruppe gruppe = optGruppe.get();
        Location spawn = gruppe.getSpawn();
        Location nearest = null;
        double distance = 0;
        for (Gruppe gameServerGruppe : gameServer.getGruppes()) {
            if ( gameServerGruppe.getSpawn() != null && (nearest == null || gameServerGruppe.getSpawn().distanceSquared(spawn) < distance)) {
                nearest = gameServerGruppe.getSpawn();
                distance = nearest.distanceSquared(spawn);
            }
        }
        if (nearest == null) {
            event.getPlayer().sendMessage(Component.text("Es konnte keine andere Gruppe gefunden werden", StartCommand.RED));
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.lore(Lists.newArrayList(Component.text("Die Entfernung zum nächsten Gegnerischen Spawn beträgt:"+Math.sqrt(distance))));
        item.setItemMeta(itemMeta);
    }
}
