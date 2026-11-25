package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;

public class TabEditorMod extends Mod {

	private static TabEditorMod instance;
	
	private BooleanSetting backgroundSetting = new BooleanSetting(TranslateText.BACKGROUND, this, true);
    private BooleanSetting headSetting = new BooleanSetting(TranslateText.HEAD, this, true);
    
	public TabEditorMod() {
		super(TranslateText.TAB_EDITOR, TranslateText.TAB_EDITOR_DESCRIPTION, ModCategory.RENDER);
		
		instance = this;
	}

	public static TabEditorMod getInstance() {
		return instance;
	}

	public BooleanSetting getBackgroundSetting() {
		return backgroundSetting;
	}

	public BooleanSetting getHeadSetting() {
		return headSetting;
	}
}
