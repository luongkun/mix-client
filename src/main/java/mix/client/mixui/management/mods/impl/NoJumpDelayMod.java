package mix.client.mixui.management.mods.impl;

import mix.client.mixui.injection.interfaces.IMixinEntityLivingBase;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventLivingUpdate;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import net.minecraft.entity.EntityLivingBase;

public class NoJumpDelayMod extends Mod {

	private BooleanSetting onGroundSetting = new BooleanSetting(TranslateText.NO_JUMP_DELAY_ON_GROUND, this, true);

	public NoJumpDelayMod() {
		super(TranslateText.NO_JUMP_DELAY, TranslateText.NO_JUMP_DELAY_DESCRIPTION, ModCategory.PLAYER);
	}

	@EventTarget
	public void onLivingUpdate(EventLivingUpdate event) {

		EntityLivingBase entity = event.getEntity();

		if (entity == null || entity != mc.thePlayer) {
			return;
		}

		if (onGroundSetting.isToggled() && !entity.onGround) {
			return;
		}

		((IMixinEntityLivingBase) entity).setJumpTicks(0);
	}
}
