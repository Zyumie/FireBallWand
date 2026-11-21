package fr.zyumie.Money.Storage;

import java.util.Map;
import java.util.UUID;

/** Abstraction du stockage des soldes (YAML aujourd'hui, autre demain). */
public interface MoneyStorage {

    /** Renvoie null si le joueur n'a jamais été enregistré. */
    Double get(UUID uuid);

    /** Persiste une valeur (peut être bufferisé en mémoire + flush différé). */
    void put(UUID uuid, double value);

    /** Vue lecture seule de tous les soldes connus (pour /moneytop). */
    Map<UUID, Double> all();

    /** Force l'écriture sur disque. */
    void flush();

    /** Charge en RAM depuis le disque (idempotent). */
    default void load() {}
}