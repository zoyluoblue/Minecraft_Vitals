package com.zoyluo.vitals.client.render;

import com.zoyluo.vitals.config.VitalsConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class EntityVisibilityPolicy {
	private EntityVisibilityPolicy() {
	}

	public static boolean shouldRender(LivingEntity entity, PlayerEntity viewer, VitalsConfig config) {
		if (entity == viewer
				|| entity instanceof ArmorStandEntity
				|| entity.isRemoved()
				|| !entity.isAlive()
				|| entity.isSpectator()
				|| entity.isInvisible()
				|| !Float.isFinite(entity.getHealth())
				|| !Float.isFinite(entity.getMaxHealth())
				|| entity.getMaxHealth() <= 0.0F) {
			return false;
		}

		return switch (classify(entity)) {
			case ARMOR_STAND -> false;
			case PLAYER -> config.showPlayers;
			case BOSS -> config.showBosses;
			case TAMED -> config.showTamed;
			case NEUTRAL -> config.showNeutral;
			case HOSTILE -> config.showHostile;
			case PASSIVE -> config.showPassive;
			case OTHER -> config.showOtherLiving;
		};
	}

	static Category classify(LivingEntity entity) {
		if (entity instanceof ArmorStandEntity) {
			return Category.ARMOR_STAND;
		}
		if (entity instanceof PlayerEntity) {
			return Category.PLAYER;
		}
		if (entity instanceof WitherEntity || entity instanceof EnderDragonEntity) {
			return Category.BOSS;
		}
		if (isTamed(entity)) {
			return Category.TAMED;
		}
		if (entity instanceof Angerable) {
			return Category.NEUTRAL;
		}
		if (entity instanceof HostileEntity) {
			return Category.HOSTILE;
		}
		if (entity instanceof PassiveEntity) {
			return Category.PASSIVE;
		}
		return Category.OTHER;
	}

	private static boolean isTamed(LivingEntity entity) {
		if (entity instanceof TameableEntity tameable && tameable.isTamed()) {
			return true;
		}
		if (entity instanceof AbstractHorseEntity horse && horse.isTame()) {
			return true;
		}
		return entity instanceof Tameable tameable && tameable.getOwnerUuid() != null;
	}

	enum Category {
		ARMOR_STAND,
		PLAYER,
		BOSS,
		TAMED,
		NEUTRAL,
		HOSTILE,
		PASSIVE,
		OTHER
	}
}
