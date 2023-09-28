package de.petanqueturniermanager.liga.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

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
	private String logoUrl = null;
	private String pdfDownloadBaseUrl = null;
	private String spielplanPdfName = null;
	private String ranglistePdfName = null;

	private static String PDF_IMAGE = "<img src=\"http://bc-linden.de/images/bclinden/pdf-download.png\" align=\"right\" style=\"width:50px;;margin-right:10px;\">";

	private static String PTM_IMAGE = "<a href=\"https://michaelmassee.github.io/Petanque-Turnier-Manager/\">"
			+ "<img src=\"https://github.com/michaelmassee/Petanque-Turnier-Manager/raw/master/doku/images/petanqueturniermanager-logo-256px.png\" align=\"right\" "
			+ "style=\"width:60px;margin-right:10px;position:relative;bottom:60px;\">" + "</a>";

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

	public LigaHtmlCleaner spielplanPdfName(String spielplanPdfName) {
		this.spielplanPdfName = spielplanPdfName;
		return this;
	}

	public LigaHtmlCleaner ranglistePdfName(String ranglistePdfName) {
		this.ranglistePdfName = ranglistePdfName;
		return this;
	}

	public LigaHtmlCleaner logoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
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
			htmlTargetFile.createNewFile(); // new if not exist

			ligaHtmlOrg = Jsoup.parse(htmlOrgFile);
			ligaHtmlNew = newLigaDocument();

			addMetaTagsToHeader(ligaHtmlNew);
			addStyleInHeader(ligaHtmlNew);

			Element bodyNew = ligaHtmlNew.select("body").first();

			bodyNew.append("<hr>");
			if (logoUrl != null) {
				String logoImg = "<img src=\"" + logoUrl + "\" align=\"right\" style=\"width:80px;\">";
				bodyNew.append(logoImg);
			}

			bodyNew.append("<h1>Überblick</h1>");
			bodyNew.append("<A HREF=\"#table0\">Meldeliste</A><br>");
			bodyNew.append("<A HREF=\"#table1\">Spielplan</A><br>");
			bodyNew.append("<A HREF=\"#table2\">Rangliste</A><br>");
			bodyNew.append("<A HREF=\"#table3\">Direktvergleich</A><br>");

			addMeldeliste(ligaHtmlOrg, bodyNew);
			addSpielplan(ligaHtmlOrg, bodyNew);
			boolean addRangliste = addRangliste(ligaHtmlOrg, bodyNew);
			boolean addDirektvergleich = addDirektvergleich(ligaHtmlOrg, bodyNew);

			if (addRangliste || addDirektvergleich) {
				bodyNew.append(PTM_IMAGE);
			}

			try (BufferedWriter fileStream = new BufferedWriter(
					new FileWriter(htmlTargetFile, StandardCharsets.UTF_8))) {
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

			Element turnierSystem = anmeldungenClone.selectFirst("tr:containsWholeText(Turniersystem: Liga)");
			if (turnierSystem != null) {
				turnierSystem.remove();
			}
			anmeldungenClone.appendTo(bodyNew);
		}
	}

	private void addSpielplan(Document ligaHtmlOrg, Element bodyNew) {
		Element spielplanClone = findSpielplanTable(ligaHtmlOrg);
		if (spielplanClone != null) {
			cleanUpTable(spielplanClone);
			bodyNew.append("<hr>");

			String formatPdfDownloadLink = formatPdfDownloadLink(spielplanPdfName);
			if (formatPdfDownloadLink != null) {
				bodyNew.append(formatPdfDownloadLink);
			}
			bodyNew.append("<A NAME=\"table1\"><h1>Spielplan</h1></A>");
			spielplanClone.selectFirst("colgroup").attr("width", "70");
			spielplanClone.appendTo(bodyNew);
		}
	}

	private boolean addRangliste(Document ligaHtmlOrg, Element bodyNew) {
		Element ranglisteClone = findRanglisteTable(ligaHtmlOrg);
		if (ranglisteClone != null) {
			cleanUpTable(ranglisteClone);

			String reihenfolge = "Reihenfolge zur Ermittlung der Platzierung: 1. Punkte +, 2. Spiele +, 3. Spielpunkte Δ, 4. Direktvergleich";
			Element reihenfolgeEL = ranglisteClone.selectFirst("tr:containsWholeText(" + reihenfolge + ")");
			if (reihenfolgeEL != null) {
				reihenfolgeEL.remove();
			}
			Element fontElePlatz = ranglisteClone.selectFirst("font:containsWholeOwnText(Platz)");
			Element fontElePlatzParent = fontElePlatz.parent();
			fontElePlatz.remove();
			Element pPlatz = new Element("p");
			pPlatz.attr("class", CLASS_TXTROTATE);
			pPlatz.appendText("Platz");
			pPlatz.appendTo(fontElePlatzParent);

			Element tdEleBegegnung = ranglisteClone.selectFirst("td:containsOwn(Begegn)");
			for (TextNode txNode : tdEleBegegnung.textNodes()) {
				txNode.remove();
			}

			Element pBegn = new Element("p");
			pBegn.attr("class", CLASS_TXTROTATE);
			pBegn.attr("style", "font-size:15px;");
			pBegn.appendText("Begegn.");
			pBegn.appendTo(tdEleBegegnung);

			bodyNew.append("<hr>");
			String formatPdfDownloadLink = formatPdfDownloadLink(ranglistePdfName);
			if (formatPdfDownloadLink != null) {
				bodyNew.append(formatPdfDownloadLink);
			}
			bodyNew.append("<A NAME=\"table2\"><h1>Rangliste</h1></A>");
			bodyNew.append("<p style=\"font-size:80%\">" + reihenfolge + "</p>");
			ranglisteClone.appendTo(bodyNew);
		} else {
			return false;
		}
		return true;
	}

	private boolean addDirektvergleich(Document ligaHtmlOrg, Element bodyNew) {
		Element direktvergleichClone = findDirektvergleichTable(ligaHtmlOrg);
		if (direktvergleichClone != null) {
			cleanUpTable(direktvergleichClone);
			bodyNew.append("<hr>");
			bodyNew.append("<A NAME=\"table3\"><h1>Direktvergleich</h1></A>");

			for (DirektvergleichResult dvgl : DirektvergleichResult.values()) {
				direktvergleichClone.selectFirst("tr:containsWholeText(" + dvgl.getAnzeigeText() + ")").remove();
			}
			direktvergleichClone.appendTo(bodyNew);

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
		} else {
			return false;
		}
		return true;
	}

	// <comment>Meldenummer (manuell nicht ändern)</comment>
	private Element findAnmeldungenTable(Document ligaHtmlOrg) {
		return selectClone(ligaHtmlOrg, "table:containsWholeText(Meldenummer \\(manuell nicht ändern\\))",
				"Anmeldungen");
	}

	// <td style="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" rowspan=2 align="center" valign=middle
	// bgcolor="#E6EBF4">KW</td>
	private Element findSpielplanTable(Document ligaHtmlOrg) {
		return selectClone(ligaHtmlOrg, "table:containsWholeText(KW):containsWholeText(Datum)", "Spielplan");
	}

	// Platz
	private Element findRanglisteTable(Document ligaHtmlOrg) {
		return selectClone(ligaHtmlOrg, "table:containsWholeText(Platz)", "Rangliste");
	}

	// Unentschieden
	private Element findDirektvergleichTable(Document ligaHtmlOrg) {
		return selectClone(ligaHtmlOrg, "table:containsWholeText(Unentschieden)", "Direktvergleich");
	}

	private Element selectClone(Document ligaHtmlOrg, String selString, String tableName) {
		try {
			Element spielPlan = ligaHtmlOrg.selectFirst(selString);
			if (spielPlan != null) {
				return spielPlan.clone();
			} else {
				logger.warn(tableName + " nicht gefunden");
			}
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

	private void addMetaTagsToHeader(Document ligaHtmlNew) {
		Element headEl = ligaHtmlNew.selectFirst("head");
		// <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
		headEl.appendElement("meta").attr("http-equiv", "content-type").attr("content", "text/html; charset=utf-8");
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

	private String formatPdfDownloadLink(String pdfname) {
		if (pdfDownloadBaseUrl != null && pdfname != null) {
			String dwnlLink = pdfDownloadBaseUrl + pdfname;
			return "<a href=\"" + dwnlLink + "\" download>" + PDF_IMAGE + "</a>";
		}
		return null;
	}

}
