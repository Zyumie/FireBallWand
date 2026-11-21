package fr.zyumie.Money;

import fr.zyumie.Money.Service.MoneyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;

/**
 * Expose notre service interne via l'API Vault pour que les autres plugins
 * (shops, quests…) puissent lire/écrire l'économie SANS installer Vault.
 * Enregistré uniquement si le mode est "internal" ou "auto"-sans-provider.
 */
public class MoneyVaultBridge implements Economy {

    private final MoneyConfig config;
    private final MoneyService service;

    public MoneyVaultBridge(MoneyConfig config, MoneyService service) {
        this.config = config;
        this.service = service;
    }

    @Override public boolean isEnabled() { return true; }
    @Override public String getName() { return "FireBallWand-Money"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 2; }

    @Override
    public String format(double amount) {
        return String.format(Locale.ROOT, "%.2f %s", amount, currencyNamePlural());
    }

    @Override public String currencyNamePlural()   { return config.currencyPlural(); }
    @Override public String currencyNameSingular() { return config.currencySingular(); }

    @Override public double getBalance(OfflinePlayer p) { return service.getBalance(p.getUniqueId()); }
    @Override public double getBalance(String n) { return getBalance(resolve(n)); }
    @Override public double getBalance(OfflinePlayer p, String w) { return getBalance(p); }
    @Override public double getBalance(String n, String w) { return getBalance(n); }

    @Override public boolean has(OfflinePlayer p, double a) { return service.has(p.getUniqueId(), a); }
    @Override public boolean has(String n, double a) { return has(resolve(n), a); }
    @Override public boolean has(OfflinePlayer p, String w, double a) { return has(p, a); }
    @Override public boolean has(String n, String w, double a) { return has(n, a); }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer p, double a) {
        boolean ok = service.deposit(p.getUniqueId(), a);
        return new EconomyResponse(a, service.getBalance(p.getUniqueId()),
                ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                ok ? null : "Dépôt refusé (limite atteinte)");
    }
    @Override public EconomyResponse depositPlayer(String n, double a) { return depositPlayer(resolve(n), a); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer p, String w, double a) { return depositPlayer(p, a); }
    @Override public EconomyResponse depositPlayer(String n, String w, double a) { return depositPlayer(n, a); }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer p, double a) {
        boolean ok = service.withdraw(p.getUniqueId(), a);
        return new EconomyResponse(a, service.getBalance(p.getUniqueId()),
                ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                ok ? null : "Solde insuffisant");
    }
    @Override public EconomyResponse withdrawPlayer(String n, double a) { return withdrawPlayer(resolve(n), a); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer p, String w, double a) { return withdrawPlayer(p, a); }
    @Override public EconomyResponse withdrawPlayer(String n, String w, double a) { return withdrawPlayer(n, a); }

    // Comptes (non gérés)
    @Override public boolean hasAccount(OfflinePlayer p) { return true; }
    @Override public boolean hasAccount(String n) { return true; }
    @Override public boolean hasAccount(OfflinePlayer p, String w) { return true; }
    @Override public boolean hasAccount(String n, String w) { return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer p) { service.touch(p.getUniqueId()); return true; }
    @Override public boolean createPlayerAccount(String n) { return createPlayerAccount(resolve(n)); }
    @Override public boolean createPlayerAccount(OfflinePlayer p, String w) { return createPlayerAccount(p); }
    @Override public boolean createPlayerAccount(String n, String w) { return createPlayerAccount(n); }

    // Banques non supportées
    private EconomyResponse ns() { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Non supporté"); }
    @Override public EconomyResponse createBank(String n, OfflinePlayer p) { return ns(); }
    @Override public EconomyResponse createBank(String n, String p) { return ns(); }
    @Override public EconomyResponse deleteBank(String n) { return ns(); }
    @Override public EconomyResponse bankBalance(String n) { return ns(); }
    @Override public EconomyResponse bankHas(String n, double a) { return ns(); }
    @Override public EconomyResponse bankWithdraw(String n, double a) { return ns(); }
    @Override public EconomyResponse bankDeposit(String n, double a) { return ns(); }
    @Override public EconomyResponse isBankOwner(String n, OfflinePlayer p) { return ns(); }
    @Override public EconomyResponse isBankOwner(String n, String p) { return ns(); }
    @Override public EconomyResponse isBankMember(String n, OfflinePlayer p) { return ns(); }
    @Override public EconomyResponse isBankMember(String n, String p) { return ns(); }
    @Override public List<String> getBanks() { return List.of(); }

    private OfflinePlayer resolve(String name) {
        OfflinePlayer p = Bukkit.getOfflinePlayerIfCached(name);
        return p != null ? p : Bukkit.getOfflinePlayer(name);
    }
}