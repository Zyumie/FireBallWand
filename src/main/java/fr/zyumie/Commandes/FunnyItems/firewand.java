package fr.zyumie.Commandes.FunnyItems;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class firewand implements Listener, CommandExecutor {

    private final Main plugin;

    public firewand(Main plugin) {
        this.plugin = plugin;
    }

    private String wandName() {
        return ChatManager.colorize(
                plugin.getCustomCheatConfig().getString("firewand.name", "&cBâton de Feu"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("fireballwand.firewand")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cet item.");
            return true;
        }

        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(wandName());
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

        if (item.getType() != Material.BLAZE_ROD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equals(wandName())) return;

        boolean incendiary = plugin.getCustomCheatConfig().getBoolean("firewand.incendiary", true);
        float yield = (float) plugin.getCustomCheatConfig().getDouble("firewand.yield", 50.0);

        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setIsIncendiary(incendiary);
        fireball.setYield(yield);
    }
}