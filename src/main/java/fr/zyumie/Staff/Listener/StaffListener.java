package fr.zyumie.Staff.Listener;

import fr.zyumie.Staff.Fly.FlyManager;
import fr.zyumie.Staff.GodMod.GodManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
import org.bukkit.event.player.PlayerRespawnEvent;
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
    private FlyManager fly()     { return plugin.getFlyManager(); }

    private boolean blocked(Player player, String protectionKey) {
        return staff().isInStaffMode(player) && staff().protection(protectionKey);
    }

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
            case StaffManager.FLY     -> {
                Boolean state = fly().toggle(player);
                if (state == null) {
                    player.sendMessage(fly().msg("native", "&7Le vol est déjà actif dans ce mode de jeu."));
                } else {
                    player.sendMessage(state
                            ? fly().msg("enable", "&aVol activé.")
                            : fly().msg("disable", "&cVol désactivé."));
                }
            }
            default -> { }
        }
    }

    // ── Protections du mode staff ────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (blocked(event.getPlayer(), "block-break")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (blocked(event.getPlayer(), "block-place")) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && blocked(player, "item-pickup")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!blocked(damager, "pvp-damage")) return;

        // Le bâton de glace reste utilisable : freezewand gère lui-même le coup
        String held = staff().getItemId(damager.getInventory().getItemInMainHand());
        if (StaffManager.FREEZE.equals(held)) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (staff().isStaffItem(event.getItemDrop().getItemStack()) || blocked(player, "item-drop")) {
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

    // ── Mort / respawn ───────────────────────────────────────────────────────

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!staff().isInStaffMode(player)) return;

        // L'inventaire d'origine est en sécurité sur disque : rien ne doit tomber au sol
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!staff().isInStaffMode(player)) return;

        // Un tick après le respawn : on rend l'inventaire d'origine et on quitte le mode staff
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !staff().isInStaffMode(player)) return;
            staff().disable(player);
            player.sendMessage(staff().msg("died",
                    "&7[Staff] &cMode staff désactivé suite à ta mort. Ton inventaire a été restauré."));
        });
    }

    // ── Connexion / déconnexion ──────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (staff().isInStaffMode(player)) staff().disable(player);
        god().disable(player);
        fly().clear(player);
        lastUse.remove(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (staff().isInStaffMode(player)) return;

        // Sauvegarde trouvée au login = crash pendant un mode staff → on rend l'inventaire
        if (staff().recover(player)) {
            player.sendMessage(staff().msg("recovered", "&7[Staff] &aTon inventaire a été restauré."));
        } else {
            staff().removeStaffItems(player); // nettoyage de sécurité
        }
    }

    // ── Godmod & protection de chute (fly) ───────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        // Vol désactivé en l'air : on ignore la chute qui suit
        if (cause == EntityDamageEvent.DamageCause.FALL && fly().consumeFallGrace(player)) {
            event.setCancelled(true);
            return;
        }

        if (!god().isGod(player)) return;

        // On laisse passer le vide et /kill pour ne pas bloquer le joueur en chute infinie
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
