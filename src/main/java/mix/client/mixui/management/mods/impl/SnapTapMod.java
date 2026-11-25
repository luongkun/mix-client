package mix.client.mixui.management.mods.impl;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventUpdate;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import net.minecraft.client.settings.KeyBinding;

public class SnapTapMod extends Mod {

	private BooleanSetting adSetting = new BooleanSetting(TranslateText.SNAPTAP_AD, this, true);
	private BooleanSetting wsSetting = new BooleanSetting(TranslateText.SNAPTAP_WS, this, false);

	private final boolean[] adDown = new boolean[2];
	private final boolean[] wsDown = new boolean[2];
	private final long[] adTime = new long[2];
	private final long[] wsTime = new long[2];

	public SnapTapMod() {
		super(TranslateText.SNAPTAP, TranslateText.SNAPTAP_DESCRIPTION, ModCategory.PLAYER);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		this.reset();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		this.releaseAll();
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {

		if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
			this.reset();
			return;
		}

		if (adSetting.isToggled()) {
			this.handlePair(mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, adDown, adTime);
		}

		if (wsSetting.isToggled()) {
			this.handlePair(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, wsDown, wsTime);
		}
	}

	private void handlePair(KeyBinding first, KeyBinding second, boolean[] down, long[] time) {

		boolean firstDown = this.isPhysicallyDown(first);
		boolean secondDown = this.isPhysicallyDown(second);
		long now = System.currentTimeMillis();

		if (firstDown && !down[0]) {
			time[0] = now;
		}

		if (secondDown && !down[1]) {
			time[1] = now;
		}

		down[0] = firstDown;
		down[1] = secondDown;

		if (firstDown && secondDown) {
			if (time[0] >= time[1]) {
				KeyBinding.setKeyBindState(second.getKeyCode(), false);
				KeyBinding.setKeyBindState(first.getKeyCode(), true);
			} else {
				KeyBinding.setKeyBindState(first.getKeyCode(), false);
				KeyBinding.setKeyBindState(second.getKeyCode(), true);
			}
		} else {
			KeyBinding.setKeyBindState(first.getKeyCode(), firstDown);
			KeyBinding.setKeyBindState(second.getKeyCode(), secondDown);
		}
	}

	private boolean isPhysicallyDown(KeyBinding keyBinding) {

		int keyCode = keyBinding.getKeyCode();

		if (keyCode == 0) {
			return false;
		}

		return Keyboard.isKeyDown(keyCode);
	}

	private void reset() {

		adDown[0] = false;
		adDown[1] = false;
		wsDown[0] = false;
		wsDown[1] = false;
		adTime[0] = 0L;
		adTime[1] = 0L;
		wsTime[0] = 0L;
		wsTime[1] = 0L;
	}

	private void releaseAll() {

		if (mc.gameSettings == null) {
			return;
		}

		KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), this.isPhysicallyDown(mc.gameSettings.keyBindLeft));
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), this.isPhysicallyDown(mc.gameSettings.keyBindRight));
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), this.isPhysicallyDown(mc.gameSettings.keyBindForward));
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), this.isPhysicallyDown(mc.gameSettings.keyBindBack));

		this.reset();
	}
}
