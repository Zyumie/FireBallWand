package fr.zyumie.fireballwand;

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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

public class fireballwand implements Listener, CommandExecutor {

	private final String wandName = "§cBâton de Feu";
    private final Main plugin;
    
    public fireballwand(Main plugin) {
        this.plugin = plugin;
        
    }

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player))
			return true;
		if (!player.isOp()) {
			player.sendMessage("§cCommande réservée aux administrateurs.");
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
