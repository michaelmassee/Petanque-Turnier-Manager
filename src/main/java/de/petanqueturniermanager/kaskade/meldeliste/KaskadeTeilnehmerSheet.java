package de.petanqueturniermanager.kaskade.meldeliste;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser.TeilnehmerNamen;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder.TeilnehmerEintrag;
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
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Bereinigte Teilnehmerliste für das Kaskaden-KO-Turniersystem – als Aushang und Webseite.
 * Listet alle aktiven Teams in einem mehrspaltigen Raster auf, gemeinsame Logik im
 * {@link TeilnehmerSheetBuilder}.
 */
public class KaskadeTeilnehmerSheet extends SheetRunner implements ISheet {

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    private final KaskadeKonfigurationSheet konfigurationSheet;
    private final KaskadeMeldeListeSheetUpdate meldeliste;

    public KaskadeTeilnehmerSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KASKADE, "Kaskaden-Teilnehmer");
        konfigurationSheet = new KaskadeKonfigurationSheet(workingSpreadsheet);
        meldeliste = new KaskadeMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    protected KaskadeKonfigurationSheet getKonfigurationSheet() {
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
                .tabColor(konfigurationSheet.getTeilnehmerTabFarbe()).pos(DefaultSheetPos.KO_TURNIERBAUM)
                .forceCreate().hideGrid().setActiv().create();
        befuelleTeilnehmerDaten();
    }

    /**
     * Schreibt Header und Datenbereich der Teilnehmerliste anhand der aktuellen Meldeliste.
     * Setzt voraus, dass das Teilnehmer-Sheet bereits existiert. Bei leerer Meldeliste wird
     * dennoch eine gültige (leere) Teilnehmerliste mit Header, Footer und Druckbereich erstellt.
     */
    protected void befuelleTeilnehmerDaten() throws GenerateException {
        processBoxinfo("processbox.teilnehmer.meldungen.einlesen");
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();

        boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();

        List<TeilnehmerEintrag> eintraege = new ArrayList<>(aktiveMeldungen.size());
        if (aktiveMeldungen.size() > 0) {
            TeilnehmerNamen namen = TeilnehmerNamenLeser.from(meldeliste, MELDELISTE_ERSTE_DATEN_ZEILE,
                    konfigurationSheet.getMeldeListeFormation(), teamnameAktiv,
                    konfigurationSheet.isMeldeListeVereinsnameAnzeigen()).lesen();
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
                .teamnameAktiv(teamnameAktiv)
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
