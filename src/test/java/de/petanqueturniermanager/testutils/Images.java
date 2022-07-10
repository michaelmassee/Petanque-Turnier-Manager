package de.petanqueturniermanager.testutils;

import java.awt.image.BufferedImage;

// Images.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, July 2016

/*
 * A growing collection of utility functions to make Office easier to use. They are currently divided into the following groups:
 * 
 * image I/O
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.sun.star.awt.Size;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.document.XMimeTypeInfo;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
import com.sun.star.uno.Exception;

import de.petanqueturniermanager.helper.Lo;

// import com.sun.star.script.provider.XScriptContext;

public class Images {
	// private static final double PIXEL_SCALE = 26.46;
	/*
	 * assume 96 dpi, so 96 pixels == 1 inch == 25.4 mm == 2540 office 1/100 mm units s0 1 pixel == 2540/96 == 26.46 office 1/100 mm units
	 */

	public static String getBitmap(String fnm)
	// load the graphic as a bitmap, and return it as a string
	{
		try {
			XNameContainer bitmapContainer = LoOrg.createInstanceMSF(XNameContainer.class,
					"com.sun.star.drawing.BitmapTable");

			// insert image into container
			if (!FileIO.isOpenable(fnm))
				return null;
			String picURL = FileIO.fnmToURL(fnm);
			if (picURL == null)
				return null;
			bitmapContainer.insertByName(fnm, picURL);
			// use the filename as the name of the bitmap

			// return the bitmap as a string
			return (String) bitmapContainer.getByName(fnm);
		} catch (Exception e) {
			System.out.println("Could not create a bitmap container for " + fnm);
			return null;
		}
	} // end of getBitmap()

	/*
	 * public static Size getImageSize(String fnm) // return the slide dimensions of the image in 1/100 mm units { BufferedImage im = loadImage(fnm); if (im == null) return null; else { // convert pixel
	 * dimensions into slide units int w = (int)Math.round(im.getWidth()*PIXEL_SCALE); int h = (int)Math.round(im.getHeight()*PIXEL_SCALE); return new Size(w,h); } } // end of getImageSize()
	 */

	public static BufferedImage loadImage(String fnm)
	// load the image stored in fnm (uses the JDK)
	{
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(fnm));
			System.out.println("Loaded " + fnm);
			// System.out.println("Width x Height: " +
			// image.getWidth() + " x " + image.getHeight());
		} catch (java.io.IOException e) {
			System.out.println("Unable to load " + fnm);
		}
		return image;
	} // end of loadImage()

	public static void saveImage(BufferedImage im, String fnm) {
		if (im == null) {
			System.out.println("No data to save in " + fnm);
			return;
		}

		try {
			ImageIO.write(im, "png", new File(fnm));
			System.out.println("Saved image to file: " + fnm);

		} catch (java.lang.Exception e) {
			System.out.println("Could not save image to " + fnm + ": " + e);
		}
	} // end of saveImage()

	public static byte[] im2bytes(BufferedImage im) {
		byte[] bytes = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(im, "png", baos);
			baos.flush();
			bytes = baos.toByteArray();
			baos.close();
		} catch (java.io.IOException e) {
			System.out.println("Could not convert image to bytes");
		}
		return bytes;
	} // end of im2bytes()

	public static String im2String(BufferedImage im) {
		byte[] bytes = im2bytes(im);
		return Base64.getMimeEncoder().encodeToString(bytes);
	} // end of im2String()

	public static BufferedImage string2im(String s) {
		byte[] bytes = Base64.getMimeDecoder().decode(s);
		return bytes2im(bytes);
	} // end of string2im()

	public static BufferedImage bytes2im(byte[] bytes) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			return ImageIO.read(bais);
		} catch (java.io.IOException ioe) {
			System.out.println("Could not convert bytes to image");
			return null;
		}
	} // end of bytes2im()

	public static XGraphic im2graphic(BufferedImage im) {
		if (im == null) {
			System.out.println("No image found");
			return null;
		}

		String tempFnm = FileIO.createTempFile("png");
		if (tempFnm == null) {
			System.out.println("Could not create a temporary file for the image");
			return null;
		}

		Images.saveImage(im, tempFnm);
		XGraphic graphic = Images.loadGraphicFile(tempFnm);
		FileIO.deleteFile(tempFnm);
		return graphic;
	} // end of im2graphic()

	public static XGraphic loadGraphicFile(String imFnm) {
		System.out.println("Loading XGraphic from " + imFnm);
		XGraphicProvider gProvider = LoOrg.createInstanceMCF(XGraphicProvider.class,
				"com.sun.star.graphic.GraphicProvider");
		if (gProvider == null) {
			System.out.println("Graphic Provider could not be found");
			return null;
		}

		PropertyValue[] fileProps = Props.makeProps("URL", FileIO.fnmToURL(imFnm));
		try {
			return gProvider.queryGraphic(fileProps);
		} catch (Exception e) {
			System.out.println("Could not load XGraphic from " + imFnm);
			return null;
		}
	} // end of loadGraphicFile()

	public static Size getSizePixels(String imFnm) {
		XGraphic graphic = loadGraphicFile(imFnm);
		if (graphic == null)
			return null;
		return (Size) Props.getProperty(graphic, "SizePixel");
	} // end of getSizePixels()

	public static Size getSize100mm(String imFnm) {
		XGraphic graphic = loadGraphicFile(imFnm);
		if (graphic == null)
			return null;
		return (Size) Props.getProperty(graphic, "Size100thMM");
	} // end of getSize100mm()

	public static XGraphic loadGraphicLink(Object graphicLink)
	// load the graphic with the specified URL, return it as an XGraphic
	{
		XGraphicProvider gProvider = LoOrg.createInstanceMCF(XGraphicProvider.class,
				"com.sun.star.graphic.GraphicProvider");
		if (gProvider == null) {
			System.out.println("Graphic Provider could not be found");
			return null;
		}

		try {
			XPropertySet xprops = Lo.qi(XPropertySet.class, graphicLink);
			PropertyValue[] gProps = Props.makeProps("URL", xprops.getPropertyValue("GraphicURL"));

			return gProvider.queryGraphic(gProps);
		} catch (Exception e) {
			System.out.println("Unable to retrieve graphic");
			return null;
		}
	} // end of loadGraphicLink()

	public static BufferedImage graphic2Im(XGraphic graphic) {
		if (graphic == null) {
			System.out.println("No graphic found");
			return null;
		}

		String tempFnm = FileIO.createTempFile("png");
		if (tempFnm == null) {
			System.out.println("Could not create a temporary file for the graphic");
			return null;
		}

		Images.saveGraphic(graphic, tempFnm, "png");
		BufferedImage im = Images.loadImage(tempFnm);
		FileIO.deleteFile(tempFnm);
		return im;
	} // end of graphic2Im()

	public static void saveGraphic(XGraphic pic, String fnm, String imFormat)
	// save image in the specified file using the supplied image format;
	// imFormat can be be "gif", "png", "jpeg", "wmf", "bmp", "svg", etc)
	{
		System.out.println("Saving graphic in " + fnm);
		XGraphicProvider gProvider = LoOrg.createInstanceMCF(XGraphicProvider.class,
				"com.sun.star.graphic.GraphicProvider");
		if (gProvider == null) {
			System.out.println("Graphic Provider could not be found");
			return;
		}

		if (pic == null) {
			System.out.println("Supplied image is null");
			return;
		}

		PropertyValue[] pngProps = Props.makeProps("URL", FileIO.fnmToURL(fnm), "MimeType", "image/" + imFormat);
		try {
			gProvider.storeGraphic(pic, pngProps);
		} catch (com.sun.star.uno.Exception e) {
			System.out.println("Unable to save graphic");
		}
	} // end of saveGraphic()

	public static String[] getMimeTypes()
	// also see non-Office Info.getMIMEType()
	{
		// create graphics export filter
		XMimeTypeInfo mi = LoOrg.createInstanceMCF(XMimeTypeInfo.class, "com.sun.star.drawing.GraphicExportFilter");
		return mi.getSupportedMimeTypeNames();
	} // end of getMimeTypes()

	public static String changeToMime(String imFormat) {
		String[] names = getMimeTypes();
		String imf = imFormat.toLowerCase().trim();
		for (int i = 0; i < names.length; i++) {
			if (names[i].contains(imf)) {
				System.out.println("Using mime type: " + names[i]);
				return names[i];
			}
		}

		System.out.println("No matching mime type, so using image/png");
		return "image/png";
	} // end of changeToMime()

	public static Size calcScale(String fnm, int maxWidth, int maxHeight)
	/*
	 * Calculate a new size for the image in fnm that is no bigger than maxWidth x maxHeight mm's This involves a rescaling of the image so it is not distorted. The new size is returned in mm units
	 */
	{
		Size imSize = Images.getSize100mm(fnm); // in 1/100 mm units
		if (imSize == null)
			return null;
		// System.out.println("Size of image: (" + imSize.Width + ", " +
		// imSize.Height + ")");

		// calculate the scale factors to obtain these maximums
		double widthScale = ((double) maxWidth * 100) / imSize.Width;
		double heightScale = ((double) maxHeight * 100) / imSize.Height;

		// use the smallest scale factor
		double scaleFactor = (widthScale < heightScale) ? widthScale : heightScale;

		// calculate new dimensions for the image
		int w = (int) Math.round(imSize.Width * scaleFactor / 100);
		int h = (int) Math.round(imSize.Height * scaleFactor / 100);
		// System.out.println("New size of image: (" + w + ", " + h + ")");

		return new Size(w, h);
	} // end of calcScale()

} // end of Images class
