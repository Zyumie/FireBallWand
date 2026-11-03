package fr.zyumie.Staff;

import fr.zyumie.Staff.GodMod.GodManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffListener implements Listener {

    private static final long USE_COOLDOWN_MS = 400L;

    private final Main plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public StaffListener(Main plugin) {
        this.plugin = plugin;
    }

    private StaffManager staff() { return plugin.getStaffManager(); }
    private GodManager god()     { return plugin.getGodManager(); }

    // ── Utilisation des items (clic droit) ───────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!staff().isInStaffMode(player)) return; // sortie rapide : cas de 99 % des clics

        String id = staff().getItemId(event.getItem());
        if (id == null) return;

        // Le bâton de glace se joue au coup : on laisse freezewand gérer
        if (id.equals(StaffManager.FREEZE)) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < USE_COOLDOWN_MS) return;
        lastUse.put(player.getUniqueId(), now);

        switch (id) {
            case StaffManager.VANISH  -> player.performCommand("vanish"); // permissions + messages gérés par /vanish
            case StaffManager.GOD     -> player.sendMessage(god().toggle(player)
                    ? staff().msg("god-enable", "&aGodmod activé.")
                    : staff().msg("god-disable", "&cGodmod désactivé."));
            case StaffManager.COMPASS -> staff().teleportNext(player);
            default -> { }
        }
    }

    // ── Protection des items staff ───────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (staff().isStaffItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (staff().isStaffItem(event.getMainHandItem()) || staff().isStaffItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!staff().isInStaffMode(player)) return;

        boolean involves = staff().isStaffItem(event.getCurrentItem())
                || staff().isStaffItem(event.getCursor());

        if (!involves && event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            involves = staff().isStaffItem(player.getInventory().getItem(event.getHotbarButton()));
        }
        if (!involves && event.getClick() == ClickType.SWAP_OFFHAND) {
            involves = staff().isStaffItem(player.getInventory().getItemInOffHand());
        }
        if (!involves) return;

        boolean outsidePlayerInv = event.getClickedInventory() != player.getInventory();
        boolean shiftIntoContainer = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getView().getTopInventory().getType() != InventoryType.CRAFTING;

        if (outsidePlayerInv || shiftIntoContainer) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!staff().isInStaffMode(player)) return;
        if (!staff().isStaffItem(event.getOldCursor())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ── Mort / connexion / déconnexion ───────────────────────────────────────

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!staff().isInStaffMode(player)) return;

        // Les items staff ne doivent jamais tomber au sol
        event.getDrops().removeIf(staff()::isStaffItem);

        // Sans keepInventory, l'inventaire est vidé : le mode staff prend fin.
        // Avec keepInventory, les items sont conservés et le mode continue.
        if (!event.getKeepInventory()) {
            staff().forget(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (staff().isInStaffMode(player)) staff().disable(player);
        god().disable(player);
        lastUse.remove(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Nettoyage de sécurité (crash serveur en plein mode staff, etc.)
        Player player = event.getPlayer();
        if (!staff().isInStaffMode(player)) {
            staff().removeStaffItems(player);
        }
    }

    // ── Godmod ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!god().isGod(player)) return;

        // On laisse passer le vide et /kill pour ne pas bloquer le joueur en chute infinie
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (god().isGod(player) && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }
}
