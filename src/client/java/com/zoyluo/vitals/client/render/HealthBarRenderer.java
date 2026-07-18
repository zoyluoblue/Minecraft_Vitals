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
	private static final int ARMOR_FILL_COLOR = 0xFFFFD45B;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final float HEALTH_BAR_TOP = 0.0F;
	private static final float ARMOR_BAR_TOP = -12.0F;

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

		Text healthText = createHealthText(entity, config);
		Text armorText = createArmorText(entity, config);
		boolean armorBarVisible = armorText != null;
		OrderedText nameText = createNameText(entity, config, textRenderer, vanillaNameplateVisible);
		int healthWidth = healthText == null ? 0 : textRenderer.getWidth(healthText);
		int armorWidth = armorText == null ? 0 : textRenderer.getWidth(armorText);
		int nameWidth = nameText == null ? 0 : textRenderer.getWidth(nameText);
		int barWidth = MathHelper.clamp(
				Math.max(MIN_BAR_WIDTH, Math.max(Math.max(healthWidth, armorWidth), nameWidth) + 10),
				MIN_BAR_WIDTH,
				MAX_BAR_WIDTH
		);

		drawBar(
				matrices.peek().getPositionMatrix(),
				consumers,
				barWidth,
				HEALTH_BAR_TOP,
				animation.displayedRatio(),
				animation.trailRatio(),
				HealthBarMath.colorFor(healthRatio),
				DAMAGE_TRAIL_COLOR
		);
		if (armorBarVisible) {
			double armorRatio = HealthBarMath.armorRatio(entity.getArmor());
			drawBar(
					matrices.peek().getPositionMatrix(),
					consumers,
					barWidth,
					ARMOR_BAR_TOP,
					armorRatio,
					armorRatio,
					ARMOR_FILL_COLOR,
					ARMOR_FILL_COLOR
			);
		}
		if (nameText != null) {
			textRenderer.draw(
					nameText,
					-nameWidth / 2.0F,
					armorBarVisible ? ARMOR_BAR_TOP - 10.0F : -11.0F,
					TEXT_COLOR,
					true,
					matrices.peek().getPositionMatrix(),
					consumers,
					TextRenderer.TextLayerType.NORMAL,
					0,
					LightmapTextureManager.MAX_LIGHT_COORDINATE
			);
		}
		if (armorText != null) {
			textRenderer.draw(
					armorText,
					-armorWidth / 2.0F,
					ARMOR_BAR_TOP + 1.0F,
					TEXT_COLOR,
					true,
					matrices.peek().getPositionMatrix(),
					consumers,
					TextRenderer.TextLayerType.NORMAL,
					0,
					LightmapTextureManager.MAX_LIGHT_COORDINATE
			);
		}
		if (healthText != null) {
			textRenderer.draw(
					healthText,
					-healthWidth / 2.0F,
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

	private static Text createHealthText(LivingEntity entity, VitalsConfig config) {
		if (!config.showHealthNumbers) {
			return null;
		}
		String current = HealthBarMath.formatValue(entity.getHealth(), config.decimalPlaces);
		String maximum = HealthBarMath.formatValue(entity.getMaxHealth(), config.decimalPlaces);
		return Text.translatable("hud.vitals.health", current, maximum);
	}

	private static Text createArmorText(LivingEntity entity, VitalsConfig config) {
		if (!config.showArmor || entity.getArmor() <= 0) {
			return null;
		}
		return Text.translatable("hud.vitals.armor", entity.getArmor());
	}

	private static void drawBar(
			Matrix4f matrix,
			VertexConsumerProvider consumers,
			int width,
			float top,
			double displayedRatio,
			double trailRatio,
			int fillColor,
			int trailColor
	) {
		VertexConsumer vertices = consumers.getBuffer(RenderLayer.getTextBackground());
		float left = -width / 2.0F;
		float right = width / 2.0F;
		float bottom = top + 11.0F;
		float frameLeft = left + 1.0F;
		float frameRight = right - 1.0F;
		float frameTop = top + 1.0F;
		float frameBottom = bottom - 1.0F;
		float innerLeft = left + 2.0F;
		float innerRight = right - 2.0F;
		float innerTop = top + 2.0F;
		float innerBottom = bottom - 2.0F;
		float innerWidth = innerRight - innerLeft;
		float fillRight = innerLeft + innerWidth * (float) HealthBarMath.clamp01(displayedRatio);
		float trailRight = Math.max(fillRight, innerLeft + innerWidth * (float) HealthBarMath.clamp01(trailRatio));

		// text_background is a translucent, quad-sorted layer. These regions must never overlap,
		// otherwise coplanar frame/fill/trail quads can be reordered between frames and flicker.
		drawRect(vertices, matrix, left, top, right, frameTop, FRAME_COLOR);
		drawRect(vertices, matrix, left, frameBottom, right, bottom, FRAME_COLOR);
		drawRect(vertices, matrix, left, frameTop, frameLeft, frameBottom, FRAME_COLOR);
		drawRect(vertices, matrix, frameRight, frameTop, right, frameBottom, FRAME_COLOR);

		drawRect(vertices, matrix, frameLeft, frameTop, frameRight, innerTop, BACKGROUND_COLOR);
		drawRect(vertices, matrix, frameLeft, innerBottom, frameRight, frameBottom, BACKGROUND_COLOR);
		drawRect(vertices, matrix, frameLeft, innerTop, innerLeft, innerBottom, BACKGROUND_COLOR);
		drawRect(vertices, matrix, innerRight, innerTop, frameRight, innerBottom, BACKGROUND_COLOR);

		if (trailRight > innerLeft) {
			if (fillRight > innerLeft) {
				drawRect(vertices, matrix, innerLeft, innerTop, fillRight, innerBottom, fillColor);
			}
			if (trailRight > fillRight) {
				drawRect(vertices, matrix, fillRight, innerTop, trailRight, innerBottom, trailColor);
			}
		}
		if (trailRight < innerRight) {
			drawRect(vertices, matrix, trailRight, innerTop, innerRight, innerBottom, BACKGROUND_COLOR);
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
