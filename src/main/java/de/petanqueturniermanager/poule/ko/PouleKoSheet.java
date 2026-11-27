/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.ko;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.poule.PouleKoQualifikation;
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
import de.petanqueturniermanager.ptmonline.PtmOnlineLiveAusloeser;

/**
 * Erstellt die KO-Bracket-Sheets (A-Finale, B-Finale) für das Poule-A/B-Turniersystem.
 * <p>
 * Ablauf:
 * <ol>
 *   <li>Vorrunden-Rangliste aktualisieren und deren bereits sortierte Gruppen übernehmen
 *       ({@link PouleVorrundenRanglisteSheetUpdate#getZuletztSortierteGruppen()}) –
 *       kein eigenes, zweites Einlesen des Vorrunde-Sheets</li>
 *   <li>A-Teams und B-Teams mit Cross-Seeding bestimmen ({@link PouleKoQualifikation}); für ausgestiegene
 *       Teams rücken die Teams dahinter in ihrer Gruppe nach</li>
 *   <li>Alte KO-Sheets löschen</li>
 *   <li>KO-Bracket-Sheets für A- und B-Turnier erstellen</li>
 * </ol>
 */
public class PouleKoSheet extends SheetRunner implements ISheet, PtmOnlineLiveAusloeser {

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

        var qualifikation = PouleKoQualifikation.von(gruppenErgebnisse, teamNrn(aktiveMeldungen));
        List<Integer> aTeamsList = qualifikation.aTeams();
        List<Integer> bTeamsList = qualifikation.bTeams();

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

    private static Set<Integer> teamNrn(TeamMeldungen meldungen) {
        return meldungen.teams().stream().map(Team::getNr).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Erstellt ein {@link TeamMeldungen}-Objekt in Setzreihenfolge.
     */
    private static TeamMeldungen erstelleGruppeTeams(List<Integer> teamNrn, TeamMeldungen aktiveMeldungen) {
        var gruppeTeams = new TeamMeldungen();
        for (int teamNr : teamNrn) {
            Team team = aktiveMeldungen.getTeam(teamNr);
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
