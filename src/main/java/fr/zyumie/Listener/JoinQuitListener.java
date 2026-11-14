package fr.zyumie.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JoinQuitListener implements Listener {

    private enum Mode { DEFAULT, CUSTOM, HIDDEN }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final Main plugin;

    // Cache thread-safe pour stocker les préfixes/suffixes et éviter les requêtes bloquantes au quit
    private final Map<UUID, String> prefixCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> suffixCache = new ConcurrentHashMap<>();

    public JoinQuitListener(Main plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Met en cache les données LuckPerms et ItemsAdder dès la connexion
        cachePlayerPrefixSuffix(player);

        if (!player.hasPlayedBefore() && cfg().getBoolean("join-quit.join.first-join.enabled", true)) {
            String raw = cfg().getString("join-quit.join.first-join.message",
                    "&d✦ &f%player% &7rejoint le serveur pour la première fois !");
            event.joinMessage(formatCached(raw, player));
            return;
        }

        switch (mode("join-quit.join.mode")) {
            case HIDDEN -> event.joinMessage(null);
            case CUSTOM -> event.joinMessage(formatCached(cfg().getString("join-quit.join.message",
                    "&8[&a+&8] &f%prefix%%player%%suffix% &7a rejoint le serveur"), player));
            case DEFAULT -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        switch (mode("join-quit.quit.mode")) {
            case HIDDEN -> event.quitMessage(null);
            case CUSTOM -> event.quitMessage(formatCached(cfg().getString("join-quit.quit.message",
                    "&8[&c-&8] &f%prefix%%player%%suffix% &7a quitté le serveur"), player));
            case DEFAULT -> { }
        }

        // Nettoie le cache pour libérer la mémoire
        prefixCache.remove(player.getUniqueId());
        suffixCache.remove(player.getUniqueId());
    }

    private void cachePlayerPrefixSuffix(Player player) {
        String prefix = "";
        String suffix = "";

        if (plugin.getLuckPermsHook() != null) {
            prefix = plugin.getLuckPermsHook().getPrefix(player);
            suffix = plugin.getLuckPermsHook().getSuffix(player);
        }

        if (plugin.getItemsAdderHook() != null
                && plugin.getItemsAdderHook().isAvailable()
                && cfg().getBoolean("chat.itemsadder.enabled", true)) {
            prefix = plugin.getItemsAdderHook().replaceFontImages(prefix);
            suffix = plugin.getItemsAdderHook().replaceFontImages(suffix);
        }

        prefixCache.put(player.getUniqueId(), prefix);
        suffixCache.put(player.getUniqueId(), suffix);
    }

    private Mode mode(String path) {
        String value = cfg().getString(path, "DEFAULT");
        try {
            return Mode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mode.DEFAULT;
        }
    }

    private Component formatCached(String raw, Player player) {
        String prefix = prefixCache.getOrDefault(player.getUniqueId(), "");
        String suffix = suffixCache.getOrDefault(player.getUniqueId(), "");

        String text = raw
                .replace("%player%", player.getName())
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix);

        return LEGACY.deserialize(ChatManager.colorize(text));
    }
}