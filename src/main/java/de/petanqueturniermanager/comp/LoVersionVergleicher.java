package de.petanqueturniermanager.comp;

/**
 * Reine Vergleichslogik ohne IO/UNO-Abhängigkeiten: prüft, ob sich die Major-Version
 * zweier LibreOffice-Versionsstrings unterscheidet (z.B. "24.8.3.2" vs. "26.2.1.1").
 * Patch-/Minor-Unterschiede innerhalb derselben Major-Version gelten als kompatibel.
 */
final class LoVersionVergleicher {

    private LoVersionVergleicher() {
    }

    /**
     * Liefert {@code true}, wenn beide Versionen eine parsebare Major-Version haben
     * und sich diese unterscheiden. Ist eine der Versionen {@code null}, leer oder
     * nicht parsebar, wird {@code false} geliefert (kein Fehlalarm).
     */
    static boolean istMajorAbweichend(String buildVersion, String laufendeVersion) {
        var buildMajor = extrahiereMajor(buildVersion);
        var laufendeMajor = extrahiereMajor(laufendeVersion);
        if (buildMajor == null || laufendeMajor == null) {
            return false;
        }
        return !buildMajor.equals(laufendeMajor);
    }

    private static Integer extrahiereMajor(String version) {
        if (version == null) {
            return null;
        }
        var trimmed = version.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        var punktIndex = trimmed.indexOf('.');
        var majorTeil = punktIndex >= 0 ? trimmed.substring(0, punktIndex) : trimmed;
        try {
            return Integer.valueOf(majorTeil);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
