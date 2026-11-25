package mix.client.mixui.management.mods.settings.impl;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.settings.Setting;

public class BooleanSetting extends Setting {

	private boolean defaultValue, toggled;
	
	public BooleanSetting(TranslateText text, Mod parent, boolean toggled) {
		super(text, parent);
		
		this.toggled = toggled;
		this.defaultValue = toggled;
		
		Mix.getInstance().getModManager().addSettings(this);
	}
	
	@Override
	public void reset() {
		this.toggled = defaultValue;
	}
	
	public boolean isToggled() {
		return toggled;
	}

	public void setToggled(boolean toggle) {
		this.toggled = toggle;
	}

	public boolean isDefaultValue() {
		return defaultValue;
	}
}
