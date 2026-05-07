package de.petanqueturniermanager.spielerdb.export;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Verfügbare Export-Formate. Jedes Format kennt seinen Ziel-Typ
 * (Datei vs. Ordner), eine Default-Endung für Datei-Picker und einen
 * i18n-Schlüssel für die Anzeige im Dialog.
 */
public enum ExportFormat {

    CSV(ZielTyp.ORDNER, "csv", "spielerdb.export.format.csv"),
    JSON(ZielTyp.DATEI, "json", "spielerdb.export.format.json"),
    CALC(ZielTyp.DATEI, "ods", "spielerdb.export.format.calc"),
    SQLITE_BACKUP(ZielTyp.DATEI, "sqlite3", "spielerdb.export.format.sqlite");

    /** Wird im Dialog gewählt: Datei-Picker oder Ordner-Picker. */
    public enum ZielTyp { DATEI, ORDNER }

    private final ZielTyp zielTyp;
    private final String defaultEndung;
    private final String i18nKey;

    ExportFormat(ZielTyp zielTyp, String defaultEndung, String i18nKey) {
        this.zielTyp = zielTyp;
        this.defaultEndung = defaultEndung;
        this.i18nKey = i18nKey;
    }

    public ZielTyp zielTyp() {
        return zielTyp;
    }

    public String defaultEndung() {
        return defaultEndung;
    }

    public String anzeigeName() {
        return I18n.get(i18nKey);
    }
}
