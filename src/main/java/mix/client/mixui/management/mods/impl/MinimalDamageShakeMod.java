package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventHurtCamera;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;

public class MinimalDamageShakeMod extends Mod {

	private NumberSetting intensitySetting = new NumberSetting(TranslateText.INTENSITY, this, 0, 0, 100, true);
	
	public MinimalDamageShakeMod() {
		super(TranslateText.MINIMAL_DAMAGE_SHAKE, TranslateText.MINIMAL_DAMAGE_SHAKE_DESCRIPTION, ModCategory.RENDER);
	}

	@EventTarget
	public void onHurtCamera(EventHurtCamera event) {
		event.setIntensity(intensitySetting.getValueFloat() / 100F);
	}
}
