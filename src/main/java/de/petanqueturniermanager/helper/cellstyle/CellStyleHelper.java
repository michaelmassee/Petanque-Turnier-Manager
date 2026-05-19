/*
 * Erstellung : 21.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;

public class CellStyleHelper {

	private static final Logger logger = LogManager.getLogger(CellStyleHelper.class);

	private final AbstractCellStyleDef cellStyleDef;
	/** Gesetzt wenn über {@link #from(ISheet, AbstractCellStyleDef)} erzeugt. */
	private final ISheet sheet;
	/** Gesetzt wenn über {@link #from(XSpreadsheetDocument, AbstractCellStyleDef)} erzeugt. */
	private final XSpreadsheetDocument spreadsheetDocument;

	private CellStyleHelper(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		this.sheet = sheet;
		this.spreadsheetDocument = null;
		this.cellStyleDef = cellStyleDef;
	}

	private CellStyleHelper(XSpreadsheetDocument spreadsheetDocument, AbstractCellStyleDef cellStyleDef) {
		this.sheet = null;
		this.spreadsheetDocument = spreadsheetDocument;
		this.cellStyleDef = cellStyleDef;
	}

	public static CellStyleHelper from(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		checkNotNull(sheet);
		checkNotNull(cellStyleDef);
		return new CellStyleHelper(sheet, cellStyleDef);
	}

	/**
	 * Erstellt einen CellStyleHelper direkt über das Dokument – für Kontexte ohne {@link ISheet}
	 * (z. B. {@code BlattschutzManager}).
	 */
	public static CellStyleHelper from(XSpreadsheetDocument doc, AbstractCellStyleDef cellStyleDef) {
		checkNotNull(doc);
		checkNotNull(cellStyleDef);
		return new CellStyleHelper(doc, cellStyleDef);
	}

	public CellStyleHelper apply() {
		checkNotNull(cellStyleDef);

		var currentSpreadsheetDocument = holeSpreadsheetDocument();
		checkNotNull(currentSpreadsheetDocument);
		applyAufDokument(currentSpreadsheetDocument);
		return this;
	}

	// -------------------------------------------------------------------------

	private XSpreadsheetDocument holeSpreadsheetDocument() {
		if (spreadsheetDocument != null) {
			return spreadsheetDocument;
		}
		checkNotNull(sheet);
		return sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
	}

	private void applyAufDokument(XSpreadsheetDocument currentSpreadsheetDocument) {
		var styleName = cellStyleDef.getName();

		// LO-Einschränkung (sc/source/ui/unoobj/styleuno.cxx): Zellstile können nicht
		// geändert werden, solange irgendein Sheet im Doc tab-geschützt ist.
		//
		// Zwei Aufruf-Kontexte:
		//   1. Innerhalb eines aktiven SheetRunner-Lazy-Scope (Standardfall via
		//      ConditionalFormatHelper oder zelleStylesAktualisieren in
		//      endCommandScope): tryEnsureUnprotectedInScope() entsperrt die
		//      konfigurierten Sheets einmalig im Scope – kein zusätzlicher
		//      Protect/Unprotect-Roundtrip pro Style-Call.
		//   2. Ohne Scope (z. B. TurnierModus.aktivieren -> BlattschutzManager.schuetzen):
		//      Defensiver Sweep über alle Sheets mit leerem Passwort, Style
		//      schreiben, danach Schutz wiederherstellen.
		// Passwortgeschützte Sheets (durch User) bleiben in beiden Fällen gesperrt –
		// dort logged der Catch-Zweig wie zuvor.

		boolean scopeHandlesProtection = BlattschutzManager.get().tryEnsureUnprotectedInScope();
		List<XProtectable> temporaerEntsperrt = scopeHandlesProtection
				? List.of()
				: entsperreAlleSheetsMitLeeremPasswort(currentSpreadsheetDocument);
		try {
			var xFamiliesSupplier = Lo.qi(XStyleFamiliesSupplier.class, currentSpreadsheetDocument);
			var xFamiliesNA = xFamiliesSupplier.getStyleFamilies();
			var aCellStylesObj = xFamiliesNA.getByName("CellStyles");
			var xCellStylesNA = Lo.qi(XNameContainer.class, aCellStylesObj);

			Object aCellStyle;
			try {
				aCellStyle = xCellStylesNA.getByName(styleName);
			} catch (NoSuchElementException e) {
				// create a new cell style
				var xDocServiceManager = Lo.qi(XMultiServiceFactory.class, currentSpreadsheetDocument);
				aCellStyle = xDocServiceManager.createInstance("com.sun.star.style.CellStyle");
				xCellStylesNA.insertByName(styleName, aCellStyle);
			}

			// modify properties of the (new) style
			var xPropSet = Lo.qi(XPropertySet.class, aCellStyle);
			for (var propKey : cellStyleDef.getCellProperties().keySet()) {
				if (xPropSet.getPropertySetInfo().hasPropertyByName(propKey)) {
					var neuerWert = cellStyleDef.getCellProperties().get(propKey);
					var aktuellerWert = xPropSet.getPropertyValue(propKey);
					if (!Objects.equals(neuerWert, aktuellerWert)) {
						xPropSet.setPropertyValue(propKey, neuerWert);
					}
				}
			}
		} catch (RuntimeException e) {
			getLogger().warn(
					"Zellstil '{}' konnte nicht gesetzt werden – evtl. LO-Einschränkung: " +
					"Zellstile können nicht geändert werden, solange ein Sheet im Dokument " +
					"tab-geschützt ist. (sc/source/ui/unoobj/styleuno.cxx)",
					styleName, e);
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
		} finally {
			schuetzeWiederMitLeeremPasswort(temporaerEntsperrt);
		}
	}

	/**
	 * Entsperrt alle aktuell mit leerem Passwort geschützten Sheets im Dokument
	 * und gibt sie zur Wiederherstellung des Schutzes zurück.
	 * Sheets mit echtem Passwort bleiben unverändert gesperrt.
	 * <p>
	 * Wird nur im Pfad <em>ohne</em> aktiven BlattschutzScope verwendet
	 * (z. B. {@code TurnierModus.aktivieren()}). Innerhalb eines Scope übernimmt
	 * {@link de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager#tryEnsureUnprotectedInScope()}
	 * diese Aufgabe einmalig.
	 */
	private List<XProtectable> entsperreAlleSheetsMitLeeremPasswort(XSpreadsheetDocument doc) {
		List<XProtectable> entsperrt = new ArrayList<>();
		var sheets = doc.getSheets();
		for (var name : sheets.getElementNames()) {
			try {
				var xSheet = Lo.qi(XSpreadsheet.class, sheets.getByName(name));
				var xProt = Lo.qi(XProtectable.class, xSheet);
				if (xProt != null && xProt.isProtected()) {
					try {
						xProt.unprotect("");
						// Verifizieren: bei Passwortschutz schlägt unprotect("") still fehl
						if (!xProt.isProtected()) {
							entsperrt.add(xProt);
						}
					} catch (RuntimeException e) {
						// Sheet mit Passwort – überspringen, bleibt geschützt
					}
				}
			} catch (NoSuchElementException | WrappedTargetException e) {
				// Sheet nicht zugreifbar – überspringen
			}
		}
		return entsperrt;
	}

	private void schuetzeWiederMitLeeremPasswort(List<XProtectable> sheets) {
		for (var xProt : sheets) {
			try {
				xProt.protect("");
			} catch (RuntimeException e) {
				getLogger().warn("Sheet-Schutz konnte nicht wiederhergestellt werden: {}", e.getMessage());
			}
		}
	}

	private Logger getLogger() {
		if (sheet != null) {
			return sheet.getLogger();
		}
		return logger;
	}

}
