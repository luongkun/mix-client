package mix.client.mixui.management.mods.impl.mechibes;

import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.utils.Sound;
import net.minecraft.util.ResourceLocation;

public class SoundKey {

	private Sound sound;
	private boolean pressed;
	private int lastPressKey;
	
	public SoundKey(String mode, String key) {
		sound = new Sound();
		try {
			sound.loadClip(new ResourceLocation("mixclient/mechvibes/" + mode + "/" + key + ".wav"));
		} catch (Exception e) {
			MixLogger.error("Failed load sound", e);
		}
		
		pressed = false;
		lastPressKey = 0;
	}
	
	public void play() {
		sound.play();
	}

	public void setVolume(float volume) {
		sound.setVolume(volume);
	}
	
	public boolean isPressed() {
		return pressed;
	}

	public void setPressed(boolean pressed) {
		this.pressed = pressed;
	}

	public int getLastPressKey() {
		return lastPressKey;
	}

	public void setLastPressKey(int lastPressKey) {
		this.lastPressKey = lastPressKey;
	}
}
