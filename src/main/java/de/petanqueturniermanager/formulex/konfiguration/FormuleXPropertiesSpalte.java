/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Konfigurationseigenschaften für das Formule X Turniersystem.
 */
public class FormuleXPropertiesSpalte extends BasePropertiesSpalte {

    public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

    static {
        ADDBaseProp(KONFIG_PROPERTIES);
    }

    private static final String KONFIG_PROP_KOPF_ZEILE_LINKS      = "Kopfzeile Links";
    private static final String KONFIG_PROP_KOPF_ZEILE_MITTE      = "Kopfzeile Mitte";
    private static final String KONFIG_PROP_KOPF_ZEILE_RECHTS     = "Kopfzeile Rechts";

    public static final String KONFIG_PROP_ANZAHL_RUNDEN           = "Anzahl Runden";
    public static final String KONFIG_PROP_NAME_SPIELRUNDE         = "Spielrunde";

    private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE   = "Spielrunde Hintergrund Gerade";
    private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE = "Spielrunde Hintergrund Ungerade";
    private static final String KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER   = "Spielrunde Header";
    private static final String KONFIG_PROP_SPIELRUNDE_SPIELBAHN           = "Spielrunde Spielbahn";

    private static final String KONFIG_PROP_MELDELISTE_FORMATION   = "Meldeliste Formation";
    private static final String KONFIG_PROP_MELDELISTE_TEAMNAME    = "Meldeliste Teamname";
    private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";

    static {
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
                .setDescription("config.desc.header.links").inSideBar());
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
                .setDescription("config.desc.header.mitte").inSideBar());
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
                .setDescription("config.desc.header.rechts").inSideBar());

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
                .setDefaultVal(1).setDescription("config.desc.aktuelle.spielrunde").inSideBarInfoPanel());

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZAHL_RUNDEN)
                .setDefaultVal(4).setDescription("config.desc.formulex.anzahl.runden").inSideBar());

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE)
                .setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
                .setDescription("config.desc.spielrunde.gerade").inSideBar());
        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE)
                .setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
                .setDescription("config.desc.spielrunde.ungerade").inSideBar());
        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER)
                .setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.spielrunde.header")
                .inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELRUNDE_SPIELBAHN)
                .setDefaultVal(SpielrundeSpielbahn.X.name()).setDescription("config.desc.spielbahn"))
                .addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
                .addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
                .addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
                .addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben").inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
                .setDefaultVal(Formation.TRIPLETTE.name())
                .setDescription("config.desc.meldeliste.formation"))
                .addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
                .addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
                .addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()).inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
                .setDefaultVal("J").setDescription("config.desc.meldeliste.teamname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
                .setDefaultVal("N").setDescription("config.desc.schweizer.vereinsname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());
    }

    FormuleXPropertiesSpalte(ISheet sheet) {
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

    public void setAktiveSpielRunde(SpielRundeNr spielrunde) {
        writeIntProperty(KONFIG_PROP_NAME_SPIELRUNDE, spielrunde.getNr());
    }

    public SpielRundeNr getAktiveSpielRunde() {
        return SpielRundeNr.from(readIntProperty(KONFIG_PROP_NAME_SPIELRUNDE));
    }

    public int getAnzahlRunden() {
        int val = readIntProperty(KONFIG_PROP_ANZAHL_RUNDEN);
        return val > 0 ? val : 4;
    }

    public void setAnzahlRunden(int anzahl) {
        writeIntProperty(KONFIG_PROP_ANZAHL_RUNDEN, Math.max(1, anzahl));
    }

    public SpielrundeSpielbahn getSpielrundeSpielbahn() {
        return SpielrundeSpielbahn.valueOf(readStringProperty(KONFIG_PROP_SPIELRUNDE_SPIELBAHN));
    }

    public void setSpielrundeSpielbahn(SpielrundeSpielbahn option) {
        setStringProperty(KONFIG_PROP_SPIELRUNDE_SPIELBAHN, option.name());
    }

    public Integer getSpielRundeHintergrundFarbeGerade() {
        return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE);
    }

    public SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle() {
        return new SpielrundeHintergrundFarbeGeradeStyle(getSpielRundeHintergrundFarbeGerade());
    }

    public Integer getSpielRundeHintergrundFarbeUnGerade() {
        return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE);
    }

    public SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle() {
        return new SpielrundeHintergrundFarbeUnGeradeStyle(getSpielRundeHintergrundFarbeUnGerade());
    }

    public Integer getSpielRundeHeaderFarbe() {
        return readCellBackColorProperty(KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER);
    }

    public Formation getMeldeListeFormation() {
        String val = readStringProperty(KONFIG_PROP_MELDELISTE_FORMATION);
        try {
            return Formation.valueOf(val);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Formation.TRIPLETTE;
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
}
