package mix.client.mixui.management.mods.impl;

import com.mojang.util.UUIDTypeAdapter;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventLocationSkin;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import net.minecraft.util.ResourceLocation;

public class SkinProtectMod extends Mod {

	public SkinProtectMod() {
		super(TranslateText.SKIN_PROTECT, TranslateText.SKIN_PROTECT_DESCRIPTION, ModCategory.PLAYER);
	}
	
	@EventTarget
	public void onLocationSkin(EventLocationSkin event) {
		
		String uuid = UUIDTypeAdapter.fromUUID(event.getPlayerInfo().getGameProfile().getId());
		String pUuid = UUIDTypeAdapter.fromUUID(mc.thePlayer.getGameProfile().getId());
		
		if(uuid.equals(pUuid)) {
			event.setCancelled(true);
			event.setSkin(new ResourceLocation("textures/entity/steve.png"));
		}
	}
}
