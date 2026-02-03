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

public class cheatsword implements CommandExecutor {

	@SuppressWarnings("removal")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (!(sender instanceof Player player))
			return true;
		
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        // Nom custom
        meta.setDisplayName(ChatColor.DARK_RED + "Épée Cheater");

        // Lore
        meta.setLore(List.of(
        	    ChatColor.RED + "∞ Cheat",
        	    ChatColor.GOLD + "∞ Aura",
        	    ChatColor.LIGHT_PURPLE + "∞ Skills"
        	));

        // Enchantements
        meta.addEnchant(Enchantment.SHARPNESS, 255, true);
        meta.addEnchant(Enchantment.SMITE, 255, true);
        meta.addEnchant(Enchantment.BANE_OF_ARTHROPODS, 255, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 255, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 255, true);
        meta.addEnchant(Enchantment.LOOTING, 255, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 255, true);
        meta.addEnchant(Enchantment.UNBREAKING, 255, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        // Attributs
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
            new AttributeModifier(UUID.randomUUID(), "damage", 2048, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED,
            new AttributeModifier(UUID.randomUUID(), "speed", 1024, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ARMOR,
            new AttributeModifier(UUID.randomUUID(), "armor", 30, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE,
            new AttributeModifier(UUID.randomUUID(), "range", 64, AttributeModifier.Operation.ADD_NUMBER));

        // Unbreakable + cacher infos
        meta.setUnbreakable(true);
        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_UNBREAKABLE
        );

        sword.setItemMeta(meta);
        
        // Le donne au Joueur
		player.getInventory().addItem(sword);
		player.sendMessage("§cl'Epée Cheat a été ajouté à ton inventaire.");
		return true;
        		
    	}
    
    
}


