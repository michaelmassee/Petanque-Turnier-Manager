/**
 * Erstellung 09.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Frame;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.FrameActionEvent;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrameActionListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;

/**
 * @author Michael Massee
 *
 */
public class DialogTools {

	private static final int X_OFFSET = 50;
	private static final int Y_OFFSET = 50;

	private final XComponentContext xContext;
	private final Frame frame;

	private DialogTools(XComponentContext xContext, Frame frame) {
		this.xContext = checkNotNull(xContext);
		this.frame = checkNotNull(frame);
		addFrameActionListener();
	}

	private void addFrameActionListener() {
		XFrame currentFrame = DocumentHelper.getCurrentFrame(xContext);
		if (currentFrame != null) {
			currentFrame.addFrameActionListener(new XFrameActionListener() {
				@Override
				public void disposing(EventObject arg0) {
					// when parent close then close this frame
					if (null != getFrame()) {
						getFrame().dispose();
					}
				}

				@Override
				public void frameAction(FrameActionEvent arg0) {
					// nichts
				}
			});
		}
	}

	public static final DialogTools from(XComponentContext xContext, Frame frame) {
		return new DialogTools(xContext, frame);
	}

	public DialogTools moveInsideTopWindow() {
		XFrame currentFrame = DocumentHelper.getCurrentFrame(xContext);
		if (currentFrame != null) {
			XWindow containerWindow = currentFrame.getContainerWindow();
			Rectangle posSize = containerWindow.getPosSize();

			int state = frame.getExtendedState();
			if (Frame.NORMAL != state) {
				frame.setExtendedState(Frame.NORMAL);
			}

			int newXPos = frame.getX();
			int newYPos = frame.getY();
			if (newXPos < posSize.X || newXPos > (posSize.X + posSize.Width)) {
				newXPos = posSize.X + X_OFFSET;
			}

			if (newYPos < posSize.Y || newYPos > (posSize.Y + posSize.Height)) {
				newYPos = posSize.Y + Y_OFFSET;
			}

			if (frame.getX() != newXPos || frame.getY() != newYPos) {
				frame.setLocation(newXPos, newYPos);
			}
		}
		return this;
	}

	/**
	 * @return the frame
	 */
	public final Frame getFrame() {
		return frame;
	}

}
