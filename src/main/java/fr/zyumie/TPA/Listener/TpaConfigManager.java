package fr.zyumie.TPA.Listener;

import fr.zyumie.fireballwand.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TpaConfigManager {

    private final Main plugin;
    private File configFile;
    private FileConfiguration config;

    private String prefix;
    private int tpaCooldown;
    private int requestExpirationSeconds;
    private int warmupSeconds;
    private boolean cancelOnMove;
    private boolean cancelOnDamage;
    private int invincibilitySeconds;
    private boolean showInvincibilityActionBar;
    private boolean cancelOnPvp;
    private boolean playSounds;
    private boolean spawnParticles;

    public TpaConfigManager(Main plugin) {
        this.plugin = plugin;
        createConfig();
        loadConfig();
    }

    private void createConfig() {
        configFile = new File(plugin.getDataFolder(), "Tpa.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("Tpa.yml", false);
        }
    }

    public void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource("Tpa.yml");
        if (defConfigStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
        }

        this.prefix = config.getString("prefix", "&8[&bTPA&8] &r");
        this.tpaCooldown = config.getInt("cooldowns.tpa", 600);
        this.requestExpirationSeconds = config.getInt("request-expiration-seconds", 60);
        this.warmupSeconds = config.getInt("warmup.seconds", 3);
        this.cancelOnMove = config.getBoolean("warmup.cancel-on-move", true);
        this.cancelOnDamage = config.getBoolean("warmup.cancel-on-damage", true);
        this.invincibilitySeconds = config.getInt("invincibility.seconds", 30);
        this.showInvincibilityActionBar = config.getBoolean("invincibility.show-actionbar", true);
        this.cancelOnPvp = config.getBoolean("invincibility.cancel-on-pvp", true);
        this.playSounds = config.getBoolean("effects.play-sounds", true);
        this.spawnParticles = config.getBoolean("effects.spawn-particles", true);
    }

    public FileConfiguration getConfig() { return config; }

    public String getPrefix() { return prefix; }
    public int getTpaCooldown() { return tpaCooldown; }
    public int getRequestExpirationSeconds() { return requestExpirationSeconds; }
    public int getWarmupSeconds() { return warmupSeconds; }
    public boolean isCancelOnMove() { return cancelOnMove; }
    public boolean isCancelOnDamage() { return cancelOnDamage; }
    public int getInvincibilitySeconds() { return invincibilitySeconds; }
    public boolean isShowInvincibilityActionBar() { return showInvincibilityActionBar; }
    public boolean isCancelOnPvp() { return cancelOnPvp; }
    public boolean isPlaySounds() { return playSounds; }
    public boolean isSpawnParticles() { return spawnParticles; }

    public String getMessage(String path, String def) {
        return config.getString("messages." + path, def);
    }
}