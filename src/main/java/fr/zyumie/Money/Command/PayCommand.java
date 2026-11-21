package fr.zyumie.Money.Command;

import fr.zyumie.Money.MoneyConfig;
import fr.zyumie.Money.MoneyModule;
import fr.zyumie.Money.Service.MoneyService;
import fr.zyumie.Money.Util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final MoneyModule module;

    public PayCommand(MoneyModule module) { this.module = module; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!module.isEnabled()) {
            Msg.send(sender, "<red>Module Money désactivé.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (!player.hasPermission("fireballwand.money.pay")) {
            send(player, module.config().msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            Msg.send(player, "<red>Usage: /pay <joueur> <montant>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(player, module.config().msg("player-not-found").replace("{player}", args[0]));
            return true;
        }
        if (target.equals(player)) {
            send(player, module.config().msg("pay-self"));
            return true;
        }

        double amount;
        try { amount = Double.parseDouble(args[1].replace(",", ".")); }
        catch (NumberFormatException e) {
            send(player, module.config().msg("invalid-amount"));
            return true;
        }
        if (!MoneyService.isValidAmount(amount)) {
            send(player, module.config().msg("invalid-amount"));
            return true;
        }

        MoneyConfig cfg = module.config();
        MoneyService service = module.service();
        double min = cfg.minTransaction();
        double max = cfg.maxBalance();
        String plural = cfg.currencyPlural();

        if (amount < min) {
            send(player, cfg.msg("pay-min")
                    .replace("{min}", fmt(min)).replace("{currency}", plural));
            return true;
        }

        double senderBalance = service.getBalance(player.getUniqueId());
        if (senderBalance < amount) {
            send(player, cfg.msg("pay-insufficient")
                    .replace("{balance}", fmt(senderBalance)).replace("{currency}", plural));
            return true;
        }

        double targetBalance = service.getBalance(target.getUniqueId());
        if (targetBalance + amount > max) {
            send(player, cfg.msg("pay-max-balance")
                    .replace("{max}", fmt(max)).replace("{currency}", plural));
            return true;
        }

        if (!service.withdraw(player.getUniqueId(), amount)) {
            send(player, cfg.msg("pay-insufficient")
                    .replace("{balance}", fmt(service.getBalance(player.getUniqueId())))
                    .replace("{currency}", plural));
            return true;
        }
        if (!service.deposit(target.getUniqueId(), amount)) {
            service.deposit(player.getUniqueId(), amount); // rembourse
            send(player, cfg.msg("pay-max-balance")
                    .replace("{max}", fmt(max)).replace("{currency}", plural));
            return true;
        }

        String amountStr = fmt(amount);
        send(player, cfg.msg("pay-success")
                .replace("{amount}", amountStr)
                .replace("{player}", target.getName())
                .replace("{currency}", plural));
        send(target, cfg.msg("pay-received")
                .replace("{amount}", amountStr)
                .replace("{player}", player.getName())
                .replace("{currency}", plural));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!module.isEnabled()) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        return List.of();
    }

    private String fmt(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    private void send(CommandSender s, String message) {
        Msg.send(s, module.config().prefix() + message);
    }
}