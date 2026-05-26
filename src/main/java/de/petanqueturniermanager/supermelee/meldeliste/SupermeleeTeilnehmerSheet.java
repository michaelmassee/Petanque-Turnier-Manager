package de.petanqueturniermanager.supermelee.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder.TeilnehmerEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

public class SupermeleeTeilnehmerSheet extends SheetRunner implements ISheet {

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 2;
    private static final int MELDELISTE_NR_SPALTE = 0;

    private final SuperMeleeKonfigurationSheet konfigurationSheet;
    private final MeldeListeSheet_Update meldeliste;
    private SpielTagNr spielTagNr = null;

    public SupermeleeTeilnehmerSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Teilnehmer");
        konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
        meldeliste = new MeldeListeSheet_Update(workingSpreadsheet);
    }

    @Override
    protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.schluesselSupermeleeTeilnehmer(getSpielTagNr().getNr()),
                getSheetName(getSpielTagNr()));
    }

    @Override
    public final TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected void doRun() throws GenerateException {
        setSpielTagNr(getKonfigurationSheet().getAktiveSpieltag());
        meldeliste.setSpielTag(getSpielTagNr());
        meldeliste.upDateSheet();
        generate();
    }

    public void generate() throws GenerateException {
        meldeliste.setSpielTag(getSpielTagNr());

        NewSheet.from(this, getSheetName(getSpielTagNr()),
                SheetMetadataHelper.schluesselSupermeleeTeilnehmer(getSpielTagNr().getNr()))
                .tabColor(konfigurationSheet.getTeilnehmerTabFarbe()).pos(DefaultSheetPos.SUPERMELEE_WORK)
                .spielTagPageStyle(getSpielTagNr())
                .forceCreate().hideGrid().setActiv().create();

        befuelleTeilnehmerDaten();
    }

    /**
     * Schreibt Header, Spielerblöcke und Footer in das bereits existierende
     * Teilnehmer-Sheet. Wird sowohl vom Vollaufbau ({@link #generate()}) als auch
     * vom Update-Pfad ({@code SupermeleeTeilnehmerSheetUpdate}) genutzt. Bei leerer
     * Meldeliste wird dennoch eine gültige (leere) Teilnehmerliste mit Header, Footer
     * und Druckbereich erstellt.
     */
    protected void befuelleTeilnehmerDaten() throws GenerateException {
        meldeliste.setSpielTag(getSpielTagNr());

        processBoxinfo("processbox.spieltag.meldungen.einlesen", getSpielTagNr().getNr());
        SpielerMeldungen aktiveUndAusgesetzt = meldeliste.getAktiveUndAusgesetztMeldungen();

        List<TeilnehmerEintrag> eintraege = new ArrayList<>(aktiveUndAusgesetzt.size());
        if (aktiveUndAusgesetzt.size() > 0) {
            Map<Integer, String> spielerNamen = leseSpielerNamenAusMeldeliste();
            for (Spieler spieler : aktiveUndAusgesetzt.getSpielerList()) {
                int nr = spieler.getNr();
                eintraege.add(new TeilnehmerEintrag(nr, "", spielerNamen.getOrDefault(nr, "")));
            }
            eintraege.sort(Comparator.comparingInt(TeilnehmerEintrag::nr));
        }

        processBoxinfo("processbox.spieltag.meldungen.einfuegen", getSpielTagNr().getNr(), eintraege.size());

        TeilnehmerSheetBuilder builder = TeilnehmerSheetBuilder.from(this)
                .daten(eintraege)
                .teamnameAktiv(false)
                .maxProBlock(konfigurationSheet.getMaxAnzTeilnehmerInSpalte())
                .spielerSpalteWidth(SuperMeleeKonfigurationSheet.SUPER_MELEE_MELDUNG_NAME_WIDTH)
                .headerFarbe(konfigurationSheet.getMeldeListeHeaderFarbe())
                .zebraFarben(konfigurationSheet.getMeldeListeHintergrundFarbeGerade(),
                        konfigurationSheet.getMeldeListeHintergrundFarbeUnGerade())
                .aufbauen();

        int letzteSpalte = builder.getLetzteDatenSpalte();
        int footerZeile = builder.getLetzteDatenZeile() + 1;
        StringCellValue footer = StringCellValue.from(getXSpreadSheet(), Position.from(0, footerZeile))
                .setValue(I18n.get("teilnehmer.footer.anzahl", eintraege.size()))
                .setEndPosMergeSpalte(letzteSpalte).setCharWeight(FontWeight.BOLD).setCharHeight(12)
                .setShrinkToFit(true);
        getSheetHelper().setStringValueInCell(footer);

        SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(eintraege.size());
        footer.zeilePlusEins().setValue(I18n.get("teilnehmer.footer.teams",
                teamRechner.getAnzDoublette(), teamRechner.getAnzTriplette()));
        getSheetHelper().setStringValueInCell(footer);
        footer.zeilePlusEins().setValue(I18n.get("teilnehmer.footer.bahnen", teamRechner.getAnzBahnen()));
        getSheetHelper().setStringValueInCell(footer);

        builder.freezeUndPrintbereich(footer.getPos().getZeile());
    }

    private Map<Integer, String> leseSpielerNamenAusMeldeliste() throws GenerateException {
        Map<Integer, String> result = new HashMap<>();
        XSpreadsheet xMeldeliste = meldeliste.getXSpreadSheet();
        if (xMeldeliste == null) {
            return result;
        }
        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        var spalte = meldeliste.getMeldungenSpalte();
        int ersteNameSpalte = spalte.getErsteMeldungNameSpalte();
        int letzteNameSpalte = spalte.getLetzteMeldungNameSpalte();

        RangeData data = RangeHelper.from(xMeldeliste, xDoc,
                RangePosition.from(MELDELISTE_NR_SPALTE, MELDELISTE_ERSTE_DATEN_ZEILE,
                        letzteNameSpalte, MELDELISTE_ERSTE_DATEN_ZEILE + 999)).getDataFromRange();
        for (RowData row : data) {
            if (row.isEmpty()) {
                break;
            }
            int nr = row.get(0).getIntVal(0);
            if (nr <= 0) {
                break;
            }
            String name = kombiniereName(row, ersteNameSpalte, letzteNameSpalte);
            result.put(nr, name);
        }
        return result;
    }

    /** Liest die Namens-Spalten aus {@code row} und liefert "Nachname, Vorname" (bzw. ein Name bei 1 Spalte). */
    private static String kombiniereName(RowData row, int ersteNameSpalte, int letzteNameSpalte) {
        if (ersteNameSpalte == letzteNameSpalte) {
            String name = row.size() > ersteNameSpalte ? row.get(ersteNameSpalte).getStringVal() : "";
            return name != null ? name.trim() : "";
        }
        String vorname = row.size() > ersteNameSpalte && row.get(ersteNameSpalte).getStringVal() != null
                ? row.get(ersteNameSpalte).getStringVal().trim() : "";
        String nachname = row.size() > letzteNameSpalte && row.get(letzteNameSpalte).getStringVal() != null
                ? row.get(letzteNameSpalte).getStringVal().trim() : "";
        if (nachname.isEmpty()) {
            return vorname;
        }
        if (vorname.isEmpty()) {
            return nachname;
        }
        return nachname + ", " + vorname;
    }

    public String getSheetName(SpielTagNr spieltagNr) {
        return SheetNamen.teilnehmer(spieltagNr.getNr());
    }

    public SpielTagNr getSpielTagNr() {
        checkNotNull(spielTagNr, "spielTagNr == null");
        return spielTagNr;
    }

    public void setSpielTagNr(SpielTagNr spielTag) {
        checkNotNull(spielTag, "spielTagNr == null");
        spielTagNr = spielTag;
    }
}
