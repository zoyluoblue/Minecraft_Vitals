package com.zoyluo.vitals.config;

import java.util.Objects;

public final class VitalsConfig {
	public static final int CURRENT_SCHEMA_VERSION = 1;
	public static final int MIN_DISTANCE = 8;
	public static final int MAX_DISTANCE = 64;
	public static final double MIN_SCALE = 0.65D;
	public static final double MAX_SCALE = 1.50D;
	public static final int MIN_DECIMAL_PLACES = 0;
	public static final int MAX_DECIMAL_PLACES = 2;

	public int schemaVersion = CURRENT_SCHEMA_VERSION;
	public boolean enabled = true;
	public boolean showName = true;
	public boolean showHealthNumbers = true;
	public boolean showArmor = false;
	public boolean animationEnabled = true;
	public int maxDistance = 32;
	public double scale = 1.0D;
	public int decimalPlaces = 2;

	public boolean showPlayers = true;
	public boolean showBosses = true;
	public boolean showTamed = true;
	public boolean showNeutral = true;
	public boolean showHostile = true;
	public boolean showPassive = true;
	public boolean showArmorStands = true;
	public boolean showOtherLiving = true;

	public static VitalsConfig defaults() {
		return new VitalsConfig();
	}

	public VitalsConfig copy() {
		VitalsConfig copy = new VitalsConfig();
		copy.schemaVersion = schemaVersion;
		copy.enabled = enabled;
		copy.showName = showName;
		copy.showHealthNumbers = showHealthNumbers;
		copy.showArmor = showArmor;
		copy.animationEnabled = animationEnabled;
		copy.maxDistance = maxDistance;
		copy.scale = scale;
		copy.decimalPlaces = decimalPlaces;
		copy.showPlayers = showPlayers;
		copy.showBosses = showBosses;
		copy.showTamed = showTamed;
		copy.showNeutral = showNeutral;
		copy.showHostile = showHostile;
		copy.showPassive = showPassive;
		copy.showArmorStands = showArmorStands;
		copy.showOtherLiving = showOtherLiving;
		return copy;
	}

	public VitalsConfig sanitizedCopy() {
		VitalsConfig sanitized = copy();
		sanitized.schemaVersion = CURRENT_SCHEMA_VERSION;
		sanitized.maxDistance = clamp(sanitized.maxDistance, MIN_DISTANCE, MAX_DISTANCE);
		sanitized.scale = clampFinite(sanitized.scale, 1.0D, MIN_SCALE, MAX_SCALE);
		sanitized.decimalPlaces = clamp(sanitized.decimalPlaces, MIN_DECIMAL_PLACES, MAX_DECIMAL_PLACES);
		return sanitized;
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static double clampFinite(double value, double fallback, double minimum, double maximum) {
		if (!Double.isFinite(value)) {
			return fallback;
		}
		return Math.max(minimum, Math.min(maximum, value));
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof VitalsConfig other)) {
			return false;
		}
		return schemaVersion == other.schemaVersion
				&& enabled == other.enabled
				&& showName == other.showName
				&& showHealthNumbers == other.showHealthNumbers
				&& showArmor == other.showArmor
				&& animationEnabled == other.animationEnabled
				&& maxDistance == other.maxDistance
				&& Double.compare(scale, other.scale) == 0
				&& decimalPlaces == other.decimalPlaces
				&& showPlayers == other.showPlayers
				&& showBosses == other.showBosses
				&& showTamed == other.showTamed
				&& showNeutral == other.showNeutral
				&& showHostile == other.showHostile
				&& showPassive == other.showPassive
				&& showArmorStands == other.showArmorStands
				&& showOtherLiving == other.showOtherLiving;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				schemaVersion,
				enabled,
				showName,
				showHealthNumbers,
				showArmor,
				animationEnabled,
				maxDistance,
				scale,
				decimalPlaces,
				showPlayers,
				showBosses,
				showTamed,
				showNeutral,
				showHostile,
				showPassive,
				showArmorStands,
				showOtherLiving
		);
	}
}
