package fr.zyumie.SoftDepend;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderAPIHook {

    private final boolean available;

    public PlaceholderAPIHook() {
        this.available = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public boolean isAvailable() {
        return available;
    }

    public String parse(Player player, String text) {
        if (!available || text == null || text.isEmpty()) return text == null ? "" : text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
