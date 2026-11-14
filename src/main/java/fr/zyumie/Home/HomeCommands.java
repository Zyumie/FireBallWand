package fr.zyumie.Home;

import fr.zyumie.Chat.Messages;
import fr.zyumie.Teleport.WarmupService;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /home [nom|list], /sethome [nom], /delhome [nom].
 *
 * Permissions : fireballwand.home.use / .set / .delete (par défaut pour tous).
 * Les anciennes permissions home.command.home / sethome / delhome restent acceptées.
 */
public final class HomeCommands implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final HomeManager manager;
    private final WarmupService warmup;

    public HomeCommands(Main plugin, HomeManager manager, WarmupService warmup) {
        this.plugin = plugin;
        this.manager = manager;
        this.warmup = warmup;
    }

    private FileConfiguration cfg() {
        return plugin.getHomeConfig();
    }

    /** Envoie un message de Home.yml ; pairs = placeholder1, valeur1, placeholder2, valeur2... */
    private void send(CommandSender to, String key, String def, String... pairs) {
        String message = cfg().getString("messages." + key, def);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            message = message.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        Messages.send(to, cfg().getString("messages.prefix", ""), message);
    }

    private static boolean has(Player player, String permission, String legacyPermission) {
        return player.hasPermission(permission) || player.hasPermission(legacyPermission);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "players-only", "<red>Commande réservée aux joueurs.");
            return true;
        }

        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "sethome" -> handleSetHome(player, args);
            case "home" -> handleHome(player, args);
            case "delhome" -> handleDelHome(player, args);
            default -> { return false; }
        }
        return true;
    }

    // ── /sethome ─────────────────────────────────────────────────────────────

    private void handleSetHome(Player player, String[] args) {
        if (!has(player, "fireballwand.home.set", "home.command.sethome")) {
            send(player, "no-permission", "<red>Tu n'as pas la permission d'effectuer cette action.");
            return;
        }

        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "default";
        if (!isValidName(name)) {
            send(player, "invalid-name", "<red>Nom invalide : lettres, chiffres, - et _ uniquement (max {max}).",
                    "max", String.valueOf(maxNameLength()));
            return;
        }

        UUID id = player.getUniqueId();
        int max = manager.getMaxHomes(player);
        if (!manager.hasHome(id, name) && manager.getHomeCount(id) >= max) {
            send(player, "max-limit", "<red>Tu as atteint ton nombre maximum de homes autorisés (<yellow>{max}<red>).",
                    "max", String.valueOf(max));
            return;
        }

        manager.setHome(id, name, player.getLocation());
        send(player, "set-success", "<green>Home <yellow>{home} <green>défini avec succès !", "home", name);
    }

    // ── /home ────────────────────────────────────────────────────────────────

    private void handleHome(Player player, String[] args) {
        if (!has(player, "fireballwand.home.use", "home.command.home")) {
            send(player, "no-permission", "<red>Tu n'as pas la permission d'effectuer cette action.");
            return;
        }

        UUID id = player.getUniqueId();

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            if (manager.getHomeCount(id) == 0) {
                send(player, "tp-none", "<red>Aucun home défini. Utilise <yellow>/sethome [nom]<red> pour en créer un.");
                return;
            }
            new HomeGui(plugin, manager, player).open();
            return;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        if (!manager.hasHome(id, name)) {
            send(player, "tp-none-named", "<red>Le home <yellow>{home} <red>n'existe pas.", "home", name);
            return;
        }
        initiateTeleport(player, name);
    }

    /** Lance la téléportation vers un home (cooldown, délai d'attente, annulation si on bouge). */
    public void initiateTeleport(Player player, String name) {
        UUID id = player.getUniqueId();
        if (!manager.hasHome(id, name)) return;

        boolean bypassCooldown = player.hasPermission("fireballwand.home.bypass.cooldown");
        boolean bypassWarmup = player.hasPermission("fireballwand.home.bypass.warmup");

        if (warmup.isPending(player)) {
            send(player, "already-pending", "<red>Une téléportation est déjà en cours.");
            return;
        }
        if (!bypassCooldown) {
            long left = warmup.cooldownLeft(id, "home");
            if (left > 0) {
                send(player, "cooldown", "<red>Patiente encore <yellow>{seconds}s <red>avant de te téléporter à nouveau.",
                        "seconds", String.valueOf(left));
                return;
            }
        }

        int delay = bypassWarmup ? 0 : Math.max(0, cfg().getInt("teleport-delay-seconds", 20));
        if (delay > 0) {
            send(player, "tp-delay-start", "<green>Téléportation dans <yellow>{seconds} secondes<green>. Ne bouge pas !",
                    "seconds", String.valueOf(delay));
        }

        warmup.start(player, delay,
                cfg().getBoolean("cancel-on-move", true),
                cfg().getBoolean("cancel-on-damage", true),
                p -> teleport(p, name, bypassCooldown),
                p -> send(p, "tp-cancelled", "<red>Téléportation annulée : tu as bougé ou subi des dégâts."));
    }

    private void teleport(Player player, String name, boolean bypassCooldown) {
        HomeData data = manager.getHome(player.getUniqueId(), name);
        Location target = data == null ? null : data.toLocation();
        if (target == null) {
            send(player, "world-missing", "<red>Le monde de ce home est introuvable.");
            return;
        }

        // teleportAsync : le chunk de destination se charge sans bloquer le thread principal
        player.teleportAsync(target).thenAccept(success -> runSync(() -> {
            if (!success || !player.isOnline()) return;
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            send(player, "tp-success", "<green>Téléportation au home effectuée !");
            if (!bypassCooldown) warmup.setCooldown(player.getUniqueId(), "home", cfg().getInt("cooldown-seconds", 0));
        }));
    }

    private void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(plugin, task);
    }

    // ── /delhome ─────────────────────────────────────────────────────────────

    private void handleDelHome(Player player, String[] args) {
        if (!has(player, "fireballwand.home.delete", "home.command.delhome")) {
            send(player, "no-permission", "<red>Tu n'as pas la permission d'effectuer cette action.");
            return;
        }

        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "default";
        if (!manager.hasHome(player.getUniqueId(), name)) {
            send(player, "del-none", "<red>Tu n'as pas de home défini avec ce nom.");
            return;
        }

        manager.deleteHome(player.getUniqueId(), name);
        send(player, "del-success", "<green>Home <yellow>{home} <green>supprimé.", "home", name);
    }

    /** Utilisé par le menu après une suppression. */
    public void sendDeleted(Player player, String name) {
        send(player, "del-success", "<green>Home <yellow>{home} <green>supprimé.", "home", name);
    }

    public void sendNoPermission(Player player) {
        send(player, "no-permission", "<red>Tu n'as pas la permission d'effectuer cette action.");
    }

    public void sendEmptySlotHint(Player player) {
        send(player, "gui-empty-slot", "<yellow>Utilise la commande <white>/sethome <nom> <yellow>pour enregistrer un home dans un emplacement libre.");
    }

    // ── Noms ─────────────────────────────────────────────────────────────────

    private int maxNameLength() {
        return Math.max(1, Math.min(32, cfg().getInt("max-name-length", 16)));
    }

    private boolean isValidName(String name) {
        if (name.isEmpty() || name.length() > maxNameLength()) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
            if (!ok) return false;
        }
        return true;
    }

    // ── Auto-complétion ──────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return List.of();

        String name = cmd.getName().toLowerCase(Locale.ROOT);
        if (!name.equals("home") && !name.equals("delhome")) return List.of();

        List<String> out = new ArrayList<>(manager.getHomes(player.getUniqueId()).keySet());
        if (name.equals("home")) out.add("list");

        String prefix = args[0].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.startsWith(prefix));
        return out;
    }
}
