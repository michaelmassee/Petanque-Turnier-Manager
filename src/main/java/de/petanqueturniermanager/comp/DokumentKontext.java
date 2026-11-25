/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.concurrent.Callable;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Dokumentbezogener Aufrufkontext für Code, der außerhalb eines konkreten UNO-Callbacks (z.B. auf
 * einem {@code SheetRunner}-Hintergrundthread) für ein bestimmtes Dokument arbeitet, während zeitgleich
 * ein anderes Dokument den UI-Fokus hält. Leser wie {@code TurnierModus} nehmen dieses ThreadLocal
 * vorrangig vor einem Fallback ohne Dokumentbezug; ein fokus-basierter Zugriff würde bei
 * zwischenzeitlichen Fokuswechseln auf das falsche Dokument zeigen.
 * <p>
 * Die PTM.ALG.*-AddIn-Funktionen brauchen den Kontext nicht: Calc übergibt ihnen das Dokument der
 * Formelzelle als Caller-Parameter (siehe {@code GlobalImpl}).
 */
public final class DokumentKontext {

	private static final ThreadLocal<XSpreadsheetDocument> AKTUELLES_DOKUMENT = new ThreadLocal<>();

	private DokumentKontext() {
	}

	public static XSpreadsheetDocument get() {
		return AKTUELLES_DOKUMENT.get();
	}

	public static void mitKontext(XSpreadsheetDocument doc, Runnable aktion) {
		XSpreadsheetDocument vorher = push(doc);
		try {
			aktion.run();
		} finally {
			restore(vorher);
		}
	}

	public static void mitKontextWerfend(XSpreadsheetDocument doc, WerfendeAktion aktion) throws GenerateException {
		XSpreadsheetDocument vorher = push(doc);
		try {
			aktion.ausfuehren();
		} finally {
			restore(vorher);
		}
	}

	/**
	 * Wie {@link #mitKontext}, für Aktionen mit Rückgabewert, die beliebige Exceptions werfen dürfen (z.B. die
	 * Dialog-Befehle im {@code ProtocolHandler}, die ohne {@code SheetRunner} laufen).
	 */
	public static <T> T mitKontextAufruf(XSpreadsheetDocument doc, Callable<T> aktion) throws Exception {
		XSpreadsheetDocument vorher = push(doc);
		try {
			return aktion.call();
		} finally {
			restore(vorher);
		}
	}

	private static XSpreadsheetDocument push(XSpreadsheetDocument doc) {
		XSpreadsheetDocument vorher = AKTUELLES_DOKUMENT.get();
		if (doc != null) {
			AKTUELLES_DOKUMENT.set(doc);
		} else {
			AKTUELLES_DOKUMENT.remove();
		}
		return vorher;
	}

	private static void restore(XSpreadsheetDocument vorher) {
		if (vorher != null) {
			AKTUELLES_DOKUMENT.set(vorher);
		} else {
			AKTUELLES_DOKUMENT.remove();
		}
	}

	/** Aktion, die {@link GenerateException} werfen darf (z.B. {@code SheetRunner.doRun()}). */
	@FunctionalInterface
	public interface WerfendeAktion {
		void ausfuehren() throws GenerateException;
	}
}
