package fr.zyumie.Money.Service;

import fr.zyumie.Money.MoneyConfig;
import fr.zyumie.Money.MoneyVaultBridge;
import fr.zyumie.Money.Storage.MoneyStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** Choisit l'économie. Le stockage local FireBallWand est prioritaire par défaut. */
public final class MoneyServiceFactory {

    private MoneyServiceFactory() {}

    public static MoneyService create(JavaPlugin plugin, MoneyConfig cfg, MoneyStorage storage) {
        String mode = cfg.mode();
        Economy external = findExternalVaultEconomy();

        if ("vault".equals(mode)) {
            if (external == null) {
                plugin.getLogger().warning("[Money] Mode 'vault' demandé mais aucun provider externe trouvé. "
                        + "Fallback sur le stockage interne FireBallWand.");
                return new InternalMoneyService(cfg, storage);
            }
            plugin.getLogger().info("[Money] Mode Vault externe : délégation à " + external.getName());
            return new VaultMoneyService(external);
        }

        if (!"internal".equals(mode) && !"auto".equals(mode)) {
            plugin.getLogger().warning("[Money] Mode inconnu '" + mode + "' ; utilisation du stockage interne.");
        } else if ("auto".equals(mode) && external != null) {
            plugin.getLogger().info("[Money] Mode auto : conservation des soldes migrés dans FireBallWand. "
                    + "Provider externe détecté (" + external.getName()
                    + ") mais ignoré. Utilisez mode: vault pour déléguer explicitement.");
        } else {
            plugin.getLogger().info("[Money] Mode interne : stockage FireBallWand.");
        }

        return new InternalMoneyService(cfg, storage);
    }

    /** Renvoie un provider Vault externe, sans renvoyer notre propre bridge. */
    public static Economy findExternalVaultEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;

        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) return null;

        Economy provider = registration.getProvider();
        return provider instanceof MoneyVaultBridge ? null : provider;
    }
}
