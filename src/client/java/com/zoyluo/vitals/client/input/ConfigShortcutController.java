package com.zoyluo.vitals.client.input;

import com.zoyluo.vitals.client.screen.VitalsConfigScreen;
import com.zoyluo.vitals.config.ConfigShortcutState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ConfigShortcutController {
	private static final ConfigShortcutState STATE = new ConfigShortcutState();

	private ConfigShortcutController() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ConfigShortcutController::tick);
	}

	private static void tick(MinecraftClient client) {
		if (client.getWindow() == null) {
			STATE.reset();
			return;
		}

		boolean focused = client.isWindowFocused();
		long handle = client.getWindow().getHandle();
		boolean leftAltDown = focused && InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_ALT);
		boolean shortcutKeyDown = focused && InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_N);
		boolean gameReady = client.player != null && client.world != null;

		if (STATE.update(leftAltDown, shortcutKeyDown, client.currentScreen != null, gameReady, focused)) {
			client.setScreen(new VitalsConfigScreen(null));
		}
	}
}
