package fr.zyumie.Commandes.FunnyItems;

import fr.zyumie.Chat.ChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Outils communs aux Funny Items (textes, give, immunité, effets de potion). */
public final class FunnyItemUtil {

    // Gère le format hex produit par ChatManager.colorize (§x§r§r§g§g§b§b)
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private FunnyItemUtil() {}

    /** Texte d'item (nom / lore) : codes & et hex, sans l'italique par défaut. */
    public static Component text(String raw) {
        return LEGACY.deserialize(ChatManager.colorize(raw)).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(text(line));
        return out;
    }

    /** Donne l'item ; ce qui ne rentre pas dans l'inventaire tombe aux pieds du joueur. */
    public static void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    /** OP et détenteurs de la permission de bypass ne sont pas affectés (si ignoreOps = true). */
    public static boolean isImmune(Player target, boolean ignoreOps, String bypassPermission) {
        return ignoreOps && (target.isOp() || target.hasPermission(bypassPermission));
    }

    /**
     * Lit un effet depuis la config au format "type:durée_en_ticks:amplificateur"
     * (ex : "slowness:100:1"). Retourne null (et prévient dans la console) s'il est invalide.
     */
    public static PotionEffect parseEffect(String raw, Logger log) {
        if (raw == null || raw.isBlank()) return null;

        String[] parts = raw.trim().split(":");
        PotionEffectType type = null;
        try {
            type = Registry.EFFECT.get(NamespacedKey.minecraft(parts[0].trim().toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            // nom invalide : traité comme inconnu ci-dessous
        }
        if (type == null) {
            log.warning("Effet inconnu dans la config : '" + raw + "'");
            return null;
        }

        int duration = parts.length > 1 ? parseInt(parts[1], 100) : 100;
        int amplifier = parts.length > 2 ? parseInt(parts[2], 0) : 0;
        return new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier), false, true, true);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
