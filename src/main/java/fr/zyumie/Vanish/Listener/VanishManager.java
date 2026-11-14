package fr.zyumie.Vanish.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.SoftDepend.TabHook;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final Main plugin;

    // Gère le format hex produit par ChatManager.colorize (§x§r§r§g§g§b§b)
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    /** UUID → niveau de vanish actif */
    private final Map<UUID, Integer> vanishedPlayers = new ConcurrentHashMap<>();

    /** UUID → pseudo (lecture thread-safe pour l'auto-complétion, sans appeler Bukkit) */
    private final Map<UUID, String> vanishedNames = new ConcurrentHashMap<>();

    /** UUIDs des joueurs à remettre en vanish à la reconnexion */
    private final Set<UUID> persistedVanishedPlayers = ConcurrentHashMap.newKeySet();

    /** UUID → gamemode avant passage en spectateur */
    private final Map<UUID, GameMode> previousGameMode = new ConcurrentHashMap<>();

    /** UUID → timestamp du dernier sneak (double-sneak) */
    private final Map<UUID, Long> lastSneak = new ConcurrentHashMap<>();

    public VanishManager(Main plugin) {
        this.plugin = plugin;
    }

    /** Vanish.yml — les clés sont à la RACINE du fichier (max-level, actionbar, messages...). */
    private FileConfiguration cfg() {
        return plugin.getVanishConfig();
    }

    // ── Niveau ───────────────────────────────────────────────────────────────

    public int getVanishLevel(Player player) {
        int max = cfg().getInt("max-level", 10);
        for (int lvl = max; lvl >= 1; lvl--) {
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

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.containsKey(uuid);
    }

    /**
     * Le viewer a-t-il le droit de voir / connaître ce joueur ?
     * À utiliser dans les commandes (/tpa, /msg...) : si false, répondre « joueur introuvable ».
     */
    public boolean canSee(CommandSender viewer, Player target) {
        Integer level = vanishedPlayers.get(target.getUniqueId());
        if (level == null) return true;                          // pas en vanish
        if (!(viewer instanceof Player p)) return true;          // console
        if (p.getUniqueId().equals(target.getUniqueId())) return true;
        return getVanishLevel(p) >= level;
    }

    /**
     * Pseudos (en minuscules) des joueurs en vanish que ce viewer ne doit pas voir.
     * Thread-safe : n'appelle pas Bukkit.getPlayer (utilisé par l'auto-complétion).
     */
    public Set<String> getHiddenNamesFor(CommandSender viewer) {
        if (vanishedPlayers.isEmpty()) return Collections.emptySet();

        if (!(viewer instanceof Player viewerPlayer)) {
            return Collections.emptySet(); // console : voit tout
        }

        int viewerLevel = getVanishLevel(viewerPlayer);
        Set<String> hidden = new HashSet<>();

        for (Map.Entry<UUID, Integer> entry : vanishedPlayers.entrySet()) {
            UUID id = entry.getKey();
            if (id.equals(viewerPlayer.getUniqueId())) continue; // on se voit soi-même
            if (viewerLevel >= entry.getValue()) continue;
            String name = vanishedNames.get(id);
            if (name != null) hidden.add(name.toLowerCase());
        }
        return hidden;
    }

    /**
     * Joueur en ligne portant exactement ce pseudo, ou null s'il est introuvable
     * OU invisible pour ce viewer (vanish de niveau supérieur).
     * À utiliser dans toute commande qui résout un pseudo : la réponse « introuvable »
     * est alors identique pour un joueur hors ligne et pour un staff caché.
     */
    public Player findVisiblePlayerExact(CommandSender viewer, String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player != null && canSee(viewer, player) ? player : null;
    }

    /**
     * Comme Bukkit.getPlayer(String) (pseudo exact, sinon début de pseudo),
     * mais ne propose jamais un joueur que le viewer n'a pas le droit de voir.
     */
    public Player findVisiblePlayer(CommandSender viewer, String name) {
        Player exact = findVisiblePlayerExact(viewer, name);
        if (exact != null) return exact;
        if (Bukkit.getPlayerExact(name) != null) return null; // pseudo exact caché : pas de repli partiel

        String lower = name.toLowerCase(Locale.ROOT);
        Player best = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!canSee(viewer, p)) continue;
            if (!p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            if (best == null || p.getName().length() < best.getName().length()) best = p;
        }
        return best;
    }

    // ── Toggle ───────────────────────────────────────────────────────────────

    public void enableVanish(Player player) {
        enableVanish(player, true);
    }

    /**
     * @param announce true = diffuse le faux message de départ (toggle manuel) ;
     *                 false = silencieux (reconnexion d'un joueur déjà en vanish).
     */
    public void enableVanish(Player player, boolean announce) {
        UUID id = player.getUniqueId();
        int level = Math.max(getVanishLevel(player), 1);

        vanishedPlayers.put(id, level);
        vanishedNames.put(id, player.getName());
        persistedVanishedPlayers.add(id);

        TabHook.setVanish(player, true);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (!canSeeVanished(online, level)) {
                online.hidePlayer(plugin, player);
            }
        }

        if (announce) broadcastFakeMessage(player, level, false);
    }

    public void disableVanish(Player player) {
        UUID id = player.getUniqueId();
        int level = vanishedPlayers.getOrDefault(id, Math.max(getVanishLevel(player), 1));

        vanishedPlayers.remove(id);
        vanishedNames.remove(id);
        persistedVanishedPlayers.remove(id);

        if (isInVanishSpectator(player)) {
            restoreGameMode(player);
        }

        TabHook.setVanish(player, false);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        broadcastFakeMessage(player, level, true);
    }

    // ── Déconnexion / reconnexion ────────────────────────────────────────────

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        if (vanishedPlayers.remove(uuid) != null) {
            persistedVanishedPlayers.add(uuid);
        }
        vanishedNames.remove(uuid);
        lastSneak.remove(uuid);
    }

    public void handleJoin(Player joining) {
        if (persistedVanishedPlayers.contains(joining.getUniqueId()) && canVanish(joining)) {
            // Reconnexion en vanish : aucun faux message (il n'avait pas été vu arriver)
            enableVanish(joining, false);
        } else {
            persistedVanishedPlayers.remove(joining.getUniqueId());
        }

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
        if (!cfg().getBoolean("spectator-on-sneak.enabled", true)) return false;
        if (!isVanished(player)) return false;

        long delay = cfg().getLong("spectator-on-sneak.double-sneak-delay", 400L);
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
        String msgOn  = cfg().getString("messages.spectator-on",  "&7[Vanish] &aMode spectateur activé.");
        String msgOff = cfg().getString("messages.spectator-off", "&7[Vanish] &cMode spectateur désactivé.");

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

    // ── Faux messages de connexion / déconnexion ─────────────────────────────

    /**
     * Imite exactement ce que verraient les joueurs si la personne se connectait/déconnectait
     * pour de vrai (même mode et même texte que join-quit dans config.yml), afin que
     * personne ne remarque le vanish. Envoyé uniquement à ceux qui ne peuvent pas voir le staff.
     */
    private void broadcastFakeMessage(Player player, int level, boolean isJoin) {
        if (!cfg().getBoolean("fake-messages.enabled", true)) return;

        FileConfiguration main = plugin.getConfig();
        String modeStr = main.getString(isJoin ? "join-quit.join.mode" : "join-quit.quit.mode", "DEFAULT");

        Component component;
        switch (modeStr.trim().toUpperCase()) {
            case "HIDDEN" -> {
                return; // les vrais joueurs n'ont pas de message non plus
            }
            case "CUSTOM" -> {
                String raw = main.getString(isJoin ? "join-quit.join.message" : "join-quit.quit.message", "");
                if (raw == null || raw.isEmpty()) return;
                component = LEGACY.deserialize(ChatManager.colorize(applyPlaceholders(raw, player, main)));
            }
            default -> // DEFAULT : message vanilla
                    component = Component.translatable(
                            isJoin ? "multiplayer.player.joined" : "multiplayer.player.left",
                            NamedTextColor.YELLOW,
                            Component.text(player.getName()));
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (!canSeeVanished(online, level)) {
                online.sendMessage(component);
            }
        }
    }

    private String applyPlaceholders(String raw, Player player, FileConfiguration main) {
        String prefix = "";
        String suffix = "";

        if (plugin.getLuckPermsHook() != null) {
            prefix = plugin.getLuckPermsHook().getPrefix(player);
            suffix = plugin.getLuckPermsHook().getSuffix(player);
        }

        if (plugin.getItemsAdderHook() != null
                && plugin.getItemsAdderHook().isAvailable()
                && main.getBoolean("chat.itemsadder.enabled", true)) {
            prefix = plugin.getItemsAdderHook().replaceFontImages(prefix);
            suffix = plugin.getItemsAdderHook().replaceFontImages(suffix);
        }

        return raw
                .replace("%player%", player.getName())
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix);
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

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
        vanishedNames.clear();
        persistedVanishedPlayers.clear();
        previousGameMode.clear();
        lastSneak.clear();
    }
}
