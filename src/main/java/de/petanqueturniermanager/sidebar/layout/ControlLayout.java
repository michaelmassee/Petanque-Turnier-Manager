package de.petanqueturniermanager.sidebar.layout;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.UnoRuntime;

/**
 * Ein Layout, dass nur ein Control enthalten kann.
 *
 * Als Höhe wird immer die Höhe des XControl verwendet. Sie ist damit statisch.
 *
 * @author daniel.sikeler
 */
public class ControlLayout implements Layout {
	private XWindow control;

	private int height;
	private int fixWidth; // wenn 0 dann von Manager

	private ControlLayout(XWindow control, int fixWidth) {
		this.control = control;
		height = control.getPosSize().Height;
		this.fixWidth = fixWidth;
	}

	public ControlLayout(XControl control) {
		this(UnoRuntime.queryInterface(XWindow.class, control), 0);
	}

	public ControlLayout(XControl control, int fixWidth) {
		this(UnoRuntime.queryInterface(XWindow.class, control), fixWidth);
	}

	@Override
	public int layout(Rectangle rect) {
		control.setPosSize(rect.X, rect.Y, (fixWidth > 0) ? fixWidth : rect.Width, height, PosSize.POSSIZE);
		return height;
	}

	/**
	 * Diese Operation ist nicht erlaubt.
	 */
	@Override
	public void addLayout(Layout layout, int space) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getHeight() {
		return height;
	}

	public final int getFixWidth() {
		return fixWidth;
	}

}
