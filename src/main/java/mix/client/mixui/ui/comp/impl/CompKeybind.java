package mix.client.mixui.ui.comp.impl;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.mods.settings.impl.KeybindSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.ui.comp.Comp;
import mix.client.mixui.utils.mouse.MouseUtils;

public class CompKeybind extends Comp {

	private KeybindSetting setting;
	private float width;
	private boolean binding;
	
	public CompKeybind(float x, float y, float width, KeybindSetting setting) {
		super(x, y);
		this.setting = setting;
		this.width = width;
	}
	
	public CompKeybind(float width, KeybindSetting setting) {
		super(0, 0);
		this.width = width;
		this.setting = setting;
	}

	@Override
	public void draw(int mouseX, int mouseY, float partialTicks) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		AccentColor accentColor = colorManager.getCurrentColor();
		
		String info = binding ? "Binding..." : Keyboard.getKeyName(setting.getKeyCode());
		
		nvg.drawGradientRoundedRect(this.getX(), this.getY(), width, 16, 4, accentColor.getColor1(), accentColor.getColor2());
		
		nvg.drawCenteredText(info, this.getX() + (width / 2), this.getY() + 5F, new Color(255, 255, 255), 8, Fonts.REGULAR);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mosueY, int mouseButton) {
		if(MouseUtils.isInside(mouseX, mosueY, this.getX(), this.getY(), width, 16) && mouseButton == 0) {
			binding = !binding;
		}
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		
		if(binding) {
			
			if(keyCode == Keyboard.KEY_ESCAPE) {
				setting.setKeyCode(Keyboard.KEY_NONE);
				binding = false;
				return;
			}
			
			setting.setKeyCode(keyCode);
			binding = false;
		}
	}

	public boolean isBinding() {
		return binding;
	}
}
