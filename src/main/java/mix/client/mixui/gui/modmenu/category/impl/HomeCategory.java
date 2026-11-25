package mix.client.mixui.gui.modmenu.category.impl;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.management.account.Account;
import mix.client.mixui.management.account.AccountManager;
import mix.client.mixui.management.account.AvatarManager;
import mix.client.mixui.management.changelog.Changelog;
import mix.client.mixui.management.changelog.ChangelogManager;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.nanovg.font.MaterialIcons;
import mix.client.mixui.management.profile.MenuPrefs;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.mouse.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class HomeCategory extends Category {

	private static final float MARGIN = 24F;
	private static final float GAP = 14F;
	private static final float FAVORITE_HEADER = 38F;
	private static final float FAVORITE_ROW_HEIGHT = 30F;
	private static final float FAVORITE_ROW_GAP = 6F;
	private static final float FAVORITE_COLUMN_GAP = 12F;

	public HomeCategory(GuiModMenu parent) {
		super(parent, TranslateText.HOME, Icon.HOME, false);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		AccountManager accountManager = instance.getAccountManager();
		AccentColor currentColor = colorManager.getCurrentColor();
		ChangelogManager changelogManager = instance.getChangelogManager();

		float contentX = getX() + MARGIN;
		float contentY = getY() + 16F;
		float contentWidth = getWidth() - (MARGIN * 2F);
		float contentBottom = getY() + getHeight() - 16F;
		float contentHeight = contentBottom - contentY;
		float heroHeight = clamp(contentHeight * 0.42F, 148F, 196F);
		float heroWidth = Math.max(300F, Math.min(contentWidth - 260F, (contentWidth - GAP) * 0.58F));
		float sideWidth = contentWidth - heroWidth - GAP;

		// Welcome hero
		File headFile = AvatarManager.getInstance().getAvatarFile(accountManager.getCurrentAccount());

		nvg.drawRoundedRect(contentX, contentY, heroWidth, heroHeight, 14, ModMenuTheme.surfaceContainer());
		nvg.drawGradientRoundedRect(contentX, contentY, 6, heroHeight, 3, currentColor.getColor1(), currentColor.getColor2());

		float avatarSize = clamp(heroHeight * 0.40F, 52F, 76F);
		float avatarY = contentY + ((heroHeight - avatarSize) / 2F);
		float textX = contentX + 22F + avatarSize + 18F;

		if(headFile == null || !headFile.exists()) {
			nvg.drawPlayerHead(new ResourceLocation("textures/entity/steve.png"), contentX + 22F, avatarY, avatarSize, avatarSize, 14);
		}else {
			nvg.drawRoundedImage(headFile, contentX + 22F, avatarY, avatarSize, avatarSize, 14);
		}

		Account account = accountManager.getCurrentAccount();
		String accountName = account != null ? account.getName()
				: Minecraft.getMinecraft().getSession().getUsername();
		float textY = contentY + (heroHeight / 2F) - 38F;

		nvg.drawText(TranslateText.WELCOME_TO_MIX.getText(), textX, textY, ModMenuTheme.onSurface(), 16, Fonts.DEMIBOLD);
		nvg.drawText(accountName == null ? "-" : accountName, textX, textY + 24F, ModMenuTheme.primary(), 12, Fonts.MEDIUM);

		String version = "MixClient " + instance.getVersion();
		float versionWidth = nvg.getTextWidth(version, 9.5F, Fonts.MEDIUM) + 22F;
		nvg.drawRoundedRect(textX, textY + 46F, versionWidth, 22, 11, ModMenuTheme.surfaceContainerHigh());
		nvg.drawCenteredText(version, textX + (versionWidth / 2F), textY + 52F, ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.MEDIUM);

		// Changelog
		float sideX = contentX + heroWidth + GAP;

		boolean changelogHovered = MouseUtils.isInside(mouseX, mouseY, sideX, contentY, sideWidth, heroHeight);

		nvg.drawRoundedRect(sideX, contentY, sideWidth, heroHeight, 14, ModMenuTheme.surfaceContainer());

		if(changelogHovered) {
			nvg.drawOutlineRoundedRect(sideX, contentY, sideWidth, heroHeight, 14, 1F,
					ColorUtils.applyAlpha(currentColor.getColor1(), 140));
		}

		nvg.drawText(TranslateText.CHANGELOG.getText(), sideX + 16F, contentY + 12F, ModMenuTheme.onSurface(), 13, Fonts.DEMIBOLD);

		// 右上角提示可点开全部更新（箭头用 ICON 字体单独画，MEDIUM 字体里没有这个字形）
		String changelogMore = TranslateText.ALL.getText();
		float moreWidth = nvg.getTextWidth(changelogMore, 9F, Fonts.MEDIUM);
		float moreX = sideX + sideWidth - 16F - moreWidth - 10F;
		Color moreColor = changelogHovered ? ModMenuTheme.primary() : ModMenuTheme.onSurfaceVariant();

		nvg.drawText(changelogMore, moreX, contentY + 13F, moreColor, 9F, Fonts.MEDIUM);
		nvg.drawCenteredText(Icon.ARROW_RIGHT, moreX + moreWidth + 6F, contentY + 13F, moreColor, 8F, Fonts.ICON);
		nvg.drawRect(sideX + 16F, contentY + 36F, sideWidth - 32F, 1, ModMenuTheme.outlineVariant());

		float changelogY = contentY + 46F;

		for(Changelog c : changelogManager.getChangelogs()) {

			if(changelogY + 16F > contentY + heroHeight - 6F) {
				break;
			}

			float badgeWidth = 46F;
			nvg.drawRoundedRect(sideX + 16F, changelogY, badgeWidth, 14, 7, c.getType().getColor());
			nvg.drawCenteredText(c.getType().getText(), sideX + 16F + (badgeWidth / 2F), changelogY + 3.5F, Color.WHITE, 8, Fonts.MEDIUM);
			nvg.drawText(nvg.getLimitText(c.getText(), 9, Fonts.MEDIUM, Math.max(80F, sideWidth - 90F)), sideX + 70F, changelogY + 3F,
					ModMenuTheme.onSurfaceVariant(), 9, Fonts.MEDIUM);

			changelogY += 18F;
		}

		// Favourites: the mods the user starred are listed right here and can be
		// toggled without opening the module browser.  (The music player stays
		// hidden until the NetEase player works again, its category is still in
		// the navigation rail.)
		FavoriteLayout layout = getFavoriteLayout();
		ArrayList<Mod> favoriteMods = getFavoriteMods();
		int favoriteCount = MenuPrefs.getInstance().getFavoriteCount();
		int totalMods = getTotalMods();

		nvg.drawRoundedRect(contentX, layout.top, contentWidth, layout.height, 14, ModMenuTheme.surfaceContainer());

		float headerY = layout.top + 14F;
		boolean headerHovered = MouseUtils.isInside(mouseX, mouseY, contentX, layout.top, contentWidth, 34F);
		float arrowSize = 22F;
		float arrowX = contentX + contentWidth - 18F - arrowSize;
		float arrowY = headerY - 4F;

		nvg.drawText(TranslateText.FAVORITE.getText(), contentX + 18F, headerY, ModMenuTheme.onSurface(), 13, Fonts.DEMIBOLD);

		nvg.drawRoundedRect(arrowX, arrowY, arrowSize, arrowSize, arrowSize / 2F,
				headerHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.applyAlpha(ModMenuTheme.surfaceContainerLow(), 160));
		nvg.drawCenteredText(Icon.ARROW_RIGHT, arrowX + (arrowSize / 2F), arrowY + 5.5F,
				headerHovered ? ModMenuTheme.primary() : ModMenuTheme.onSurfaceVariant(), 12, Fonts.ICON);

		String counter = favoriteCount + " / " + totalMods + "  " + TranslateText.MODULE.getText();
		nvg.drawText(counter, arrowX - 12F - nvg.getTextWidth(counter, 9.5F, Fonts.REGULAR), headerY + 3F,
				ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);

		if(favoriteMods.isEmpty()) {

			float centerY = layout.listTop + (layout.listHeight / 2F);

			nvg.drawCenteredText(Icon.STAR, contentX + (contentWidth / 2F), centerY - 44F,
					ModMenuTheme.applyAlpha(ModMenuTheme.onSurfaceVariant(), 55), 30F, Fonts.ICON);
			nvg.drawCenteredText(TranslateText.FAVORITE_EMPTY.getText(), contentX + (contentWidth / 2F), centerY - 6F,
					ModMenuTheme.onSurfaceVariant(), 10.5F, Fonts.MEDIUM);
			nvg.drawCenteredText(TranslateText.FAVORITE_EMPTY_HINT.getText(), contentX + (contentWidth / 2F), centerY + 12F,
					ModMenuTheme.applyAlpha(ModMenuTheme.onSurfaceVariant(), 150), 9F, Fonts.REGULAR);

		}else {

			int capacity = layout.columns * layout.rowsPerColumn;

			for(int i = 0; i < favoriteMods.size() && i < capacity; i++) {

				Mod m = favoriteMods.get(i);
				float rowX = layout.rowX(i);
				float rowY = layout.rowY(i);
				float animation = m.getAnimation().getValue();
				boolean hovered = MouseUtils.isInside(mouseX, mouseY, rowX, rowY, layout.rowWidth, FAVORITE_ROW_HEIGHT);

				m.getAnimation().setAnimation(m.isToggled() ? 1.0F : 0.0F, 11);

				nvg.drawRoundedRect(rowX, rowY, layout.rowWidth, FAVORITE_ROW_HEIGHT, 8,
						hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());

				if(hovered) {
					nvg.drawOutlineRoundedRect(rowX, rowY, layout.rowWidth, FAVORITE_ROW_HEIGHT, 8, 1F,
							ColorUtils.applyAlpha(currentColor.getColor1(), 130));
				}

				nvg.drawRoundedRect(rowX + 8F, rowY + 7F, 16F, 16F, 5F,
						ModMenuTheme.mix(ModMenuTheme.surfaceContainerHigh(), ModMenuTheme.primaryContainer(), animation));
				// 与模组卡片同一套居中约定：drawCenteredTextMid 按字形包围盒居中，
				// 换 Material Symbols 字体后不需要再手调 y 偏移。
				nvg.drawCenteredTextMid(getModIcon(m), rowX + 16F, rowY + 15F,
						ModMenuTheme.mix(ModMenuTheme.onSurfaceVariant(), ModMenuTheme.onPrimaryContainer(), animation), 11F, Fonts.MATERIAL);

				nvg.drawText(nvg.getLimitText(m.getName(), 10.5F, Fonts.MEDIUM, layout.rowWidth - 110F), rowX + 32F, rowY + 10F,
						ModMenuTheme.onSurface(), 10.5F, Fonts.MEDIUM);

				nvg.drawCenteredTextMid(MaterialIcons.STAR, rowX + layout.rowWidth - 44F, rowY + 15F,
						ModMenuTheme.primary(), 14F, Fonts.MATERIAL);

				float switchX = rowX + layout.rowWidth - 34F;
				float switchY = rowY + 6F;

				nvg.drawRoundedRect(switchX, switchY, 30, 16, 8,
						ModMenuTheme.mix(ModMenuTheme.surfaceContainerHighest(), ModMenuTheme.primary(), animation));
				nvg.drawRoundedRect(switchX + 2 + (animation * 14), switchY + 2, 12, 12, 6, Color.WHITE);
			}
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		if(mouseButton != 0) {
			return;
		}

		FavoriteLayout layout = getFavoriteLayout();
		float contentX = getX() + MARGIN;
		float contentWidth = getWidth() - (MARGIN * 2F);

		// 顶部右侧的更新卡片：点开全部更新内容的悬浮窗
		float cardY = getY() + 16F;
		float cardBottom = getY() + getHeight() - 16F;
		float cardHeight = cardBottom - cardY;
		float cardHeroHeight = clamp(cardHeight * 0.42F, 148F, 196F);
		float cardHeroWidth = Math.max(300F, Math.min(contentWidth - 260F, (contentWidth - GAP) * 0.58F));
		float cardWidth = contentWidth - cardHeroWidth - GAP;
		float cardX = contentX + cardHeroWidth + GAP;

		if(MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, cardHeroHeight)) {
			getParent().openChangelogOverlay();
			return;
		}

		if(!MouseUtils.isInside(mouseX, mouseY, contentX, layout.top, contentWidth, layout.height)) {
			return;
		}

		ArrayList<Mod> favoriteMods = getFavoriteMods();
		int capacity = layout.columns * layout.rowsPerColumn;

		for(int i = 0; i < favoriteMods.size() && i < capacity; i++) {

			Mod m = favoriteMods.get(i);
			float rowX = layout.rowX(i);
			float rowY = layout.rowY(i);

			if(!MouseUtils.isInside(mouseX, mouseY, rowX, rowY, layout.rowWidth, FAVORITE_ROW_HEIGHT)) {
				continue;
			}

			if(MouseUtils.isInside(mouseX, mouseY, rowX + layout.rowWidth - 54F, rowY + 3F, 22F, FAVORITE_ROW_HEIGHT - 6F)) {
				MenuPrefs.getInstance().toggleFavorite(m.getNameKey());
			}else {
				m.toggle();
			}

			return;
		}

		// Header or empty space: open the complete favourite list.
		openFavoriteList();
	}

	private void openFavoriteList() {

		Category moduleCategory = getParent().getCategoryByClass(ModuleCategory.class);

		if(moduleCategory instanceof ModuleCategory) {
			((ModuleCategory) moduleCategory).setFilterCategory(ModCategory.FAVORITE);
			getParent().setCurrentCategory(moduleCategory);
			moduleCategory.scroll.setScrollPosition(0);
		}
	}

	private ArrayList<Mod> getFavoriteMods() {

		ArrayList<Mod> result = new ArrayList<Mod>();
		List<String> favorites = MenuPrefs.getInstance().getFavorites();
		ArrayList<Mod> mods = Mix.getInstance().getModManager().getMods();

		for(String nameKey : favorites) {

			if(nameKey == null) {
				continue;
			}

			for(Mod m : mods) {

				if(nameKey.equals(m.getNameKey()) && !m.isHide()) {
					result.add(m);
					break;
				}
			}
		}

		return result;
	}

	private String getModIcon(Mod m) {

		Category moduleCategory = getParent().getCategoryByClass(ModuleCategory.class);

		if(moduleCategory instanceof ModuleCategory) {
			return ((ModuleCategory) moduleCategory).getModIconFor(m);
		}

		return Icon.SLIDERS;
	}

	private FavoriteLayout getFavoriteLayout() {

		FavoriteLayout layout = new FavoriteLayout();

		float contentY = getY() + 16F;
		float contentWidth = getWidth() - (MARGIN * 2F);
		float contentBottom = getY() + getHeight() - 16F;
		float contentHeight = contentBottom - contentY;
		float heroHeight = clamp(contentHeight * 0.42F, 148F, 196F);

		layout.top = contentY + heroHeight + GAP;
		layout.height = contentBottom - layout.top;
		layout.listTop = layout.top + FAVORITE_HEADER;
		layout.listHeight = Math.max(0F, layout.top + layout.height - 10F - layout.listTop);
		layout.columns = Math.max(1, (int) ((contentWidth - 36F + FAVORITE_COLUMN_GAP) / 220F));
		layout.rowsPerColumn = Math.max(1, (int) ((layout.listHeight + FAVORITE_ROW_GAP) / (FAVORITE_ROW_HEIGHT + FAVORITE_ROW_GAP)));
		layout.rowWidth = (contentWidth - 36F - (FAVORITE_COLUMN_GAP * (layout.columns - 1))) / layout.columns;
		layout.contentX = getX() + MARGIN;

		return layout;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private int getTotalMods() {

		Category moduleCategory = getParent().getCategoryByClass(ModuleCategory.class);

		if(moduleCategory instanceof ModuleCategory) {
			return ((ModuleCategory) moduleCategory).getTotalMods();
		}

		return Mix.getInstance().getModManager().getMods().size();
	}

	private class FavoriteLayout {

		private float contentX;
		private float top;
		private float height;
		private float listTop;
		private float listHeight;
		private int columns;
		private int rowsPerColumn;
		private float rowWidth;

		private float rowX(int index) {
			return contentX + 18F + ((index / rowsPerColumn) * (rowWidth + FAVORITE_COLUMN_GAP));
		}

		private float rowY(int index) {
			return listTop + ((index % rowsPerColumn) * (FAVORITE_ROW_HEIGHT + FAVORITE_ROW_GAP));
		}
	}
}
