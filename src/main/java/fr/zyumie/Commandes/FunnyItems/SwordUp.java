package fr.zyumie.Commandes.FunnyItems;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Sword Up : l'épée donne de la lévitation au joueur touché.
 * Les OP (et fireballwand.swordup.bypass) ne sont pas affectés.
 */
public class SwordUp implements CommandExecutor, Listener {

    private static final String GIVE_PERMISSION = "fireballwand.swordup";
    private static final String BYPASS_PERMISSION = "fireballwand.swordup.bypass";

    private final Main plugin;
    private final NamespacedKey key;

    public SwordUp(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "sword_up");
    }

    private FileConfiguration cfg() {
        return plugin.getCustomCheatConfig();
    }

    private String msg(String path, String def) {
        return ChatManager.colorize(cfg().getString("sword-up.messages." + path, def));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }
        if (!player.hasPermission(GIVE_PERMISSION)) {
            player.sendMessage(msg("no-permission", "&c❌ Tu n'as pas la permission d'utiliser cette commande."));
            return true;
        }

        FunnyItemUtil.give(player, createItem());
        player.sendMessage(msg("given", "&aTu as reçu la &dSword Up &a!"));
        return true;
    }

    private ItemStack createItem() {
        Material material = Material.matchMaterial(cfg().getString("sword-up.material", "IRON_SWORD"));
        if (material == null || !Tag.ITEMS_SWORDS.isTagged(material)) material = Material.IRON_SWORD;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(FunnyItemUtil.text(cfg().getString("sword-up.name", "&dSword Up")));
        List<String> lore = cfg().getStringList("sword-up.lore");
        if (!lore.isEmpty()) meta.lore(FunnyItemUtil.lore(lore));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        // Pré-filtre bon marché : seules les épées avec métadonnées sont examinées
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        if (!Tag.ITEMS_SWORDS.isTagged(hand.getType()) || !hand.hasItemMeta()) return;
        if (!hand.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

        if (FunnyItemUtil.isImmune(target, cfg().getBoolean("sword-up.ignore-ops", true), BYPASS_PERMISSION)) {
            attacker.sendMessage(msg("immune", "&c❌ &f%player% &cest immunisé contre la Sword Up.")
                    .replace("%player%", target.getName()));
            return;
        }

        int duration = Math.max(1, cfg().getInt("sword-up.duration-ticks", 100));
        int amplifier = Math.max(0, cfg().getInt("sword-up.amplifier", 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, amplifier, false, true, true));

        target.sendMessage(msg("hit", "&d⬆ Tu as été propulsé dans les airs par une Sword Up !"));
    }
}
