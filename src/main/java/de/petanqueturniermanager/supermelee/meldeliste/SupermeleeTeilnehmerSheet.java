package de.petanqueturniermanager.supermelee.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
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
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.toolbar.TurnierModus;

public class SupermeleeTeilnehmerSheet extends SheetRunner implements ISheet {

    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 2;
    private static final int MELDELISTE_NR_SPALTE = 0;
    private static final int MELDELISTE_NAME_SPALTE = 1;

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

        meldeliste.doSort(meldeliste.getSpielerNameErsteSpalte(), true);

        processBoxinfo("processbox.spieltag.meldungen.einlesen", getSpielTagNr().getNr());
        SpielerMeldungen aktiveUndAusgesetzt = meldeliste.getAktiveUndAusgesetztMeldungen();

        if (aktiveUndAusgesetzt.size() == 0) {
            MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.teilnehmer.fehler"))
                    .message(I18n.get("msg.text.keine.meldungen")).show();
            return;
        }

        Map<Integer, String> spielerNamen = leseSpielerNamenAusMeldeliste();

        List<TeilnehmerEintrag> eintraege = new ArrayList<>(aktiveUndAusgesetzt.size());
        for (Spieler spieler : aktiveUndAusgesetzt.getSpielerList()) {
            int nr = spieler.getNr();
            eintraege.add(new TeilnehmerEintrag(nr, "", spielerNamen.getOrDefault(nr, "")));
        }

        processBoxinfo("processbox.spieltag.meldungen.einfuegen", getSpielTagNr().getNr(), aktiveUndAusgesetzt.size());

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
                .setValue(I18n.get("teilnehmer.footer.anzahl", aktiveUndAusgesetzt.size()))
                .setEndPosMergeSpalte(letzteSpalte).setCharWeight(FontWeight.BOLD).setCharHeight(12)
                .setShrinkToFit(true);
        getSheetHelper().setStringValueInCell(footer);

        SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(aktiveUndAusgesetzt.size());
        footer.zeilePlusEins().setValue(I18n.get("teilnehmer.footer.teams",
                teamRechner.getAnzDoublette(), teamRechner.getAnzTriplette()));
        getSheetHelper().setStringValueInCell(footer);
        footer.zeilePlusEins().setValue(I18n.get("teilnehmer.footer.bahnen", teamRechner.getAnzBahnen()));
        getSheetHelper().setStringValueInCell(footer);

        builder.freezeUndPrintbereich(footer.getPos().getZeile());

        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE).ifPresent(
                    k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
        }
    }

    private Map<Integer, String> leseSpielerNamenAusMeldeliste() throws GenerateException {
        Map<Integer, String> result = new HashMap<>();
        XSpreadsheet xMeldeliste = meldeliste.getXSpreadSheet();
        if (xMeldeliste == null) {
            return result;
        }
        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        RangeData data = RangeHelper.from(xMeldeliste, xDoc,
                RangePosition.from(MELDELISTE_NR_SPALTE, MELDELISTE_ERSTE_DATEN_ZEILE,
                        MELDELISTE_NAME_SPALTE, MELDELISTE_ERSTE_DATEN_ZEILE + 999)).getDataFromRange();
        for (RowData row : data) {
            if (row.isEmpty()) {
                break;
            }
            int nr = row.get(0).getIntVal(0);
            if (nr <= 0) {
                break;
            }
            String name = row.size() > 1 ? row.get(1).getStringVal() : "";
            result.put(nr, name != null ? name : "");
        }
        return result;
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
