package fr.zyumie.Vanish;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.Vanish.Listener.VanishManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VanishCommand implements CommandExecutor {

    private final Main plugin;

    public VanishCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        VanishManager vm = plugin.getVanishManager();

        // ── GESTION DE LA SOUS-COMMANDE : /vanish list ────────────────────────
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("fireballwand.vanish.list") && !sender.hasPermission("fireballwand.admin")) {
                sender.sendMessage(ChatManager.colorize("&cTu n'as pas la permission d'utiliser cette commande."));
                return true;
            }

            // Hiérarchie : on ne liste que les vanishés que l'expéditeur a le niveau de voir
            List<Player> visible = new ArrayList<>();
            for (UUID uuid : vm.getVanishedUUIDs()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && vm.canSee(sender, p)) visible.add(p);
            }

            if (visible.isEmpty()) {
                sender.sendMessage(ChatManager.colorize("&7[Vanish] &cAucun joueur n'est actuellement en vanish."));
                return true;
            }

            sender.sendMessage(ChatManager.colorize("&8--- &6Joueurs en Vanish (&e" + visible.size() + "&6) &8---"));
            for (Player p : visible) {
                int level = vm.getVanishLevel(p);
                sender.sendMessage(ChatManager.colorize("&7- &f" + p.getName() + " &8(Niveau: &e" + level + "&8)"));
            }
            return true;
        }

        // ── VÉRIFICATION JOUEUR POUR LE TOGGLE CLASSIQUE ─────────────────────
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSeul un joueur peut utiliser cette commande.");
            return true;
        }

        // ── Cible ────────────────────────────────────────────────────────────
        Player target = player;

        if (args.length >= 1) {
            if (!player.hasPermission("fireballwand.vanish.others")) {
                player.sendMessage(msg("messages.no-permission-other", "&cTu n'as pas la permission de vanisher d'autres joueurs.")
                        .replace("%player%", args[0]));
                return true;
            }
            target = plugin.getServer().getPlayerExact(args[0]);
            // Un vanishé de niveau supérieur doit rester introuvable
            if (target != null && !vm.canSee(player, target)) target = null;
            if (target == null) {
                player.sendMessage(msg("messages.player-not-found", "&cJoueur introuvable : &f%player%")
                        .replace("%player%", args[0]));
                return true;
            }
        }

        // ── Permission de base ───────────────────────────────────────────────
        if (!vm.canVanish(target)) {
            String key = target.equals(player) ? "messages.no-permission" : "messages.no-permission-other";
            player.sendMessage(msg(key, "&cPas de permission.").replace("%player%", target.getName()));
            return true;
        }

        // ── Toggle ───────────────────────────────────────────────────────────
        if (!vm.isVanished(target)) {
            vm.enableVanish(target);
            int lvl = vm.getVanishLevel(target);

            player.sendMessage(msg("messages.enable", "&aVanish activé. &7(niveau &f%level%&7)")
                    .replace("%level%", String.valueOf(lvl)));

            if (!target.equals(player))
                target.sendMessage(msg("messages.enable-other", "&aTon vanish a été activé par &f%player%&a.")
                        .replace("%player%", player.getName()));

        } else {
            vm.disableVanish(target);

            player.sendMessage(msg("messages.disable", "&cVanish désactivé."));

            if (!target.equals(player))
                target.sendMessage(msg("messages.disable-other", "&cTon vanish a été désactivé par &f%player%&c.")
                        .replace("%player%", player.getName()));
        }

        return true;
    }

    /** Récupère un message de la config et le colorise. */
    private String msg(String path, String fallback) {
        String raw = plugin.getVanishConfig().getString(path, fallback);
        return ChatManager.colorize(raw);
    }
}