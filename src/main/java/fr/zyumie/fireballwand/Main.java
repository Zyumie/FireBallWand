package fr.zyumie.fireballwand;

import fr.zyumie.Commandes.Cheats.cheatmace;
import fr.zyumie.Commandes.Cheats.cheatsword;
import fr.zyumie.Commandes.Cheats.diamond_armorcheat;
import fr.zyumie.Commandes.Cheats.netherite_armorcheat;
import fr.zyumie.Commandes.firewand;
import fr.zyumie.Commandes.kick_armor;
import fr.zyumie.Listener.ChatManager;
import fr.zyumie.Listener.VersionManager;
import fr.zyumie.SoftDepend.ItemsAdderHook;
import fr.zyumie.SoftDepend.LuckPermsHook;
import fr.zyumie.Vanish.ServerListListener;
import fr.zyumie.Vanish.VanishCommand;
import fr.zyumie.Vanish.VanishListener;
import fr.zyumie.Vanish.VanishManager;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private LuckPermsHook    luckPermsHook;
    private ItemsAdderHook   itemsAdderHook;
    private VanishManager    vanishManager;

    @Override
    public void onEnable() {
        // Config
        saveDefaultConfig();

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
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListListener(this), this);

        // ── Commandes ────────────────────────────────────────────────────────
        firewand firewand = new firewand(this);
        getCommand("firewand").setExecutor(firewand);
        getServer().getPluginManager().registerEvents(firewand, this);

        kick_armor kickArmor = new kick_armor(this);
        getCommand("kickarmor").setExecutor(kickArmor);

        fr.zyumie.Commandes.freezewand freezewand = new fr.zyumie.Commandes.freezewand(this);
        getCommand("freezewand").setExecutor(freezewand);
        getCommand("unfreezewand-all").setExecutor(freezewand);

        getCommand("cheatsword").setExecutor(new cheatsword(this));
        getCommand("cheatmace").setExecutor(new cheatmace(this));
        getCommand("diamond_armorcheat").setExecutor(new diamond_armorcheat(this));
        getCommand("netherite_armorcheat").setExecutor(new netherite_armorcheat(this));

        // ── Listeners généraux ───────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new ConfigManager(this), this);
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
        getLogger().info("════════════════════════════════════");
        getLogger().info("🔥 FireBallWand désactivé... :(");
        getLogger().info("════════════════════════════════════");
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public VanishManager  getVanishManager()    { return vanishManager;    }
    public LuckPermsHook  getLuckPermsHook()    { return luckPermsHook;    }
    public ItemsAdderHook getItemsAdderHook()   { return itemsAdderHook;   }

    // ── Startup log ──────────────────────────────────────────────────────────

    public void logStartup() {
        String RED    = "\u001B[31m";
        String ORANGE = "\u001B[38;5;208m";
        String YELLOW = "\u001B[33m";
        String RESET  = "\u001B[0m";

        getLogger().info(RED    + "                                 .''.");
        getLogger().info(ORANGE + "       .''.             *''*    :_\\/_:     . ");
        getLogger().info(YELLOW + "      :_\\/_:   .    .:.*_\\/_*   : /\\ :  .'.:.'.");
        getLogger().info(RED    + "  .''.: /\\ : _\\(/_  ':'* /\\ *  : '..'.  -=:o:=-");
        getLogger().info(ORANGE + " :_\\/_:'.:::. /)\\*''*  .|.* '.\\'/.'_\\(/_'.':'.'");
        getLogger().info(YELLOW + " : /\\ : :::::  '*_\\/_* | |  -= o =- /)\\    '  *");
        getLogger().info(RED    + " '..'  ':::'   * /\\ * |'|  .'/.'\\'.  '._____");
        getLogger().info(ORANGE + "      *        __*..* |  |     :      |.   |' .---\"|");
        getLogger().info(YELLOW + "       _*   .-'   '-. |  |     .--'|  ||   | _|    |");
        getLogger().info(RED    + "    .-'|  _.|  |    ||   '-__  |   |  |    ||      |");
        getLogger().info(ORANGE + "    |' | |.    |    ||       | |   |  |    ||      |");
        getLogger().info(YELLOW + " ___|  '-'     '    \"\"       '-'   '-.'    '`      |____");
        getLogger().info(RED    + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + RESET);
        getLogger().info(YELLOW + "🔥 FireBallWand v" + getDescription().getVersion() + " activé !" + RESET);
    }
}
