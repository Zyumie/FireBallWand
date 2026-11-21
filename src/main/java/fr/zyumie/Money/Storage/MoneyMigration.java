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
 * Importe les anciennes économies vers plugins/FireBallWand/data/money.yml.
 *
 * Sources reconnues :
 * - plugins/FireBallWand/money-data.yml : ancien fichier de stockage FireBallWand
 * - MainAPI (DataStore YAML ou MySQL), puis les fichiers data/money.yml de Main ou MainPlugin
 * - plugins/Money/money-data.yml et plugins/Money/data/money.yml
 * - plugins/Essentials/userdata/UUID.yml : money
 * - plugins/FireBallWand/migration-source.yml : l'un des formats YAML ci-dessus
 *
 * Les soldes déjà présents dans la cible sont prioritaires. Pour chaque UUID
 * absent, la première source valide rencontrée fournit le solde. Les sources
 * ne sont jamais supprimées ; une copie .pre-migration.bak est conservée.
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
        // Charger la cible existante pour fusionner les données manquantes sans
        // jamais écraser un solde déjà présent dans data/money.yml.
        YamlConfiguration destination = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        ConfigurationSection existingBalances = destination.getConfigurationSection("balances");
        int existingCount = existingBalances == null ? 0 : existingBalances.getKeys(false).size();
        if (existingCount > 0) {
            plugin.getLogger().info("[Money] " + existingCount
                    + " solde(s) déjà présents dans la nouvelle cible ; ils seront conservés.");
        }

        int totalImported = 0;
        int sourcesUsed = 0;

        File previousFireBallWandFile = new File(plugin.getDataFolder(), "money-data.yml");
        int importedFromPreviousFile = importYamlSource(previousFireBallWandFile, destination);
        if (importedFromPreviousFile > 0) {
            totalImported += importedFromPreviousFile;
            sourcesUsed++;
        }

        Map<UUID, Double> liveMainApiBalances = readMainApiDataStore();
        int importedFromMainApi = mergeMissingBalances(destination, liveMainApiBalances);
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
            int importedFromSource = mergeMissingBalances(destination, balances);
            if (importedFromSource > 0) {
                sourcesUsed++;
                totalImported += importedFromSource;
                plugin.getLogger().info("[Money] " + importedFromSource
                        + " solde(s) importé(s) depuis " + source.getPath());
            } else {
                plugin.getLogger().info("[Money] Aucun nouveau solde depuis " + source.getPath()
                        + " (UUID déjà présents dans la cible ou une source prioritaire).");
            }
        }

        if (totalImported == 0) {
            plugin.getLogger().info("[Money] Migration : aucun nouveau solde à importer. Cible : "
                    + targetFile.getPath());
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

    /** Importe un fichier YAML sans remplacer les UUID déjà présents dans la cible. */
    private int importYamlSource(File source, YamlConfiguration destination) {
        if (source == null || !source.isFile()) return 0;
        try {
            Map<UUID, Double> balances = readYamlFile(source);
            if (balances.isEmpty()) {
                plugin.getLogger().info("[Money] Ancien fichier FireBallWand absent de soldes valides : "
                        + source.getPath());
                return 0;
            }
            backup(List.of(source));
            int imported = mergeMissingBalances(destination, balances);
            plugin.getLogger().info("[Money] Ancien fichier FireBallWand : " + imported + "/"
                    + balances.size() + " solde(s) importé(s) depuis " + source.getPath() + ".");
            return imported;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    "[Money] Impossible de lire l'ancien fichier FireBallWand " + source.getPath(), ex);
            return 0;
        }
    }

    /** Ajoute uniquement les UUID absents ; les valeurs de la cible sont prioritaires. */
    private int mergeMissingBalances(YamlConfiguration destination, Map<UUID, Double> balances) {
        int imported = 0;
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            String path = "balances." + entry.getKey();
            Double amount = validAmount(entry.getValue());
            if (amount == null) continue;

            if (validAmount(destination.get(path)) != null) continue;
            destination.set(path, amount);
            imported++;
        }
        return imported;
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
