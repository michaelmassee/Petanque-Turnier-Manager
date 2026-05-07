package de.petanqueturniermanager.spielerdb;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Datei-Formate der Spieler-DB — gemeinsam von Export und Import genutzt.
 * Jedes Format kennt seinen Ziel-Typ (Datei vs. Ordner), eine Default-Endung
 * für Datei-Picker und einen i18n-Schlüssel für die Anzeige im Dialog.
 */
public enum SpielerDbDateiFormat {

    CSV(ZielTyp.ORDNER, "csv", "spielerdb.format.csv"),
    JSON(ZielTyp.DATEI, "json", "spielerdb.format.json"),
    CALC(ZielTyp.DATEI, "ods", "spielerdb.format.calc"),
    SQLITE_BACKUP(ZielTyp.DATEI, "sqlite3", "spielerdb.format.sqlite");

    /** Wird im Dialog gewählt: Datei-Picker oder Ordner-Picker. */
    public enum ZielTyp { DATEI, ORDNER }

    private final ZielTyp zielTyp;
    private final String defaultEndung;
    private final String i18nKey;

    SpielerDbDateiFormat(ZielTyp zielTyp, String defaultEndung, String i18nKey) {
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
