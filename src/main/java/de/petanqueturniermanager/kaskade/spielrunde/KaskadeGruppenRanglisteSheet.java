/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.KaskadenFeldBelegung;
import de.petanqueturniermanager.algorithmen.KaskadenKoRundenPlan;
import de.petanqueturniermanager.algorithmen.KaskadenKoRundenPlaner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt die Kaskaden-Gruppenrangliste nach Abschluss aller Kaskadenrunden.<br>
 * <br>
 * Das Sheet zeigt für jedes Endfeld (A, B, C, D …) die Setzliste der zugewiesenen
 * Teams nebeneinander: jeweils zwei Spalten (Pos | Team-Nr).<br>
 * <br>
 * Die Feldbelegung wird aus den gespeicherten Kaskadenrunden-Sheets gelesen
 * ({@link KaskadeRundenErgebnisLeser}).
 *
 * @author Michael Massee
 */
public class KaskadeGruppenRanglisteSheet extends SheetRunner implements ISheet {

    private static final Logger LOGGER = LogManager.getLogger(KaskadeGruppenRanglisteSheet.class);

    /**
     * Pro Gruppe: 2 Spalten (Pos | Team-Nr).
     * Gruppen-Spaltenoffsets (relativ zum Start des Gruppenblocks):
     */
    private static final int BLOCK_POS_OFFSET  = 0;
    private static final int BLOCK_NR_OFFSET   = 1;
    private static final int BLOCK_BREITE      = 2;

    /** Zeilen-Layout. */
    private static final int HAUPTHEADER_ZEILE = 0;
    private static final int GRUPPE_HEADER_ZEILE = 1;
    private static final int SPALTEN_HEADER_ZEILE = 2;
    private static final int ERSTE_DATEN_ZEILE = 3;

    private static final int CHARHEIGHT_HAUPTHEADER = 16;
    private static final int CHARHEIGHT_HEADER       = 13;
    private static final int CHARHEIGHT_DATEN        = 13;

    private final KaskadeKonfigurationSheet konfigurationSheet;
    private final KaskadeMeldeListeSheetUpdate meldeListe;

    /** Wird für Tests gesetzt, damit forceCreate aktiviert wird. */
    private boolean forceOk;

    public KaskadeGruppenRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KASKADE, "Kaskaden-Gruppenrangliste");
        konfigurationSheet = new KaskadeKonfigurationSheet(workingSpreadsheet);
        meldeListe = new KaskadeMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE,
                SheetNamen.kaskadeGruppenrangliste());
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    public KaskadeKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    protected void doRun() throws GenerateException {
        processBoxinfo("processbox.kaskade.gruppenrangliste.erstellen");
        getxCalculatable().enableAutomaticCalculation(false);

        meldeListe.upDateSheet();
        int gesamtTeams = meldeListe.getMeldungenSortiertNachSetzposition().size();

        boolean freispielGewonnen = konfigurationSheet.getFreispielPunktePlus()
                > konfigurationSheet.getFreispielPunkteMinus();
        var plan = KaskadenKoRundenPlaner.berechne(
                gesamtTeams, konfigurationSheet.getAnzahlKaskaden(), freispielGewonnen);

        var belegungen = new KaskadeRundenErgebnisLeser(getWorkingSpreadsheet()).ladeFeldBelegungen(plan);
        if (belegungen.isEmpty()) {
            LOGGER.info("Keine Feldbelegungen vorhanden – Gruppenrangliste wird nicht erstellt.");
            return;
        }

        var sheetName  = SheetNamen.kaskadeGruppenrangliste();
        var schluessel = SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE;
        var tabFarbe   = konfigurationSheet.getRanglisteTabFarbe();

        var erstellt = NewSheet.from(this, sheetName, schluessel)
                .pos(DefaultSheetPos.KASKADE_GRUPPENRANGLISTE)
                .setForceCreate(isForceOk())
                .tabColor(tabFarbe)
                .setActiv()
                .hideGrid()
                .create()
                .isDidCreate();

        if (!erstellt) {
            return;
        }

        hauptHeaderSchreiben(plan, belegungen);
        gruppeHeadersSchreiben(belegungen);
        datenSchreiben(belegungen);
        formatieren(belegungen);
        druckBereichSetzen(plan, belegungen);
    }

    // ---------------------------------------------------------------
    // Sheet-Inhalte
    // ---------------------------------------------------------------

    /**
     * Schreibt den zentrierten Haupt-Header über alle Gruppen-Spalten.
     */
    private void hauptHeaderSchreiben(KaskadenKoRundenPlan plan, List<KaskadenFeldBelegung> belegungen)
            throws GenerateException {
        var sheet      = getXSpreadSheet();
        var headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();
        int anzGruppen  = belegungen.size();
        int letzteSpalte = anzGruppen * BLOCK_BREITE - 1;

        var hauptHeader = StringCellValue.from(sheet, Position.from(0, HAUPTHEADER_ZEILE))
                .setValue(SheetNamen.kaskadeGruppenrangliste())
                .setVertJustify(CellVertJustify2.CENTER)
                .centerHoriJustify()
                .setCharHeight(CHARHEIGHT_HAUPTHEADER)
                .setCharWeight(FontWeight.BOLD)
                .setCellBackColor(headerFarbe)
                .setEndPosMergeSpaltePlus(letzteSpalte)
                .setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
        getSheetHelper().setStringValueInCell(hauptHeader);
    }

    /**
     * Schreibt die Gruppen-Überschrift und Spalten-Header (Pos | Nr) für jede Gruppe.
     */
    private void gruppeHeadersSchreiben(List<KaskadenFeldBelegung> belegungen) throws GenerateException {
        var sheet       = getXSpreadSheet();
        var headerFarbe = konfigurationSheet.getMeldeListeHeaderFarbe();
        var posLabel    = I18n.get("kaskade.gruppenrangliste.spalte.pos");
        var nrLabel     = I18n.get("kaskade.gruppenrangliste.spalte.team");

        for (int i = 0; i < belegungen.size(); i++) {
            var belegung    = belegungen.get(i);
            int basisSpalte = i * BLOCK_BREITE;
            boolean istLetzte = (i == belegungen.size() - 1);

            var gruppeHeaderBorder = istLetzte
                    ? BorderFactory.from().allThin().toBorder()
                    : BorderFactory.from().allThin().doubleLn().forRight().toBorder();

            // Spalten-Header-Border: fette Unterlinie als Trenner zum Datenbereich + ggf. doppelte rechte Trennlinie
            var spaltenHeaderBorderNr = istLetzte
                    ? BorderFactory.from().allThin().boldLn().forBottom().toBorder()
                    : BorderFactory.from().allThin().boldLn().forBottom().doubleLn().forRight().toBorder();

            // Gruppen-Header (über beide Spalten des Blocks)
            var gruppeHeader = StringCellValue.from(sheet,
                    Position.from(basisSpalte + BLOCK_POS_OFFSET, GRUPPE_HEADER_ZEILE))
                    .setValue(I18n.get("kaskade.feld.info.header",
                            belegung.bezeichner(), belegung.feld().gesamtTeams()))
                    .setVertJustify(CellVertJustify2.CENTER)
                    .centerHoriJustify()
                    .setCharHeight(CHARHEIGHT_HEADER)
                    .setCharWeight(FontWeight.BOLD)
                    .setShrinkToFit(true)
                    .setCellBackColor(headerFarbe)
                    .setEndPosMergeSpaltePlus(BLOCK_BREITE - 1)
                    .setBorder(gruppeHeaderBorder);
            getSheetHelper().setStringValueInCell(gruppeHeader);

            // Spalten-Header: Pos – fette Unterlinie als Trenner zum Datenbereich
            var posHeader = StringCellValue.from(sheet,
                    Position.from(basisSpalte + BLOCK_POS_OFFSET, SPALTEN_HEADER_ZEILE))
                    .setValue(posLabel)
                    .setVertJustify(CellVertJustify2.CENTER)
                    .centerHoriJustify()
                    .setCharHeight(CHARHEIGHT_HEADER)
                    .setCellBackColor(headerFarbe)
                    .setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
            getSheetHelper().setStringValueInCell(posHeader);

            // Spalten-Header: Nr – fette Unterlinie + bei nicht-letzter Gruppe doppelte rechte Trennlinie
            var nrHeader = StringCellValue.from(sheet,
                    Position.from(basisSpalte + BLOCK_NR_OFFSET, SPALTEN_HEADER_ZEILE))
                    .setValue(nrLabel)
                    .setVertJustify(CellVertJustify2.CENTER)
                    .centerHoriJustify()
                    .setCharHeight(CHARHEIGHT_HEADER)
                    .setCellBackColor(headerFarbe)
                    .setBorder(spaltenHeaderBorderNr);
            getSheetHelper().setStringValueInCell(nrHeader);
        }
    }

    /**
     * Schreibt die Teamdaten (Pos + Team-Nr) für alle Gruppen als Datenblock.
     */
    private void datenSchreiben(List<KaskadenFeldBelegung> belegungen) throws GenerateException {
        int maxZeilen = belegungen.stream()
                .mapToInt(b -> b.teamNrs().size())
                .max()
                .orElse(0);

        if (maxZeilen == 0) {
            return;
        }

        // Pro Gruppe einen separaten Datenblock schreiben
        for (int i = 0; i < belegungen.size(); i++) {
            var belegung    = belegungen.get(i);
            var teamNrs     = belegung.teamNrs();
            int basisSpalte = i * BLOCK_BREITE;

            var datenBlock = new RangeData();
            for (int pos = 1; pos <= teamNrs.size(); pos++) {
                var zeile = datenBlock.addNewRow();
                zeile.add(new CellData(pos));             // Pos
                zeile.add(new CellData(teamNrs.get(pos - 1))); // Team-Nr
            }

            var datenStart = Position.from(basisSpalte, ERSTE_DATEN_ZEILE);
            RangeHelper.from(this, datenBlock.getRangePosition(datenStart)).setDataInRange(datenBlock);
        }
    }

    /**
     * Formatiert alle Datenzellen und setzt Spaltenbreiten.
     */
    private void formatieren(List<KaskadenFeldBelegung> belegungen) throws GenerateException {
        var sheet = getXSpreadSheet();
        int maxZeilen = belegungen.stream()
                .mapToInt(b -> b.teamNrs().size())
                .max()
                .orElse(0);

        if (maxZeilen == 0) {
            return;
        }

        int letzteZeile   = ERSTE_DATEN_ZEILE + maxZeilen - 1;
        int letzteSpalte  = belegungen.size() * BLOCK_BREITE - 1;
        var datenRange    = RangePosition.from(0, ERSTE_DATEN_ZEILE, letzteSpalte, letzteZeile);

        TableBorder2 border = BorderFactory.from().allThin().toBorder();
        getSheetHelper().setPropertyInRange(sheet, datenRange, TABLE_BORDER2, border);
        getSheetHelper().setPropertyInRange(sheet, datenRange, HORI_JUSTIFY, CellHoriJustify.CENTER);
        getSheetHelper().setPropertyInRange(sheet, datenRange, VERT_JUSTIFY, CellVertJustify2.CENTER);
        getSheetHelper().setPropertyInRange(sheet, datenRange, CHAR_HEIGHT,  CHARHEIGHT_DATEN);

        // Zebra-Formatierung für Datenbereich
        RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
                .geradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeGerade())
                .ungeradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeUnGerade())
                .apply();

        // Team-Nr-Spalten fett
        for (int i = 0; i < belegungen.size(); i++) {
            int nrSpalte = i * BLOCK_BREITE + BLOCK_NR_OFFSET;
            var nrRange  = RangePosition.from(nrSpalte, ERSTE_DATEN_ZEILE, nrSpalte, letzteZeile);
            getSheetHelper().setPropertyInRange(sheet, nrRange, CHAR_WEIGHT, FontWeight.BOLD);
        }

        // Doppelte vertikale Trennlinie rechts an jeder NR-Spalte (außer der letzten Gruppe)
        var trennBorder = BorderFactory.from().allThin().doubleLn().forRight().toBorder();
        for (int i = 0; i < belegungen.size() - 1; i++) {
            int nrSpalte = i * BLOCK_BREITE + BLOCK_NR_OFFSET;
            var trennRange = RangePosition.from(nrSpalte, ERSTE_DATEN_ZEILE, nrSpalte, letzteZeile);
            getSheetHelper().setPropertyInRange(sheet, trennRange, TABLE_BORDER2, trennBorder);
        }

        // Alle Spalten und Zeilen auf optimale Breite/Höhe setzen
        getSheetHelper().setOptimaleBreiteUndHoeheAlles(sheet, HAUPTHEADER_ZEILE, letzteZeile, 0, letzteSpalte);
    }

    private void druckBereichSetzen(KaskadenKoRundenPlan plan, List<KaskadenFeldBelegung> belegungen)
            throws GenerateException {
        processBoxinfo("processbox.print.bereich");
        var xSheet = getXSpreadSheet();
        if (xSheet == null) {
            return;
        }
        int maxZeilen = belegungen.stream().mapToInt(b -> b.teamNrs().size()).max().orElse(0);
        if (maxZeilen == 0) {
            return;
        }
        int letzteZeile  = ERSTE_DATEN_ZEILE + maxZeilen - 1;
        int letzteSpalte = belegungen.size() * BLOCK_BREITE - 1;
        var druckBereich = RangePosition.from(0, HAUPTHEADER_ZEILE, letzteSpalte, letzteZeile);
        PrintArea.from(xSheet, getWorkingSpreadsheet()).setPrintArea(druckBereich);
    }

    public boolean isForceOk() {
        return forceOk;
    }

    public void setForceOk(boolean forceOk) {
        this.forceOk = forceOk;
    }
}
