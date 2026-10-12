/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.meldeliste.TeamAnzeigeIndex;

class SchweizerTeamNrAufloeserTest {

	private static final String UUID_A = "aaaaaaaa-0000-4000-8000-000000000001";
	private static final String UUID_B = "bbbbbbbb-0000-4000-8000-000000000002";
	private static final String UUID_GELOESCHT = "cccccccc-0000-4000-8000-000000000003";
	/** Meldeliste: UUID_A hat jetzt Nr 12, UUID_B Nr 4. */
	private static final MeldelisteUuidIndex UUID_INDEX = MeldelisteUuidIndex.aus(
			new String[][] { { "=PTM.ALG.TNRUUID(12;\"" + UUID_A + "\")" }, { "=PTM.ALG.TNRUUID(4;\"" + UUID_B + "\")" } },
			new int[] { 12, 4 });

	private final AtomicInteger anzIndexLaden = new AtomicInteger();

	@Test
	void vonHandKorrigierteNummerOhneUuid() throws GenerateException {
		RowData zeile = zeile(7.0, 8.0);

		var aufloeser = aufloeser(new TeamAnzeigeIndex());

		assertThat(aufloeser.teamA(zeile, "")).isEqualTo(7);
		assertThat(aufloeser.teamB(zeile, "")).isEqualTo(8);
		assertThat(anzIndexLaden).hasValue(0);
	}

	@Test
	void vonHandKorrigierterNameLoestUeberMeldelisteAuf() throws GenerateException {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("Anna / Berta", 3);
		index.hinzufuegen("Carl / Dora", 4);
		RowData zeile = zeile("Anna / Berta", "Carl / Dora");

		var aufloeser = aufloeser(index);

		assertThat(aufloeser.teamA(zeile, "")).isEqualTo(3);
		assertThat(aufloeser.teamB(zeile, "")).isEqualTo(4);
		assertThat(anzIndexLaden).as("Meldeliste nur einmal lesen").hasValue(1);
	}

	@Test
	void leererGegnerIstFreilos() throws GenerateException {
		RowData zeile = zeile("Anna / Berta", "");

		assertThat(aufloeser(new TeamAnzeigeIndex()).teamB(zeile, "")).isZero();
	}

	@Test
	void leereZeileIstDatenende() throws GenerateException {
		assertThat(aufloeser(new TeamAnzeigeIndex()).teamA(zeile("", ""), "")).isZero();
	}

	@Test
	void mehrdeutigerGegnerWirdNichtAlsFreilosGewertet() {
		TeamAnzeigeIndex index = new TeamAnzeigeIndex();
		index.hinzufuegen("Anna", 3);
		index.hinzufuegen("Anna", 5);
		RowData zeile = zeile("Carl", "Anna");

		assertThatThrownBy(() -> aufloeser(index).teamB(zeile, "")).isInstanceOf(GenerateException.class);
	}

	@Test
	void unbekannteKennungWirdNichtAlsDatenendeGewertet() {
		RowData zeile = zeile("Gelöschtes Team", "");

		assertThatThrownBy(() -> aufloeser(new TeamAnzeigeIndex()).teamA(zeile, ""))
				.isInstanceOf(GenerateException.class);
	}

	@Test
	void uuidInFormelHatVorrangUndLiefertAktuelleNummer() throws GenerateException {
		// Team 7 wurde in der Meldeliste auf 12 umnummeriert; die Zelle zeigt noch den alten Wert
		RowData zeile = zeile(7.0, 8.0);

		var aufloeser = aufloeser(new TeamAnzeigeIndex());

		assertThat(aufloeser.teamA(zeile, formelMit(UUID_A))).isEqualTo(12);
		assertThat(aufloeser.teamB(zeile, formelMit(UUID_B))).isEqualTo(4);
		assertThat(anzIndexLaden).hasValue(0);
	}

	@Test
	void uuidOhneMeldungIstNichtInMeldeliste() throws GenerateException {
		RowData zeile = zeile("Gelöscht", "");

		assertThat(aufloeser(new TeamAnzeigeIndex()).teamA(zeile, formelMit(UUID_GELOESCHT)))
				.isEqualTo(SchweizerTeamNrAufloeser.NICHT_IN_MELDELISTE);
	}

	private SchweizerTeamNrAufloeser aufloeser(TeamAnzeigeIndex index) {
		return new SchweizerTeamNrAufloeser(() -> {
			anzIndexLaden.incrementAndGet();
			return index;
		}, () -> UUID_INDEX);
	}

	private static String formelMit(String uuid) {
		return "=INDEX($'Meldeliste'.$A$1:$A$999;PTM.ALG.UUIDZEILE(\"" + uuid + "\";$'Meldeliste'.$A$1:$A$999))";
	}

	/** Zeile mit den Teamzellen A und B (ab TEAM_A_SPALTE). */
	private static RowData zeile(Object teamA, Object teamB) {
		RowData zeile = new RowData();
		zeile.add(new CellData(teamA));
		zeile.add(new CellData(teamB));
		return zeile;
	}
}
