package fr.zyumie.fireballwand;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class kick_armor implements Listener, CommandExecutor {

	private final NamespacedKey armorKey;

	public kick_armor(JavaPlugin plugin) {
		this.armorKey = new NamespacedKey(plugin, "armor_id");
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player wearer = event.getPlayer();

		if (event.getFrom().getBlockX() == event.getTo().getBlockX()
				&& event.getFrom().getBlockY() == event.getTo().getBlockY()
				&& event.getFrom().getBlockZ() == event.getTo().getBlockZ())
			return;

		ItemStack chest = wearer.getInventory().getChestplate();
		if (chest == null || chest.getType() != Material.DIAMOND_CHESTPLATE)
			return;

		ItemMeta meta = chest.getItemMeta();
		if (meta == null)
			return;

		PersistentDataContainer data = meta.getPersistentDataContainer();
		String id = data.get(armorKey, PersistentDataType.STRING);
		if (id == null || !id.equals("KICK_ARMOR"))
			return;

		wearer.getWorld().getPlayers().forEach(p -> {
			if (p.equals(wearer))
				return;
			if (p.isOp())
				return;
			if (p.getLocation().distanceSquared(wearer.getLocation()) <= 9) {
				p.kickPlayer("Trop près d’un joueur protégé.");
			}
		});
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Seul un joueur peut utiliser cette commande.");
			return true;
		}
		if (!player.isOp()) {
			player.sendMessage("§cCommande réservée aux administrateurs.");
			return true;
		}

		ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
		ItemMeta meta = chest.getItemMeta();
		if (meta != null) {
			meta.setDisplayName("§cPlastron du Kick");
			meta.getPersistentDataContainer().set(armorKey, PersistentDataType.STRING, "KICK_ARMOR");
			chest.setItemMeta(meta);
		}

		player.getInventory().addItem(chest);
		player.sendMessage("§aTu as reçu le plastron spécial !");
		return true;
	}
}
