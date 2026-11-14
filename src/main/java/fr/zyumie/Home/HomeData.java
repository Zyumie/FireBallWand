package fr.zyumie.Home;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Un home. Le monde est gardé sous forme de nom et résolu seulement au moment de la
 * téléportation : on peut donc lire les données hors du thread principal, et un home
 * dont le monde est chargé plus tard n'est jamais perdu.
 */
public record HomeData(String world, double x, double y, double z, float yaw, float pitch, long setTime) {

    public static HomeData of(Location loc, long setTime) {
        return new HomeData(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(), setTime);
    }

    /** @return la position, ou null si le monde n'est pas chargé */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
