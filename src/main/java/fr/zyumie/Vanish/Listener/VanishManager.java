package fr.zyumie.Vanish.Listener;

import fr.zyumie.Chat.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import fr.zyumie.fireballwand.Main;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gère l'état du vanish, la visibilité hiérarchique et le mode spectateur.
 *
 * Niveaux (fireballwand.vanish.level.N) :
 *   Un observateur de niveau N voit les vanishés de niveau ≤ N.
 *   Helper=1 | Modo=2 | Admin=3 | Proprio=4
 */
public class VanishManager {

    private final Main plugin;

    /** UUID → niveau de vanish */
    private final Map<UUID, Integer> vanishedPlayers = new HashMap<>();

    /** UUID → gamemode avant passage en spectateur */
    private final Map<UUID, GameMode> previousGameMode = new HashMap<>();

    /** UUID → timestamp du dernier sneak (pour double-sneak) */
    private final Map<UUID, Long> lastSneak = new HashMap<>();

    private final int maxLevel;

    public VanishManager(Main plugin) {
        this.plugin = plugin;
        this.maxLevel = plugin.getVanishConfig().getInt("vanish.max-level", 10);
    }

    // ── Niveau ──────────────────────────────────────────────────────────────

    public int getVanishLevel(Player player) {
        for (int lvl = maxLevel; lvl >= 1; lvl--) {
            if (player.hasPermission("fireballwand.vanish.level." + lvl)) return lvl;
        }
        return 0;
    }

    public boolean canVanish(Player player) {
        return getVanishLevel(player) >= 1 || player.hasPermission("fireballwand.vanish");
    }

    // ── Toggle vanish ────────────────────────────────────────────────────────

    public boolean isVanished(Player player) {
        return vanishedPlayers.containsKey(player.getUniqueId());
    }

    public void enableVanish(Player player) {
        int level = Math.max(getVanishLevel(player), 1);
        vanishedPlayers.put(player.getUniqueId(), level);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (!canSeeVanished(online, level)) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    public void disableVanish(Player player) {
        vanishedPlayers.remove(player.getUniqueId());

        // Si en spectateur via vanish → remettre le gamemode précédent
        if (isInVanishSpectator(player)) {
            restoreGameMode(player);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    // ── Rejoin ──────────────────────────────────────────────────────────────

    public void applyVanishOnJoin(Player joining) {
        for (Map.Entry<UUID, Integer> entry : vanishedPlayers.entrySet()) {
            Player vanished = Bukkit.getPlayer(entry.getKey());
            if (vanished == null || vanished.equals(joining)) continue;
            if (!canSeeVanished(joining, entry.getValue())) {
                joining.hidePlayer(plugin, vanished);
            }
        }
    }

    // ── Visibilité hiérarchique ──────────────────────────────────────────────

    public boolean canSeeVanished(Player observer, int vanishedLevel) {
        return getVanishLevel(observer) >= vanishedLevel;
    }

    // ── Spectateur (double-sneak) ────────────────────────────────────────────

    public boolean isInVanishSpectator(Player player) {
        return previousGameMode.containsKey(player.getUniqueId());
    }

    /**
     * Appelé à chaque sneak du joueur vanishé.
     * Retourne true si un double-sneak vient d'être détecté et traité.
     */
    public boolean handleSneak(Player player) {
        if (!plugin.getConfig().getBoolean("vanish.spectator-on-sneak.enabled", true)) return false;
        if (!isVanished(player)) return false;

        long delay = plugin.getConfig().getLong("vanish.spectator-on-sneak.double-sneak-delay", 400L);
        long now = System.currentTimeMillis();
        Long last = lastSneak.get(player.getUniqueId());

        lastSneak.put(player.getUniqueId(), now);

        if (last != null && (now - last) <= delay) {
            lastSneak.remove(player.getUniqueId());
            toggleSpectator(player);
            return true;
        }
        return false;
    }

    private void toggleSpectator(Player player) {
        String msgOn  = plugin.getConfig().getString("vanish.messages.spectator-on",  "&7[Vanish] &aMode spectateur activé.");
        String msgOff = plugin.getConfig().getString("vanish.messages.spectator-off", "&7[Vanish] &cMode spectateur désactivé.");

        if (!isInVanishSpectator(player)) {
            // Sauvegarder le gamemode actuel et passer en spectateur
            previousGameMode.put(player.getUniqueId(), player.getGameMode());
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatManager.colorize(msgOn));
        } else {
            restoreGameMode(player);
            player.sendMessage(ChatManager.colorize(msgOff));
        }
    }

    private void restoreGameMode(Player player) {
        GameMode gm = previousGameMode.remove(player.getUniqueId());
        if (gm != null) player.setGameMode(gm);
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────

    public int getVisiblePlayerCount() {
        return Math.max(0, Bukkit.getOnlinePlayers().size() - vanishedPlayers.size());
    }

    public Set<UUID> getVanishedUUIDs() {
        return Collections.unmodifiableSet(vanishedPlayers.keySet());
    }

    public void clear() {
        vanishedPlayers.clear();
        previousGameMode.clear();
        lastSneak.clear();
    }
}
