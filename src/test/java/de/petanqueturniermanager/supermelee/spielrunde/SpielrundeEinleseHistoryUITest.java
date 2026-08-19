/*
 * Erstellung : 2026-05-18 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;

/**
 * Regressionstest für {@link SpielrundeDelegate#gespieltenRundenEinlesen}.<br>
 * <br>
 * Hintergrund: Nach Sessions/Neustart (oder Re-Auslosung einer Runde) wird die
 * komplette Spielhistorie der Spieler ausschließlich über das Wieder-Einlesen
 * der persistierten Spielrunden-Sheets rekonstruiert. Dieser Test stellt sicher,
 * dass dabei <b>sowohl</b> {@link Spieler#warImTeamMit} <b>als auch</b>
 * {@link Spieler#warGegnerVon} korrekt wiederhergestellt werden — letzteres
 * fehlte historisch und führte bei Mehr-Runden-/Mehr-Spieltage-Turnieren zu
 * doppelten Gegner-Paarungen, weil {@link de.petanqueturniermanager.algorithmen.supermelee.SuperMeleePaarungenV2}
 * die Gegner-Optimierung dann auf einem leeren Score-Modell betrieb.
 */
public class SpielrundeEinleseHistoryUITest extends BaseCalcUITest {

	private static final int ANZ_SPIELER = 24; // Triplette: 8 Teams pro Runde

	private TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen;
	private MeldeListeSheet_NeuerSpieltag meldeListeNeuerSpieltag;
	private SpielrundeSheet_Naechste spielrundeSheetNaechste;

	@BeforeEach
	public void setUp() throws GenerateException {
		testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_SPIELER);
		meldeListeNeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
		spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
	}

	/**
	 * Zwei Runden auf Spieltag 1 auslosen. Anschließend mit frischer
	 * {@link SpielerMeldungen} {@code gespieltenRundenEinlesen} aufrufen
	 * (Simulation Session-Neustart / Re-Auslosung) und prüfen, dass jede
	 * persistierte Mitspieler- und Gegner-Beziehung rekonstruiert wird.
	 */
	@Test
	public void einlesen_einSpieltag_rekonstruiertMitspielerUndGegner() throws GenerateException {
		spielrundeSheetNaechste.run(); // 1.1
		spielrundeSheetNaechste.run(); // 1.2

		Map<Integer, Set<Integer>> erwarteteMitspieler = new HashMap<>();
		Map<Integer, Set<Integer>> erwarteteGegner = new HashMap<>();
		sammlePaarungenAusSheet(SpielTagNr.from(1), 1, erwarteteMitspieler, erwarteteGegner);
		sammlePaarungenAusSheet(SpielTagNr.from(1), 2, erwarteteMitspieler, erwarteteGegner);
		validiereErwartungenNichtLeer(erwarteteMitspieler, erwarteteGegner);

		SpielerMeldungen meldungen = frischeAktiveMeldungen();
		spielrundeSheetNaechste.setSpielTag(SpielTagNr.from(1));
		spielrundeSheetNaechste.setSpielRundeNr(SpielRundeNr.from(3));

		spielrundeSheetNaechste.gespieltenRundenEinlesen(meldungen, 1, 2);

		assertHistorie(meldungen, erwarteteMitspieler, erwarteteGegner);
	}

	/**
	 * Wochenend-Szenario: zwei Spieltage, beide mit gespielten Runden. Beim
	 * Auslosen einer neuen Runde auf Spieltag 2 müssen sowohl die Runden des
	 * aktuellen als auch die des vorigen Spieltags vollständig in die Historie
	 * einfließen — inklusive Gegner.
	 */
	@Test
	public void einlesen_zweiSpieltage_rekonstruiertHistorieKomplett() throws GenerateException {
		spielrundeSheetNaechste.run(); // 1.1
		spielrundeSheetNaechste.run(); // 1.2
		meldeListeNeuerSpieltag.naechsteSpieltag();
		testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(SpielTagNr.from(2));
		spielrundeSheetNaechste.run(); // 2.1

		Map<Integer, Set<Integer>> erwarteteMitspieler = new HashMap<>();
		Map<Integer, Set<Integer>> erwarteteGegner = new HashMap<>();
		sammlePaarungenAusSheet(SpielTagNr.from(1), 1, erwarteteMitspieler, erwarteteGegner);
		sammlePaarungenAusSheet(SpielTagNr.from(1), 2, erwarteteMitspieler, erwarteteGegner);
		sammlePaarungenAusSheet(SpielTagNr.from(2), 1, erwarteteMitspieler, erwarteteGegner);
		validiereErwartungenNichtLeer(erwarteteMitspieler, erwarteteGegner);

		SpielerMeldungen meldungen = frischeAktiveMeldungen();
		spielrundeSheetNaechste.setSpielTag(SpielTagNr.from(2));
		spielrundeSheetNaechste.setSpielRundeNr(SpielRundeNr.from(2));

		spielrundeSheetNaechste.gespieltenRundenEinlesen(meldungen, 1, 1);

		assertHistorie(meldungen, erwarteteMitspieler, erwarteteGegner);
	}

	private SpielerMeldungen frischeAktiveMeldungen() throws GenerateException {
		SpielerMeldungen meldungen = spielrundeSheetNaechste.getMeldeListe().getAktiveMeldungen();
		assertThat(meldungen.size()).as("aktive Meldungen müssen alle Spieler enthalten").isEqualTo(ANZ_SPIELER);
		for (Spieler s : meldungen.spieler()) {
			assertThat(s.anzahlMitSpieler())
					.as("Spieler %d sollte vor Einlesen keine Mitspieler-Historie haben", s.getNr())
					.isZero();
		}
		return meldungen;
	}

	/**
	 * Liest aus dem persistierten Spielrunden-Sheet alle Spieler-Paarungen ab
	 * und schreibt sie in die Erwartungs-Maps. Symmetrische Eintragung
	 * (a→b und b→a), passend zur gegenseitigen Registrierung im Modell.
	 */
	private void sammlePaarungenAusSheet(SpielTagNr spielTag, int spielrunde,
			Map<Integer, Set<Integer>> mitspielerOut, Map<Integer, Set<Integer>> gegnerOut) throws GenerateException {
		String sheetName = spielrundeSheetNaechste.getSheetName(spielTag, SpielRundeNr.from(spielrunde));
		XSpreadsheet xsheet = sheetHlp.findByName(sheetName);
		assertThat(xsheet).as("Spielrunden-Sheet '%s' muss existieren", sheetName).isNotNull();

		int zeile = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
		for (int safety = 0; safety < 1000; safety++) {
			int paarungMarker = sheetHlp.getIntFromCell(xsheet,
					Position.from(SpielrundeSheetKonstanten.PAARUNG_CNTR_SPALTE, zeile));
			if (paarungMarker == -1) {
				return; // Ende der Daten
			}
			List<Integer> team1 = leseSpielerNrTeam(xsheet, zeile, 0);
			List<Integer> team2 = leseSpielerNrTeam(xsheet, zeile, 3);
			eintragenSymmetrisch(mitspielerOut, team1, team1);
			eintragenSymmetrisch(mitspielerOut, team2, team2);
			eintragenSymmetrisch(gegnerOut, team1, team2);
			zeile++;
		}
	}

	private List<Integer> leseSpielerNrTeam(XSpreadsheet xsheet, int zeile, int spaltenOffset) {
		List<Integer> nummern = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			int nr = sheetHlp.getIntFromCell(xsheet,
					Position.from(SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE + spaltenOffset + i, zeile));
			if (nr > 0) {
				nummern.add(nr);
			}
		}
		return nummern;
	}

	private static void eintragenSymmetrisch(Map<Integer, Set<Integer>> map,
			List<Integer> linke, List<Integer> rechte) {
		for (int a : linke) {
			for (int b : rechte) {
				if (a == b) {
					continue;
				}
				map.computeIfAbsent(a, k -> new HashSet<>()).add(b);
				map.computeIfAbsent(b, k -> new HashSet<>()).add(a);
			}
		}
	}

	private static void validiereErwartungenNichtLeer(Map<Integer, Set<Integer>> mitspieler,
			Map<Integer, Set<Integer>> gegner) {
		assertThat(mitspieler).as("Mitspieler-Erwartung darf nicht leer sein").isNotEmpty();
		assertThat(gegner).as("Gegner-Erwartung darf nicht leer sein").isNotEmpty();
	}

	private static void assertHistorie(SpielerMeldungen meldungen,
			Map<Integer, Set<Integer>> erwarteteMitspieler,
			Map<Integer, Set<Integer>> erwarteteGegner) {
		for (Map.Entry<Integer, Set<Integer>> entry : erwarteteMitspieler.entrySet()) {
			Spieler a = meldungen.findSpielerByNr(entry.getKey());
			assertThat(a).as("Spieler %d aus Erwartung muss in Meldungen existieren", entry.getKey()).isNotNull();
			for (int bNr : entry.getValue()) {
				Spieler b = meldungen.findSpielerByNr(bNr);
				assertThat(b).as("Spieler %d aus Erwartung muss in Meldungen existieren", bNr).isNotNull();
				assertThat(a.warImTeamMit(b))
						.as("warImTeamMit fehlt: Spieler %d ↔ Spieler %d", a.getNr(), b.getNr())
						.isTrue();
			}
		}
		for (Map.Entry<Integer, Set<Integer>> entry : erwarteteGegner.entrySet()) {
			Spieler a = meldungen.findSpielerByNr(entry.getKey());
			assertThat(a).isNotNull();
			for (int bNr : entry.getValue()) {
				Spieler b = meldungen.findSpielerByNr(bNr);
				assertThat(b).isNotNull();
				assertThat(a.warGegnerVon(b))
						.as("warGegnerVon fehlt: Spieler %d ↔ Spieler %d", a.getNr(), b.getNr())
						.isTrue();
			}
		}
	}
}
