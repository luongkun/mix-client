package mix.client.mixui.ui.comp.impl;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.color.palette.ColorPalette;
import mix.client.mixui.management.color.palette.ColorType;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.settings.impl.ImageSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.ui.comp.Comp;
import mix.client.mixui.utils.Multithreading;
import mix.client.mixui.utils.file.FileUtils;
import mix.client.mixui.utils.mouse.MouseUtils;

public class CompImageSelect extends Comp {

	private ImageSetting imageSetting;
	
	public CompImageSelect(float x, float y, ImageSetting imageSetting) {
		super(x, y);
		this.imageSetting = imageSetting;
	}
	
	public CompImageSelect(ImageSetting imageSetting) {
		super(0, 0);
		this.imageSetting = imageSetting;
	}

	@Override
	public void draw(int mouseX, int mouseY, float partialTicks) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		AccentColor accentColor = colorManager.getCurrentColor();
		ColorPalette palette = colorManager.getPalette();
		
		String name = imageSetting.getImage() == null ? TranslateText.NONE.getText() : imageSetting.getImage().getName();
		float nameWidth = nvg.getTextWidth(name, 9, Fonts.REGULAR);
		
		nvg.drawGradientRoundedRect(this.getX(), this.getY(), 16, 16, 4, accentColor.getColor1(), accentColor.getColor2());
		nvg.drawText(name, this.getX() - nameWidth - 5, this.getY() + 4, palette.getFontColor(ColorType.DARK), 9, Fonts.REGULAR);
		nvg.drawCenteredText(Icon.FOLDER, this.getX() + 8, this.getY() + 2.5F, Color.WHITE, 10, Fonts.ICON);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		if(MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), 16, 16) && mouseButton == 0) {
			
			Multithreading.runAsync(() -> {
				
				File image = FileUtils.selectImageFile();
				
				if(image != null) {
					
					FileManager fileManager = Mix.getInstance().getFileManager();
					File cacheDir = new File(fileManager.getCacheDir(), "custom-image");
					
					fileManager.createDir(cacheDir);
					
					File newImage = new File(cacheDir, image.getName());
					
					try {
						FileUtils.copyFile(image, newImage);
					} catch (IOException e) {}
					
					imageSetting.setImage(newImage);
				}
			});
		}
	}
}
