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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class cheatmace implements CommandExecutor {

    private final Main plugin;

    public cheatmace(Main plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("removal")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("fireballwand.cheatmace")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cet item.");
            return true;
        }

        String name = ChatManager.colorize(
                plugin.getConfig().getString("cheats.mace.name", "&6Mace Cheater"));

        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(List.of("§e∞ AuraFarm"));

        meta.addEnchant(Enchantment.WIND_BURST, 1, true);
        meta.addEnchant(Enchantment.DENSITY, 255, true);
        meta.addEnchant(Enchantment.BREACH, 255, true);
        meta.addEnchant(Enchantment.LOOTING, 255, true);

        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(UUID.randomUUID(), "armor", 30, AttributeModifier.Operation.ADD_NUMBER));

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        mace.setItemMeta(meta);
        player.getInventory().addItem(mace);
        player.sendMessage("§8La Mace Cheat a été ajoutée à ton inventaire.");
        return true;
    }
}
