package fr.zyumie.Vanish.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Set;
import java.util.UUID;

public class VanishListener implements Listener {

    // Gère le format hex produit par ChatManager.colorize (§x§r§r§g§g§b§b)
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final Main plugin;

    public VanishListener(Main plugin) {
        this.plugin = plugin;
        scheduleActionBar();
    }

    /** Action bar « vous êtes en vanish » — Vanish.yml → actionbar.* (clés à la racine). */
    private void scheduleActionBar() {
        FileConfiguration cfg = plugin.getVanishConfig();
        if (!cfg.getBoolean("actionbar.enabled", true)) return;

        long interval = Math.max(1L, cfg.getLong("actionbar.interval", 40L));

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Set<UUID> vanished = plugin.getVanishManager().getVanishedUUIDs();
            if (vanished.isEmpty()) return;

            // Relu à chaque passage : le message se met à jour avec /fw reload
            String raw = plugin.getVanishConfig().getString("actionbar.message",
                    "&8[ &7Vous êtes actuellement en vanish &8]");
            Component message = LEGACY.deserialize(ChatManager.colorize(raw));

            for (UUID uuid : vanished) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendActionBar(message);
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
            event.joinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player leaving = event.getPlayer();
        VanishManager vm = plugin.getVanishManager();

        if (vm.isVanished(leaving)) {
            event.quitMessage(null);
            vm.handleQuit(leaving);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.getVanishManager().isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        plugin.getVanishManager().handleSneak(event.getPlayer());
    }
}
