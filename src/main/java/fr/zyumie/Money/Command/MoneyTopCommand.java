package fr.zyumie.Money.Command;

import fr.zyumie.Money.MoneyConfig;
import fr.zyumie.Money.MoneyModule;
import fr.zyumie.Money.Util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoneyTopCommand implements CommandExecutor, TabCompleter {

    private final MoneyModule module;

    public MoneyTopCommand(MoneyModule module) { this.module = module; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!module.isEnabled()) {
            Msg.send(sender, "<red>Module Money désactivé.");
            return true;
        }

        MoneyConfig cfg = module.config();
        List<Map.Entry<String, Double>> all = module.service().getTop(1000);

        if (all == null || all.isEmpty()) {
            Msg.send(sender, "<red>Aucun joueur dans le classement pour le moment.");
            return true;
        }

        int perPage = cfg.topPerPage();
        int page = 1;
        if (args.length >= 1) {
            try { page = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        page = Math.max(1, page);

        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) perPage));
        page = Math.min(page, totalPages);

        List<Map.Entry<String, Double>> pageEntries =
                all.subList(Math.min((page - 1) * perPage, all.size()),
                        Math.min(page * perPage, all.size()));

        String plural = cfg.currencyPlural();

        String header = cfg.msg("moneytop-header");
        if (!header.isEmpty()) {
            send(sender, header
                    .replace("{page}", String.valueOf(page))
                    .replace("{max}", String.valueOf(totalPages)));
        }

        int rank = (page - 1) * perPage + 1;
        String entryFormat = cfg.msg("moneytop-entry");
        for (Map.Entry<String, Double> e : pageEntries) {
            if (!entryFormat.isEmpty()) {
                send(sender, entryFormat
                        .replace("{rank}", String.valueOf(rank++))
                        .replace("{player}", e.getKey())
                        .replace("{amount}", String.format(Locale.ROOT, "%.2f", e.getValue()))
                        .replace("{currency}", plural));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }

    private void send(CommandSender s, String message) {
        Msg.send(s, module.config().prefix() + message);
    }
}