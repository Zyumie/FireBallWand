package fr.zyumie.Money.Command;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final MoneyModule module;

    public MoneyCommand(MoneyModule module) { this.module = module; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!module.isEnabled()) {
            Msg.send(sender, "<red>Module Money désactivé (voir config.yml → modules.money.enabled).");
            return true;
        }

        // /money reload → recharge uniquement Money.yml
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("fireballwand.money.admin")) {
                send(sender, module.config().msg("no-permission"));
                return true;
            }
            module.config().reload();
            send(sender, module.config().msg("reload-success"));
            return true;
        }

        MoneyService service = module.service();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /money <joueur>");
                return true;
            }
            double bal = service.getBalance(player.getUniqueId());
            send(sender, module.config().msg("balance")
                    .replace("{amount}", fmt(bal))
                    .replace("{currency}", bal == 1.0
                            ? module.config().currencySingular()
                            : module.config().currencyPlural()));
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
            if (target == null) {
                send(sender, module.config().msg("player-not-found").replace("{player}", args[0]));
                return true;
            }
            double bal = service.getBalance(target.getUniqueId());
            send(sender, module.config().msg("balance-other")
                    .replace("{player}", target.getName() != null ? target.getName() : args[0])
                    .replace("{amount}", fmt(bal))
                    .replace("{currency}", bal == 1.0
                            ? module.config().currencySingular()
                            : module.config().currencyPlural()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!module.isEnabled()) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            if (sender.hasPermission("fireballwand.money.admin") && "reload".startsWith(prefix)) {
                suggestions.add("reload");
            }
            Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .forEach(suggestions::add);
            return suggestions;
        }
        return List.of();
    }

    private String fmt(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    private void send(CommandSender s, String message) {
        Msg.send(s, module.config().prefix() + message);
    }
}