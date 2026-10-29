package fr.zyumie.Vanish;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.Vanish.Listener.VanishManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
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
        boolean fakeMsgEnabled = plugin.getVanishConfig().getBoolean("fake-join-quit-messages.enabled", true);

        if (!vm.isVanished(target)) {
            vm.enableVanish(target);
            int lvl = vm.getVanishLevel(target);

            player.sendMessage(msg("vanish.messages.enable", "&aVanish activé. &7(niveau &f%level%&7)")
                    .replace("%level%", String.valueOf(lvl)));

            if (!target.equals(player))
                target.sendMessage(msg("vanish.messages.enable-other", "&aTon vanish a été activé par &f%player%&a.")
                        .replace("%player%", player.getName()));

            // Simuler un message de QUIT (déconnexion) dans le chat
            if (fakeMsgEnabled) {
                String quitRaw = plugin.getVanishConfig().getString("fake-join-quit-messages.quit", "&e%player% left the game.");
                String quitMsg = ChatManager.colorize(quitRaw.replace("%player%", target.getName()));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    // On l'envoie uniquement à ceux qui peuvent le voir se faire "déconnecter" (ou à tout le monde selon la logique)
                    p.sendMessage(quitMsg);
                }
            }

        } else {
            vm.disableVanish(target);

            player.sendMessage(msg("vanish.messages.disable", "&cVanish désactivé."));

            if (!target.equals(player))
                target.sendMessage(msg("vanish.messages.disable-other", "&cTon vanish a été désactivé par &f%player%&c.")
                        .replace("%player%", player.getName()));

            // Simuler un message de JOIN (connexion) dans le chat
            if (fakeMsgEnabled) {
                String joinRaw = plugin.getVanishConfig().getString("fake-join-quit-messages.join", "&e%player% joined the game.");
                String joinMsg = ChatManager.colorize(joinRaw.replace("%player%", target.getName()));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(joinMsg);
                }
            }
        }

        return true;
    }

    /** Récupère un message de la config et le colorise. */
    private String msg(String path, String fallback) {
        String raw = plugin.getVanishConfig().getString(path, fallback);
        return ChatManager.colorize(raw);
    }
}