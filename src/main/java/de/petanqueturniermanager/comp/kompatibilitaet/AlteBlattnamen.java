/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheets;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;

/**
 * Benennt Blätter um, deren übersetzter Name sich geändert hat. Formeln werden mit dem aktuellen Namen aus der
 * Übersetzung aufgebaut ({@link SheetNamen}); ein Dokument mit dem alten Reiternamen würde sonst neue Formeln ins
 * Leere zeigen lassen. Beim Umbenennen passt Calc alle vorhandenen Bezüge selbst an.
 * <p>
 * Unabhängig von der Dokumentversion: Der Name hängt an der Sprache, nicht an der Plugin-Version. Läuft vor jedem
 * Lauf, der das Dokument ändert, und ist idempotent.
 */
final class AlteBlattnamen {

	private static final Logger logger = LogManager.getLogger(AlteBlattnamen.class);

	/** Ein Blatt mit seinem aktuellen Namen und den früher verwendeten Übersetzungen. */
	record Umbenennung(Supplier<String> aktuellerName, List<String> alteNamen) {

		Umbenennung {
			alteNamen = List.copyOf(alteNamen);
		}
	}

	/**
	 * Meldeliste (2026-11-23): NL „Deelnemerslijst“ meinte die Teilnehmerliste, FR „Liste des inscriptions“ wurde an
	 * PTM-Online („Liste des inscrits“) angeglichen.
	 */
	static final List<Umbenennung> UMBENENNUNGEN = List.of(
			new Umbenennung(SheetNamen::meldeliste, List.of("Deelnemerslijst", "Liste des inscriptions")));

	private AlteBlattnamen() {
	}

	/** Benennt jedes Blatt mit einem alten Namen um, solange es noch kein Blatt mit dem aktuellen Namen gibt. */
	static void heile(WorkingSpreadsheet ws) {
		XSpreadsheets sheets = ws.getWorkingSpreadsheetDocument().getSheets();
		for (Umbenennung umbenennung : UMBENENNUNGEN) {
			String aktuell = umbenennung.aktuellerName().get();
			zuUmbenennen(aktuell, umbenennung.alteNamen(), sheets::hasByName)
					.ifPresent(alterName -> benenneUm(sheets, alterName, aktuell));
		}
	}

	/**
	 * UNO-frei.
	 *
	 * @return der vorhandene alte Name, wenn das Blatt umbenannt werden soll
	 */
	static Optional<String> zuUmbenennen(String aktuellerName, List<String> alteNamen, Predicate<String> vorhanden) {
		if (vorhanden.test(aktuellerName)) {
			return Optional.empty();
		}
		return alteNamen.stream().filter(alterName -> !alterName.equals(aktuellerName)).filter(vorhanden)
				.findFirst();
	}

	private static void benenneUm(XSpreadsheets sheets, String alterName, String neuerName) {
		try {
			XNamed blatt = Lo.qi(XNamed.class, sheets.getByName(alterName));
			blatt.setName(neuerName);
			logger.info("Blatt '{}' in '{}' umbenannt (geänderte Übersetzung)", alterName, neuerName);
		} catch (NoSuchElementException | WrappedTargetException | RuntimeException e) {
			// Etwa bei geschützter Dokumentstruktur: Das Dokument bleibt nutzbar, nur neue Formeln zeigen ins Leere.
			logger.warn("Blatt '{}' konnte nicht in '{}' umbenannt werden", alterName, neuerName, e);
		}
	}
}
