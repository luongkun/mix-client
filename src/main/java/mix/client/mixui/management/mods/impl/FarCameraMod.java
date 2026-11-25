package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventCameraRotation;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;

public class FarCameraMod extends Mod {

	private NumberSetting rangeSetting = new NumberSetting(TranslateText.RANGE, this, 15, 0, 50, true);
	
	public FarCameraMod() {
		super(TranslateText.FAR_CAMERA, TranslateText.FAR_CAMERA_DESCRIPTION, ModCategory.RENDER);
	}

	@EventTarget
	public void onCameraRotation(EventCameraRotation event) {
		event.setThirdPersonDistance(rangeSetting.getValueFloat());
	}
}
