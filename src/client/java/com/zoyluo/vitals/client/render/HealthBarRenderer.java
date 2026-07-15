package com.zoyluo.vitals.client.render;

import com.zoyluo.vitals.client.config.VitalsConfigManager;
import com.zoyluo.vitals.config.VitalsConfig;
import com.zoyluo.vitals.render.HealthAnimationState;
import com.zoyluo.vitals.render.HealthBarMath;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Language;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HealthBarRenderer {
	private static final int QUERY_LIMIT = 256;
	private static final int RENDER_LIMIT = 96;
	private static final int MIN_BAR_WIDTH = 86;
	private static final int MAX_BAR_WIDTH = 152;
	private static final int FRAME_COLOR = 0xE6C9A96E;
	private static final int BACKGROUND_COLOR = 0xE616181D;
	private static final int DAMAGE_TRAIL_COLOR = 0xFFF1D18A;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	private static final HealthBarAnimationCache ANIMATIONS = new HealthBarAnimationCache();
	private static long frame;
	private static long lastFrameNanos;

	private HealthBarRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(HealthBarRenderer::render);
	}

	public static void clear() {
		ANIMATIONS.clear();
		lastFrameNanos = 0L;
	}

	private static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		VitalsConfig config = VitalsConfigManager.current();
		frame++;

		if (!config.enabled
				|| client.world == null
				|| client.player == null
				|| client.options.hudHidden
				|| context.matrixStack() == null
				|| context.consumers() == null) {
			if (!config.enabled) {
				clear();
			}
			return;
		}

		double deltaSeconds = frameDeltaSeconds();
		Vec3d cameraPos = context.camera().getPos();
		double range = config.maxDistance;
		double rangeSquared = range * range;
		Box queryBox = Box.of(cameraPos, range * 2.0D, range * 2.0D, range * 2.0D);
		List<LivingEntity> candidates = new ArrayList<>(Math.min(RENDER_LIMIT, 64));
		client.world.collectEntitiesByType(
				TypeFilter.instanceOf(LivingEntity.class),
				queryBox,
				entity -> entity.squaredDistanceTo(cameraPos) <= rangeSquared
						&& EntityVisibilityPolicy.shouldRender(entity, client.player, config),
				candidates,
				QUERY_LIMIT
		);
		candidates.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(cameraPos)));

		float tickDelta = context.tickCounter().getTickDelta(false);
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		int count = Math.min(RENDER_LIMIT, candidates.size());
		for (int index = 0; index < count; index++) {
			LivingEntity entity = candidates.get(index);
			if (context.frustum() != null
					&& !dispatcher.shouldRender(entity, context.frustum(), cameraPos.x, cameraPos.y, cameraPos.z)) {
				continue;
			}
			renderEntity(context, dispatcher, entity, config, cameraPos, tickDelta, deltaSeconds);
		}

		if (frame % 30L == 0L) {
			ANIMATIONS.prune(frame);
		}
	}

	private static void renderEntity(
			WorldRenderContext context,
			EntityRenderDispatcher dispatcher,
			LivingEntity entity,
			VitalsConfig config,
			Vec3d cameraPos,
			float tickDelta,
			double deltaSeconds
	) {
		double healthRatio = HealthBarMath.ratio(entity.getHealth(), entity.getMaxHealth());
		HealthAnimationState animation = ANIMATIONS.update(
				entity.getUuid(),
				healthRatio,
				deltaSeconds,
				config.animationEnabled,
				frame
		);

		double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - cameraPos.x;
		double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - cameraPos.y;
		double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - cameraPos.z;

		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		boolean vanillaNameplateVisible = hasVanillaNameplate(entity);
		matrices.push();
		matrices.translate(x, y + entity.getHeight() + (vanillaNameplateVisible ? 0.80D : 0.45D), z);
		matrices.multiply(dispatcher.getRotation());
		float scale = (float) (0.025D * config.scale);
		matrices.scale(scale, -scale, scale);

		Text valueText = createValueText(entity, config);
		OrderedText nameText = createNameText(entity, config, textRenderer, vanillaNameplateVisible);
		int valueWidth = valueText == null ? 0 : textRenderer.getWidth(valueText);
		int nameWidth = nameText == null ? 0 : textRenderer.getWidth(nameText);
		int barWidth = MathHelper.clamp(Math.max(MIN_BAR_WIDTH, Math.max(valueWidth, nameWidth) + 10), MIN_BAR_WIDTH, MAX_BAR_WIDTH);

		drawBar(
				matrices.peek().getPositionMatrix(),
				consumers,
				barWidth,
				animation.displayedRatio(),
				animation.trailRatio(),
				healthRatio
		);
		if (nameText != null) {
			textRenderer.draw(
					nameText,
					-nameWidth / 2.0F,
					-11.0F,
					TEXT_COLOR,
					true,
					matrices.peek().getPositionMatrix(),
					consumers,
					TextRenderer.TextLayerType.NORMAL,
					0,
					LightmapTextureManager.MAX_LIGHT_COORDINATE
			);
		}
		if (valueText != null) {
			textRenderer.draw(
					valueText,
					-valueWidth / 2.0F,
					1.0F,
					TEXT_COLOR,
					true,
					matrices.peek().getPositionMatrix(),
					consumers,
					TextRenderer.TextLayerType.NORMAL,
					0,
					LightmapTextureManager.MAX_LIGHT_COORDINATE
			);
		}
		matrices.pop();
	}

	private static boolean hasVanillaNameplate(LivingEntity entity) {
		MinecraftClient client = MinecraftClient.getInstance();
		return entity.shouldRenderName() || (entity.hasCustomName() && client.targetedEntity == entity);
	}

	private static OrderedText createNameText(
			LivingEntity entity,
			VitalsConfig config,
			TextRenderer textRenderer,
			boolean vanillaNameplateVisible
	) {
		if (!config.showName || vanillaNameplateVisible) {
			return null;
		}
		StringVisitable trimmed = textRenderer.trimToWidth(entity.getDisplayName(), MAX_BAR_WIDTH - 10);
		return Language.getInstance().reorder(trimmed);
	}

	private static Text createValueText(LivingEntity entity, VitalsConfig config) {
		String current = HealthBarMath.formatValue(entity.getHealth(), config.decimalPlaces);
		String maximum = HealthBarMath.formatValue(entity.getMaxHealth(), config.decimalPlaces);
		if (config.showHealthNumbers && config.showArmor) {
			return Text.translatable("hud.vitals.health_armor", current, maximum, entity.getArmor());
		}
		if (config.showHealthNumbers) {
			return Text.translatable("hud.vitals.health", current, maximum);
		}
		if (config.showArmor) {
			return Text.translatable("hud.vitals.armor", entity.getArmor());
		}
		return null;
	}

	private static void drawBar(
			Matrix4f matrix,
			VertexConsumerProvider consumers,
			int width,
			double displayedRatio,
			double trailRatio,
			double actualRatio
	) {
		VertexConsumer vertices = consumers.getBuffer(RenderLayer.getTextBackground());
		float left = -width / 2.0F;
		float right = width / 2.0F;
		float top = 0.0F;
		float bottom = 11.0F;
		drawRect(vertices, matrix, left, top, right, bottom, FRAME_COLOR);
		drawRect(vertices, matrix, left + 1.0F, top + 1.0F, right - 1.0F, bottom - 1.0F, BACKGROUND_COLOR);

		float innerLeft = left + 2.0F;
		float innerRight = right - 2.0F;
		float innerWidth = innerRight - innerLeft;
		float trailRight = innerLeft + innerWidth * (float) HealthBarMath.clamp01(trailRatio);
		float fillRight = innerLeft + innerWidth * (float) HealthBarMath.clamp01(displayedRatio);
		if (trailRight > innerLeft) {
			drawRect(vertices, matrix, innerLeft, top + 2.0F, trailRight, bottom - 2.0F, DAMAGE_TRAIL_COLOR);
		}
		if (fillRight > innerLeft) {
			drawRect(vertices, matrix, innerLeft, top + 2.0F, fillRight, bottom - 2.0F, HealthBarMath.colorFor(actualRatio));
		}
	}

	private static void drawRect(VertexConsumer vertices, Matrix4f matrix, float left, float top, float right, float bottom, int color) {
		int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		vertices.vertex(matrix, left, top, 0.0F).color(color).light(light);
		vertices.vertex(matrix, left, bottom, 0.0F).color(color).light(light);
		vertices.vertex(matrix, right, bottom, 0.0F).color(color).light(light);
		vertices.vertex(matrix, right, top, 0.0F).color(color).light(light);
	}

	private static double frameDeltaSeconds() {
		long now = System.nanoTime();
		if (lastFrameNanos == 0L) {
			lastFrameNanos = now;
			return 1.0D / 60.0D;
		}
		double delta = (now - lastFrameNanos) / 1_000_000_000.0D;
		lastFrameNanos = now;
		return Math.max(0.0D, Math.min(0.1D, delta));
	}
}
