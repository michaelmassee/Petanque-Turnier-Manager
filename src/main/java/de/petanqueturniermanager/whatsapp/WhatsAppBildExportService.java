/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.upload.HtmlZuBildKonvertierer;
import de.petanqueturniermanager.webserver.SheetResolverFactory;
import de.petanqueturniermanager.webserver.TabelleHtmlRenderer;
import de.petanqueturniermanager.webserver.TabelleModel;
import de.petanqueturniermanager.webserver.TabellenMapper;

/**
 * Rendert ein Sheet (nur dessen Druckbereich) als PNG für den WhatsApp-Post.
 * <p>
 * Nutzt denselben Druckbereich-/Rendering-Weg wie der Webserver ({@link TabellenMapper} +
 * {@link TabelleHtmlRenderer}) statt eines nativen Sheet→PDF-Exports: {@link TabellenMapper#map}
 * ermittelt den Druckbereich robust (mit Fallback auf die benutzte Fläche, falls kein expliziter
 * Druckbereich gesetzt ist) und liefert ein zellbasiertes {@link TabelleModel}, das zu einem
 * HTML-{@code <table>}-Fragment gerendert und via {@link HtmlZuBildKonvertierer} direkt (ohne
 * PDF-Zwischenschritt und ohne eigene Breiten-/Höhenberechnung – das Bild wird 1:1 auf den
 * tatsächlichen Inhalt zugeschnitten) zu PNG gerastert wird.
 */
public class WhatsAppBildExportService {

	private static final Logger logger = LogManager.getLogger(WhatsAppBildExportService.class);
	private static final TabellenMapper TABELLEN_MAPPER = new TabellenMapper();
	private static final TabelleHtmlRenderer TABELLE_HTML_RENDERER = TabelleHtmlRenderer.fuerPdf();

	private final WorkingSpreadsheet ws;

	public WhatsAppBildExportService(WorkingSpreadsheet ws) {
		this.ws = ws;
	}

	public ExportiertesBild exportiere(WhatsAppAktion aktion, TurnierSystem ts, Path zielVerzeichnis)
			throws GenerateException {
		if (aktion == WhatsAppAktion.RANGLISTE) {
			WhatsAppRanglisteAktualisierer.aktualisiereWennNoetig(ws, ts);
		}
		XSpreadsheet sheet = SheetResolverFactory.erstellen(aktion.resolverKey()).resolve(ws)
				.orElseThrow(() -> new GenerateException(I18n.get("whatsapp.post.fehler.kein.blatt",
						aktion.titel(ts))));
		String sheetName = sheetName(sheet);
		TabelleModel model = TABELLEN_MAPPER.map(sheet, ws.getWorkingSpreadsheetDocument());
		if (model.getZeilen() == 0 || model.getSpalten() == 0) {
			throw new GenerateException(I18n.get("whatsapp.post.fehler.kein.blatt", aktion.titel(ts)));
		}
		String tabelleFragment = TABELLE_HTML_RENDERER.render(model);
		byte[] png = HtmlZuBildKonvertierer.konvertiere(tabelleFragment, zielVerzeichnis.toUri().toString());
		Path pngDatei = zielVerzeichnis.resolve(sheetName + ".png");
		try {
			Files.write(pngDatei, png);
		} catch (IOException e) {
			logger.error("WhatsApp-Bild für '{}' konnte nicht geschrieben werden", sheetName, e);
			throw new GenerateException(e.getMessage());
		}
		return new ExportiertesBild(sheetName, pngDatei, png);
	}

	private static String sheetName(XSpreadsheet sheet) throws GenerateException {
		XNamed named = Lo.qi(XNamed.class, sheet);
		if (named == null || named.getName().isBlank()) {
			throw new GenerateException("Blattname konnte nicht ermittelt werden");
		}
		return named.getName();
	}

	public record ExportiertesBild(String sheetName, Path png, byte[] bytes) {
	}
}
