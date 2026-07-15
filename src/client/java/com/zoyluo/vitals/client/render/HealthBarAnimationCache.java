package com.zoyluo.vitals.client.render;

import com.zoyluo.vitals.render.HealthAnimationState;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class HealthBarAnimationCache {
	private static final int MAX_ENTRIES = 128;
	private static final long STALE_AFTER_FRAMES = 120L;

	private final LinkedHashMap<UUID, Entry> entries = new LinkedHashMap<>(32, 0.75F, true);

	HealthAnimationState update(UUID entityId, double targetRatio, double deltaSeconds, boolean animationEnabled, long frame) {
		Entry entry = entries.get(entityId);
		if (entry == null) {
			while (entries.size() >= MAX_ENTRIES) {
				Iterator<UUID> iterator = entries.keySet().iterator();
				if (!iterator.hasNext()) {
					break;
				}
				iterator.next();
				iterator.remove();
			}
			entry = new Entry(new HealthAnimationState(), frame);
			entries.put(entityId, entry);
		}
		entry.lastSeenFrame = frame;
		entry.state.update(targetRatio, deltaSeconds, animationEnabled);
		return entry.state;
	}

	void prune(long frame) {
		entries.entrySet().removeIf(entry -> frame - entry.getValue().lastSeenFrame > STALE_AFTER_FRAMES);
	}

	void clear() {
		entries.clear();
	}

	private static final class Entry {
		private final HealthAnimationState state;
		private long lastSeenFrame;

		private Entry(HealthAnimationState state, long lastSeenFrame) {
			this.state = state;
			this.lastSeenFrame = lastSeenFrame;
		}
	}
}
