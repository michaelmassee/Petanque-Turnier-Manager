package de.petanqueturniermanager.webserver;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

import de.petanqueturniermanager.helper.i18n.I18n;

public final class RegieSlug {

    private static final Set<String> RESERVIERT = Set.of(
            "assets", "images", "events", "steuerung", "gong.wav");

    private RegieSlug() {
    }

    public static String ausName(String name) {
        if (name == null) {
            return "";
        }
        String normalisiert = Normalizer.normalize(name.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalisiert
                .replace("ß", "ss")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    public static void validiere(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException(I18n.get("webserver.regie.fehler.slug.leer"));
        }
        if (!slug.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException(I18n.get("webserver.regie.fehler.slug.zeichen", slug));
        }
        if (slug.startsWith("-") || slug.endsWith("-")) {
            throw new IllegalArgumentException(I18n.get("webserver.regie.fehler.slug.bindestrich", slug));
        }
        if (RESERVIERT.contains(slug)) {
            throw new IllegalArgumentException(I18n.get("webserver.regie.fehler.slug.reserviert", slug));
        }
    }
}
