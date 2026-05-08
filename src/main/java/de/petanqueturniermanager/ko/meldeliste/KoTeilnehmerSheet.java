package de.petanqueturniermanager.ko.meldeliste;

import java.util.ArrayList;
import java.util.Comparator;
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
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Bereinigte Teilnehmerliste für das K.-O. Turniersystem – als Aushang und Webseite.
 * Listet alle aktiven Teams in einem mehrspaltigen Raster auf, gemeinsame Logik im
 * {@link TeilnehmerSheetBuilder}.
 */
public class KoTeilnehmerSheet extends SheetRunner implements ISheet {

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

    private final KoKonfigurationSheet konfigurationSheet;
    private final KoMeldeListeSheetUpdate meldeliste;

    public KoTeilnehmerSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KO, "KO-Teilnehmer");
        konfigurationSheet = new KoKonfigurationSheet(workingSpreadsheet);
        meldeliste = new KoMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    protected KoKonfigurationSheet getKonfigurationSheet() {
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
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
        }
        meldeliste.upDateSheet();
        generate();
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
        }
    }

    public void generate() throws GenerateException {
        NewSheet.from(this, SheetNamen.teilnehmer(), SheetMetadataHelper.SCHLUESSEL_TEILNEHMER)
                .tabColor(getKonfigurationSheet().getTeilnehmerTabFarbe()).pos(DefaultSheetPos.KO_TURNIERBAUM)
                .forceCreate().hideGrid().setActiv().create();

        processBoxinfo("processbox.teilnehmer.meldungen.einlesen");
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();

        if (aktiveMeldungen.size() == 0) {
            MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.teilnehmer.fehler"))
                    .message(I18n.get("msg.text.keine.meldungen")).show();
            return;
        }

        boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
        TeilnehmerNamen namen = TeilnehmerNamenLeser.from(meldeliste, MELDELISTE_ERSTE_DATEN_ZEILE,
                konfigurationSheet.getMeldeListeFormation(), teamnameAktiv,
                konfigurationSheet.isMeldeListeVereinsnameAnzeigen()).lesen();
        Map<Integer, String> spielerNamen = namen.spielerNamen();
        Map<Integer, String> teamnamen = namen.teamnamen();

        List<TeilnehmerEintrag> eintraege = new ArrayList<>(aktiveMeldungen.size());
        for (Team team : aktiveMeldungen.getTeamList()) {
            int nr = team.getNr();
            eintraege.add(new TeilnehmerEintrag(nr,
                    teamnamen.getOrDefault(nr, ""),
                    spielerNamen.getOrDefault(nr, "")));
        }
        eintraege.sort(Comparator.comparingInt(TeilnehmerEintrag::nr));

        processBoxinfo("processbox.teilnehmer.meldungen.einfuegen", aktiveMeldungen.size());

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
