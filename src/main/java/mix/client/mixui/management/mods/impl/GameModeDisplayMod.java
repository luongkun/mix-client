package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.SimpleHUDMod;
import mix.client.mixui.utils.PlayerUtils;

public class GameModeDisplayMod extends SimpleHUDMod {

	public GameModeDisplayMod() {
		super(TranslateText.GAME_MODE_DISPLAY, TranslateText.GAME_MODE_DISPLAY_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		this.draw();
	}
	
	@Override
	public String getText() {
		
		String prefix = "Mode: ";
		
		if(PlayerUtils.isSurvival()) {
			return prefix + "Survival";
		}
		
		if(PlayerUtils.isCreative()) {
			return prefix + "Creative";
		}
		
		if(PlayerUtils.isSpectator()) {
			return prefix + "Spectator";
		}
		
		return prefix + "Error";
	}
}
