package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventFireOverlay;
import mix.client.mixui.management.event.impl.EventRenderPumpkinOverlay;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;

public class OverlayEditorMod extends Mod {

	private BooleanSetting hidePumpkinSetting = new BooleanSetting(TranslateText.HIDE_PUMPKIN, this, false);
	private BooleanSetting hideFireSetting = new BooleanSetting(TranslateText.HIDE_FIRE, this, false);
	
	public OverlayEditorMod() {
		super(TranslateText.OVERLAY_EDITOR, TranslateText.OVERLAY_EDITOR_DESCRIPTION, ModCategory.RENDER);
	}
	
	@EventTarget
	public void onRenderPumpkinOverlay(EventRenderPumpkinOverlay event) {
		event.setCancelled(hidePumpkinSetting.isToggled());
	}
	
	@EventTarget
	public void onFireOverlay(EventFireOverlay event) {
		event.setCancelled(hideFireSetting.isToggled());
	}
}
