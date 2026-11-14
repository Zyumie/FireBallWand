package fr.zyumie.Staff.Fly;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlyManager {

    /** Durée pendant laquelle une chute est ignorée après avoir coupé le vol en l'air. */
    private static final long FALL_GRACE_MS = 15_000L;

    private final Main plugin;
    private final Map<UUID, Long> fallGrace = new ConcurrentHashMap<>();

    public FlyManager(Main plugin) {
        this.plugin = plugin;
    }

    public String msg(String key, String def) {
        return ChatManager.colorize(plugin.getStaffConfig().getString("fly.messages." + key, def));
    }

    /** Vitesse maximale (1 à 10) : 10 correspond à la limite du jeu (1.0). */
    public int getMaxSpeed() {
        return Math.max(1, Math.min(10, plugin.getStaffConfig().getInt("fly.max-speed", 10)));
    }

    /**
     * Active/désactive le vol.
     *
     * @return le nouvel état, ou null si le mode de jeu permet déjà de voler (créatif/spectateur)
     */
    public Boolean toggle(Player player) {
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return null;

        boolean enable = !player.getAllowFlight();
        player.setAllowFlight(enable);

        if (enable) {
            if (!player.isOnGround()) player.setFlying(true);
        } else {
            if (player.isFlying()) player.setFlying(false);
            fallGrace.put(player.getUniqueId(), System.currentTimeMillis() + FALL_GRACE_MS);
        }
        return enable;
    }

    /** Niveau 1 = vitesse vanilla (0.1), niveau 10 = maximum (1.0). */
    public void setSpeed(Player player, int level) {
        int clamped = Math.max(1, Math.min(getMaxSpeed(), level));
        player.setFlySpeed(Math.min(1.0f, clamped / 10f));
    }

    /** @return true (et consomme la protection) si la chute doit être ignorée. */
    public boolean consumeFallGrace(Player player) {
        Long until = fallGrace.remove(player.getUniqueId());
        return until != null && System.currentTimeMillis() <= until;
    }

    public void clear(Player player) {
        fallGrace.remove(player.getUniqueId());
    }
}
