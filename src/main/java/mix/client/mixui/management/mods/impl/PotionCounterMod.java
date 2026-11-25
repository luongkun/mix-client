package mix.client.mixui.management.mods.impl;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.SimpleHUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.PlayerUtils;
import net.minecraft.potion.Potion;

public class PotionCounterMod extends SimpleHUDMod {

	private BooleanSetting iconSetting = new BooleanSetting(TranslateText.ICON, this, true);
	
	public PotionCounterMod() {
		super(TranslateText.POTION_COUNTER, TranslateText.POTION_COUNTER_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		this.draw();
	}
	
	@Override
	public String getText() {
		
		int amount = PlayerUtils.getPotionsFromInventory(Potion.heal);
		
		return amount + " " + (amount <= 1 ? "pot" : "pots");
	}
	
	@Override
	public String getIcon() {
		return iconSetting.isToggled() ? Icon.ARCHIVE : null;
	}
}
