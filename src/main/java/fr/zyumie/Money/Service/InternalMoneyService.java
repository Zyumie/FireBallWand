package fr.zyumie.Money.Service;

import fr.zyumie.Money.MoneyConfig;
import fr.zyumie.Money.Storage.MoneyStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InternalMoneyService implements MoneyService {

    private final MoneyConfig config;
    private final MoneyStorage storage;
    private final Map<UUID, Double> cache = new ConcurrentHashMap<>();

    private volatile List<Map.Entry<String, Double>> topCache;
    private final AtomicLong topCacheAt = new AtomicLong(0L);

    public InternalMoneyService(MoneyConfig config, MoneyStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    @Override
    public void touch(UUID uuid) {
        if (!cache.containsKey(uuid)) {
            Double stored = storage.get(uuid);
            cache.put(uuid, stored != null ? sanitize(stored) : config.startingBalance());
            if (stored == null) storage.put(uuid, cache.get(uuid));
        }
    }

    @Override public double getBalance(UUID uuid) { touch(uuid); return cache.getOrDefault(uuid, 0.0); }
    @Override public boolean has(UUID uuid, double amount) { return isValidAmount(amount) && getBalance(uuid) >= amount; }

    @Override
    public boolean deposit(UUID uuid, double amount) {
        if (!isValidAmount(amount)) return false;
        double current = getBalance(uuid);
        double max = config.maxBalance();
        if (current + amount > max) return false;
        cache.put(uuid, current + amount);
        storage.put(uuid, cache.get(uuid));
        topCache = null;
        return true;
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (!isValidAmount(amount)) return false;
        double current = getBalance(uuid);
        if (current < amount) return false;
        cache.put(uuid, current - amount);
        storage.put(uuid, cache.get(uuid));
        topCache = null;
        return true;
    }

    @Override
    public void set(UUID uuid, double amount) {
        if (!Double.isFinite(amount)) return;
        cache.put(uuid, Math.max(0, Math.min(config.maxBalance(), amount)));
        storage.put(uuid, cache.get(uuid));
        topCache = null;
    }

    @Override
    public List<Map.Entry<String, Double>> getTop(int limit) {
        long ttl = config.topCacheSeconds() * 1000L;
        long now = System.currentTimeMillis();
        List<Map.Entry<String, Double>> cached = topCache;
        if (cached != null && ttl > 0 && (now - topCacheAt.get()) < ttl) {
            return cached.size() > limit ? cached.subList(0, limit) : cached;
        }
        // Fusionne cache mémoire + disque
        Map<String, Double> all = new LinkedHashMap<>();
        Set<UUID> seen = new HashSet<>(cache.keySet());
        for (UUID uuid : cache.keySet()) all.put(nameOf(uuid), cache.get(uuid));
        for (Map.Entry<UUID, Double> e : storage.all().entrySet()) {
            if (seen.contains(e.getKey())) continue;
            all.put(nameOf(e.getKey()), sanitize(e.getValue()));
        }
        List<Map.Entry<String, Double>> sorted = all.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();
        topCache = sorted;
        topCacheAt.set(now);
        return sorted.size() > limit ? sorted.subList(0, limit) : sorted;
    }

    @Override public void flush() { storage.flush(); }

    private static boolean isValidAmount(double v) { return Double.isFinite(v) && v >= 0; }
    private static double sanitize(double v) { return Double.isFinite(v) && v >= 0 ? v : 0.0; }

    private static String nameOf(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }
}