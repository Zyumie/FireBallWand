package fr.zyumie.Staff.GodMod;

import fr.zyumie.Staff.StaffManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Gère /godmod et /ungodmod (même exécuteur, distingué par le nom de la commande). */
public class GodCommand implements CommandExecutor {

    private final Main plugin;

    public GodCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSeul un joueur peut utiliser cette commande.");
            return true;
        }

        StaffManager staff = plugin.getStaffManager();
        GodManager god = plugin.getGodManager();

        if (!player.hasPermission("fireballwand.god")) {
            player.sendMessage(staff.msg("no-permission", "&cTu n'as pas la permission."));
            return true;
        }

        if (command.getName().equalsIgnoreCase("godmod")) {
            player.sendMessage(god.enable(player)
                    ? staff.msg("god-enable", "&aGodmod activé.")
                    : staff.msg("god-already-on", "&7Tu es déjà en godmod."));
        } else {
            player.sendMessage(god.disable(player)
                    ? staff.msg("god-disable", "&cGodmod désactivé.")
                    : staff.msg("god-already-off", "&7Tu n'es pas en godmod."));
        }
        return true;
    }
}
