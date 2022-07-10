package de.petanqueturniermanager.testutils;

// XML.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, December 2016

/*
 * XML utilities: XML Document IO DOM data extraction String extraction XLS transforming Flat XML filter selection
 * 
 * Useful code: http://www.drdobbs.com/jvm/easy-dom-parsing-in-java/231002580
 * 
 * http://www.java2s.com/Code/Java/XML/FindAllElementsByTagName.htm
 * 
 * 
 * XSLT tutorial at W3Schools http://www.w3schools.com/xml/xsl_intro.asp
 * 
 * Also: "Appendix B. The XSLT You Need for OpenOffice.org" http:// books.evc-cit.info/apb.php
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XML {
	private static final String INDENT_FNM = "indent.xsl";
	// for indenting XML tags, and adding newlines between tags

	// ---------------------- XML Document IO --------------------

	public static Document loadDoc(String fnm) {
		Document doc = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			dbf.setNamespaceAware(true); // false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(new FileInputStream(new File(fnm)));
		} catch (Exception e) {
			System.out.println(e);
		}
		return doc;
	} // end of loadDoc()

	public static Document url2Doc(String urlStr) {
		Document doc = null;
		try {
			URL xmlUrl = new URL(urlStr);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			dbf.setNamespaceAware(true); // false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(xmlUrl.openStream());
		} catch (Exception e) {
			System.out.println(e);
		}
		return doc;
	} // end of url2Doc()

	public static Document str2Doc(String xmlStr) {
		Document doc = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			dbf.setNamespaceAware(true); // false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(new InputSource(new StringReader(xmlStr)));
		} catch (Exception e) {
			System.out.println(e);
		}
		return doc;
	} // end of str2Doc()

	public static void saveDoc(Document doc, String xmlFnm)
	// save doc to xmlFnm
	{
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(xmlFnm));

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);
			t.transform(source, result);

			System.out.println("Saved document to " + xmlFnm);
		} catch (Exception e) {
			System.out.println("Unable to save document to " + xmlFnm);
			System.out.println("  " + e);
		}
	} // end of saveDoc()

	// --------------- DOM data extraction -----------------------

	public static Node getNode(String tagName, NodeList nodes)
	// return first tagName node from list
	{
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeName().equalsIgnoreCase(tagName))
				return node;
		}
		return null;
	} // end of getNode()

	public static String getNodeValue(String tagName, NodeList nodes)
	// find first tagName node in the list, and return its text
	{
		if (nodes == null)
			return "";

		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			if (n.getNodeName().equalsIgnoreCase(tagName))
				return getNodeValue(n);
		}
		return "";
	} // end of getNodeValue()

	public static String getNodeValue(Node node)
	// return the text stored in the node
	{
		if (node == null)
			return "";
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node n = childNodes.item(i);
			if (n.getNodeType() == Node.TEXT_NODE)
				return n.getNodeValue().trim();
		}
		return "";
	} // end of getNodeValue()

	public static ArrayList<String> getNodeValues(NodeList nodes)
	// return all the node values
	{
		if (nodes == null)
			return null;

		ArrayList<String> vals = new ArrayList<String>();
		for (int i = 0; i < nodes.getLength(); i++) {
			String val = getNodeValue(nodes.item(i));
			if (val != null)
				vals.add(val);
		}
		return vals;
	} // end of getNodeValues()

	public static String getNodeAttr(String attrName, Node node)
	// return the named attribute value from node
	{
		if (node == null)
			return "";
		NamedNodeMap attrs = node.getAttributes();
		if (attrs == null)
			return "";
		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			if (attr.getNodeName().equalsIgnoreCase(attrName))
				return attr.getNodeValue().trim();
		}
		return "";
	} // end of getNodeAttr()

	public static Object[][] getAllNodeValues(NodeList rowNodes, String[] colIDs)
	/*
	 * assumes an XML structure like:
	 * 
	 * <rowID> <col1ID>str1</col1ID> <col2ID>str2</col2ID> : <colNID>strN</colNID> </rowID> <rowID> ... </rowID> <rowID> ... </rowID>
	 * 
	 * The data from a sequence of <col>s becomes one row in the generated 2D array.
	 * 
	 * The first row of the 2D array contains the col ID strings.
	 */
	{
		int numRows = rowNodes.getLength();
		int numCols = colIDs.length;
		Object[][] data = new Object[numRows + 1][numCols];

		// put column strings in first row of array
		for (int col = 0; col < numCols; col++)
			data[0][col] = LoOrg.capitalize(colIDs[col]);

		for (int i = 0; i < numRows; i++) {
			// extract all the column strings for ith row
			NodeList colNodes = rowNodes.item(i).getChildNodes();
			for (int col = 0; col < numCols; col++)
				data[i + 1][col] = getNodeValue(colIDs[col], colNodes);
		}
		return data;
	} // end of getAllNodeValues()

	// ---------------------- String extraction --------------------------

	public static String getDocString(Document doc) {
		try {
			TransformerFactory trf = TransformerFactory.newInstance();
			Transformer tr = trf.newTransformer();

			DOMSource domSource = new DOMSource(doc);

			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);

			tr.transform(domSource, result);
			System.out.println("Extracting string from document");
			String xmlStr = sw.toString();
			return indent2Str(xmlStr);
		} catch (TransformerException ex) {
			System.out.println("Could not convert document to a string");
			return null;
		}
	} // end of getDocString()

	// ------------------------- XLS transforming ----------------------

	public static String applyXSLT(String xmlFnm, String xslFnm) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Source xslt = new StreamSource(new File(xslFnm));
			Transformer t = tf.newTransformer(xslt);

			System.out.println("Applying filter " + xslFnm + " to " + xmlFnm);
			Source text = new StreamSource(new File(xmlFnm));
			StreamResult result = new StreamResult(new StringWriter());

			t.transform(text, result);
			return result.getWriter().toString();
		} catch (Exception e) {
			System.out.println("Unable to transform " + xmlFnm + " with " + xslFnm);
			System.out.println("  " + e);
			return null;
		}
	} // end of applyXSLT()

	public static String applyXSLT2str(String xmlStr, String xslFnm) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Source xslt = new StreamSource(new File(xslFnm));
			Transformer t = tf.newTransformer(xslt);

			System.out.println("Applying the filter in " + xslFnm);
			Source text = new StreamSource(new StringReader(xmlStr));
			StreamResult result = new StreamResult(new StringWriter());

			t.transform(text, result);
			return result.getWriter().toString();
		} catch (Exception e) {
			System.out.println("Unable to transform the string");
			System.out.println("  " + e);
			// e.printStackTrace();
			return null;
		}
	} // end of applyXSLT2str()

	public static String indent(String xmlFnm) {
		return applyXSLT(xmlFnm, FileIO.getUtilsFolder() + INDENT_FNM);
	}

	public static String indent2Str(String xmlStr) {
		return applyXSLT2str(xmlStr, FileIO.getUtilsFolder() + INDENT_FNM);
	}

	// --------------------- Flat XML filter selection ----------------

	public static String getFlatFilterName(String docType)
	// return the Flat XML filter name for the doc type
	{
		if (docType == LoOrg.WRITER_STR)
			return "OpenDocument Text Flat XML";
		else if (docType == LoOrg.CALC_STR)
			return "OpenDocument Spreadsheet Flat XML";
		else if (docType == LoOrg.DRAW_STR)
			return "OpenDocument Drawing Flat XML";
		else if (docType == LoOrg.IMPRESS_STR)
			return "OpenDocument Presentation Flat XML";
		else {
			System.out.println("No Flat XML filter for this document type; using Flat text");
			return "OpenDocument Text Flat XML";
		}
	} // end of getFlatFilterName()

} // end of XML class
