package fr.zyumie.Vanish.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.UUID;

public class VanishListener implements Listener {

    private final Main plugin;

    public VanishListener(Main plugin) {
        this.plugin = plugin;
        scheduleActionBar();
    }

    private void scheduleActionBar() {
        if (!plugin.getConfig().getBoolean("vanish.actionbar.enabled", true)) return;

        long interval = plugin.getConfig().getLong("vanish.actionbar.interval", 40L);
        String rawMsg = plugin.getConfig().getString("vanish.actionbar.message",
                "&8[ &7Vous êtes actuellement en vanish &8]");

        String msg = ChatManager.colorize(rawMsg);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : plugin.getVanishManager().getVanishedUUIDs()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.spigot().sendMessage(
                            ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(msg)
                    );
                }
            }
        }, interval, interval);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        VanishManager vm = plugin.getVanishManager();

        vm.handleJoin(joining);

        if (vm.isVanished(joining)) {
            event.setJoinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player leaving = event.getPlayer();
        VanishManager vm = plugin.getVanishManager();

        if (vm.isVanished(leaving)) {
            event.setQuitMessage(null);
            vm.handleQuit(leaving);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.getVanishManager().isVanished(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        plugin.getVanishManager().handleSneak(event.getPlayer());
    }
}