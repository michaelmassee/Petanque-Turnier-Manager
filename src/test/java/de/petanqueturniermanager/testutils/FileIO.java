package de.petanqueturniermanager.testutils;

// FileIO.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, March 2015

/*
 * A growing collection of utility functions to make Office easier to use. They are currently divided into the following groups:
 * 
 * File IO file creation / deletion saving/writing to a file zip access
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.zip.ZipFile;

import com.sun.star.container.XNameAccess;
import com.sun.star.io.XActiveDataSink;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XTextInputStream;
import com.sun.star.packages.zip.XZipFileAccess;
import com.sun.star.ucb.XSimpleFileAccess3;

import de.petanqueturniermanager.helper.Lo;

public class FileIO {

	public static String getUtilsFolder()
	// http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
	{
		try {
			return FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			System.out.println(e);
			return null;
		}
	} // end of getUtilsFolder()

	public static String getAbsolutePath(String fnm) {
		return new File(fnm).getAbsolutePath();
	}

	public static String urlToPath(String url) {
		try {
			return Paths.get(new URL(url).toURI()).toString();
		} catch (java.lang.Exception e) {
			System.out.println("Could not parse " + url);
			return null;
		}
	} // end of urlToPath()

	public static boolean isOpenable(String fnm)
	// convert a file path to URL format
	{
		File f = new File(fnm);
		if (!f.exists()) {
			System.out.println(fnm + " does not exist");
			return false;
		}
		if (!f.isFile()) {
			System.out.println(fnm + " is not a file");
			return false;
		}
		if (!f.canRead()) {
			System.out.println(fnm + " is not readable");
			return false;
		}
		return true;
	} // end of isOpenable()

	public static String fnmToURL(String fnm)
	// convert a file path to URL format
	{
		try {
			StringBuffer sb = null;
			String path = new File(fnm).getCanonicalPath();
			sb = new StringBuffer("file:///");
			sb.append(path.replace('\\', '/'));
			return sb.toString();
		} catch (java.io.IOException e) {
			System.out.println("Could not access " + fnm);
			return null;
		}
	} // end of fnmToURL()

	public static String URI2Path(String URIfnm) {
		try {
			File file = new File(new URI(URIfnm).getSchemeSpecificPart());
			return file.getCanonicalPath();
			// e.g. C:\Program Files (x86)\LibreOffice 4\program\addin
		} catch (java.lang.Exception e) {
			System.out.println("Could not translate settings path");
			return URIfnm;
		}
	} // end of URI2Path()

	public static boolean makeDirectory(String dir) {
		File file = new File(dir);
		if (!file.exists()) {
			if (file.mkdir()) {
				System.out.println("Created " + dir);
				return true;
			} else {
				System.out.println("Could not create " + dir);
				return false;
			}
		} else {
			// System.out.println(dir + " already exists");
			return true;
		}
	} // end of makeDirectory()

	public static String[] getFileNames(String dir) {
		File[] files = new File(dir).listFiles();
		if (files == null) {
			System.out.println("No directory found called " + dir);
			return null;
		}

		ArrayList<String> results = new ArrayList<String>();
		for (File file : files) {
			if (file.isFile())
				results.add(file.getName());
		}

		int numFiles = results.size();
		if (numFiles == 0) {
			System.out.println("No files found in the directory " + dir);
			return null;
		}

		String[] fnms = new String[numFiles];
		for (int i = 0; i < numFiles; i++)
			fnms[i] = results.get(i);
		return fnms;
	} // end of getFileNames()

	public static String getFnm(String path) {
		return (new File(path)).getName();
	}

	// ---- ------------- file creation / deletion --------------

	public static String createTempFile(String imFormat) {
		try {
			File temp = File.createTempFile("loTemp", "." + imFormat);
			temp.deleteOnExit(); // should delete file at JVM exit
			return temp.getAbsolutePath();
		} catch (java.io.IOException e) {
			System.out.println("Could not create temp file");
			return null;
		}
	} // end of createTempFile();

	public static void deleteFiles(ArrayList<String> dbFnms) {
		System.out.println();
		for (int i = 0; i < dbFnms.size(); i++)
			deleteFile(dbFnms.get(i));
	} // end of deleteFiles()

	public static void deleteFile(String fnm) {
		File file = new File(fnm);
		if (file.delete())
			System.out.println(fnm + " deleted");
		else
			System.out.println(fnm + "could not be deleted");
	} // end of deleteFile()

	// ---------------- saving/writing to a file --------------------

	public static void saveString(String fnm, String s) {
		if (s == null) {
			System.out.println("No data to save in " + fnm);
			return;
		}

		try {
			FileWriter fw = new FileWriter(new File(fnm));
			fw.write(s);
			fw.close();
			System.out.println("Saved string to file: " + fnm);
		} catch (java.io.IOException ex) {
			System.out.println("Could not save string to file: " + fnm);
		}
	} // end of saveString()

	public static void saveBytes(String fnm, byte[] bytes) {
		if (bytes == null) {
			System.out.println("No data to save in " + fnm);
			return;
		}

		try {
			FileOutputStream fos = new FileOutputStream(fnm);
			fos.write(bytes);
			fos.close();
			System.out.println("Saved bytes to file: " + fnm);
		} catch (java.io.IOException ex) {
			System.out.println("Could not save bytes to file: " + fnm);
		}
	} // end of saveBytes()

	public static void saveArray(String fnm, Object[][] arr) {
		if (arr == null) {
			System.out.println("No data to save in " + fnm);
			return;
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fnm));
			int numCols = arr[0].length;
			int numRows = arr.length;
			for (int j = 0; j < numRows; j++) {
				for (int i = 0; i < numCols; i++)
					bw.write((String) arr[j][i] + "\t");
				bw.write("\n");
			}
			bw.close();
			System.out.println("Saved array to file: " + fnm);
		} catch (java.io.IOException ex) {
			System.out.println("Could not save array to file: " + fnm);
		}
	} // end of saveArray()

	public static void saveArray(String fnm, double[][] arr)
	// repeated code but for saving a doubles array
	{
		if (arr == null) {
			System.out.println("No data to save in " + fnm);
			return;
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fnm));
			int numCols = arr[0].length;
			int numRows = arr.length;
			for (int j = 0; j < numRows; j++) {
				for (int i = 0; i < numCols; i++)
					bw.write(arr[j][i] + "\t");
				bw.write("\n");
			}
			bw.close();
			System.out.println("Saved array to file: " + fnm);
		} catch (java.io.IOException ex) {
			System.out.println("Could not save array to file: " + fnm);
		}
	} // end of saveArray()

	/*
	 * public static boolean saveFile(String fnm, byte[] data) { String openFileURL = FileIO.fnmToURL(fnm); boolean res = false; try { XSimpleFileAccess sfa =
	 * LoOrg.createInstanceMCF(XSimpleFileAccess.class, "com.sun.star.comp.ucb.SimpleFileAccess"); System.out.println("sf 0"); XOutputStream os = sfa.openFileWrite(openFileURL);
	 * System.out.println("sf 1"); os.writeBytes(data); os.flush(); os.closeOutput(); res = true; } catch (java.lang.Exception e) { System.out.println(e); } return res; } // end of saveFile
	 */

	public static void appendTo(String fnm, String msg) {
		try {
			FileWriter fw = new FileWriter(fnm, true);
			fw.write(msg + "\n");
			fw.close();
		} catch (java.io.IOException e) {
			System.out.println("Unable to append to " + fnm);
		}
	} // end of appendTo()

	// ----------------------- zip access ---------------------------------------

	public static XZipFileAccess zipAccess(String fnm)
	// get zip access to the document using Office API
	{
		return LoOrg.createInstanceMCF(XZipFileAccess.class, "com.sun.star.packages.zip.ZipFileAccess",
				new Object[] { fnmToURL(fnm) });
	}

	public static void zipListUno(String fnm)
	// replaced by more detailed Java version; see below
	{
		XZipFileAccess zfa = zipAccess(fnm);
		XNameAccess nmAccess = Lo.qi(XNameAccess.class, zfa);
		String[] names = nmAccess.getElementNames();

		System.out.println("\nZipped Contents of " + fnm);
		LoOrg.printNames(names, 1);
	} // end of zipListUno()

	public static void unzipFile(XZipFileAccess zfa, String fnm) {
		String fileName = Info.getName(fnm);
		String ext = Info.getExt(fnm);
		try {
			System.out.println("Extracting " + fnm);
			XInputStream inStream = zfa.getStreamByPattern("*" + fnm);

			XSimpleFileAccess3 fileAcc = LoOrg.createInstanceMCF(XSimpleFileAccess3.class,
					"com.sun.star.ucb.SimpleFileAccess");

			String copyFnm = (ext == null) ? (fileName + "Copy") : (fileName + "Copy." + ext);
			System.out.println("Saving to " + copyFnm);
			fileAcc.writeFile(FileIO.fnmToURL(copyFnm), inStream);
		} catch (com.sun.star.uno.Exception e) {
			System.out.println(e);
		}
	} // end of unzipFile()

	public static String getMimeType(XZipFileAccess zfa)
	// return the contents of mimetype
	// also see Info.getMIMEType() for use of MimetypesFileTypeMap
	{
		try {
			// System.out.println("Extracting mime.type");
			XInputStream inStream = zfa.getStreamByPattern("mimetype");
			String[] lines = FileIO.readLines(inStream);
			if (lines != null)
				// System.out.println(" " + lines[0]);
				return lines[0].trim();
		} catch (com.sun.star.uno.Exception e) {
			System.out.println(e);
		}

		System.out.println("No mimetype found");
		return null;
	} // end of getMimeType()

	public static String[] readLines(XInputStream is)
	// return the contents of an Office input stream as an array of lines
	{
		String[] linesArr = null;
		ArrayList<String> lines = new ArrayList<String>();

		try {
			XTextInputStream tis = LoOrg.createInstanceMCF(XTextInputStream.class, "com.sun.star.io.TextInputStream");
			XActiveDataSink sink = Lo.qi(XActiveDataSink.class, tis);
			sink.setInputStream(is);

			while (!tis.isEOF())
				lines.add(tis.readLine());
			tis.closeInput();

			linesArr = new String[lines.size()];
			lines.toArray(linesArr);
		} catch (Exception e) {
			System.out.println(e);
		}

		return linesArr;
	} // end of readLines()

	// ----------------- switch to Java's zip APIs ------------

	public static void zipList(String fnm)
	// from http://www.drdobbs.com/jvm/java-and-the-zip-file-format/184410339
	{
		DateFormat df = DateFormat.getDateInstance(); // date format
		DateFormat tf = DateFormat.getTimeInstance(); // time format
		tf.setTimeZone(TimeZone.getDefault());
		try {
			ZipFile zfile = new ZipFile(fnm);
			System.out.println("Listing of " + zfile.getName() + ":");
			System.out.println("Raw Size    Size     Date        Time         Name");
			System.out.println("--------  -------  -------      -------      --------");
			Enumeration<? extends java.util.zip.ZipEntry> zfs = zfile.entries();
			while (zfs.hasMoreElements()) {
				java.util.zip.ZipEntry entry = zfs.nextElement();
				Date d = new Date(entry.getTime());
				System.out.print(padSpaces(entry.getSize(), 9) + " ");
				System.out.print(padSpaces(entry.getCompressedSize(), 7) + " ");
				System.out.print(" " + df.format(d) + " ");
				System.out.print(" " + tf.format(d) + "  ");
				System.out.println(" " + entry.getName());
			}
			System.out.println();
		} catch (java.io.IOException e) {
			System.out.println(e);
		}
	} // end of zipList()

	private static String padSpaces(long l, int width)
	// Used to print a long integer using a specific width
	{
		String s = new Long(l).toString();
		while (s.length() < width)
			s += " ";
		return s;
	} // end of padSpaces()

} // end of FileIO class
