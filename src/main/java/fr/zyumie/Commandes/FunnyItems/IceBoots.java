package fr.zyumie.Commandes.FunnyItems;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ice Boots : le porteur glisse sur n'importe quel bloc plein, comme sur de la glace.
 *
 * Aucune API ne permet de changer la glissance d'un joueur. Le plugin envoie donc au SEUL porteur
 * de faux blocs de glace sous ses pieds (packets uniquement, le monde n'est jamais modifié) :
 * son client applique alors la friction de la glace. Les autres joueurs ne voient rien.
 *
 * Performance : le PlayerMoveEvent ne fait un travail que quand le porteur change de bloc ;
 * les porteurs sont suivis via PlayerArmorChangeEvent (aucune lecture de métadonnées à chaque pas).
 */
public class IceBoots implements CommandExecutor, Listener {

    private static final String GIVE_PERMISSION = "fireballwand.iceboots";

    private final Main plugin;
    private final NamespacedKey key;

    /** Joueurs qui portent actuellement les bottes (thread principal uniquement). */
    private final Set<UUID> wearers = new HashSet<>();

    /** Blocs actuellement « glacés » côté client pour chaque porteur. */
    private final Map<UUID, Area> areas = new HashMap<>();

    private record Area(World world, Set<Long> keys) {}

    public IceBoots(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "ice_boots");
    }

    private FileConfiguration cfg() {
        return plugin.getCustomCheatConfig();
    }

    private String msg(String path, String def) {
        return ChatManager.colorize(cfg().getString("ice-boots.messages." + path, def));
    }

    // ── Commande ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }
        if (!player.hasPermission(GIVE_PERMISSION)) {
            player.sendMessage(msg("no-permission", "&c❌ Tu n'as pas la permission d'utiliser cette commande."));
            return true;
        }

        FunnyItemUtil.give(player, createItem());
        player.sendMessage(msg("given", "&aTu as reçu les &bIce Boots &a!"));
        return true;
    }

    private ItemStack createItem() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

        meta.displayName(FunnyItemUtil.text(cfg().getString("ice-boots.name", "&bIce Boots")));
        List<String> lore = cfg().getStringList("ice-boots.lore");
        if (!lore.isEmpty()) meta.lore(FunnyItemUtil.lore(lore));

        meta.setColor(parseColor(cfg().getString("ice-boots.color", "7FDFFF")));
        meta.setUnbreakable(cfg().getBoolean("ice-boots.unbreakable", true));
        meta.addItemFlags(ItemFlag.HIDE_DYE, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    private Color parseColor(String hex) {
        try {
            return Color.fromRGB(Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return Color.fromRGB(0x7FDFFF);
        }
    }

    private boolean isIceBoots(ItemStack item) {
        return item != null
                && item.getType() == Material.LEATHER_BOOTS
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    // ── Suivi des porteurs ───────────────────────────────────────────────────

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.FEET) return;

        Player player = event.getPlayer();
        if (isIceBoots(event.getNewItem())) {
            wearers.add(player.getUniqueId());
            refresh(player, player.getLocation());
        } else {
            wearers.remove(player.getUniqueId());
            clear(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sync(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) sync(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        wearers.remove(id);
        areas.remove(id);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Les faux blocs de l'ancien monde n'existent plus côté client ; le prochain pas en recrée
        areas.remove(event.getPlayer().getUniqueId());
    }

    private void sync(Player player) {
        if (isIceBoots(player.getInventory().getBoots())) {
            wearers.add(player.getUniqueId());
        } else {
            wearers.remove(player.getUniqueId());
            clear(player);
        }
    }

    // ── Glace sous les pieds ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        if (!wearers.contains(player.getUniqueId())) return;

        refresh(player, event.getTo());
    }

    private void refresh(Player player, Location at) {
        World world = at.getWorld();
        if (world == null) return;

        UUID id = player.getUniqueId();
        Area old = areas.get(id);
        if (old != null && !old.world().equals(world)) {
            areas.remove(id);
            old = null;
        }

        int radius = Math.max(0, Math.min(3, cfg().getInt("ice-boots.radius", 1)));
        int y = at.getBlockY() - 1;
        int cx = at.getBlockX();
        int cz = at.getBlockZ();

        Set<Long> next = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                // Ne jamais charger un chunk de façon synchrone (TPS)
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                // Blocs pleins uniquement : une dalle remplacée par un bloc plein ferait sauter le joueur
                if (!world.getBlockAt(x, y, z).getType().isOccluding()) continue;
                next.add(pack(x, y, z));
            }
        }

        if (old != null && old.keys().equals(next)) return;

        BlockData ice = iceData();
        for (long k : next) {
            if (old == null || !old.keys().contains(k)) {
                player.sendBlockChange(new Location(world, unpackX(k), unpackY(k), unpackZ(k)), ice);
            }
        }
        if (old != null) {
            for (long k : old.keys()) {
                if (!next.contains(k)) restore(player, world, k);
            }
        }
        areas.put(id, new Area(world, next));
    }

    private BlockData iceData() {
        Material material = Material.matchMaterial(cfg().getString("ice-boots.block", "PACKED_ICE"));
        if (material == null || !material.isBlock()) material = Material.PACKED_ICE;
        return material.createBlockData();
    }

    /** Renvoie au client le vrai bloc du serveur. */
    private void restore(Player player, World world, long key) {
        int x = unpackX(key);
        int y = unpackY(key);
        int z = unpackZ(key);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return;
        player.sendBlockChange(new Location(world, x, y, z), world.getBlockAt(x, y, z).getBlockData());
    }

    private void clear(Player player) {
        Area area = areas.remove(player.getUniqueId());
        if (area == null || !area.world().equals(player.getWorld())) return;
        for (long k : area.keys()) restore(player, area.world(), k);
    }

    /** À appeler à l'arrêt du plugin : rend leurs vrais blocs aux porteurs connectés. */
    public void shutdown() {
        for (UUID id : new HashSet<>(areas.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) clear(player);
        }
        areas.clear();
        wearers.clear();
    }

    // ── Position empaquetée dans un long (même format que BlockPos) ──────────

    private static long pack(int x, int y, int z) {
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    private static int unpackX(long v) {
        return (int) (v >> 38);
    }

    private static int unpackY(long v) {
        return (int) (v << 52 >> 52);
    }

    private static int unpackZ(long v) {
        return (int) (v << 26 >> 38);
    }
}
