package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.SimpleHUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.nanovg.font.Icon;

public class HealthDisplayMod extends SimpleHUDMod {

	private BooleanSetting iconSetting = new BooleanSetting(TranslateText.ICON, this, true);
	
	public HealthDisplayMod() {
		super(TranslateText.HEALTH_DISPLAY, TranslateText.HEALTH_DISPLAY_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		this.draw();
	}
	
	@Override
	public String getText() {
		return (int) mc.thePlayer.getHealth() + " Health";
	}
	
	@Override
	public String getIcon() {
		return iconSetting.isToggled() ? Icon.HEART : null;
	}
}
