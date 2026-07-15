package com.zoyluo.vitals;

import com.zoyluo.vitals.config.ConfigShortcutState;
import com.zoyluo.vitals.config.VitalsConfig;
import com.zoyluo.vitals.render.HealthAnimationState;
import com.zoyluo.vitals.render.HealthBarMath;

public final class VitalsLogicChecks {
	private VitalsLogicChecks() {
	}

	public static void main(String[] args) {
		checkConfigSanitization();
		checkFormattingAndColors();
		checkShortcutEdges();
		checkAnimation();
		System.out.println("Vitals logic checks passed.");
	}

	private static void checkConfigSanitization() {
		VitalsConfig input = VitalsConfig.defaults();
		input.schemaVersion = -4;
		input.maxDistance = 1_000;
		input.scale = Double.NaN;
		input.decimalPlaces = -8;

		VitalsConfig sanitized = input.sanitizedCopy();
		require(sanitized.schemaVersion == VitalsConfig.CURRENT_SCHEMA_VERSION, "schema version must normalize");
		require(sanitized.maxDistance == VitalsConfig.MAX_DISTANCE, "distance must clamp high");
		require(sanitized.scale == 1.0D, "non-finite scale must use default");
		require(sanitized.decimalPlaces == VitalsConfig.MIN_DECIMAL_PLACES, "precision must clamp low");
		require(input.schemaVersion == -4, "sanitization must not mutate the source object");
	}

	private static void checkFormattingAndColors() {
		require(HealthBarMath.ratio(5.0D, 20.0D) == 0.25D, "health ratio");
		require(HealthBarMath.ratio(25.0D, 20.0D) == 1.0D, "health ratio upper clamp");
		require(HealthBarMath.ratio(1.0D, 0.0D) == 0.0D, "invalid maximum health");
		require("20".equals(HealthBarMath.formatValue(20.0D, 2)), "whole numbers must trim zeros");
		require("19.88".equals(HealthBarMath.formatValue(19.875D, 2)), "precise health rounding");
		require(HealthBarMath.colorFor(0.75D) == HealthBarMath.HEALTHY_COLOR, "healthy color");
		require(HealthBarMath.colorFor(0.40D) == HealthBarMath.WARNING_COLOR, "warning color");
		require(HealthBarMath.colorFor(0.10D) == HealthBarMath.CRITICAL_COLOR, "critical color");
	}

	private static void checkShortcutEdges() {
		ConfigShortcutState state = new ConfigShortcutState();
		require(!state.update(false, true, false, true, true), "V alone must not open");
		require(state.update(true, true, false, true, true), "left Alt plus V must open on rising edge");
		require(!state.update(true, true, false, true, true), "holding the chord must not repeat");
		require(!state.update(false, false, false, true, true), "release must not open");
		require(!state.update(true, true, true, true, true), "an existing screen must block opening");
		require(!state.update(true, true, false, true, true), "closing while held must not reopen");
		state.update(false, false, false, true, true);
		require(!state.update(true, true, false, true, false), "an unfocused window must not open");
		require(state.update(true, true, false, true, true), "refocused chord may open after unfocused reset");
	}

	private static void checkAnimation() {
		HealthAnimationState state = new HealthAnimationState();
		state.update(1.0D, 1.0D / 60.0D, true);
		require(state.displayedRatio() == 1.0D, "first appearance must not animate from zero");

		state.update(0.25D, 1.0D / 60.0D, true);
		require(state.displayedRatio() < 1.0D && state.displayedRatio() > 0.25D, "damage must animate smoothly");
		require(state.trailRatio() >= state.displayedRatio(), "damage trail must stay behind the foreground");
		for (int index = 0; index < 180; index++) {
			state.update(0.25D, 1.0D / 60.0D, true);
		}
		require(Math.abs(state.displayedRatio() - 0.25D) < 0.001D, "foreground must converge");
		require(Math.abs(state.trailRatio() - 0.25D) < 0.001D, "trail must converge");

		state.update(0.80D, 1.0D / 60.0D, false);
		require(state.displayedRatio() == 0.80D && state.trailRatio() == 0.80D, "disabled animation must snap");
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
