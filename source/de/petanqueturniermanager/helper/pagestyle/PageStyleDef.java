package de.petanqueturniermanager.helper.pagestyle;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XHeaderFooterContent;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.XPropertyHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class PageStyleDef {

	public static final String SPIELTAG_FORMAT_NAME = "Spieltag";

	private final String name;
	private final PageProperties pageProperties;

	public PageStyleDef(SpielTagNr spielTag) throws GenerateException {
		this(SPIELTAG_FORMAT_NAME + " " + spielTag.getNr(), new PageProperties());
	}

	public PageStyleDef(String name) {
		this(name, new PageProperties());
	}

	public PageStyleDef(String name, PageProperties pageProperties) {
		checkNotNull(name);
		checkNotNull(pageProperties);
		this.name = name;
		this.pageProperties = pageProperties;
	}

	public String getName() {
		return name;
	}

	public PageProperties getPageProperties() {
		return pageProperties;
	}

	public void formatHeaderFooter(XPropertyHelper xPropSet) {
		checkNotNull(xPropSet);
		if (pageProperties.getHeaderLeft() != null || pageProperties.getHeaderCenter() != null || pageProperties.getHeaderRight() != null) {
			// header
			Object headerProp = xPropSet.getProperty(PageProperties.RIGHTPAGE_HEADER_CONTENT);
			if (headerProp != null) {
				XHeaderFooterContent headerFooterContent = UnoRuntime.queryInterface(XHeaderFooterContent.class, headerProp);
				if (pageProperties.getHeaderLeft() != null) {
					headerFooterContent.getLeftText().setString(pageProperties.getHeaderLeft());
				}
				if (pageProperties.getHeaderCenter() != null) {
					headerFooterContent.getCenterText().setString(pageProperties.getHeaderCenter());
				}
				if (pageProperties.getHeaderRight() != null) {
					headerFooterContent.getRightText().setString(pageProperties.getHeaderRight());
				}
				pageProperties.setHeaderContent(headerFooterContent);
			}
		}

		if (pageProperties.getFooterLeft() != null || pageProperties.getFooterCenter() != null || pageProperties.getFooterRight() != null) {
			// footer
			Object footerProp = xPropSet.getProperty(PageProperties.RIGHTPAGE_FOOTER_CONTENT);
			if (footerProp != null) {
				XHeaderFooterContent headerFooterContent = UnoRuntime.queryInterface(XHeaderFooterContent.class, footerProp);
				if (pageProperties.getFooterLeft() != null) {
					headerFooterContent.getLeftText().setString(pageProperties.getFooterLeft());
				}
				if (pageProperties.getFooterCenter() != null) {
					headerFooterContent.getCenterText().setString(pageProperties.getFooterCenter());
				}
				if (pageProperties.getFooterRight() != null) {
					headerFooterContent.getRightText().setString(pageProperties.getFooterRight());
				}
				pageProperties.setFooterContent(headerFooterContent);
			}
		}
	}

	public PageStyleDef setFooterRight(String text) {
		pageProperties.setFooterRight(text);
		return this;
	}

	public void setFooterLeft(String text) {
		pageProperties.setFooterLeft(text);
	}

	public void setHeaderCenter(String text) {
		pageProperties.setHeaderCenter(text);
	}
}