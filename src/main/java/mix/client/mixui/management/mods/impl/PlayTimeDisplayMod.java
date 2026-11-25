package mix.client.mixui.management.mods.impl;

import mix.client.mixui.Mix;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.SimpleHUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.nanovg.font.Icon;

public class PlayTimeDisplayMod extends SimpleHUDMod {

	private BooleanSetting iconSetting = new BooleanSetting(TranslateText.ICON, this, true);
	
	public PlayTimeDisplayMod() {
		super(TranslateText.PLAY_TIME_DISPLAY, TranslateText.PLAY_TIME_DISPLAY_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		this.draw();
	}
	
	@Override
	public String getText() {
		
		int sec = (int) ((System.currentTimeMillis() - Mix.getInstance().getApi().getLaunchTime()) / 1000);
		int min = (sec % 3600) / 60;
		int hour = sec / 3600;
		sec = sec % 60;
		
		return String.format("%02d", hour) + ":" + String.format("%02d", min) + ":" + String.format("%02d", sec);
	}
	
	@Override
	public String getIcon() {
		return iconSetting.isToggled() ? Icon.CLOCK : null;
	}
}
