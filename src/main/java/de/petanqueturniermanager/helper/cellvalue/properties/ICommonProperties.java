/**
 * Erstellung 16.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.cellvalue.properties;

/**
 * @author Michael Massee
 *
 * @param <T>
 */
public interface ICommonProperties {

	// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1table_1_1CellProperties.html
	String WIDTH = "Width";
	String HORI_JUSTIFY = "HoriJustify";
	String VERT_JUSTIFY = "VertJustify";
	String CHAR_COLOR = "CharColor";
	String CHAR_WEIGHT = "CharWeight";
	String CHAR_HEIGHT = "CharHeight";
	// public static final String TABLE_BORDER = "TableBorder";
	String TABLE_BORDER2 = "TableBorder2";
	String HEIGHT = "Height";
	// SHRINK_TO_FIT = Boolean, Text in der Zelle wird an der Zelle Große angepasst
	String SHRINK_TO_FIT = "ShrinkToFit";
	String CELL_BACK_COLOR = "CellBackColor";
	String ROTATEANGLE = "RotateAngle";
	String IS_CELLBACKGROUND_TRANSPARENT = "IsCellBackgroundTransparent";

	// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1util_1_1NumberFormatter.html
	// https://wiki.openoffice.org/wiki/Documentation/DevGuide/OfficeDev/Number_Formats
	String NUMBERFORMAT = "NumberFormat";

	String TOP_MARGIN = "ParaTopMargin";
	String BOTTOM_MARGIN = "ParaBottomMargin";
	String LEFT_MARGIN = "ParaLeftMargin";
	String RIGHT_MARGIN = "ParaRightMargin";

}