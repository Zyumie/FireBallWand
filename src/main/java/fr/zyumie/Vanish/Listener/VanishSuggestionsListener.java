package fr.zyumie.Vanish.Listener;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import fr.zyumie.fireballwand.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Retire les pseudos des joueurs en vanish de l'auto-complétion de TOUTES les commandes
 * (/tpa, /tpahere, /msg, /tp, commandes d'autres plugins...), pour les joueurs qui n'ont
 * pas le niveau de les voir. Les vanishés de niveau inférieur ou égal restent visibles.
 *
 * Cet évènement peut être appelé sur un thread async : on n'utilise que des structures
 * thread-safe (VanishManager#getHiddenNamesFor).
 */
public class VanishSuggestionsListener implements Listener {

    private final Main plugin;

    public VanishSuggestionsListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSuggestions(AsyncPlayerSendSuggestionsEvent event) {
        if (!plugin.getVanishConfig().getBoolean("tab-complete.hide-vanished", true)) return;

        Set<String> hidden = plugin.getVanishManager().getHiddenNamesFor(event.getPlayer());
        if (hidden.isEmpty()) return;

        Suggestions original = event.getSuggestions();
        List<Suggestion> kept = new ArrayList<>(original.getList().size());

        for (Suggestion suggestion : original.getList()) {
            if (!hidden.contains(suggestion.getText().toLowerCase())) {
                kept.add(suggestion);
            }
        }

        if (kept.size() != original.getList().size()) {
            event.setSuggestions(new Suggestions(original.getRange(), kept));
        }
    }
}
