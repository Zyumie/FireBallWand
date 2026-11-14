package fr.zyumie.Home;

import fr.zyumie.Chat.Messages;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Menu /home : une ligne de 9 cases, 5 emplacements de home au centre. */
public final class HomeGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Main plugin;
    private final HomeManager manager;
    private final Player player;
    private final Inventory inventory;

    public HomeGui(Main plugin, HomeManager manager, Player player) {
        this.plugin = plugin;
        this.manager = manager;
        this.player = player;
        String title = plugin.getHomeConfig().getString("messages.gui-title", "<dark_gray>Gestion de vos Homes");
        this.inventory = Bukkit.createInventory(this, 9, Messages.parse(title));
        loadItems();
    }

    /** Texte d'item sans l'italique par défaut ; les valeurs dynamiques passent en « unparsed » (aucune injection). */
    private static Component text(String mini, TagResolver... resolvers) {
        return MM.deserialize(mini, resolvers).decoration(TextDecoration.ITALIC, false);
    }

    private void loadItems() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int slot = 0; slot < 9; slot++) {
            if (slot < 2 || slot > 6) inventory.setItem(slot, filler);
        }

        List<Map.Entry<String, HomeData>> homesList = new ArrayList<>(manager.getHomes(player.getUniqueId()).entrySet());
        int maxHomes = manager.getMaxHomes(player);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // Les 5 emplacements centraux (cases 2 à 6) correspondent aux 5 homes possibles
        for (int i = 0; i < 5; i++) {
            int homeIndex = i + 1;
            ItemStack item;
            ItemMeta meta;

            if (homeIndex > maxHomes) {
                item = new ItemStack(Material.BARRIER);
                meta = item.getItemMeta();
                meta.displayName(text("<red><b>Home #<n> (Verrouillé)</b>", Placeholder.unparsed("n", String.valueOf(homeIndex))));
                meta.lore(List.of(
                        text("<gray>Nécessite la permission: <yellow>fireballwand.home.max.<n>", Placeholder.unparsed("n", String.valueOf(homeIndex))),
                        text("<red>➤ Accès refusé")));
                meta.getPersistentDataContainer().set(manager.actionKey, PersistentDataType.STRING, "locked");

            } else if (i < homesList.size()) {
                Map.Entry<String, HomeData> entry = homesList.get(i);
                HomeData hd = entry.getValue();

                item = new ItemStack(Material.RED_BED);
                meta = item.getItemMeta();
                meta.displayName(text("<aqua><b>Home: <yellow><name></b>", Placeholder.unparsed("name", entry.getKey())));
                meta.lore(List.of(
                        text("<gray>Monde: <white><world>", Placeholder.unparsed("world", hd.world())),
                        text("<gray>X: <white><x> <gray>Y: <white><y> <gray>Z: <white><z>",
                                Placeholder.unparsed("x", String.format("%.1f", hd.x())),
                                Placeholder.unparsed("y", String.format("%.1f", hd.y())),
                                Placeholder.unparsed("z", String.format("%.1f", hd.z()))),
                        text("<gray>Créé le: <white><date>", Placeholder.unparsed("date", sdf.format(new Date(hd.setTime())))),
                        Component.empty(),
                        text("<green>➤ Clic gauche pour se téléporter"),
                        text("<red>➤ Clic droit pour supprimer")));
                meta.getPersistentDataContainer().set(manager.actionKey, PersistentDataType.STRING, "teleport");
                meta.getPersistentDataContainer().set(manager.homeNameKey, PersistentDataType.STRING, entry.getKey());

            } else {
                item = new ItemStack(Material.LIME_DYE);
                meta = item.getItemMeta();
                meta.displayName(text("<green><b>Emplacement libre #<n></b>", Placeholder.unparsed("n", String.valueOf(homeIndex))));
                meta.lore(List.of(
                        text("<gray>Utilise <yellow>/sethome <nom>"),
                        text("<gray>pour définir un home ici.")));
                meta.getPersistentDataContainer().set(manager.actionKey, PersistentDataType.STRING, "empty");
            }

            item.setItemMeta(meta);
            inventory.setItem(2 + i, item);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}
