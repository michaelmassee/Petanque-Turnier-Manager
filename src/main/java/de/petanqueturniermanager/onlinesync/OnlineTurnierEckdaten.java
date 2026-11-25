/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.speicher.PtmOnlineSyncStatus;

/**
 * Lesbare Eckdaten eines PTM-Online-Turniers für die Verbinden-Auswahl: Turniersystem, Formation, Anmeldeart, Beginn,
 * Ort, Status sowie Anmeldungen und Teilnehmergrenze. Unbekannte Online-Werte erscheinen unverändert, damit auch Turnierarten, die PTM
 * nicht anlegen kann, erkennbar bleiben.
 */
public final class OnlineTurnierEckdaten {

	private static final Logger logger = LogManager.getLogger(OnlineTurnierEckdaten.class);
	private static final String TRENNER_KURZ = " · ";
	private static final String TRENNER_DETAIL = "   ";
	private static final String ANMELDEART_SUPERMELEE = "supermelee";

	private OnlineTurnierEckdaten() {
	}

	/** Einzeilige Kurzform für einen Listeneintrag, z. B. {@code Schweizer · Doublette · Formée}. */
	public static String kurz(OnlineTournamentDto turnier) {
		return verbinde(TRENNER_KURZ, turnierSystem(turnier), formation(turnier), anmeldeartOhneSupermelee(turnier));
	}

	/** Mehrzeilige Detailanzeige des ausgewählten Turniers. */
	public static String details(OnlineTournamentDto turnier) {
		return String.join("\n",
				verbinde(TRENNER_DETAIL, feld("turniersystem", turnierSystem(turnier)),
						feld("formation", formation(turnier)), feld("anmeldeart", anmeldeart(turnier.registrationType))),
				verbinde(TRENNER_DETAIL, feld("beginn", beginn(turnier)), feld("status", status(turnier)),
						anmeldungen(turnier)),
				feld("ort", StringUtils.defaultString(turnier.location)));
	}

	/** Turnierdatum im lokalen Format, z. B. {@code 25.11.2026}; ein nicht lesbares Datum erscheint unverändert. */
	public static String datum(OnlineTournamentDto turnier) {
		return datum(turnier.date, Locale.getDefault(Locale.Category.FORMAT));
	}

	static String datum(String isoDatum, Locale locale) {
		if (StringUtils.isBlank(isoDatum)) {
			return "";
		}
		try {
			return LocalDate.parse(isoDatum)
					.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
		} catch (DateTimeParseException e) {
			logger.warn("PTM-Online: Turnierdatum '{}' nicht lesbar", isoDatum, e);
			return isoDatum;
		}
	}

	/**
	 * Bezeichnung des lokalen Turniersystems zum Online-Typ; Supermêlée nur zusammen mit der Supermêlée-Anmeldung. Ein
	 * unbekannter Typ erscheint unverändert.
	 */
	public static String turnierSystem(OnlineTournamentDto turnier) {
		return TurnierSystemOnlineTypMapping.turnierSystem(turnier.type, turnier.registrationType)
				.or(() -> turnierSystemNurNachTyp(turnier.type)).map(TurnierSystem::getBezeichnung)
				.orElse(StringUtils.defaultString(turnier.type));
	}

	public static String formation(OnlineTournamentDto turnier) {
		return formationBezeichnung(turnier.formationsWert());
	}

	/** Formation aus dem Vergleichswert ({@link OnlineVerbindbarkeit#vergleichswert(Formation)}). */
	public static String formationBezeichnung(String formation) {
		return Arrays.stream(Formation.values()).filter(f -> f != Formation.MELEE && f != Formation.NUR_TEAMNAME)
				.filter(f -> OnlineVerbindbarkeit.vergleichswert(f).equals(formation)).findFirst()
				.map(Formation::getBezeichnung)
				.orElseGet(() -> OnlineTournamentDto.FORMATION_ANDERE.equals(formation)
						? I18n.get("ptmonline.formation.andere")
						: StringUtils.defaultString(formation));
	}

	public static String anmeldeart(String anmeldeart) {
		return switch (StringUtils.defaultString(anmeldeart)) {
			case "forme", "melee", ANMELDEART_SUPERMELEE -> I18n.get("ptmonline.anmeldeart." + anmeldeart);
			default -> StringUtils.defaultString(anmeldeart);
		};
	}

	private static Optional<TurnierSystem> turnierSystemNurNachTyp(String onlineTyp) {
		return Arrays.stream(TurnierSystem.values())
				.filter(ts -> TurnierSystemOnlineTypMapping.onlineTyp(ts).filter(typ -> typ.equals(onlineTyp))
						.isPresent())
				.findFirst();
	}

	/** Die Supermêlée-Anmeldung steckt schon im Turniersystem und wird in der Kurzform nicht wiederholt. */
	private static String anmeldeartOhneSupermelee(OnlineTournamentDto turnier) {
		return ANMELDEART_SUPERMELEE.equals(turnier.registrationType) ? "" : anmeldeart(turnier.registrationType);
	}

	private static String beginn(OnlineTournamentDto turnier) {
		return verbinde(" ", datum(turnier), StringUtils.defaultString(turnier.startTime));
	}

	private static String status(OnlineTournamentDto turnier) {
		return turnier.status == null ? "" : PtmOnlineSyncStatus.statusAnzeige(turnier.status);
	}

	/**
	 * Anmeldungen mit Teilnehmergrenze ({@code 12 / 32}). Ein älterer Server liefert keine Anzahl; dann erscheint nur
	 * die Grenze.
	 */
	private static String anmeldungen(OnlineTournamentDto turnier) {
		String grenze = turnier.maxRegistrations > 0 ? String.valueOf(turnier.maxRegistrations) : "";
		if (turnier.registrationCount == null) {
			return feld("max_anmeldungen", grenze);
		}
		return feld("anmeldungen", verbinde(" / ", String.valueOf(turnier.registrationCount), grenze));
	}

	/** Beschriftetes Feld; leer, wenn der Wert fehlt. */
	private static String feld(String schluessel, String wert) {
		return StringUtils.isBlank(wert) ? ""
				: I18n.get("ptmonline.turnier.eckdaten." + schluessel) + ": " + wert;
	}

	private static String verbinde(String trenner, String... teile) {
		return Stream.of(teile).filter(StringUtils::isNotBlank).collect(Collectors.joining(trenner));
	}
}
