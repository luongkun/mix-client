package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventWaterOverlay;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;

public class ClearWaterMod extends Mod {
	
	public ClearWaterMod() {
		super(TranslateText.CLEAR_WATER, TranslateText.CLEAR_WATER_DESCRIPTION, ModCategory.RENDER);
	}

	@EventTarget
	public void onWaterOverlay(EventWaterOverlay event) {
		event.setCancelled(true);
	}
}
