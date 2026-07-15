package com.zoyluo.vitals.render;

public final class HealthAnimationState {
	private static final double HEALTH_SETTLE_SECONDS = 0.18D;
	private static final double DAMAGE_TRAIL_HOLD_SECONDS = 0.25D;
	private static final double DAMAGE_TRAIL_SETTLE_SECONDS = 0.35D;

	private boolean initialized;
	private double targetRatio;
	private double displayedRatio;
	private double trailRatio;
	private double trailHoldSeconds;

	public void update(double target, double deltaSeconds, boolean animationEnabled) {
		target = HealthBarMath.clamp01(target);
		if (!initialized) {
			initialized = true;
			targetRatio = target;
			displayedRatio = target;
			trailRatio = target;
			return;
		}

		if (!animationEnabled) {
			targetRatio = target;
			displayedRatio = target;
			trailRatio = target;
			trailHoldSeconds = 0.0D;
			return;
		}

		double delta = Double.isFinite(deltaSeconds) ? Math.max(0.0D, Math.min(0.1D, deltaSeconds)) : 0.0D;
		if (target < targetRatio) {
			trailRatio = Math.max(trailRatio, displayedRatio);
			trailHoldSeconds = DAMAGE_TRAIL_HOLD_SECONDS;
		} else if (target > targetRatio) {
			trailRatio = Math.max(trailRatio, target);
		}
		targetRatio = target;

		displayedRatio = HealthBarMath.approach(displayedRatio, targetRatio, delta, HEALTH_SETTLE_SECONDS);
		if (trailHoldSeconds > 0.0D) {
			trailHoldSeconds = Math.max(0.0D, trailHoldSeconds - delta);
		} else {
			trailRatio = HealthBarMath.approach(trailRatio, targetRatio, delta, DAMAGE_TRAIL_SETTLE_SECONDS);
		}
		trailRatio = Math.max(displayedRatio, trailRatio);
	}

	public double displayedRatio() {
		return displayedRatio;
	}

	public double trailRatio() {
		return trailRatio;
	}
}
