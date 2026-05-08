package de.petanqueturniermanager.helper.pagestyle;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.sun.star.sheet.XHeaderFooterContent;
import com.sun.star.text.XText;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.XPropertyHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class PageStyleDef {

	private final String pageStyleName;
	private final PageProperties pageProperties;

	public PageStyleDef(SpielTagNr spielTag) {
		this(PageStyle.SPIELTAG.getName() + " " + spielTag.getNr(), new PageProperties());
	}

	public PageStyleDef(PageStyle pageStyle) {
		this(pageStyle.getName(), new PageProperties());
	}

	public PageStyleDef(String name) {
		this(name, new PageProperties());
	}

	public PageStyleDef(String pageStyleName, PageProperties pageProperties) {
		this.pageStyleName = checkNotNull(pageStyleName);
		this.pageProperties = checkNotNull(pageProperties);
	}

	public String getPageStyleName() {
		return pageStyleName;
	}

	public PageProperties getPageProperties() {
		return pageProperties;
	}

	public void formatHeaderFooter(XPropertyHelper xPropSet) {
		checkNotNull(xPropSet);
		if (pageProperties.getHeaderLeft() != null || pageProperties.getHeaderCenter() != null
				|| pageProperties.getHeaderRight() != null) {
			// header
			Object headerProp = xPropSet.getProperty(PageProperties.RIGHTPAGE_HEADER_CONTENT);
			if (headerProp != null) {
				XHeaderFooterContent headerFooterContent = Lo.qi(XHeaderFooterContent.class, headerProp);
				setStringIfChanged(headerFooterContent.getLeftText(),   pageProperties.getHeaderLeft());
				setStringIfChanged(headerFooterContent.getCenterText(), pageProperties.getHeaderCenter());
				setStringIfChanged(headerFooterContent.getRightText(),  pageProperties.getHeaderRight());
				pageProperties.setHeaderContent(headerFooterContent);
			}
		}

		if (pageProperties.getFooterLeft() != null || pageProperties.getFooterCenter() != null
				|| pageProperties.getFooterRight() != null) {
			// footer
			Object footerProp = xPropSet.getProperty(PageProperties.RIGHTPAGE_FOOTER_CONTENT);
			if (footerProp != null) {
				XHeaderFooterContent headerFooterContent = Lo.qi(XHeaderFooterContent.class, footerProp);
				setStringIfChanged(headerFooterContent.getLeftText(),   pageProperties.getFooterLeft());
				setStringIfChanged(headerFooterContent.getCenterText(), pageProperties.getFooterCenter());
				setStringIfChanged(headerFooterContent.getRightText(),  pageProperties.getFooterRight());
				pageProperties.setFooterContent(headerFooterContent);
			}
		}
	}

	/**
	 * Schreibt {@code neu} nur, wenn sich der bestehende Text vom neuen unterscheidet.
	 * Reduziert UNO-Events und Drucklayout-Reflow auf Aufrufer-Ebene.
	 * {@code null} als neuer Wert bedeutet „nicht anfassen".
	 */
	private static void setStringIfChanged(XText text, String neu) {
		if (text == null || neu == null) {
			return;
		}
		String alt = text.getString();
		if (!Objects.equals(alt, neu)) {
			text.setString(neu);
		}
	}

	public PageStyleDef setFooterRight(String text) {
		pageProperties.setFooterRight(text);
		return this;
	}

	public PageStyleDef setFooterCenter(String text) {
		pageProperties.setFooterCenter(text);
		return this;
	}

	public void setFooterLeft(String text) {
		pageProperties.setFooterLeft(text);
	}

	public void setHeaderCenter(String text) {
		pageProperties.setHeaderCenter(text);
	}

	/**
	 * @param string
	 */
	public void setHeaderLeft(String string) {
		pageProperties.setHeaderLeft(string);
	}

	/**
	 * @param string
	 */
	public void setHeaderRight(String string) {
		pageProperties.setHeaderRight(string);
	}
}
