package fr.zyumie.Chat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MsgCommand implements CommandExecutor {

    private final MsgManager msgManager;

    public MsgCommand(MsgManager msgManager) {
        this.msgManager = msgManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        //  /r  ⬇️⬇️
        if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {

            UUID targetUUID = msgManager.getLastTarget(player);
            if (targetUUID == null) {
                player.sendMessage(MsgManager.colorize("&cVous n'avez personne à qui répondre."));
                return true;
            }

            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null || !target.isOnline()) {
                player.sendMessage(MsgManager.colorize("&cLa personne à qui vous voulez répondre n'est plus en ligne."));
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(MsgManager.colorize("&cUsage: /r <message>"));
                return true;
            }

            String message = String.join(" ", args);
            msgManager.sendPrivateMessage(player, target, message);
            return true;
        }

        //  /Msg ⬇️⬇️

        if (args.length < 2) {
            player.sendMessage(MsgManager.colorize("&cUsage: /msg <joueur> <message>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MsgManager.colorize("&cJoueur introuvable ou hors-ligne."));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(MsgManager.colorize("&cVous ne pouvez pas vous envoyer un message à vous-même."));
            return true;
        }

        // Recomposer le message à partir du 2ème argument (args[1])
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String message = sb.toString().trim();

        // Envoyer le message
        msgManager.sendPrivateMessage(player, target, message);

        return true;
    }
}