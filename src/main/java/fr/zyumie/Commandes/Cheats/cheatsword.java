package fr.zyumie.Commandes.Cheats;

import fr.zyumie.Chat.ChatManager;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class cheatsword implements CommandExecutor {

    private final Main plugin;

    public cheatsword(Main plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("removal")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("fireballwand.cheatsword")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cet item.");
            return true;
        }

        String name = ChatManager.colorize(
                plugin.getConfig().getString("cheats.sword.name", "&4Épée Cheater"));

        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(List.of("§c∞ Cheat", "§6∞ Aura", "§d∞ Skills"));

        meta.addEnchant(Enchantment.SHARPNESS, 255, true);
        meta.addEnchant(Enchantment.SMITE, 255, true);
        meta.addEnchant(Enchantment.BANE_OF_ARTHROPODS, 255, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 255, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 255, true);
        meta.addEnchant(Enchantment.LOOTING, 255, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 255, true);
        meta.addEnchant(Enchantment.UNBREAKING, 255, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(UUID.randomUUID(), "damage", 2048, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(UUID.randomUUID(), "speed", 1024, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(UUID.randomUUID(), "armor", 30, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(UUID.randomUUID(), "range", 64, AttributeModifier.Operation.ADD_NUMBER));

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        sword.setItemMeta(meta);
        player.getInventory().addItem(sword);
        player.sendMessage("§cl'Épée Cheat a été ajoutée à ton inventaire.");
        return true;
    }
}
