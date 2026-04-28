/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.FormuleXErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Erstellt die nächste Spielrunde nach dem Formule X System.
 * <p>
 * Runde 1 wird frei ausgelost, ab Runde 2 wird nach akkumulierter Wertung gepaart (1vs2, 3vs4 ...).
 * Die Anzahl Runden ist durch die Konfiguration begrenzt.
 */
public class FormuleXSpielrundeSheetNaechste extends FormuleXAbstractSpielrundeSheet {

    public FormuleXSpielrundeSheetNaechste(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        getxCalculatable().enableAutomaticCalculation(false);
        processBoxinfo("processbox.naechste.spielrunde", getSpielRundeNr().getNr());
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
        }
        naechsteSpielrundeEinfuegen();
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
        }
    }

    public boolean naechsteSpielrundeEinfuegen() throws GenerateException {
        SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
        setSpielRundeNrInSheet(aktuelleSpielrunde);
        getMeldeListe().vollstaendigAktualisieren();
        TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

        if (aktiveMeldungen.size() == 0) {
            TeamMeldungen alleMeldungen = getMeldeListe().getAlleMeldungen();
            if (alleMeldungen.size() > 0) {
                MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
                        .caption(I18n.get("msg.caption.keine.aktiven.meldungen"))
                        .message(I18n.get("msg.text.keine.aktiven.teams.aktivieren", alleMeldungen.size()))
                        .show();
                if (result == MessageBoxResult.YES) {
                    getMeldeListe().alleTeamsAktivieren();
                    aktiveMeldungen = getMeldeListe().getAktiveMeldungen();
                }
            }
        }

        if (!canStart(aktiveMeldungen)) {
            return false;
        }

        int neueSpielrundeNr = aktuelleSpielrunde.getNr();
        if (getXSpreadSheet() != null) {
            if (!alleErgebnisseEingetragen(aktuelleSpielrunde)) {
                getSheetHelper().setActiveSheet(getXSpreadSheet());
                MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                        .caption(I18n.get("msg.caption.naechste.runde.nicht.moeglich"))
                        .message(I18n.get("msg.text.naechste.runde.ergebnisse.fehlen", aktuelleSpielrunde.getNr()))
                        .show();
                return false;
            }
            neueSpielrundeNr++;
        }

        int maxRunden = getKonfigurationSheet().getAnzahlRunden();
        if (neueSpielrundeNr > maxRunden) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("msg.caption.formulex.max.runden"))
                    .message(I18n.get("msg.text.formulex.max.runden.erreicht", maxRunden))
                    .show();
            return false;
        }

        getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(neueSpielrundeNr));

        List<FormuleXErgebnis> ergebnisse = gespieltenRundenEinlesen(aktiveMeldungen, 1, neueSpielrundeNr - 1);

        return neueSpielrunde(aktiveMeldungen, SpielRundeNr.from(neueSpielrundeNr), ergebnisse);
    }

    private boolean alleErgebnisseEingetragen(SpielRundeNr spielRundeNr) throws GenerateException {
        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                getSpielrundeSchluessel(spielRundeNr.getNr()), getSheetName(spielRundeNr));
        if (sheet == null) {
            return true;
        }
        RangePosition readRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE,
                ERSTE_DATEN_ZEILE + 999);
        RangeData rowsData = RangeHelper
                .from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange).getDataFromRange();

        for (RowData row : rowsData) {
            if (row.size() < 2) {
                break;
            }
            int nrA = row.get(0).getIntVal(0);
            if (nrA <= 0) {
                break;
            }
            int nrB = row.get(1).getIntVal(0);
            if (nrB <= 0) {
                continue; // Freilos-Zeile, kein Ergebnis nötig
            }
            int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
            int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;
            if (ergA == 0 && ergB == 0) {
                return false;
            }
        }
        return true;
    }
}
