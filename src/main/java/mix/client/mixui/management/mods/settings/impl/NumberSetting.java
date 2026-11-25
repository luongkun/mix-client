package mix.client.mixui.management.mods.settings.impl;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.settings.Setting;

public class NumberSetting extends Setting {

	private double defaultValue, value, minValue, maxValue;
	private boolean integer;
	
	public NumberSetting(TranslateText text, Mod parent, double defaultValue, double minValue, double maxValue, boolean integer) {
		super(text, parent);
		
		this.value = defaultValue;
		this.defaultValue = defaultValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.integer = integer;
		
		Mix.getInstance().getModManager().addSettings(this);
	}
	
	@Override
	public void reset() {
		this.value = defaultValue;
	}
	
	public double getValue() {
		
		if(integer) {
			this.value = (int) value;
		}
		
		return value;
	}
	
	public int getValueInt() {
		
		if(integer) {
			this.value = (int) value;
		}
		
		return (int) value;
	}
	
	public float getValueFloat() {
		
		if(integer) {
			this.value = (int) value;
		}
		
		return (float) value;
	}
	
	public long getValueLong() {
		
		if(integer) {
			this.value = (int) value;
		}
		
		return (long) value;
	}
	
	public void setValue(double value) {
		this.value = value;
	}

	public double getMinValue() {
		return minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public double getDefaultValue() {
		return defaultValue;
	}
}
