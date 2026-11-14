package fr.zyumie.Home;

import fr.zyumie.fireballwand.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Import unique des homes de l'ancien plugin Home (plugin « Main », fichier data/home.yml).
 *
 * Le fichier d'origine n'est JAMAIS modifié ni supprimé (il sert de sauvegarde). Un joueur qui a
 * déjà un fichier dans le nouveau système n'est pas écrasé. Une fois l'import fait, un fichier
 * marqueur évite de le relancer. Les anciens stockages MySQL ne sont pas importés.
 */
public final class HomeMigration {

    private static final String MARKER = ".imported";
    private static final String[] CANDIDATES = {"MainPlugin/data/home.yml", "Main/data/home.yml"};

    private HomeMigration() {}

    public static void run(Main plugin, HomeManager manager) {
        File marker = new File(new File(plugin.getDataFolder(), "data/homes"), MARKER);
        if (marker.exists()) return;

        File pluginsDir = plugin.getDataFolder().getParentFile();
        File source = null;
        for (String candidate : CANDIDATES) {
            File f = new File(pluginsDir, candidate);
            if (f.exists()) {
                source = f;
                break;
            }
        }
        if (source == null) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(source);
        int players = 0;
        int homes = 0;

        // Disposition 1 : homes.<uuid>...   Disposition 2 : <uuid>... (format du DataStore)
        ConfigurationSection nested = yml.getConfigurationSection("homes");
        Map<UUID, Map<String, HomeData>> found = new LinkedHashMap<>();
        if (nested != null) collect(nested, found);
        collect(yml, found);

        for (Map.Entry<UUID, Map<String, HomeData>> entry : found.entrySet()) {
            if (entry.getValue().isEmpty() || manager.file(entry.getKey()).exists()) continue;
            manager.write(entry.getKey(), entry.getValue());
            players++;
            homes += entry.getValue().size();
        }

        try {
            Files.writeString(marker.toPath(), "Import depuis " + source.getPath() + " le " + new java.util.Date() + "\n");
        } catch (IOException e) {
            plugin.getLogger().warning("[Home] Marqueur d'import non écrit : " + e.getMessage());
        }
        plugin.getLogger().info("[Home] Import terminé : " + homes + " home(s) pour " + players
                + " joueur(s) depuis " + source.getPath() + " (fichier d'origine conservé).");
    }

    private static void collect(ConfigurationSection root, Map<UUID, Map<String, HomeData>> out) {
        for (String key : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue; // clé qui n'est pas un joueur (ex : "homes")
            }
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            Map<String, HomeData> playerHomes = out.computeIfAbsent(uuid, k -> new LinkedHashMap<>());

            ConfigurationSection sub = sec.getConfigurationSection("homes");
            if (sub != null) {
                for (String name : sub.getKeys(false)) {
                    ConfigurationSection h = sub.getConfigurationSection(name);
                    HomeData data = h == null ? null : read(h);
                    if (data != null) playerHomes.put(uniqueName(sanitize(name), playerHomes), data);
                }
            }
            // Très ancien format : un seul home directement sous l'UUID
            if (sec.contains("world")) {
                HomeData data = read(sec);
                if (data != null) playerHomes.putIfAbsent("default", data);
            }
        }
    }

    private static HomeData read(ConfigurationSection s) {
        String world = s.getString("world");
        if (world == null || world.isBlank()) return null;
        return new HomeData(world, s.getDouble("x", 0), s.getDouble("y", 64), s.getDouble("z", 0),
                (float) s.getDouble("yaw", 0), (float) s.getDouble("pitch", 0),
                s.getLong("set-time", System.currentTimeMillis()));
    }

    /** Garde uniquement a-z, 0-9, _ et - (évite les balises de texte et les clés ambiguës). */
    private static String sanitize(String name) {
        String clean = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        if (clean.length() > 32) clean = clean.substring(0, 32);
        return clean.isEmpty() ? "home" : clean;
    }

    private static String uniqueName(String base, Map<String, HomeData> existing) {
        String name = base;
        int i = 2;
        while (existing.containsKey(name)) name = base + "_" + i++;
        return name;
    }
}
