package fr.zyumie.Chat;

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

    private static final String MESSAGE_TOKEN = "\u0000MSG\u0000";

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

    private String papi(Player player, String text) {
        if (text == null) return "";
        if (plugin.getPlaceholderAPIHook() == null || !plugin.getPlaceholderAPIHook().isAvailable()) return text;
        if (!plugin.getConfig().getBoolean("chat.placeholderapi.enabled", true)) return text;
        return plugin.getPlaceholderAPIHook().parse(player, text);
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

        // ── Vanish ──
        if (plugin.getVanishManager().isVanished(player)) {
            String vSuffix = plugin.getConfig().getString("chat.vanish-chat-suffix", "/");
            String warning = plugin.getConfig().getString("chat.vanish-chat-warning",
                    "&7[Vanish] &cMettez &f/ &cen fin de message pour envoyer.");

            if (!event.getMessage().endsWith(vSuffix)) {
                event.setCancelled(true);
                player.sendMessage(colorize(warning));
                return;
            }
            event.setMessage(event.getMessage()
                    .substring(0, event.getMessage().length() - vSuffix.length()).stripTrailing());
        }

        // ── Prefix / Suffix LuckPerms ──
        String prefix = "";
        String suffix = "";
        if (plugin.getLuckPermsHook() != null) {
            prefix = plugin.getLuckPermsHook().getPrefix(player);
            suffix = plugin.getLuckPermsHook().getSuffix(player);
        }

        // ── Message joueur ──
        String rawMessage = event.getMessage();
        if (player.hasPermission("fireballwand.chat.placeholders")) {
            rawMessage = papi(player, rawMessage);
        }
        String message = colorizeWithPermission(player, rawMessage);

        boolean ia = plugin.getItemsAdderHook() != null
                && plugin.getItemsAdderHook().isAvailable()
                && plugin.getConfig().getBoolean("chat.itemsadder.enabled", true);

        if (ia) {
            prefix  = plugin.getItemsAdderHook().replaceFontImages(prefix);
            suffix  = plugin.getItemsAdderHook().replaceFontImages(suffix);
            message = plugin.getItemsAdderHook().replaceFontImages(message);
        }

        event.setMessage(message);

        String format = plugin.getConfig().getString("chat.format",
                "%prefix%%player%%suffix% &7: &f%message%");

        format = format
                .replace("%prefix%",  prefix)
                .replace("%player%",  player.getName())
                .replace("%suffix%",  suffix)
                .replace("%message%", MESSAGE_TOKEN);

        format = papi(player, format);

        if (ia) {
            format = plugin.getItemsAdderHook().replaceFontImages(format);
        }

        format = colorize(format);

        format = format.replace("%", "%%").replace(MESSAGE_TOKEN, "%2$s");

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
