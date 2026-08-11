package fr.zyumie.SoftDepend;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;

/**
 * Hook ItemsAdder (soft-depend).
 * Permet de résoudre les placeholders :namespace:icon: dans les préfixes/suffixes
 * pour afficher des images/glyphes dans le chat.
 *
 * ItemsAdder expose FontImageWrapper.replaceFontImages(String) qui convertit
 * automatiquement les balises :namespace:icon: en caractères spéciaux.
 */
public class ItemsAdderHook {

    private final boolean available;

    public ItemsAdderHook() {
        boolean ok;
        try {
            Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            ok = true;
        } catch (ClassNotFoundException e) {
            ok = false;
        }
        this.available = ok;
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Remplace les balises :namespace:icon: par les glyphes ItemsAdder.
     * Retourne le texte inchangé si ItemsAdder n'est pas disponible.
     */
    public String replaceFontImages(String text) {
        if (!available || text == null) return text == null ? "" : text;
        try {
            return FontImageWrapper.replaceFontImages(text);
        } catch (Exception e) {
            return text;
        }
    }
}
