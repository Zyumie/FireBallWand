package fr.zyumie.Money.Storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stockage YAML : money-data.yml, chargé une fois en mémoire. */
public class YamlMoneyStorage implements MoneyStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Double> data = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public YamlMoneyStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "money-data.yml");
    }

    @Override
    public synchronized void load() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs() && !folder.isDirectory()) {
            plugin.getLogger().severe("[Money] Impossible de créer le dossier " + folder.getPath());
            return;
        }

        // La migration doit impérativement finir avant le chargement du cache.
        new MoneyMigration(plugin, file).run();

        data.clear();
        dirty = false;
        if (!file.isFile()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("balances");
        if (section == null) return;

        int skipped = 0;
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Object raw = section.get(key);
                if (!(raw instanceof Number number)) {
                    skipped++;
                    continue;
                }
                double balance = number.doubleValue();
                if (!Double.isFinite(balance) || balance < 0.0) {
                    skipped++;
                    continue;
                }
                data.put(uuid, balance);
            } catch (IllegalArgumentException ex) {
                skipped++;
            }
        }

        plugin.getLogger().info("[Money] " + data.size() + " solde(s) chargé(s) depuis " + file.getName()
                + (skipped > 0 ? " ; " + skipped + " entrée(s) invalide(s) ignorée(s)." : "."));
    }

    @Override public Double get(UUID uuid) { return data.get(uuid); }
    @Override public void put(UUID uuid, double value) {
        if (uuid == null || !Double.isFinite(value) || value < 0.0) return;
        data.put(uuid, value);
        dirty = true;
    }
    @Override public Map<UUID, Double> all() { return Collections.unmodifiableMap(data); }

    @Override
    public synchronized void flush() {
        if (!dirty) return;

        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : data.entrySet()) {
            yml.set("balances." + entry.getKey(), entry.getValue());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IllegalStateException("Impossible de créer " + parent.getPath());
            }
            yml.save(file);
            dirty = false;
        } catch (Exception ex) {
            plugin.getLogger().warning("[Money] Échec de sauvegarde de " + file.getName() + " : "
                    + ex.getMessage());
        }
    }
}
