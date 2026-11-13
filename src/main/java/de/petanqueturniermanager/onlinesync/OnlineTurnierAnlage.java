/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteSpaltenDialogOption;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Was ein leeres Dokument anlegen muss, um sich mit einem Online-Turnier verbinden zu lassen: Turniersystem,
 * Formation und Anmeldeart werden aus dem Online-Turnier abgeleitet. Das so angelegte Dokument bewertet
 * {@link OnlineVerbindbarkeit} anschließend als verbindbar.
 *
 * @param formation      Formation der Meldeliste, bei Supermêlée der Spielrunden-Modus; leer bei der Liga
 * @param meleeAnmeldung vorhanden, wenn online Einzelspieler angemeldet werden (Mêlée-Anmeldeliste)
 * @param name           Name des Online-Turniers, bei der Liga der Gruppenname
 */
public record OnlineTurnierAnlage(TurnierSystem turnierSystem, Optional<Formation> formation,
		Optional<MeleeAnmeldungModus> meleeAnmeldung, String name) {

	private static final String ANMELDEART_MELEE = "melee";
	private static final String ANMELDEART_FORME = "forme";

	/** @return leer, wenn PTM für dieses Online-Turnier kein passendes Dokument anlegen kann */
	public static Optional<OnlineTurnierAnlage> aus(OnlineTournamentDto turnier) {
		return TurnierSystemOnlineTypMapping.turnierSystem(turnier.type, turnier.registrationType)
				.flatMap(ts -> fuer(ts, turnier));
	}

	private static Optional<OnlineTurnierAnlage> fuer(TurnierSystem ts, OnlineTournamentDto turnier) {
		Optional<Formation> online = formation(turnier);
		String name = turnier.name == null ? "" : turnier.name;
		return switch (ts) {
			case SUPERMELEE -> online.filter(Formation::erlaubtMeleeAnmeldung)
					.map(f -> new OnlineTurnierAnlage(ts, Optional.of(f), Optional.empty(), name));
			case LIGA -> nurTeamAnmeldung(turnier)
					? Optional.of(new OnlineTurnierAnlage(ts, Optional.empty(), Optional.empty(), name))
					: Optional.empty();
			case TRIPTETE -> online.filter(f -> f == Formation.TRIPLETTE).filter(f -> nurTeamAnmeldung(turnier))
					.map(f -> new OnlineTurnierAnlage(ts, Optional.of(f), Optional.empty(), name));
			case SCHWEIZER, MAASTRICHTER, JGJ, KO, POULE, KASKADE, FORMULEX -> mitAnmeldeart(ts, online, turnier, name);
			case KEIN -> Optional.empty();
		};
	}

	/** Systeme mit Meldeliste und optionaler Mêlée-Anmeldung (alle Formationen des Start-Dialogs). */
	private static Optional<OnlineTurnierAnlage> mitAnmeldeart(TurnierSystem ts, Optional<Formation> online,
			OnlineTournamentDto turnier, String name) {
		if (ANMELDEART_MELEE.equals(turnier.registrationType)) {
			return online.filter(Formation::erlaubtMeleeAnmeldung).map(
					f -> new OnlineTurnierAnlage(ts, Optional.of(f), Optional.of(MeleeAnmeldungModus.EINFACH), name));
		}
		if (!nurTeamAnmeldung(turnier)) {
			return Optional.empty();
		}
		return online.map(f -> new OnlineTurnierAnlage(ts, Optional.of(f), Optional.empty(), name));
	}

	private static boolean nurTeamAnmeldung(OnlineTournamentDto turnier) {
		return ANMELDEART_FORME.equals(turnier.registrationType);
	}

	/** Online-Formation; {@code andere} ist die Meldeliste nur mit Teamnamen. */
	private static Optional<Formation> formation(OnlineTournamentDto turnier) {
		String wert = turnier.formationsWert();
		if (OnlineTournamentDto.FORMATION_ANDERE.equals(wert)) {
			return Optional.of(Formation.NUR_TEAMNAME);
		}
		for (Formation f : new Formation[] { Formation.TETE, Formation.DOUBLETTE, Formation.TRIPLETTE }) {
			if (OnlineVerbindbarkeit.vergleichswert(f).equals(wert)) {
				return Optional.of(f);
			}
		}
		return Optional.empty();
	}

	/** Formation der Meldeliste; nur bei der Liga gibt es keine. */
	public Formation meldelisteFormation() {
		return formation.orElseThrow(() -> new IllegalStateException("Keine Formation für " + turnierSystem));
	}

	/** Teamname-Spalte wie im Start-Dialog: Vorbelegung des Systems, ohne Spielernamen immer. */
	public boolean teamnameAnzeigen(boolean vorbelegung) {
		return vorbelegung || MeldelisteSpaltenDialogOption.teamnameErzwungen(meldelisteFormation());
	}

	/** Verbindbarkeit, die das angelegte Dokument haben wird. */
	public OnlineVerbindbarkeit verbindbarkeit() {
		return OnlineVerbindbarkeit.fuer(turnierSystem, meleeAnmeldung.isPresent(),
				formation.map(OnlineVerbindbarkeit::vergleichswert));
	}
}
