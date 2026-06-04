package fr.zyumie.fireballwand;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import fr.zyumie.Commandes.firewand;
import fr.zyumie.Commandes.freezewand;
import fr.zyumie.Commandes.kick_armor;
import fr.zyumie.Commandes.Cheats.cheatmace;
import fr.zyumie.Commandes.Cheats.cheatsword;
import fr.zyumie.Commandes.Cheats.diamond_armorcheat;
import fr.zyumie.Commandes.Cheats.netherite_armorcheat;
import fr.zyumie.Listener.ChatManager;
import fr.zyumie.Listener.VersionManager;
import fr.zyumie.SoftDepend.LuckPermsHook;
import fr.zyumie.Vanish.*;

import org.bukkit.command.CommandExecutor;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

	private LuckPermsHook luckPermsHook;
	private VanishManager vanishManager;
	
	@Override
	public void onEnable() {
		
		// FireballWand
	    firewand firewand = new firewand(this);
	    getCommand("firewand").setExecutor(firewand);

		// Kick armor
		kick_armor kickArmor = new kick_armor(this);
		getCommand("kickarmor").setExecutor(kickArmor);

		// Freeze wand
		freezewand freezewand = new freezewand(this);
	    getCommand("freezewand").setExecutor(freezewand);
		
		// Listener
		getServer().getPluginManager().registerEvents(this, this);
	    getServer().getPluginManager().registerEvents(firewand, this);
		getServer().getPluginManager().registerEvents(new ConfigManager(this), this);
	    getServer().getPluginManager().registerEvents(new ChatManager(this), this);
	    
	    
		// CheckVersion
		VersionManager.check(this);
		VersionManager.init(this);
		getServer().getPluginManager().registerEvents(new VersionManager(), this);
        
		
		// LuckPerm's ( Vérifie si il est la ou non )
	    if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
	        luckPermsHook = new LuckPermsHook();
	        getLogger().info("LuckPerms détecté, hook activé.");
	    } else {
	        getLogger().info("LuckPerms non détecté.");
	    }
		
        
		// Give une Armes Cheater
		getCommand("cheatsword").setExecutor(new cheatsword());
		getCommand("cheatmace").setExecutor(new cheatmace());	
		
		
		// Give une Armures Cheater
		getCommand("diamond_armorcheat").setExecutor(new diamond_armorcheat());	
		getCommand("netherite_armorcheat").setExecutor(new netherite_armorcheat());	


		// Vanish
		vanishManager = new VanishManager(this);
		getCommand("vanish").setExecutor(new VanishCommand(this));
		getServer().getPluginManager().registerEvents(new VanishListener(this), this);
		getServer().getPluginManager().registerEvents(new ServerListListener(this), this);


		logStartup();
		
	}
	
	
	public void logStartup() {
		
	    String RED    = "\u001B[31m";
	    String ORANGE = "\u001B[38;5;208m"; // orange ANSI 256 couleurs
	    String YELLOW = "\u001B[33m";
	    String RESET  = "\u001B[0m";

	    getLogger().info(RED + "                                 .''.");
	    getLogger().info(ORANGE + "       .''.             *''*    :_\\/_:     . ");
	    getLogger().info(YELLOW + "      :_\\/_:   .    .:.*_\\/_*   : /\\ :  .'.:.'." );
	    getLogger().info(RED + "  .''.: /\\ : _\\(/_  ':'* /\\ *  : '..'.  -=:o:=-");
	    getLogger().info(ORANGE + " :_\\/_:'.:::. /)\\*''*  .|.* '.\\'/.'_\\(/_'.':'.'");
	    getLogger().info(YELLOW + " : /\\ : :::::  '*_\\/_* | |  -= o =- /)\\    '  *");
	    getLogger().info(RED + " '..'  ':::'   * /\\ * |'|  .'/.\'\\.  '._____");
	    getLogger().info(ORANGE + "      *        __*..* |  |     :      |.   |' .---\"|");
	    getLogger().info(YELLOW + "       _*   .-'   '-. |  |     .--'|  ||   | _|    |");
	    getLogger().info(RED + "    .-'|  _.|  |    ||   '-__  |   |  |    ||      |");
	    getLogger().info(ORANGE + "    |' | |.    |    ||       | |   |  |    ||      |");
	    getLogger().info(YELLOW + " ___|  '-'     '    \"\"       '-'   '-.'    '`      |____");
	    getLogger().info(RED + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + RESET);
	    
	    getLogger().info(YELLOW + "🔥 FireBallWand v" + getDescription().getVersion() + " activé !" + RESET);
	    
	}

	
	@Override
	public void onDisable() {

	    getLogger().info("════════════════════════════════════");
	    getLogger().info("🔥 §cFireBallWand désactivé... :(");
	    getLogger().info("════════════════════════════════════");

	}

	public VanishManager getVanishManager() { return vanishManager; }

	public LuckPermsHook getLuckPermsHook() {
	    return luckPermsHook;
	}

}