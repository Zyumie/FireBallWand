package fr.zyumie.Vanish.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.SoftDepend.TabHook;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final Main plugin;

    // Structures thread-safe : lues aussi par le thread async du ping serveur.

    /** UUID → niveau de vanish actif */
    private final Map<UUID, Integer> vanishedPlayers = new ConcurrentHashMap<>();

    /** UUIDs des joueurs à remettre en vanish à la reconnexion */
    private final Set<UUID> persistedVanishedPlayers = ConcurrentHashMap.newKeySet();

    /** UUID → gamemode avant passage en spectateur */
    private final Map<UUID, GameMode> previousGameMode = new ConcurrentHashMap<>();

    /** UUID → timestamp du dernier sneak (double-sneak) */
    private final Map<UUID, Long> lastSneak = new ConcurrentHashMap<>();

    private final int maxLevel;

    public VanishManager(Main plugin) {
        this.plugin = plugin;
        this.maxLevel = plugin.getVanishConfig().getInt("vanish.max-level", 10);
    }

    // ── Niveau ───────────────────────────────────────────────────────────────

    public int getVanishLevel(Player player) {
        for (int lvl = maxLevel; lvl >= 1; lvl--) {
            if (player.hasPermission("fireballwand.vanish.level." + lvl)) return lvl;
        }
        return 0;
    }

    public boolean canVanish(Player player) {
        return getVanishLevel(player) >= 1 || player.hasPermission("fireballwand.vanish");
    }

    public boolean canSeeVanished(Player observer, int vanishedLevel) {
        return getVanishLevel(observer) >= vanishedLevel;
    }

    // ── État ─────────────────────────────────────────────────────────────────

    public boolean isVanished(Player player) {
        return vanishedPlayers.containsKey(player.getUniqueId());
    }

    /** Version UUID : utilisable depuis un thread async (ping serveur). */
    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.containsKey(uuid);
    }

    // ── Toggle ───────────────────────────────────────────────────────────────

    public void enableVanish(Player player) {
        int level = Math.max(getVanishLevel(player), 1);
        vanishedPlayers.put(player.getUniqueId(), level);
        persistedVanishedPlayers.add(player.getUniqueId());

        // Flag lu par TAB (et d'autres plugins) → caché de la tablist / compteurs
        TabHook.setVanish(player, true);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (!canSeeVanished(online, level)) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    public void disableVanish(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
        persistedVanishedPlayers.remove(player.getUniqueId());

        if (isInVanishSpectator(player)) {
            restoreGameMode(player);
        }

        TabHook.setVanish(player, false);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    // ── Déconnexion / reconnexion ────────────────────────────────────────────

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        if (vanishedPlayers.remove(uuid) != null) {
            persistedVanishedPlayers.add(uuid);
        }
        lastSneak.remove(uuid);
    }

    public void handleJoin(Player joining) {
        if (persistedVanishedPlayers.contains(joining.getUniqueId()) && canVanish(joining)) {
            enableVanish(joining);
        } else {
            persistedVanishedPlayers.remove(joining.getUniqueId());
        }

        // Cacher au nouvel arrivant les vanishés qu'il n'a pas le niveau de voir
        for (Map.Entry<UUID, Integer> entry : vanishedPlayers.entrySet()) {
            Player vanished = Bukkit.getPlayer(entry.getKey());
            if (vanished == null || vanished.equals(joining)) continue;
            if (!canSeeVanished(joining, entry.getValue())) {
                joining.hidePlayer(plugin, vanished);
            }
        }
    }

    // ── Spectateur (double-sneak) ────────────────────────────────────────────

    public boolean isInVanishSpectator(Player player) {
        return previousGameMode.containsKey(player.getUniqueId());
    }

    public boolean handleSneak(Player player) {
        if (!plugin.getConfig().getBoolean("vanish.spectator-on-sneak.enabled", true)) return false;
        if (!isVanished(player)) return false;

        long delay = plugin.getConfig().getLong("vanish.spectator-on-sneak.double-sneak-delay", 400L);
        long now = System.currentTimeMillis();
        Long last = lastSneak.put(player.getUniqueId(), now);

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

    // ── Utilitaires ──────────────────────────────────────────────────────────

    /** Joueurs réellement visibles (thread-safe, utilisé par le ping serveur). */
    public int getVisiblePlayerCount() {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!vanishedPlayers.containsKey(p.getUniqueId())) count++;
        }
        return count;
    }

    public Set<UUID> getVanishedUUIDs() {
        return Collections.unmodifiableSet(vanishedPlayers.keySet());
    }

    public void clear() {
        vanishedPlayers.clear();
        persistedVanishedPlayers.clear();
        previousGameMode.clear();
        lastSneak.clear();
    }
}
