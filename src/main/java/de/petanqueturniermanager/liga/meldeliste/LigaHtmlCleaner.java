package de.petanqueturniermanager.liga.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import de.petanqueturniermanager.algorithmen.DirektvergleichResult;

/**
 * Erstellung 04.07.2022 / Michael Massee<br>
 * <br>
 * Selector API<br>
 * // https://jsoup.org/cookbook/extracting-data/selector-syntax<br>
 * // https://jsoup.org/apidocs/org/jsoup/select/Selector.html<br>
 * <br>
 */

public class LigaHtmlCleaner {
	private static final String CLASS_TXTROTATE = "txtrotate";

	private static final Logger logger = LogManager.getLogger(LigaHtmlCleaner.class);

	private final File htmlOrgFile;
	private final File htmlTargetFile;
	private String gruppe = null;
	private String logoUrl = null;
	private String pdfImageUrl = null;
	private String pdfDownloadBaseUrl = null;

	static String PTM_IMAGE = "<a href=\"https://michaelmassee.github.io/Petanque-Turnier-Manager/\">"
			+ "<img src=\"https://github.com/michaelmassee/Petanque-Turnier-Manager/raw/master/doku/images/petanqueturniermanager-logo-256px.png\" align=\"right\" style=\"width:80px;\">"
			+ "</a>";

	LigaHtmlCleaner(URI htmlOrgFile, File htmlTargetFile) {
		this(new File(checkNotNull(htmlOrgFile, "htmlOrgFile==null")), htmlTargetFile);
	}

	LigaHtmlCleaner(URI htmlOrgFile, URI htmlTargetFile) {
		this(new File(checkNotNull(htmlOrgFile, "htmlOrgFile==null")),
				new File(checkNotNull(htmlTargetFile, "htmlTargetFile==null")));
	}

	LigaHtmlCleaner(File htmlOrgFile, File htmlTargetFile) {
		this.htmlOrgFile = checkNotNull(htmlOrgFile, "htmlOrgFile==null");
		this.htmlTargetFile = checkNotNull(htmlTargetFile, "htmlTargetFile==null");
	}

	public static LigaHtmlCleaner from(URI htmlOrgFile, File htmlTargetFile) {
		return new LigaHtmlCleaner(htmlOrgFile, htmlTargetFile);
	}

	public LigaHtmlCleaner gruppe(String gruppe) {
		this.gruppe = gruppe;
		return this;
	}

	public LigaHtmlCleaner logoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
		return this;
	}

	public LigaHtmlCleaner pdfImageUrl(String pdfImageUrl) {
		this.pdfImageUrl = pdfImageUrl;
		return this;
	}

	public LigaHtmlCleaner pdfDownloadBaseUrl(String pdfDownloadBaseUrl) {
		this.pdfDownloadBaseUrl = pdfDownloadBaseUrl;
		return this;
	}

	public File cleanUp() {
		Document ligaHtmlOrg;
		Document ligaHtmlNew;
		try {
			ligaHtmlOrg = Jsoup.parse(htmlOrgFile);
			ligaHtmlNew = newLigaDocument();

			addStyleInHeader(ligaHtmlNew);

			htmlTargetFile.createNewFile();

			Element bodyNew = ligaHtmlNew.select("body").first();

			bodyNew.append("<hr>");
			if (logoUrl != null) {
				bodyNew.append(logoUrl);
			}

			bodyNew.append("<h1>Überblick</h1>");
			bodyNew.append("<A HREF=\"#table0\">Meldeliste</A><br>");
			bodyNew.append("<A HREF=\"#table1\">Spielplan</A><br>");
			bodyNew.append("<A HREF=\"#table2\">Rangliste</A><br>");
			bodyNew.append("<A HREF=\"#table3\">Direktvergleich</A><br>");

			addMeldeliste(ligaHtmlOrg, bodyNew);
			addSpielplan(ligaHtmlOrg, bodyNew);
			addRangliste(ligaHtmlOrg, bodyNew);
			addDirektvergleich(ligaHtmlOrg, bodyNew);

			try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(htmlTargetFile))) {
				fileStream.write(ligaHtmlNew.outerHtml());
			}

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return htmlTargetFile;
	}

	private void addMeldeliste(Document ligaHtmlOrg, Element bodyNew) {
		Element anmeldungenClone = findAnmeldungenTable(ligaHtmlOrg);
		if (anmeldungenClone != null) {
			cleanUpTable(anmeldungenClone);
			bodyNew.append("<hr>");
			bodyNew.append("<A NAME=\"table0\"><h1>Meldeliste</h1></A>");

			anmeldungenClone.selectFirst("tr:containsWholeText(Turniersystem: Liga)").remove();
			anmeldungenClone.appendTo(bodyNew);
		}
	}

	private void addSpielplan(Document ligaHtmlOrg, Element bodyNew) {
		Element spielplanClone = findSpielplanTable(ligaHtmlOrg);
		if (spielplanClone != null) {
			cleanUpTable(spielplanClone);
			bodyNew.append("<hr>");

			String formatPdfDownloadLink = formatPdfDownloadLink("Spielplan");
			if (formatPdfDownloadLink != null) {
				bodyNew.append(formatPdfDownloadLink);
			}
			bodyNew.append("<A NAME=\"table1\"><h1>Spielplan</h1></A>");
			spielplanClone.selectFirst("colgroup").attr("width", "70");
			spielplanClone.appendTo(bodyNew);
		}
	}

	private void addRangliste(Document ligaHtmlOrg, Element bodyNew) {
		Element ranglisteClone = findRanglisteTable(ligaHtmlOrg);
		if (ranglisteClone != null) {
			cleanUpTable(ranglisteClone);

			String reihenfolge = "Reihenfolge zur Ermittlung der Platzierung: 1. Punkte +, 2. Spiele +, 3. Spielpunkte Δ, 4. Direktvergleich";
			Element reihenfolgeEL = ranglisteClone.selectFirst("tr:containsWholeText(" + reihenfolge + ")");
			if (reihenfolgeEL != null) {
				reihenfolgeEL.remove();
			}
			Element fontElePlatz = ranglisteClone.selectFirst("font:containsWholeText(Platz)");
			Element fontElePlatzParent = fontElePlatz.parent();
			fontElePlatz.remove();
			Element pPlatz = new Element("p");
			pPlatz.attr("class", CLASS_TXTROTATE);
			pPlatz.appendText("Platz");
			pPlatz.appendTo(fontElePlatzParent);

			bodyNew.append("<hr>");
			String formatPdfDownloadLink = formatPdfDownloadLink("Rangliste");
			if (formatPdfDownloadLink != null) {
				bodyNew.append(formatPdfDownloadLink);
			}
			bodyNew.append("<A NAME=\"table2\"><h1>Rangliste</h1></A>");
			bodyNew.append("<p style=\"font-size:80%\">" + reihenfolge + "</p>");
			ranglisteClone.appendTo(bodyNew);
		}

	}

	private void addDirektvergleich(Document ligaHtmlOrg, Element bodyNew) {
		Element direktvergleichClone = findDirektvergleichTable(ligaHtmlOrg);
		if (direktvergleichClone != null) {
			cleanUpTable(direktvergleichClone);
			bodyNew.append("<hr>");
			bodyNew.append("<A NAME=\"table3\"><h1>Direktvergleich</h1></A>");

			for (DirektvergleichResult dvgl : DirektvergleichResult.values()) {
				direktvergleichClone.selectFirst("tr:containsWholeText(" + dvgl.getAnzeigeText() + ")").remove();
			}
			direktvergleichClone.appendTo(bodyNew);
			bodyNew.append(PTM_IMAGE);

			// footer wieder einfuegen
			Element footerTable = new Element("table");
			for (DirektvergleichResult dvgl : DirektvergleichResult.sortedByCodeList()) {
				if (dvgl.getCode() == -1) {
					continue;
				}
				Element tr = new Element("tr");
				Element tdCode = new Element("td");
				tdCode.appendText("" + dvgl.getCode());
				tdCode.appendTo(tr);
				Element tdText = new Element("td");
				tdText.appendText(dvgl.getAnzeigeText());
				tdText.appendTo(tr);
				tr.appendTo(footerTable);
			}
			footerTable.appendTo(bodyNew);

		}

	}

	private Element findAnmeldungenTable(Document ligaHtmlOrg) {
		// <td colspan=2 height="22" align="left" valign=top><b><font color="#00599D">Turniersystem: Liga</font></b></td>
		try {
			return ligaHtmlOrg.selectFirst("table:containsWholeText(Turniersystem: Liga)").clone();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	// <td style="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" rowspan=2 align="center" valign=middle
	// bgcolor="#E6EBF4">KW</td>
	private Element findSpielplanTable(Document ligaHtmlOrg) {
		try {
			return ligaHtmlOrg.selectFirst("table:containsWholeText(KW)").clone();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	// Platz
	private Element findRanglisteTable(Document ligaHtmlOrg) {
		try {
			return ligaHtmlOrg.selectFirst("table:containsWholeText(Platz)").clone();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	// Unentschieden
	private Element findDirektvergleichTable(Document ligaHtmlOrg) {
		try {
			return ligaHtmlOrg.selectFirst("table:containsWholeText(Unentschieden)").clone();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	private void cleanUpTable(Element table) {
		// Tabellen reinigen
		for (Element elComment : table.select("comment")) {
			elComment.remove();
		}
		// <a class="comment-indicator"
		for (Element elA : table.select("a")) {
			elA.remove();
		}
	}

	private void addStyleInHeader(Document ligaHtmlNew) {
		Element headEl = ligaHtmlNew.selectFirst("head");
		Element styleEl = headEl.append("<style></style>").selectFirst("style");
		styleEl.append("body,div,table,thead,tbody,tfoot,tr,th,td,p { font-family:\"Liberation Sans\"}");
		styleEl.append("td {padding:4px;}");
		styleEl.append("." + CLASS_TXTROTATE
				+ "{-webkit-transform: rotate(90deg);-moz-transform: rotate(90deg);-o-transform: rotate(90deg);writing-mode: lr-tb;}");
	}

	private Document newLigaDocument() {
		Document ligaHtmlNew;
		ligaHtmlNew = Jsoup.parse("<html></html>");
		return ligaHtmlNew;
	}

	private String formatPdfDownloadLink(String tableName) {

		if (pdfDownloadBaseUrl != null && pdfImageUrl != null && gruppe != null) {
			String dwnlLink = pdfDownloadBaseUrl + StringUtils.remove(gruppe, ' ') + "-" + tableName + ".pdf";
			return "<a href=\"" + dwnlLink + "\" download>" + pdfImageUrl + "</a>";
		}
		return null;
	}

}
