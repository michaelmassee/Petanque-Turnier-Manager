/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Entscheidet für jedes Online-Turnier, ob sich das Dokument damit verbinden lässt, und nennt sonst den Grund:
 * abgeschlossen, anderes Turniersystem, andere Anmeldeart oder andere Formation. Die Verbinden-Auswahl zeigt so alle
 * Turniere des API-Keys, nicht verbindbare mit Begründung.
 * <p>
 * Ein leeres Dokument ({@link #fuerLeeresDokument()}) legt Turniersystem, Formation und Anmeldeart erst aus dem
 * gewählten Online-Turnier an ({@link OnlineTurnierAnlage}). Verbindbar ist dort jedes abbildbare Turnier, das weder
 * abgeschlossen noch mit einem anderen Dokument verbunden ist.
 */
public final class OnlineVerbindbarkeit {

	/**
	 * @param hinderungsgrund leer, wenn das Turnier verbunden werden kann
	 */
	public record Bewertung(OnlineTournamentDto turnier, Optional<String> hinderungsgrund) {

		public boolean istVerbindbar() {
			return hinderungsgrund.isEmpty();
		}
	}

	private static final OnlineVerbindbarkeit LEERES_DOKUMENT = new OnlineVerbindbarkeit(TurnierSystem.KEIN, false,
			Optional.empty());

	private final TurnierSystem turnierSystem;
	private final boolean meleeAnmeldung;
	private final Optional<String> dokumentFormation;

	private OnlineVerbindbarkeit(TurnierSystem turnierSystem, boolean meleeAnmeldung,
			Optional<String> dokumentFormation) {
		this.turnierSystem = turnierSystem;
		this.meleeAnmeldung = meleeAnmeldung;
		this.dokumentFormation = dokumentFormation;
	}

	/**
	 * @param dokumentFormation Formation des Dokuments als Vergleichswert ({@link #vergleichswert(Formation)}); leer
	 *                          bei Turniersystemen ohne Formation, dann entfällt der Vergleich
	 */
	public static OnlineVerbindbarkeit fuer(TurnierSystem turnierSystem, boolean meleeAnmeldung,
			Optional<String> dokumentFormation) {
		return new OnlineVerbindbarkeit(turnierSystem, meleeAnmeldung, dokumentFormation);
	}

	/** Bewertung für ein Dokument ohne Turniersystem, das erst aus dem gewählten Online-Turnier angelegt wird. */
	public static OnlineVerbindbarkeit fuerLeeresDokument() {
		return LEERES_DOKUMENT;
	}

	public boolean istLeeresDokument() {
		return turnierSystem == TurnierSystem.KEIN;
	}

	/** Formation so, wie PTM-Online sie in der Turnierliste meldet; ohne feste Teamgröße {@code andere}. */
	public static String vergleichswert(Formation formation) {
		return switch (formation) {
			case TETE, DOUBLETTE, TRIPLETTE -> formation.name().toLowerCase(Locale.ROOT);
			case MELEE, NUR_TEAMNAME -> OnlineTournamentDto.FORMATION_ANDERE;
		};
	}

	public List<Bewertung> bewerte(List<OnlineTournamentDto> turniere) {
		return turniere.stream().map(this::bewerte).toList();
	}

	Bewertung bewerte(OnlineTournamentDto turnier) {
		return new Bewertung(turnier, hinderungsgrund(turnier));
	}

	private Optional<String> hinderungsgrund(OnlineTournamentDto turnier) {
		if (turnier.istAbgeschlossen()) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.abgeschlossen"));
		}
		if (istLeeresDokument()) {
			return hinderungsgrundLeeresDokument(turnier);
		}
		if (!TurnierSystemOnlineTypMapping.onlineTyp(turnierSystem).orElse("").equals(turnier.type)) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.turniersystem",
					OnlineTurnierEckdaten.turnierSystem(turnier)));
		}
		String erwarteteAnmeldeart = TurnierSystemOnlineTypMapping.onlineRegistrationTyp(turnierSystem,
				meleeAnmeldung);
		if (!erwarteteAnmeldeart.equals(turnier.registrationType)) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.anmeldeart",
					OnlineTurnierEckdaten.anmeldeart(turnier.registrationType), OnlineTurnierEckdaten.anmeldeart(erwarteteAnmeldeart)));
		}
		if (dokumentFormation.isPresent() && !dokumentFormation.get().equals(turnier.formationsWert())) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.formation",
					OnlineTurnierEckdaten.formationBezeichnung(turnier.formationsWert()), OnlineTurnierEckdaten.formationBezeichnung(dokumentFormation.get())));
		}
		return Optional.empty();
	}

	private static Optional<String> hinderungsgrundLeeresDokument(OnlineTournamentDto turnier) {
		if (turnier.documentManaged) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.bereits_verbunden"));
		}
		if (OnlineTurnierAnlage.aus(turnier).isEmpty()) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.nicht_unterstuetzt"));
		}
		return Optional.empty();
	}
}
