package de.petanqueturniermanager.algorithmen;

import java.util.stream.Stream;

import org.apache.commons.text.CaseUtils;

/**
 * Erstellung 23.06.2022 / Michael Massee
 */

public enum DirektvergleichResult {
	VERLOREN(1), GEWONNEN(2), GLEICH(0), KEINERGEBNIS(3), FEHLER(-1);

	DirektvergleichResult(int i) {
		code = i;
	}

	private final int code;

	public int getCode() {
		return code;
	}

	public String getAnzeigeText() {
		return CaseUtils.toCamelCase(name(), true, null);
	}

	public static DirektvergleichResult getByCode(int code) {
		for (DirektvergleichResult dvglrslt : values()) {
			if (dvglrslt.getCode() == code) {
				return dvglrslt;
			}
		}
		return null;
	}

	public static Stream<DirektvergleichResult> stream() {
		return Stream.of(DirektvergleichResult.values());
	}

}
