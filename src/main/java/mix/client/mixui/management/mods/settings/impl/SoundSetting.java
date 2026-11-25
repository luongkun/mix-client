package mix.client.mixui.management.mods.settings.impl;

import java.io.File;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.settings.Setting;

public class SoundSetting extends Setting {

	private File sound;
	
	public SoundSetting(TranslateText nameTranslate, Mod parent) {
		super(nameTranslate, parent);
		
		this.sound = null;
		
		Mix.getInstance().getModManager().addSettings(this);
	}

	@Override
	public void reset() {
		this.sound = null;
	}

	public File getSound() {
		return sound;
	}

	public void setSound(File sound) {
		this.sound = sound;
	}
}
