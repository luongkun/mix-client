package mix.client.mixui.management.mods.impl;

import java.util.Locale;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.SimpleHUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.nanovg.font.Icon;
import net.minecraft.util.MathHelper;

public class YawDisplayMod extends SimpleHUDMod {

	private BooleanSetting iconSetting = new BooleanSetting(TranslateText.ICON, this, true);

	public YawDisplayMod() {
		super(TranslateText.YAW_DISPLAY, TranslateText.YAW_DISPLAY_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		this.draw();
	}

	@Override
	public String getText() {

		if (mc.thePlayer == null) {
			return null;
		}

		return String.format(Locale.US, "Yaw: %.1f", MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw));
	}

	@Override
	public String getIcon() {
		return iconSetting.isToggled() ? Icon.COMPASS : null;
	}
}
