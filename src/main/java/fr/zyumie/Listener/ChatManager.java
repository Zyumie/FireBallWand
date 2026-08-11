package fr.zyumie.Listener;

import fr.zyumie.fireballwand.Main;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatManager implements Listener {

    private final Main plugin;

    // Pattern HEX : &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");
    // Pattern couleurs classiques : &x
    private static final Pattern COLOR_PATTERN = Pattern.compile("&[0-9a-fA-FkKlLmMnNoOrR]");

    public ChatManager(Main plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    //  Colorize complet (HEX + &codes) — usage interne (préfixes etc.)
    // ---------------------------------------------------------------
    public static String colorize(String text) {
        if (text == null) return "";

        // HEX d'abord
        Matcher hex = HEX_PATTERN.matcher(text);
        StringBuffer buf = new StringBuffer();
        while (hex.find()) {
            hex.appendReplacement(buf, ChatColor.of("#" + hex.group(1)).toString());
        }
        hex.appendTail(buf);

        // Puis &codes classiques
        return ChatColor.translateAlternateColorCodes('&', buf.toString());
    }

    // ---------------------------------------------------------------
    //  Colorize selon les permissions du joueur
    //  - fireballwand.chat.color     → &codes classiques
    //  - fireballwand.chat.color.hex → codes HEX en plus
    // ---------------------------------------------------------------
    private String colorizeWithPermission(Player player, String text) {
        if (text == null) return "";

        boolean colorEnabled  = plugin.getConfig().getBoolean("chat.color.enabled", true);
        boolean hasColor      = colorEnabled && player.hasPermission("fireballwand.chat.color");
        boolean hasHex        = colorEnabled && player.hasPermission("fireballwand.chat.color.hex");

        String result = text;

        // HEX (uniquement si permission hex)
        if (hasHex) {
            Matcher hex = HEX_PATTERN.matcher(result);
            StringBuffer buf = new StringBuffer();
            while (hex.find()) {
                hex.appendReplacement(buf, ChatColor.of("#" + hex.group(1)).toString());
            }
            hex.appendTail(buf);
            result = buf.toString();
        } else {
            // Strip les codes HEX bruts pour qu'ils n'apparaissent pas tels quels
            result = result.replaceAll("&#[a-fA-F0-9]{6}", "");
        }

        // Codes classiques (uniquement si permission color ou hex)
        if (hasColor || hasHex) {
            result = ChatColor.translateAlternateColorCodes('&', result);
        } else {
            // On retire les & pour éviter que ça s'affiche brut
            result = result.replace("&", "");
        }

        return result;
    }

    // ---------------------------------------------------------------
    //  Format du Chat
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // ── Vanish : message bloqué sauf si se termine par le suffixe config ──
        if (plugin.getVanishManager().isVanished(player)) {
            String suffix  = plugin.getConfig().getString("chat.vanish-chat-suffix", "/");
            String warning = plugin.getConfig().getString("chat.vanish-chat-warning",
                    "&7[Vanish] &cMettez &f/ &cen fin de message pour envoyer.");

            if (!event.getMessage().endsWith(suffix)) {
                event.setCancelled(true);
                player.sendMessage(colorize(warning));
                return;
            }
            // Retirer le suffixe avant envoi
            event.setMessage(event.getMessage()
                    .substring(0, event.getMessage().length() - suffix.length()).stripTrailing());
        }

        // ── Préfix / Suffix LuckPerms ──
        String prefix = "";
        String suffix = "";
        if (plugin.getLuckPermsHook() != null) {
            prefix = plugin.getLuckPermsHook().getPrefix(player);
            suffix = plugin.getLuckPermsHook().getSuffix(player);
        }

        // ── ItemsAdder : résolution des glyphes dans préfix/suffix ──
        if (plugin.getItemsAdderHook() != null
                && plugin.getItemsAdderHook().isAvailable()
                && plugin.getConfig().getBoolean("chat.itemsadder.enabled", true)) {
            prefix = plugin.getItemsAdderHook().replaceFontImages(prefix);
            suffix = plugin.getItemsAdderHook().replaceFontImages(suffix);
        }

        // Coloriser préfix/suffix (droits serveur, pas du joueur)
        prefix = colorize(prefix);
        suffix = colorize(suffix);

        // ── Message joueur : couleurs selon permission ──
        String message = colorizeWithPermission(player, event.getMessage());
        event.setMessage(message);

        // ── Format final ──
        String format = plugin.getConfig().getString("chat.format",
                "%prefix%%player%%suffix% &7: &f%message%");

        format = format
                .replace("%prefix%",  prefix)
                .replace("%player%",  player.getName())
                .replace("%suffix%",  suffix)
                .replace("%message%", "%2$s"); // %2$s = placeholder Bukkit pour le message

        // Coloriser le format lui-même (hors message joueur déjà traité)
        format = colorize(format);

        event.setFormat(format);
    }

    // ---------------------------------------------------------------
    //  Message de Bienvenue (première connexion)
    // ---------------------------------------------------------------
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) return;
        if (!plugin.getConfig().getBoolean("welcome.enabled", true)) return;

        String msg = plugin.getConfig().getString("welcome.msg");
        if (msg == null || msg.isEmpty()) return;

        msg = msg.replace("%player%", event.getPlayer().getName());
        event.getPlayer().sendMessage(colorize(msg));
    }
}
