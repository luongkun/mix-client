package mix.client.mixui.gui.modmenu.category.impl;
import java.awt.Color;
import java.io.File;
import org.lwjgl.input.Keyboard;
import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.CategoryIslandRenderer;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.file.FileManager;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.profile.Profile;
import mix.client.mixui.management.profile.ProfileIcon;
import mix.client.mixui.management.profile.ProfileManager;
import mix.client.mixui.management.profile.ProfileType;
import mix.client.mixui.ui.comp.impl.field.CompTextBox;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.SearchUtils;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.SmoothStepAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;
public class ProfileCategory extends Category {
	// 分类标签外面那座悬浮小岛，逻辑在 CategoryIslandRenderer 里，四个页面共用
	private final CategoryIslandRenderer chipIsland = new CategoryIslandRenderer();
	private static final float MARGIN = 24F;
	private static final float CARD_HEIGHT = 84F;
	private static final float CARD_GAP = 12F;
	private static final float CHIP_HEIGHT = 26F;
	private ProfileType currentType;
	private Animation profileAnimation;
	private boolean openProfile;
	private ProfileIcon currentIcon;
	private CompTextBox nameBox = new CompTextBox();
	private CompTextBox serverIpBox = new CompTextBox();
	public ProfileCategory(GuiModMenu parent) {
		super(parent, TranslateText.PROFILE, Icon.EDIT, true);
	}
	@Override
	public void onShown() {
		// 从左侧栏切回这个分类时重播一次圆环入场动画
		chipIsland.reset();
	}
	@Override
	public void initGui() {
		currentType = ProfileType.ALL;
		currentIcon = ProfileIcon.COMMAND;
		openProfile = false;
		profileAnimation = new SmoothStepAnimation(240, 1.0);
		profileAnimation.setValue(1.0);
	}
	@Override
	public void initCategory() {
		// 滚动位置由 GuiModMenu 按分类记忆并恢复
		openProfile = false;
		profileAnimation = new SmoothStepAnimation(240, 1.0);
		profileAnimation.setValue(1.0);
		setCanClose(true);
	}
	@Override
	public boolean isSubPageOpen() {
		return openProfile;
	}
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		profileAnimation.setDirection(openProfile ? Direction.BACKWARDS : Direction.FORWARDS);
		if(profileAnimation.isDone(Direction.FORWARDS)) {
			nameBox.setText("");
			serverIpBox.setText("");
			setCanClose(true);
		}
		// 整个分类页裁在内容区里：第二层滑入时不能压到左侧导航栏，也不能溢出菜单右边缘。
		nvg.save();
		nvg.scissor(getX(), getY(), getWidth(), getHeight());
		// 第一层（配置列表）固定不动，不做任何位移动画。
		// 之前第一层会跟着往左整页滑出，滑出途中越过左侧导航栏，看起来像"从栏上划出去"；
		// 现在它只安静待在原地，由第二层的整页不透明底色把它盖住。
		drawBrowser(mouseX, mouseY, partialTicks);
		// 第二层（新增配置）只做位移动画：从右侧滑入盖住第一层。
		if(openProfile || !profileAnimation.isDone(Direction.FORWARDS)) {
			nvg.save();
			nvg.translate((float) profileAnimation.getValue() * getWidth(), 0);
			drawAddProfile(mouseX, mouseY, partialTicks);
			nvg.restore();
		}
		nvg.restore();
	}
	private void drawBrowser(int mouseX, int mouseY, float partialTicks) {
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ProfileManager profileManager = instance.getProfileManager();
		ColorManager colorManager = instance.getColorManager();
		AccentColor accent = colorManager.getCurrentColor();
		float chipStartX = getX() + MARGIN;
		float chipX = chipStartX;
		float chipY = getY() + 10F;
		// 小岛宽度完全由标签本身决定：先量一遍总宽。绘制放到列表之后，
		// 小岛才会浮在卡片上层，而不是被卡片顶边拼成一条横栏。
		float chipRowWidth = 0F;
		for(ProfileType type : ProfileType.values()) {
			chipRowWidth += nvg.getTextWidth(type.getName(), 10.5F, Fonts.MEDIUM) + 26F + 8F;
		}
		chipRowWidth -= 8F;
		int columns = getColumns();
		float cardWidth = (getWidth() - MARGIN * 2F - CARD_GAP * (columns - 1)) / columns;
		// 视口上沿 = 小岛底边（且一定落在顶栏磨砂 ramp 落地之后）：裁在这里，
		// 遮挡带的首行 alpha 才和面板相等，接缝不会读成一条横线。
		float listTop = CategoryIslandRenderer.viewportTop(chipY, CHIP_HEIGHT);
		// 内容顶端留一段与胶囊等高的空白，它会随内容一起滚走。
		float cardsTop = CategoryIslandRenderer.contentTop(chipY, CHIP_HEIGHT);
		float viewportHeight = getY() + getHeight() - 12F - listTop;
		int visible = 0;
		for(Profile p : profileManager.getProfiles()) {
			if(!filter(p)) {
				visible++;
			}
		}
		int rows = (visible + columns - 1) / columns;
		float contentHeight = CategoryIslandRenderer.topSpacer(CHIP_HEIGHT)
				+ (rows == 0 ? 0 : rows * (CARD_HEIGHT + CARD_GAP) - CARD_GAP);
		scroll.setMaxScroll(Math.max(0, contentHeight - viewportHeight));
		// 裁剪上沿严格等于视口上沿，卡片向上滚时被小岛盖住，而不是在
		// 视口上沿被硬切成一条横贯内容区的水平分界。
		nvg.save();
		nvg.scissor(getX(), listTop, getWidth(), viewportHeight);
		nvg.translate(0, scroll.getValue());
		int index = 0;
		for(Profile p : profileManager.getProfiles()) {
			if(filter(p)) {
				continue;
			}
			int column = index % columns;
			int row = index / columns;
			float cardX = getX() + MARGIN + column * (cardWidth + CARD_GAP);
			float cardY = cardsTop + row * (CARD_HEIGHT + CARD_GAP);
			if(cardY + scroll.getValue() + CARD_HEIGHT < listTop || cardY + scroll.getValue() > listTop + viewportHeight) {
				index++;
				continue;
			}
			drawProfileCard(nvg, p, cardX, cardY, cardWidth,
					MouseUtils.isInside(mouseX, mouseY, cardX, cardY + scroll.getValue(), cardWidth, CARD_HEIGHT)
							&& !CategoryIslandRenderer.overIsland(mouseX, mouseY, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT),
					accent);
			index++;
		}
		nvg.restore();
		// 列表画完再画小岛：它是浮在列表头上的独立胶囊，卡片从它背后滚过去。
		// 渐隐带铺满内容区全宽：它盖的是行被裁切的那一段，只糊中间会在左右留白。
		CategoryIslandRenderer.drawScrollFade(nvg, getX(), getWidth(), chipY, CHIP_HEIGHT, ModMenuTheme.surface());
		chipIsland.draw(nvg, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT, getX() + getWidth() - MARGIN);
		for(ProfileType type : ProfileType.values()) {
			String name = type.getName();
			float chipWidth = nvg.getTextWidth(name, 10.5F, Fonts.MEDIUM) + 26F;
			boolean selected = type.equals(currentType);
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, chipX, chipY, chipWidth, CHIP_HEIGHT);
			if(selected) {
				nvg.drawGradientRoundedRect(chipX, chipY, chipWidth, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
						accent.getColor1(), accent.getColor2());
				nvg.drawText(name, chipX + 13F, chipY + 8F, Color.WHITE, 10.5F, Fonts.MEDIUM);
			}else {
				nvg.drawRoundedRect(chipX, chipY, chipWidth, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
						hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
				nvg.drawText(name, chipX + 13F, chipY + 8F, ModMenuTheme.onSurfaceVariant(), 10.5F, Fonts.MEDIUM);
			}
			chipX += chipWidth + 8F;
		}
	}

	private void drawProfileCard(NanoVGManager nvg, Profile p, float x, float y, float width, boolean hovered, AccentColor accent) {
		boolean create = p.getId() == 999;
		nvg.drawRoundedRect(x, y, width, CARD_HEIGHT, 12,
				hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
		if(create) {
			nvg.drawRoundedRect(x + 14F, y + 18F, 48F, 48F, 14F, ModMenuTheme.primaryContainer());
			nvg.drawCenteredText(Icon.PLUS, x + 38F, y + 27F, ModMenuTheme.onPrimaryContainer(), 22, Fonts.ICON);
			nvg.drawText(TranslateText.ADD_PROFILE.getText(), x + 74F, y + 24F,
					ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);
			nvg.drawText(TranslateText.PROFILE.getText(), x + 74F, y + 46F,
					ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);
			nvg.drawRoundedRect(x + width - 38F, y + 18F, 24F, 24F, 12F, ModMenuTheme.surfaceContainer());
			nvg.drawCenteredText(Icon.ARROW_RIGHT, x + width - 26F, y + 23F,
					ModMenuTheme.onSurfaceVariant(), 12, Fonts.ICON);
			return;
		}
		if(p.getIcon() != null) {
			nvg.drawRoundedImage(p.getIcon().getIcon(), x + 14F, y + 18F, 48F, 48F, 12F);
		}
		nvg.drawText(nvg.getLimitText(p.getName(), 11.5F, Fonts.MEDIUM, Math.max(60F, width - 160F)),
				x + 74F, y + 20F, ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);
		p.getStarAnimation().setAnimation(p.getType().equals(ProfileType.FAVORITE) ? 1.0F : 0.0F, 16);
		nvg.drawText(p.getType().getName(), x + 74F, y + 42F,
				ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);
		// Star
		float starX = x + width - 62F;
		nvg.drawRoundedRect(starX, y + 18F, 24F, 24F, 12F, ModMenuTheme.surfaceContainer());
		nvg.drawCenteredText(Icon.STAR, starX + 12F, y + 23F, ModMenuTheme.onSurfaceVariant(), 12, Fonts.ICON);
		nvg.drawCenteredText(Icon.STAR_FILL, starX + 12F, y + 23F,
				ColorUtils.applyAlpha(new Color(255, 193, 74), (int) (p.getStarAnimation().getValue() * 255)), 12, Fonts.ICON);
		// Delete
		float trashX = x + width - 32F;
		nvg.drawRoundedRect(trashX, y + 18F, 24F, 24F, 12F, ModMenuTheme.surfaceContainer());
		nvg.drawCenteredText(Icon.TRASH, trashX + 12F, y + 23F, ModMenuTheme.error(), 12, Fonts.ICON);
	}
	private void drawAddProfile(int mouseX, int mouseY, float partialTicks) {
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		AccentColor accent = instance.getColorManager().getCurrentColor();
		float panelX = getX() + MARGIN;
		float panelY = getY() + 8F;
		float panelWidth = getWidth() - MARGIN * 2F;
		float panelHeight = getHeight() - 16F;
		// 不透明页底：下面的浏览器还在往左滑出，不留这一层的话左侧 24px 边距里
		// 会把导航栏和卡片透出来，看起来像第二层没盖住第一层。
		nvg.drawRect(getX(), getY(), getWidth(), getHeight(), ModMenuTheme.surface());
		nvg.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 14, ModMenuTheme.surfaceContainer());
		// Header
		boolean backHovered = MouseUtils.isInside(mouseX, mouseY, panelX + 8F, panelY + 8F, 34F, 34F);
		nvg.drawRoundedRect(panelX + 8F, panelY + 8F, 34F, 34F, 17F,
				backHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
		nvg.drawCenteredText(Icon.ARROW_LEFT, panelX + 25F, panelY + 17F, ModMenuTheme.onSurface(), 16, Fonts.ICON);
		nvg.drawText(TranslateText.ADD_PROFILE.getText(), panelX + 56F, panelY + 10F,
				ModMenuTheme.onSurface(), 15, Fonts.DEMIBOLD);
		nvg.drawText(TranslateText.PROFILE.getText(), panelX + 56F, panelY + 30F,
				ModMenuTheme.onSurfaceVariant(), 9, Fonts.REGULAR);
		nvg.drawRect(panelX + 14F, panelY + 52F, panelWidth - 28F, 1, ModMenuTheme.outlineVariant());
		// Icon picker
		nvg.drawText(TranslateText.ICON.getText(), panelX + 24F, panelY + 74F,
				ModMenuTheme.onSurface(), 11, Fonts.MEDIUM);
		float iconX = 0;
		for(ProfileIcon icon : ProfileIcon.values()) {
			float x = panelX + 24F + iconX;
			float y = panelY + 98F;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, y, 44F, 44F);
			icon.getAnimation().setAnimation(currentIcon.equals(icon) ? 1.0F : 0.0F, 16);
			nvg.drawRoundedRect(x, y, 44F, 44F, 12F,
					hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
			nvg.drawRoundedImage(icon.getIcon(), x + 6F, y + 6F, 32F, 32F, 8F);
			float selected = icon.getAnimation().getValue();
			if(selected > 0.01F) {
				int alpha = (int) (selected * 255);
				nvg.drawGradientOutlineRoundedRect(x, y, 44F, 44F, 12F, 2F,
						ColorUtils.applyAlpha(accent.getColor1(), alpha),
						ColorUtils.applyAlpha(accent.getColor2(), alpha));
			}
			iconX += 56F;
		}
		// Name / server
		float fieldWidth = (panelWidth - 72F) / 2F;
		float fieldY = panelY + 190F;
		float secondX = panelX + 24F + fieldWidth + 24F;
		nvg.drawText(TranslateText.NAME.getText(), panelX + 24F, panelY + 164F,
				ModMenuTheme.onSurface(), 11, Fonts.MEDIUM);
		nvg.drawText(TranslateText.SERVER_IP.getText(), secondX, panelY + 164F,
				ModMenuTheme.onSurface(), 11, Fonts.MEDIUM);
		nameBox.setPosition(panelX + 24F, fieldY, fieldWidth, 30F);
		nameBox.setDefaultText(TranslateText.NAME.getText());
		nameBox.draw(mouseX, mouseY, partialTicks);
		serverIpBox.setPosition(secondX, fieldY, fieldWidth, 30F);
		serverIpBox.setDefaultText(TranslateText.SERVER_IP.getText());
		serverIpBox.draw(mouseX, mouseY, partialTicks);
		// Create button
		float buttonWidth = 120F;
		float buttonX = panelX + panelWidth - buttonWidth - 24F;
		float buttonY = panelY + panelHeight - 52F;
		boolean buttonHovered = MouseUtils.isInside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 34F);
		if(!nameBox.getText().isEmpty()) {
			nvg.drawGradientRoundedRect(buttonX, buttonY, buttonWidth, 34F, 17F,
					accent.getColor1(), accent.getColor2());
			nvg.drawCenteredText(TranslateText.CREATE.getText(), buttonX + buttonWidth / 2F, buttonY + 11F,
					Color.WHITE, 10.5F, Fonts.MEDIUM);
		}else {
			nvg.drawRoundedRect(buttonX, buttonY, buttonWidth, 34F, 17F,
					buttonHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
			nvg.drawCenteredText(TranslateText.CREATE.getText(), buttonX + buttonWidth / 2F, buttonY + 11F,
					ModMenuTheme.onSurfaceVariant(), 10.5F, Fonts.MEDIUM);
		}
	}
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		Mix instance = Mix.getInstance();
		ProfileManager profileManager = instance.getProfileManager();
		if(openProfile) {
			if(!profileAnimation.isDone(Direction.BACKWARDS)) {
				return;
			}
			if(mouseButton == 3) {
				openProfile = false;
				return;
			}
			if(mouseButton != 0) {
				return;
			}
			float panelX = getX() + MARGIN;
			float panelY = getY() + 8F;
			float panelWidth = getWidth() - MARGIN * 2F;
			float panelHeight = getHeight() - 16F;
			if(MouseUtils.isInside(mouseX, mouseY, panelX + 6F, panelY + 6F, 38F, 38F)) {
				openProfile = false;
				return;
			}
			float iconX = 0;
			for(ProfileIcon icon : ProfileIcon.values()) {
				if(MouseUtils.isInside(mouseX, mouseY, panelX + 24F + iconX, panelY + 98F, 44F, 44F)) {
					currentIcon = icon;
					return;
				}
				iconX += 56F;
			}
			nameBox.mouseClicked(mouseX, mouseY, mouseButton);
			serverIpBox.mouseClicked(mouseX, mouseY, mouseButton);
			float buttonX = panelX + panelWidth - 144F;
			float buttonY = panelY + panelHeight - 52F;
			if(MouseUtils.isInside(mouseX, mouseY, buttonX, buttonY, 120F, 34F) && !nameBox.getText().isEmpty()) {
				FileManager fileManager = instance.getFileManager();
				String serverIp = serverIpBox.getText().isEmpty() ? "" : serverIpBox.getText();
				profileManager.save(new File(fileManager.getProfileDir(), nameBox.getText() + ".json"), serverIp, ProfileType.ALL, currentIcon);
				profileManager.loadProfiles(false);
				openProfile = false;
			}
			return;
		}
		if(mouseButton != 0 || !profileAnimation.isDone(Direction.FORWARDS)) {
			return;
		}
		NanoVGManager nvg = instance.getNanoVGManager();
		float chipX = getX() + MARGIN;
		float chipY = getY() + 10F;
		for(ProfileType type : ProfileType.values()) {
			float chipWidth = nvg.getTextWidth(type.getName(), 10.5F, Fonts.MEDIUM) + 26F;
			if(MouseUtils.isInside(mouseX, mouseY, chipX, chipY, chipWidth, CHIP_HEIGHT)) {
				currentType = type;
				scroll.setScrollPosition(0);
				return;
			}
			chipX += chipWidth + 8F;
		}
		int columns = getColumns();
		float cardWidth = (getWidth() - MARGIN * 2F - CARD_GAP * (columns - 1)) / columns;
		float listTop = CategoryIslandRenderer.viewportTop(getY() + 10F, CHIP_HEIGHT);
		float cardsTop = CategoryIslandRenderer.contentTop(getY() + 10F, CHIP_HEIGHT);
		float viewportHeight = getY() + getHeight() - 12F - listTop;
		if(!MouseUtils.isInside(mouseX, mouseY, getX(), cardsTop, getWidth(), listTop + viewportHeight - cardsTop)) {
			return;
		}
		int index = 0;
		for(Profile p : profileManager.getProfiles()) {
			if(filter(p)) {
				continue;
			}
			int column = index % columns;
			int row = index / columns;
			float cardX = getX() + MARGIN + column * (cardWidth + CARD_GAP);
			float cardY = cardsTop + row * (CARD_HEIGHT + CARD_GAP) + scroll.getValue();
			if(MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, CARD_HEIGHT)) {
				boolean favorite = p.getId() != 999 && MouseUtils.isInside(mouseX, mouseY, cardX + cardWidth - 62F, cardY + 16F, 28F, 28F);
				boolean delete = p.getId() != 999 && MouseUtils.isInside(mouseX, mouseY, cardX + cardWidth - 32F, cardY + 16F, 28F, 28F);
				if(p.getId() == 999) {
					openProfile = true;
					setCanClose(false);
					profileAnimation.setDirection(Direction.BACKWARDS);
					return;
				}
				if(favorite) {
					if(p.getType().equals(ProfileType.FAVORITE)) {
						p.setType(ProfileType.ALL);
					}else {
						p.setType(ProfileType.FAVORITE);
					}
					profileManager.save(p.getJsonFile(), p.getServerIp(), p.getType(), p.getIcon());
					return;
				}
				if(delete) {
					profileManager.delete(p);
					return;
				}
				instance.getModManager().disableAll();
				profileManager.load(p.getJsonFile());
				return;
			}
			index++;
		}
	}
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if(openProfile) {
			nameBox.keyTyped(typedChar, keyCode);
			serverIpBox.keyTyped(typedChar, keyCode);
			if(keyCode == Keyboard.KEY_ESCAPE) {
				openProfile = false;
			}
		}
	}
	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		if(openProfile) {
			nameBox.mouseReleased(mouseX, mouseY, mouseButton);
			serverIpBox.mouseReleased(mouseX, mouseY, mouseButton);
		}
	}
	private int getColumns() {
		return Math.max(1, (int) ((getWidth() - MARGIN * 2F + CARD_GAP) / 224F));
	}
	private boolean filter(Profile p) {
		if(currentType.equals(ProfileType.FAVORITE) && !p.getType().equals(ProfileType.FAVORITE) && p.getId() != 999) {
			return true;
		}
		String search = this.getSearchBox().getText();
		if(!search.isEmpty() && !SearchUtils.isSimillar(p.getName(), search) && p.getId() != 999) {
			return true;
		}
		return false;
	}}