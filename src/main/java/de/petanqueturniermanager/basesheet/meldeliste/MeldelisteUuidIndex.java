/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Zuordnung UUID ↔ aktuelle Teamnummer der Meldeliste, aus der Nr-Spalte ({@code PTM.ALG.TNRUUID}) in je einem
 * Lesezugriff für Formeln und Werte. Spielrunden speichern die UUID; wer mit Nummern rechnet (Algorithmen,
 * Ranglisten), bildet sie hierüber auf die Nummer zum Zeitpunkt des Laufs ab – und zurück beim Schreiben.
 */
public final class MeldelisteUuidIndex {

	/** Nr-Spalte A bis Zeile 999, wie {@link MeldeListeHelper#nrBereichMeldeliste()}. */
	private static final RangePosition NR_SPALTE = RangePosition.from(0, 0, 0, 998);

	private final Map<String, Integer> nrProUuid;
	private final Map<Integer, String> uuidProNr;

	private MeldelisteUuidIndex(Map<String, Integer> nrProUuid, Map<Integer, String> uuidProNr) {
		this.nrProUuid = nrProUuid;
		this.uuidProNr = uuidProNr;
	}

	/**
	 * Formeln und angezeigte Nummern der Nr-Spalte ab Zeile 1, Index = 0-basierte Sheet-Zeile.
	 *
	 * @param nummern angezeigte Nummer je Zeile, 0 für leer oder Text
	 */
	public record NrSpalte(String[][] formeln, int[] nummern) {
	}

	/** Liest die Meldeliste des Dokuments; ohne Meldeliste ist der Index leer. */
	public static MeldelisteUuidIndex lese(WorkingSpreadsheet ws) {
		SheetHelper helper = new SheetHelper(ws);
		XSpreadsheet meldeliste = helper.findByName(SheetNamen.meldeliste());
		if (meldeliste == null) {
			return aus(new String[0][0], new int[0]);
		}
		NrSpalte spalte = leseNrSpalte(helper, meldeliste, ws.getWorkingSpreadsheetDocument());
		return aus(spalte.formeln(), spalte.nummern());
	}

	/** Liest die Nr-Spalte (A1:A999) der Meldeliste, Formeln und Werte je in einem Zugriff. */
	public static NrSpalte leseNrSpalte(SheetHelper helper, XSpreadsheet meldeliste, XSpreadsheetDocument doc) {
		String[][] formeln = helper.getFormulaArrayFromRange(meldeliste, NR_SPALTE);
		RangeData werte = RangeHelper.from(meldeliste, doc, NR_SPALTE).getDataFromRange();
		int[] nummern = new int[werte.size()];
		for (int i = 0; i < nummern.length; i++) {
			nummern[i] = werte.get(i).isEmpty() ? 0 : werte.get(i).get(0).getIntVal(0);
		}
		return new NrSpalte(formeln, nummern);
	}

	/**
	 * UNO-frei: Formeln und angezeigte Nummern derselben Zeilen. Zeilen ohne UUID oder ohne Nr (> 0) zählen nicht.
	 */
	public static MeldelisteUuidIndex aus(String[][] formeln, int[] nummern) {
		Map<String, Integer> nrProUuid = new HashMap<>();
		Map<Integer, String> uuidProNr = new HashMap<>();
		for (int i = 0; i < Math.min(formeln.length, nummern.length); i++) {
			int nr = nummern[i];
			Optional<String> uuid = formeln[i].length > 0 ? TeamUuidFormel.findeUuid(formeln[i][0]) : Optional.empty();
			if (nr > 0 && uuid.isPresent()) {
				nrProUuid.putIfAbsent(uuid.get(), nr);
				uuidProNr.putIfAbsent(nr, uuid.get());
			}
		}
		return new MeldelisteUuidIndex(nrProUuid, uuidProNr);
	}

	/** @return aktuelle Teamnummer zur UUID; leer, wenn die Meldung nicht (mehr) in der Meldeliste steht */
	public OptionalInt nr(String uuid) {
		Integer nr = nrProUuid.get(uuid);
		return nr == null ? OptionalInt.empty() : OptionalInt.of(nr);
	}

	/** @return UUID der Meldung mit dieser Teamnummer */
	public Optional<String> uuid(int nr) {
		return Optional.ofNullable(uuidProNr.get(nr));
	}

	/**
	 * Schlüssel für Formeln, die das Team mit dieser Nummer referenzieren: die UUID, sonst die Nummer selbst. Die
	 * Meldelisten-Aktualisierung vor jedem Rundenaufbau vergibt UUIDs; fehlt sie trotzdem, ist die Zelle nur gegen
	 * Umnummerieren nicht geschützt.
	 */
	public TeamSchluessel schluessel(int nr) {
		return uuid(nr).map(TeamSchluessel::uuidLiteral).orElseGet(() -> TeamSchluessel.nr(String.valueOf(nr)));
	}
	/** Schlüssel für echte Teams in UUID-Spielplänen; keine Nummernsuche bei fehlender UUID. */
	public TeamSchluessel uuidSchluessel(int nr) throws GenerateException {
		return uuid(nr).map(TeamSchluessel::uuidLiteral).orElseThrow(() ->
				new GenerateException(
						I18n.get("spielplan.team.uuid_fehlt", String.valueOf(nr))));
	}

}
