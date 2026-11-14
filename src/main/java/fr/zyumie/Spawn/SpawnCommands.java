package fr.zyumie.Spawn;

import fr.zyumie.Chat.Messages;
import fr.zyumie.Teleport.WarmupService;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * /spawn et /setspawn.
 *
 * Permissions : fireballwand.spawn.use (tous) / fireballwand.spawn.set (OP) ; les anciennes
 * newspawn.use / newspawn.set restent acceptées. Bypass : fireballwand.spawn.bypass.warmup / .cooldown.
 */
public final class SpawnCommands implements CommandExecutor {

    private final Main plugin;
    private final SpawnManager spawns;
    private final WarmupService warmup;

    public SpawnCommands(Main plugin, SpawnManager spawns, WarmupService warmup) {
        this.plugin = plugin;
        this.spawns = spawns;
        this.warmup = warmup;
    }

    private FileConfiguration cfg() {
        return plugin.getSpawnConfig();
    }

    /** Envoie un message de Spawn.yml ; pairs = placeholder1, valeur1, placeholder2, valeur2... */
    private void send(CommandSender to, String key, String def, String... pairs) {
        String message = cfg().getString("messages." + key, def);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            message = message.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        Messages.send(to, cfg().getString("messages.prefix", ""), message);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "players-only", "<red>Cette commande est réservée aux joueurs.</red>");
            return true;
        }

        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "setspawn" -> handleSet(player);
            case "spawn" -> handleSpawn(player);
            default -> { return false; }
        }
        return true;
    }

    private void handleSet(Player player) {
        if (!player.hasPermission("fireballwand.spawn.set") && !player.hasPermission("newspawn.set")) {
            send(player, "no-permission", "<red>Tu n'as pas la permission d'effectuer cette action.</red>");
            return;
        }

        Location loc = player.getLocation();
        spawns.set(loc);
        send(player, "spawn-set", "<green>Spawn défini en <white>{world} <gray>({x}, {y}, {z})</gray></white>.</green>",
                "world", loc.getWorld().getName(),
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ()));
    }

    private void handleSpawn(Player player) {
        if (!player.hasPermission("fireballwand.spawn.use") && !player.hasPermission("newspawn.use")) {
            send(player, "no-permission", "<red>Tu n'as pas la permission d'effectuer cette action.</red>");
            return;
        }
        if (!spawns.isDefined()) {
            send(player, "no-spawn", "<red>Aucun spawn n'a été défini. Utilise <white>/setspawn</white>.</red>");
            return;
        }
        if (spawns.getSpawn() == null) {
            send(player, "world-missing", "<red>Le monde du spawn est introuvable.</red>");
            return;
        }
        if (warmup.isPending(player)) {
            send(player, "already-pending", "<red>Une téléportation est déjà en cours.</red>");
            return;
        }

        boolean bypassCooldown = player.hasPermission("fireballwand.spawn.bypass.cooldown");
        boolean bypassWarmup = player.hasPermission("fireballwand.spawn.bypass.warmup");

        if (!bypassCooldown) {
            long left = warmup.cooldownLeft(player.getUniqueId(), "spawn");
            if (left > 0) {
                send(player, "cooldown", "<red>Patiente encore <white>{seconds}s</white> avant de réutiliser /spawn.</red>",
                        "seconds", String.valueOf(left));
                return;
            }
        }

        int delay = bypassWarmup ? 0 : Math.max(0, cfg().getInt("spawn-delay", 15));
        if (delay > 0) {
            send(player, "teleport-pending", "<yellow>Téléportation au spawn dans <white>{time}s</white>. Ne bouge pas !</yellow>",
                    "time", String.valueOf(delay));
        }

        warmup.start(player, delay,
                cfg().getBoolean("cancel-on-move", true),
                cfg().getBoolean("cancel-on-damage", true),
                p -> teleport(p, bypassCooldown),
                p -> send(p, "teleport-cancelled", "<red>Téléportation annulée, tu as bougé.</red>"));
    }

    private void teleport(Player player, boolean bypassCooldown) {
        Location spawn = spawns.getSpawn();
        if (spawn == null) {
            send(player, "world-missing", "<red>Le monde du spawn est introuvable.</red>");
            return;
        }

        // teleportAsync : le chunk de destination se charge sans bloquer le thread principal
        player.teleportAsync(spawn).thenAccept(success -> runSync(() -> {
            if (!success || !player.isOnline()) return;
            send(player, "teleported", "<green>Tu as été téléporté au spawn.</green>");
            if (!bypassCooldown) warmup.setCooldown(player.getUniqueId(), "spawn", cfg().getInt("cooldown-seconds", 0));
        }));
    }

    private void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(plugin, task);
    }
}
