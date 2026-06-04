package fr.zyumie.Vanish;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import fr.zyumie.Vanish.*;
import fr.zyumie.fireballwand.Main;

/**
 * Commande /vanish
 *
 * Usage :
 *   /vanish          → toggle vanish sur soi-même
 *   /vanish <joueur> → toggle vanish sur un autre joueur (nécessite .others)
 */
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

        // --- Cible ---
        Player target = player;

        if (args.length >= 1) {
            if (!player.hasPermission("fireballwand.vanish.others")) {
                player.sendMessage("§cTu n'as pas la permission de vanisher d'autres joueurs.");
                return true;
            }
            target = plugin.getServer().getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage("§cJoueur introuvable : §f" + args[0]);
                return true;
            }
        }

        // --- Permission de base ---
        if (!vm.canVanish(target)) {
            player.sendMessage("§c" + (target.equals(player)
                    ? "Tu n'as pas la permission d'utiliser le vanish."
                    : target.getName() + " n'a pas la permission d'utiliser le vanish."));
            return true;
        }

        // --- Toggle ---
        String who = target.equals(player) ? "Vanish" : "Vanish de §f" + target.getName();

        if (!vm.isVanished(target)) {
            vm.enableVanish(target);
            int lvl = vm.getVanishLevel(target);
            player.sendMessage("§a" + who + " §aactivé. §7(niveau §f" + lvl + "§7)");
            if (!target.equals(player))
                target.sendMessage("§aTon vanish a été activé par §f" + player.getName() + "§a.");
        } else {
            vm.disableVanish(target);
            player.sendMessage("§c" + who + " §cdésactivé.");
            if (!target.equals(player))
                target.sendMessage("§cTon vanish a été désactivé par §f" + player.getName() + "§c.");
        }

        return true;
    }
}