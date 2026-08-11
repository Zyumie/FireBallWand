package fr.zyumie.Commandes.Cheats;

import fr.zyumie.Listener.ChatManager;
import fr.zyumie.fireballwand.Main;
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
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.List;
import java.util.UUID;

public class diamond_armorcheat implements CommandExecutor {

    private final Main plugin;

    public diamond_armorcheat(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("fireballwand.diamond_armorcheat")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cet item.");
            return true;
        }

        player.getInventory().addItem(createArmor(Material.DIAMOND_HELMET));
        player.getInventory().addItem(createArmor(Material.DIAMOND_CHESTPLATE));
        player.getInventory().addItem(createArmor(Material.DIAMOND_LEGGINGS));
        player.getInventory().addItem(createArmor(Material.DIAMOND_BOOTS));

        player.sendMessage("§cL'Armure en Diamant Cheat complète a été ajoutée à ton inventaire.");
        return true;
    }

    @SuppressWarnings("removal")
    private ItemStack createArmor(Material material) {
        String name = ChatManager.colorize(
                plugin.getConfig().getString("cheats.armor.name", "&4Armor Cheat"));

        ItemStack item = new ItemStack(material);
        ArmorMeta meta = (ArmorMeta) item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(List.of("§c∞ Cheat", "§6∞ Aura", "§d∞ Skills"));

        meta.addEnchant(Enchantment.PROTECTION, 255, true);
        meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 255, true);
        meta.addEnchant(Enchantment.BLAST_PROTECTION, 255, true);
        meta.addEnchant(Enchantment.FIRE_PROTECTION, 255, true);
        meta.addEnchant(Enchantment.THORNS, 255, true);
        meta.addEnchant(Enchantment.UNBREAKING, 255, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        if (material == Material.DIAMOND_HELMET) {
            meta.addEnchant(Enchantment.RESPIRATION, 255, true);
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        }
        if (material == Material.DIAMOND_BOOTS) {
            meta.addEnchant(Enchantment.FEATHER_FALLING, 255, true);
            meta.addEnchant(Enchantment.DEPTH_STRIDER, 255, true);
            meta.addEnchant(Enchantment.FROST_WALKER, 255, true);
            meta.addEnchant(Enchantment.SOUL_SPEED, 255, true);
        }

        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(UUID.randomUUID(), "armor", 30, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS,
                new AttributeModifier(UUID.randomUUID(), "toughness", 20, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.MAX_HEALTH,
                new AttributeModifier(UUID.randomUUID(), "health", 1024, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(UUID.randomUUID(), "damage", 2048, AttributeModifier.Operation.ADD_NUMBER));

        meta.setTrim(new ArmorTrim(TrimMaterial.REDSTONE, TrimPattern.FLOW));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ARMOR_TRIM);

        item.setItemMeta(meta);
        return item;
    }
}
