package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.utils.Multithreading;
import mix.client.mixui.viaversion.ViaLoadingBase;
import mix.client.mixui.viaversion.ViaMix;
import mix.client.mixui.viaversion.protocolinfo.ProtocolInfo;

public class ViaVersionMod extends Mod {

	private static ViaVersionMod instance;
	
	private boolean loaded;
	
	public ViaVersionMod() {
		super(TranslateText.VIA_VERSION, TranslateText.VIA_VERSION_DESCRIPTION, ModCategory.OTHER);
		
		instance = this;
		loaded = false;
	}

	@Override
	public void onEnable() {
		super.onEnable();
		
		if(!loaded) {
			loaded = true;
			Multithreading.runAsync(() -> {
				ViaMix.create();
				ViaMix.getInstance().initAsyncSlider();
			});
		}
	}
	
	@Override
	public void onDisable() {
		
		super.onDisable();
		
		if(loaded) {
			ViaMix.getInstance().getAsyncVersionSlider().setVersion(ProtocolInfo.R1_8.getProtocolVersion().getVersion());
			ViaLoadingBase.getInstance().reload(ProtocolInfo.R1_8.getProtocolVersion());
		}
	}

	public static ViaVersionMod getInstance() {
		return instance;
	}

	public boolean isLoaded() {
		return loaded;
	}
}
