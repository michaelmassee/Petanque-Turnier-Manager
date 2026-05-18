/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.kaskade.KaskadeTurnierTestDaten;

/**
 * UI-Tests für den Kaskade-Rangliste-Listener-Pfad
 * ({@link KaskadeGruppenRanglisteSheetUpdate}) und den Hook in
 * {@link KaskadeSpielrundeSheet}. Die Gruppenrangliste entsteht erst am Ende
 * aller Kaskadenrunden (über {@code KaskadeKoFeldSheet}); danach prüft der
 * Listener-Pfad, dass {@link KaskadeGruppenRanglisteSheetUpdate} überlebt
 * geschriebene Daten korrekt überschreibt (Block-Write).
 */
public class KaskadeGruppenRanglisteHookUITest extends BaseCalcUITest {

	@BeforeEach
	public void seed() {
		RandomSource.setSeed(42L);
	}

	/**
	 * Korrumpiert eine Zelle im Datenbereich der Gruppenrangliste und prüft,
	 * dass {@link KaskadeGruppenRanglisteSheetUpdate#doRun()} sie aus den
	 * Kaskadenrunden-Sheets zurückschreibt. Damit ist das gesamte Update-
	 * Verhalten (Existenz-Check, Block-Read, Block-Write) abgesichert.
	 */
	@Test
	public void testGruppenranglisteUpdateUeberschreibtStaleDaten() throws GenerateException {
		new KaskadeTurnierTestDaten(wkingSpreadsheet).generate();

		String name = SheetNamen.kaskadeGruppenrangliste();
		XSpreadsheet gruppenRangliste = sheetHlp.findByName(name);
		assertThat(gruppenRangliste)
				.as("Gruppenrangliste muss nach KaskadeTurnierTestDaten existieren")
				.isNotNull();

		// Team-Nr-Spalte der ersten Gruppe (Block-Nr-Offset=1, erste Datenzeile=3)
		RangePosition erstePos = RangePosition.from(1, 3, 1, 3);
		RangeHelper zelle = RangeHelper.from(gruppenRangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), erstePos);
		int originalTeamNr = zelle.getDataFromRange().get(0).get(0).getIntVal(-1);
		assertThat(originalTeamNr).as("Originaler Team-Nr Eintrag muss > 0 sein").isPositive();

		// Künstlich auf 999 setzen (kein realer Wert)
		zelle.setDataInRange(new RangeData(new Object[][] { { 999 } }));
		int verschmutzt = zelle.getDataFromRange().get(0).get(0).getIntVal(-1);
		assertThat(verschmutzt).isEqualTo(999);

		// Update ausführen – das ist exakt der Pfad, den der Listener triggern würde
		new KaskadeGruppenRanglisteSheetUpdate(wkingSpreadsheet).doRun();

		int restored = zelle.getDataFromRange().get(0).get(0).getIntVal(-1);
		assertThat(restored)
				.as("KaskadeGruppenRanglisteSheetUpdate muss den korrekten Team-Nr-Wert wiederherstellen")
				.isEqualTo(originalTeamNr);
	}

	/**
	 * Wenn die Gruppenrangliste noch nicht existiert (= Kaskaden-Turnier nur
	 * teilweise gestartet, keine KO-Felder erstellt), darf der Hook im
	 * {@link KaskadeSpielrundeSheet}-Flow sie <em>nicht</em> durch die Hintertür
	 * anlegen. Wird vom {@link KaskadenRanglistenAktualisierer} via Existenz-
	 * Guard sichergestellt.
	 */
	@Test
	public void testHookErzeugtKeineGruppenranglisteWennNichtVorhanden() throws GenerateException {
		// Direkter Aktualisierer-Aufruf ohne vorhandenes Sheet – darf nicht crashen
		// und darf das Sheet nicht erzeugen.
		String name = SheetNamen.kaskadeGruppenrangliste();
		assertThat(sheetHlp.findByName(name))
				.as("Vorbedingung: Gruppenrangliste existiert nicht")
				.isNull();

		new KaskadenRanglistenAktualisierer(wkingSpreadsheet, sheetHlp).aktualisiereRanglisten();

		assertThat(sheetHlp.findByName(name))
				.as("Aktualisierer darf Sheet nicht via Hintertür anlegen")
				.isNull();
	}

	/**
	 * Regression im Kiosk-Modus: nach Vollaufbau muss ein erneutes
	 * {@link KaskadeGruppenRanglisteSheetUpdate#doRun()} unter aktivem TurnierModus +
	 * Kaskade-Blattschutz sauber durchlaufen.
	 */
	@Test
	public void kioskModus_gruppenranglisteUpdateUnterSchutz() throws GenerateException {
		new KaskadeTurnierTestDaten(wkingSpreadsheet).generate();
		mitKioskModus(TurnierSystem.KASKADE, () ->
				new KaskadeGruppenRanglisteSheetUpdate(wkingSpreadsheet).doRun());

		assertThat(sheetHlp.findByName(SheetNamen.kaskadeGruppenrangliste()))
				.as("Kaskade-Gruppenrangliste muss nach Kiosk-Update existieren")
				.isNotNull();
	}
}
