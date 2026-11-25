package mix.client.mixui.gui.modmenu.category.impl;

import java.awt.Desktop;
import java.io.IOException;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.screenshot.Screenshot;
import mix.client.mixui.management.screenshot.ScreenshotManager;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;

public class ScreenshotCategory extends Category {

	private static final float MARGIN = 24F;
	private static final float BAR_HEIGHT = 48F;
	private static final float THUMB_SIZE = 34F;
	private static final float THUMB_GAP = 6F;

	private Screenshot currentScreenshot;

	private SimpleAnimation leftAnimation = new SimpleAnimation();
	private SimpleAnimation rightAnimation = new SimpleAnimation();
	private SimpleAnimation trashAnimation = new SimpleAnimation();

	public ScreenshotCategory(GuiModMenu parent) {
		super(parent, TranslateText.SCREENSHOT, Icon.CAMERA, false);
	}

	@Override
	public void initCategory() {
		// 滚动位置由 GuiModMenu 按分类记忆并恢复
	}

	@Override
	public void initGui() {

		ScreenshotManager screenshotManager = Mix.getInstance().getScreenshotManager();

		if(currentScreenshot == null && !screenshotManager.getScreenshots().isEmpty()) {
			currentScreenshot = screenshotManager.getScreenshots().get(0);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ScreenshotManager screenshotManager = instance.getScreenshotManager();
		AccentColor accent = instance.getColorManager().getCurrentColor();

		screenshotManager.loadScreenshots();

		if(currentScreenshot == null && !screenshotManager.getScreenshots().isEmpty()) {
			currentScreenshot = screenshotManager.getScreenshots().get(0);
		}

		float barX = getX() + MARGIN;
		float barY = getY() + getHeight() - BAR_HEIGHT - 10F;
		float barWidth = getWidth() - MARGIN * 2F;
		float viewerX = getX() + MARGIN;
		float viewerY = getY() + 10F;
		float viewerWidth = getWidth() - MARGIN * 2F;
		float viewerHeight = barY - viewerY - 10F;

		leftAnimation.setAnimation(MouseUtils.isInside(mouseX, mouseY, viewerX, viewerY, 36F, viewerHeight) ? 1.0F : 0.0F, 16);
		rightAnimation.setAnimation(MouseUtils.isInside(mouseX, mouseY, viewerX + viewerWidth - 36F, viewerY, 36F, viewerHeight) ? 1.0F : 0.0F, 16);

		if(currentScreenshot != null) {

			trashAnimation.setAnimation(MouseUtils.isInside(mouseX, mouseY, viewerX + viewerWidth - 44F, viewerY + 10F, 34F, 34F) ? 1.0F : 0.0F, 16);

			nvg.drawRoundedImage(currentScreenshot.getImage(), viewerX, viewerY, viewerWidth, viewerHeight, 14F);

			// Delete
			float trashX = viewerX + viewerWidth - 44F;
			float trashY = viewerY + 10F;
			nvg.drawRoundedRect(trashX, trashY, 34F, 34F, 17F,
					ModMenuTheme.applyAlpha(ModMenuTheme.surface(), (int) (140 + trashAnimation.getValue() * 80)));
			nvg.drawCenteredText(Icon.TRASH, trashX + 17F, trashY + 9F,
					ModMenuTheme.applyAlpha(ModMenuTheme.error(), (int) (150 + trashAnimation.getValue() * 105)), 14, Fonts.ICON);

			drawArrow(nvg, viewerX + 10F, viewerY + viewerHeight / 2F - 17F, Icon.ARROW_LEFT, leftAnimation.getValue());
			drawArrow(nvg, viewerX + viewerWidth - 44F, viewerY + viewerHeight / 2F - 17F, Icon.ARROW_RIGHT, rightAnimation.getValue());

		}else {

			nvg.drawRoundedRect(viewerX, viewerY, viewerWidth, viewerHeight, 14, ModMenuTheme.surfaceContainerLow());
			nvg.drawCenteredText(Icon.CAMERA, viewerX + viewerWidth / 2F, viewerY + viewerHeight / 2F - 34F,
					ModMenuTheme.onSurfaceVariant(), 52, Fonts.ICON);
			nvg.drawCenteredText(TranslateText.SCREENSHOT.getText(), viewerX + viewerWidth / 2F, viewerY + viewerHeight / 2F + 30F,
					ModMenuTheme.onSurfaceVariant(), 11, Fonts.MEDIUM);
		}

		// Thumbnail strip
		nvg.drawRoundedRect(barX, barY, barWidth, BAR_HEIGHT, 12, ModMenuTheme.surfaceContainer());

		float thumbViewportX = barX + 8F;
		float thumbViewportY = barY + 7F;
		float thumbViewportWidth = barWidth - 16F;
		float thumbViewportHeight = BAR_HEIGHT - 14F;

		int count = screenshotManager.getScreenshots().size();
		scroll.setMaxScroll(Math.max(0, count * (THUMB_SIZE + THUMB_GAP) - THUMB_GAP - thumbViewportWidth));

		nvg.save();
		nvg.scissor(thumbViewportX, thumbViewportY, thumbViewportWidth, thumbViewportHeight);
		nvg.translate(scroll.getValue(), 0);

		float thumbX = 0;

		for(Screenshot screenshot : screenshotManager.getScreenshots()) {

			float x = barX + 8F + thumbX;
			boolean selected = screenshot.equals(currentScreenshot);
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, x + scroll.getValue(), thumbViewportY, THUMB_SIZE, THUMB_SIZE);

			nvg.save();
			nvg.scissor(x, thumbViewportY, THUMB_SIZE, THUMB_SIZE);
			nvg.drawImage(screenshot.getImage(), x - 13F, thumbViewportY, 60F, THUMB_SIZE);
			nvg.restore();

			nvg.drawRoundedRect(x, thumbViewportY, THUMB_SIZE, THUMB_SIZE, 6F,
					ModMenuTheme.applyAlpha(hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow(),
							hovered ? 40 : 20));

			screenshot.getSelectAnimation().setAnimation(selected ? 1.0F : 0.0F, 16);

			float selectedValue = screenshot.getSelectAnimation().getValue();

			if(selectedValue > 0.01F) {
				int alpha = (int) (selectedValue * 255);
				nvg.drawGradientOutlineRoundedRect(x, thumbViewportY, THUMB_SIZE, THUMB_SIZE, 6F, 2F,
						ColorUtils.applyAlpha(accent.getColor1(), alpha),
						ColorUtils.applyAlpha(accent.getColor2(), alpha));
			}

			thumbX += THUMB_SIZE + THUMB_GAP;
		}

		nvg.restore();
	}

	private void drawArrow(NanoVGManager nvg, float x, float y, String icon, float value) {

		if(value <= 0.01F) {
			return;
		}

		nvg.drawRoundedRect(x, y, 34F, 34F, 17F,
				ModMenuTheme.applyAlpha(ModMenuTheme.surface(), (int) (value * 210)));
		nvg.drawCenteredText(icon, x + 17F, y + 9F,
				ModMenuTheme.applyAlpha(ModMenuTheme.onSurface(), (int) (value * 255)), 15, Fonts.ICON);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		ScreenshotManager screenshotManager = Mix.getInstance().getScreenshotManager();

		if(mouseButton != 0) {
			return;
		}

		float barX = getX() + MARGIN;
		float barY = getY() + getHeight() - BAR_HEIGHT - 10F;
		float barWidth = getWidth() - MARGIN * 2F;
		float viewerX = getX() + MARGIN;
		float viewerY = getY() + 10F;
		float viewerWidth = getWidth() - MARGIN * 2F;
		float viewerHeight = barY - viewerY - 10F;

		if(currentScreenshot != null && MouseUtils.isInside(mouseX, mouseY, viewerX + viewerWidth - 48F, viewerY + 6F, 42F, 42F)) {

			int index = screenshotManager.getScreenshots().indexOf(currentScreenshot) - 1;
			screenshotManager.delete(currentScreenshot);

			if(index < 0) {
				index = 0;
			}

			if(screenshotManager.getScreenshots().isEmpty()) {
				currentScreenshot = null;
			}else {
				currentScreenshot = screenshotManager.getScreenshots().get(Math.min(index, screenshotManager.getScreenshots().size() - 1));
			}

			return;
		}

		// Thumbnails
		float thumbViewportX = barX + 8F;
		float thumbViewportY = barY + 7F;
		float thumbViewportWidth = barWidth - 16F;

		if(MouseUtils.isInside(mouseX, mouseY, thumbViewportX, thumbViewportY, thumbViewportWidth, THUMB_SIZE)) {

			float thumbX = scroll.getValue();

			for(Screenshot screenshot : screenshotManager.getScreenshots()) {

				float x = barX + 8F + thumbX;

				if(MouseUtils.isInside(mouseX, mouseY, x, thumbViewportY, THUMB_SIZE, THUMB_SIZE)) {
					currentScreenshot = screenshot;
					return;
				}

				thumbX += THUMB_SIZE + THUMB_GAP;
			}
		}

		if(currentScreenshot == null) {
			return;
		}

		if(MouseUtils.isInside(mouseX, mouseY, viewerX + 10F, viewerY + viewerHeight / 2F - 17F, 34F, 34F)) {
			currentScreenshot = screenshotManager.getBackScreenshot(currentScreenshot);
			return;
		}

		if(MouseUtils.isInside(mouseX, mouseY, viewerX + viewerWidth - 44F, viewerY + viewerHeight / 2F - 17F, 34F, 34F)) {
			currentScreenshot = screenshotManager.getNextScreenshot(currentScreenshot);
			return;
		}

		if(MouseUtils.isInside(mouseX, mouseY, viewerX, viewerY, viewerWidth, viewerHeight)) {
			try {
				Desktop.getDesktop().open(currentScreenshot.getImage());
			} catch (IOException e) {}
		}
	}
}
