/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Dokumentbezogener Aufrufkontext für Code, der außerhalb eines konkreten UNO-Callbacks (z.B. auf
 * einem {@code SheetRunner}-Hintergrundthread) PTM.ALG.*-AddIn-Formeln für ein bestimmtes Dokument
 * schreibt/auswertet, während zeitgleich ein anderes Dokument den UI-Fokus hält.
 * <p>
 * {@code GlobalImpl.getDocumentPropertiesHelper()} liest dieses ThreadLocal vorrangig vor dem
 * fokus-basierten {@code DocumentHelper.getCurrentSpreadsheetDocument()}-Fallback, der durch
 * zwischenzeitliche Fokuswechsel (z.B. ein noch nicht in den Vordergrund geholtes neues Dokument)
 * auf das falsche Dokument zeigen kann.
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
