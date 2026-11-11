/*
 * Erstellung 26.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.numberformat;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/**
 * Zahlenformate für Zellen. Datums-/Zeitformate werden über ein festes Muster definiert, Zahlenformate über die
 * Anzahl der Nachkommastellen – deren Muster erzeugt {@link NumberFormatHelper} passend zur Gebietsschema-Einstellung
 * (Dezimaltrennzeichen).
 *
 * @author Michael Massee
 */
public enum UserNumberFormat {
	WOCHEN_TAG("TTT"),
	DATE("TT.MM.JJJJ"),
	DATE_SHORT("TT.MM.JJ"),
	TIME("HH:MM"),
	/** Zahl mit genau einer Nachkommastelle, z. B. 1,5 bzw. 1.5 */
	ZAHL_EINE_NACHKOMMASTELLE(1);

	private final String pattern;
	private final int nachkommastellen;

	UserNumberFormat(String pattern) {
		this.pattern = checkNotNull(pattern);
		this.nachkommastellen = -1;
	}

	UserNumberFormat(int nachkommastellen) {
		checkArgument(nachkommastellen >= 0);
		this.pattern = null;
		this.nachkommastellen = nachkommastellen;
	}

	/**
	 * @return festes Formatmuster; leer bei Zahlenformaten, deren Muster gebietsschemaabhängig erzeugt wird
	 */
	public final Optional<String> getPattern() {
		return Optional.ofNullable(pattern);
	}

	/**
	 * @return Anzahl Nachkommastellen eines Zahlenformats, -1 bei Formaten mit festem Muster
	 */
	public final int getNachkommastellen() {
		return nachkommastellen;
	}
}
