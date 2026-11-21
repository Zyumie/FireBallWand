package fr.zyumie.Money.Service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

/** Délègue à un provider Vault externe (Essentials, CMI, Vault2.0…). */
public class VaultMoneyService implements MoneyService {

    private final Economy economy;

    public VaultMoneyService(Economy economy) { this.economy = economy; }

    @Override public double getBalance(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return economy.getBalance(p);
    }

    @Override public boolean has(UUID uuid, double amount) {
        return economy.has(Bukkit.getOfflinePlayer(uuid), amount);
    }

    @Override public boolean deposit(UUID uuid, double amount) {
        EconomyResponse r = economy.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
        return r.transactionSuccess();
    }

    @Override public boolean withdraw(UUID uuid, double amount) {
        EconomyResponse r = economy.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), amount);
        return r.transactionSuccess();
    }

    @Override public void set(UUID uuid, double amount) {
        // Non standard : on calcule le delta
        double cur = getBalance(uuid);
        if (amount > cur) deposit(uuid, amount - cur);
        else if (amount < cur) withdraw(uuid, cur - amount);
    }

    @Override
    public List<Map.Entry<String, Double>> getTop(int limit) {
        // Vault n'expose pas de "top" — on renvoie uniquement les joueurs connectés
        return Bukkit.getOnlinePlayers().stream()
                .map(p -> Map.entry(p.getName(), economy.getBalance(p)))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override public void touch(UUID uuid) { /* Vault gère lui-même */ }
    @Override public void flush() { /* Vault gère lui-même */ }
}