package fr.zyumie.Staff.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mode staff : l'inventaire d'origine est sauvegardé sur disque (StaffInventoryStore),
 * remplacé par le kit de modération, puis restitué à la sortie du mode.
 */
public class StaffManager {

    public static final String VANISH  = "VANISH";
    public static final String FREEZE  = "FREEZE";
    public static final String GOD     = "GOD";
    public static final String COMPASS = "COMPASS";
    public static final String FLY     = "FLY";

    // Gère le format hex produit par ChatManager.colorize (§x§r§r§g§g§b§b)
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final Main plugin;
    private final StaffInventoryStore store;
    private final NamespacedKey itemKey;
    private final NamespacedKey freezeKey; // même clé que freezewand → son listener reconnaît l'item

    private final Set<UUID> staffMode = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> compassIndex = new HashMap<>();

    private record Pending(ItemStack item, int slot) {}

    public StaffManager(Main plugin) {
        this.plugin = plugin;
        this.store = new StaffInventoryStore(plugin);
        this.itemKey = new NamespacedKey(plugin, "staff_item");
        this.freezeKey = new NamespacedKey(plugin, "freeze_wand");
    }

    private FileConfiguration cfg() {
        return plugin.getStaffConfig();
    }

    // ── Messages & options ───────────────────────────────────────────────────

    public String msg(String key, String def) {
        return ChatManager.colorize(cfg().getString("staff.messages." + key, def));
    }

    /** Protections du mode staff (staff.protections.<key>), actives par défaut. */
    public boolean protection(String key) {
        return cfg().getBoolean("staff.protections." + key, true);
    }

    // ── État ─────────────────────────────────────────────────────────────────

    public boolean isInStaffMode(Player player) {
        return staffMode.contains(player.getUniqueId());
    }

    public void forget(Player player) {
        staffMode.remove(player.getUniqueId());
        compassIndex.remove(player.getUniqueId());
    }

    // ── Toggle ───────────────────────────────────────────────────────────────

    public void toggle(Player player) {
        if (isInStaffMode(player)) {
            disable(player);
            player.sendMessage(msg("disable", "&7[Staff] &cMode staff désactivé. Ton inventaire a été restauré."));
        } else if (enable(player)) {
            player.sendMessage(msg("enable", "&7[Staff] &aMode staff activé."));
        }
    }

    public boolean enable(Player player) {
        if (isInStaffMode(player)) return false;
        UUID id = player.getUniqueId();

        // Sauvegarde oubliée (crash) : on la restaure d'abord pour ne jamais l'écraser
        if (store.hasSnapshot(id) && !store.restore(player)) {
            player.sendMessage(msg("save-failed", "&7[Staff] &cImpossible de sauvegarder ton inventaire, mode staff annulé."));
            return false;
        }

        List<Pending> kit = buildKit(player);
        boolean saveXp = cfg().getBoolean("staff.inventory.save-xp", false);

        // Écriture sur disque AVANT de vider l'inventaire
        if (!store.save(player, saveXp)) {
            player.sendMessage(msg("save-failed", "&7[Staff] &cImpossible de sauvegarder ton inventaire, mode staff annulé."));
            return false;
        }

        player.closeInventory();
        player.setItemOnCursor(null);

        PlayerInventory inv = player.getInventory();
        inv.clear();

        for (Pending p : kit) {
            int slot = (p.slot() >= 0 && p.slot() < 36 && inv.getItem(p.slot()) == null)
                    ? p.slot() : inv.firstEmpty();
            if (slot >= 0) inv.setItem(slot, p.item());
        }

        if (saveXp) {
            player.setLevel(0);
            player.setExp(0f);
        }

        staffMode.add(id);
        return true;
    }

    /** Sortie du mode : restaure l'inventaire d'origine. */
    public void disable(Player player) {
        if (!store.restore(player)) {
            removeStaffItems(player); // pas de sauvegarde lisible : on retire au moins le kit
        }
        forget(player);
    }

    /** Restauration après crash (appelée à la connexion). */
    public boolean recover(Player player) {
        if (isInStaffMode(player) || !store.hasSnapshot(player.getUniqueId())) return false;
        return store.restore(player);
    }

    public void disableAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInStaffMode(p)) disable(p);
        }
        staffMode.clear();
    }

    // ── Kit ──────────────────────────────────────────────────────────────────

    private List<Pending> buildKit(Player player) {
        List<Pending> kit = new ArrayList<>();

        if (plugin.getVanishManager().canVanish(player)) {
            kit.add(new Pending(build(VANISH, Material.ENDER_EYE, "&5Vanish",
                    "&7Clic droit : activer/désactiver le vanish"), slot(VANISH, 0)));
        }
        if (player.hasPermission("fireballwand.freezewand")) {
            kit.add(new Pending(buildFreeze(), slot(FREEZE, 1)));
        }
        if (player.hasPermission("fireballwand.god")) {
            kit.add(new Pending(build(GOD, Material.TOTEM_OF_UNDYING, "&6Godmod",
                    "&7Clic droit : activer/désactiver le godmod"), slot(GOD, 2)));
        }
        if (player.hasPermission("fireballwand.staff.teleport")) {
            kit.add(new Pending(build(COMPASS, Material.COMPASS, "&bBoussole de téléportation",
                    "&7Clic droit : te téléporter au joueur suivant"), slot(COMPASS, 3)));
        }
        if (cfg().getBoolean("fly.enabled", true) && player.hasPermission("fireballwand.fly")) {
            kit.add(new Pending(build(FLY, Material.FEATHER, "&fFly",
                    "&7Clic droit : activer/désactiver le vol"), slot(FLY, 4)));
        }
        return kit;
    }

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
        return cfg().getInt("staff.items." + id.toLowerCase() + ".slot", def);
    }

    private ItemStack build(String id, Material defMat, String defName, String defLore) {
        String path = "staff.items." + id.toLowerCase();

        Material mat = Material.matchMaterial(cfg().getString(path + ".material", defMat.name()));
        if (mat == null || mat.isAir()) mat = defMat;

        return tag(new ItemStack(mat), id, cfg().getString(path + ".name", defName), defLore);
    }

    private ItemStack buildFreeze() {
        // STONE_SWORD + clé "freeze_wand" : requis par freezewand.isFreezeWand()
        // Nom : Staff.yml si renseigné, sinon celui de CustomCheat.yml (freezewand.name)
        String name = cfg().getString("staff.items.freeze.name",
                plugin.getCustomCheatConfig().getString("freezewand.name", "&bBâton de Glace"));

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
        return LEGACY.deserialize(ChatManager.colorize(text)).decoration(TextDecoration.ITALIC, false);
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
