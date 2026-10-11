/*
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.konfigdialog.SpielrundeFooterConfigProperty;
import de.petanqueturniermanager.konfigdialog.ZeitplanConfigProperty;

/**
 * @author Michael Massee
 */
public class TurnierDialog extends BasePropertiesDialog {

	static final Logger logger = LogManager.getLogger(TurnierDialog.class);
	private static final int DIALOG_WIDTH = 300;
	private static final Map<String, Integer> SORTIER_RANG = Map.ofEntries(
			Map.entry("Editierbare Felder hervorheben", 0),
			Map.entry("Melee Anmeldung", 10),
			Map.entry("Teilnehmerliste Anzahl je Spalte", 20),
			Map.entry("Meldeliste Sortierung", 21),
			Map.entry("Checkin-Liste Sortierung", 22),
			Map.entry("Teilnehmerliste Sortierung", 23),
			Map.entry("Spielrunde", 30),
			Map.entry("Spielrunde Spielbahn", 31),
			Map.entry("Spielplan Team Anzeige", 32),
			Map.entry("Schweizer Ranking Modus", 40),
			Map.entry("Rangliste Team Anzeige", 41),
			Map.entry("Freispiel Punkte +", 50),
			Map.entry("Freispiel Punkte -", 51));

	public TurnierDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		// alles außer Color, Kopf/Fußzeilen, Felder mit eigenem Dialog und internen Zustandsfeldern
		return konfigprop -> konfigprop.getType() != ConfigPropertyType.COLOR
				&& !(konfigprop instanceof HeaderFooterConfigProperty)
				&& !(konfigprop instanceof SpielrundeFooterConfigProperty)
				&& !(konfigprop instanceof ZeitplanConfigProperty)
				&& !konfigprop.isIntern()
				&& !konfigprop.isExportKonfig();
	}

	@Override
	protected int getBreite() {
		return DIALOG_WIDTH;
	}

	@Override
	protected Comparator<ConfigProperty<?>> getKonfigFieldComparator() {
		return turnierKonfigComparator();
	}

	/**
	 * Sortiert bekannte Turnier-Optionen in ihrem fachlichen Ablauf. Nicht zugeordnete,
	 * turniersystemspezifische Optionen behalten durch die stabile Stream-Sortierung ihre
	 * definierte Reihenfolge aus der jeweiligen Properties-Spalte.
	 */
	static Comparator<ConfigProperty<?>> turnierKonfigComparator() {
		return Comparator.comparingInt(configProperty -> sortierRang(configProperty.getKey()));
	}

	private static int sortierRang(String key) {
		Integer festerRang = SORTIER_RANG.get(key);
		if (festerRang != null) {
			return festerRang;
		}

		String normalisiert = key.toLowerCase(Locale.ROOT);
		if (enthaeltEines(normalisiert, "meldung", "teilnehmer", "checkin", "melee", "formation", "verein")) {
			return 10;
		}
		if (enthaeltEines(normalisiert, "spielrunde", "spielplan", "spieltag", "spielbaum", "kaskade", "poule",
				"bahn", "paarung", "spielziel", "runde")) {
			return 30;
		}
		if (enthaeltEines(normalisiert, "rangliste", "ranking", "direktvergleich", "buchholz")) {
			return 40;
		}
		if (enthaeltEines(normalisiert, "freispiel", "punkte", "wertung")) {
			return 50;
		}
		return 100;
	}

	private static boolean enthaeltEines(String wert, String... suchbegriffe) {
		for (String suchbegriff : suchbegriffe) {
			if (wert.contains(suchbegriff)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected String getTitle() {
		return I18n.get("dialog.title.turnier.konfiguration");
	}

}
