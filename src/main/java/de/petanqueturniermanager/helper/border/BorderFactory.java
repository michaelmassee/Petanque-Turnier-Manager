/**
* Erstellung : 02.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.border;

import com.sun.star.table.BorderLine2;
import com.sun.star.table.BorderLineStyle;
import com.sun.star.table.TableBorder2;

public class BorderFactory {

	BorderLine2 topLine = null;
	BorderLine2 bottomLine = null;
	BorderLine2 leftLine = null;
	BorderLine2 rightLine = null;
	BorderLine2 horizontalLine = null;
	BorderLine2 verticalLine = null;
	BorderLine2 forLine = null;
	int distance = -1;

	private BorderFactory() {
	}

	private BorderFactory(BorderFactory factr) {
		BorderFactory newBorderFactory = new BorderFactory();
		newBorderFactory.topLine = cloneBorderLine2(factr.topLine);
		newBorderFactory.bottomLine = cloneBorderLine2(factr.bottomLine);
		newBorderFactory.leftLine = cloneBorderLine2(factr.leftLine);
		newBorderFactory.rightLine = cloneBorderLine2(factr.rightLine);
		newBorderFactory.horizontalLine = cloneBorderLine2(factr.horizontalLine);
		newBorderFactory.verticalLine = cloneBorderLine2(factr.verticalLine);
		newBorderFactory.forLine = cloneBorderLine2(factr.forLine);
		newBorderFactory.distance = factr.distance;
	}

	public static final BorderFactory from() {
		return new BorderFactory();
	}

	public static final BorderFactory from(BorderFactory factr) {
		return new BorderFactory(factr);
	}

	public TableBorder2 toBorder() {
		TableBorder2 tableBorder = new TableBorder2();
		tableBorder.TopLine = topLine;
		if (topLine != null) {
			tableBorder.IsTopLineValid = true;
		} else {
			tableBorder.TopLine = new BorderLine2();
			tableBorder.IsTopLineValid = false;
		}

		tableBorder.BottomLine = bottomLine;
		if (bottomLine != null) {
			tableBorder.IsBottomLineValid = true;
		} else {
			tableBorder.BottomLine = new BorderLine2();
			tableBorder.IsBottomLineValid = false;
		}

		tableBorder.LeftLine = leftLine;
		if (leftLine != null) {
			tableBorder.IsLeftLineValid = true;
		} else {
			tableBorder.LeftLine = new BorderLine2();
			tableBorder.IsLeftLineValid = false;
		}

		tableBorder.RightLine = rightLine;
		if (rightLine != null) {
			tableBorder.IsRightLineValid = true;
		} else {
			tableBorder.RightLine = new BorderLine2();
			tableBorder.IsRightLineValid = false;
		}

		tableBorder.HorizontalLine = horizontalLine;
		if (horizontalLine != null) {
			tableBorder.IsHorizontalLineValid = true;
		} else {
			tableBorder.HorizontalLine = new BorderLine2();
			tableBorder.IsHorizontalLineValid = false;
		}

		tableBorder.VerticalLine = verticalLine;
		if (verticalLine != null) {
			tableBorder.IsVerticalLineValid = true;
		} else {
			tableBorder.VerticalLine = new BorderLine2();
			tableBorder.IsVerticalLineValid = false;
		}

		if (distance > -1) {
			tableBorder.Distance = (short) distance;
			tableBorder.IsDistanceValid = true;
		} else {
			tableBorder.IsDistanceValid = false;
		}

		return tableBorder;
	}

	public BorderFactory allThin() {
		topLine = BorderFactory.thinLine();
		bottomLine = BorderFactory.thinLine();
		leftLine = BorderFactory.thinLine();
		rightLine = BorderFactory.thinLine();
		horizontalLine = BorderFactory.thinLine();
		verticalLine = BorderFactory.thinLine();
		return this;
	}

	public BorderFactory allBold() {
		topLine = BorderFactory.boldLine();
		bottomLine = BorderFactory.boldLine();
		leftLine = BorderFactory.boldLine();
		rightLine = BorderFactory.boldLine();
		horizontalLine = BorderFactory.boldLine();
		verticalLine = BorderFactory.boldLine();
		return this;
	}

	public BorderFactory distance(int distance) {
		this.distance = distance;
		return this;
	}

	public BorderFactory thinLn() {
		forLine = BorderFactory.thinLine();
		return this;
	}

	public BorderFactory boldLn() {
		forLine = BorderFactory.boldLine();
		return this;
	}

	public BorderFactory doubleLn() {
		forLine = BorderFactory.doubleLine();
		return this;
	}

	/**
	 * Linie Oben aendern
	 * 
	 */
	public BorderFactory forTop() {
		if (forLine != null) {
			topLine = forLine;
		}
		return this;
	}

	/**
	 * Linie Unten aendern
	 * 
	 */

	public BorderFactory forBottom() {
		if (forLine != null) {
			bottomLine = forLine;
		}
		return this;
	}

	/**
	 * Linie Links aendern
	 * 
	 */
	public BorderFactory forLeft() {
		if (forLine != null) {
			leftLine = forLine;
		}
		return this;
	}

	/**
	 * Linie Rechts aendern
	 * 
	 */
	public BorderFactory forRight() {
		if (forLine != null) {
			rightLine = forLine;
		}
		return this;
	}

	// https://api.libreoffice.org/docs/idl/ref/structcom_1_1sun_1_1star_1_1table_1_1BorderLine2.html#af257a0b7f752fc39fc92aca04f336030
	static BorderLine2 thinLine() {
		BorderLine2 borderLine = new BorderLine2();
		borderLine.Color = 0; // 0x0d3472;
		borderLine.InnerLineWidth = 0;
		borderLine.OuterLineWidth = 10;
		borderLine.LineDistance = 0; // muss 0
		borderLine.LineStyle = BorderLineStyle.SOLID;
		borderLine.LineWidth = 0;
		return borderLine;
	}

	static BorderLine2 boldLine() {
		BorderLine2 borderLine = new BorderLine2();
		borderLine.Color = 0; // 0x0d3472;
		borderLine.InnerLineWidth = 18;
		borderLine.OuterLineWidth = 18;
		borderLine.LineDistance = 53;
		borderLine.LineStyle = BorderLineStyle.SOLID;
		borderLine.LineWidth = 60; // 60, 88
		return borderLine;
	}

	static BorderLine2 doubleLine() {
		BorderLine2 borderLine = new BorderLine2();
		borderLine.Color = 0; // 0x0d3472;
		borderLine.InnerLineWidth = 18;
		borderLine.OuterLineWidth = 18;
		borderLine.LineDistance = 53;
		borderLine.LineStyle = BorderLineStyle.DOUBLE_THIN;
		borderLine.LineWidth = 88;
		return borderLine;
	}

	private BorderLine2 cloneBorderLine2(BorderLine2 ln) {
		if (ln != null) {
			BorderLine2 borderLine = new BorderLine2();
			borderLine.Color = ln.Color;
			borderLine.InnerLineWidth = ln.InnerLineWidth;
			borderLine.OuterLineWidth = ln.OuterLineWidth;
			borderLine.LineDistance = ln.LineDistance;
			borderLine.LineStyle = ln.LineStyle;
			borderLine.LineWidth = ln.LineWidth;
			return borderLine;
		}
		return null;
	}
}
