/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheet;
import de.petanqueturniermanager.model.TeamMeldungen;

final class JgjSiegergeldQuelle implements SiegergeldQuelle {

	private static final int MAX_ZEILEN = 500;

	private final WorkingSpreadsheet workingSpreadsheet;

	JgjSiegergeldQuelle(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	@Override
	public List<SiegergeldEintrag> leseTop3() throws GenerateException {
		XSpreadsheet sheet = findeSheet();
		if (sheet == null) {
			return List.of();
		}

		SheetHelper sheetHelper = new SheetHelper(workingSpreadsheet);
		List<SiegergeldEintrag> eintraege = new ArrayList<>();
		for (int zeile = JGJGesamtranglisteSheet.ERSTE_DATEN_ZEILE;
				zeile < JGJGesamtranglisteSheet.ERSTE_DATEN_ZEILE + MAX_ZEILEN; zeile++) {
			String name = sheetHelper.getTextFromCell(sheet, Position.from(JGJGesamtranglisteSheet.TEAM_NAME_SPALTE, zeile));
			if (name == null || name.isBlank()) {
				continue;
			}
			int platz = sheetHelper.getIntFromCell(sheet, Position.from(JGJGesamtranglisteSheet.PLATZ_SPALTE, zeile));
			if (platz < 1 || platz > 3) {
				continue;
			}
			String gruppe = sheetHelper.getTextFromCell(sheet, Position.from(JGJGesamtranglisteSheet.GRUPPE_SPALTE, zeile));
			int nr = sheetHelper.getIntFromCell(sheet, Position.from(JGJGesamtranglisteSheet.TEAM_NR_SPALTE, zeile));
			eintraege.add(new SiegergeldEintrag(gruppe == null || gruppe.isBlank() ? "A" : gruppe, platz, nr, name));
		}
		eintraege.sort(Comparator.comparing(SiegergeldEintrag::gruppe).thenComparingInt(SiegergeldEintrag::platz));
		return eintraege;
	}

	@Override
	public int teilnehmerAnzahl() throws GenerateException {
		TeamMeldungen aktiveMeldungen = new JGJMeldeListeSheet_Update(workingSpreadsheet).getAktiveMeldungen();
		if (aktiveMeldungen.size() > 0) {
			return aktiveMeldungen.size();
		}
		XSpreadsheet sheet = findeSheet();
		if (sheet == null) {
			return 0;
		}

		SheetHelper sheetHelper = new SheetHelper(workingSpreadsheet);
		int anzahl = 0;
		for (int zeile = JGJGesamtranglisteSheet.ERSTE_DATEN_ZEILE;
				zeile < JGJGesamtranglisteSheet.ERSTE_DATEN_ZEILE + MAX_ZEILEN; zeile++) {
			String name = sheetHelper.getTextFromCell(sheet, Position.from(JGJGesamtranglisteSheet.TEAM_NAME_SPALTE, zeile));
			if (name != null && !name.isBlank()) {
				anzahl++;
			}
		}
		return anzahl;
	}

	@Override
	public List<SiegergeldEintrag> allgemeineEintraege() throws GenerateException {
		TeamMeldungen aktiveMeldungen = new JGJMeldeListeSheet_Update(workingSpreadsheet).getAktiveMeldungen();
		List<TeamMeldungen> gruppen = new JGJGesamtranglisteSheet(workingSpreadsheet).ermittleGruppen(aktiveMeldungen);
		if (gruppen.size() < 2) {
			return SiegergeldAllgemeineEintraege.einzelgruppe(3);
		}
		List<String> gruppenBuchstaben = new ArrayList<>();
		for (int i = 0; i < gruppen.size(); i++) {
			gruppenBuchstaben.add(SiegergeldAllgemeineEintraege.gruppenBuchstabe(i));
		}
		return SiegergeldAllgemeineEintraege.gruppen(gruppenBuchstaben, 4);
	}

	private XSpreadsheet findeSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				workingSpreadsheet.getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_JGJ_GESAMTRANGLISTE,
				SheetNamen.LEGACY_JGJ_GESAMTRANGLISTE);
	}
}
