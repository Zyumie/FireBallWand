package fr.zyumie.SoftDepend;

import fr.zyumie.Money.MoneyConfig;
import fr.zyumie.Money.MoneyVaultBridge;
import fr.zyumie.Money.Service.InternalMoneyService;
import fr.zyumie.Money.Service.MoneyService;
import fr.zyumie.Money.Service.VaultMoneyService;
import fr.zyumie.Money.Storage.MoneyStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Partie optionnelle de l'intégration Vault.
 * Cette classe n'est chargée par MoneyModule que si le plugin "Vault" est présent.
 * Toutes les références à net.milkbowl.vault restent confinées ici et dans les classes
 * VaultMoneyService / MoneyVaultBridge.
 */
public final class VaultHook {

    private final JavaPlugin plugin;
    private final MoneyConfig config;
    private final MoneyStorage storage;

    private MoneyVaultBridge bridge;
    private boolean registered;

    public VaultHook(JavaPlugin plugin, MoneyConfig config, MoneyStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
    }

    /** Choisit le service, puis publie le bridge interne si nécessaire. */
    public MoneyService createService() {
        String mode = config.mode();
        Economy external = findExternalEconomy();
        MoneyService selected;

        if ("vault".equals(mode) && external != null) {
            plugin.getLogger().info("[Money] Mode Vault externe : délégation à " + external.getName() + ".");
            selected = new VaultMoneyService(external);
        } else {
            if ("vault".equals(mode)) {
                plugin.getLogger().warning("[Money] Mode 'vault' demandé mais aucun provider externe trouvé. "
                        + "Fallback sur le stockage interne FireBallWand.");
            } else if (!"internal".equals(mode) && !"auto".equals(mode)) {
                plugin.getLogger().warning("[Money] Mode inconnu '" + mode + "' ; utilisation du stockage interne.");
            } else if ("auto".equals(mode) && external != null) {
                plugin.getLogger().info("[Money] Mode auto : conservation des soldes FireBallWand. "
                        + "Provider externe détecté (" + external.getName()
                        + ") mais ignoré. Utilisez mode: vault pour déléguer explicitement.");
            } else {
                plugin.getLogger().info("[Money] Mode interne : stockage FireBallWand.");
            }

            selected = new InternalMoneyService(config, storage);
        }

        // Le bridge n'est publié que lorsque FireBallWand gère lui-même les soldes.
        if (selected instanceof InternalMoneyService) {
            bridge = new MoneyVaultBridge(config, selected);
            Bukkit.getServicesManager().register(
                    Economy.class, bridge, plugin, ServicePriority.Highest);
            registered = true;
            plugin.getLogger().info("[Money] Enregistré comme provider Vault.");
        }

        return selected;
    }

    private Economy findExternalEconomy() {
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) return null;

        Economy provider = registration.getProvider();
        return provider instanceof MoneyVaultBridge ? null : provider;
    }

    public void unregister() {
        if (!registered || bridge == null) return;
        Bukkit.getServicesManager().unregister(Economy.class, bridge);
        bridge = null;
        registered = false;
    }
}
