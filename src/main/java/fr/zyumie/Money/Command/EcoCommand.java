package fr.zyumie.Money.Command;

import fr.zyumie.Money.MoneyConfig;
import fr.zyumie.Money.MoneyModule;
import fr.zyumie.Money.Service.MoneyService;
import fr.zyumie.Money.Util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class EcoCommand implements CommandExecutor, TabCompleter {

    private final MoneyModule module;

    public EcoCommand(MoneyModule module) { this.module = module; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!module.isEnabled()) {
            Msg.send(sender, "<red>Module Money désactivé.");
            return true;
        }
        if (!sender.hasPermission("fireballwand.money.admin")) {
            send(sender, module.config().msg("no-permission"));
            return true;
        }
        if (args.length < 3) {
            Msg.send(sender, "<red>Usage: /eco <give|set|take> <joueur> <montant>");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String targetName = args[1];
        double amount;
        try { amount = Double.parseDouble(args[2].replace(",", ".")); }
        catch (NumberFormatException e) {
            send(sender, module.config().msg("invalid-amount"));
            return true;
        }
        if (!MoneyService.isValidAmount(amount)) {
            send(sender, module.config().msg("invalid-amount"));
            return true;
        }

        OfflinePlayer target = Bukkit.getPlayer(targetName);
        if (target == null) target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) {
            send(sender, module.config().msg("player-not-found").replace("{player}", targetName));
            return true;
        }

        MoneyConfig cfg = module.config();
        MoneyService service = module.service();
        String displayName = target.getName() != null ? target.getName() : targetName;
        String plural = cfg.currencyPlural();
        String amountStr = String.format(Locale.ROOT, "%.2f", amount);

        switch (action) {
            case "give" -> {
                if (!service.deposit(target.getUniqueId(), amount)) {
                    Msg.send(sender, "<red>Refusé : plafond de solde dépassé.");
                    return true;
                }
                send(sender, cfg.msg("eco-give")
                        .replace("{amount}", amountStr)
                        .replace("{player}", displayName)
                        .replace("{currency}", plural));
            }
            case "take" -> {
                if (!service.withdraw(target.getUniqueId(), amount)) {
                    Msg.send(sender, "<red>Refusé : solde insuffisant chez " + displayName + ".");
                    return true;
                }
                send(sender, cfg.msg("eco-take")
                        .replace("{amount}", amountStr)
                        .replace("{player}", displayName)
                        .replace("{currency}", plural));
            }
            case "set" -> {
                service.set(target.getUniqueId(), amount);
                send(sender, cfg.msg("eco-set")
                        .replace("{amount}", amountStr)
                        .replace("{player}", displayName)
                        .replace("{currency}", plural));
            }
            default -> Msg.send(sender,
                    "<red>Action inconnue. Usage: /eco <give|set|take> <joueur> <montant>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!module.isEnabled()) return List.of();
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            return List.of("give", "set", "take").stream()
                    .filter(s -> s.startsWith(p)).toList();
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        return List.of();
    }

    private void send(CommandSender s, String message) {
        Msg.send(s, module.config().prefix() + message);
    }
}