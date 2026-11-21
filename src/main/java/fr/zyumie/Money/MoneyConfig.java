package fr.zyumie.Money;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

/** Charge et encapsule Money.yml (séparé de config.yml du plugin). */
public final class MoneyConfig {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    public MoneyConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "Money.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("Money.yml")) {
                if (in != null) Files.copy(in, file.toPath());
                else file.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Impossible de créer Money.yml : " + e.getMessage());
            }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Copie l'ancienne config Money.jar vers Money.yml si ce dernier n'existe pas encore.
     * À appeler AVANT new MoneyConfig(plugin).
     */
    public static void migrateOldConfigIfNeeded(JavaPlugin plugin) {
        File newFile = new File(plugin.getDataFolder(), "Money.yml");
        if (newFile.exists()) return;  // L'admin a déjà sa config, on ne touche pas

        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null) return;

        File oldFile = null;
        for (String mainName : new String[]{"Main", "MainPlugin", "main", "mainplugin"}) {
            File candidate = new File(new File(pluginsDir, mainName), "money/config.yml");
            if (candidate.exists()) { oldFile = candidate; break; }
        }
        if (oldFile == null) return;

        try {
            YamlConfiguration oldCfg = YamlConfiguration.loadConfiguration(oldFile);
            YamlConfiguration newCfg = new YamlConfiguration();

            // Copie uniquement les sections qui existent dans le nouveau format
            for (String section : new String[]{"economy", "moneytop", "messages"}) {
                ConfigurationSection sec = oldCfg.getConfigurationSection(section);
                if (sec == null) continue;
                for (String key : sec.getKeys(true)) {
                    if (sec.isConfigurationSection(key)) continue;
                    newCfg.set(section + "." + key, sec.get(key));
                }
            }
            newCfg.set("mode", "internal");  // les soldes migrés doivent être utilisés par FireBallWand
            newCfg.save(newFile);
            plugin.getLogger().info("[Money] Ancienne config migrée depuis " + oldFile.getPath());
        } catch (Exception e) {
            plugin.getLogger().warning("[Money] Migration config échouée : " + e.getMessage());
        }
    }

    public void reload() { load(); }

    public String mode()             { return cfg.getString("mode", "internal").trim().toLowerCase(Locale.ROOT); }
    public String currencySingular() { return cfg.getString("economy.currency-singular", "Crédit"); }
    public String currencyPlural()   { return cfg.getString("economy.currency-plural", "Crédits"); }
    public double startingBalance()  { return cfg.getDouble("economy.starting-balance", 500.0); }
    public double minTransaction()   { return cfg.getDouble("economy.min-transaction", 0.5); }
    public double maxBalance()       { return cfg.getDouble("economy.max-balance", 999_999_999.0); }
    public int topPerPage()          { return cfg.getInt("moneytop.entries-per-page", 10); }
    public long topCacheSeconds()    { return cfg.getLong("moneytop.cache-seconds", 30); }
    public String msg(String path)   { return cfg.getString("messages." + path, ""); }
    public String prefix()           { return msg("prefix"); }
}