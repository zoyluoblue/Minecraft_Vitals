package com.zoyluo.vitals.render;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class HealthBarMath {
	public static final int HEALTHY_COLOR = 0xFF45B85A;
	public static final int WARNING_COLOR = 0xFFD9A441;
	public static final int CRITICAL_COLOR = 0xFFD65050;

	private HealthBarMath() {
	}

	public static double ratio(double health, double maximumHealth) {
		if (!Double.isFinite(health) || !Double.isFinite(maximumHealth) || maximumHealth <= 0.0D) {
			return 0.0D;
		}
		return clamp01(health / maximumHealth);
	}

	public static double approach(double current, double target, double deltaSeconds, double settleSeconds) {
		current = clamp01(current);
		target = clamp01(target);
		if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0D) {
			return current;
		}
		if (!Double.isFinite(settleSeconds) || settleSeconds <= 0.0D) {
			return target;
		}

		double clampedDelta = Math.min(deltaSeconds, 0.1D);
		double responseRate = -Math.log(0.05D) / settleSeconds;
		double result = target + (current - target) * Math.exp(-responseRate * clampedDelta);
		if (Math.abs(result - target) < 0.0001D) {
			return target;
		}
		return clamp01(result);
	}

	public static String formatValue(double value, int decimalPlaces) {
		if (!Double.isFinite(value)) {
			return "0";
		}
		int precision = Math.max(0, Math.min(2, decimalPlaces));
		BigDecimal decimal = BigDecimal.valueOf(Math.max(0.0D, value))
				.setScale(precision, RoundingMode.HALF_UP)
				.stripTrailingZeros();
		return decimal.toPlainString();
	}

	public static int colorFor(double ratio) {
		double normalized = clamp01(ratio);
		if (normalized <= 0.25D) {
			return CRITICAL_COLOR;
		}
		if (normalized <= 0.50D) {
			return WARNING_COLOR;
		}
		return HEALTHY_COLOR;
	}

	public static double clamp01(double value) {
		if (!Double.isFinite(value)) {
			return 0.0D;
		}
		return Math.max(0.0D, Math.min(1.0D, value));
	}
}
