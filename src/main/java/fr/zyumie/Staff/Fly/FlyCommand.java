package fr.zyumie.Staff.Fly;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /fly                      → active/désactive son vol            (fireballwand.fly)
 * /fly <joueur>             → idem pour un autre joueur           (fireballwand.fly.others)
 * /fly speed <1-max> [joueur] → règle la vitesse de vol           (fireballwand.fly.speed)
 */
public class FlyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public FlyCommand(Main plugin) {
        this.plugin = plugin;
    }

    private FlyManager fly() {
        return plugin.getFlyManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("speed")) {
            return handleSpeed(sender, args);
        }

        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage(usage());
                return true;
            }
            if (!self.hasPermission("fireballwand.fly")) {
                sender.sendMessage(fly().msg("no-permission", "&cTu n'as pas la permission."));
                return true;
            }
            target = self;
        } else {
            if (!sender.hasPermission("fireballwand.fly.others")) {
                sender.sendMessage(fly().msg("no-permission", "&cTu n'as pas la permission."));
                return true;
            }
            target = plugin.getVanishManager().findVisiblePlayerExact(sender, args[0]);
            if (target == null) {
                sender.sendMessage(fly().msg("player-not-found", "&cJoueur introuvable : &f%player%")
                        .replace("%player%", args[0]));
                return true;
            }
        }

        Boolean state = fly().toggle(target);
        if (state == null) {
            sender.sendMessage(fly().msg("native", "&7Le vol est déjà actif dans ce mode de jeu."));
            return true;
        }

        String own = state ? fly().msg("enable", "&aVol activé.") : fly().msg("disable", "&cVol désactivé.");

        if (target.equals(sender)) {
            sender.sendMessage(own);
        } else {
            sender.sendMessage((state
                    ? fly().msg("other-enable", "&aVol activé pour &f%player%&a.")
                    : fly().msg("other-disable", "&cVol désactivé pour &f%player%&c."))
                    .replace("%player%", target.getName()));
            target.sendMessage(own);
        }
        return true;
    }

    private boolean handleSpeed(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fireballwand.fly.speed")) {
            sender.sendMessage(fly().msg("no-permission", "&cTu n'as pas la permission."));
            return true;
        }

        int max = fly().getMaxSpeed();

        if (args.length < 2) {
            sender.sendMessage(usage());
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(speedInvalid(max));
            return true;
        }
        if (level < 1 || level > max) {
            sender.sendMessage(speedInvalid(max));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission("fireballwand.fly.others")) {
                sender.sendMessage(fly().msg("no-permission", "&cTu n'as pas la permission."));
                return true;
            }
            target = plugin.getVanishManager().findVisiblePlayerExact(sender, args[2]);
            if (target == null) {
                sender.sendMessage(fly().msg("player-not-found", "&cJoueur introuvable : &f%player%")
                        .replace("%player%", args[2]));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(usage());
            return true;
        }

        fly().setSpeed(target, level);

        String speedMsg = fly().msg("speed", "&aVitesse de vol : &f%speed%&a.")
                .replace("%speed%", String.valueOf(level));
        target.sendMessage(speedMsg);
        if (!target.equals(sender)) {
            sender.sendMessage(speedMsg + " §7(" + target.getName() + ")");
        }
        return true;
    }

    private String usage() {
        return fly().msg("usage", "&cUsage : /fly [joueur] | /fly speed <1-%max%> [joueur]")
                .replace("%max%", String.valueOf(fly().getMaxSpeed()));
    }

    private String speedInvalid(int max) {
        return fly().msg("speed-invalid", "&cVitesse invalide (1-%max%).")
                .replace("%max%", String.valueOf(max));
    }

    // ── Tab completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        boolean speedSub = args.length >= 2 && args[0].equalsIgnoreCase("speed");

        if (args.length == 1) {
            if (sender.hasPermission("fireballwand.fly.speed")) out.add("speed");
            if (sender.hasPermission("fireballwand.fly.others")) addVisiblePlayers(sender, out);
        } else if (args.length == 2 && speedSub) {
            for (int i = 1; i <= fly().getMaxSpeed(); i++) out.add(String.valueOf(i));
        } else if (args.length == 3 && speedSub && sender.hasPermission("fireballwand.fly.others")) {
            addVisiblePlayers(sender, out);
        }

        String prefix = args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        return out;
    }

    /** N'expose pas les joueurs vanishés que l'expéditeur ne peut pas voir. */
    private void addVisiblePlayers(CommandSender sender, List<String> out) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player viewer && !viewer.canSee(p)) continue;
            out.add(p.getName());
        }
    }
}
