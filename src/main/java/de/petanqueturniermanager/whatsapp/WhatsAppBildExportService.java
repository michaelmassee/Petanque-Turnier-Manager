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
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellRangeAddress;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
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
		PdfExport export = PdfExport.from(ws)
				.sheetName(sheetName)
				.prefix1(sheetName)
				.zielVerzeichnis(zielVerzeichnis);
		RangePosition druckbereich = druckbereich(sheet);
		if (druckbereich != null) {
			export.range(druckbereich);
		}
		Path pdf = Path.of(export.doExport());
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

	private static RangePosition druckbereich(XSpreadsheet sheet) {
		try {
			XPrintAreas printAreas = Lo.qi(XPrintAreas.class, sheet);
			if (printAreas == null) {
				return null;
			}
			CellRangeAddress[] bereiche = printAreas.getPrintAreas();
			if (bereiche == null || bereiche.length == 0) {
				return null;
			}
			CellRangeAddress box = begrenzungsrahmen(bereiche);
			return RangePosition.from(box.StartColumn, box.StartRow, box.EndColumn, box.EndRow);
		} catch (Exception e) {
			logger.debug("WhatsApp-Druckbereich konnte nicht ermittelt werden, exportiere komplettes Blatt", e);
			return null;
		}
	}

	private static CellRangeAddress begrenzungsrahmen(CellRangeAddress[] bereiche) {
		var box = new CellRangeAddress();
		box.Sheet = bereiche[0].Sheet;
		box.StartColumn = bereiche[0].StartColumn;
		box.StartRow = bereiche[0].StartRow;
		box.EndColumn = bereiche[0].EndColumn;
		box.EndRow = bereiche[0].EndRow;
		for (int i = 1; i < bereiche.length; i++) {
			box.StartColumn = Math.min(box.StartColumn, bereiche[i].StartColumn);
			box.StartRow = Math.min(box.StartRow, bereiche[i].StartRow);
			box.EndColumn = Math.max(box.EndColumn, bereiche[i].EndColumn);
			box.EndRow = Math.max(box.EndRow, bereiche[i].EndRow);
		}
		return box;
	}

	public record ExportiertesBild(String sheetName, Path png, byte[] bytes) {
	}
}
