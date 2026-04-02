/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.ko;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.PouleRanglisteRechner;
import de.petanqueturniermanager.algorithmen.PouleTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt die KO-Bracket-Sheets (A-Finale, B-Finale) für das Poule-A/B-Turniersystem.
 * <p>
 * Ablauf:
 * <ol>
 *   <li>Vorrunden-Rangliste prüfen und ggf. erstellen/aktualisieren</li>
 *   <li>Vorrunde-Sheet lesen und Gruppen-Rankings berechnen</li>
 *   <li>A-Teams und B-Teams mit Cross-Seeding bestimmen</li>
 *   <li>Alte KO-Sheets löschen</li>
 *   <li>KO-Bracket-Sheets für A- und B-Turnier erstellen</li>
 * </ol>
 */
public class PouleKoSheet extends SheetRunner implements ISheet {

    public PouleKoSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.POULE, "Poule-KO");
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        // Kein eigenes Sheet – delegiert an KoTurnierbaumSheet-Sheets
        return null;
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return null;
    }

    @Override
    protected PouleKonfigurationSheet getKonfigurationSheet() {
        return new PouleKonfigurationSheet(getWorkingSpreadsheet());
    }

    @Override
    public void doRun() throws GenerateException {
        processBoxinfo("processbox.poule.ko.erstellen");

        if (!pruefeUndAktualisiereVorrundenRangliste()) {
            return;
        }

        var vorrundeSheet = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                SheetNamen.pouleVorrunde());

        if (vorrundeSheet == null) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("poule.ko.caption"))
                    .message(I18n.get("poule.ko.fehler.keine.ergebnisse"))
                    .show();
            return;
        }

        var gruppenErgebnisse = leseUndSortiereGruppen(vorrundeSheet);

        if (gruppenErgebnisse.isEmpty()) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("poule.ko.caption"))
                    .message(I18n.get("poule.ko.fehler.keine.ergebnisse"))
                    .show();
            return;
        }

        var meldeliste = new PouleMeldeListeSheetUpdate(getWorkingSpreadsheet());
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();

        List<PouleTeamErgebnis> aTeamsList = bestimmeATeams(gruppenErgebnisse);
        List<PouleTeamErgebnis> bTeamsList = bestimmeBTeams(gruppenErgebnisse);

        if (aTeamsList.size() < 2) {
            MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("poule.ko.caption"))
                    .message(I18n.get("poule.ko.fehler.keine.ergebnisse"))
                    .show();
            return;
        }

        // Alte KO-Sheets löschen
        alleKoSheetsLoeschen();

        var konfigAdapter = new PouleKoConfigAdapter(getKonfigurationSheet());
        var koSheet = new KoTurnierbaumSheet(getWorkingSpreadsheet());

        // A-Turnier erstellen
        TeamMeldungen aTeams = erstelleGruppeTeams(aTeamsList, aktiveMeldungen);
        String aSheetName = SheetNamen.koFinaleGruppe("A");
        processBoxinfo("processbox.erstelle.sheet.teams", aSheetName, aTeams.size());
        koSheet.erstelleGruppeBracket(aTeams, aSheetName, DefaultSheetPos.POULE_KO,
                konfigAdapter, SheetMetadataHelper.schluesselPouleKo("A"));

        // B-Turnier erstellen (falls genug Teams)
        if (bTeamsList.size() >= 2) {
            SheetRunner.testDoCancelTask();
            TeamMeldungen bTeams = erstelleGruppeTeams(bTeamsList, aktiveMeldungen);
            String bSheetName = SheetNamen.koFinaleGruppe("B");
            processBoxinfo("processbox.erstelle.sheet.teams", bSheetName, bTeams.size());
            koSheet.erstelleGruppeBracket(bTeams, bSheetName,
                    (short) (DefaultSheetPos.POULE_KO + 1),
                    konfigAdapter, SheetMetadataHelper.schluesselPouleKo("B"));
        }
    }

    /**
     * Liest das Vorrunde-Sheet und berechnet die sortierten Ergebnisse pro Gruppe.
     *
     * @return Liste von sortierten Gruppen (Platz 1 zuerst), leere Liste wenn keine Daten
     */
    private List<List<PouleTeamErgebnis>> leseUndSortiereGruppen(XSpreadsheet vorrundeSheet)
            throws GenerateException {

        var readRange = RangePosition.from(
                AbstractPouleVorrundeSheet.SPALTE_POULE_NR,
                AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE,
                AbstractPouleVorrundeSheet.SPALTE_ERG_B,
                AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE + 9999);

        var rowsData = RangeHelper
                .from(vorrundeSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
                .getDataFromRange();

        List<Map<Integer, int[]>> gruppenRoh = new ArrayList<>();
        Map<Integer, int[]> aktuelleGruppe = null;

        for (RowData row : rowsData) {
            if (row.size() < 7) {
                break;
            }

            int pouleNrZellWert = row.get(0).getIntVal(0);
            int teamANr = row.get(1).getIntVal(0);
            int teamBNr = row.get(3).getIntVal(0);

            if (teamANr == 0 && teamBNr == 0) {
                aktuelleGruppe = null;
                continue;
            }

            if (pouleNrZellWert > 0 || aktuelleGruppe == null) {
                aktuelleGruppe = new HashMap<>();
                gruppenRoh.add(aktuelleGruppe);
            }

            int ergA = row.get(5).getIntVal(0);
            int ergB = row.get(6).getIntVal(0);

            if (teamANr > 0 && teamBNr == 0) {
                aktuelleGruppe.computeIfAbsent(teamANr, k -> new int[4])[0]++;
                continue;
            }

            if (teamANr <= 0) {
                continue;
            }

            aktuelleGruppe.computeIfAbsent(teamANr, k -> new int[4]);
            aktuelleGruppe.computeIfAbsent(teamBNr, k -> new int[4]);

            if (ergA > 0 || ergB > 0) {
                aktuelleGruppe.get(teamANr)[2] += ergA;
                aktuelleGruppe.get(teamANr)[3] += ergB;
                aktuelleGruppe.get(teamBNr)[2] += ergB;
                aktuelleGruppe.get(teamBNr)[3] += ergA;

                if (ergA > ergB) {
                    aktuelleGruppe.get(teamANr)[0]++;
                    aktuelleGruppe.get(teamBNr)[1]++;
                } else if (ergB > ergA) {
                    aktuelleGruppe.get(teamBNr)[0]++;
                    aktuelleGruppe.get(teamANr)[1]++;
                }
            }
        }

        var rechner = new PouleRanglisteRechner();
        var sortiertGruppen = new ArrayList<List<PouleTeamErgebnis>>();

        for (var gruppeRoh : gruppenRoh) {
            var ergebnisse = new ArrayList<PouleTeamErgebnis>();
            for (var entry : gruppeRoh.entrySet()) {
                int teamNr = entry.getKey();
                int[] stats = entry.getValue();
                ergebnisse.add(new PouleTeamErgebnis(teamNr, stats[0], stats[1],
                        stats[2] - stats[3], stats[2], List.of()));
            }
            sortiertGruppen.add(rechner.sortiere(ergebnisse));
        }

        return sortiertGruppen;
    }

    /**
     * Bestimmt die A-Turnier-Teams mit Cross-Seeding über Gruppenpaare.
     * <p>
     * Gruppenpaare (G1,G2), (G3,G4), ... werden zusammengefasst.
     * Für jedes Paar: erst alle Platz-1-Teams (G1P1, G2P1, G3P1, ...) dann
     * alle Platz-2-Teams in umgekehrter Gruppenreihenfolge (G_n P2, ..., G1 P2),
     * damit benachbarte Teams im Turnierbaum aus verschiedenen Gruppen kommen.
     */
    private List<PouleTeamErgebnis> bestimmeATeams(List<List<PouleTeamErgebnis>> gruppen) {
        var aTeams = new ArrayList<PouleTeamErgebnis>();

        // Erst alle Platz-1-Teams in Paarfolge
        for (int i = 0; i < gruppen.size(); i += 2) {
            if (!gruppen.get(i).isEmpty()) {
                aTeams.add(gruppen.get(i).get(0));
            }
            if (i + 1 < gruppen.size() && !gruppen.get(i + 1).isEmpty()) {
                aTeams.add(gruppen.get(i + 1).get(0));
            }
        }

        // Dann alle Platz-2-Teams in umgekehrter Reihenfolge
        for (int i = gruppen.size() - 1; i >= 0; i--) {
            if (gruppen.get(i).size() >= 2) {
                aTeams.add(gruppen.get(i).get(1));
            }
        }

        return aTeams;
    }

    /**
     * Bestimmt die B-Turnier-Teams (Platz 3 und ggf. 4 aller Gruppen).
     */
    private List<PouleTeamErgebnis> bestimmeBTeams(List<List<PouleTeamErgebnis>> gruppen) {
        var bTeams = new ArrayList<PouleTeamErgebnis>();

        // Platz-3-Teams in Paarfolge
        for (int i = 0; i < gruppen.size(); i += 2) {
            if (gruppen.get(i).size() >= 3) {
                bTeams.add(gruppen.get(i).get(2));
            }
            if (i + 1 < gruppen.size() && gruppen.get(i + 1).size() >= 3) {
                bTeams.add(gruppen.get(i + 1).get(2));
            }
        }

        // Platz-4-Teams in umgekehrter Reihenfolge
        for (int i = gruppen.size() - 1; i >= 0; i--) {
            if (gruppen.get(i).size() >= 4) {
                bTeams.add(gruppen.get(i).get(3));
            }
        }

        return bTeams;
    }

    /**
     * Erstellt ein {@link TeamMeldungen}-Objekt aus der sortierten Ergebnisliste.
     */
    private TeamMeldungen erstelleGruppeTeams(List<PouleTeamErgebnis> ergebnisse,
            TeamMeldungen aktiveMeldungen) {
        var gruppeTeams = new TeamMeldungen();
        for (var erg : ergebnisse) {
            Team team = aktiveMeldungen.getTeam(erg.teamNr());
            if (team != null) {
                gruppeTeams.addTeamWennNichtVorhanden(team);
            }
        }
        return gruppeTeams;
    }

    /**
     * Prüft ob die Vorrunden-Rangliste vorhanden ist. Wenn nicht, wird der Benutzer
     * gefragt ob sie erstellt werden soll. Die Rangliste wird immer aktualisiert.
     *
     * @return true wenn Rangliste vorhanden und aktualisiert, false wenn abgebrochen
     */
    private boolean pruefeUndAktualisiereVorrundenRangliste() throws GenerateException {
        var ranglisteUpdate = new PouleVorrundenRanglisteSheetUpdate(getWorkingSpreadsheet());
        if (ranglisteUpdate.getXSpreadSheet() == null) {
            MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
                    .caption(I18n.get("poule.ko.vorrunden.rangliste.fehlt.caption"))
                    .message(I18n.get("poule.ko.vorrunden.rangliste.fehlt.text"))
                    .show();
            if (result != MessageBoxResult.YES) {
                return false;
            }
        }
        processBoxinfo("processbox.rangliste.aktualisieren");
        ranglisteUpdate.doRun();
        return true;
    }

    /**
     * Löscht alle vorhandenen KO-Sheets (A-Finale bis Z-Finale).
     */
    private void alleKoSheetsLoeschen() throws GenerateException {
        for (char c = 'A'; c <= 'Z'; c++) {
            String sheetName = SheetNamen.koFinaleGruppe(String.valueOf(c));
            if (getSheetHelper().findByName(sheetName) != null) {
                getSheetHelper().removeSheet(sheetName);
            }
        }
    }
}
