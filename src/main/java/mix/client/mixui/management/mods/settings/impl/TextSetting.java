package mix.client.mixui.management.mods.settings.impl;

import java.util.ArrayList;
import java.util.List;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.settings.Setting;

public class TextSetting extends Setting {

	private String defaultText, text;
	private final List<Runnable> listeners = new ArrayList<Runnable>();
	
	public TextSetting(TranslateText tText, Mod parent, String text) {
		super(tText, parent);
		this.text = text;
		this.defaultText = text;
		
		Mix.getInstance().getModManager().addSettings(this);
	}
	
	@Override
	public void reset() {
		setText(defaultText);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

	public void addListener(Runnable listener) {
		listeners.add(listener);
	}

	public String getDefaultText() {
		return defaultText;
	}
}
