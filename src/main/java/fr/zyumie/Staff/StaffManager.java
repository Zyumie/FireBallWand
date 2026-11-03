package fr.zyumie.Staff;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mode staff : donne des items de modération (vanish, freeze, godmod, boussole)
 * dans les cases LIBRES de l'inventaire — rien n'est remplacé, donc rien ne peut
 * être perdu. Refaire /staff retire simplement tous les items marqués.
 */
public class StaffManager {

    public static final String VANISH  = "VANISH";
    public static final String FREEZE  = "FREEZE";
    public static final String GOD     = "GOD";
    public static final String COMPASS = "COMPASS";

    private final Main plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey freezeKey; // même clé que freezewand → son listener reconnaît l'item

    private final Set<UUID> staffMode = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> compassIndex = new HashMap<>();

    private record Pending(ItemStack item, int slot) {}

    public StaffManager(Main plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "staff_item");
        this.freezeKey = new NamespacedKey(plugin, "freeze_wand");
    }

    // ── Messages ─────────────────────────────────────────────────────────────

    public String msg(String key, String def) {
        return ChatManager.colorize(plugin.getStaffConfig().getString("staff.messages." + key, def));
    }

    // ── État ─────────────────────────────────────────────────────────────────

    public boolean isInStaffMode(Player player) {
        return staffMode.contains(player.getUniqueId());
    }

    /** Oublie l'état sans toucher à l'inventaire (ex : mort sans keepInventory). */
    public void forget(Player player) {
        staffMode.remove(player.getUniqueId());
        compassIndex.remove(player.getUniqueId());
    }

    // ── Toggle ───────────────────────────────────────────────────────────────

    public void toggle(Player player) {
        if (isInStaffMode(player)) {
            disable(player);
            player.sendMessage(msg("disable", "&7[Staff] &cMode staff désactivé."));
        } else if (enable(player)) {
            player.sendMessage(msg("enable", "&7[Staff] &aMode staff activé."));
        }
    }

    public boolean enable(Player player) {
        List<Pending> pending = new ArrayList<>();

        if (plugin.getVanishManager().canVanish(player)) {
            pending.add(new Pending(build(VANISH, Material.ENDER_EYE, "&5Vanish",
                    "&7Clic droit : activer/désactiver le vanish"), slot(VANISH, 0)));
        }
        if (player.hasPermission("fireballwand.freezewand")) {
            pending.add(new Pending(buildFreeze(), slot(FREEZE, 1)));
        }
        if (player.hasPermission("fireballwand.god")) {
            pending.add(new Pending(build(GOD, Material.TOTEM_OF_UNDYING, "&6Godmod",
                    "&7Clic droit : activer/désactiver le godmod"), slot(GOD, 2)));
        }
        if (player.hasPermission("fireballwand.staff.teleport")) {
            pending.add(new Pending(build(COMPASS, Material.COMPASS, "&bBoussole de téléportation",
                    "&7Clic droit : te téléporter au joueur suivant"), slot(COMPASS, 3)));
        }

        PlayerInventory inv = player.getInventory();
        int free = 0;
        for (int i = 0; i < 36; i++) if (inv.getItem(i) == null) free++;

        if (free < pending.size()) {
            player.sendMessage(msg("no-space", "&7[Staff] &cLibère au moins &f%count% &ccase(s) dans ton inventaire.")
                    .replace("%count%", String.valueOf(pending.size())));
            return false;
        }

        for (Pending p : pending) {
            int target = (p.slot() >= 0 && p.slot() < 9 && inv.getItem(p.slot()) == null)
                    ? p.slot() : inv.firstEmpty();
            inv.setItem(target, p.item());
        }

        staffMode.add(player.getUniqueId());
        return true;
    }

    public void disable(Player player) {
        removeStaffItems(player);
        forget(player);
    }

    public void disableAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInStaffMode(p)) disable(p);
        }
        staffMode.clear();
    }

    // ── Items ────────────────────────────────────────────────────────────────

    public boolean isStaffItem(ItemStack item) {
        return getItemId(item) != null;
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    public void removeStaffItems(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isStaffItem(inv.getItem(i))) inv.setItem(i, null);
        }
        if (isStaffItem(player.getItemOnCursor())) player.setItemOnCursor(null);
    }

    private int slot(String id, int def) {
        return plugin.getStaffConfig().getInt("staff.items." + id.toLowerCase() + ".slot", def);
    }

    private ItemStack build(String id, Material defMat, String defName, String defLore) {
        String path = "staff.items." + id.toLowerCase();

        Material mat = Material.matchMaterial(plugin.getStaffConfig().getString(path + ".material", defMat.name()));
        if (mat == null || mat.isAir()) mat = defMat;

        String name = plugin.getStaffConfig().getString(path + ".name", defName);
        return tag(new ItemStack(mat), id, name, defLore);
    }

    private ItemStack buildFreeze() {
        // STONE_SWORD + clé "freeze_wand" : requis par freezewand.isFreezeWand()
        String name = plugin.getStaffConfig().getString("staff.items.freeze.name", "&bBâton de Glace");
        ItemStack item = tag(new ItemStack(Material.STONE_SWORD), FREEZE, name,
                "&7Frappe un joueur pour le geler/dégeler");

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(freezeKey, PersistentDataType.STRING, "FREEZE_WAND");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tag(ItemStack item, String id, String name, String lore) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy(name));
        meta.lore(List.of(legacy(lore)));
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private Component legacy(String text) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(ChatManager.colorize(text))
                .decoration(TextDecoration.ITALIC, false);
    }

    // ── Boussole ─────────────────────────────────────────────────────────────

    /** Téléporte le staff au joueur suivant (ordre alphabétique, boucle). */
    public void teleportNext(Player staff) {
        List<Player> targets = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(staff) && staff.canSee(p))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .map(p -> (Player) p)
                .toList();

        if (targets.isEmpty()) {
            staff.sendMessage(msg("tp-none", "&7[Staff] &cAucun joueur à qui te téléporter."));
            return;
        }

        int next = compassIndex.getOrDefault(staff.getUniqueId(), -1) + 1;
        if (next >= targets.size()) next = 0;
        compassIndex.put(staff.getUniqueId(), next);

        Player target = targets.get(next);
        staff.teleport(target.getLocation());
        staff.sendMessage(msg("tp", "&7[Staff] &aTéléporté à &f%player%&a.")
                .replace("%player%", target.getName()));
    }
}
