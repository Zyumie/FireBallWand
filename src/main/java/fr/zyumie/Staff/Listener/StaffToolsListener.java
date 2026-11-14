package fr.zyumie.Staff.Listener;

import fr.zyumie.fireballwand.Main;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Outils de modération discrets :
 *  - conteneurs silencieux (variante A : lecture seule, via le mode spectateur) ;
 *  - invsee : clic droit sur un joueur pour voir son inventaire en direct.
 */
public class StaffToolsListener implements Listener {

    private static final Set<Material> SILENT_CONTAINERS = EnumSet.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.HOPPER,
            Material.DISPENSER, Material.DROPPER, Material.FURNACE, Material.BLAST_FURNACE,
            Material.SMOKER, Material.BREWING_STAND);

    private final Main plugin;
    private final NamespacedKey silentKey;

    /** Gamemode à rendre à la fermeture d'un conteneur ouvert en silence. */
    private final Map<UUID, GameMode> silentPrev = new HashMap<>();
    private final Set<UUID> invseeViewers = new HashSet<>();

    public StaffToolsListener(Main plugin) {
        this.plugin = plugin;
        this.silentKey = new NamespacedKey(plugin, "silent_prev_gamemode");
    }

    private FileConfiguration cfg() {
        return plugin.getStaffConfig();
    }

    // ── Conteneurs silencieux ────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || !isSilentContainer(block.getType())) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) return; // comportement vanilla (pose de bloc, etc.)
        if (silentPrev.containsKey(player.getUniqueId())) return;

        if (!cfg().getBoolean("staff.silent-containers.enabled", true)) return;
        if (cfg().getBoolean("staff.silent-containers.require-vanish", true)
                && !plugin.getVanishManager().isVanished(player)) return;

        // Coffre bloqué par un bloc plein au-dessus : le jeu refuse de l'ouvrir, on ne force pas
        if ((block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)
                && block.getRelative(BlockFace.UP).getType().isOccluding()) return;

        BlockState state = block.getState(false);
        if (!(state instanceof Container container)) return;
        Inventory inventory = container.getInventory();

        event.setCancelled(true);

        GameMode previous = player.getGameMode();
        if (previous != GameMode.SPECTATOR) {
            silentPrev.put(player.getUniqueId(), previous);
            // Garde-fou crash : si le serveur plante pendant l'ouverture, on restaure à la reconnexion
            player.getPersistentDataContainer().set(silentKey, PersistentDataType.STRING, previous.name());
            player.setGameMode(GameMode.SPECTATOR);
        }
        // Déjà spectateur (ex : double-sneak du vanish) : ouverture silencieuse sans rien changer

        player.openInventory(inventory);
    }

    private boolean isSilentContainer(Material type) {
        return SILENT_CONTAINERS.contains(type) || Tag.SHULKER_BOXES.isTagged(type);
    }

    private void restoreSilent(Player player) {
        GameMode previous = silentPrev.remove(player.getUniqueId());
        if (previous == null) return;
        player.setGameMode(previous);
        player.getPersistentDataContainer().remove(silentKey);
    }

    // ── Invsee ───────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInvsee(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player viewer = event.getPlayer();
        if (target.equals(viewer)) return;
        if (!cfg().getBoolean("staff.invsee.enabled", true)) return;
        if (!viewer.hasPermission("fireballwand.staff.invsee")) return;
        if (cfg().getBoolean("staff.invsee.require-staff-mode", true)
                && !plugin.getStaffManager().isInStaffMode(viewer)) return;
        if (cfg().getBoolean("staff.invsee.require-sneak", false) && !viewer.isSneaking()) return;

        event.setCancelled(true);

        invseeViewers.add(viewer.getUniqueId());
        viewer.openInventory(target.getInventory());
        viewer.sendMessage(plugin.getStaffManager()
                .msg("invsee-open", "&7[Staff] &aInventaire de &f%player%&a.")
                .replace("%player%", target.getName()));
    }

    // Lecture seule, sauf avec fireballwand.staff.invsee.edit

    @EventHandler(priority = EventPriority.HIGH)
    public void onInvseeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (invseeViewers.contains(player.getUniqueId()) && !player.hasPermission("fireballwand.staff.invsee.edit")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInvseeDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (invseeViewers.contains(player.getUniqueId()) && !player.hasPermission("fireballwand.staff.invsee.edit")) {
            event.setCancelled(true);
        }
    }

    // ── Fermeture / connexion / déconnexion ──────────────────────────────────

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        invseeViewers.remove(player.getUniqueId());
        restoreSilent(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        invseeViewers.remove(player.getUniqueId());
        restoreSilent(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String stored = player.getPersistentDataContainer().get(silentKey, PersistentDataType.STRING);
        if (stored == null) return;

        // Crash pendant l'ouverture silencieuse : le joueur est resté bloqué en spectateur
        try {
            GameMode previous = GameMode.valueOf(stored);
            if (player.getGameMode() == GameMode.SPECTATOR) player.setGameMode(previous);
        } catch (IllegalArgumentException ignored) {
            // valeur inconnue : on nettoie simplement
        }
        player.getPersistentDataContainer().remove(silentKey);
    }
}
