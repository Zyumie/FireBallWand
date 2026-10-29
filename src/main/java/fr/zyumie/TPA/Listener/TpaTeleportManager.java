package fr.zyumie.TPA.Listener;

import fr.zyumie.TPA.TpaMessageUtil;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaTeleportManager {

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeWarmups = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> warmupTargets = new ConcurrentHashMap<>();

    public TpaTeleportManager(Main plugin) { this.plugin = plugin; }

    public boolean isWarmingUp(UUID uuid) { return activeWarmups.containsKey(uuid); }

    public void startWarmup(Player whoMoves, Player destination) {
        UUID moverId = whoMoves.getUniqueId();
        UUID destId = destination.getUniqueId();
        cancelWarmup(moverId, null, false);

        int warmupSeconds = plugin.getTpaConfigManager().getWarmupSeconds();
        if (warmupSeconds <= 0 || whoMoves.hasPermission("fireballwand.tpa.bypass.warmup")) {
            executeTeleport(whoMoves, destination);
            return;
        }

        warmupTargets.put(moverId, destId);
        String startMsg = plugin.getTpaConfigManager().getMessage("warmup-start", "&eTéléportation dans &6{seconds}s&e...")
                .replace("{seconds}", String.valueOf(warmupSeconds));
        TpaMessageUtil.sendMessage(whoMoves, plugin.getTpaConfigManager().getPrefix(), startMsg);

        if (plugin.getTpaConfigManager().isPlaySounds()) {
            TpaMessageUtil.playSound(whoMoves, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int remaining = warmupSeconds;

            @Override
            public void run() {
                if (!whoMoves.isOnline()) {
                    cancelWarmup(moverId, null, false);
                    return;
                }
                Player currentDest = Bukkit.getPlayer(destId);
                if (currentDest == null || !currentDest.isOnline()) {
                    cancelWarmup(moverId, plugin.getTpaConfigManager().getMessage("warmup-cancelled-target-moved", "&cCible déconnectée."), true);
                    return;
                }
                if (remaining <= 0) {
                    cancelWarmup(moverId, null, false);
                    executeTeleport(whoMoves, currentDest);
                    return;
                }
                whoMoves.sendActionBar(Component.text(TpaMessageUtil.colorize("&6Téléportation dans &e" + remaining + "s&6...")));
                if (plugin.getTpaConfigManager().isSpawnParticles()) {
                    whoMoves.getWorld().spawnParticle(Particle.PORTAL, whoMoves.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                }
                remaining--;
            }
        }, 0L, 20L);

        activeWarmups.put(moverId, task);
    }

    private void executeTeleport(Player whoMoves, Player destination) {
        Location targetLoc = destination.getLocation();
        whoMoves.teleportAsync(targetLoc).thenAccept(success -> {
            if (success) {
                plugin.getTpaManager().recordTeleport(whoMoves.getUniqueId());
                plugin.getTpaInvincibilityManager().setInvincible(whoMoves, plugin.getTpaConfigManager().getInvincibilitySeconds());

                if (plugin.getTpaConfigManager().isPlaySounds()) {
                    TpaMessageUtil.playSound(whoMoves, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    TpaMessageUtil.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
                TpaMessageUtil.sendMessage(whoMoves, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("teleport-success", "&aTéléportation réussie !"));
            }
        });
    }

    public void cancelWarmup(UUID moverId, String reason, boolean playSound) {
        BukkitTask task = activeWarmups.remove(moverId);
        warmupTargets.remove(moverId);
        if (task != null) task.cancel();

        Player sender = Bukkit.getPlayer(moverId);
        if (sender != null && sender.isOnline()) {
            sender.sendActionBar(Component.empty());
            if (reason != null) TpaMessageUtil.sendMessage(sender, plugin.getTpaConfigManager().getPrefix(), reason);
            if (playSound && plugin.getTpaConfigManager().isPlaySounds()) {
                TpaMessageUtil.playSound(sender, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    public void cancelAll() {
        for (BukkitTask task : activeWarmups.values()) {
            if (task != null) task.cancel();
        }
        activeWarmups.clear();
        warmupTargets.clear();
    }
}