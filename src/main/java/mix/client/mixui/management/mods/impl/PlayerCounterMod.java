package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.SimpleHUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.nanovg.font.Icon;

public class PlayerCounterMod extends SimpleHUDMod {

	private BooleanSetting iconSetting = new BooleanSetting(TranslateText.ICON, this, true);
	
	public PlayerCounterMod() {
		super(TranslateText.PLAYER_COUNTER, TranslateText.PLAYER_COUNTER_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		this.draw();
	}
	
	@Override
	public String getText() {
		return "Player: " + mc.thePlayer.sendQueue.getPlayerInfoMap().size();
	}
	
	@Override
	public String getIcon() {
		return iconSetting.isToggled() ? Icon.USERS : null;
	}
}
