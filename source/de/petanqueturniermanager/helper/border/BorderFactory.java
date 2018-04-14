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

	public static final BorderFactory from() {
		return new BorderFactory();
	}

	public TableBorder2 toBorder() {
		TableBorder2 tableBorder = new TableBorder2();
		tableBorder.TopLine = this.topLine;
		tableBorder.BottomLine = this.bottomLine;
		tableBorder.LeftLine = this.leftLine;
		tableBorder.RightLine = this.rightLine;
		tableBorder.HorizontalLine = this.horizontalLine;
		tableBorder.VerticalLine = this.verticalLine;
		tableBorder.IsTopLineValid = tableBorder.IsBottomLineValid = true;
		tableBorder.IsLeftLineValid = tableBorder.IsRightLineValid = true;
		tableBorder.IsDistanceValid = tableBorder.IsHorizontalLineValid = tableBorder.IsVerticalLineValid = true;
		return tableBorder;
	}

	public BorderFactory allThin() {
		this.topLine = BorderFactory.thinLine();
		this.bottomLine = BorderFactory.thinLine();
		this.leftLine = BorderFactory.thinLine();
		this.rightLine = BorderFactory.thinLine();
		this.horizontalLine = BorderFactory.thinLine();
		this.verticalLine = BorderFactory.thinLine();
		return this;
	}

	public BorderFactory boldLn() {
		this.forLine = BorderFactory.boldLine();
		return this;
	}

	public BorderFactory doubleLn() {
		this.forLine = BorderFactory.doubleLine();
		return this;
	}

	public BorderFactory forTop() {
		if (this.forLine != null) {
			this.topLine = this.forLine;
		}
		return this;
	}

	public BorderFactory forBottom() {
		if (this.forLine != null) {
			this.bottomLine = this.forLine;
		}
		return this;
	}

	public BorderFactory forLeft() {
		if (this.forLine != null) {
			this.leftLine = this.forLine;
		}
		return this;
	}

	public BorderFactory forRight() {
		if (this.forLine != null) {
			this.rightLine = this.forLine;
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
		borderLine.LineWidth = 88;
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
