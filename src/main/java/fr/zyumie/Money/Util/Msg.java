package fr.zyumie.Money.Util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/** Envoi de messages MiniMessage (<red>, <gold>, <bold>…). */
public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Msg() {}

    public static Component parse(String text) {
        return MM.deserialize(text == null ? "" : text);
    }

    public static void send(CommandSender sender, String text) {
        if (sender == null) return;
        sender.sendMessage(parse(text));
    }
}