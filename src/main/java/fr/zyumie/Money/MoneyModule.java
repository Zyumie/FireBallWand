package fr.zyumie.Money;

import fr.zyumie.Money.Command.*;
import fr.zyumie.Money.Listener.PlayerListener;
import fr.zyumie.Money.Service.InternalMoneyService;
import fr.zyumie.Money.Service.MoneyService;
import fr.zyumie.Money.Service.MoneyServiceFactory;
import fr.zyumie.Money.Storage.MoneyStorage;
import fr.zyumie.Money.Storage.YamlMoneyStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/** Point d'entrée du module Money. */
public class MoneyModule {

    private final JavaPlugin plugin;
    private MoneyConfig config;
    private MoneyService service;
    private MoneyStorage storage;
    private PlayerListener listener;
    private Economy vaultBridge;
    private boolean vaultRegistered = false;

    public MoneyModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return service != null;
    }

    public MoneyService service() {
        return service;
    }

    public MoneyConfig config() {
        return config;
    }

    public void enable() {
        if (service != null) {
            plugin.getLogger().warning("[Money] Module déjà activé ; deuxième initialisation ignorée.");
            return;
        }

        if (!plugin.getConfig().getBoolean("modules.money.enabled", true)) {
            plugin.getLogger().info("[Money] Module désactivé via config.yml.");
            return;
        }

        registerCommand("money", new MoneyCommand(this));
        registerCommand("pay", new PayCommand(this));
        registerCommand("moneytop", new MoneyTopCommand(this));
        registerCommand("eco", new EcoCommand(this));

        // La migration de configuration et des soldes se fait avant le choix du service.
        MoneyConfig.migrateOldConfigIfNeeded(plugin);
        config = new MoneyConfig(plugin);

        storage = new YamlMoneyStorage(plugin);
        storage.load();
        service = MoneyServiceFactory.create(plugin, config, storage);

        boolean isInternal = service instanceof InternalMoneyService;
        if (isInternal && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultBridge = new MoneyVaultBridge(config, service);
            Bukkit.getServicesManager().register(
                    Economy.class, vaultBridge, plugin, ServicePriority.Highest);
            vaultRegistered = true;
            plugin.getLogger().info("[Money] Enregistré comme provider Vault.");
        }

        listener = new PlayerListener(this);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        plugin.getLogger().info("[Money] Module activé (" + service.getClass().getSimpleName() + ").");
    }

    public void disable() {
        if (service != null) {
            try {
                service.flush();
            } catch (Exception ex) {
                plugin.getLogger().warning("[Money] Erreur lors de la sauvegarde (flush) : " + ex.getMessage());
            }
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (vaultRegistered && vaultBridge != null) {
            Bukkit.getServicesManager().unregister(Economy.class, vaultBridge);
            vaultBridge = null;
            vaultRegistered = false;
        }
        service = null;
        storage = null;
        config = null;
        plugin.getLogger().info("[Money] Module désactivé.");
    }

    public void reload() {
        disable();
        enable();
    }

    private void registerCommand(String name, Object executorAndCompleter) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().warning("[Money] Commande '" + name + "' absente de plugin.yml.");
            return;
        }
        if (executorAndCompleter instanceof CommandExecutor executor) {
            cmd.setExecutor(executor);
        } else {
            plugin.getLogger().warning("[Money] '" + name + "' n'implémente pas CommandExecutor.");
        }
        if (executorAndCompleter instanceof TabCompleter completer) {
            cmd.setTabCompleter(completer);
        }
    }
}