package fr.zyumie.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StartKitManager implements Listener, CommandExecutor {

    private final Main plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public StartKitManager(Main plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getConfig() {
        return plugin.getStartKitConfig();
    }

    // ── GESTION DE LA COMMANDE /STARTERKIT ──────────────────────────────────
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSeul un joueur peut utiliser cette commande.");
            return true;
        }

        if (!player.hasPermission("fireballwand.starterkit")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cette commande.");
            return true;
        }

        if (!getConfig().getBoolean("settings.command-enabled", true)) {
            player.sendMessage("§cLa commande du kit de démarrage est désactivée.");
            return true;
        }

        // Vérification du Cooldown
        long cooldownSeconds = getConfig().getLong("settings.cooldown", 86400L);
        long now = System.currentTimeMillis();

        if (!player.hasPermission("fireballwand.starterkit.bypass")) {
            if (cooldowns.containsKey(player.getUniqueId())) {
                long lastUse = cooldowns.get(player.getUniqueId());
                long timeLeft = (lastUse + (cooldownSeconds * 1000L) - now) / 1000L;

                if (timeLeft > 0) {
                    String msg = getConfig().getString("kit.message-cooldown", "&cVous devez attendre &e{time} &cavants de pouvoir récupérer ce kit à nouveau.")
                            .replace("{time}", formatDuration(timeLeft));
                    player.sendMessage(ChatManager.colorize(msg));
                    return true;
                }
            }
        }

        // Attribution du kit
        giveKit(player);
        cooldowns.put(player.getUniqueId(), now);

        String msg = getConfig().getString("kit.message", "&aVous avez reçu votre &bKit Aventurier &ade départ !");
        player.sendMessage(ChatManager.colorize(msg));

        return true;
    }

    // ── GESTION DE LA PREMIÈRE CONNEXION ──────────────────────────────────────
    @EventHandler
    public void onFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore() && getConfig().getBoolean("settings.give-on-first-join", true)) {
            giveKit(player);
            String msg = getConfig().getString("kit.message", "&aVous avez reçu votre &bKit Aventurier &ade départ !");
            player.sendMessage(ChatManager.colorize(msg));
        }
    }

    // ── DISTRIBUTEUR DU KIT ──────────────────────────────────────────────────
    public void giveKit(Player player) {
        // 1. Armure
        ConfigurationSection armorSec = getConfig().getConfigurationSection("kit.armor");
        if (armorSec != null) {
            if (armorSec.isConfigurationSection("helmet")) player.getInventory().setHelmet(buildItem(armorSec.getConfigurationSection("helmet")));
            if (armorSec.isConfigurationSection("chestplate")) player.getInventory().setChestplate(buildItem(armorSec.getConfigurationSection("chestplate")));
            if (armorSec.isConfigurationSection("leggings")) player.getInventory().setLeggings(buildItem(armorSec.getConfigurationSection("leggings")));
            if (armorSec.isConfigurationSection("boots")) player.getInventory().setBoots(buildItem(armorSec.getConfigurationSection("boots")));
        }

        // 2. Inventaire (Armes & Outils)
        List<Map<?, ?>> items = getConfig().getMapList("kit.items");
        for (Map<?, ?> rawMap : items) {
            ItemStack item = parseItemFromMap(rawMap);
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }
    }

    // ── CRÉATION D'ITEM DEPUIS LE YAML ───────────────────────────────────────
    private ItemStack buildItem(ConfigurationSection section) {
        if (section == null) return null;

        String matName = section.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) return null;

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (section.contains("name")) {
                meta.setDisplayName(ChatManager.colorize(section.getString("name")));
            }
            if (section.contains("lore")) {
                List<String> rawLore = section.getStringList("lore");
                List<String> colorLore = new ArrayList<>();
                for (String l : rawLore) colorLore.add(ChatManager.colorize(l));
                meta.setLore(colorLore);
            }

            List<String> enchants = section.getStringList("enchantments");
            for (String enchStr : enchants) {
                applyEnchantment(meta, enchStr);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private ItemStack parseItemFromMap(Map<?, ?> map) {
        String matName = (String) map.get("material");
        if (matName == null) return null;

        Material mat = Material.matchMaterial(matName);
        if (mat == null) return null;

        int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (map.containsKey("name")) {
                meta.setDisplayName(ChatManager.colorize((String) map.get("name")));
            }
            if (map.containsKey("lore")) {
                List<String> rawLore = (List<String>) map.get("lore");
                List<String> colorLore = new ArrayList<>();
                for (String l : rawLore) colorLore.add(ChatManager.colorize(l));
                meta.setLore(colorLore);
            }

            if (map.containsKey("enchantments")) {
                List<String> enchants = (List<String>) map.get("enchantments");
                for (String enchStr : enchants) {
                    applyEnchantment(meta, enchStr);
                }
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyEnchantment(ItemMeta meta, String enchStr) {
        String[] parts = enchStr.split(":");
        if (parts.length == 2) {
            String enchantName = parts[0].toLowerCase();
            int level = Integer.parseInt(parts[1]);

            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchantName));
            if (ench != null) {
                meta.addEnchant(ench, level, true);
            }
        }
    }

    private String formatDuration(long secs) {
        if (secs <= 0) return "0s";
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}