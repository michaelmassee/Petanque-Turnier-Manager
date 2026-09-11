/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.ko;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.poule.PouleTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt die KO-Bracket-Sheets (A-Finale, B-Finale) für das Poule-A/B-Turniersystem.
 * <p>
 * Ablauf:
 * <ol>
 *   <li>Vorrunden-Rangliste aktualisieren und deren bereits sortierte Gruppen übernehmen
 *       ({@link PouleVorrundenRanglisteSheetUpdate#getZuletztSortierteGruppen()}) –
 *       kein eigenes, zweites Einlesen des Vorrunde-Sheets</li>
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

        PouleVorrundenRanglisteSheetUpdate ranglisteUpdate = pruefeUndAktualisiereVorrundenRangliste();
        if (ranglisteUpdate == null) {
            return;
        }

        // Bereits von der Vorrunden-Rangliste berechnete und geschriebene Gruppeneinteilung
        // wiederverwenden – kein zweites Einlesen des Vorrunde-Sheets, damit KO-Bracket und
        // Rangliste niemals auseinanderlaufen können.
        var gruppenErgebnisse = ranglisteUpdate.getZuletztSortierteGruppen();

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
        List<KoTurnierbaumSheet.GruppenBracketAuftrag> bracketAuftraege = new ArrayList<>();

        // A-Turnier erstellen
        TeamMeldungen aTeams = erstelleGruppeTeams(aTeamsList, aktiveMeldungen);
        String aSheetName = SheetNamen.koFinaleGruppe("A");
        processBoxinfo("processbox.erstelle.sheet.teams", aSheetName, aTeams.size());
        bracketAuftraege.add(new KoTurnierbaumSheet.GruppenBracketAuftrag(
                aTeams, aSheetName, DefaultSheetPos.POULE_KO,
                SheetMetadataHelper.schluesselPouleKo("A"), null));

        // B-Turnier erstellen (falls genug Teams)
        if (bTeamsList.size() >= 2) {
            SheetRunner.testDoCancelTask();
            TeamMeldungen bTeams = erstelleGruppeTeams(bTeamsList, aktiveMeldungen);
            String bSheetName = SheetNamen.koFinaleGruppe("B");
            processBoxinfo("processbox.erstelle.sheet.teams", bSheetName, bTeams.size());
            bracketAuftraege.add(new KoTurnierbaumSheet.GruppenBracketAuftrag(
                    bTeams, bSheetName, (short) (DefaultSheetPos.POULE_KO + 1),
                    SheetMetadataHelper.schluesselPouleKo("B"), null));
        }
        koSheet.erstelleGruppenBrackets(bracketAuftraege, konfigAdapter);
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
     * @return die aktualisierte Vorrunden-Rangliste (zum Weiterverwenden ihrer sortierten
     *         Gruppen), oder {@code null} wenn der Benutzer abgebrochen hat
     */
    private PouleVorrundenRanglisteSheetUpdate pruefeUndAktualisiereVorrundenRangliste() throws GenerateException {
        var ranglisteUpdate = new PouleVorrundenRanglisteSheetUpdate(getWorkingSpreadsheet());
        if (ranglisteUpdate.getXSpreadSheet() == null) {
            MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
                    .caption(I18n.get("poule.ko.vorrunden.rangliste.fehlt.caption"))
                    .message(I18n.get("poule.ko.vorrunden.rangliste.fehlt.text"))
                    .show();
            if (result != MessageBoxResult.YES) {
                return null;
            }
        }
        processBoxinfo("processbox.rangliste.aktualisieren");
        ranglisteUpdate.doRun();
        return ranglisteUpdate;
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
