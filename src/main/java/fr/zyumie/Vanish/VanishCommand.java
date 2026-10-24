package fr.zyumie.Vanish;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final Main plugin;

    public VanishCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSeul un joueur peut utiliser cette commande.");
            return true;
        }

        VanishManager vm = plugin.getVanishManager();

        // ── Cible ────────────────────────────────────────────────────────────
        Player target = player;

        if (args.length >= 1) {
            if (!player.hasPermission("fireballwand.vanish.others")) {
                player.sendMessage(msg("vanish.messages.no-permission-other", "&cTu n'as pas la permission de vanisher d'autres joueurs.")
                        .replace("%player%", args[0]));
                return true;
            }
            target = plugin.getServer().getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage(msg("vanish.messages.player-not-found", "&cJoueur introuvable : &f%player%")
                        .replace("%player%", args[0]));
                return true;
            }
        }

        // ── Permission de base ───────────────────────────────────────────────
        if (!vm.canVanish(target)) {
            String key = target.equals(player) ? "vanish.messages.no-permission" : "vanish.messages.no-permission-other";
            player.sendMessage(msg(key, "&cPas de permission.").replace("%player%", target.getName()));
            return true;
        }

        // ── Toggle ───────────────────────────────────────────────────────────
        if (!vm.isVanished(target)) {
            vm.enableVanish(target);
            int lvl = vm.getVanishLevel(target);

            player.sendMessage(msg("vanish.messages.enable", "&aVanish activé. &7(niveau &f%level%&7)")
                    .replace("%level%", String.valueOf(lvl)));

            if (!target.equals(player))
                target.sendMessage(msg("vanish.messages.enable-other", "&aTon vanish a été activé par &f%player%&a.")
                        .replace("%player%", player.getName()));
        } else {
            vm.disableVanish(target);

            player.sendMessage(msg("vanish.messages.disable", "&cVanish désactivé."));

            if (!target.equals(player))
                target.sendMessage(msg("vanish.messages.disable-other", "&cTon vanish a été désactivé par &f%player%&c.")
                        .replace("%player%", player.getName()));
        }

        return true;
    }

    /** Récupère un message de la config et le colorise. */
    private String msg(String path, String fallback) {
        String raw = plugin.getConfig().getString(path, fallback);
        return ChatManager.colorize(raw);
    }
}
