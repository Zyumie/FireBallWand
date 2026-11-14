package fr.zyumie.Home;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class HomeListener implements Listener {

    private final Main plugin;
    private final HomeManager manager;
    private final HomeCommands commands;

    public HomeListener(Main plugin, HomeManager manager, HomeCommands commands) {
        this.plugin = plugin;
        this.manager = manager;
        this.commands = commands;
    }

    /** Chargement des homes AVANT l'arrivée du joueur, hors du thread principal (aucune E/S au join). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        manager.cache(event.getUniqueId(), manager.readOrdered(event.getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.unload(event.getPlayer().getUniqueId());
    }

    // ── Menu /home ───────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof HomeGui)) return;

        event.setCancelled(true); // le menu est en lecture seule (anti-vol, anti-dupe)

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != top) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || !current.hasItemMeta()) return;

        PersistentDataContainer data = current.getItemMeta().getPersistentDataContainer();
        String action = data.get(manager.actionKey, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "locked" -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                commands.sendNoPermission(player);
            }
            case "empty" -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                commands.sendEmptySlotHint(player);
            }
            case "teleport" -> {
                String homeName = data.get(manager.homeNameKey, PersistentDataType.STRING);
                if (homeName == null) return;

                if (event.isRightClick()) {
                    manager.deleteHome(player.getUniqueId(), homeName);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    commands.sendDeleted(player, homeName);
                    new HomeGui(plugin, manager, player).open();
                } else if (event.isLeftClick()) {
                    player.closeInventory();
                    commands.initiateTeleport(player, homeName);
                }
            }
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof HomeGui) {
            event.setCancelled(true);
        }
    }
}
