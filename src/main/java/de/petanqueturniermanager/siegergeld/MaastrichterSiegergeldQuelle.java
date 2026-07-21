/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser.TeilnehmerNamen;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.ko.KoTurnierbaumLayout;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterGruppenSpalteHelper;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

final class MaastrichterSiegergeldQuelle implements SiegergeldQuelle {

	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	private final WorkingSpreadsheet workingSpreadsheet;
	private final MaastrichterKonfigurationSheet konfigurationSheet;
	private final MaastrichterMeldeListeSheetUpdate meldeliste;
	private final SheetHelper sheetHelper;

	MaastrichterSiegergeldQuelle(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
		this.konfigurationSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
		this.meldeliste = new MaastrichterMeldeListeSheetUpdate(workingSpreadsheet);
		this.sheetHelper = new SheetHelper(workingSpreadsheet);
	}

	@Override
	public List<SiegergeldEintrag> leseTop3() throws GenerateException {
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen.size() == 0) {
			return List.of();
		}

		NamenIndex namenIndex = leseNamenIndex();
		Map<String, List<Integer>> gruppen = teamsNachFinalgruppe(aktiveMeldungen);
		List<SiegergeldEintrag> result = new ArrayList<>();
		for (Map.Entry<String, List<Integer>> gruppe : gruppen.entrySet()) {
			XSpreadsheet finalSheet = findeFinalSheet(gruppe.getKey());
			if (finalSheet == null || gruppe.getValue().size() < 2) {
				continue;
			}
			KoTurnierbaumLayout layout = KoTurnierbaumLayout.from(gruppe.getValue().size(),
					konfigurationSheet.getSpielbaumSpielbahn() != SpielrundeSpielbahn.X,
					konfigurationSheet.isSpielbaumBahnNurRunde1(), true,
					konfigurationSheet.isSpielbaumSpielUmPlatz3());
			leseGruppe(finalSheet, layout, gruppe.getKey(), namenIndex, result);
		}
		result.sort(Comparator.comparing(SiegergeldEintrag::gruppe).thenComparingInt(SiegergeldEintrag::platz));
		return result;
	}

	@Override
	public int teilnehmerAnzahl() throws GenerateException {
		return meldeliste.getAktiveMeldungen().size();
	}

	@Override
	public List<SiegergeldEintrag> allgemeineEintraege() throws GenerateException {
		Map<String, List<Integer>> gruppen = teamsNachFinalgruppe(meldeliste.getAktiveMeldungen());
		return SiegergeldAllgemeineEintraege.gruppen(gruppen.keySet(), gruppen.size() > 1 ? 4 : 3);
	}

	private void leseGruppe(XSpreadsheet finalSheet, KoTurnierbaumLayout layout, String gruppe,
			NamenIndex namenIndex, List<SiegergeldEintrag> result) {
		TeamRef platz1 = leseSieger(finalSheet, layout.siegerSpalte(), layout.siegerNameSpalte(),
				layout.siegerZeile(), namenIndex);
		if (platz1.istLeer()) {
			return;
		}
		result.add(platz1.toEintrag(gruppe, 1));

		TeamRef platz2 = leseFinalVerlierer(finalSheet, layout, namenIndex);
		if (!platz2.istLeer()) {
			result.add(platz2.toEintrag(gruppe, 2));
		}

		if (layout.hatSpielUmPlatz3()) {
			TeamRef platz3 = leseSieger(finalSheet, layout.siegerSpalte(), layout.siegerNameSpalte(),
					layout.platz3SiegerZeile(), namenIndex);
			if (!platz3.istLeer()) {
				result.add(platz3.toEintrag(gruppe, 3));
			}
		}
	}

	private TeamRef leseFinalVerlierer(XSpreadsheet finalSheet, KoTurnierbaumLayout layout,
			NamenIndex namenIndex) {
		int runde = layout.numRunden();
		int scoreA = leseInt(finalSheet, layout.scoreSpalte(runde), layout.finaleTeamAZeile());
		int scoreB = leseInt(finalSheet, layout.scoreSpalte(runde), layout.finaleTeamBZeile());
		if (scoreA < 0 || scoreB < 0 || scoreA == scoreB) {
			return TeamRef.leer();
		}
		int verliererZeile = scoreA < scoreB ? layout.finaleTeamAZeile() : layout.finaleTeamBZeile();
		return leseTeam(finalSheet, layout.teamSpalte(runde), verliererZeile, namenIndex);
	}

	private TeamRef leseSieger(XSpreadsheet sheet, int siegerSpalte, int siegerNameSpalte, int zeile,
			NamenIndex namenIndex) {
		if (konfigurationSheet.getSpielbaumTeamAnzeige() == KoSpielbaumTeamAnzeige.NR) {
			int nr = leseInt(sheet, siegerSpalte, zeile);
			String name = sheetHelper.getTextFromCell(sheet, Position.from(siegerNameSpalte, zeile));
			return TeamRef.fromNr(nr, name, namenIndex);
		}
		String name = sheetHelper.getTextFromCell(sheet, Position.from(siegerSpalte, zeile));
		return TeamRef.fromName(name, namenIndex);
	}

	private TeamRef leseTeam(XSpreadsheet sheet, int spalte, int zeile, NamenIndex namenIndex) {
		if (konfigurationSheet.getSpielbaumTeamAnzeige() == KoSpielbaumTeamAnzeige.NR) {
			return TeamRef.fromNr(leseInt(sheet, spalte, zeile), "", namenIndex);
		}
		return TeamRef.fromName(sheetHelper.getTextFromCell(sheet, Position.from(spalte, zeile)), namenIndex);
	}

	private int leseInt(XSpreadsheet sheet, int spalte, int zeile) {
		return sheetHelper.getIntFromCell(sheet, Position.from(spalte, zeile));
	}

	private XSpreadsheet findeFinalSheet(String gruppe) throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(workingSpreadsheet.getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.schluesselMaastrichterFinalrunde(gruppe), SheetNamen.koFinaleGruppe(gruppe));
	}

	private Map<String, List<Integer>> teamsNachFinalgruppe(TeamMeldungen aktiveMeldungen) throws GenerateException {
		Map<Integer, String> teamNrZuGruppe = MaastrichterGruppenSpalteHelper
				.lesePreservedGruppen(new MaastrichterVorrundenRanglisteSheetUpdate(workingSpreadsheet));
		Map<String, List<Integer>> gruppen = new LinkedHashMap<>();
		for (Team team : aktiveMeldungen.getTeamList()) {
			String gruppe = normalisiereGruppe(teamNrZuGruppe.get(team.getNr()));
			if (!gruppe.isEmpty()) {
				gruppen.computeIfAbsent(gruppe, ignored -> new ArrayList<>()).add(team.getNr());
			}
		}
		if (gruppen.isEmpty() && findeFinalSheet("A") != null) {
			List<Integer> alleTeamNrn = new ArrayList<>();
			for (Team team : aktiveMeldungen.getTeamList()) {
				alleTeamNrn.add(team.getNr());
			}
			gruppen.put("A", alleTeamNrn);
		}
		return gruppen;
	}

	private NamenIndex leseNamenIndex() throws GenerateException {
		boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
		TeilnehmerNamen namen = TeilnehmerNamenLeser.from(meldeliste, MELDELISTE_ERSTE_DATEN_ZEILE,
				konfigurationSheet.getMeldeListeFormation(), teamnameAktiv,
				konfigurationSheet.isMeldeListeVereinsnameAnzeigen()).lesen();
		Map<Integer, String> nrZuName = teamnameAktiv ? namen.teamnamen() : namen.spielerNamen();
		Map<String, Integer> nameZuNr = new HashMap<>();
		for (Map.Entry<Integer, String> entry : nrZuName.entrySet()) {
			String key = normalisiereName(entry.getValue());
			if (!key.isEmpty()) {
				nameZuNr.putIfAbsent(key, entry.getKey());
			}
		}
		return new NamenIndex(nrZuName, nameZuNr);
	}

	private static String normalisiereGruppe(String gruppe) {
		return gruppe == null ? "" : gruppe.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalisiereName(String name) {
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}

	private record NamenIndex(Map<Integer, String> nrZuName, Map<String, Integer> nameZuNr) {
	}

	private record TeamRef(int nr, String name) {

		static TeamRef leer() {
			return new TeamRef(-1, "");
		}

		static TeamRef fromNr(int nr, String name, NamenIndex namenIndex) {
			if (nr <= 0) {
				return leer();
			}
			String resolvedName = name == null || name.isBlank() ? namenIndex.nrZuName().getOrDefault(nr, "") : name;
			return new TeamRef(nr, resolvedName);
		}

		static TeamRef fromName(String name, NamenIndex namenIndex) {
			if (name == null || name.isBlank()) {
				return leer();
			}
			String trimmed = name.trim();
			return new TeamRef(namenIndex.nameZuNr().getOrDefault(normalisiereName(trimmed), -1), trimmed);
		}

		boolean istLeer() {
			return nr <= 0 && (name == null || name.isBlank());
		}

		SiegergeldEintrag toEintrag(String gruppe, int platz) {
			return new SiegergeldEintrag(gruppe, platz, nr, name);
		}
	}
}
