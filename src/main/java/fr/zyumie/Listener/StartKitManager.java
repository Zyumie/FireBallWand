package fr.zyumie.Listener;

import fr.zyumie.Chat.ChatManager;
import fr.zyumie.fireballwand.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StartKitManager implements Listener, CommandExecutor, TabCompleter {

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!getConfig().getBoolean("settings.command-enabled", true)) {
            sender.sendMessage(ChatManager.colorize("&cLa commande du kit de démarrage est désactivée."));
            return true;
        }

        // Calcul du nombre total d'items dans le kit (Armure (4) + Items de la liste)
        int totalItemsCount = 4;
        List<?> items = getConfig().getMapList("kit.items");
        if (items != null) {
            totalItemsCount += items.size();
        }

        // ── CAS 1 : /starterkit <joueur> (Admins / Console) ──
        if (args.length >= 1) {
            if (!sender.hasPermission("fireballwand.starterkit.others") && !sender.hasPermission("fireballwand.admin")) {
                sender.sendMessage(ChatManager.colorize("&cTu n'as pas la permission de donner un kit à un autre joueur."));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatManager.colorize("&cJoueur introuvable ou hors ligne : &f" + args[0]));
                return true;
            }

            if (!hasEnoughInventorySpace(target, totalItemsCount)) {
                sender.sendMessage(ChatManager.colorize("&cL'inventaire de &e" + target.getName() + " &cest trop plein !"));
                return true;
            }

            giveKit(target);

            String kitMsg = getConfig().getString("kit.message", "&aVous avez reçu votre &bKit Aventurier &ade départ !");
            target.sendMessage(ChatManager.colorize(kitMsg));

            sender.sendMessage(ChatManager.colorize("&aTu as donné le kit de démarrage à &e" + target.getName() + "&a."));
            return true;
        }

        // ── CAS 2 : /starterkit (Pour soi-même) ─────────────────────────
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatManager.colorize("&cUtilisation : /starterkit <joueur>"));
            return true;
        }

        if (!player.hasPermission("fireballwand.starterkit")) {
            player.sendMessage(ChatManager.colorize("&cTu n'as pas la permission d'utiliser cette commande."));
            return true;
        }

        if (!hasEnoughInventorySpace(player, totalItemsCount)) {
            player.sendMessage(ChatManager.colorize("&cTon inventaire est trop plein ! Fais de la place avant de récupérer ton kit."));
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

    // ── AUTO-COMPLÉTION DES PSEUDOS ─────────────────────────────────────────
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && (sender.hasPermission("fireballwand.starterkit.others") || sender.hasPermission("fireballwand.admin"))) {
            List<String> completions = new ArrayList<>();
            String current = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(current)) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }
        return Collections.emptyList();
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

    // ── VÉRIFICATION DE LA PLACE DANS L'INVENTAIRE ────────────────────────────
    private boolean hasEnoughInventorySpace(Player player, int requiredSlots) {
        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                freeSlots++;
            }
        }
        return freeSlots >= requiredSlots;
    }

    // ── DISTRIBUTEUR DU KIT (HYBRIDE DÉTECTION ARMURE) ───────────────────────
    public void giveKit(Player player) {
        ConfigurationSection armorSec = getConfig().getConfigurationSection("kit.armor");
        if (armorSec != null) {
            handleArmorPiece(player, buildItem(armorSec.getConfigurationSection("helmet")), ArmorSlot.HELMET);
            handleArmorPiece(player, buildItem(armorSec.getConfigurationSection("chestplate")), ArmorSlot.CHESTPLATE);
            handleArmorPiece(player, buildItem(armorSec.getConfigurationSection("leggings")), ArmorSlot.LEGGINGS);
            handleArmorPiece(player, buildItem(armorSec.getConfigurationSection("boots")), ArmorSlot.BOOTS);
        }

        List<?> items = getConfig().getMapList("kit.items");
        if (items != null) {
            for (Object obj : items) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<?, ?> rawMap = (Map<?, ?>) obj;
                    ItemStack item = parseItemFromMap(rawMap);
                    if (item != null) {
                        addItemToInventorySafe(player, item);
                    }
                }
            }
        }
    }

    private enum ArmorSlot { HELMET, CHESTPLATE, LEGGINGS, BOOTS }

    private void handleArmorPiece(Player player, ItemStack kitArmorPiece, ArmorSlot slot) {
        if (kitArmorPiece == null) return;

        switch (slot) {
            case HELMET:
                if (isSlotEmpty(player.getInventory().getHelmet())) {
                    player.getInventory().setHelmet(kitArmorPiece);
                } else {
                    addItemToInventorySafe(player, kitArmorPiece);
                }
                break;
            case CHESTPLATE:
                if (isSlotEmpty(player.getInventory().getChestplate())) {
                    player.getInventory().setChestplate(kitArmorPiece);
                } else {
                    addItemToInventorySafe(player, kitArmorPiece);
                }
                break;
            case LEGGINGS:
                if (isSlotEmpty(player.getInventory().getLeggings())) {
                    player.getInventory().setLeggings(kitArmorPiece);
                } else {
                    addItemToInventorySafe(player, kitArmorPiece);
                }
                break;
            case BOOTS:
                if (isSlotEmpty(player.getInventory().getBoots())) {
                    player.getInventory().setBoots(kitArmorPiece);
                } else {
                    addItemToInventorySafe(player, kitArmorPiece);
                }
                break;
        }
    }

    private boolean isSlotEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private void addItemToInventorySafe(Player player, ItemStack item) {
        if (item == null) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

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
                List<String> getEnchants = (List<String>) map.get("enchantments");
                if (getEnchants != null) {
                    for (String enchStr : getEnchants) {
                        applyEnchantment(meta, enchStr);
                    }
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