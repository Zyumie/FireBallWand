package fr.zyumie.Commandes;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.zyumie.fireballwand.Main;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

public class firewand implements Listener, CommandExecutor {

	private final String wandName = "§cBâton de Feu";
    public firewand(Main plugin) {
        
    }

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (!(sender instanceof Player player))
			return true;
		
		// Que celui qui a la Perm ⬇️ peut ce la Give
		if (!player.hasPermission("fireballwand.firewand")) {
		    player.sendMessage("§cTu n’as pas la permission d’utiliser cet item.");
		    return true;
		}

		ItemStack wand = new ItemStack(Material.BLAZE_ROD);
		ItemMeta meta = wand.getItemMeta();
		meta.setDisplayName(wandName);
		meta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		wand.setItemMeta(meta);

		player.getInventory().addItem(wand);
		player.sendMessage("§aBâton de Feu ajouté à ton inventaire.");
		return true;
	}

	@EventHandler
	public void onPlayerUse(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() != Material.BLAZE_ROD)
			return;

		ItemMeta meta = item.getItemMeta();
		if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(wandName)) {
			Fireball fireball = player.launchProjectile(Fireball.class);
			fireball.setIsIncendiary(true);
			fireball.setYield(50F);
		}
	}
}
