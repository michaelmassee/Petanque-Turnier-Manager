/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.IFreispielPropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.IZeitplanPropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.StringTools;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.konfigdialog.ZeitplanConfigProperty;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Konfigurationseigenschaften für das Formule X Turniersystem.
 */
public class FormuleXPropertiesSpalte extends BasePropertiesSpalte
        implements IFreispielPropertiesSpalte, IZeitplanPropertiesSpalte {

    public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

    static {
        ADDBaseProp(KONFIG_PROPERTIES);
        addCheckinSortProp(KONFIG_PROPERTIES);
        addTeilnehmerListeSortProp(KONFIG_PROPERTIES);
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

    private static final String KONFIG_PROP_FREISPIEL_PUNKTE_PLUS  = "Freispiel Punkte +";
    private static final String KONFIG_PROP_FREISPIEL_PUNKTE_MINUS = "Freispiel Punkte -";

    static {
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_LINKS)
                .setDescription("config.desc.header.links"));
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_MITTE)
                .setDescription("config.desc.header.mitte"));
        KONFIG_PROPERTIES.add(HeaderFooterConfigProperty.from(KONFIG_PROP_KOPF_ZEILE_RECHTS)
                .setDescription("config.desc.header.rechts"));

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_NAME_SPIELRUNDE)
                .setDefaultVal(1).setDescription("config.desc.aktuelle.spielrunde"));

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_ANZAHL_RUNDEN)
                .setDefaultVal(4).setDescription("config.desc.formulex.anzahl.runden"));

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_GERADE)
                .setDefaultVal(DEFAULT_GERADE_BACK_COLOR)
                .setDescription("config.desc.spielrunde.gerade"));
        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_UNGERADE)
                .setDefaultVal(DEFAULT_UNGERADE_BACK_COLOR)
                .setDescription("config.desc.spielrunde.ungerade"));
        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.COLOR, KONFIG_PROP_SPIELRUNDE_COLOR_BACK_HEADER)
                .setDefaultVal(DEFAULT_HEADER_BACK_COLOR).setDescription("config.desc.spielrunde.header"));

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELRUNDE_SPIELBAHN)
                .setDefaultVal(SpielrundeSpielbahn.X.name()).setDescription("config.desc.spielbahn"))
                .addAuswahl(SpielrundeSpielbahn.X.name(), "Keine Spalte")
                .addAuswahl(SpielrundeSpielbahn.L.name(), "Leere Spalte")
                .addAuswahl(SpielrundeSpielbahn.N.name(), "Durchnummerieren (1-n)")
                .addAuswahl(SpielrundeSpielbahn.R.name(), "Zufällig vergeben"));

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
                .setDefaultVal(Formation.TRIPLETTE.name())
                .setDescription("config.desc.meldeliste.formation"))
                .addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
                .addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
                .addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()));

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
                .setDefaultVal("J").setDescription("config.desc.meldeliste.teamname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein"));

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
                .setDefaultVal("N").setDescription("config.desc.schweizer.vereinsname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein"));

        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_PLUS)
                .setDefaultVal(13).setDescription("config.desc.freispiel.punkte.plus"));
        KONFIG_PROPERTIES.add(ConfigProperty.from(ConfigPropertyType.INTEGER, KONFIG_PROP_FREISPIEL_PUNKTE_MINUS)
                .setDefaultVal(0).setDescription("config.desc.freispiel.punkte.minus"));

        KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Boolean>from(ConfigPropertyType.BOOLEAN, KONFIG_PROP_ZEITPLAN_AKTIV)
                .setDefaultVal(false).setDescription("config.desc.zeitplan.aktiv"));
        KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN)
                .setDefaultVal(0).setDescription("config.desc.zeitplan.anzahl.bahnen"));
        KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN)
                .setDefaultVal(15).setDescription("config.desc.zeitplan.zeitlimit"));
        KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN)
                .setDefaultVal(5).setDescription("config.desc.zeitplan.durchgang.pause"));
        KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<Integer>from(ConfigPropertyType.INTEGER, KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN)
                .setDefaultVal(10).setDescription("config.desc.zeitplan.runden.pause"));
        KONFIG_PROPERTIES.add(ZeitplanConfigProperty.<String>from(ConfigPropertyType.STRING, KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT)
                .setDefaultVal("09:00").setDescription("config.desc.zeitplan.turnier.startzeit").kompaktesTextfeld()
                .validierung(StringTools::isValidUhrzeitHhMm));

        ADDUploadProp(KONFIG_PROPERTIES);
        ADDSpielrundenExportProp(KONFIG_PROPERTIES);
        ADDTeilnehmerlisteExportProp(KONFIG_PROPERTIES);
        ADDAbschlussSheetExportProp(KONFIG_PROPERTIES);
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

    @Override
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
        return readEnumProperty(KONFIG_PROP_MELDELISTE_FORMATION, Formation.class, Formation.TRIPLETTE);
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

    @Override
    public Integer getFreispielPunktePlus() {
        return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_PLUS);
    }

    @Override
    public Integer getFreispielPunkteMinus() {
        return readIntProperty(KONFIG_PROP_FREISPIEL_PUNKTE_MINUS);
    }

    @Override
    public boolean isZeitplanAktiv() {
        return Boolean.TRUE.equals(readBooleanProperty(KONFIG_PROP_ZEITPLAN_AKTIV));
    }

    @Override
    public void setZeitplanAktiv(boolean aktiv) {
        setStringProperty(KONFIG_PROP_ZEITPLAN_AKTIV, StringTools.booleanToString(aktiv));
    }

    @Override
    public int getZeitplanAnzahlBahnen() {
        return readIntProperty(KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN);
    }

    @Override
    public void setZeitplanAnzahlBahnen(int bahnen) {
        writeIntProperty(KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN, bahnen);
    }

    @Override
    public boolean isDurchgangAufteilungWirksam() {
        return isZeitplanAktiv() && getZeitplanAnzahlBahnen() > 0;
    }

    @Override
    public int getZeitplanZeitlimitMinuten() {
        return readIntProperty(KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
    }

    @Override
    public void setZeitplanZeitlimitMinuten(int minuten) {
        writeIntProperty(KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN, minuten);
    }

    @Override
    public int getZeitplanDurchgangPauseMinuten() {
        return readIntProperty(KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN);
    }

    @Override
    public void setZeitplanDurchgangPauseMinuten(int minuten) {
        writeIntProperty(KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN, minuten);
    }

    @Override
    public int getZeitplanRundenPauseMinuten() {
        return readIntProperty(KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN);
    }

    @Override
    public void setZeitplanRundenPauseMinuten(int minuten) {
        writeIntProperty(KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN, minuten);
    }

    @Override
    public String getZeitplanTurnierStartzeit() {
        return readStringProperty(KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT);
    }

    @Override
    public void setZeitplanTurnierStartzeit(String hhMm) {
        setStringProperty(KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT, hhMm);
    }
}
