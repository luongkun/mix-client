package mix.client.mixui.management.mods.impl;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.gui.GuiQuickPlay;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventKey;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.KeybindSetting;

public class HypixelQuickPlayMod extends Mod {

	private KeybindSetting keybindSetting = new KeybindSetting(TranslateText.KEYBIND, this, Keyboard.KEY_N);
	
	public HypixelQuickPlayMod() {
		super(TranslateText.HYPIXEL_QUICK_PLAY, TranslateText.HYPIXEL_QUICK_PLAY_DESCRIPTION, ModCategory.PLAYER);
	}

	@EventTarget
	public void onKey(EventKey event) {
		
		if(event.getKeyCode() == keybindSetting.getKeyCode()) {
			mc.displayGuiScreen(new GuiQuickPlay());
		}
	}
}
