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
public class RanglisteGeradeUngeradeFormatHelper {

	private final ISheet sheet;
	private final RangePosition rangePos;
	private int geradeColor = BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR;
	private int ungeradeColor = BasePropertiesSpalte.DEFAULT_UNGERADE__BACK_COLOR;
	private int validateSpalteNr = -1;

	private RanglisteGeradeUngeradeFormatHelper(ISheet sheet, RangePosition rangePos) {
		this.sheet = checkNotNull(sheet);
		this.rangePos = checkNotNull(rangePos);
	}

	public static final RanglisteGeradeUngeradeFormatHelper from(ISheet sheet, RangePosition rangePos) {
		return new RanglisteGeradeUngeradeFormatHelper(sheet, rangePos);
	}

	public RanglisteGeradeUngeradeFormatHelper geradeFarbe(int geradeColor) {
		this.geradeColor = geradeColor;
		return this;
	}

	public RanglisteGeradeUngeradeFormatHelper ungeradeFarbe(int ungeradeColor) {
		this.ungeradeColor = ungeradeColor;
		return this;
	}

	public RanglisteGeradeUngeradeFormatHelper validateSpalte(int validateSpalteNr) {
		this.validateSpalteNr = validateSpalteNr;
		return this;
	}

	public RanglisteGeradeUngeradeFormatHelper apply() throws GenerateException {
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
					.style(new FehlerStyle()).applyAndDoReset();
		}

		conditionalFormatHelper.formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyAndDoReset()
				.formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).applyAndDoReset();

		return this;
	}

}
