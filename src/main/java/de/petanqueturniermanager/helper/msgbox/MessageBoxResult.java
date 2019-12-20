/**
* Erstellung : 26.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.msgbox;

import com.sun.star.awt.MessageBoxResults;

public enum MessageBoxResult {

	// @formatter:off
	CANCEL	(MessageBoxResults.CANCEL),
	IGNORE	(MessageBoxResults.IGNORE),
	NO		(MessageBoxResults.NO),
	OK		(MessageBoxResults.OK),
	RETRY	(MessageBoxResults.RETRY),
	YES		(MessageBoxResults.YES);
	// @formatter:on

	private final short result;

	private MessageBoxResult(short result) {
		this.result = result;
	}

	public int getResult() {
		return this.result;
	}

	public static MessageBoxResult findResult(short result) {
		for (MessageBoxResult messageBoxResult : values()) {
			if (messageBoxResult.getResult() == result) {
				return messageBoxResult;
			}
		}
		return null;
	}

}
