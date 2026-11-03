package fr.zyumie.Staff;

import fr.zyumie.fireballwand.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/** Charge Staff.yml (créé depuis le jar au premier démarrage) et permet de le recharger. */
public class StaffConfig {

    private static final String FILE_NAME = "Staff.yml";

    private final File file;
    private FileConfiguration config;

    public StaffConfig(Main plugin) {
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration get() {
        return config;
    }
}
