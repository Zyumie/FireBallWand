package fr.zyumie.Commandes;

import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class PotionOfDeath implements CommandExecutor, Listener {

    private final Main plugin;
    private final NamespacedKey potionKey;
    private final NamespacedKey potionDeathKey; // Tag temporaire pour activer le Keep Inventory à la mort

    public PotionOfDeath(Main plugin) {
        this.plugin = plugin;
        this.potionKey = new NamespacedKey(plugin, "potion_of_death");
        this.potionDeathKey = new NamespacedKey(plugin, "potion_death_tag");
    }

    // ── Commande /potionofdeath pour se give l'item ────────────────────────
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }

        if (!player.hasPermission("fireballwand.potion-of-death")) {
            String noPermMsg = plugin.getConfig().getString("potion-of-death.messages.no-permission", "&c❌ Tu n'as pas la permission d'utiliser cette commande.");
            player.sendMessage(formatColor(noPermMsg));
            return true;
        }

        player.getInventory().addItem(createPotionItem());
        String givenMsg = plugin.getConfig().getString("potion-of-death.messages.given", "&aTu as reçu la Potion of Death !");
        player.sendMessage(formatColor(givenMsg));
        return true;
    }

    // ── Création de l'item unique ──────────────────────────────────────────
    private ItemStack createPotionItem() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            String name = plugin.getConfig().getString("potion-of-death.name", "&4&l☠ Potion of Death ☠");
            meta.setDisplayName(formatColor(name));

            List<String> lore = plugin.getConfig().getStringList("potion-of-death.lore");
            List<String> formattedLore = new ArrayList<>();
            for (String line : lore) {
                formattedLore.add(formatColor(line));
            }
            meta.setLore(formattedLore);

            // Couleur rouge sang du liquide
            meta.setColor(Color.fromRGB(150, 0, 0));

            // Masquer "Sans effet" et les détails superflus
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

            // Tag persistant pour identifier l'item de manière sécurisée
            meta.getPersistentDataContainer().set(potionKey, PersistentDataType.BYTE, (byte) 1);

            potion.setItemMeta(meta);
        }
        return potion;
    }

    // ── Gestion de l'effet quand la potion s'écrase ────────────────────────
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ItemStack item = event.getPotion().getItem();

        // Vérifier si la potion lancée possède bien notre tag persistant
        boolean isOurPotion = item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer().has(potionKey, PersistentDataType.BYTE);

        if (!isOurPotion) return;

        // Toucher tous les joueurs affectés par la zone d'impact de la splash potion
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;

            // Protection : Ne tue pas les OP ou ceux qui ont la permission bypass
            if (target.isOp() || target.hasPermission("fireballwand.potion-of-death.bypass")) {
                String bypassMsg = plugin.getConfig().getString("potion-of-death.messages.bypass", "&a🛡️ Tu as résisté à la Potion of Death grâce à tes privilèges !");
                target.sendMessage(formatColor(bypassMsg));
                continue;
            }

            // Marquer la victime pour activer le KeepInventory à sa mort
            target.getPersistentDataContainer().set(potionDeathKey, PersistentDataType.BYTE, (byte) 1);

            // Message envoyé à la victime
            String victimMsg = plugin.getConfig().getString("potion-of-death.messages.victim-killed", "&c⚡ Tu as été exterminé par une Potion of Death !");
            target.sendMessage(formatColor(victimMsg));

            // Message global dans le chat (si activé dans la config)
            boolean broadcastEnabled = plugin.getConfig().getBoolean("potion-of-death.broadcast.enabled", true);
            if (broadcastEnabled) {
                String broadcastMsg = plugin.getConfig().getString("potion-of-death.messages.broadcast-killed", "&4☠ %player% a été terrassé par une Potion of Death !")
                        .replace("%player%", target.getName());
                Bukkit.broadcastMessage(formatColor(broadcastMsg));
            }

            // --- INSTANT KILL ---
            target.setHealth(0.0);
        }
    }

    // ── GESTION DU KEEP INVENTORY COMPLET ──────────────────────────────────
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        if (pdc.has(potionDeathKey, PersistentDataType.BYTE)) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            pdc.remove(potionDeathKey); // Retire le tag temporaire
        }
    }

    private String formatColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}