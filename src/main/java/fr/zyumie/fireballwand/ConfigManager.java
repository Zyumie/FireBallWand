package fr.zyumie.fireballwand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager implements Listener {

    private final Main plugin;

    private File vanishFile;
    private FileConfiguration vanishConfig;

    private File customCheatFile;
    private FileConfiguration customCheatConfig;

    private File startKitFile;
    private FileConfiguration startKitConfig;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        // ── config.yml ───────────────────────────────────────────────────────
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        // ── Vanish.yml ───────────────────────────────────────────────────────
        vanishFile = new File(plugin.getDataFolder(), "Vanish.yml");
        if (!vanishFile.exists()) {
            plugin.saveResource("Vanish.yml", false);
        }
        vanishConfig = YamlConfiguration.loadConfiguration(vanishFile);
        InputStream vanishStream = plugin.getResource("Vanish.yml");
        if (vanishStream != null) {
            vanishConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(vanishStream, StandardCharsets.UTF_8)));
        }

        // ── CustomCheat.yml ──────────────────────────────────────────────────
        customCheatFile = new File(plugin.getDataFolder(), "CustomCheat.yml");
        if (!customCheatFile.exists()) {
            plugin.saveResource("CustomCheat.yml", false);
        }
        customCheatConfig = YamlConfiguration.loadConfiguration(customCheatFile);
        InputStream cheatStream = plugin.getResource("CustomCheat.yml");
        if (cheatStream != null) {
            customCheatConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(cheatStream, StandardCharsets.UTF_8)));
        }

        // ── StartKit.yml ─────────────────────────────────────────────────────
        startKitFile = new File(plugin.getDataFolder(), "StartKit.yml");
        if (!startKitFile.exists()) {
            plugin.saveResource("StartKit.yml", false);
        }
        startKitConfig = YamlConfiguration.loadConfiguration(startKitFile);
        InputStream kitStream = plugin.getResource("StartKit.yml");
        if (kitStream != null) {
            startKitConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(kitStream, StandardCharsets.UTF_8)));
        }
    }

    public FileConfiguration getVanishConfig() {
        return vanishConfig;
    }

    public FileConfiguration getCustomCheatConfig() {
        return customCheatConfig;
    }

    public FileConfiguration getStartKitConfig() {
        return startKitConfig;
    }

    public void reload() {
        loadConfigs();
    }
}