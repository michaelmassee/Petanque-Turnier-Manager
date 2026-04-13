/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;

/**
 * Konfigurationseigenschaften für das Kaskaden-KO-Turniersystem.
 */
public class KaskadePropertiesSpalte extends BasePropertiesSpalte {

    public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

    static {
        ADDBaseProp(KONFIG_PROPERTIES, false);
    }

    private static final String KONFIG_PROP_KOPF_ZEILE_LINKS      = "Kopfzeile Links";
    private static final String KONFIG_PROP_KOPF_ZEILE_MITTE      = "Kopfzeile Mitte";
    private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS     = "Kopfzeile Rechts";
    private static final String KONFIG_PROP_MELDELISTE_FORMATION  = "Meldeliste Formation";
    private static final String KONFIG_PROP_MELDELISTE_TEAMNAME   = "Meldeliste Teamname";
    private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";

    public static final String KONFIG_PROP_ANZAHL_KASKADEN        = "Kaskaden Anzahl";
    public static final String KONFIG_PROP_AKTIVE_KASKADENRUNDE   = "Aktive Kaskadenrunde";
    public static final String KONFIG_PROP_KO_FELDER_ERSTELLT     = "KO-Felder erstellt";

    static {
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
                .setDescription("config.desc.header.links").inSideBar());
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
                .setDescription("config.desc.header.mitte").inSideBar());
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
                .setDescription("config.desc.header.rechts").inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
                .setDefaultVal(Formation.DOUBLETTE.name())
                .setDescription("config.desc.ko.formation"))
                .addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
                .addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
                .addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()).inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
                .setDefaultVal("J").setDescription("config.desc.meldeliste.teamname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
                .setDefaultVal("N").setDescription("config.desc.ko.vereinsname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZAHL_KASKADEN)
                .setDefaultVal(2)
                .setDescription("config.desc.kaskade.anzahl.kaskaden")
                .inSideBar());

        // Interne Zustandseigenschaften – nicht in der Sidebar sichtbar
        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_AKTIVE_KASKADENRUNDE)
                .setDefaultVal(0));
        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.STRING, KONFIG_PROP_KO_FELDER_ERSTELLT)
                .setDefaultVal("N"));

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, "Tab-Farbe Kaskaden-KO")
                .setDefaultVal(SheetTabFarben.KO_TURNIERBAUM)
                .setDescription("config.desc.tab.farbe.kaskade").tabFarbe());
    }

    KaskadePropertiesSpalte(ISheet sheet) {
        super(sheet);
    }

    @Override
    protected List<ConfigProperty<?>> getKonfigProperties() {
        return KONFIG_PROPERTIES;
    }

    public String getKopfZeileLinks() {
        return readStringProperty(KONFIG_PROP_KOPF_ZEILE_LINKS);
    }

    public String getKopfZeileMitte() {
        return readStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE);
    }

    public void setKopfZeileMitte(String text) {
        setStringProperty(KONFIG_PROP_KOPF_ZEILE_MITTE, text);
    }

    public String getKopfZeileRechts() {
        return readStringProperty(KONFIG_PROP_KOPF_ZEILE_RECHTS);
    }

    public Formation getMeldeListeFormation() {
        String val = readStringProperty(KONFIG_PROP_MELDELISTE_FORMATION);
        try {
            return Formation.valueOf(val);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Formation.DOUBLETTE;
        }
    }

    public void setMeldeListeFormation(Formation formation) {
        setStringProperty(KONFIG_PROP_MELDELISTE_FORMATION, formation.name());
    }

    public boolean isMeldeListeTeamnameAnzeigen() {
        return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME));
    }

    public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
        setStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME, anzeigen ? "J" : "N");
    }

    public boolean isMeldeListeVereinsnameAnzeigen() {
        return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME));
    }

    public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
        setStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME, anzeigen ? "J" : "N");
    }

    private static final String KONFIG_PROP_KASKADEN_TAB_FARBE = "Tab-Farbe Kaskaden-KO";

    /**
     * Konfigurierbare Tab-Farbe für die Kaskaden-KO KO-Feld-Sheets.
     */
    public int getKaskadenTabFarbe() {
        return readIntProperty(KONFIG_PROP_KASKADEN_TAB_FARBE);
    }

    /**
     * Anzahl Kaskaden-Runden vor dem KO-Modus (Default 2 = ABCD-System).
     * Wert 2 erzeugt A/B/C/D-Felder, Wert 3 erzeugt A/B/C/D/E/F/G/H-Felder.
     */
    public int getAnzahlKaskaden() {
        int val = readIntProperty(KONFIG_PROP_ANZAHL_KASKADEN);
        return val >= 2 ? val : 2;
    }

    public void setAnzahlKaskaden(int anzahl) {
        writeIntProperty(KONFIG_PROP_ANZAHL_KASKADEN, Math.max(2, anzahl));
    }

    /**
     * Nummer der zuletzt erstellten Kaskadenrunde (0 = noch keine Runde erstellt).
     */
    public int getAktiveKaskadenRunde() {
        int val = readIntProperty(KONFIG_PROP_AKTIVE_KASKADENRUNDE);
        return val >= 0 ? val : 0;
    }

    public void setAktiveKaskadenRunde(int rundeNr) {
        writeIntProperty(KONFIG_PROP_AKTIVE_KASKADENRUNDE, Math.max(0, rundeNr));
    }

    /**
     * {@code true} wenn die KO-Feld-Sheets bereits angelegt wurden.
     */
    public boolean isKoFelderErstellt() {
        return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_KO_FELDER_ERSTELLT));
    }

    public void setKoFelderErstellt(boolean erstellt) {
        setStringProperty(KONFIG_PROP_KO_FELDER_ERSTELLT, erstellt ? "J" : "N");
    }
}
