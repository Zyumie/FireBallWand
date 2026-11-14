package fr.zyumie.Spawn;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Locale;

/** Téléportation au spawn à la connexion et à la réapparition (l'attente de /spawn est gérée par WarmupService). */
public final class SpawnListener implements Listener {

    private final Main plugin;
    private final SpawnManager spawns;

    public SpawnListener(Main plugin, SpawnManager spawns) {
        this.plugin = plugin;
        this.spawns = spawns;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        String mode = plugin.getSpawnConfig().getString("join-teleport", "FIRST_JOIN").toUpperCase(Locale.ROOT);
        boolean teleport = switch (mode) {
            case "ALWAYS" -> true;
            case "FIRST_JOIN" -> !event.getPlayer().hasPlayedBefore();
            default -> false;
        };
        if (!teleport) return;

        Location spawn = spawns.getSpawn();
        if (spawn != null) event.getPlayer().teleportAsync(spawn);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getSpawnConfig().getBoolean("respawn-at-spawn", true)) return;

        // Un lit / une ancre de réapparition valide garde la priorité
        if (event.isBedSpawn() || event.isAnchorSpawn()) return;

        Location spawn = spawns.getSpawn();
        if (spawn != null) event.setRespawnLocation(spawn);
    }
}
