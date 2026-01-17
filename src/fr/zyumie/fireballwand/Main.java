package fr.zyumie.fireballwand;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		// FireballWand
	    fireballwand fireballwand = new fireballwand(this);
	    getServer().getPluginManager().registerEvents(fireballwand, this);
	    getCommand("firewand").setExecutor(fireballwand);

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
	}
}