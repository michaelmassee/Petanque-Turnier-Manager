/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

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
					turnierSystemBezeichnung(turnier)));
		}
		String erwarteteAnmeldeart = TurnierSystemOnlineTypMapping.onlineRegistrationTyp(turnierSystem,
				meleeAnmeldung);
		if (!erwarteteAnmeldeart.equals(turnier.registrationType)) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.anmeldeart",
					anmeldeartBezeichnung(turnier.registrationType), anmeldeartBezeichnung(erwarteteAnmeldeart)));
		}
		if (dokumentFormation.isPresent() && !dokumentFormation.get().equals(turnier.formationsWert())) {
			return Optional.of(I18n.get("ptmonline.turnier.verbinden.grund.formation",
					formationBezeichnung(turnier.formationsWert()), formationBezeichnung(dokumentFormation.get())));
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

	/** Bezeichnung des lokalen Turniersystems zum Online-Typ; ein unbekannter Typ erscheint unverändert. */
	private static String turnierSystemBezeichnung(OnlineTournamentDto turnier) {
		return Arrays.stream(TurnierSystem.values())
				.filter(ts -> TurnierSystemOnlineTypMapping.onlineTyp(ts).orElse("").equals(turnier.type))
				.findFirst().map(TurnierSystem::getBezeichnung).orElse(StringUtils.defaultString(turnier.type));
	}

	private static String anmeldeartBezeichnung(String anmeldeart) {
		return switch (StringUtils.defaultString(anmeldeart)) {
			case "forme", "melee", "supermelee" -> I18n.get("ptmonline.anmeldeart." + anmeldeart);
			default -> StringUtils.defaultString(anmeldeart);
		};
	}

	private static String formationBezeichnung(String formation) {
		return Arrays.stream(Formation.values())
				.filter(f -> f != Formation.MELEE && f != Formation.NUR_TEAMNAME)
				.filter(f -> vergleichswert(f).equals(formation)).findFirst().map(Formation::getBezeichnung)
				.orElseGet(() -> OnlineTournamentDto.FORMATION_ANDERE.equals(formation)
						? I18n.get("ptmonline.formation.andere")
						: StringUtils.defaultString(formation));
	}
}
