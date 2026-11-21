package fr.zyumie.Money.Storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Importe les anciennes économies vers plugins/FireBallWand/money-data.yml.
 *
 * Sources reconnues :
 * - plugins/Main ou MainPlugin/data/money.yml : UUID -> balance
 * - plugins/Money/money-data.yml : balances -> UUID -> montant
 * - plugins/Essentials/userdata/UUID.yml : money
 * - plugins/FireBallWand/migration-source.yml : l'un des formats YAML ci-dessus
 *
 * Les sources sont examinées dans l'ordre de priorité. Si plusieurs sources
 * existent, les soldes déjà importés depuis une source prioritaire sont conservés.
 * Les fichiers source ne sont jamais modifiés ; une copie .pre-migration.bak
 * est créée avant l'import.
 */
public final class MoneyMigration {

    private final JavaPlugin plugin;
    private final File targetFile;

    public MoneyMigration(JavaPlugin plugin, File targetFile) {
        this.plugin = plugin;
        this.targetFile = targetFile;
    }

    /** Lance l'import. Retourne le nombre de soldes nouvellement importés. */
    public int run() {
        if (hasTargetData()) {
            plugin.getLogger().info("[Money] Migration ignorée : " + targetFile.getName()
                    + " contient déjà des soldes. Aucune donnée existante ne sera écrasée.");
            return 0;
        }

        YamlConfiguration destination = new YamlConfiguration();
        int totalImported = 0;
        int sourcesUsed = 0;

        // Priorité à l'API MainAPI active : elle lit aussi bien le YAML que MySQL.
        // C'est important lorsque storage.type=MYSQL et qu'aucun money.yml local n'existe.
        Map<UUID, Double> liveMainApiBalances = readMainApiDataStore();
        int importedFromMainApi = 0;
        for (Map.Entry<UUID, Double> entry : liveMainApiBalances.entrySet()) {
            String path = "balances." + entry.getKey();
            if (destination.contains(path)) continue;
            destination.set(path, entry.getValue());
            importedFromMainApi++;
        }
        if (importedFromMainApi > 0) {
            totalImported += importedFromMainApi;
            sourcesUsed++;
            plugin.getLogger().info("[Money] " + importedFromMainApi
                    + " solde(s) importé(s) depuis MainAPI (DataStore YAML/MySQL).");
        }

        for (File source : candidates()) {
            if (source == null || !source.exists()) continue;

            Map<UUID, Double> balances;
            List<File> filesToBackup = new ArrayList<>();

            try {
                if (source.isDirectory()) {
                    balances = readEssentialsDirectory(source, filesToBackup);
                } else if (source.isFile()) {
                    balances = readYamlFile(source);
                    if (!balances.isEmpty()) filesToBackup.add(source);
                } else {
                    continue;
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING,
                        "[Money] Impossible de lire la source " + source.getPath(), ex);
                continue;
            }

            if (balances.isEmpty()) {
                plugin.getLogger().info("[Money] Source ignorée (aucun solde valide) : " + source.getPath());
                continue;
            }

            backup(filesToBackup);

            int importedFromSource = 0;
            for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                String path = "balances." + entry.getKey();
                // La première source valide rencontrée garde la priorité.
                if (destination.contains(path)) continue;
                destination.set(path, entry.getValue());
                importedFromSource++;
            }

            if (importedFromSource > 0) {
                sourcesUsed++;
                totalImported += importedFromSource;
                plugin.getLogger().info("[Money] " + importedFromSource
                        + " solde(s) importé(s) depuis " + source.getPath());
            } else {
                plugin.getLogger().info("[Money] Aucun nouveau solde depuis " + source.getPath()
                        + " (UUID déjà importés depuis une source prioritaire).");
            }
        }

        if (totalImported == 0) {
            plugin.getLogger().info("[Money] Migration : aucune ancienne donnée valide détectée.");
            return 0;
        }

        try {
            saveAtomically(destination);
            plugin.getLogger().info("[Money] Migration terminée : " + totalImported
                    + " solde(s) importé(s) depuis " + sourcesUsed + " source(s). Cible : "
                    + targetFile.getPath());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Money] Échec d'écriture de la cible ; les sources originales sont intactes.", ex);
            return 0;
        }

        return totalImported;
    }


    /**
     * Lit MainAPI par réflexion pour ne pas ajouter une dépendance de compilation.
     * L'ancien MoneyManager utilisait MainAPI.get().getDataStore().load("money", uuid)
     * ; listKeys("money") fonctionne avec YamlDataStore comme avec MySQLDataStore.
     */
    private Map<UUID, Double> readMainApiDataStore() {
        Map<UUID, Double> result = new LinkedHashMap<>();
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("fr.zyumie.main.api.MainAPI", true, loader);
            Object api = apiClass.getMethod("get").invoke(null);
            if (api == null) return result;

            Method getDataStore = apiClass.getMethod("getDataStore");
            Object store = getDataStore.invoke(api);
            if (store == null) return result;

            Class<?> storeType = getDataStore.getReturnType();
            Method listKeys = storeType.getMethod("listKeys", String.class);
            Method load = storeType.getMethod("load", String.class, String.class);
            Object keysObject = listKeys.invoke(store, "money");
            if (!(keysObject instanceof Iterable<?> keys)) return result;

            for (Object keyObject : keys) {
                if (keyObject == null) continue;
                String key = keyObject.toString();
                UUID uuid = parseUuid(key);
                if (uuid == null) continue;

                Object raw = load.invoke(store, "money", key);
                if (!(raw instanceof Map<?, ?> values)) continue;
                Double amount = validAmount(values.get("balance"));
                if (amount != null) result.putIfAbsent(uuid, amount);
            }
        } catch (ClassNotFoundException ignored) {
            // Pas d'ancienne MainAPI dans ce serveur : les imports par fichiers prendront le relais.
        } catch (NoSuchMethodException ex) {
            plugin.getLogger().warning("[Money] MainAPI détectée, mais son DataStore ne correspond pas "
                    + "à l'ancienne API attendue (listKeys/load). Import par fichiers uniquement.");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    "[Money] Lecture de MainAPI impossible. Vérifie que son DataStore est initialisé "
                            + "avant MoneyModule.enable() ; les imports par fichiers vont continuer.", ex);
        }
        return result;
    }

    private boolean hasTargetData() {
        if (!targetFile.isFile()) return false;
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(targetFile);
            ConfigurationSection balances = cfg.getConfigurationSection("balances");
            return balances != null && !balances.getKeys(false).isEmpty();
        } catch (Exception ex) {
            plugin.getLogger().warning("[Money] Impossible de vérifier la cible de migration : "
                    + ex.getMessage() + ". Par sécurité, aucune importation ne sera faite.");
            return true;
        }
    }

    private List<File> candidates() {
        List<File> result = new ArrayList<>();
        File pluginsDir = plugin.getDataFolder().getAbsoluteFile().getParentFile();
        if (pluginsDir == null) return result;

        // Ancien stockage MainAPI : <UUID>.balance dans data/money.yml
        for (String folder : new String[]{
                "Main", "MainPlugin", "main", "mainplugin", "MainAPI", "MainApi"
        }) {
            result.add(new File(new File(pluginsDir, folder), "data/money.yml"));
        }

        // Ancien plugin Money autonome.
        result.add(new File(new File(pluginsDir, "Money"), "money-data.yml"));
        result.add(new File(new File(pluginsDir, "Money"), "data/money.yml"));

        // Essentials / EssentialsX : un fichier par UUID.
        result.add(new File(new File(pluginsDir, "Essentials"), "userdata"));

        // Import manuel, placé dans le dossier de données de FireBallWand.
        result.add(new File(plugin.getDataFolder(), "migration-source.yml"));
        return result;
    }

    /** Lit les deux formats fichier : balances.UUID = nombre, ou UUID.balance = nombre. */
    private Map<UUID, Double> readYamlFile(File file) {
        YamlConfiguration source = YamlConfiguration.loadConfiguration(file);
        Map<UUID, Double> result = new LinkedHashMap<>();

        ConfigurationSection balances = source.getConfigurationSection("balances");
        if (balances != null) {
            for (String key : balances.getKeys(false)) {
                UUID uuid = parseUuid(key);
                Double amount = validAmount(balances.get(key));
                if (uuid != null && amount != null) result.putIfAbsent(uuid, amount);
            }
        }

        // Format MainAPI : <uuid>:\n  balance: 123.0
        for (String key : source.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid == null) continue;

            ConfigurationSection playerSection = source.getConfigurationSection(key);
            if (playerSection == null || !playerSection.contains("balance")) continue;

            Double amount = validAmount(playerSection.get("balance"));
            if (amount != null) result.putIfAbsent(uuid, amount);
        }

        return result;
    }

    private Map<UUID, Double> readEssentialsDirectory(File userdataDir, List<File> filesToBackup) {
        Map<UUID, Double> result = new LinkedHashMap<>();
        File[] files = userdataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return result;

        java.util.Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            String filename = file.getName();
            String uuidText = filename.substring(0, filename.length() - 4);
            UUID uuid = parseUuid(uuidText);
            if (uuid == null) continue;

            YamlConfiguration source = YamlConfiguration.loadConfiguration(file);
            Double amount = validAmount(source.get("money"));
            if (amount == null) continue;

            result.putIfAbsent(uuid, amount);
            filesToBackup.add(file);
        }
        return result;
    }

    private void backup(List<File> files) {
        for (File source : files) {
            try {
                File backup = new File(source.getParentFile(), source.getName() + ".pre-migration.bak");
                Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("[Money] Backup créé : " + backup.getPath());
            } catch (IOException ex) {
                // Les fichiers source ne sont pas modifiés par l'import ; on peut continuer sans backup.
                plugin.getLogger().warning("[Money] Backup impossible pour " + source.getPath()
                        + " : " + ex.getMessage());
            }
        }
    }

    private void saveAtomically(YamlConfiguration destination) throws IOException {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Impossible de créer le dossier " + parent.getPath());
        }

        File temporary = new File(parent, targetFile.getName() + ".tmp");
        destination.save(temporary);
        try {
            Files.move(temporary.toPath(), targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Double validAmount(Object raw) {
        if (!(raw instanceof Number number)) return null;
        double value = number.doubleValue();
        return Double.isFinite(value) && value >= 0.0 ? value : null;
    }
}
