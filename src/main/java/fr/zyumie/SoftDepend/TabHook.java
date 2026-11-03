package fr.zyumie.SoftDepend;

import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Compatibilité TAB (Neznamy) — aucune dépendance de compilation requise.
 *
 * TAB considère un joueur comme "vanished" quand il possède la metadata
 * Bukkit "vanished" = true (convention partagée avec SuperVanish, Essentials...).
 * Les joueurs sans la permission TAB "tab.seevanished" ne le voient alors plus
 * dans la tablist, les nametags et les placeholders de comptage.
 *
 * Appeler cette méthode même si TAB est absent est sans danger.
 */
public final class TabHook {

    private static final String KEY = "vanished";

    private TabHook() {}

    public static void setVanish(Player player, boolean vanished) {
        Plugin plugin = JavaPlugin.getProvidingPlugin(TabHook.class);

        if (vanished) {
            player.setMetadata(KEY, new FixedMetadataValue(plugin, true));
        } else {
            player.removeMetadata(KEY, plugin);
        }
    }
}
