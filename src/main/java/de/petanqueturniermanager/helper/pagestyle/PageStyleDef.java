package de.petanqueturniermanager.helper.pagestyle;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XHeaderFooterContent;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.XPropertyHelper;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

public class PageStyleDef {

	private static final Logger logger = LogManager.getLogger(PageStyleDef.class);

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
		XSpreadsheetDocument xSpreadsheetDocument = xPropSet.getXSpreadsheetDocument();

		if (pageProperties.getHeaderLeft() != null || pageProperties.getHeaderCenter() != null
				|| pageProperties.getHeaderRight() != null) {
			// header
			Object headerProp = xPropSet.getProperty(PageProperties.RIGHTPAGE_HEADER_CONTENT);
			if (headerProp != null) {
				XHeaderFooterContent headerFooterContent = Lo.qi(XHeaderFooterContent.class, headerProp);
				schreibeText(headerFooterContent.getLeftText(),   pageProperties.getHeaderLeft(),   xSpreadsheetDocument);
				schreibeText(headerFooterContent.getCenterText(), pageProperties.getHeaderCenter(), xSpreadsheetDocument);
				schreibeText(headerFooterContent.getRightText(),  pageProperties.getHeaderRight(),  xSpreadsheetDocument);
				pageProperties.setHeaderContent(headerFooterContent);
			}
		}

		if (pageProperties.getFooterLeft() != null || pageProperties.getFooterCenter() != null
				|| pageProperties.getFooterRight() != null) {
			// footer
			Object footerProp = xPropSet.getProperty(PageProperties.RIGHTPAGE_FOOTER_CONTENT);
			if (footerProp != null) {
				XHeaderFooterContent headerFooterContent = Lo.qi(XHeaderFooterContent.class, footerProp);
				schreibeText(headerFooterContent.getLeftText(),   pageProperties.getFooterLeft(),   xSpreadsheetDocument);
				schreibeText(headerFooterContent.getCenterText(), pageProperties.getFooterCenter(), xSpreadsheetDocument);
				schreibeText(headerFooterContent.getRightText(),  pageProperties.getFooterRight(),  xSpreadsheetDocument);
				pageProperties.setFooterContent(headerFooterContent);
			}
		}
	}

	/**
	 * Schreibt {@code neu} in {@code text}. Enthält der Wert keine Platzhalter-Tokens
	 * (siehe {@link HeaderFooterPlatzhalter}), wird nur bei tatsächlicher Änderung
	 * geschrieben (reduziert UNO-Events und Drucklayout-Reflow). Enthält der Wert
	 * Tokens, wird der Text neu aufgebaut und Tokens durch echte UNO-Textfelder
	 * ersetzt – ein Vergleich mit {@code text.getString()} ist hier nicht möglich,
	 * da dieser bei Feldern nur LO-interne Dummy-Werte liefert (siehe
	 * {@code ScHeaderFooterTextObj::FillDummyFieldData}).
	 * {@code null} als neuer Wert bedeutet „nicht anfassen".
	 */
	private static void schreibeText(XText text, String neu, XSpreadsheetDocument xSpreadsheetDocument) {
		if (text == null || neu == null) {
			return;
		}

		if (!HeaderFooterPlatzhalter.enthaeltPlatzhalter(neu)) {
			String alt = text.getString();
			if (!Objects.equals(alt, neu)) {
				text.setString(neu);
			}
			return;
		}

		schreibeMitPlatzhaltern(text, neu, xSpreadsheetDocument);
	}

	private static void schreibeMitPlatzhaltern(XText text, String neu, XSpreadsheetDocument xSpreadsheetDocument) {
		if (xSpreadsheetDocument == null) {
			logger.warn("Platzhalter in Kopf-/Fußzeile ohne Dokument-Kontext nicht auflösbar, schreibe Rohtext: {}", neu);
			text.setString(neu);
			return;
		}

		XMultiServiceFactory xMultiServiceFactory = Lo.qi(XMultiServiceFactory.class, xSpreadsheetDocument);
		if (xMultiServiceFactory == null) {
			logger.warn("XMultiServiceFactory nicht verfügbar, schreibe Rohtext: {}", neu);
			text.setString(neu);
			return;
		}

		text.setString("");
		XTextCursor cursor = text.createTextCursor();
		List<HeaderFooterTextSegment> segmente = HeaderFooterTokenParser.parse(neu);
		for (HeaderFooterTextSegment segment : segmente) {
			switch (segment) {
			case HeaderFooterTextSegment.Literal literal -> text.insertString(cursor, literal.text(), false);
			case HeaderFooterTextSegment.Platzhalter platzhalterSegment -> {
				XTextContent feld = erzeugeTextFeld(xMultiServiceFactory, platzhalterSegment.platzhalter());
				if (feld != null) {
					text.insertTextContent(cursor, feld, false);
				}
			}
			}
		}
	}

	private static XTextContent erzeugeTextFeld(XMultiServiceFactory xMultiServiceFactory, HeaderFooterPlatzhalter platzhalter) {
		try {
			Object feldInstanz = xMultiServiceFactory.createInstance(platzhalter.getUnoServiceName());
			return Lo.qi(XTextContent.class, feldInstanz);
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Platzhalter-Textfeld konnte nicht erzeugt werden: " + platzhalter, e);
			return null;
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
