package fr.zyumie.Money.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** API économique interne, indépendante de Vault. */
public interface MoneyService {

    double getBalance(UUID uuid);
    boolean has(UUID uuid, double amount);
    boolean deposit(UUID uuid, double amount);
    boolean withdraw(UUID uuid, double amount);
    void set(UUID uuid, double amount);

    /** Classement trié (nom -> solde). */
    List<Map.Entry<String, Double>> getTop(int limit);

    /** Appelé pour préparer un joueur (chargement lazy). */
    default void touch(UUID uuid) {}

    /** Sauvegarde finale. */
    default void flush() {}

    static boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount >= 0;
    }

}

