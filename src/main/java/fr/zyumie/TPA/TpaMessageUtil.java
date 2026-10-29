package fr.zyumie.TPA;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TpaMessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) rep.append('§').append(c);
            matcher.appendReplacement(buffer, rep.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static void sendMessage(CommandSender sender, String prefix, String message) {
        if (sender != null && message != null) sender.sendMessage(colorize(prefix + message));
    }

    public static void sendTpaRequestButtons(Player player, String prefix, String senderName) {
        Component accept = Component.text("[ACCEPTER]").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept " + senderName));
        Component deny = Component.text("[REFUSER]").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpdeny " + senderName));
        player.sendMessage(Component.text(colorize(prefix)).append(accept).append(Component.text("   ")).append(deny));
    }

    public static String formatDuration(long secs) {
        if (secs <= 0) return "0s";
        long m = secs / 60, s = secs % 60;
        return m > 0 ? (s > 0 ? m + "m " + s + "s" : m + "m") : s + "s";
    }

    public static void playSound(Player player, Sound sound, float vol, float pitch) {
        if (player != null && sound != null) player.playSound(player.getLocation(), sound, vol, pitch);
    }
}