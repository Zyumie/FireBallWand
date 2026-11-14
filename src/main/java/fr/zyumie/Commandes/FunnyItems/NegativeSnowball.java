package fr.zyumie.Commandes.FunnyItems;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * Snowball à effets négatifs : les effets de CustomCheat.yml (negative-snowball.effects)
 * sont appliqués au joueur touché. Les OP (et fireballwand.negativesnowball.bypass) sont immunisés.
 */
public class NegativeSnowball implements CommandExecutor, Listener {

    private static final String GIVE_PERMISSION = "fireballwand.negativesnowball";
    private static final String BYPASS_PERMISSION = "fireballwand.negativesnowball.bypass";

    private final Main plugin;
    private final NamespacedKey key;

    public NegativeSnowball(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "negative_snowball");
    }

    private FileConfiguration cfg() {
        return plugin.getCustomCheatConfig();
    }

    private String msg(String path, String def) {
        return ChatManager.colorize(cfg().getString("negative-snowball.messages." + path, def));
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
        player.sendMessage(msg("given", "&aTu as reçu des &bSnowballs Maudites &a!"));
        return true;
    }

    private ItemStack createItem() {
        int amount = Math.max(1, Math.min(16, cfg().getInt("negative-snowball.amount", 16)));
        ItemStack item = new ItemStack(Material.SNOWBALL, amount);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(FunnyItemUtil.text(cfg().getString("negative-snowball.name", "&bSnowball Maudite")));
        List<String> lore = cfg().getStringList("negative-snowball.lore");
        if (!lore.isEmpty()) meta.lore(FunnyItemUtil.lore(lore));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        // Sorties rapides : ProjectileHitEvent concerne aussi les flèches, tridents...
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!(event.getHitEntity() instanceof Player target)) return;

        ItemStack item = snowball.getItem();
        if (!item.hasItemMeta()
                || !item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

        Player thrower = snowball.getShooter() instanceof Player p ? p : null;

        if (FunnyItemUtil.isImmune(target, cfg().getBoolean("negative-snowball.ignore-ops", true), BYPASS_PERMISSION)) {
            target.sendMessage(msg("bypass", "&a🛡 Tu as résisté à la Snowball Maudite grâce à tes privilèges !"));
            if (thrower != null) {
                thrower.sendMessage(msg("immune", "&c❌ &f%player% &cest immunisé.")
                        .replace("%player%", target.getName()));
            }
            return;
        }

        for (String raw : cfg().getStringList("negative-snowball.effects")) {
            PotionEffect effect = FunnyItemUtil.parseEffect(raw, plugin.getLogger());
            if (effect != null) target.addPotionEffect(effect);
        }

        target.sendMessage(msg("hit", "&c❄ Tu as été touché par une Snowball Maudite !"));
        if (thrower != null) {
            thrower.sendMessage(msg("hit-sender", "&bTu as maudit &f%player% &b!")
                    .replace("%player%", target.getName()));
        }
    }
}
