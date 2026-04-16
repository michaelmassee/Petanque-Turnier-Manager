/**
 * Erstellung : 21.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;

public class CellStyleHelper {

	/** Log-Drosselung: einmalige Warnung pro Style-Name (verhindert Log-Spam in Schleifen). */
	private static final Set<String> MISSING_STYLE_WARNED = Collections.synchronizedSet(new HashSet<>());

	private final AbstractCellStyleDef cellStyleDef;
	private final ISheet sheet;

	private CellStyleHelper(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		this.sheet = sheet;
		this.cellStyleDef = cellStyleDef;
	}

	public static CellStyleHelper from(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		checkNotNull(sheet);
		checkNotNull(cellStyleDef);
		return new CellStyleHelper(sheet, cellStyleDef);
	}

	/**
	 * Erstellt den Zellstil und setzt seine Properties (create or update).
	 * Nur in garantiert ungeschütztem Dokumentzustand aufrufen –
	 * LibreOffice verbietet Style-Modifikationen wenn irgendein Sheet tab-geschützt ist.
	 *
	 * @return this
	 */
	public CellStyleHelper apply() {
		var styleName = cellStyleDef.getName();

		try {
			var currentSpreadsheetDocument = sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();

			var xFamiliesSupplier = Lo.qi(XStyleFamiliesSupplier.class, currentSpreadsheetDocument);
			var xFamiliesNA = xFamiliesSupplier.getStyleFamilies();
			Object aCellStylesObj = xFamiliesNA.getByName("CellStyles");
			var xCellStylesNA = Lo.qi(XNameContainer.class, aCellStylesObj);

			Object aCellStyle;
			try {
				aCellStyle = xCellStylesNA.getByName(styleName);
			} catch (NoSuchElementException e) {
				// Stil noch nicht vorhanden → neu anlegen
				var xDocServiceManager = Lo.qi(XMultiServiceFactory.class, currentSpreadsheetDocument);
				aCellStyle = xDocServiceManager.createInstance("com.sun.star.style.CellStyle");
				xCellStylesNA.insertByName(styleName, aCellStyle);
			}

			// Properties setzen (create or update)
			var xPropSet = Lo.qi(XPropertySet.class, aCellStyle);
			for (var entry : cellStyleDef.getCellProperties().entrySet()) {
				xPropSet.setPropertyValue(entry.getKey(), entry.getValue());
			}
		} catch (Exception e) {
			sheet.getLogger().error(e.getMessage(), e);
		}
		return this;
	}

	/**
	 * Erstellt den Zellstil falls er noch nicht existiert.
	 * Existiert er bereits, werden seine Properties NICHT verändert –
	 * kein {@code setPropertyValue} → kein RuntimeException bei geschützten Sheets.
	 * <p>
	 * Normalfall (nach {@code alleStylesInitialisieren()}):
	 * {@code getByName()} trifft sofort → return, keine Sheet-Schutz-Prüfung.
	 * Die Sheet-Schutz-Prüfung läuft nur im Ausnahmefall (Style fehlt).
	 * <p>
	 * Style fehlt + Sheet geschützt → einmalige Warnung im Log, kein Absturz.
	 *
	 * @return this
	 */
	public CellStyleHelper ensureCreated() {
		var styleName = cellStyleDef.getName();
		try {
			var currentDoc = sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
			var xCellStylesNA = Lo.qi(XNameContainer.class,
					Lo.qi(XStyleFamiliesSupplier.class, currentDoc).getStyleFamilies().getByName("CellStyles"));

			try {
				xCellStylesNA.getByName(styleName);
				return this; // ← Normalfall: Style existiert, sofortiger Return
			} catch (NoSuchElementException e) {
				// Style fehlt → Ausnahmefall (altes Dokument ohne Initialisierung)
			}

			// Sheet-Schutz nur im Ausnahmefall prüfen
			if (irgendeinSheetGeschuetzt(currentDoc)) {
				if (MISSING_STYLE_WARNED.add(styleName)) {
					sheet.getLogger().warn(
							"Style '{}' fehlt, kann nicht erstellt werden (Sheet-Schutz aktiv). "
									+ "Neu aufbauen: Blattschutz deaktivieren → Neue Meldeliste erstellen.",
							styleName);
				}
				return this;
			}

			// Style neu erstellen (Schutz nicht aktiv)
			return apply();
		} catch (Exception e) {
			sheet.getLogger().error(e.getMessage(), e);
		}
		return this;
	}

	private boolean irgendeinSheetGeschuetzt(XSpreadsheetDocument doc) {
		try {
			var sheets = doc.getSheets();
			for (var name : sheets.getElementNames()) {
				var protectable = Lo.qi(XProtectable.class, Lo.qi(XSpreadsheet.class, sheets.getByName(name)));
				if (protectable != null && protectable.isProtected()) {
					return true;
				}
			}
		} catch (Exception e) {
			sheet.getLogger().warn("Sheet-Schutz-Prüfung fehlgeschlagen: {}", e.getMessage());
		}
		return false;
	}

}
