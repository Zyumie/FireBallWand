package fr.zyumie.Chat;

import fr.zyumie.fireballwand.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsgManager {

    private final Main plugin;

    // Pattern HEX : &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");

    // Stocke la dernière relation de message : UUID du joueur -> UUID de son dernier interlocuteur
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();

    public MsgManager(Main plugin) {
        this.plugin = plugin;
    }


    //  Colorize simple (HEX + &codes)

    public static String colorize(String text) {
        if (text == null) return "";

        Matcher hex = HEX_PATTERN.matcher(text);
        StringBuffer buf = new StringBuffer();
        while (hex.find()) {
            hex.appendReplacement(buf, ChatColor.of("#" + hex.group(1)).toString());
        }
        hex.appendTail(buf);

        return ChatColor.translateAlternateColorCodes('&', buf.toString());
    }


    //  Colorize selon les permissions du joueur

    private String colorizeWithPermission(Player player, String text) {
        if (text == null) return "";

        boolean colorEnabled = plugin.getConfig().getBoolean("chat.color.enabled", true);
        boolean hasColor     = colorEnabled && player.hasPermission("fireballwand.chat.color");
        boolean hasHex       = colorEnabled && player.hasPermission("fireballwand.chat.color.hex");

        String result = text;

        if (hasHex) {
            Matcher hex = HEX_PATTERN.matcher(result);
            StringBuffer buf = new StringBuffer();
            while (hex.find()) {
                hex.appendReplacement(buf, ChatColor.of("#" + hex.group(1)).toString());
            }
            hex.appendTail(buf);
            result = buf.toString();
        } else {
            result = result.replaceAll("&#[a-fA-F0-9]{6}", "");
        }

        if (hasColor || hasHex) {
            result = ChatColor.translateAlternateColorCodes('&', result);
        } else {
            result = result.replace("&", "");
        }

        return result;
    }


    //  Envoi du message privé formaté & mémorisation pour le /r

    public void sendPrivateMessage(Player sender, Player target, String rawMessage) {
        // 1. Appliquer les permissions de couleur
        String message = colorizeWithPermission(sender, rawMessage);

        // 2. Support ItemsAdder si actif
        if (plugin.getItemsAdderHook() != null
                && plugin.getItemsAdderHook().isAvailable()
                && plugin.getConfig().getBoolean("chat.itemsadder.enabled", true)) {
            message = plugin.getItemsAdderHook().replaceFontImages(message);
        }

        // 3. Récupération des formats depuis la config
        String formatTo = plugin.getConfig().getString("messages.format-to", "&7(To &f%target%&7) &f%message%");
        String formatFrom = plugin.getConfig().getString("messages.format-from", "&7(From &f%sender%&7) &f%message%");

        // 4. Remplacements des variables
        String finalTo = formatTo
                .replace("%target%", target.getName())
                .replace("%sender%", sender.getName())
                .replace("%message%", message);

        String finalFrom = formatFrom
                .replace("%target%", target.getName())
                .replace("%sender%", sender.getName())
                .replace("%message%", message);

        // 5. Envoi des messages
        sender.sendMessage(colorize(finalTo));
        target.sendMessage(colorize(finalFrom));

        // 6. Enregistrement de la conversation pour la commande /r
        lastMessaged.put(sender.getUniqueId(), target.getUniqueId());
        lastMessaged.put(target.getUniqueId(), sender.getUniqueId());
    }

    //  Récupérer la dernière personne à qui le joueur a parlé
    public UUID getLastTarget(Player player) {
        return lastMessaged.get(player.getUniqueId());
    }
}