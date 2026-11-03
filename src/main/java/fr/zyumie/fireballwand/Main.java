package fr.zyumie.fireballwand;

import fr.zyumie.Commandes.*;
import fr.zyumie.Commandes.Cheats.cheatmace;
import fr.zyumie.Commandes.Cheats.cheatsword;
import fr.zyumie.Commandes.Cheats.diamond_armorcheat;
import fr.zyumie.Commandes.Cheats.netherite_armorcheat;
import fr.zyumie.Chat.MsgCommand;
import fr.zyumie.Chat.MsgManager;
import fr.zyumie.Chat.ChatManager;
import fr.zyumie.Commandes.FunnyItems.*;
import fr.zyumie.Listener.StartKitManager;
import fr.zyumie.Listener.VersionManager;
import fr.zyumie.SoftDepend.ItemsAdderHook;
import fr.zyumie.SoftDepend.LuckPermsHook;
import fr.zyumie.SoftDepend.PlaceholderAPIHook;
import fr.zyumie.Staff.GodMod.GodCommand;
import fr.zyumie.Staff.GodMod.GodManager;
import fr.zyumie.Staff.StaffCommand;
import fr.zyumie.Staff.StaffConfig;
import fr.zyumie.Staff.StaffListener;
import fr.zyumie.Staff.StaffManager;
import fr.zyumie.TPA.TpaCommand;
import fr.zyumie.Vanish.Listener.ServerListListener;
import fr.zyumie.Vanish.VanishCommand;
import fr.zyumie.Vanish.Listener.VanishListener;
import fr.zyumie.Vanish.Listener.VanishManager;

import fr.zyumie.TPA.Listener.TpaConfigManager;
import fr.zyumie.TPA.Listener.TpaManager;
import fr.zyumie.TPA.Listener.TpaTeleportManager;
import fr.zyumie.TPA.Listener.TpaInvincibilityManager;
import fr.zyumie.TPA.Listener.TpaListener;

import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private LuckPermsHook    luckPermsHook;
    private ItemsAdderHook   itemsAdderHook;
    private PlaceholderAPIHook placeholderAPIHook;
    private VanishManager    vanishManager;
    private ConfigManager    customConfigManager;

    private StaffConfig      staffConfig;
    private StaffManager     staffManager;
    private GodManager       godManager;

    private TpaConfigManager tpaConfigManager;
    private TpaManager tpaManager;
    private TpaTeleportManager tpaTeleportManager;
    private TpaInvincibilityManager tpaInvincibilityManager;

    @Override
    public void onEnable() {
        // Config Manager personnalisé (Gère config.yml, Vanish.yml, CustomCheat.yml)
        customConfigManager = new ConfigManager(this);

        // Config du package Staff (Staff.yml)
        staffConfig = new StaffConfig(this);

        // ── Commande globale /fw reload ─────────────────────────────────────
        if (getCommand("fireballwand") != null) {
            getCommand("fireballwand").setExecutor(new FireballWandCommand(this));
        }

        // ── Soft depends ─────────────────────────────────────────────────────
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsHook = new LuckPermsHook();
            getLogger().info("LuckPerms détecté, hook activé.");
        } else {
            getLogger().info("LuckPerms non détecté.");
        }

        if (getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            itemsAdderHook = new ItemsAdderHook();
            getLogger().info("ItemsAdder détecté, hook activé.");
        } else {
            getLogger().info("ItemsAdder non détecté.");
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook();
            getLogger().info("PlaceHolderAPI détecté, hook activé.");
        } else {
            getLogger().info("PlaceHolderAPI non détecté.");
        }

        if (getServer().getPluginManager().getPlugin("TAB") != null) {
            getLogger().info("TAB (Neznamy) détecté, hook Vanish activé.");
        } else {
            getLogger().info("TAB non détecté.");
        }

        // ── Vanish (instancié EN PREMIER — les listeners en dépendent) ───────
        vanishManager = new VanishManager(this);
        if (getCommand("vanish") != null) getCommand("vanish").setExecutor(new VanishCommand(this));
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListListener(this), this);

        // ── Staff / Godmod (config : Staff.yml) ──────────────────────────────
        // Les managers sont toujours créés (très légers) : les messages en dépendent.
        godManager = new GodManager();
        staffManager = new StaffManager(this);

        boolean staffEnabled = getStaffConfig().getBoolean("staff.enabled", true);
        boolean godEnabled   = getStaffConfig().getBoolean("god.enabled", true);

        CommandExecutor disabled = (sender, cmd, label, args) -> {
            sender.sendMessage("§cCette fonctionnalité est désactivée.");
            return true;
        };

        bind("staff", staffEnabled ? new StaffCommand(this) : disabled);

        if (godEnabled) {
            GodCommand godCommand = new GodCommand(this);
            bind("godmod", godCommand);
            bind("ungodmod", godCommand);
        } else {
            bind("godmod", disabled);
            bind("ungodmod", disabled);
        }

        // Le listener sert aux deux fonctionnalités : inutile de l'enregistrer si les deux sont coupées
        if (staffEnabled || godEnabled) {
            getServer().getPluginManager().registerEvents(new StaffListener(this), this);
        }

        // ── TPA System ───────────────────────────────────────────────────────
        this.tpaConfigManager = new TpaConfigManager(this);
        this.tpaManager = new TpaManager(this);
        this.tpaTeleportManager = new TpaTeleportManager(this);
        this.tpaInvincibilityManager = new TpaInvincibilityManager(this);

        TpaCommand tpaCommand = new TpaCommand(this);

        if (getCommand("tpa") != null) {
            getCommand("tpa").setExecutor(tpaCommand);
            getCommand("tpa").setTabCompleter(tpaCommand);
        }
        if (getCommand("tpahere") != null) {
            getCommand("tpahere").setExecutor(tpaCommand);
            getCommand("tpahere").setTabCompleter(tpaCommand);
        }
        if (getCommand("tpaccept") != null) {
            getCommand("tpaccept").setExecutor(tpaCommand);
            getCommand("tpaccept").setTabCompleter(tpaCommand);
        }
        if (getCommand("tpdeny") != null) {
            getCommand("tpdeny").setExecutor(tpaCommand);
            getCommand("tpdeny").setTabCompleter(tpaCommand);
        }
        if (getCommand("tpacancel") != null) {
            getCommand("tpacancel").setExecutor(tpaCommand);
        }
        if (getCommand("tpareload") != null) {
            getCommand("tpareload").setExecutor(tpaCommand);
        }

        getServer().getPluginManager().registerEvents(new TpaListener(this), this);

        // ── Commandes ────────────────────────────────────────────────────────
        firewand firewandCmd = new firewand(this);
        if (getCommand("firewand") != null) getCommand("firewand").setExecutor(firewandCmd);
        getServer().getPluginManager().registerEvents(firewandCmd, this);

        kickarmor kickArmor = new kickarmor(this);
        if (getCommand("kickarmor") != null) getCommand("kickarmor").setExecutor(kickArmor);

        freezewand freezewand = new freezewand(this);
        if (getCommand("freezewand") != null) getCommand("freezewand").setExecutor(freezewand);
        if (getCommand("unfreezewand-all") != null) getCommand("unfreezewand-all").setExecutor(freezewand);

        WinCharge_Boosted winCharge = new WinCharge_Boosted(this);
        if (getCommand("wincharge_boosted") != null) getCommand("wincharge_boosted").setExecutor(winCharge);
        getServer().getPluginManager().registerEvents(winCharge, this);

        PotionOfDeath potionOfDeath = new PotionOfDeath(this);
        if (getCommand("potionofdeath") != null) {
            getCommand("potionofdeath").setExecutor(potionOfDeath);
        }
        getServer().getPluginManager().registerEvents(potionOfDeath, this);

        if (getCommand("cheatsword") != null) getCommand("cheatsword").setExecutor(new cheatsword(this));
        if (getCommand("cheatmace") != null) getCommand("cheatmace").setExecutor(new cheatmace(this));
        if (getCommand("diamond_armorcheat") != null) getCommand("diamond_armorcheat").setExecutor(new diamond_armorcheat(this));
        if (getCommand("netherite_armorcheat") != null) getCommand("netherite_armorcheat").setExecutor(new netherite_armorcheat(this));

        // ── System de Msg ───────────────────────────────────────────────────
        MsgManager msgManager = new MsgManager(this);
        MsgCommand msgCommand = new MsgCommand(msgManager);

        if (getCommand("msg") != null) getCommand("msg").setExecutor(msgCommand);
        if (getCommand("reply") != null) getCommand("reply").setExecutor(msgCommand);

        // ── Kit de Départ ───────────────────────────────────────────────────
        StartKitManager startKitManager = new StartKitManager(this);
        getServer().getPluginManager().registerEvents(startKitManager, this);

        if (getCommand("starterkit") != null) {
            getCommand("starterkit").setExecutor(startKitManager);
            getCommand("starterkit").setTabCompleter(startKitManager);
        }

        // ── Listeners généraux ───────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new ChatManager(this), this);

        // ── Version check ────────────────────────────────────────────────────
        VersionManager.check(this);
        VersionManager.init(this);
        getServer().getPluginManager().registerEvents(new VersionManager(), this);

        logStartup();
    }

    @Override
    public void onDisable() {
        if (staffManager != null) staffManager.disableAll();
        if (godManager != null) godManager.clear();
        if (vanishManager != null) vanishManager.clear();
        if (tpaTeleportManager != null) tpaTeleportManager.cancelAll();
        if (tpaInvincibilityManager != null) tpaInvincibilityManager.clearAll();

        getLogger().info("════════════════════════════════════");
        getLogger().info("🔥 FireBallWand désactivé... :(");
        getLogger().info("════════════════════════════════════");
    }

    /** Associe un exécuteur à une commande si elle est déclarée dans plugin.yml. */
    private void bind(String name, CommandExecutor executor) {
        if (getCommand(name) != null) getCommand(name).setExecutor(executor);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public VanishManager  getVanishManager()    { return vanishManager;    }
    public LuckPermsHook  getLuckPermsHook()    { return luckPermsHook;    }
    public ItemsAdderHook getItemsAdderHook()   { return itemsAdderHook;   }
    public PlaceholderAPIHook getPlaceholderAPIHook() { return placeholderAPIHook; }

    public StaffManager getStaffManager() { return staffManager; }
    public GodManager   getGodManager()   { return godManager;   }

    /** Contenu de Staff.yml. */
    public FileConfiguration getStaffConfig() {
        return staffConfig.get();
    }

    /** À appeler depuis /fw reload pour recharger Staff.yml. */
    public void reloadStaffConfig() {
        staffConfig.reload();
    }

    public FileConfiguration getVanishConfig() {
        return customConfigManager.getVanishConfig();
    }

    public FileConfiguration getCustomCheatConfig() {
        return customConfigManager.getCustomCheatConfig();
    }

    public FileConfiguration getStartKitConfig() {
        return customConfigManager.getStartKitConfig();
    }

    public TpaConfigManager        getTpaConfigManager()       { return tpaConfigManager; }
    public TpaManager              getTpaManager()             { return tpaManager; }
    public TpaTeleportManager      getTpaTeleportManager()     { return tpaTeleportManager; }
    public TpaInvincibilityManager getTpaInvincibilityManager() { return tpaInvincibilityManager; }

    // ── Startup log ──────────────────────────────────────────────────────────

    public void logStartup() {
        String R1 = "\u001B[38;5;196m"; // Rouge vif
        String R2 = "\u001B[38;5;202m"; // Orange-rouge
        String O1 = "\u001B[38;5;208m"; // Orange
        String Y1 = "\u001B[38;5;214m"; // Jaune-orange
        String Y2 = "\u001B[38;5;220m"; // Jaune vif
        String GRAY = "\u001B[38;5;242m";
        String WHITE = "\u001B[38;5;255m";
        String BOLD = "\u001B[1m";
        String RESET = "\u001B[0m";

        getLogger().info(R1 + "  ___ ___ ___  ___ ___  _   _   ___  ___  _ _  _  ___  " + RESET);
        getLogger().info(R2 + " | __|_ _| _ \\| __| _ )/_\\ | | | |  \\/  |/ _\\| | |   \\ " + RESET);
        getLogger().info(O1 + " | _| | ||   /| _|| _ \\  _ \\| |_| | |\\/| |   | |_| |) |" + RESET);
        getLogger().info(Y1 + " |_| |___|_|_\\|___|___/_/ \\_\\___|_|_|  |_|_|_|___|___/ " + RESET);
        getLogger().info(Y2 + "               🔥 FIREBALL WAND CORE V" + getDescription().getVersion() + " 🔥" + RESET);
        getLogger().info(GRAY + " ──────────────────────────────────────────────────────────" + RESET);
        getLogger().info(WHITE + "  • Status         : " + Y2 + BOLD + "ONLINE & READY" + RESET);
        getLogger().info(WHITE + "  • Modules Actifs : " + O1 + "Vanish, Staff, TPA, StarterKit, Cheats, Chat" + RESET);
        getLogger().info(WHITE + "  • Developpeur    : " + R2 + "Zyumie" + RESET);
        getLogger().info(GRAY + " ──────────────────────────────────────────────────────────" + RESET);
    }
}
