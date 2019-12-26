package de.petanqueturniermanager.helper.pagestyle;

import java.util.HashMap;

import com.sun.star.sheet.XHeaderFooterContent;

/**
 *
 * @author michael
 */

public class PageProperties extends HashMap<String, Object> {

	// https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1style_1_1PageProperties.html

	public static final String HEADER_IS_ON = "HeaderIsOn";
	public static final String HEADER_IS_SHARED = "HeaderIsShared"; // linker und rechter Seite gleich
	public static final String HEADER_TEXT = "HeaderText";
	public static final String RIGHTPAGE_HEADER_CONTENT = "RightPageHeaderContent";

	public static final String FOOTER_IS_ON = "FooterIsOn";
	public static final String FOOTER_IS_SHARED = "FooterIsShared"; // linker und rechter Seite gleich
	public static final String FOOTER_TEXT = "FooterText";
	public static final String RIGHTPAGE_FOOTER_CONTENT = "RightPageFooterContent";

	private String headerLeft = null;
	private String headerCenter = null;
	private String headerRight = null;

	private String footerLeft = null;
	private String footerCenter = null;
	private String footerRight = null;

	// -----------------------------------------------------
	public PageProperties setHeaderLeft(String text) {
		headerLeft = text;
		return this;
	}

	public String getHeaderLeft() {
		return headerLeft;
	}

	public PageProperties setHeaderCenter(String text) {
		headerCenter = text;
		return this;
	}

	public String getHeaderCenter() {
		return headerCenter;
	}

	public PageProperties setHeaderRight(String text) {
		headerRight = text;
		return this;
	}

	public String getHeaderRight() {
		return headerRight;
	}

	// -----------------------------------------------------

	public PageProperties setFooterLeft(String text) {
		footerLeft = text;
		return this;
	}

	public String getFooterLeft() {
		return footerLeft;
	}

	public PageProperties setFooterCenter(String text) {
		footerCenter = text;
		return this;
	}

	public String getFooterCenter() {
		return footerCenter;
	}

	public PageProperties setFooterRight(String text) {
		footerRight = text;
		return this;
	}

	public String getFooterRight() {
		return footerRight;
	}
	// -----------------------------------------------------

	public void setHeaderContent(XHeaderFooterContent headerFooterContent) {
		put(RIGHTPAGE_HEADER_CONTENT, headerFooterContent);
		put(HEADER_IS_ON, true);
		put(HEADER_IS_SHARED, true);
	}

	/**
	 * links und rechts footer gleich
	 *
	 * @param headerFooterContent
	 */
	public void setFooterContent(XHeaderFooterContent headerFooterContent) {
		put(RIGHTPAGE_FOOTER_CONTENT, headerFooterContent);
		put(FOOTER_IS_ON, true);
		put(FOOTER_IS_SHARED, true);
	}

}
