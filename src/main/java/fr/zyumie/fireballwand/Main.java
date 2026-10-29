package fr.zyumie.fireballwand;

import fr.zyumie.Commandes.Cheats.cheatmace;
import fr.zyumie.Commandes.Cheats.cheatsword;
import fr.zyumie.Commandes.Cheats.diamond_armorcheat;
import fr.zyumie.Commandes.Cheats.netherite_armorcheat;
import fr.zyumie.Chat.MsgCommand;
import fr.zyumie.Chat.MsgManager;
import fr.zyumie.Commandes.PotionOfDeath;
import fr.zyumie.Commandes.WinCharge_Boosted;
import fr.zyumie.Commandes.firewand;
import fr.zyumie.Commandes.kickarmor;
import fr.zyumie.Chat.ChatManager;
import fr.zyumie.Listener.StartKitManager;
import fr.zyumie.Listener.VersionManager;
import fr.zyumie.SoftDepend.ItemsAdderHook;
import fr.zyumie.SoftDepend.LuckPermsHook;
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

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private LuckPermsHook    luckPermsHook;
    private ItemsAdderHook   itemsAdderHook;
    private VanishManager    vanishManager;
    private ConfigManager    customConfigManager;

    private TpaConfigManager tpaConfigManager;
    private TpaManager tpaManager;
    private TpaTeleportManager tpaTeleportManager;
    private TpaInvincibilityManager tpaInvincibilityManager;

    @Override
    public void onEnable() {
        // Config Manager personnalisé (Gère config.yml, Vanish.yml, CustomCheat.yml)
        customConfigManager = new ConfigManager(this);

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

        // ── Vanish (instancié EN PREMIER — les listeners en dépendent) ───────
        vanishManager = new VanishManager(this);
        if (getCommand("vanish") != null) getCommand("vanish").setExecutor(new VanishCommand(this));
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListListener(this), this);

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

        fr.zyumie.Commandes.freezewand freezewand = new fr.zyumie.Commandes.freezewand(this);
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
        if (vanishManager != null) vanishManager.clear();
        if (tpaTeleportManager != null) tpaTeleportManager.cancelAll();
        if (tpaInvincibilityManager != null) tpaInvincibilityManager.clearAll();
        if (tpaManager != null) tpaManager.saveCooldownsSync();

        getLogger().info("════════════════════════════════════");
        getLogger().info("🔥 FireBallWand désactivé... :(");
        getLogger().info("════════════════════════════════════");
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public VanishManager  getVanishManager()    { return vanishManager;    }
    public LuckPermsHook  getLuckPermsHook()    { return luckPermsHook;    }
    public ItemsAdderHook getItemsAdderHook()   { return itemsAdderHook;   }

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
        String RED    = "\u001B[31m";
        String ORANGE = "\u001B[38;5;208m";
        String YELLOW = "\u001B[33m";
        String RESET  = "\u001B[0m";

        getLogger().info(RED    + "                                 .''.");
        getLogger().info(ORANGE + "       .''.             *''*    :_\\/_:     . ");
        getLogger().info(YELLOW + "      :_\\/_:   .    .:.*_\\/_*   : /\\ :  .'.:.'.");
        getLogger().info(RED    + "  .''.: /\\ : _\\(/_  ':'* /\\ : '..'.  -=:o:=-");
        getLogger().info(ORANGE + " :_\\/_:'.:::. /)\\*''*  .|.* '.\\'/.'_\\(/_'.':'.'");
        getLogger().info(YELLOW + " : /\\ : :::::  '*_\\/_* | |  -= o =- /)\\    '  *");
        getLogger().info(RED    + " '..'  ':::'   * /\\|'|  .'/.'\\'.  '._____");
        getLogger().info(ORANGE + "      *        __*..* |  |     :      |.   |' .---\"|");
        getLogger().info(YELLOW + "       _*   .-'   '-. |  |     .--'|  ||   | _|    |");
        getLogger().info(RED    + "    .-'|  _.|  |    ||   '-__  |   |  |    ||      |");
        getLogger().info(ORANGE + "    |' | |.    |    ||       | |   |  |    ||      |");
        getLogger().info(YELLOW + " ___|  '-'     '    \"\"       '-'   '-.'    '`      |____");
        getLogger().info(RED    + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + RESET);
        getLogger().info(YELLOW + "🔥 FireBallWand v" + getDescription().getVersion() + " activé !" + RESET);
    }
}