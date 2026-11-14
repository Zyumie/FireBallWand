package fr.zyumie.Spawn;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Position du spawn, stockée dans plugins/FireBallWand/data/spawn-location.yml
 * (volontairement différent de Spawn.yml : Windows ne distingue pas majuscules et minuscules).
 * Si l'ancien plugin NewSpawn avait un spawn.yml, il est repris automatiquement au premier démarrage.
 */
public final class SpawnManager {

    private final Main plugin;
    private final File file;

    private String world;
    private double x, y, z;
    private float yaw, pitch;
    private boolean defined;

    public SpawnManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/spawn-location.yml");
    }

    public void load() {
        if (!file.exists()) importLegacy();
        if (!file.exists()) {
            defined = false;
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        this.world = yml.getString("world");
        this.x = yml.getDouble("x");
        this.y = yml.getDouble("y");
        this.z = yml.getDouble("z");
        this.yaw = (float) yml.getDouble("yaw");
        this.pitch = (float) yml.getDouble("pitch");
        this.defined = world != null;
    }

    /** Reprend plugins/NewSpawn/spawn.yml (ancien plugin) sans le modifier. */
    private void importLegacy() {
        File old = new File(plugin.getDataFolder().getParentFile(), "NewSpawn/spawn.yml");
        if (!old.exists()) return;
        if (YamlConfiguration.loadConfiguration(old).getString("world") == null) return;

        try {
            Files.createDirectories(file.getParentFile().toPath());
            Files.copy(old.toPath(), file.toPath());
            plugin.getLogger().info("[Spawn] Spawn repris depuis l'ancien plugin NewSpawn (fichier d'origine conservé).");
        } catch (IOException e) {
            plugin.getLogger().warning("[Spawn] Import de l'ancien spawn impossible : " + e.getMessage());
        }
    }

    public void set(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        this.defined = true;
        save();
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("world", world);
        yml.set("x", x);
        yml.set("y", y);
        yml.set("z", z);
        yml.set("yaw", yaw);
        yml.set("pitch", pitch);
        try {
            Files.createDirectories(file.getParentFile().toPath());
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Spawn] Impossible de sauvegarder le spawn : " + e.getMessage());
        }
    }

    public boolean isDefined() {
        return defined;
    }

    /** Le monde est résolu à la demande (il peut être chargé après le plugin). */
    public World getWorld() {
        return defined ? Bukkit.getWorld(world) : null;
    }

    /** @return le spawn, ou null s'il n'est pas défini / si son monde n'est pas chargé */
    public Location getSpawn() {
        World w = getWorld();
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
