package fr.zyumie.Staff;

import fr.zyumie.fireballwand.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StaffCommand implements CommandExecutor {

    private final Main plugin;

    public StaffCommand(Main plugin) {
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

        if (!player.hasPermission("fireballwand.staff")) {
            player.sendMessage(staff.msg("no-permission", "&cTu n'as pas la permission."));
            return true;
        }

        staff.toggle(player);
        return true;
    }
}
