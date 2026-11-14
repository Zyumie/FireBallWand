package fr.zyumie.Commandes;

import fr.zyumie.fireballwand.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FireballWandCommand implements CommandExecutor {

    private final Main plugin;

    public FireballWandCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fireballwand.admin")) {
            sender.sendMessage("§c❌ Tu n'as pas la permission d'exécuter cette commande.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Recharge config.yml, Vanish.yml, CustomCheat.yml, StartKit.yml, Staff.yml et Tpa.yml
            plugin.reloadAll();

            sender.sendMessage("§a[FireballWand] §e✔ Toutes les configurations ont été rechargées avec succès !");
            return true;
        }

        sender.sendMessage("§8[§cFireballWand§8] §7Utilisation : §e/fw reload");
        return true;
    }
}