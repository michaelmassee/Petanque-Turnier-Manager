/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.webserver.SheetResolverFactory;

public class WhatsAppBildExportService {

	private static final Logger logger = LogManager.getLogger(WhatsAppBildExportService.class);

	private final WorkingSpreadsheet ws;

	public WhatsAppBildExportService(WorkingSpreadsheet ws) {
		this.ws = ws;
	}

	public ExportiertesBild exportiere(WhatsAppAktion aktion, Path zielVerzeichnis) throws GenerateException {
		XSpreadsheet sheet = SheetResolverFactory.erstellen(aktion.resolverKey()).resolve(ws)
				.orElseThrow(() -> new GenerateException("Kein passendes Blatt für WhatsApp-Aktion gefunden: "
						+ aktion.resolverKey()));
		String sheetName = sheetName(sheet);
		Path pdf = Path.of(PdfExport.from(ws)
				.sheetName(sheetName)
				.prefix1(sheetName)
				.zielVerzeichnis(zielVerzeichnis)
				.doExport());
		Path png = zielVerzeichnis.resolve(sheetName + ".png");
		try (var document = PDDocument.load(pdf.toFile())) {
			var renderer = new PDFRenderer(document);
			var bild = renderer.renderImageWithDPI(0, 150);
			ImageIO.write(bild, "png", png.toFile());
			return new ExportiertesBild(sheetName, png, Files.readAllBytes(png));
		} catch (IOException e) {
			logger.error("WhatsApp-Blatt '{}' konnte nicht zu PNG gerastert werden", sheetName, e);
			throw new GenerateException(e.getMessage());
		}
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
