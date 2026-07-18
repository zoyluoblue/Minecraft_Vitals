package com.zoyluo.vitals.config;

public final class ConfigShortcutState {
	private boolean chordWasDown;

	public boolean update(
			boolean leftAltDown,
			boolean shortcutKeyDown,
			boolean screenOpen,
			boolean gameReady,
			boolean windowFocused
	) {
		boolean chordDown = windowFocused && leftAltDown && shortcutKeyDown;
		boolean shouldOpen = chordDown && !chordWasDown && !screenOpen && gameReady;
		chordWasDown = chordDown;
		return shouldOpen;
	}

	public void reset() {
		chordWasDown = false;
	}
}
