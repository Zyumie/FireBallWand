package fr.zyumie.TPA;

import fr.zyumie.TPA.Listener.TpaRequest;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public TpaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "tpa":
                return handleTpa(sender, args, false);
            case "tpahere":
                return handleTpa(sender, args, true);
            case "tpaccept":
            case "tpyes":
                return handleTpAccept(sender, args);
            case "tpdeny":
            case "tpno":
                return handleTpDeny(sender, args);
            case "tpacancel":
                return handleTpaCancel(sender);
            case "tpareload":
                return handleTpaReload(sender);
            default:
                return false;
        }
    }

    private boolean handleTpa(CommandSender sender, String[] args, boolean tpHere) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TpaMessageUtil.colorize(plugin.getTpaConfigManager().getMessage("only-players", "&cSeuls les joueurs.")));
            return true;
        }

        String perm = tpHere ? "fireballwand.tpahere" : "fireballwand.tpa";
        if (!player.hasPermission(perm)) {
            TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("no-permission", "&cPas la permission."));
            return true;
        }

        if (args.length < 1) {
            String usageKey = tpHere ? "tpahere-usage" : "tpa-usage";
            String defaultUsage = tpHere ? "&cUtilisation : /tpahere <joueur>" : "&cUtilisation : /tpa <joueur>";
            TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage(usageKey, defaultUsage));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("player-not-found", "&cJoueur introuvable."));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("cannot-tpa-self", "&cImpossible sur soi-même."));
            return true;
        }

        if (!tpHere && !player.hasPermission("fireballwand.tpa.bypass.cooldown")) {
            long remaining = plugin.getTpaManager().getRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("tpa-cooldown", "&cAttendez {time}.").replace("{time}", TpaMessageUtil.formatDuration(remaining)));
                return true;
            }
        }

        if (plugin.getTpaManager().hasPendingRequestFrom(target.getUniqueId(), player.getUniqueId())) {
            TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("already-pending", "&cDéjà une demande."));
            return true;
        }

        plugin.getTpaManager().sendRequest(player, target, tpHere);
        return true;
    }

    private boolean handleTpAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player targetPlayer)) return true;
        if (!targetPlayer.hasPermission("fireballwand.tpa")) {
            TpaMessageUtil.sendMessage(targetPlayer, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("no-permission", "&cPas la permission."));
            return true;
        }

        TpaRequest request = null;
        if (args.length > 0) {
            Player specific = Bukkit.getPlayerExact(args[0]);
            if (specific != null) {
                request = plugin.getTpaManager().getRequestFrom(targetPlayer.getUniqueId(), specific.getUniqueId());
            }
        } else {
            request = plugin.getTpaManager().getLatestRequest(targetPlayer.getUniqueId());
        }

        if (request == null) {
            TpaMessageUtil.sendMessage(targetPlayer, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("no-pending-requests", "&cAucune demande."));
            return true;
        }

        Player requester = Bukkit.getPlayer(request.getSenderId());
        if (requester == null || !requester.isOnline()) {
            plugin.getTpaManager().removeRequest(targetPlayer.getUniqueId(), request.getSenderId());
            return true;
        }

        plugin.getTpaManager().removeRequest(targetPlayer.getUniqueId(), requester.getUniqueId());

        TpaMessageUtil.sendMessage(targetPlayer, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("request-accepted-target", "&aAccepté !").replace("{sender}", requester.getName()));
        TpaMessageUtil.sendMessage(requester, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("request-accepted-sender", "&aAccepté !").replace("{target}", targetPlayer.getName()));

        if (request.isTpHere()) {
            plugin.getTpaTeleportManager().startWarmup(targetPlayer, requester);
        } else {
            plugin.getTpaTeleportManager().startWarmup(requester, targetPlayer);
        }
        return true;
    }

    private boolean handleTpDeny(CommandSender sender, String[] args) {
        if (!(sender instanceof Player targetPlayer)) return true;
        if (!targetPlayer.hasPermission("fireballwand.tpa")) {
            TpaMessageUtil.sendMessage(targetPlayer, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("no-permission", "&cPas la permission."));
            return true;
        }

        TpaRequest request = null;
        if (args.length > 0) {
            Player specific = Bukkit.getPlayerExact(args[0]);
            if (specific != null) {
                request = plugin.getTpaManager().getRequestFrom(targetPlayer.getUniqueId(), specific.getUniqueId());
            }
        } else {
            request = plugin.getTpaManager().getLatestRequest(targetPlayer.getUniqueId());
        }

        if (request == null) {
            TpaMessageUtil.sendMessage(targetPlayer, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("no-pending-requests", "&cAucune demande."));
            return true;
        }

        Player requester = Bukkit.getPlayer(request.getSenderId());
        plugin.getTpaManager().removeRequest(targetPlayer.getUniqueId(), request.getSenderId());
        TpaMessageUtil.sendMessage(targetPlayer, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("request-denied-target", "&cRefusé."));

        if (requester != null && requester.isOnline()) {
            TpaMessageUtil.sendMessage(requester, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("request-denied-sender", "&cRefusé par la cible."));
        }
        return true;
    }

    private boolean handleTpaCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("fireballwand.tpa")) {
            TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("no-permission", "&cPas la permission."));
            return true;
        }

        UUID targetId = plugin.getTpaManager().cancelOutgoingRequest(player.getUniqueId());
        if (targetId == null) {
            TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("no-request-to-cancel", "&cAucune demande à annuler."));
            return true;
        }

        TpaMessageUtil.sendMessage(player, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("request-cancelled", "&eDemande annulée."));
        return true;
    }

    private boolean handleTpaReload(CommandSender sender) {
        if (!sender.hasPermission("fireballwand.tpa.admin")) return true;

        plugin.getTpaConfigManager().loadConfig();
        plugin.getTpaManager().loadCooldowns();
        TpaMessageUtil.sendMessage(sender, plugin.getTpaConfigManager().getPrefix(), plugin.getTpaConfigManager().getMessage("config-reloaded", "&aRechargé !"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();
        if ((cmdName.equals("tpa") || cmdName.equals("tpahere") || cmdName.equals("tpaccept") || cmdName.equals("tpdeny")) && args.length == 1) {
            List<String> completions = new ArrayList<>();
            String current = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player pl && p.getUniqueId().equals(pl.getUniqueId())) continue;
                if (p.getName().toLowerCase().startsWith(current)) completions.add(p.getName());
            }
            return completions;
        }
        return Collections.emptyList();
    }
}