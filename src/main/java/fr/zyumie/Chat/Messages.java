package fr.zyumie.Chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Lecture des messages de configuration des modules Home et Spawn.
 *
 * Deux formats sont acceptés, détectés automatiquement :
 *  - MiniMessage si le texte contient des balises  <green>, <yellow>, <b>...
 *  - sinon codes classiques & et hex &#RRGGBB (comme le reste du plugin).
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Gère le format hex produit par ChatManager.colorize (§x§r§r§g§g§b§b)
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private Messages() {}

    public static boolean isMiniMessage(String raw) {
        if (raw == null) return false;
        int open = raw.indexOf('<');
        return open >= 0 && raw.indexOf('>', open) > open;
    }

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        return isMiniMessage(raw) ? MM.deserialize(raw) : LEGACY.deserialize(ChatManager.colorize(raw));
    }

    /** Préfixe + message ; chacun peut être dans l'un ou l'autre format. */
    public static Component withPrefix(String prefix, String message) {
        if (prefix == null || prefix.isEmpty()) return parse(message);
        // Même format : un seul passage (les couleurs du préfixe se prolongent, comme avant)
        if (isMiniMessage(prefix) && isMiniMessage(message)) return MM.deserialize(prefix + message);
        return Component.empty().append(parse(prefix)).append(parse(message));
    }

    public static void send(CommandSender to, String prefix, String message) {
        to.sendMessage(withPrefix(prefix, message));
    }
}
