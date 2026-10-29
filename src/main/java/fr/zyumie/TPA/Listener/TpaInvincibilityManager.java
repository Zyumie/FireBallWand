package fr.zyumie.TPA.Listener;

import fr.zyumie.TPA.TpaMessageUtil;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaInvincibilityManager {

    private final Main plugin;
    private final Map<UUID, Long> invinciblePlayers = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;

    public TpaInvincibilityManager(Main plugin) { this.plugin = plugin; }

    private void startTickerIfNeeded() {
        if (tickerTask == null || tickerTask.isCancelled()) {
            tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (invinciblePlayers.isEmpty()) {
                    tickerTask.cancel();
                    tickerTask = null;
                    return;
                }
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, Long>> it = invinciblePlayers.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> entry = it.next();
                    UUID uuid = entry.getKey();
                    long expiry = entry.getValue();
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        it.remove();
                        continue;
                    }
                    long remainingMillis = expiry - now;
                    if (remainingMillis <= 0) {
                        it.remove();
                        player.sendActionBar(Component.empty());
                        TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("invincibility-end", "&cInvulnérabilité expirée."));
                    } else if (plugin.getTpaConfigManager().isShowInvincibilityActionBar()) {
                        long secs = (remainingMillis + 999L) / 1000L;
                        String msg = plugin.getTpaConfigManager().getMessage("invincibility-actionbar", "&b✦ Protection : &e{seconds}s").replace("{seconds}", String.valueOf(secs));
                        player.sendActionBar(Component.text(TpaMessageUtil.colorize(msg)));
                    }
                }
            }, 10L, 10L);
        }
    }

    public void setInvincible(Player player, int seconds) {
        if (player == null || !player.isOnline() || seconds <= 0) return;
        invinciblePlayers.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
        startTickerIfNeeded();
        TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("invincibility-start", "&aInvulnérable !").replace("{seconds}", String.valueOf(seconds)));
    }

    public boolean isInvincible(UUID uuid) {
        Long expiry = invinciblePlayers.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            invinciblePlayers.remove(uuid);
            return false;
        }
        return true;
    }

    public void removeInvincibility(Player player, boolean pvp) {
        if (player != null && invinciblePlayers.remove(player.getUniqueId()) != null) {
            player.sendActionBar(Component.empty());
            if (pvp) {
                TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("invincibility-pvp-lost", "&cProtection perdue (PvP) !"));
            }
        }
    }

    public void clearAll() {
        if (tickerTask != null) tickerTask.cancel();
        invinciblePlayers.clear();
    }
}