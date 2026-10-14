/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.NrZellFormel;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.kompatibilitaet.DokumentKompatibilitaet.Ergebnis;
import de.petanqueturniermanager.comp.kompatibilitaet.DokumentKompatibilitaet.Status;

class DokumentKompatibilitaetTest {

	private static final String UUID = "0b8c1f8e-3c55-4b7a-9d1e-2f6a7c9e1a01";
	private static final Optional<String> PLUGIN = Optional.of("5.40.0");
	private static final DokumentMigration NICHT_MIGRIERBAR = schritt("5.40.00", false);
	private static final DokumentMigration MIGRIERBAR = schritt("5.41.00", true);

	@Test
	void dokumentVorDerGrenzeIstZuAltUndNenntSie() {
		Ergebnis ergebnis = bewerte(Optional.of("5.39.12"), List.of(NICHT_MIGRIERBAR), false);

		assertThat(ergebnis.status()).isEqualTo(Status.ZU_ALT);
		assertThat(ergebnis.grenzVersion()).contains("5.40.00");
	}

	@Test
	void dokumentAbDerGrenzeIstKompatibel() {
		assertThat(bewerte(Optional.of("5.40.0"), List.of(NICHT_MIGRIERBAR), true).status())
				.as("die Version entscheidet, nicht der Inhalt (eingefügte nummerierte Liste)")
				.isEqualTo(Status.KOMPATIBEL);
	}

	@Test
	void nurMigrierbareOffeneSchritteBrauchenMigration() {
		Ergebnis ergebnis = bewerte(Optional.of("5.40.0"), List.of(NICHT_MIGRIERBAR, MIGRIERBAR), false);

		assertThat(ergebnis.status()).isEqualTo(Status.MIGRATION_NOETIG);
		assertThat(ergebnis.offeneSchritte()).containsExactly(MIGRIERBAR);
	}

	@Test
	void mehrereOffeneSchritteLaufenAufsteigendNachVersion() throws Exception {
		List<String> ausgefuehrt = new ArrayList<>();
		DokumentMigration v542 = protokollierenderSchritt("5.42.00", ausgefuehrt);
		DokumentMigration v541 = protokollierenderSchritt("5.41.00", ausgefuehrt);
		DokumentMigration v5410 = protokollierenderSchritt("5.41.10", ausgefuehrt);

		Ergebnis ergebnis = DokumentKompatibilitaet.bewerte(Optional.of("5.41.0"), Optional.of("5.42.0"),
				List.of(v542, v541, v5410), schritt -> false);
		DokumentKompatibilitaet.migriere(ergebnis.offeneSchritte(), null);

		assertThat(ergebnis.status()).isEqualTo(Status.MIGRATION_NOETIG);
		assertThat(ausgefuehrt).as("5.41.00 ist erledigt, der Rest in Versionsreihenfolge")
				.containsExactly("5.41.10", "5.42.00");
	}

	@Test
	void einNichtMigrierbarerSchrittMachtDasDokumentZuAlt() {
		assertThat(bewerte(Optional.of("5.30.0"), List.of(NICHT_MIGRIERBAR, MIGRIERBAR), false).status())
				.isEqualTo(Status.ZU_ALT);
	}

	@Test
	void ohneVersionEntscheidetDerSchrittAmInhalt() {
		assertThat(bewerte(Optional.empty(), List.of(NICHT_MIGRIERBAR), true).status()).isEqualTo(Status.ZU_ALT);
		assertThat(bewerte(Optional.empty(), List.of(NICHT_MIGRIERBAR), false).status())
				.as("z. B. ein gerade neu angelegtes Dokument").isEqualTo(Status.KOMPATIBEL);
	}

	@Test
	void dokumentEinerNeuerenPluginVersion() {
		assertThat(bewerte(Optional.of("5.41.0"), List.of(NICHT_MIGRIERBAR), false).status())
				.isEqualTo(Status.NEUER_ALS_PLUGIN);
	}

	@Test
	void teamUuidUmstellungErkenntDasAlteInhaltsformat() {
		assertThat(TeamUuidUmstellung.istAltesInhaltsformat(new String[][] { { "Nr" }, { "1" }, { "2" } },
				new int[] { 0, 1, 2 })).as("nur reine Zahlen").isTrue();
		String[][] mitUuid = { { "Nr" }, { NrZellFormel.team(UUID).formel(1) }, { "2" } };
		assertThat(TeamUuidUmstellung.istAltesInhaltsformat(mitUuid, new int[] { 0, 1, 2 })).as("eine UUID")
				.isFalse();
		assertThat(TeamUuidUmstellung.istAltesInhaltsformat(new String[][] { { "Nr" }, { "" } }, new int[] { 0, 0 }))
				.as("leere Meldeliste").isFalse();
	}

	@Test
	void teamUuidUmstellungIstDieErsteGrenzeUndNichtMigrierbar() {
		assertThat(DokumentKompatibilitaet.SCHRITTE).first().satisfies(schritt -> {
			assertThat(schritt.abVersion()).isEqualTo("5.40.00");
			assertThat(schritt.istMigrierbar()).isFalse();
		});
	}

	@Test
	void hinweisNurBeiZuAltOderNeuerAlsPlugin() {
		assertThat(DokumentKompatibilitaetsHinweis
				.hinweisText(bewerte(Optional.of("5.39.0"), List.of(NICHT_MIGRIERBAR), false))).isPresent();
		assertThat(DokumentKompatibilitaetsHinweis
				.hinweisText(bewerte(Optional.of("5.41.0"), List.of(NICHT_MIGRIERBAR), false))).isPresent();
		assertThat(DokumentKompatibilitaetsHinweis
				.hinweisText(bewerte(Optional.of("5.40.0"), List.of(NICHT_MIGRIERBAR, MIGRIERBAR), false)))
				.as("Migration läuft still beim nächsten Lauf").isEmpty();
	}

	private static Ergebnis bewerte(Optional<String> dokumentVersion, List<DokumentMigration> schritte,
			boolean noetigOhneVersion) {
		return DokumentKompatibilitaet.bewerte(dokumentVersion, PLUGIN, schritte, schritt -> noetigOhneVersion);
	}

	private static DokumentMigration protokollierenderSchritt(String abVersion, List<String> ausgefuehrt) {
		return new DokumentMigration() {
			@Override
			public String abVersion() {
				return abVersion;
			}

			@Override
			public boolean istMigrierbar() {
				return true;
			}

			@Override
			public void migriere(WorkingSpreadsheet ws) {
				ausgefuehrt.add(abVersion);
			}
		};
	}

	private static DokumentMigration schritt(String abVersion, boolean migrierbar) {
		return new DokumentMigration() {
			@Override
			public String abVersion() {
				return abVersion;
			}

			@Override
			public boolean istMigrierbar() {
				return migrierbar;
			}
		};
	}
}
