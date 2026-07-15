package com.zoyluo.vitals.client.screen;

import com.zoyluo.vitals.client.config.VitalsConfigManager;
import com.zoyluo.vitals.config.VitalsConfig;
import com.zoyluo.vitals.render.HealthBarMath;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public final class VitalsConfigScreen extends Screen {
	private static final int PANEL_BACKGROUND = 0xF014101A;
	private static final int PANEL_INNER = 0xE51C1724;
	private static final int PANEL_HEADER_TOP = 0xF02B2037;
	private static final int PANEL_HEADER_BOTTOM = 0xF01A1422;
	private static final int GOLD = 0xFFC9A96E;
	private static final int MUTED_TEXT = 0xFFB9AFBF;
	private static final int SUCCESS_TEXT = 0xFF70D48B;
	private static final int ERROR_TEXT = 0xFFF07B7B;
	private static final int PREVIEW_BACKGROUND = 0xFF16181D;
	private static final int PREVIEW_TRAIL = 0xFFF1D18A;

	private final Screen parent;
	private final List<ClickableWidget> dependentWidgets = new ArrayList<>();
	private VitalsConfig working;
	private Page page = Page.DISPLAY;
	private Text status;
	private boolean statusIsError;
	private ButtonWidget applyButton;

	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;
	private int contentLeft;
	private int contentWidth;
	private int previewTop;
	private boolean showPreview;

	public VitalsConfigScreen(Screen parent) {
		super(Text.translatable("screen.vitals.title"));
		this.parent = parent;
		this.working = VitalsConfigManager.current().copy();
	}

	@Override
	protected void init() {
		dependentWidgets.clear();
		panelWidth = Math.min(560, width - 16);
		panelHeight = Math.min(280, height - 16);
		panelLeft = (width - panelWidth) / 2;
		panelTop = (height - panelHeight) / 2;
		contentLeft = panelLeft + 16;
		contentWidth = panelWidth - 32;
		showPreview = panelHeight >= 250;
		previewTop = panelTop + 174;

		int tabY = panelTop + 46;
		int tabWidth = Math.min(132, (contentWidth - 6) / 2);
		ButtonWidget displayTab = addDrawableChild(ButtonWidget.builder(
				Text.translatable("screen.vitals.tab.display"),
				button -> switchPage(Page.DISPLAY)
		).dimensions(contentLeft, tabY, tabWidth, 20).build());
		ButtonWidget entityTab = addDrawableChild(ButtonWidget.builder(
				Text.translatable("screen.vitals.tab.entities"),
				button -> switchPage(Page.ENTITIES)
		).dimensions(contentLeft + tabWidth + 6, tabY, tabWidth, 20).build());
		displayTab.active = page != Page.DISPLAY;
		entityTab.active = page != Page.ENTITIES;

		int controlsTop = panelTop + 72;
		int columnGap = 8;
		int columnWidth = (contentWidth - columnGap) / 2;
		if (page == Page.DISPLAY) {
			buildDisplayControls(controlsTop, columnWidth, columnGap);
		} else {
			buildEntityControls(controlsTop, columnWidth, columnGap);
		}

		int footerY = panelTop + panelHeight - 26;
		int footerGap = 4;
		int footerButtonWidth = (contentWidth - footerGap * 3) / 4;
		addDrawableChild(ButtonWidget.builder(
				Text.translatable("screen.vitals.defaults"),
				button -> resetDefaults()
		).dimensions(contentLeft, footerY, footerButtonWidth, 20).build());
		addDrawableChild(ButtonWidget.builder(
				Text.translatable("gui.cancel"),
				button -> discardAndClose()
		).dimensions(contentLeft + (footerButtonWidth + footerGap), footerY, footerButtonWidth, 20).build());
		applyButton = addDrawableChild(ButtonWidget.builder(
				Text.translatable("screen.vitals.apply"),
				button -> save(false)
		).dimensions(contentLeft + (footerButtonWidth + footerGap) * 2, footerY, footerButtonWidth, 20).build());
		addDrawableChild(ButtonWidget.builder(
				Text.translatable("gui.done"),
				button -> save(true)
		).dimensions(contentLeft + (footerButtonWidth + footerGap) * 3, footerY, footerButtonWidth, 20).build());

		updateActiveStates();
		updateApplyButton();
	}

	private void buildDisplayControls(int top, int columnWidth, int gap) {
		addBoolean(contentLeft, top, columnWidth, "option.vitals.enabled", working.enabled, false, value -> working.enabled = value);
		addBoolean(contentLeft + columnWidth + gap, top, columnWidth, "option.vitals.animation", working.animationEnabled, true, value -> working.animationEnabled = value);
		addBoolean(contentLeft, top + 24, columnWidth, "option.vitals.show_name", working.showName, true, value -> working.showName = value);
		addBoolean(contentLeft + columnWidth + gap, top + 24, columnWidth, "option.vitals.show_health", working.showHealthNumbers, true, value -> working.showHealthNumbers = value);
		addBoolean(contentLeft, top + 48, columnWidth, "option.vitals.show_armor", working.showArmor, true, value -> working.showArmor = value);
		addPrecisionControl(contentLeft + columnWidth + gap, top + 48, columnWidth);
		addSlider(
				contentLeft,
				top + 72,
				columnWidth,
				VitalsConfig.MIN_DISTANCE,
				VitalsConfig.MAX_DISTANCE,
				working.maxDistance,
				value -> Text.translatable("option.vitals.max_distance", Math.round(value)),
				value -> working.maxDistance = (int) Math.round(value)
		);
		addSlider(
				contentLeft + columnWidth + gap,
				top + 72,
				columnWidth,
				VitalsConfig.MIN_SCALE,
				VitalsConfig.MAX_SCALE,
				working.scale,
				value -> Text.translatable("option.vitals.scale", Math.round(value * 100.0D)),
				value -> working.scale = value
		);
	}

	private void buildEntityControls(int top, int columnWidth, int gap) {
		addBoolean(contentLeft, top, columnWidth, "option.vitals.players", working.showPlayers, true, value -> working.showPlayers = value);
		addBoolean(contentLeft + columnWidth + gap, top, columnWidth, "option.vitals.bosses", working.showBosses, true, value -> working.showBosses = value);
		addBoolean(contentLeft, top + 24, columnWidth, "option.vitals.tamed", working.showTamed, true, value -> working.showTamed = value);
		addBoolean(contentLeft + columnWidth + gap, top + 24, columnWidth, "option.vitals.neutral", working.showNeutral, true, value -> working.showNeutral = value);
		addBoolean(contentLeft, top + 48, columnWidth, "option.vitals.hostile", working.showHostile, true, value -> working.showHostile = value);
		addBoolean(contentLeft + columnWidth + gap, top + 48, columnWidth, "option.vitals.passive", working.showPassive, true, value -> working.showPassive = value);
		addBoolean(contentLeft, top + 72, columnWidth, "option.vitals.armor_stands", working.showArmorStands, true, value -> working.showArmorStands = value);
		addBoolean(contentLeft + columnWidth + gap, top + 72, columnWidth, "option.vitals.other_living", working.showOtherLiving, true, value -> working.showOtherLiving = value);
	}

	private void addBoolean(
			int x,
			int y,
			int widgetWidth,
			String translationKey,
			boolean initial,
			boolean dependent,
			Consumer<Boolean> setter
	) {
		CyclingButtonWidget<Boolean> widget = addDrawableChild(CyclingButtonWidget.onOffBuilder(initial).build(
				x,
				y,
				widgetWidth,
				20,
				Text.translatable(translationKey),
				(button, value) -> {
					setter.accept(value);
					markChanged();
					updateActiveStates();
				}
		));
		if (dependent) {
			dependentWidgets.add(widget);
		}
	}

	private void addPrecisionControl(int x, int y, int widgetWidth) {
		CyclingButtonWidget<Integer> widget = addDrawableChild(CyclingButtonWidget.<Integer>builder(
				value -> Text.translatable("option.vitals.decimal_value", value)
		).values(0, 1, 2).initially(working.decimalPlaces).build(
				x,
				y,
				widgetWidth,
				20,
				Text.translatable("option.vitals.decimal_places"),
				(button, value) -> {
					working.decimalPlaces = value;
					markChanged();
				}
		));
		dependentWidgets.add(widget);
	}

	private void addSlider(
			int x,
			int y,
			int widgetWidth,
			double minimum,
			double maximum,
			double initial,
			DoubleFunction<Text> messageFactory,
			DoubleConsumer setter
	) {
		ConfigSlider widget = addDrawableChild(new ConfigSlider(
				x,
				y,
				widgetWidth,
				minimum,
				maximum,
				initial,
				messageFactory,
				value -> {
					setter.accept(value);
					markChanged();
				}
		));
		dependentWidgets.add(widget);
	}

	private void switchPage(Page newPage) {
		if (page != newPage) {
			page = newPage;
			clearAndInit();
		}
	}

	private void resetDefaults() {
		working = VitalsConfig.defaults();
		status = Text.translatable("message.vitals.defaults_loaded");
		statusIsError = false;
		clearAndInit();
	}

	private void save(boolean closeAfterSave) {
		if (!isDirty()) {
			if (closeAfterSave) {
				discardAndClose();
			}
			return;
		}

		VitalsConfigManager.SaveResult result = VitalsConfigManager.saveAndApply(working);
		if (!result.success()) {
			status = Text.translatable("message.vitals.config_save_failed", result.errorCode());
			statusIsError = true;
			updateApplyButton();
			return;
		}

		working = VitalsConfigManager.current().copy();
		status = Text.translatable("message.vitals.config_saved");
		statusIsError = false;
		updateApplyButton();
		if (closeAfterSave) {
			if (client != null && client.player != null) {
				client.player.sendMessage(Text.translatable("message.vitals.config_saved"), true);
			}
			discardAndClose();
		}
	}

	private void markChanged() {
		status = null;
		updateApplyButton();
	}

	private void updateActiveStates() {
		for (ClickableWidget widget : dependentWidgets) {
			widget.active = working.enabled;
		}
	}

	private void updateApplyButton() {
		if (applyButton != null) {
			applyButton.active = isDirty();
		}
	}

	private boolean isDirty() {
		return !working.sanitizedCopy().equals(VitalsConfigManager.current());
	}

	@Override
	public void close() {
		if (client == null) {
			return;
		}
		if (!isDirty()) {
			discardAndClose();
			return;
		}
		client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (client != null) {
						client.setScreen(confirmed ? parent : this);
					}
				},
				Text.translatable("screen.vitals.discard_title"),
				Text.translatable("screen.vitals.discard_message"),
				Text.translatable("screen.vitals.discard"),
				Text.translatable("screen.vitals.keep_editing")
		));
	}

	private void discardAndClose() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL_BACKGROUND);
		context.drawBorder(panelLeft, panelTop, panelWidth, panelHeight, GOLD);
		context.fillGradient(
				panelLeft + 1,
				panelTop + 1,
				panelLeft + panelWidth - 1,
				panelTop + 40,
				PANEL_HEADER_TOP,
				PANEL_HEADER_BOTTOM
		);
		context.fill(panelLeft + 1, panelTop + 40, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, PANEL_INNER);

		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelTop + 9, GOLD);
		Text shortcut = Text.translatable(
				Util.getOperatingSystem() == Util.OperatingSystem.OSX
						? "screen.vitals.shortcut.mac"
						: "screen.vitals.shortcut.windows"
		);
		context.drawCenteredTextWithShadow(textRenderer, shortcut, width / 2, panelTop + 25, MUTED_TEXT);
		if (showPreview) {
			drawPreview(context);
		}

		if (status != null) {
			context.drawCenteredTextWithShadow(
					textRenderer,
					status,
					width / 2,
					showPreview ? panelTop + panelHeight - 38 : panelTop + 174,
					statusIsError ? ERROR_TEXT : SUCCESS_TEXT
			);
		}
		super.render(context, mouseX, mouseY, delta);
	}

	private void drawPreview(DrawContext context) {
		int previewCenter = panelLeft + panelWidth / 2;
		context.drawCenteredTextWithShadow(
				textRenderer,
				Text.translatable("screen.vitals.preview"),
				previewCenter,
				previewTop - 10,
				MUTED_TEXT
		);

		int previewWidth = Math.min(contentWidth - 24, Math.max(96, (int) Math.round(150.0D * working.scale)));
		int left = previewCenter - previewWidth / 2;
		int right = previewCenter + previewWidth / 2;
		int barTop = previewTop + (working.showName ? 10 : 2);
		if (working.showName) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vitals.preview_name"), previewCenter, previewTop, 0xFFFFFFFF);
		}

		context.fill(left, barTop, right, barTop + 13, GOLD);
		context.fill(left + 1, barTop + 1, right - 1, barTop + 12, PREVIEW_BACKGROUND);
		int innerWidth = previewWidth - 4;
		context.fill(left + 2, barTop + 2, left + 2 + (int) (innerWidth * 0.72D), barTop + 11, PREVIEW_TRAIL);
		int healthColor = working.enabled ? HealthBarMath.colorFor(0.63D) : 0xFF66616B;
		context.fill(left + 2, barTop + 2, left + 2 + (int) (innerWidth * 0.63D), barTop + 11, healthColor);

		Text previewValue = previewValueText();
		if (previewValue != null) {
			context.drawCenteredTextWithShadow(textRenderer, previewValue, previewCenter, barTop + 2, 0xFFFFFFFF);
		}
		if (!working.enabled) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vitals.preview_disabled"), previewCenter, barTop + 15, MUTED_TEXT);
		}
	}

	private Text previewValueText() {
		String current = HealthBarMath.formatValue(12.625D, working.decimalPlaces);
		String maximum = HealthBarMath.formatValue(20.0D, working.decimalPlaces);
		if (working.showHealthNumbers && working.showArmor) {
			return Text.translatable("hud.vitals.health_armor", current, maximum, 8);
		}
		if (working.showHealthNumbers) {
			return Text.translatable("hud.vitals.health", current, maximum);
		}
		if (working.showArmor) {
			return Text.translatable("hud.vitals.armor", 8);
		}
		return null;
	}

	private enum Page {
		DISPLAY,
		ENTITIES
	}

	private static final class ConfigSlider extends SliderWidget {
		private final double minimum;
		private final double maximum;
		private final DoubleFunction<Text> messageFactory;
		private final DoubleConsumer setter;

		private ConfigSlider(
				int x,
				int y,
				int width,
				double minimum,
				double maximum,
				double initial,
				DoubleFunction<Text> messageFactory,
				DoubleConsumer setter
		) {
			super(x, y, width, 20, Text.empty(), normalize(initial, minimum, maximum));
			this.minimum = minimum;
			this.maximum = maximum;
			this.messageFactory = messageFactory;
			this.setter = setter;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			if (messageFactory != null) {
				setMessage(messageFactory.apply(actualValue()));
			}
		}

		@Override
		protected void applyValue() {
			if (setter != null) {
				setter.accept(actualValue());
			}
		}

		private double actualValue() {
			return minimum + value * (maximum - minimum);
		}

		private static double normalize(double value, double minimum, double maximum) {
			return Math.max(0.0D, Math.min(1.0D, (value - minimum) / (maximum - minimum)));
		}
	}
}
