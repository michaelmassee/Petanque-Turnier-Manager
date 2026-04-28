/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * Zentraler Zugriffspunkt für alle lokalisierten Tabellennamen im Petanque-Turnier-Manager.
 * <p>
 * Tabellennamen werden über das i18n-Framework ({@link I18n#get(String)}) aufgelöst
 * und passen sich damit der LibreOffice-Spracheinstellung an.
 * <p>
 * {@code LEGACY_*}-Konstanten enthalten den unveränderlichen deutschen Originalnamen
 * und werden ausschließlich als Fallback in
 * {@code SheetMetadataHelper.findeSheetUndHeile()} übergeben,
 * um ältere Dokumente ohne Metadaten-Eintrag korrekt zu öffnen.
 * <p>
 * {@code KEY_*}-Konstanten sind {@code public} für den Testzugriff via expliziter Key-Listen.
 */
public final class SheetNamen {

    // ── i18n-Schlüssel – einfache Tabellennamen ──────────────────────────────

    public static final String KEY_MELDELISTE                       = "sheet.name.meldeliste";
    public static final String KEY_RANGLISTE                        = "sheet.name.rangliste";
    public static final String KEY_DIREKTVERGLEICH                  = "sheet.name.direktvergleich";
    public static final String KEY_SPIELPLAN                        = "sheet.name.spielplan";
    public static final String KEY_ENDRANGLISTE                     = "sheet.name.endrangliste";
    public static final String KEY_SUPERMELEE_TEAMS                 = "sheet.name.supermelee.teams";
    public static final String KEY_KO_KONFIGURATION                 = "sheet.name.ko.konfiguration";
    public static final String KEY_KASKADE_KONFIGURATION            = "sheet.name.kaskade.konfiguration";
    public static final String KEY_FORMULEX_KONFIGURATION           = "sheet.name.formulex.konfiguration";
    public static final String KEY_FORMULEX_RANGLISTE               = "sheet.name.formulex.rangliste";
    public static final String KEY_KO_RUNDE                        = "sheet.name.ko.runde";
    public static final String KEY_CADRAGE                          = "sheet.name.cadrage";
    public static final String KEY_VORRUNDEN_ERGEBNISSE             = "sheet.name.vorrunden.ergebnisse";
    public static final String KEY_VORRUNDEN_HILFSBLATT             = "sheet.name.vorrunden.hilfsblatt";
    public static final String KEY_MAASTRICHTER_VR_RANGLISTE        = "sheet.name.maastrichter.vorrunden.rangliste";
    public static final String KEY_ANMELDUNGEN                      = "sheet.name.anmeldungen";
    public static final String KEY_TEILNEHMER                       = "sheet.name.teilnehmer";
    public static final String KEY_POULE_VORRUNDE                   = "sheet.name.poule.vorrunde";
    public static final String KEY_POULE_TEILNEHMER                 = "sheet.name.poule.teilnehmer";
    public static final String KEY_POULE_VORRUNDEN_RANGLISTE        = "sheet.name.poule.vorrunden.rangliste";

    // ── i18n-Schlüssel – Muster mit Platzhaltern ─────────────────────────────

    /** Schweizer Spielrunde: {0} = Rundennummer. Beispiel: "3. Spielrunde" */
    public static final String KEY_SPIELRUNDE_MUSTER                = "sheet.name.spielrunde.muster";
    /** Supermelee Spielrunde: {0} = Spieltagnummer, {1} = Rundennummer. Beispiel: "1.3. Spielrunde" */
    public static final String KEY_SUPERMELEE_SPIELRUNDE_MUSTER     = "sheet.name.supermelee.spielrunde.muster";
    /** Supermelee SpielrundePlan: {0} = Spieltagnummer, {1} = Rundennummer. Beispiel: "1.3. SpielrundePlan" */
    public static final String KEY_SPIELRUNDE_PLAN_MUSTER           = "sheet.name.spielrunde.plan.muster";
    /** Spieltag-Rangliste: {0} = Spieltagnummer. Beispiel: "2. Spieltag Rangliste" */
    public static final String KEY_SPIELTAG_RANGLISTE_MUSTER        = "sheet.name.spieltag.rangliste.muster";
    /** KO-Turnierbaum Einzelgruppe (ohne Buchstabe): z.B. "KO Turnierbaum" */
    public static final String KEY_KO_TURNIERBAUM_EINZEL            = "sheet.name.ko.turnierbaum.einzel";
    /** KO-Turnierbaum Gruppe: {0} = Gruppenbuchstabe. Beispiel: "KO Turnierbaum A" */
    public static final String KEY_KO_TURNIERBAUM_GRUPPE_MUSTER     = "sheet.name.ko.turnierbaum.gruppe.muster";
    /** Maastrichter Vorrunde: {0} = Rundennummer. Beispiel: "Vorrunde 2" */
    public static final String KEY_MAASTRICHTER_VORRUNDE_MUSTER     = "sheet.name.maastrichter.vorrunde.muster";
    /** Maastrichter Finalrunde: {0} = Gruppenbuchstabe. Beispiel: "Finalrunde A" */
    public static final String KEY_MAASTRICHTER_FINALRUNDE_MUSTER   = "sheet.name.maastrichter.finalrunde.muster";
    /** KO-Finale-Gruppe: {0} = Buchstabe. Beispiel: "A-Finale" */
    public static final String KEY_KO_FINALE_GRUPPE_MUSTER          = "sheet.name.ko.finale.gruppe.muster";
    /** Supermelee Anmeldungen: {0} = Spieltagnummer. Beispiel: "1. Spieltag Anmeldungen" */
    public static final String KEY_ANMELDUNGEN_MUSTER               = "sheet.name.anmeldungen.muster";
    /** Supermelee Teilnehmer: {0} = Spieltagnummer. Beispiel: "1. Spieltag Teilnehmer" */
    public static final String KEY_TEILNEHMER_MUSTER                = "sheet.name.teilnehmer.muster";
    /** Poule-Spielplan: {0} = Gruppennummer. Beispiel: "Poule 1 Spielplan" */
    public static final String KEY_POULE_SPIELPLAN_MUSTER           = "sheet.name.poule.spielplan.muster";
    /** Kaskaden-Runde: {0} = Rundennummer. Beispiel: "1. Kaskaden-Runde" */
    public static final String KEY_KASKADE_RUNDE_MUSTER             = "sheet.name.kaskade.runde.muster";
    /** Kaskaden-Feld: {0} = Bezeichner. Beispiel: "Kaskade A-Feld" */
    public static final String KEY_KASKADE_FELD_MUSTER              = "sheet.name.kaskade.feld.muster";
    /** Kaskaden-Gruppenrangliste: Übersicht aller Felder nach Kaskadenrunden */
    public static final String KEY_KASKADE_GRUPPENRANGLISTE         = "sheet.name.kaskade.gruppenrangliste";
    /** Formule X Spielrunde: {0} = Rundennummer. Beispiel: "1. Spielrunde" */
    public static final String KEY_FORMULEX_SPIELRUNDE_MUSTER       = "sheet.name.formulex.spielrunde.muster";

    // ── Legacy-Werte: unveränderliche deutsche Originalnamen ─────────────────
    // Werden ausschließlich als Fallback in findeSheetUndHeile() für alte Dokumente verwendet.

    public static final String LEGACY_MELDELISTE                    = "Meldeliste";
    public static final String LEGACY_RANGLISTE                     = "Rangliste";
    public static final String LEGACY_DIREKTVERGLEICH               = "Direktvergleich";
    public static final String LEGACY_SPIELPLAN                     = "Spielplan";
    public static final String LEGACY_ENDRANGLISTE                  = "Endrangliste";
    public static final String LEGACY_SUPERMELEE_TEAMS              = "Superm\u00e9l\u00e9e Teams";
    public static final String LEGACY_KO_KONFIGURATION              = "KO Konfiguration";
    public static final String LEGACY_KASKADE_KONFIGURATION         = "Kaskaden Konfiguration";
    public static final String LEGACY_KO_RUNDE                     = "KO Runde";
    public static final String LEGACY_CADRAGE                       = "Cadrage";
    public static final String LEGACY_VORRUNDEN_ERGEBNISSE          = "Ergebnisse aus Vorrunden";
    public static final String LEGACY_VORRUNDEN_HILFSBLATT          = "VorRunden";
    public static final String LEGACY_MAASTRICHTER_VR_RANGLISTE     = "Vorrunden-Rangliste";
    public static final String LEGACY_ANMELDUNGEN                   = "Anmeldungen";
    public static final String LEGACY_TEILNEHMER                    = "Teilnehmer";
    public static final String LEGACY_KO_TURNIERBAUM_EINZEL         = "KO Turnierbaum";
    public static final String LEGACY_SPIELRUNDE_PRAEFIX            = "Spielrunde";
    public static final String LEGACY_SPIELRUNDE_PLAN_PRAEFIX       = "SpielrundePlan";
    public static final String LEGACY_MAASTRICHTER_VORRUNDE_PRAEFIX = "Vorrunde";
    public static final String LEGACY_MAASTRICHTER_FINALRUNDE_PRAEFIX = "Finalrunde";
    public static final String LEGACY_KO_FINALE_GRUPPE_SUFFIX        = "Finale";
    public static final String LEGACY_POULE_VORRUNDE                 = "Poule Vorrunde";
    public static final String LEGACY_POULE_TEILNEHMER               = "Poule Teilnehmer";
    public static final String LEGACY_POULE_SPIELPLAN_PRAEFIX        = "Spielplan";
    public static final String LEGACY_POULE_VORRUNDEN_RANGLISTE      = "Poule Vorrunden-Rangliste";
    public static final String LEGACY_FORMULEX_KONFIGURATION        = "Formule X Konfiguration";
    public static final String LEGACY_FORMULEX_SPIELRUNDE_PRAEFIX   = "Spielrunde";
    public static final String LEGACY_FORMULEX_RANGLISTE            = "Formule X Rangliste";
    public static final String LEGACY_KASKADE_RUNDE_PRAEFIX          = "Kaskaden-Runde";
    public static final String LEGACY_KASKADE_FELD_PRAEFIX           = "Kaskade";
    public static final String LEGACY_KASKADE_FELD_SUFFIX            = "Feld";
    public static final String LEGACY_KASKADE_GRUPPENRANGLISTE       = "Kaskaden-Gruppenrangliste";

    private SheetNamen() {
    }

    // ── Accessor-Methoden – einfache Tabellennamen ───────────────────────────

    public static String meldeliste() {
        return getOderFallback(KEY_MELDELISTE, LEGACY_MELDELISTE);
    }

    public static String rangliste() {
        return getOderFallback(KEY_RANGLISTE, LEGACY_RANGLISTE);
    }

    public static String direktvergleich() {
        return getOderFallback(KEY_DIREKTVERGLEICH, LEGACY_DIREKTVERGLEICH);
    }

    public static String spielplan() {
        return getOderFallback(KEY_SPIELPLAN, LEGACY_SPIELPLAN);
    }

    public static String endrangliste() {
        return getOderFallback(KEY_ENDRANGLISTE, LEGACY_ENDRANGLISTE);
    }

    public static String supermeleeTeams() {
        return getOderFallback(KEY_SUPERMELEE_TEAMS, LEGACY_SUPERMELEE_TEAMS);
    }

    public static String koKonfiguration() {
        return getOderFallback(KEY_KO_KONFIGURATION, LEGACY_KO_KONFIGURATION);
    }

    public static String kaskadeKonfiguration() {
        return getOderFallback(KEY_KASKADE_KONFIGURATION, LEGACY_KASKADE_KONFIGURATION);
    }

    public static String formulexKonfiguration() {
        return getOderFallback(KEY_FORMULEX_KONFIGURATION, LEGACY_FORMULEX_KONFIGURATION);
    }

    public static String formulexRangliste() {
        return getOderFallback(KEY_FORMULEX_RANGLISTE, LEGACY_FORMULEX_RANGLISTE);
    }

    public static String koRunde() {
        return getOderFallback(KEY_KO_RUNDE, LEGACY_KO_RUNDE);
    }

    public static String cadrage() {
        return getOderFallback(KEY_CADRAGE, LEGACY_CADRAGE);
    }

    public static String vorrundenErgebnisse() {
        return getOderFallback(KEY_VORRUNDEN_ERGEBNISSE, LEGACY_VORRUNDEN_ERGEBNISSE);
    }

    /**
     * Temporäres Rechenhilfsblatt für Vorrunden-Paarungsdaten: z.B. "VorRunden".
     *
     * @return lokalisierter Tabellenname
     * @see de.petanqueturniermanager.maastrichter.korunde.Vorrunden
     */
    public static String vorrundenHilfsblatt() {
        return getOderFallback(KEY_VORRUNDEN_HILFSBLATT, LEGACY_VORRUNDEN_HILFSBLATT);
    }

    public static String maastrichterVorrundenRangliste() {
        return getOderFallback(KEY_MAASTRICHTER_VR_RANGLISTE, LEGACY_MAASTRICHTER_VR_RANGLISTE);
    }

    public static String anmeldungen() {
        return getOderFallback(KEY_ANMELDUNGEN, LEGACY_ANMELDUNGEN);
    }

    public static String teilnehmer() {
        return getOderFallback(KEY_TEILNEHMER, LEGACY_TEILNEHMER);
    }

    public static String pouleVorrunde() {
        return getOderFallback(KEY_POULE_VORRUNDE, LEGACY_POULE_VORRUNDE);
    }

    public static String pouleTeilnehmer() {
        return getOderFallback(KEY_POULE_TEILNEHMER, LEGACY_POULE_TEILNEHMER);
    }

    public static String pouleVorrundenRangliste() {
        return getOderFallback(KEY_POULE_VORRUNDEN_RANGLISTE, LEGACY_POULE_VORRUNDEN_RANGLISTE);
    }

    /**
     * Poule-Meldeliste: identisch mit der Standard-Meldeliste.
     * Das Poule-System verwendet keinen eigenen Blattnamen für die Meldeliste.
     */
    public static String pouleMeldeliste() {
        return meldeliste();
    }

    // ── Accessor-Methoden – Composite-Tabellennamen ──────────────────────────

    /**
     * Schweizer Spielrunde: z.B. "3. Spielrunde".
     *
     * @param rundeNr Nummer der Spielrunde
     * @return lokalisierter Tabellenname
     */
    public static String spielrunde(int rundeNr) {
        var muster = getOderFallback(KEY_SPIELRUNDE_MUSTER, "{0}. " + LEGACY_SPIELRUNDE_PRAEFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{rundeNr});
    }

    /**
     * Supermelee Spielrunde: z.B. "1.3. Spielrunde".
     *
     * @param spieltagNr Nummer des Spieltags
     * @param rundeNr    Nummer der Spielrunde
     * @return lokalisierter Tabellenname
     */
    public static String supermeleeSpielrunde(int spieltagNr, int rundeNr) {
        var muster = getOderFallback(KEY_SUPERMELEE_SPIELRUNDE_MUSTER,
                "{0}.{1}. " + LEGACY_SPIELRUNDE_PRAEFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{spieltagNr, rundeNr});
    }

    /**
     * Supermelee SpielrundePlan: z.B. "1.3. SpielrundePlan".
     *
     * @param spieltagNr Nummer des Spieltags
     * @param rundeNr    Nummer der Spielrunde
     * @return lokalisierter Tabellenname
     */
    public static String spielrundePlan(int spieltagNr, int rundeNr) {
        var muster = getOderFallback(KEY_SPIELRUNDE_PLAN_MUSTER,
                "{0}.{1}. " + LEGACY_SPIELRUNDE_PLAN_PRAEFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{spieltagNr, rundeNr});
    }

    /**
     * Spieltag-Rangliste: z.B. "2. Spieltag Rangliste".
     *
     * @param spieltagNr Nummer des Spieltags
     * @return lokalisierter Tabellenname
     */
    public static String spieltagRangliste(int spieltagNr) {
        var muster = getOderFallback(KEY_SPIELTAG_RANGLISTE_MUSTER, "{0}. Spieltag Rangliste");
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{spieltagNr});
    }

    /**
     * KO-Turnierbaum (Einzelgruppe, ohne Buchstaben-Suffix): z.B. "KO Turnierbaum".
     *
     * @return lokalisierter Tabellenname
     */
    public static String koTurnierbaumEinzel() {
        return getOderFallback(KEY_KO_TURNIERBAUM_EINZEL, LEGACY_KO_TURNIERBAUM_EINZEL);
    }

    /**
     * KO-Turnierbaum mit Gruppe: z.B. "KO Turnierbaum A".
     *
     * @param buchstabe Gruppenbuchstabe, z.B. "A", "B"
     * @return lokalisierter Tabellenname
     */
    public static String koTurnierbaumGruppe(String buchstabe) {
        var muster = getOderFallback(KEY_KO_TURNIERBAUM_GRUPPE_MUSTER,
                LEGACY_KO_TURNIERBAUM_EINZEL + " {0}");
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{buchstabe});
    }

    /**
     * Maastrichter Vorrunde: z.B. "Vorrunde 2".
     *
     * @param rundeNr Nummer der Vorrunde
     * @return lokalisierter Tabellenname
     */
    public static String maastrichterVorrunde(int rundeNr) {
        var muster = getOderFallback(KEY_MAASTRICHTER_VORRUNDE_MUSTER,
                "{0}. " + LEGACY_MAASTRICHTER_VORRUNDE_PRAEFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{rundeNr});
    }

    /**
     * Maastrichter Finalrunde: z.B. "Finalrunde A".
     *
     * @param buchstabe Gruppenbuchstabe, z.B. "A", "B"
     * @return lokalisierter Tabellenname
     */
    public static String maastrichterFinalrunde(String buchstabe) {
        var muster = getOderFallback(KEY_MAASTRICHTER_FINALRUNDE_MUSTER,
                LEGACY_MAASTRICHTER_FINALRUNDE_PRAEFIX + " {0}");
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{buchstabe});
    }

    /**
     * KO-Finale-Gruppe: z.B. "A-Finale".
     *
     * @param buchstabe Gruppenbuchstabe, z.B. "A", "B"
     * @return lokalisierter Tabellenname
     */
    public static String koFinaleGruppe(String buchstabe) {
        var muster = getOderFallback(KEY_KO_FINALE_GRUPPE_MUSTER,
                "{0}-" + LEGACY_KO_FINALE_GRUPPE_SUFFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{buchstabe});
    }

    /**
     * Supermelee Anmeldungen: z.B. "1. Spieltag Anmeldungen".
     *
     * @param spieltagNr Nummer des Spieltags
     * @return lokalisierter Tabellenname
     */
    public static String anmeldungen(int spieltagNr) {
        var muster = getOderFallback(KEY_ANMELDUNGEN_MUSTER, "{0}. Spieltag " + LEGACY_ANMELDUNGEN);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{spieltagNr});
    }

    /**
     * Supermelee Teilnehmer: z.B. "1. Spieltag Teilnehmer".
     *
     * @param spieltagNr Nummer des Spieltags
     * @return lokalisierter Tabellenname
     */
    public static String teilnehmer(int spieltagNr) {
        var muster = getOderFallback(KEY_TEILNEHMER_MUSTER, "{0}. Spieltag " + LEGACY_TEILNEHMER);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{spieltagNr});
    }

    /**
     * Poule-Spielplan-Sheet: z.B. "Poule 1 Spielplan".
     *
     * @param pouleNr Nummer der Gruppe
     * @return lokalisierter Tabellenname
     */
    public static String pouleSpielplan(int pouleNr) {
        var muster = getOderFallback(KEY_POULE_SPIELPLAN_MUSTER, "Poule {0} " + LEGACY_POULE_SPIELPLAN_PRAEFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{pouleNr});
    }

    /**
     * Kaskaden-KO-Runde: z.B. "1. Kaskaden-Runde".
     *
     * @param rundeNr Nummer der Kaskadenrunde
     * @return lokalisierter Tabellenname
     */
    public static String kaskadenRunde(int rundeNr) {
        var muster = getOderFallback(KEY_KASKADE_RUNDE_MUSTER, "{0}. " + LEGACY_KASKADE_RUNDE_PRAEFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{rundeNr});
    }

    /**
     * Kaskaden-KO-Feld: z.B. "Kaskade A-Feld".
     *
     * @param bezeichner Feldbezeichner, z.B. "A", "B"
     * @return lokalisierter Tabellenname
     */
    public static String kaskadenFeld(String bezeichner) {
        var muster = getOderFallback(KEY_KASKADE_FELD_MUSTER,
                LEGACY_KASKADE_FELD_PRAEFIX + " {0}-" + LEGACY_KASKADE_FELD_SUFFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{bezeichner});
    }

    /**
     * Kaskaden-Gruppenrangliste: Übersicht aller Felder nach den Kaskadenrunden.
     *
     * @return lokalisierter Tabellenname
     */
    public static String kaskadeGruppenrangliste() {
        return getOderFallback(KEY_KASKADE_GRUPPENRANGLISTE, LEGACY_KASKADE_GRUPPENRANGLISTE);
    }

    /**
     * Formule X Spielrunde: z.B. "1. Spielrunde".
     *
     * @param rundeNr Nummer der Spielrunde
     * @return lokalisierter Tabellenname
     */
    public static String formulexSpielrunde(int rundeNr) {
        var muster = getOderFallback(KEY_FORMULEX_SPIELRUNDE_MUSTER, "{0}. " + LEGACY_FORMULEX_SPIELRUNDE_PRAEFIX);
        return new MessageFormat(muster, Locale.ROOT).format(new Object[]{rundeNr});
    }

    // ── Hilfsmethode ─────────────────────────────────────────────────────────

    /**
     * Gibt den i18n-Wert zurück, oder den Legacy-Namen wenn I18n nicht initialisiert ist,
     * der Schlüssel fehlt oder ein leerer Wert zurückgegeben wird.
     *
     * @param schluessel i18n-Schlüssel
     * @param legacy     unveränderlicher deutscher Originalname als Fallback
     * @return lokalisierter Tabellenname oder Legacy-Name
     */
    private static String getOderFallback(String schluessel, String legacy) {
        var wert = I18n.get(schluessel);
        if (wert == null || wert.isBlank() || schluessel.equals(wert)) {
            return legacy;
        }
        return wert;
    }
}
