package mix.client.mixui.gui.modmenu.category.impl.setting.impl;

import java.awt.Color;
import java.io.File;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.impl.SettingCategory;
import mix.client.mixui.gui.modmenu.category.impl.setting.SettingScene;
import mix.client.mixui.management.account.Account;
import mix.client.mixui.management.account.AvatarManager;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.color.Theme;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.impl.GlobalSettingsMod;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.profile.MenuPrefs;
import mix.client.mixui.ui.comp.impl.CompComboBox;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.file.FileUtils;
import mix.client.mixui.utils.mouse.MouseUtils;
import mix.client.mixui.utils.mouse.Scroll;
import net.minecraft.util.ResourceLocation;

public class AppearanceScene extends SettingScene {

	private static final float[] SCALE_OPTIONS = {0.75F, 0.9F, 1.0F, 1.25F, 1.5F, 2.0F};
	private static final String[] SCALE_LABELS = {"75%", "90%", "100%", "125%", "150%", "200%"};

	private Scroll scroll = new Scroll();
	private CompComboBox modTheme;

	public AppearanceScene(SettingCategory parent) {
		super(parent, TranslateText.APPEARANCE, TranslateText.APPEARANCE_DESCRIPTION, Icon.MONITOR);
	}

	@Override
	public void initGui() {
		modTheme = new CompComboBox(150, GlobalSettingsMod.getInstance().getModThemeSetting());
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		AccentColor currentColor = colorManager.getCurrentColor();

		float x = this.getX();
		float y = this.getY();
		float width = this.getWidth();

		// Theme
		nvg.drawRoundedRect(x, y, width, 92F, 12, ModMenuTheme.surfaceContainerLow());
		nvg.drawText(TranslateText.THEME.getText(), x + 16F, y + 13F, ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);

		float offsetX = 0;

		for(Theme t : Theme.values()) {

			float swatchX = x + 16F + offsetX;
			float swatchY = y + 38F;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, swatchX, swatchY, 40F, 40F);

			t.getAnimation().setAnimation(t.equals(colorManager.getTheme()) ? 1.0F : 0.0F, 16);

			nvg.drawRoundedRect(swatchX, swatchY, 40F, 40F, 10F,
					hovered ? t.getNormalBackgroundColor(255) : t.getDarkBackgroundColor(255));

			float selected = t.getAnimation().getValue();

			if(selected > 0.01F) {
				int alpha = (int) (selected * 255);
				nvg.drawGradientOutlineRoundedRect(swatchX, swatchY, 40F, 40F, 10F, 2F,
						ColorUtils.applyAlpha(currentColor.getColor1(), alpha),
						ColorUtils.applyAlpha(currentColor.getColor2(), alpha));
				nvg.drawCenteredText(Icon.CHECK, swatchX + 20F, swatchY + 11F,
						ColorUtils.applyAlpha(currentColor.getInterpolateColor(), alpha), 15, Fonts.ICON);
			}

			offsetX += 50F;
		}

		// Accent colours
		float accentY = y + 104F;
		nvg.drawRoundedRect(x, accentY, width, 104F, 12, ModMenuTheme.surfaceContainerLow());
		nvg.drawText(TranslateText.ACCENT_COLOR.getText(), x + 16F, accentY + 13F,
				ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);

		float viewportX = x + 10F;
		float viewportY = accentY + 36F;
		float viewportWidth = width - 20F;
		float viewportHeight = 54F;

		int colorCount = colorManager.getColors().size();
		scroll.setMaxScroll(Math.max(0, colorCount * 44F - 4F - viewportWidth));

		if(MouseUtils.isInside(mouseX, mouseY, viewportX, viewportY, viewportWidth, viewportHeight)) {
			scroll.onScroll();
		}
		scroll.onAnimation();

		nvg.save();
		nvg.scissor(viewportX, viewportY, viewportWidth, viewportHeight);
		nvg.translate(scroll.getValue(), 0);

		offsetX = 0;

		for(AccentColor c : colorManager.getColors()) {

			float circleX = x + 16F + offsetX;
			float circleY = accentY + 44F;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, circleX + scroll.getValue(), circleY, 36F, 36F);

			c.getAnimation().setAnimation(c.equals(currentColor) ? 1.0F : 0.0F, 16);

			nvg.drawGradientRoundedRect(circleX, circleY, 36F, 36F, 18F, c.getColor1(), c.getColor2());

			float selected = c.getAnimation().getValue();

			if(selected > 0.01F) {
				nvg.drawOutlineRoundedRect(circleX - 2F, circleY - 2F, 40F, 40F, 20F, 1.4F,
						ColorUtils.applyAlpha(currentColor.getInterpolateColor(), (int) (selected * 255)));
				nvg.drawCenteredText(Icon.CHECK, circleX + 18F, circleY + 10F,
						new Color(255, 255, 255, (int) (selected * 255)), 14, Fonts.ICON);
			}else if(hovered) {
				nvg.drawOutlineRoundedRect(circleX - 2F, circleY - 2F, 40F, 40F, 20F, 1F,
						ModMenuTheme.outline());
			}

			offsetX += 44F;
		}

		nvg.restore();

		// HUD theme
		float hudY = accentY + 116F;
		nvg.drawRoundedRect(x, hudY, width, 64F, 12, ModMenuTheme.surfaceContainerLow());
		nvg.drawText(TranslateText.HUD_THEME.getText(), x + 16F, hudY + 15F,
				ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);
		nvg.drawText(TranslateText.APPEARANCE_DESCRIPTION.getText(), x + 16F, hudY + 35F,
				ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.REGULAR);

		modTheme.setX(x + width - 166F);
		modTheme.setY(hudY + 24F);
		modTheme.draw(mouseX, mouseY, partialTicks);

		// Menu preferences: UI scale + custom avatar
		float prefY = hudY + 76F;
		float halfWidth = (width - 12F) / 2F;

		// UI scale
		nvg.drawRoundedRect(x, prefY, halfWidth, 110F, 12, ModMenuTheme.surfaceContainerLow());
		nvg.drawText(TranslateText.UI_SCALE.getText(), x + 16F, prefY + 13F, ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);
		nvg.drawText(TranslateText.UI_SCALE_DESCRIPTION.getText(), x + 16F, prefY + 31F, ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.REGULAR);

		// The effective scale also depends on how much room the window offers, so a
		// large choice can be capped.  Say so instead of looking "not saved".
		float limit = getModMenu().getWindowScaleLimit();
		float requested = MenuPrefs.getInstance().getMenuScale();
		boolean capped = requested > limit + 0.001F;
		String appliedScale = Math.round(getModMenu().getDesignScale() * 100F) + "%";

		nvg.drawText(appliedScale, x + halfWidth - 16F - nvg.getTextWidth(appliedScale, 9F, Fonts.MEDIUM), prefY + 15F,
				capped ? ModMenuTheme.primary() : ModMenuTheme.onSurfaceVariant(), 9F, Fonts.MEDIUM);

		float chipY = prefY + 56F;
		float chipX = x + 16F;

		for(int i = 0; i < SCALE_OPTIONS.length; i++) {

			boolean applied = Math.abs(requested - SCALE_OPTIONS[i]) < 0.001F;
			boolean fits = SCALE_OPTIONS[i] <= limit + 0.001F;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, chipX, chipY, 48F, 26F);

			if(applied) {
				nvg.drawGradientRoundedRect(chipX, chipY, 48F, 26F, 13F, currentColor.getColor1(), currentColor.getColor2());
				nvg.drawCenteredText(SCALE_LABELS[i], chipX + 24F, chipY + 8F, Color.WHITE, 9F, Fonts.MEDIUM);

				if(!fits) {
					nvg.drawOutlineRoundedRect(chipX, chipY, 48F, 26F, 13F, 1F, ModMenuTheme.applyAlpha(Color.WHITE, 170));
				}
			}else {
				nvg.drawRoundedRect(chipX, chipY, 48F, 26F, 13F,
						ModMenuTheme.applyAlpha(ModMenuTheme.surfaceContainer(), fits ? 255 : 150));
				if(hovered) {
					nvg.drawOutlineRoundedRect(chipX, chipY, 48F, 26F, 13F, 1F, ModMenuTheme.outline());
				}
				nvg.drawCenteredText(SCALE_LABELS[i], chipX + 24F, chipY + 8F,
						ModMenuTheme.applyAlpha(ModMenuTheme.onSurfaceVariant(), fits ? 255 : 120), 9F, Fonts.MEDIUM);
			}

			chipX += 54F;
		}

		if(capped) {
			String limitText = TranslateText.UI_SCALE_LIMIT.getText().replace("%s", Math.round(limit * 100F) + "%");
			nvg.drawText(limitText, x + 16F, prefY + 90F, ModMenuTheme.applyAlpha(ModMenuTheme.primary(), 200), 8F, Fonts.REGULAR);
		}

		// Custom avatar
		float avatarCardX = x + halfWidth + 12F;
		nvg.drawRoundedRect(avatarCardX, prefY, halfWidth, 110F, 12, ModMenuTheme.surfaceContainerLow());
		nvg.drawText(TranslateText.CUSTOM_AVATAR.getText(), avatarCardX + 16F, prefY + 13F, ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);
		nvg.drawText(TranslateText.CUSTOM_AVATAR_DESCRIPTION.getText(), avatarCardX + 16F, prefY + 31F, ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.REGULAR);

		File previewFile = AvatarManager.getInstance().getCustomAvatarFile();
		Account account = instance.getAccountManager().getCurrentAccount();

		if(previewFile == null && account != null) {
			previewFile = AvatarManager.getInstance().getAvatarFile(account);
		}

		if(previewFile == null || !previewFile.exists()) {
			nvg.drawPlayerHead(new ResourceLocation("textures/entity/steve.png"), avatarCardX + 16F, prefY + 48F, 46F, 46F, 23F);
		}else {
			nvg.drawRoundedImage(previewFile, avatarCardX + 16F, prefY + 48F, 46F, 46F, 23F);
		}

		float prefButtonY = prefY + 58F;

		nvg.drawGradientRoundedRect(avatarCardX + 76F, prefButtonY, 86F, 26F, 13F, currentColor.getColor1(), currentColor.getColor2());
		nvg.drawCenteredText(TranslateText.SELECT_IMAGE.getText(), avatarCardX + 76F + 43F, prefButtonY + 8F, Color.WHITE, 9F, Fonts.MEDIUM);

		boolean clearHovered = MouseUtils.isInside(mouseX, mouseY, avatarCardX + 170F, prefButtonY, 56F, 26F);
		nvg.drawRoundedRect(avatarCardX + 170F, prefButtonY, 56F, 26F, 13F,
				clearHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
		nvg.drawCenteredText(TranslateText.CLEAR_AVATAR.getText(), avatarCardX + 170F + 28F, prefButtonY + 8F, ModMenuTheme.onSurfaceVariant(), 9F, Fonts.MEDIUM);

	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		if(mouseButton != 0) {
			return;
		}

		Mix instance = Mix.getInstance();
		ColorManager colorManager = instance.getColorManager();

		float x = this.getX();
		float y = this.getY();
		float width = this.getWidth();

		float offsetX = 0;

		for(Theme t : Theme.values()) {

			if(MouseUtils.isInside(mouseX, mouseY, x + 16F + offsetX, y + 38F, 40F, 40F)) {
				colorManager.setTheme(t);
			}

			offsetX += 50F;
		}

		float accentY = y + 104F;
		float viewportX = x + 10F;
		float viewportY = accentY + 36F;

		if(MouseUtils.isInside(mouseX, mouseY, viewportX, viewportY, width - 20F, 54F)) {

			offsetX = scroll.getValue();

			for(AccentColor c : colorManager.getColors()) {

				if(MouseUtils.isInside(mouseX, mouseY, x + 16F + offsetX, accentY + 44F, 36F, 36F)) {
					colorManager.setCurrentColor(c);
					break;
				}

				offsetX += 44F;
			}
		}

		// Menu preferences: UI scale + custom avatar
		float prefY = accentY + 116F + 76F;
		float halfWidth = (width - 12F) / 2F;
		float chipY = prefY + 56F;
		float chipX = x + 16F;

		for(int i = 0; i < SCALE_OPTIONS.length; i++) {

			if(MouseUtils.isInside(mouseX, mouseY, chipX, chipY, 48F, 26F)) {
				MenuPrefs.getInstance().setMenuScale(SCALE_OPTIONS[i]);
				getModMenu().refreshLayout();
				break;
			}

			chipX += 54F;
		}

		float avatarCardX = x + halfWidth + 12F;
		float prefButtonY = prefY + 58F;

		if(MouseUtils.isInside(mouseX, mouseY, avatarCardX + 76F, prefButtonY, 86F, 26F)) {

			File selected = FileUtils.selectImageFile();

			if(selected != null) {
				AvatarManager.getInstance().setCustomAvatar(selected);
			}
		}

		if(MouseUtils.isInside(mouseX, mouseY, avatarCardX + 170F, prefButtonY, 56F, 26F)) {
			AvatarManager.getInstance().clearCustomAvatar();
		}

		modTheme.mouseClicked(mouseX, mouseY, mouseButton);
	}

}
