package mix.client.mixui.ui.comp.impl.field;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.HeaderGlow;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.color.palette.ColorPalette;
import mix.client.mixui.management.color.palette.ColorType;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.TimerUtils;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;

public class CompSearchBox extends CompTextBoxBase {

	private TimerUtils timer = new TimerUtils();
	private SimpleAnimation searchAnimation = new SimpleAnimation();
	// Hover glow and focus ring are driven separately so the field reacts to the
	// cursor even before it is clicked, and keeps a stronger state once focused.
	private SimpleAnimation hoverAnimation = new SimpleAnimation();
	private SimpleAnimation focusAnimation = new SimpleAnimation();
	
	public CompSearchBox(float x, float y, float width, float height) {
		super(x, y, width, height);
	}
	
	public CompSearchBox() {
		super(0, 0, 0, 0);
	}

	@Override
	public void draw(int mouseX, int mouseY, float partialTicks) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		ColorPalette palette = colorManager.getPalette();
		
		float height = this.getHeight();
		int selectionEnd = this.getSelectionEnd();
		int cursorPosition = this.getCursorPosition();
		String text = this.getText();
		boolean focused = this.isFocused();
		
		float addX = 0;
		float halfHeight = height / 2F;
		// Left gutter reserved for the magnifier glyph. The text/caret used to sit
		// at +16 while the glyph already reached +18, so they overlapped.
		float textX = this.getX() + 26F;
		float rightPad = 6F;
		
		int outTextSize = 0;
		String resultText = "";
		
		for(char c : this.getText().toCharArray()) {
			
			resultText = resultText + Character.toString(c);
			
			if(nvg.getTextWidth(resultText, halfHeight, Fonts.REGULAR) + 26F + rightPad > this.getWidth()) {
				outTextSize++;
				
			    addX = this.getWidth() - nvg.getTextWidth(resultText, halfHeight, Fonts.REGULAR) - 26F - rightPad;
			}
		}
		
	    if(selectionEnd < outTextSize) {
	    	
			StringBuilder reversedText = new StringBuilder(this.getText()).reverse();
			
	    	addX = this.getWidth() - nvg.getTextWidth(reversedText.toString().substring(outTextSize - selectionEnd), halfHeight, Fonts.REGULAR) - 26F - rightPad;
	    }
	    
		// The search field is its own capsule: it never sits inside an extra
		// wrapper, it only reacts with motion. Everything below is drawn on the
		// capsule itself (same bounds, same radius) so no outer container appears.
		boolean hovered = MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
		
		hoverAnimation.setAnimation(hovered ? 1.0F : 0.0F, 16);
		focusAnimation.setAnimation(focused ? 1.0F : 0.0F, 15);
		
		float hover = hoverAnimation.getValue();
		float focus = focusAnimation.getValue();
		float emphasis = Math.max(hover * 0.55F, focus);
		float radius = this.getHeight() / 2F;
		
		Color accent = colorManager.getCurrentColor().getColor1();
		// Only the capsule itself animates: no shadow, no wrapper, no second
		// shape with its own bounds. The motion is a stroked halo that hugs the
		// capsule outline exactly, so the field keeps reading as one capsule.
		Color capsuleFill = mix(palette.getBackgroundColor(ColorType.NORMAL), palette.getBackgroundColor(ColorType.DARK), 0.40F * emphasis);
		capsuleFill = mix(capsuleFill, accent, 0.10F * focus);
		
		float glowIntensity = 0.22F + HeaderGlow.breath() * 0.18F + hover * 0.30F + focus * 0.50F;
		
		HeaderGlow.draw(nvg, this.getX(), this.getY(), this.getWidth(), this.getHeight(), radius, glowIntensity, accent);
		
		nvg.drawRoundedRect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), radius, capsuleFill);
		
		nvg.save();
		nvg.scissor(this.getX() + 1, this.getY(), this.getWidth() - 2, this.getHeight());
		
		if(cursorPosition != selectionEnd) {
			
			int start = selectionEnd > cursorPosition ? cursorPosition : selectionEnd;
			int end = selectionEnd > cursorPosition ? selectionEnd : cursorPosition;
			
			float selectionWidth = nvg.getTextWidth(this.getText().substring(start, end), halfHeight, Fonts.REGULAR);
			float offset = nvg.getTextWidth(this.getText().substring(0, start), halfHeight, Fonts.REGULAR);
			
			if(selectionWidth != 0) {
				nvg.drawRect(textX + offset + addX, this.getY() + (this.getHeight() / 2) - (nvg.getTextHeight(text, halfHeight, Fonts.REGULAR) / 2), selectionWidth, nvg.getTextHeight(text, halfHeight, Fonts.REGULAR), colorManager.getCurrentColor().getColor1());
			}
		}
		
		searchAnimation.setAnimation(!focused && this.getText().isEmpty() ? 1.0F : 0.0F, 16);
		
		nvg.drawText(Icon.SEARCH, this.getX() + 8, this.getY() + (this.getHeight() / 2) - (nvg.getTextHeight(text, halfHeight, Fonts.REGULAR) / 2), mix(palette.getFontColor(ColorType.NORMAL), accent, 0.75F * Math.max(focus, hover * 0.5F)), halfHeight, Fonts.ICON);
		
		if(this.getText().isEmpty()) {
			nvg.save();
			nvg.translate((searchAnimation.getValue() * 8) - 8, 0);
			nvg.drawText(TranslateText.SEARCH.getText(), textX, this.getY() + (this.getHeight() / 2) - (nvg.getTextHeight(text, halfHeight, Fonts.REGULAR) / 2) + 1, palette.getFontColor(ColorType.NORMAL, (int) (searchAnimation.getValue() * 255)), halfHeight, Fonts.REGULAR);
			nvg.restore();
		}
		
		nvg.drawText(this.getText(), textX + addX, this.getY() + (this.getHeight() / 2) - (nvg.getTextHeight(text, halfHeight, Fonts.REGULAR) / 2) + 1, palette.getFontColor(ColorType.DARK), halfHeight, Fonts.REGULAR);
		
		if(timer.delay(600)) {
			
			float position = nvg.getTextWidth(this.getText(), halfHeight, Fonts.REGULAR) - nvg.getTextWidth(this.getText().substring(cursorPosition), halfHeight, Fonts.REGULAR);
			
			if(focused && cursorPosition == selectionEnd) {
				nvg.drawRect(textX + addX + position, 
						this.getY() + (this.getHeight() / 2) - (nvg.getTextHeight(text, halfHeight, Fonts.REGULAR) / 2) - 0.5F, 
						0.7F, 10, palette.getFontColor(ColorType.DARK));
			}
			
			if(timer.delay(1200)) {
				timer.reset();
			}
		}
		
		nvg.restore();
	}
	
	private static Color mix(Color a, Color b, float amount) {
		amount = Math.max(0.0F, Math.min(1.0F, amount));
		return new Color(
				(int) (a.getRed() + (b.getRed() - a.getRed()) * amount),
				(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * amount),
				a.getAlpha());
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		super.keyTyped(typedChar, keyCode);
	}
}
