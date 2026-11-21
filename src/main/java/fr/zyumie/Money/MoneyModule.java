package fr.zyumie.Money;

import fr.zyumie.Money.Command.*;
import fr.zyumie.Money.Listener.PlayerListener;
import fr.zyumie.Money.Service.InternalMoneyService;
import fr.zyumie.Money.Service.MoneyService;
import fr.zyumie.Money.Storage.MoneyStorage;
import fr.zyumie.Money.Storage.YamlMoneyStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Point d'entrée du module Money, indépendant de l'API Vault au chargement. */
public class MoneyModule {

    private final JavaPlugin plugin;
    private MoneyConfig config;
    private MoneyService service;
    private MoneyStorage storage;
    private PlayerListener listener;

    // Object volontairement : le type concret du hook ne doit pas être résolu sans Vault.
    private Object vaultHook;

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

        // Démarrage sûr par défaut : l'économie interne ne dépend jamais de Vault.
        service = new InternalMoneyService(config, storage);
        initializeOptionalVaultHook();

        listener = new PlayerListener(this);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        plugin.getLogger().info("[Money] Module activé (" + service.getClass().getSimpleName() + ").");
    }

    /**
     * Charge toutes les classes Vault par réflexion, et uniquement après avoir confirmé
     * que le plugin Vault est installé. Sans Vault, aucun type Economy n'est résolu.
     */
    private void initializeOptionalVaultHook() {
        Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (vaultPlugin == null || !vaultPlugin.isEnabled()) {
            plugin.getLogger().info("[Money] Vault absent : économie interne autonome, aucun hook chargé.");
            if ("vault".equalsIgnoreCase(config.mode())) {
                plugin.getLogger().warning("[Money] Mode 'vault' demandé sans Vault : fallback sur l'économie interne.");
            }
            return;
        }

        Object hook = null;
        try {
            // Ne pas remplacer ce nom par un import Java : c'est l'isolation de chargement recherchée.
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> hookClass = Class.forName(
                    "fr.zyumie.SoftDepend.VaultHook", true, loader);

            hook = hookClass.getConstructor(
                    JavaPlugin.class, MoneyConfig.class, MoneyStorage.class)
                    .newInstance(plugin, config, storage);

            Object selectedService = hookClass.getMethod("createService").invoke(hook);
            if (!(selectedService instanceof MoneyService selected)) {
                throw new IllegalStateException("MoneyVaultHook.createService() n'a pas retourné MoneyService.");
            }

            service = selected;
            vaultHook = hook;
            plugin.getLogger().info("[Money] Hook Vault chargé : " + service.getClass().getSimpleName() + ".");

        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            safelyUnregisterHook(hook);
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Money] Le hook Vault a échoué ; l'économie interne reste active.", cause);

        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            safelyUnregisterHook(hook);
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Money] API Vault indisponible/incompatible ; l'économie interne reste active.", ex);
        }
    }

    private void safelyUnregisterHook(Object hook) {
        if (hook == null) return;
        try {
            hook.getClass().getMethod("unregister").invoke(hook);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // Le démarrage interne doit rester prioritaire même si le hook est partiellement initialisé.
        }
    }

    private void unregisterOptionalVaultHook() {
        if (vaultHook == null) return;
        safelyUnregisterHook(vaultHook);
        vaultHook = null;
    }

    public void disable() {
        if (service != null) {
            try {
                service.flush();
            } catch (Exception ex) {
                plugin.getLogger().warning("[Money] Erreur lors de la sauvegarde (flush) : " + ex.getMessage());
            }
        }

        unregisterOptionalVaultHook();

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
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
