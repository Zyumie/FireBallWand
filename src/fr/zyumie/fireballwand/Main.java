package fr.zyumie.fireballwand;

import org.bukkit.entity.Player;
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

import org.bukkit.command.CommandExecutor;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

	private LuckPermsHook luckPermsHook;
	
	@Override
	public void onEnable() {
		
		// FireballWand
	    firewand firewand = new firewand(this);
	    getCommand("firewand").setExecutor(firewand);

		// Kick armor
		kick_armor kickArmor = new kick_armor(this);
		getCommand("kickarmor").setExecutor(kickArmor);

		// Freeze wand
		freezewand freezeWand = new freezewand(this);
		getCommand("freezewand").setExecutor((sender, command, label, args) -> {
			if (sender instanceof Player player)
				freezeWand.giveWand(player);
		
			return true;
		});
		
		// Listener
		getServer().getPluginManager().registerEvents(this, this);
	    getServer().getPluginManager().registerEvents(firewand, this);
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
			

	}
	
	
	public LuckPermsHook getLuckPermsHook() {
	    return luckPermsHook;
	}

	
}