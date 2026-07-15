package com.zoyluo.vitals.client;

import com.zoyluo.vitals.client.config.VitalsConfigManager;
import com.zoyluo.vitals.client.input.ConfigShortcutController;
import com.zoyluo.vitals.client.render.HealthBarRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class VitalsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VitalsConfigManager.load();
		ConfigShortcutController.register();
		HealthBarRenderer.register();
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> HealthBarRenderer.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HealthBarRenderer.clear());
	}
}
