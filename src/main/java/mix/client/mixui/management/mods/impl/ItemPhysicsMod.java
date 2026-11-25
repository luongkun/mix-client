package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;

public class ItemPhysicsMod extends Mod {

	private static ItemPhysicsMod instance;
	
	private NumberSetting speedSetting = new NumberSetting(TranslateText.SPEED, this, 1, 0.5, 4, false);
	
	public ItemPhysicsMod() {
		super(TranslateText.ITEM_PHYSICS, TranslateText.ITEM_PHYSICS_DESCRIPTION, ModCategory.RENDER);
		
		instance = this;
	}

	@Override
	public void onEnable() {
		super.onEnable();
		
		if(Items2DMod.getInstance().isToggled()) {
			Items2DMod.getInstance().setToggled(false);
		}
	}
	
	public static ItemPhysicsMod getInstance() {
		return instance;
	}

	public NumberSetting getSpeedSetting() {
		return speedSetting;
	}
}
