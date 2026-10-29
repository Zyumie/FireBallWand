package fr.zyumie.TPA.Listener;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TpaListener implements Listener {
    private final Main plugin;
    public TpaListener(Main plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getTpaConfigManager().isCancelOnMove()) return;
        Player player = event.getPlayer();
        if (!plugin.getTpaTeleportManager().isWarmingUp(player.getUniqueId())) return;

        Location from = event.getFrom(), to = event.getTo();
        if (to == null) return;

        if (Math.abs(from.getX() - to.getX()) > 0.08 || Math.abs(from.getY() - to.getY()) > 0.08 || Math.abs(from.getZ() - to.getZ()) > 0.08) {
            plugin.getTpaTeleportManager().cancelWarmup(player.getUniqueId(), plugin.getTpaConfigManager().getMessage("warmup-cancelled-move", "&cAnnulé : vous avez bougé !"), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getTpaInvincibilityManager().isInvincible(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getTpaConfigManager().isCancelOnDamage() && !event.isCancelled() && plugin.getTpaTeleportManager().isWarmingUp(player.getUniqueId())) {
            plugin.getTpaTeleportManager().cancelWarmup(player.getUniqueId(), plugin.getTpaConfigManager().getMessage("warmup-cancelled-damage", "&cAnnulé : dégâts subis !"), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getTpaConfigManager().isCancelOnPvp()) return;
        Player attacker = event.getDamager() instanceof Player p ? p : (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p ? p : null);
        if (attacker != null && plugin.getTpaInvincibilityManager().isInvincible(attacker.getUniqueId())) {
            plugin.getTpaInvincibilityManager().removeInvincibility(attacker, true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getTpaTeleportManager().cancelWarmup(player.getUniqueId(), null, false);
        plugin.getTpaInvincibilityManager().removeInvincibility(player, false);
        plugin.getTpaManager().cleanupPlayer(player.getUniqueId());
    }
}