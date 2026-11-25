package mix.client.mixui.management.mods.impl;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventZoomFov;
import mix.client.mixui.management.event.impl.EventScrollMouse;
import mix.client.mixui.management.event.impl.EventTick;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.KeybindSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;

public class ZoomMod extends Mod {

	private SimpleAnimation zoomAnimation = new SimpleAnimation();
	
	private boolean active;
	private float lastSensitivity;
	private float currentFactor = 1;
	
	public boolean wasCinematic;
	
	private BooleanSetting scrollSetting = new BooleanSetting(TranslateText.SCROLL, this, false);
	private BooleanSetting smoothZoomSetting = new BooleanSetting(TranslateText.SMOOTH_ZOOM, this, false);
	
	private NumberSetting zoomSpeedSetting = new NumberSetting(TranslateText.ZOOM_SPEED, this, 14, 5, 20, true);
	private NumberSetting factorSetting = new NumberSetting(TranslateText.ZOOM_FACTOR, this, 4, 2, 15, true);
	
	private BooleanSetting smoothCameraSetting = new BooleanSetting(TranslateText.SMOOTH_CAMERA, this, true);
	private KeybindSetting keybindSetting = new KeybindSetting(TranslateText.KEYBIND, this, Keyboard.KEY_C);
	
	public ZoomMod() {
		super(TranslateText.ZOOM, TranslateText.ZOOM_DESCRIPTION, ModCategory.PLAYER);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		stopZoom();
	}

	private void stopZoom() {
		if(active) {
			active = false;
			setFactor(1);
			mc.gameSettings.mouseSensitivity = lastSensitivity;
			mc.gameSettings.smoothCamera = wasCinematic;
		}
	}

	@EventTarget
	public void onTick(EventTick event) {
		if(mc.thePlayer != null && mc.inGameHasFocus && keybindSetting.isKeyDown()) {
			if(!active) {
				active = true;
				lastSensitivity = mc.gameSettings.mouseSensitivity;
				resetFactor();
				wasCinematic = this.mc.gameSettings.smoothCamera;
				mc.gameSettings.smoothCamera = smoothCameraSetting.isToggled();
				mc.renderGlobal.setDisplayListEntitiesDirty();
			}
		}else if(active) {
			stopZoom();
		}
	}
	
	@EventTarget
	public void onFov(EventZoomFov event) {
		
		zoomAnimation.setAnimation(currentFactor, zoomSpeedSetting.getValueFloat());

		event.setFov(event.getFov() * (smoothZoomSetting.isToggled() ? zoomAnimation.getValue() : currentFactor));
	}
	
	@EventTarget
	public void onScroll(EventScrollMouse event) {
		if(active && scrollSetting.isToggled()) {
			event.setCancelled(true);
			if(event.getAmount() < 0) {
				if(currentFactor < 0.98) {
					currentFactor+=0.03;
				}
			}else if(event.getAmount() > 0) {
				if(currentFactor > 0.06) {
					currentFactor-=0.03;
				}
			}
		}
	}
	
	public void resetFactor() {
		setFactor(1 / factorSetting.getValueFloat());
	}

	public void setFactor(float factor) {
		if(factor != currentFactor) {
			mc.renderGlobal.setDisplayListEntitiesDirty();
		}
		currentFactor = factor;
	}
}
