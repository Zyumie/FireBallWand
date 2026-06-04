package fr.zyumie.fireballwand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

public class ConfigManager implements Listener {

    private final Main plugin;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }
}
