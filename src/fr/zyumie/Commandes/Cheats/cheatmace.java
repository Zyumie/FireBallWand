package fr.zyumie.Commandes.Cheats;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;

public class cheatmace implements CommandExecutor {

	@SuppressWarnings("removal")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (!(sender instanceof Player player))
			return true;
		
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        // Nom custom
        meta.setDisplayName(ChatColor.GOLD + "Mace Cheater");

        // Lore
        meta.setLore(List.of(
        	    ChatColor.YELLOW + "∞ AuraFarm"
        	));

        // Enchantements
        meta.addEnchant(Enchantment.WIND_BURST, 1, true);
        meta.addEnchant(Enchantment.DENSITY, 255, true);
        meta.addEnchant(Enchantment.BREACH, 255, true);
        meta.addEnchant(Enchantment.LOOTING, 255, true);

        // Attributs
        meta.addAttributeModifier(Attribute.ARMOR,
            new AttributeModifier(UUID.randomUUID(), "armor", 30, AttributeModifier.Operation.ADD_NUMBER));

        // Unbreakable + cacher infos
        meta.setUnbreakable(true);
        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_UNBREAKABLE
        );

        mace.setItemMeta(meta);
        
        // Le donne au Joueur
		player.getInventory().addItem(mace);
		player.sendMessage("§8La Mace Cheat a été ajouté à ton inventaire.");
		return true;
        		
    	}
    
    
}


