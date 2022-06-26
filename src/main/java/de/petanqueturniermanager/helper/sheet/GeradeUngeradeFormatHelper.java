/**
 * Erstellung 24.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.ConditionOperator;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
public class GeradeUngeradeFormatHelper {

	private final ISheet sheet;
	private final RangePosition rangePos;
	private int geradeColor = BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR;
	private int ungeradeColor = BasePropertiesSpalte.DEFAULT_UNGERADE__BACK_COLOR;
	private int validateSpalteNr = -1;

	private GeradeUngeradeFormatHelper(ISheet sheet, RangePosition rangePos) {
		this.sheet = checkNotNull(sheet);
		this.rangePos = checkNotNull(rangePos);
	}

	public static final GeradeUngeradeFormatHelper from(ISheet sheet, RangePosition rangePos) {
		return new GeradeUngeradeFormatHelper(sheet, rangePos);
	}

	public GeradeUngeradeFormatHelper geradeFarbe(int geradeColor) {
		this.geradeColor = geradeColor;
		return this;
	}

	public GeradeUngeradeFormatHelper ungeradeFarbe(int ungeradeColor) {
		this.ungeradeColor = ungeradeColor;
		return this;
	}

	public GeradeUngeradeFormatHelper validateSpalte(int validateSpalteNr) {
		this.validateSpalteNr = validateSpalteNr;
		return this;
	}

	public GeradeUngeradeFormatHelper apply() throws GenerateException {
		RanglisteHintergrundFarbeGeradeStyle ranglisteHintergrundFarbeGeradeStyle = new RanglisteHintergrundFarbeGeradeStyle(
				geradeColor);
		RanglisteHintergrundFarbeUnGeradeStyle ranglisteHintergrundFarbeUnGeradeStyle = new RanglisteHintergrundFarbeUnGeradeStyle(
				ungeradeColor);

		ConditionalFormatHelper conditionalFormatHelper = ConditionalFormatHelper.from(sheet, rangePos).clear();

		// formula when in validate spalte ein error
		// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
		// Achtung spalte plus 1 weil A ist nicht 0 sondern 1
		if (validateSpalteNr > -1) {
			String formulaSortError = "LEN(TRIM(INDIRECT(ADDRESS(ROW();" + (validateSpalteNr + 1) + "))))>0";
			// -----------------------------
			// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
			conditionalFormatHelper.formula1(formulaSortError).operator(ConditionOperator.FORMULA)
					.style(new FehlerStyle()).applyAndReset();
		}

		conditionalFormatHelper.formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyAndReset()
				.formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).applyAndReset();

		return this;
	}

}
