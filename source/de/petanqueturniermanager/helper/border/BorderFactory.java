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

	private BorderFactory() {
	}

	public static final BorderFactory from() {
		return new BorderFactory();
	}

	public TableBorder2 toBorder() {
		TableBorder2 tableBorder = new TableBorder2();
		tableBorder.TopLine = topLine;
		tableBorder.BottomLine = bottomLine;
		tableBorder.LeftLine = leftLine;
		tableBorder.RightLine = rightLine;
		tableBorder.HorizontalLine = horizontalLine;
		tableBorder.VerticalLine = verticalLine;
		tableBorder.IsTopLineValid = tableBorder.IsBottomLineValid = true;
		tableBorder.IsLeftLineValid = tableBorder.IsRightLineValid = true;
		tableBorder.IsDistanceValid = tableBorder.IsHorizontalLineValid = tableBorder.IsVerticalLineValid = true;
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

	public BorderFactory forTop() {
		if (forLine != null) {
			topLine = forLine;
		}
		return this;
	}

	public BorderFactory forBottom() {
		if (forLine != null) {
			bottomLine = forLine;
		}
		return this;
	}

	public BorderFactory forLeft() {
		if (forLine != null) {
			leftLine = forLine;
		}
		return this;
	}

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
		borderLine.LineDistance = 0;
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

	// function getDoubleBorder ()
	// '---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//
	// Dim oRahmenLinieBold As Object
	// Set oRahmenLinieBold = createUNOStruct("com.sun.star.table.BorderLine2")
	//
	// With oRahmenLinieBold
	// .Color = RGB(0, 0, 0)
	// .InnerLineWidth = 18
	// .LineDistance = 53
	// .LineStyle = 15
	// .LineWidth = 88
	// .OuterLineWidth = 18
	// End With
	// '---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//
	// Dim oRahmenLinieBold As Object
	// Set oRahmenLinieBold = createUNOStruct("com.sun.star.table.BorderLine2")
	//
	// With oRahmenLinieBold
	// .Color = RGB(0, 0, 0)
	// .InnerLineWidth = 0
	// .LineDistance = 0
	// .LineStyle = 0
	// .LineWidth = 53
	// .OuterLineWidth = 53
	// End With
	//
	// getBoldBorder = oRahmenLinieBold
	//
	// end Function

}
