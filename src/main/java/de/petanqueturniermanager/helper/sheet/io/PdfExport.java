package de.petanqueturniermanager.helper.sheet.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.io.IOException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.PropertyValueHelper;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

/*
 * Erstellung 01.07.2022 / Michael Massee
 */

/**
 * // args2(2).Name = "SelectionOnly"</br>
 * // ' Only the selected printing Area will we exported.</br>
 * // args2(2).Value = TRUE</br>
 * // aMediaDescriptor(1).Name = "PageRange"</br>
 * // aMediaDescriptor(1).Value = ("1,3") </br>
 * </br>
 * // oBereiche=oDoc.CurrentSelection</br>
 * // dim aFilterData(0) as new com.sun.star.beans.PropertyValue</br>
 * // aFilterData(0).Name = "Selection"</br>
 * // aFilterData(0).Value = obereiche</br>
 * // arg(2).Name = "FilterData"</br>
 * // arg(2).Value = aFilterData()</br>
 * </br>
 * </br>
 * // FilterProps[0] = new PropertyValue();</br>
 * // FilterProps[0].Name = "PageRange";</br>
 * // FilterProps[0].Value = "1-" + pageCount;</br>
 * //</br>
 * // storeProps[1] = new PropertyValue();</br>
 * // storeProps[1].Name = "FilterData";</br>
 * // storeProps[1].Value = FilterProps;</br>
 * </br>
 * // https://wiki.documentfoundation.org/Macros/Python_Guide/PDF_export_filter_data</br>
 *
 */

public class PdfExport extends AbstractStore<PdfExport> {

	private static final String CALC_PDF_EXPORT_FILTER = "calc_pdf_Export";

	private static final Logger logger = LogManager.getLogger(PdfExport.class);

	private String sheetName = null;
	private RangePosition rangePosition = null;
	private Path zielVerzeichnis = null;

	private PdfExport(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	public static PdfExport from(WorkingSpreadsheet workingSpreadsheet) {
		return new PdfExport(workingSpreadsheet);
	}

	public PdfExport sheetName(String sheetName) {
		this.sheetName = sheetName;
		return this;
	}

	public PdfExport range(RangePosition rangePosition) throws GenerateException {
		this.rangePosition = rangePosition;
		return this;
	}

	public PdfExport zielVerzeichnis(Path dir) {
		this.zielVerzeichnis = dir;
		return this;
	}

	private Object selectRangetoExport() throws GenerateException {
		checkNotNull(sheetName);
		checkNotNull(rangePosition);

		XSelectionSupplier xSelectionSupplier = Lo.qi(XSelectionSupplier.class,
				getWorkingSpreadsheet().getWorkingSpreadsheetView());

		XCellRange cell = TurnierSheet.from(sheetName, getWorkingSpreadsheet()).setActiv()
				.getCellRangeByPosition(rangePosition);

		xSelectionSupplier.select(cell);
		return xSelectionSupplier.getSelection();
	}

	private Object sheetSelektion(XSpreadsheet xSheet) {
		getWorkingSpreadsheet().getWorkingSpreadsheetView().setActiveSheet(xSheet);
		var xSelectionSupplier = Lo.qi(XSelectionSupplier.class,
				getWorkingSpreadsheet().getWorkingSpreadsheetView());
		xSelectionSupplier.select(xSheet);
		return xSelectionSupplier.getSelection();
	}

	public URI doExport() throws GenerateException {
		URI pdfFile = null;

		if (!istGespeichert() && zielVerzeichnis == null) {
			String errMsg = I18n.get("error.dokument.nicht.gespeichert");
			logger.warn(errMsg);
			throw new GenerateException(errMsg);
		}

		try {
			String newFileName = istGespeichert()
					? newFileName(null)
					: newFileName(null, ungespeicherterBasisDateiname());
			newFileName = FilenameUtils.removeExtension(newFileName);
			pdfFile = zielVerzeichnis != null
					? zielVerzeichnis.resolve(newFileName + ".pdf").toUri()
					: newLocationInSameDir(newFileName + ".pdf");

			logger.info("Erstelle PDF :" + pdfFile);
			Map<String, Object> filterData = new HashMap<>();

			Map<String, Object> saveprops = new HashMap<>();
			saveprops.put(SAVE_PROP_OVERWRITE, Boolean.TRUE);
			saveprops.put(SAVE_PROP_FILTER_NAME, CALC_PDF_EXPORT_FILTER);

			filterData.put(SAVE_PROP_FILTER_EXPORTBOOKMARKS, Boolean.FALSE);
			filterData.put(SAVE_PROP_FILTER_EXPORTNOTES, Boolean.FALSE);

			if (rangePosition != null) {
				filterData.put(SAVE_PROP_FILTER_SELECTION, selectRangetoExport());
			} else if (sheetName != null) {
				var xSheet = this.getSheetHelper().findByName(sheetName);
				if (xSheet != null) {
					filterData.put(SAVE_PROP_FILTER_SELECTION, sheetSelektion(xSheet));
				} else {
					throw new GenerateException(I18n.get("error.tabelle.nicht.vorhanden", sheetName));
				}
			}

			if (!filterData.isEmpty()) {
				saveprops.put(SAVE_PROP_FILTERDATA, PropertyValueHelper.map2Proplist(filterData));
			}

			// stores the object's persistent data to a URL and continues to be a representation of the old URL
			getxStorable().storeToURL(pdfFile.toString(), PropertyValueHelper.map2Proplist(saveprops));

		} catch (MalformedURLException | URISyntaxException | IOException e) {
			logger.error(e.getMessage(), e);
		}

		return pdfFile;
	}

	private String ungespeicherterBasisDateiname() {
		return ungespeicherterBasisDateiname(
				new DocumentPropertiesHelper(getWorkingSpreadsheet()).getTurnierSystemAusDocument());
	}

	static String ungespeicherterBasisDateiname(TurnierSystem turnierSystem) {
		if (turnierSystem == null) {
			return "Export";
		}
		return switch (turnierSystem) {
			case LIGA -> "Liga";
			case SUPERMELEE -> "SuperMelee";
			case JGJ -> "JederGegenJeden";
			case SCHWEIZER -> "Schweizer";
			case MAASTRICHTER -> "Maastrichter";
			case KO -> "KO";
			case FORMULEX -> "FormuleX";
			case KASKADE -> "KaskadenKO";
			case POULE -> "Poule";
			case TRIPTETE -> "TripTete";
			default -> "Export";
		};
	}

}
