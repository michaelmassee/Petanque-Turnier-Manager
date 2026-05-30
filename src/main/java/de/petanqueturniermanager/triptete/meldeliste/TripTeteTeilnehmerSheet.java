package de.petanqueturniermanager.triptete.meldeliste;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser.TeilnehmerNamen;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder.TeilnehmerEintrag;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * Bereinigte Teilnehmerliste für das Trip-Tête-System – als Aushang und Webseite.
 */
public class TripTeteTeilnehmerSheet extends SheetRunner implements ISheet {

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;

    private final TripTeteKonfigurationSheet konfigurationSheet;
    private final TripTeteMeldeListeSheetUpdate meldeliste;

    public TripTeteTeilnehmerSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.TRIPTETE, "TripTete-Teilnehmer");
        konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
        meldeliste = new TripTeteMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    protected TripTeteKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
                SheetNamen.teilnehmer());
    }

    @Override
    public final TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected void doRun() throws GenerateException {
        meldeliste.upDateSheet();
        generate();
    }

    public void generate() throws GenerateException {
        NewSheet.from(this, SheetNamen.teilnehmer(), SheetMetadataHelper.SCHLUESSEL_TEILNEHMER)
                .tabColor(getKonfigurationSheet().getTeilnehmerTabFarbe())
                .pos(DefaultSheetPos.TRIPTETE_WORK)
                .forceCreate().hideGrid().setActiv().create();
        befuelleTeilnehmerDaten();
    }

    protected void befuelleTeilnehmerDaten() throws GenerateException {
        processBoxinfo("processbox.teilnehmer.meldungen.einlesen");
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();

        List<TeilnehmerEintrag> eintraege = new ArrayList<>(aktiveMeldungen.size());
        if (aktiveMeldungen.size() > 0) {
            TeilnehmerNamen namen = TeilnehmerNamenLeser.from(meldeliste, MELDELISTE_ERSTE_DATEN_ZEILE,
                    Formation.TETE, false, false).lesen();
            Map<Integer, String> spielerNamen = namen.spielerNamen();
            Map<Integer, String> teamnamen = namen.teamnamen();
            Map<Integer, String> sortNamen = namen.sortNamen();

            for (Team team : aktiveMeldungen.getTeamList()) {
                int nr = team.getNr();
                eintraege.add(new TeilnehmerEintrag(nr,
                        teamnamen.getOrDefault(nr, ""),
                        spielerNamen.getOrDefault(nr, ""),
                        sortNamen.getOrDefault(nr, "")));
            }
            eintraege.sort(konfigurationSheet.getTeilnehmerListeSortModus().comparator());
        }

        processBoxinfo("processbox.teilnehmer.meldungen.einfuegen", eintraege.size());

        TeilnehmerSheetBuilder builder = TeilnehmerSheetBuilder.from(this)
                .daten(eintraege)
                .teamnameAktiv(false)
                .maxProBlock(konfigurationSheet.getMaxAnzTeilnehmerInSpalte())
                .headerFarbe(konfigurationSheet.getMeldeListeHeaderFarbe())
                .zebraFarben(konfigurationSheet.getMeldeListeHintergrundFarbeGerade(),
                        konfigurationSheet.getMeldeListeHintergrundFarbeUnGerade())
                .aufbauen();

        int letzteSpalte = builder.getLetzteDatenSpalte();
        int footerZeile = builder.getLetzteDatenZeile() + 1;
        StringCellValue footer = StringCellValue.from(getXSpreadSheet(), Position.from(0, footerZeile))
                .setValue(I18n.get("teilnehmer.footer.anzahl", aktiveMeldungen.size()))
                .setEndPosMergeSpalte(letzteSpalte).setCharWeight(FontWeight.BOLD).setCharHeight(12)
                .setShrinkToFit(true);
        getSheetHelper().setStringValueInCell(footer);
        builder.freezeUndPrintbereich(footerZeile);
    }
}
