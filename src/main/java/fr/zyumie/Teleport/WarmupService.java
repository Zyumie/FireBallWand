package fr.zyumie.Teleport;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Délai d'attente (warmup) et cooldowns partagés par Home et Spawn.
 *
 * Un seul jeu d'écouteurs pour les deux modules, et surtout un seul test en tête de chaque
 * évènement fréquent : quand personne n'attend de téléportation (le cas normal), les handlers
 * de déplacement et de dégâts sortent immédiatement, sans même chercher le joueur dans une map.
 * Tout se passe sur le thread principal.
 */
public final class WarmupService implements Listener {

    private record Pending(BukkitTask task, boolean cancelOnMove, boolean cancelOnDamage, Consumer<Player> onCancel) {}

    private final Main plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public WarmupService(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ── Warmup ───────────────────────────────────────────────────────────────

    public boolean isPending(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    /**
     * Attend delaySeconds puis exécute onComplete (immédiatement si delaySeconds <= 0).
     * onCancel est appelé si le joueur bouge / subit des dégâts pendant l'attente.
     */
    public void start(Player player, int delaySeconds, boolean cancelOnMove, boolean cancelOnDamage,
                      Consumer<Player> onComplete, Consumer<Player> onCancel) {
        if (delaySeconds <= 0) {
            onComplete.accept(player);
            return;
        }

        UUID id = player.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            if (player.isOnline()) onComplete.accept(player);
        }, delaySeconds * 20L);

        pending.put(id, new Pending(task, cancelOnMove, cancelOnDamage, onCancel));
    }

    /** @return true si une attente était en cours et a été annulée */
    public boolean cancel(Player player, boolean notify) {
        Pending p = pending.remove(player.getUniqueId());
        if (p == null) return false;
        p.task().cancel();
        if (notify && p.onCancel() != null) p.onCancel().accept(player);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (pending.isEmpty() || !event.hasChangedBlock()) return;

        Pending p = pending.get(event.getPlayer().getUniqueId());
        if (p != null && p.cancelOnMove()) cancel(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (pending.isEmpty() || !(event.getEntity() instanceof Player player)) return;

        Pending p = pending.get(player.getUniqueId());
        if (p != null && p.cancelOnDamage() && event.getFinalDamage() > 0) cancel(player, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer(), false);
    }

    // ── Cooldowns ────────────────────────────────────────────────────────────

    /** @return secondes restantes (arrondies au supérieur), 0 s'il n'y a pas de cooldown */
    public long cooldownLeft(UUID id, String feature) {
        String key = feature + ":" + id;
        Long until = cooldowns.get(key);
        if (until == null) return 0L;

        long left = until - System.currentTimeMillis();
        if (left <= 0L) {
            cooldowns.remove(key);
            return 0L;
        }
        return (left + 999L) / 1000L;
    }

    public void setCooldown(UUID id, String feature, int seconds) {
        if (seconds <= 0) return;
        if (cooldowns.size() > 256) purgeExpired();
        cooldowns.put(feature + ":" + id, System.currentTimeMillis() + seconds * 1000L);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Long> it = cooldowns.values().iterator();
        while (it.hasNext()) {
            if (it.next() <= now) it.remove();
        }
    }

    /** À l'arrêt du plugin. */
    public void shutdown() {
        for (Pending p : pending.values()) p.task().cancel();
        pending.clear();
        cooldowns.clear();
    }
}
