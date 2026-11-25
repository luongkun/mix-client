package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.SimpleHUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.nanovg.font.Icon;

public class DayCounterMod extends SimpleHUDMod {

	private BooleanSetting iconSetting = new BooleanSetting(TranslateText.ICON, this, true);
	
	public DayCounterMod() {
		super(TranslateText.DAY_COUNTER, TranslateText.DAY_COUNTER_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		this.draw();
	}
	
	@Override
	public String getText() {
		
		long time = mc.theWorld.getWorldInfo().getWorldTotalTime() / 24000L;
		
		return time + " Day" + (time != 1L ? "s" :"");
	}
	
	@Override
	public String getIcon() {
		return iconSetting.isToggled() ? Icon.SUNRISE : null;
	}
}
