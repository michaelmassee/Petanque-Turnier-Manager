/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.pagestyle;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Platzhalter-Tokens für Kopf-/Fußzeilen-Konfigurationstexte (z.B. {@code "Seite {SEITE} von {SEITEN}"}).
 *
 * <p>{@link #getUnoServiceName()} wird von {@link PageStyleDef} verwendet, um beim Schreiben in
 * den nativen Calc-PageStyle das Token durch ein echtes UNO-Textfeld ({@code XTextField}) zu
 * ersetzen (analog zu den Buttons im nativen Dialog "Seitenvorlage bearbeiten &gt; Kopf-/Fußzeile").
 */
public enum HeaderFooterPlatzhalter {

	DATUM("{DATUM}", "com.sun.star.text.TextField.Date", "platzhalter.datum"),
	UHRZEIT("{UHRZEIT}", "com.sun.star.text.TextField.Time", "platzhalter.uhrzeit"),
	SEITE("{SEITE}", "com.sun.star.text.TextField.PageNumber", "platzhalter.seite"),
	SEITEN("{SEITEN}", "com.sun.star.text.TextField.PageCount", "platzhalter.seiten"),
	TABELLE("{TABELLE}", "com.sun.star.text.TextField.SheetName", "platzhalter.tabelle"),
	DATEINAME("{DATEINAME}", "com.sun.star.text.TextField.FileName", "platzhalter.dateiname");

	private static final Map<String, HeaderFooterPlatzhalter> NACH_TOKEN = Arrays.stream(values())
			.collect(Collectors.toMap(HeaderFooterPlatzhalter::getToken, Function.identity()));

	private final String token;
	private final String unoServiceName;
	private final String buttonLabelKey;

	HeaderFooterPlatzhalter(String token, String unoServiceName, String buttonLabelKey) {
		this.token = token;
		this.unoServiceName = unoServiceName;
		this.buttonLabelKey = buttonLabelKey;
	}

	public String getToken() {
		return token;
	}

	public String getUnoServiceName() {
		return unoServiceName;
	}

	public String getButtonLabelKey() {
		return buttonLabelKey;
	}

	public static HeaderFooterPlatzhalter vonToken(String token) {
		return NACH_TOKEN.get(token);
	}

	public static boolean enthaeltPlatzhalter(String text) {
		if (text == null) {
			return false;
		}
		return NACH_TOKEN.keySet().stream().anyMatch(text::contains);
	}
}
