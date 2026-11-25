package mix.client.mixui.management.mods.impl;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventDamageEntity;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.HUDMod;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.utils.ColorUtils;

public class JumpResetIndicatorMod extends HUDMod {

	private NumberSetting durationSetting = new NumberSetting(TranslateText.JUMP_RESET_DURATION, this, 900, 300, 3000, true);

	private long triggerTime = 0L;

	public JumpResetIndicatorMod() {
		super(TranslateText.JUMP_RESET_INDICATOR, TranslateText.JUMP_RESET_INDICATOR_DESCRIPTION);
	}

	@EventTarget
	public void onDamageEntity(EventDamageEntity event) {

		if (mc.thePlayer == null || event.getEntity() != mc.thePlayer) {
			return;
		}

		this.triggerTime = System.currentTimeMillis();
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {

		long duration = durationSetting.getValueLong();
		long elapsed = System.currentTimeMillis() - this.triggerTime;

		if (this.triggerTime == 0L || elapsed < 0L || elapsed > duration) {
			return;
		}

		float progress = 1.0F - ((float) elapsed / (float) duration);
		int alpha = (int) (255.0F * Math.min(1.0F, progress * 2.0F));

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		AccentColor currentColor = instance.getColorManager().getCurrentColor();

		String text = TranslateText.JUMP_RESET_LABEL.getText();
		float textWidth = this.getTextWidth(text, 9.0F, Fonts.MEDIUM);
		float width = textWidth + 12.0F;
		float height = 18.0F;

		this.setWidth((int) width);
		this.setHeight((int) height);

		nvg.setupAndDraw(() -> {

			this.drawBackground(width, height);

			this.drawCenteredText(text, width / 2.0F, 5.5F, 9.0F, Fonts.MEDIUM);

			float barWidth = (width - 8.0F) * progress;

			this.drawRoundedRect(4.0F, height - 3.5F, width - 8.0F, 1.5F, 0.75F, new Color(0, 0, 0, 90));
			this.drawRoundedRect(4.0F, height - 3.5F, Math.max(barWidth, 0.0F), 1.5F, 0.75F,
					ColorUtils.applyAlpha(currentColor.getInterpolateColor(alpha), alpha));
		});
	}
}
