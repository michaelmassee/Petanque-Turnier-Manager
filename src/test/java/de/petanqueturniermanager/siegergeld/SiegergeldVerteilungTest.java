/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SiegergeldVerteilungTest {

	@Test
	void platzAnteilDefaultIstSechzigDreissigZehn() {
		assertThat(SiegergeldVerteilung.platzAnteil(1)).isEqualTo(60);
		assertThat(SiegergeldVerteilung.platzAnteil(2)).isEqualTo(30);
		assertThat(SiegergeldVerteilung.platzAnteil(3)).isEqualTo(10);
		assertThat(SiegergeldVerteilung.platzAnteil(4)).isZero();
	}

	@Test
	void gruppenAnteilDefaultGiltFuerAllePlaetzeDerErstenGruppe() {
		assertThat(SiegergeldVerteilung.gruppenAnteil("A", "A")).isEqualTo(100);
		assertThat(SiegergeldVerteilung.gruppenAnteil("A", "B")).isZero();
	}

	@Test
	void allgemeineEintraegeEnthaltenLeerePlaetzeProGruppe() {
		assertThat(SiegergeldAllgemeineEintraege.gruppen(List.of("A", "B"), 4))
				.extracting(SiegergeldEintrag::gruppe, SiegergeldEintrag::platz,
						SiegergeldEintrag::nr, SiegergeldEintrag::name)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("A", 1, 0, ""),
						org.assertj.core.groups.Tuple.tuple("A", 2, 0, ""),
						org.assertj.core.groups.Tuple.tuple("A", 3, 0, ""),
						org.assertj.core.groups.Tuple.tuple("A", 4, 0, ""),
						org.assertj.core.groups.Tuple.tuple("B", 1, 0, ""),
						org.assertj.core.groups.Tuple.tuple("B", 2, 0, ""),
						org.assertj.core.groups.Tuple.tuple("B", 3, 0, ""),
						org.assertj.core.groups.Tuple.tuple("B", 4, 0, ""));
	}

	@Test
	void betragFormelBerechnetMitSeparatemGruppenUndPlatzanteil() {
		List<SiegergeldEintrag> eintraege = SiegergeldAllgemeineEintraege.gruppen(List.of("A", "B"), 4);
		Map<String, Integer> gruppenStartZeilen = new LinkedHashMap<>();
		gruppenStartZeilen.put("A", 8);
		gruppenStartZeilen.put("B", 12);

		assertThat(SiegergeldSheet.betragFormel(8, gruppenStartZeilen, eintraege))
				.isEqualTo("=$B$6*D9/100*E9/100");
		assertThat(SiegergeldSheet.betragFormel(13, gruppenStartZeilen, eintraege))
				.isEqualTo("=$B$6*D13/100*E14/100");
	}

	@Test
	void betragAufgerundetFormelRundetBetragAufGanzeEinheitAuf() {
		assertThat(SiegergeldSheet.betragAufgerundetFormel(8)).isEqualTo("=CEILING(F9;1)");
	}

	@Test
	void summenFormelSummiertBetragsspalten() {
		assertThat(SiegergeldSheet.summenFormel(2, 11)).isEqualTo("=SUM(C9:C12)");
		assertThat(SiegergeldSheet.summenFormel(5, 11)).isEqualTo("=SUM(F9:F12)");
		assertThat(SiegergeldSheet.summenFormel(6, 11)).isEqualTo("=SUM(G9:G12)");
	}

	@Test
	void prozentSummenWerdenGetrenntBerechnet() {
		List<SiegergeldEintrag> eintraege = SiegergeldAllgemeineEintraege.gruppen(List.of("A", "B"), 4);
		Map<String, Integer> gruppenStartZeilen = new LinkedHashMap<>();
		gruppenStartZeilen.put("A", 8);
		gruppenStartZeilen.put("B", 12);

		assertThat(SiegergeldSheet.gruppenanteilSummeFormel(15)).isEqualTo("=SUM(D9:D16)");
		assertThat(SiegergeldSheet.platzanteilSummeFormel("A", gruppenStartZeilen, eintraege))
				.isEqualTo("=SUM(E9:E12)");
		assertThat(SiegergeldSheet.platzanteilSummeFormel("B", gruppenStartZeilen, eintraege))
				.isEqualTo("=SUM(E13:E16)");
	}

	@Test
	void gruppenEndZeileErmitteltBlockFuerZusammengefuehrteGruppenzellen() {
		List<SiegergeldEintrag> eintraege = SiegergeldAllgemeineEintraege.gruppen(List.of("A", "B"), 4);
		Map<String, Integer> gruppenStartZeilen = new LinkedHashMap<>();
		gruppenStartZeilen.put("A", 8);
		gruppenStartZeilen.put("B", 12);

		assertThat(SiegergeldSheet.gruppenEndZeile("A", gruppenStartZeilen, eintraege)).isEqualTo(11);
		assertThat(SiegergeldSheet.gruppenEndZeile("B", gruppenStartZeilen, eintraege)).isEqualTo(15);
	}

	@Test
	void eintraegeWerdenNachGruppeUndPlatzSortiert() {
		assertThat(SiegergeldSheet.sortierteEintraege(List.of(
				new SiegergeldEintrag("B", 2, 0, ""),
				new SiegergeldEintrag("A", 3, 0, ""),
				new SiegergeldEintrag("B", 1, 0, ""),
				new SiegergeldEintrag("A", 1, 0, ""))))
				.extracting(SiegergeldEintrag::gruppe, SiegergeldEintrag::platz)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("A", 1),
						org.assertj.core.groups.Tuple.tuple("A", 3),
						org.assertj.core.groups.Tuple.tuple("B", 1),
						org.assertj.core.groups.Tuple.tuple("B", 2));
	}
}
