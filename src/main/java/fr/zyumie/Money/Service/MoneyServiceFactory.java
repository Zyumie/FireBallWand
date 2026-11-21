package fr.zyumie.Money.Service;

import fr.zyumie.Money.MoneyConfig;
import fr.zyumie.Money.Storage.MoneyStorage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Factory autonome : ne référence volontairement aucune classe de l'API Vault.
 * Le choix optionnel Vault est entièrement pris en charge par MoneyVaultHook.
 */
public final class MoneyServiceFactory {

    private MoneyServiceFactory() {}

    public static MoneyService create(JavaPlugin plugin, MoneyConfig config, MoneyStorage storage) {
        String mode = config.mode();
        if (!"internal".equals(mode) && !"auto".equals(mode) && !"vault".equals(mode)) {
            plugin.getLogger().warning("[Money] Mode inconnu '" + mode + "' ; utilisation du stockage interne.");
        } else if ("vault".equals(mode)) {
            plugin.getLogger().warning("[Money] Le mode Vault nécessite le hook optionnel ; fallback interne.");
        }
        return new InternalMoneyService(config, storage);
    }
}
