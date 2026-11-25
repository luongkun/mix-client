package mix.client.mixui.management.mods;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;

public class SimpleHUDMod extends HUDMod {

	public SimpleHUDMod(TranslateText nameTranslate, TranslateText descriptionText) {
		super(nameTranslate, descriptionText);
	}

	public void draw() {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		boolean hasIcon = getIcon() != null;
		float addX = hasIcon ? this.getTextWidth(getIcon(), HUD_ICON_SIZE, Fonts.ICON) + HUD_ICON_GAP : 0;
		
		if(getText() != null) {
			nvg.setupAndDraw(() -> {
				
				float bgWidth = (this.getTextWidth(this.getText(), HUD_FONT_SIZE, Fonts.REGULAR) + HUD_PADDING_X * 2F) + addX;
				
				this.drawBackground(bgWidth, HUD_HEIGHT);
				this.drawText(this.getText(), HUD_PADDING_X + addX, HUD_PADDING_Y, HUD_FONT_SIZE, Fonts.REGULAR);
				
				if(hasIcon) {
					this.drawText(getIcon(), HUD_PADDING_X, 4F, HUD_ICON_SIZE, Fonts.ICON);
				}
				
				this.setWidth((int) bgWidth);
				this.setHeight((int) HUD_HEIGHT);
			});
		}
	}
	
	public String getText() {
		return null;
	}
	
	public String getIcon() {
		return null;
	}
}
