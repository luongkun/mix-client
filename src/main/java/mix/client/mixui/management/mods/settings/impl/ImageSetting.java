package mix.client.mixui.management.mods.settings.impl;

import java.io.File;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.settings.Setting;

public class ImageSetting extends Setting {

	private File image;
	
	public ImageSetting(TranslateText nameTranslate, Mod parent) {
		super(nameTranslate, parent);
		
		this.image = null;
		
		Mix.getInstance().getModManager().addSettings(this);
	}

	@Override
	public void reset() {
		this.image = null;
	}

	public File getImage() {
		return image;
	}

	public void setImage(File image) {
		this.image = image;
	}
}
