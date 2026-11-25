package mix.client.mixui.gui.modmenu;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.GuiEditHUD;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.gui.modmenu.category.impl.CosmeticsCategory;
import mix.client.mixui.gui.modmenu.category.impl.HomeCategory;
import mix.client.mixui.gui.modmenu.category.impl.ModuleCategory;
import mix.client.mixui.gui.modmenu.category.impl.MusicCategory;
import mix.client.mixui.gui.modmenu.category.impl.ProfileCategory;
import mix.client.mixui.gui.modmenu.category.impl.ScreenshotCategory;
import mix.client.mixui.gui.modmenu.category.impl.SettingCategory;
import mix.client.mixui.management.changelog.Changelog;
import mix.client.mixui.management.changelog.ChangelogType;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.color.palette.ColorPalette;
import mix.client.mixui.management.color.palette.ColorType;
import mix.client.mixui.management.event.impl.EventRenderNotification;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Font;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.profile.MenuPrefs;
import mix.client.mixui.ui.comp.impl.field.CompSearchBox;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.MathUtils;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.easing.EaseBackIn;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import mix.client.mixui.utils.buffer.ScreenAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;
import mix.client.mixui.utils.mouse.Scroll;
import mix.client.mixui.utils.render.BlurUtils;
import mix.client.mixui.debug.UiCapture;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class GuiModMenu extends GuiScreen {

	private static final int DESIGN_WIDTH = 938;
	private static final int DESIGN_HEIGHT = 580;

	// Alpha at the very top edge of the panel. The panel fill ramps from this to
	// fully opaque over one header height, so the top of the menu reads as glass
	// over the blurred game rather than as a solid blank slab.
	//
	// 只降透明度是不够的：透出来的其实是游戏画面（天空/山），颜色本身是蓝绿的，
	// 于是顶部读成「一块透明天窗」而不是「磨砂玻璃」。所以透明度之外还要同时
	// 抬高色温（见 HEADER_FROST_MIX），让顶部是牛奶白而不是天空色。
	private static final int HEADER_GLASS_ALPHA = 0;

	// 顶部磨砂的「牛奶白」掺入量。t=0 处把面板色朝白色拉这么多，往下按 (1-t)^1.6
	// 收回到面板本色 —— t=1 时掺入量正好是 0，落底色 == 面板本色，所以下沿无接缝。
	// 浅色主题要够白才读成磨砂，深色主题只提一点，否则会发灰。
	private static final float HEADER_FROST_MIX_LIGHT = 0.62F;
	private static final float HEADER_FROST_MIX_DARK = 0.26F;

	// Vertical centre shared by the page title, the search field and the version
	// badge. Everything in the header is placed off this single line.
	private static final float HEADER_CENTER_Y = 31F;

	// 冰霜带必须严格停在 header 下沿，不能继续往内容区里渗。
	// 之前把它做成 headerHeight + 40，结果渐隐尾巴正好压在分类胶囊那一行上：
	// 胶囊是后画的，会把尾巴从中间盖掉，剩下「胶囊上方一段渐变 + 胶囊下方一段」
	// 两条水平边界 —— 比原来更糟。现在曲线在 header 下沿就以斜率 0 落地，
	// 落地处 alpha 已经等于 255，与下方实色面完全同色，所以看不到任何接缝。
	private static final float HEADER_FADE_TAIL = 0F;

	/**
	 * 顶部磨砂的纵向深度（设计单位，从面板上沿往下）。
	 * header 高约 65，这里多给 30，让模糊带越过标题/搜索那一行、
	 * 正好在分类胶囊上方化完，不与胶囊抢同一行。
	 */
	private static final float HEADER_FROST_DEPTH = 30F;

	/** 顶部磨砂的最大混合量（最上面一行有多糊）。0.92 = 几乎完全用模糊结果。 */
	private static final float HEADER_FROST_STRENGTH = 0.92F;

	// Number of short gradients the header ramp is stitched from. More segments
	// = smoother curve, all drawing the same colour so no seam can show.
	private static final int HEADER_FADE_SEGMENTS = 15;

	// CJK glyphs sit high inside their font box: the measured box centre is a few
	// units below the ink centre, so a box-centred label rides above the icon it
	// shares a line with. This is the measured pixel difference (4 physical px at
	// the default design scale) expressed in design units.
	// 老值 3.5F 是给 drawTextMid 早期「按 size/2 摆基线」的补偿量。现在
	// drawTextMid 按墨迹包围盒把盒中心对到中心线，和图标用的是同一套坐标系，
	// 再加偏移反而会把标题压到图标下面去，所以归零。
	private static final float TITLE_TEXT_INK_NUDGE = 0.0F;

	private Animation introAnimation;
	private int x, y, width, height;
	private int railWidth, headerHeight;
	private int searchWidth;

	// The menu is laid out in the 938x580 8.0 design space.  NanoVG's default setup
	// already scales logical GUI coordinates by the Minecraft GUI factor, so we
	// apply one extra transform when drawing and keep the screen-space rectangle
	// separately for ScreenAnimation's framebuffer composite.
	private float designScale = 1.0F;
	private float windowScaleLimit = 1.0F;
	private float guiScale = 1.0F;
	private float screenX, screenY, screenWidth, screenHeight;

	private ArrayList<Category> categories = new ArrayList<Category>();
	private Category currentCategory;

	private SimpleAnimation moveAnimation = new SimpleAnimation();

	private ScreenAnimation screenAnimation = new ScreenAnimation();

	private Scroll scroll = new Scroll();

	private HashMap<Category, Float> categoryScrollPositions = new HashMap<Category, Float>();

	private boolean toEditHUD, canClose;

	private CompSearchBox searchBox = new CompSearchBox();

	// Hover drivers for the header glow; one per header element so the title and
	// the right-hand capsule light up independently.
	private SimpleAnimation titleHoverAnimation = new SimpleAnimation();
	private SimpleAnimation badgeHoverAnimation = new SimpleAnimation();

	// 右上角只有一格位置，两个元素轮流用它：能搜索的分类显示搜索框，
	// 其它分类（以及二级页）显示版本徽章。原来是 if/else 硬切 ——
	// 从首页点进「模组」的那一帧，徽章整块消失、搜索框整块出现，观感像闪了一下。
	// 1 = 搜索框，0 = 徽章；两者按同一条曲线交叉淡化（三阶 smoothstep，两端斜率为 0）。
	private SimpleAnimation headerSwapAnimation = new SimpleAnimation(1.0F);

	// 搜索框当前帧的横向偏移。mouseClicked 要用它把鼠标坐标换算回
	// 「搜索框静止时的坐标系」，否则淡入淡出的这 200ms 里点击会错位。
	private float headerSwapDx;

	// 更新内容悬浮窗
	private boolean changelogOverlayOpen;
	private SimpleAnimation changelogOverlayAnimation = new SimpleAnimation();
	private Scroll changelogOverlayScroll = new Scroll();
	private ArrayList<ChangelogRow> changelogRows = new ArrayList<ChangelogRow>();

	public GuiModMenu() {

		categories.add(new HomeCategory(this));
		categories.add(new ModuleCategory(this));

		// The music player card is only hidden on the home page; the music
		// category itself stays in the navigation.
		categories.add(new MusicCategory(this));

		categories.add(new ProfileCategory(this));
		categories.add(new CosmeticsCategory(this));
		categories.add(new ScreenshotCategory(this));
		categories.add(new SettingCategory(this));

		currentCategory = getCategoryByClass(HomeCategory.class);
	}

	@Override
	public void initGui() {

		updateLayout();

		// Re-read mixclient/changelog.txt so edits show up without restarting the game.
		Mix.getInstance().getChangelogManager().reload();

		introAnimation = new EaseBackIn(320, 1.0F, 2.0F);
		introAnimation.setDirection(Direction.FORWARDS);

		for(Category c : categories) {
			c.initGui();
		}

		// Restore the saved scroll position (sub-pages keep their own scroll state).
		scroll.setScrollPosition(getSavedScroll(currentCategory));
		searchBox.setText("");
		searchBox.setFocused(false);
		toEditHUD = false;
		canClose = true;

		// 重新打开菜单时不要带着上一次的更新悬浮窗
		changelogOverlayOpen = false;
		changelogOverlayAnimation.setValue(0.0F);
		changelogOverlayScroll.resetAll();

		// 每次打开菜单都让当前分类的标签圆环从头描一遍
		if(currentCategory != null) {
			currentCategory.onShown();
		}
	}

	// The Appearance settings call this after the UI scale changed so the new
	// size applies immediately, without closing and reopening the menu.
	public float getDesignScale() {
		return designScale;
	}

	// Largest scale that still fits the current window; the Appearance settings
	// use it to explain why a bigger choice is not applied verbatim.
	public float getWindowScaleLimit() {
		return windowScaleLimit;
	}

	public void refreshLayout() {
		updateLayout();
	}

	// Recomputes the design scale and the screen-space rectangle.  A larger user
	// scale is only honoured while the panel still fits inside the window.
	private void updateLayout() {

		ScaledResolution sr = new ScaledResolution(mc);

		float windowScale = Math.min((mc.displayWidth - 24) / (float) DESIGN_WIDTH,
				(mc.displayHeight - 24) / (float) DESIGN_HEIGHT);

		windowScaleLimit = windowScale;
		designScale = Math.max(0.1F, Math.min(MenuPrefs.getInstance().getMenuScale(), windowScale));
		guiScale = sr.getScaleFactor();

		// Design-space rectangle used by the menu layout.
		width = DESIGN_WIDTH;
		height = DESIGN_HEIGHT;
		x = 0;
		y = 0;

		// Screen-space (Minecraft scaled-GUI) rectangle used by ScreenAnimation.
		screenWidth = width * designScale / guiScale;
		screenHeight = height * designScale / guiScale;
		screenX = (sr.getScaledWidth() - screenWidth) / 2F;
		screenY = (sr.getScaledHeight() - screenHeight) / 2F;

		railWidth = Math.max(74, Math.round(width * 0.096F));
		// Slightly tighter than the old 0.131 factor: the header band was carrying a
		// lot of empty space above the first row, so it moves up instead of growing.
		headerHeight = Math.max(56, Math.round(height * 0.112F));
		searchWidth = Math.max(150, Math.min(260, Math.round((width - railWidth) / 3.2F)));
	}

	public void setCurrentCategory(Category category) {

		if(category == null || category.equals(currentCategory)) {
			return;
		}

		saveScrollPosition(currentCategory);
		currentCategory = category;
		searchBox.setText("");
		searchBox.setFocused(false);
		setCanClose(true);
		scroll.setScrollPosition(getSavedScroll(category));
		category.onShown();
	}

	public void saveScrollPosition(Category category) {

		if(category != null) {
			categoryScrollPositions.put(category, scroll.getScrollPosition());
		}
	}

	public float getSavedScroll(Category category) {

		Float saved = categoryScrollPositions.get(category);
		return saved == null ? 0.0F : saved;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();

		int designMouseX = toDesignX(mouseX);
		int designMouseY = toDesignY(mouseY);

		// 顶部渐变磨砂：模糊本身是真的（shader 里同时拿原图和模糊图，按屏幕纵向
		// smoothstep 权重插值），遮罩从屏幕顶一路铺到 header 下沿再往下一点，
		// 所以「上面是磨砂玻璃、往下化开」是逐像素连续的，不存在任何硬边。
		ScaledResolution blurRes = new ScaledResolution(mc);
		float blurRadius = (float) (Math.min(introAnimation.getValue(), 1) * 20) + 1F;

		if (UiCapture.isBlurProbeOff()) {
			blurRadius = 0F;
		}

		float frostStart = 0F;
		float frostEnd = (screenY + (headerHeight + HEADER_FROST_DEPTH) * designScale / guiScale)
				/ Math.max(1F, blurRes.getScaledHeight());

		BlurUtils.setFrostMask(frostStart, frostEnd, HEADER_FROST_STRENGTH);
		BlurUtils.setFrostInvert(UiCapture.isFrostInverted());
		BlurUtils.drawBlurScreen(blurRadius);

		screenAnimation.wrap(() -> {
			nvg.translate(screenX, screenY);
			nvg.scale(0, 0, designScale / guiScale);
			nvg.drawShadow(0, 0, width, height, 18);
		}, screenX, screenY, screenWidth, screenHeight, 2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1), false);

		screenAnimation.wrap(() -> {
			nvg.translate(screenX, screenY);
			nvg.scale(0, 0, designScale / guiScale);
			drawNanoVG(designMouseX, designMouseY, partialTicks);
		}, screenX, screenY, screenWidth, screenHeight, 2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1), true);

		new EventRenderNotification().call();

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private int toDesignX(int mouseX) {
		return Math.round((mouseX - screenX) * guiScale / designScale);
	}

	private int toDesignY(int mouseY) {
		return Math.round((mouseY - screenY) * guiScale / designScale);
	}

	private void drawNanoVG(int mouseX, int mouseY, float partialTicks) {

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		ColorPalette palette = colorManager.getPalette();
		AccentColor currentColor = colorManager.getCurrentColor();

		int contentX = x + railWidth;
		int contentY = y + headerHeight;
		int contentWidth = width - railWidth;
		int contentHeight = height - headerHeight;

		// Anything above the content rectangle (page header, search box, divider) is a
		// mask over the scrolled list, so the list must not react to the cursor there.
		boolean insideContent = MouseUtils.isInside(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight);
		int contentMouseX = insideContent ? mouseX : -10000;
		int contentMouseY = insideContent ? mouseY : -10000;

		if(introAnimation.isDone(Direction.BACKWARDS)) {
			mc.displayGuiScreen(toEditHUD ? new GuiEditHUD(true) : null);
		}

		// Main surfaces. The top band used to be one short vertical fade that hit
		// full opacity exactly at the header's bottom edge, so the header and the
		// content area met along a straight line - two slabs butted together.
		//
		// 现在这条带子同时做两件事：顶部掺白成磨砂（frost），以及 alpha 从
		// HEADER_GLASS_ALPHA 缓升到 255。曲线用 smoothstep，两端斜率都是 0，
		// 落底处颜色和 alpha 都已经等于下方实色面，所以下沿既没有台阶也没有接缝。
		//
		// 范围严格停在 header 下沿（HEADER_FADE_TAIL = 0）：再往下压就会和
		// 分类胶囊抢同一行，胶囊是后画的，会把渐变从中间切开。
		//
		// Nothing here is a divider: no line, no full-width band, no extra
		// container. It is the same panel fill, only with its own alpha ramping.
		// 渐变带必须和模糊遮罩覆盖同一段纵向区间，否则「模糊还在跑、面板已经实色」
		// 的那一段会把磨砂整块盖回去，读起来就是一块白板。
		float headerFade = headerHeight + HEADER_FROST_DEPTH + HEADER_FADE_TAIL;

		drawHeaderRamp(nvg, x, y, width, headerFade, 18, 18, ModMenuTheme.surface());

		nvg.drawRoundedRectVarying(x, y + headerFade - 1F, width, height - headerFade + 1F, 0, 0, 18, 18, ModMenuTheme.surface());

		drawHeaderRamp(nvg, x, y, railWidth, headerFade, 18, 18, ModMenuTheme.surfaceContainerLow());

		nvg.drawRoundedRectVarying(x, y + headerFade - 1F, railWidth, height - headerFade + 1F, 0, 0, 18, 0, ModMenuTheme.surfaceContainerLow());
		// 内容区顶边不再画任何横向分隔线：分类页顶部那座悬浮小岛本身就交代了边界，
		// 再加一条横贯整幅的线就会和小岛拼成一整个大容器（这正是之前被吐槽的观感）。

		// Logo / version
		int logoSize = Math.min(42, railWidth - 30);
		int logoX = x + (railWidth - logoSize) / 2;
		nvg.drawGradientRoundedRect(logoX, y + 17, logoSize, logoSize, logoSize / 2F, currentColor.getColor1(), currentColor.getColor2());
		// 图标一律走 drawCenteredTextMid（按字形墨迹盒居中）。旧的 drawCenteredText 是
		// 「y += size/2 的基线约定 + 按字宽减半」，对 Material Symbols 这种墨迹在 em 盒里
		// 偏上、且左 bearing 不为 0 的字体，会稳定地偏右上 —— 这就是图标看着不在正中的原因。
		nvg.drawCenteredTextMid(Icon.MIX, logoX + (logoSize / 2F), y + 17 + (logoSize / 2F), Color.WHITE, 20, Fonts.ICON);
		nvg.drawCenteredText(instance.getVersion(), x + (railWidth / 2F), y + 17 + logoSize + 7, ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.MEDIUM);

		// Page header. The title keeps its own silhouette: the glow is only ever
		// stroked around it, never filled behind it, so no new container appears.
		float headerBreath = HeaderGlow.breath();

		float titleIconX = contentX + 24;
		float titleTextX = contentX + 50;
		float titleGlowX = titleIconX - 8F;
		float titleGlowY = y + HEADER_CENTER_Y - 16F;
		float titleGlowW = (titleTextX + nvg.getTextWidth(this.currentCategory.getName(), 22, Fonts.DEMIBOLD)) - titleGlowX + 8F;
		float titleGlowH = 32F;

		titleHoverAnimation.setAnimation(MouseUtils.isInside(mouseX, mouseY, titleGlowX, titleGlowY, titleGlowW, titleGlowH) ? 1.0F : 0.0F, 14);

		float titleIntensity = 0.26F + headerBreath * 0.20F + titleHoverAnimation.getValue() * 0.54F;

		HeaderGlow.draw(nvg, titleGlowX, titleGlowY, titleGlowW, titleGlowH, titleGlowH / 2F, titleIntensity, ModMenuTheme.primary());

		// Icon and label are placed off the same centre line. The icon glyph and the
		// CJK label have very different glyph boxes, so both are centred by their own
		// measured box instead of by the shared size/2 baseline offset - that offset
		// is exactly what made the two look non-parallel.
		nvg.drawTextMid(this.currentCategory.getIcon(), titleIconX, y + HEADER_CENTER_Y, ModMenuTheme.primary(), 18, Fonts.ICON);
		nvg.drawTextMid(this.currentCategory.getName(), titleTextX, y + HEADER_CENTER_Y + TITLE_TEXT_INK_NUDGE, ModMenuTheme.onSurface(), 22, Fonts.DEMIBOLD);

		boolean showSearch = this.currentCategory.isShowSearchBox() && !this.currentCategory.isSubPageOpen();

		headerSwapAnimation.setAnimation(showSearch ? 1.0F : 0.0F, 14);

		float swap = Math.max(0.0F, Math.min(1.0F, headerSwapAnimation.getValue()));

		// 三阶 smoothstep：swap->eased 的两端斜率都是 0，起手和落手都不会「顿一下」。
		// 用 ease-out cubic 直接 1-(1-t)^3 会让起手有个突跳，交叉淡化时两个元素
		// 加起来的总不透明度会瞬间超过 1，白色底上能看出一点亮斑。
		float eased = swap * swap * (3.0F - 2.0F * swap);

		// 搜索框比徽章宽得多，两者的左端不在同一条线上。右端是固定锚点（都对齐到 -24），
		// 所以观感是「同一颗胶囊在原地变宽变窄」，而不是一个飞出去另一个飞进来。
		float searchAlpha = eased;
		float badgeAlpha = 1.0F - eased;

		// 一侧从右边滑进来、另一侧往右边滑出去，位移量 10px，方向一致
		// （都朝右），读起来像同一个东西被替换，而不是两团影子对撞。
		float searchDx = (1.0F - eased) * 10.0F;
		float badgeDx = eased * 10.0F;

		headerSwapDx = searchDx;

		if(searchAlpha > 0.004F) {

			searchBox.setPosition(contentX + contentWidth - searchWidth - 24 + searchDx, Math.round(y + HEADER_CENTER_Y - 15F), searchWidth, 30);

			// 用 nvg 的全局 alpha 做整体淡入淡出：搜索框内部有边框、图标、光标、选中高亮
			// 好几层自己算的 alpha，逐个去乘既漏又容易被它们各自的动画覆盖。
			nvg.save();
			nvg.setAlpha(searchAlpha);
			searchBox.draw(mouseX, mouseY, partialTicks);
			nvg.restore();
		}

		if(badgeAlpha > 0.004F) {

			String version = "MixClient " + instance.getVersion();
			float versionWidth = nvg.getTextWidth(version, 9, Fonts.MEDIUM) + 22;
			float badgeX = contentX + contentWidth - versionWidth - 24 + badgeDx;
			float badgeH = 22F;
			float badgeY = y + HEADER_CENTER_Y - (badgeH / 2F);

			badgeHoverAnimation.setAnimation(MouseUtils.isInside(mouseX, mouseY, badgeX, badgeY, versionWidth, badgeH) ? 1.0F : 0.0F, 14);

			float badgeIntensity = 0.24F + headerBreath * 0.18F + badgeHoverAnimation.getValue() * 0.56F;

			nvg.save();
			nvg.setAlpha(badgeAlpha);
			HeaderGlow.draw(nvg, badgeX, badgeY, versionWidth, badgeH, badgeH / 2F, badgeIntensity, ModMenuTheme.primary());

			nvg.drawRoundedRect(badgeX, badgeY, versionWidth, badgeH, badgeH / 2F, ModMenuTheme.surfaceContainer());
			nvg.drawCenteredTextMid(version, contentX + contentWidth - 24 - (versionWidth / 2F) + badgeDx, badgeY + (badgeH / 2F), ModMenuTheme.onSurfaceVariant(), 9, Fonts.MEDIUM);
			nvg.restore();
		}

		// Navigation rail items
		float navStart = y + 92;
		float navPitch = Math.max(43F, Math.min(56F, (height - 196F) / Math.max(1F, categories.size())));

		moveAnimation.setAnimation(categories.indexOf(currentCategory) * navPitch, 18);

		nvg.save();
		nvg.scissor(x, y + 76, railWidth, height - 178);

		for(int i = 0; i < categories.size(); i++) {

			Category c = categories.get(i);
			float itemY = navStart + (i * navPitch);
			boolean selected = c.equals(currentCategory);
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, x + (railWidth - 58) / 2F, itemY - 2, 58, 38);

			c.getCategoryAnimation().setAnimation(selected ? 1.0F : 0.0F, 16);

			if(hovered && !selected) {
				nvg.drawRoundedRect(x + (railWidth - 58) / 2F, itemY - 2, 58, 34, 17, ModMenuTheme.applyAlpha(ModMenuTheme.onSurfaceVariant(), 18));
			}

			if(c.getCategoryAnimation().getValue() > 0.01F) {
				nvg.drawRoundedRect(x + (railWidth - 58) / 2F, itemY - 2, 58, 34, 17,
						ModMenuTheme.applyAlpha(ModMenuTheme.primary(), (int) (c.getCategoryAnimation().getValue() * 46)));
				nvg.drawGradientRoundedRect(x + (railWidth - 58) / 2F, itemY - 2, 58, 34, 17,
						ColorUtils.applyAlpha(currentColor.getColor1(), (int) (c.getCategoryAnimation().getValue() * 235)),
						ColorUtils.applyAlpha(currentColor.getColor2(), (int) (c.getCategoryAnimation().getValue() * 235)));
			}

			Color iconColor = selected ? Color.WHITE : ModMenuTheme.onSurfaceVariant();

			// Icons only - the category name under the glyph was removed on request.
			// 命中/选中底块是 itemY-2 起、34 高，中心线就是 itemY+15
			nvg.drawCenteredTextMid(c.getIcon(), x + (railWidth / 2F), itemY + 15, iconColor, 19, Fonts.ICON);
		}

		nvg.restore();

		// HUD editor shortcut
		float editY = y + height - 92;
		boolean editHovered = MouseUtils.isInside(mouseX, mouseY, x + (railWidth - 40) / 2F, editY, 40, 40);
		nvg.drawRoundedRect(x + (railWidth - 40) / 2F, editY, 40, 40, 20,
				editHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
		nvg.drawCenteredTextMid(Icon.LAYOUT, x + (railWidth / 2F), editY + 20, ModMenuTheme.onSurface(), 18, Fonts.ICON);
		nvg.drawCenteredText("HUD", x + (railWidth / 2F), editY + 41, ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.MEDIUM);

		// Current category content
		for(Category c : categories) {

			if(c.equals(currentCategory)) {

				nvg.save();

				if(!c.isInitialized()) {
					c.setInitialized(true);
					c.initCategory();
					searchBox.setText("");
					c.setCanClose(true);
				}

				c.getCategoryAnimation().setAnimation(1.0F, 16);

				nvg.scissor(contentX, contentY, contentWidth, contentHeight);
				nvg.translate(0, 38 - (c.getCategoryAnimation().getValue() * 38));

				c.drawScreen(contentMouseX, contentMouseY, partialTicks);

				nvg.restore();

			}else if(c.isInitialized()) {
				c.setInitialized(false);
			}
		}

		if(changelogOverlayOpen) {
			changelogOverlayScroll.onScroll();
		}else if(!currentCategory.isSubPageOpen() && MouseUtils.isInside(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
			scroll.onScroll();
		}

		scroll.onAnimation();
		changelogOverlayScroll.onAnimation();

		this.drawChangelogOverlay();

		if(currentCategory.isShowSearchBox() && !currentCategory.isSubPageOpen() && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_F)) {
			currentCategory.getSearchBox().setFocused(true);
		}
	}

	/**
	 * 画面板顶部的玻璃过渡。
	 *
	 * <p>为什么不能只用一个 drawVerticalGradient…：NanoVG 的渐变是线性的，
	 * 线性 ramp 无论拉多长，到落底那一段的斜率都还很大，紧跟着就是全实色内容区，
	 * 边界上必然留一个「拐点」。这里把 ramp 拆成若干段、每段按 smoothstep 曲线
	 * 算两端的透明度，拼接出来的整体就是一条两端斜率都为 0 的曲线：
	 * 顶边从玻璃色起步，经过页头时慢慢变实，落进内容区时已经几乎是平的，
	 * 所以看不出「在这里换了一块」。</p>
	 *
	 * <p>颜色始终从同一个面板色派生（只掺白、只改 alpha），不新增色块、
	 * 不新增容器，也不会画出任何横线；落底处与实色面板同色，所以看不出终点。</p>
	 */
	private void drawHeaderRamp(NanoVGManager nvg, float x, float y, float w, float h, int radiusTopLeft, int radiusTopRight, Color base) {

		// 整条渐变必须是「一个 shape + 一个 nvgLinearGradient」，不能拼。
		//
		// 拼段的历史：先按 smoothstep 曲线把过渡带切成 15 段，每段一个线性渐变。
		// 相邻两段的边界落在同一个亚像素位置上，光栅化各取一半覆盖率 ->
		// 那一行只剩 50% 的 alpha，背后是已经糊开的天空（偏蓝），于是每条接缝
		// 都被读成一条横线。实测 v7 是 7 道暗线（间隔 5~6 像素，单行掉 13~39 个亮度）；
		// v8 给每段多画 1.2 像素想压住接缝，结果接缝行变成双重覆盖 ->
		// 反而亮 47 个亮度，线还在，只是从暗线变亮线。
		//
		// 结论：只要形状有缝，alpha 合成就一定有台阶。所以这里退成一次绘制 ——
		// 圆角在同一个 shape 里给（tl/tr 圆、bl/br 直角），
		// 端点颜色就是整条带子的两个端点，中间由 nvgLinearGradient 线性插值。
		// 线性插值的斜率恒定、无任何内部边界，所以结构上不可能出现横线；
		// 「上淡下实」的整体观感由 shader 里的 smoothstep 遮罩（MaskY）负责，
		// 面板这一层只做纯 alpha 过渡，两层叠加后依然是一条自顶上化开的连续带。
		nvg.drawVerticalGradientRoundedRectVarying(x, y, w, h, radiusTopLeft, radiusTopRight, 0, 0,
				headerRampColor(base, 0F), headerRampColor(base, 1F));
	}

	/**
	 * 过渡带上的颜色：同一个面板色，只改「掺白量」和 alpha。
	 *
	 * <p>两件事同时随 t 走，缺一不可：</p>
	 *
	 * <ul>
	 *   <li><b>掺白</b>负责「磨砂」。只降 alpha 的话透过来的就是游戏画面本身
	 *       （天空/山），颜色是蓝绿的，读出来是一块透明天窗而不是磨砂玻璃。
	 *       顶部拉到 {@link #HEADER_FROST_MIX_LIGHT}，往下按 (1-t)^1.6 收回 0，
	 *       所以 t=1 处颜色严格等于面板本色。</li>
	 *   <li><b>alpha</b> 从 {@link #HEADER_GLASS_ALPHA} 走到 255，落底时已经是
	 *       不透明的面板本色 —— 和下方实色内容区完全同色，任何 y 上都读不出接缝。</li>
	 * </ul>
	 */
	private static Color headerRampColor(Color base, float t) {

		float x = Math.max(0F, Math.min(1F, t));
		float eased = x * x * (3F - 2F * x);

		// 颜色严格等于面板本色 —— 不再是「掺白的牛奶色」。
		// 之前那一版把 base 往白色掺，目的是让半透明区看起来像磨砂；
		// 但掺白只改变颜色、不会抹开背后的像素，读起来仍是「一块白板盖上去」。
		// 真正的磨砂交给 BlurUtils 的 MaskY 遮罩去做（背景像素本身被模糊），
		// 这里退回纯 alpha 渐隐：顶部几乎全透明，露出下面已经糊开的背景，
		// 越往下越接近不透明，下沿与内容区完全同色，任何 y 上都读不出接缝。
		float frost = ModMenuTheme.isDark() ? HEADER_FROST_MIX_DARK : HEADER_FROST_MIX_LIGHT;

		Color tinted = ModMenuTheme.mix(base, Color.WHITE, frost * 0.10F * (1F - eased));
		int alpha = (int) Math.round(HEADER_GLASS_ALPHA + (255F - HEADER_GLASS_ALPHA) * eased);

		return ModMenuTheme.applyAlpha(tinted, alpha);
	}

	public boolean isChangelogOverlayOpen() {
		return changelogOverlayOpen;
	}

	// 首页的更新卡片会打开这个悬浮窗，用来查看全部更新内容
	public void openChangelogOverlay() {

		this.buildChangelogRows();

		changelogOverlayOpen = true;
		changelogOverlayScroll.resetAll();
		changelogOverlayScroll.setScrollPosition(0.0F);
	}

	public void closeChangelogOverlay() {
		changelogOverlayOpen = false;
	}

	private void buildChangelogRows() {

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();

		changelogRows.clear();

		float maxWidth = Math.max(120F, (this.getChangelogPanelWidth() - 32F) - 46F - 10F - 16F);

		ChangelogType currentType = null;
		int sectionStart = 0;

		for(Changelog changelog : Mix.getInstance().getChangelogManager().getChangelogs()) {

			// 换类型时插一行分组标题，之前的条目补上"这是第几组"
			if(currentType == null || changelog.getType() != currentType) {

				currentType = changelog.getType();
				sectionStart = changelogRows.size();

				changelogRows.add(new ChangelogRow(currentType));
			}

			ArrayList<String> lines = this.wrapText(nvg, changelog.getText(), 10.5F, Fonts.REGULAR, maxWidth);
			changelogRows.add(new ChangelogRow(changelog, lines.toArray(new String[0])));
		}
	}

	private float getChangelogPanelWidth() {
		return Math.min(560F, width * 0.62F);
	}

	private float getChangelogPanelHeight() {
		return Math.min(440F, height * 0.76F);
	}

	private float getChangelogPanelX() {
		return x + ((width - this.getChangelogPanelWidth()) / 2F);
	}

	private float getChangelogPanelY() {
		return y + ((height - this.getChangelogPanelHeight()) / 2F);
	}

	// NanoVG 没有自动换行，这里按实际字宽逐字符折行（中文没有空格可断，只能按字符来）
	private ArrayList<String> wrapText(NanoVGManager nvg, String text, float size, Font font, float maxWidth) {

		ArrayList<String> lines = new ArrayList<String>();

		if(text == null) {
			return lines;
		}

		StringBuilder line = new StringBuilder();

		for(int i = 0; i < text.length(); i++) {

			char c = text.charAt(i);

			if(c == '\n') {
				lines.add(line.toString());
				line.setLength(0);
				continue;
			}

			line.append(c);

			if(nvg.getTextWidth(line.toString(), size, font) > maxWidth) {

				line.setLength(line.length() - 1);

				if(line.length() > 0) {
					lines.add(line.toString());
				}

				line.setLength(0);
				line.append(c);
			}
		}

		if(line.length() > 0) {
			lines.add(line.toString());
		}

		if(lines.isEmpty()) {
			lines.add("");
		}

		return lines;
	}

	private void drawChangelogOverlay() {

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();

		changelogOverlayAnimation.setAnimation(changelogOverlayOpen ? 1.0F : 0.0F, 22);

		float animation = changelogOverlayAnimation.getValue();

		if(animation < 0.01F) {
			return;
		}

		float panelWidth = this.getChangelogPanelWidth();
		float panelHeight = this.getChangelogPanelHeight();
		float panelX = this.getChangelogPanelX();
		float panelY = this.getChangelogPanelY();

		// 背景压暗
		nvg.drawRoundedRect(x, y, width, height, 18, new Color(0, 0, 0, (int) (64F * Math.min(1.0F, animation))));

		nvg.setAlpha(animation);

		// 阴影只做很轻的一层：原来 26 圈纯黑描边会在面板四周糊出一大块死黑
		nvg.drawShadow(panelX, panelY, panelWidth, panelHeight, 16, 11);
		nvg.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 16, ModMenuTheme.surfaceContainerHigh());
		nvg.drawOutlineRoundedRect(panelX, panelY, panelWidth, panelHeight, 16, 1F, ModMenuTheme.outlineVariant());

		// 标题
		nvg.drawText(TranslateText.CHANGELOG.getText(), panelX + 20F, panelY + 18F, ModMenuTheme.onSurface(), 17F, Fonts.DEMIBOLD);
		nvg.drawText("changelog.txt", panelX + 20F, panelY + 41F, ModMenuTheme.onSurfaceVariant(), 9F, Fonts.REGULAR);

		// 关闭按钮
		float closeSize = 26F;
		float closeX = panelX + panelWidth - 20F - closeSize;
		float closeY = panelY + 16F;

		nvg.drawRoundedRect(closeX, closeY, closeSize, closeSize, closeSize / 2F, ModMenuTheme.surfaceContainerHighest());
		nvg.drawCenteredTextMid(Icon.X, closeX + (closeSize / 2F), closeY + (closeSize / 2F), ModMenuTheme.onSurface(), 12F, Fonts.ICON);

		nvg.drawRect(panelX + 16F, panelY + 58F, panelWidth - 32F, 1, ModMenuTheme.outlineVariant());

		float listX = panelX + 16F;
		float listY = panelY + 70F;
		float listWidth = panelWidth - 32F;
		float listHeight = Math.max(40F, (panelY + panelHeight - 16F) - listY);

		float contentHeight = 0F;

		for(ChangelogRow row : changelogRows) {
			contentHeight += row.height;
		}

		changelogOverlayScroll.setMaxScroll(Math.max(0F, contentHeight - listHeight));

		nvg.save();
		nvg.scissor(listX, listY, listWidth, listHeight);

		float rowY = listY + changelogOverlayScroll.getValue();

		for(ChangelogRow row : changelogRows) {

			if(rowY + row.height >= listY && rowY <= listY + listHeight) {

				if(row.isHeader()) {

					// 分组标题：左侧一条短色条 + 类型名，比条目略小、颜色用次要色
					Color headerColor = row.getHeaderColor();

					nvg.drawRoundedRect(listX, rowY + 9F, 3F, 8F, 1.5F, headerColor);
					nvg.drawText(row.header, listX + 9F, rowY + 6F, ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.DEMIBOLD);

				} else {

					float badgeWidth = 46F;

					nvg.drawRoundedRect(listX, rowY + 2F, badgeWidth, 14F, 7F, row.changelog.getType().getColor());
					nvg.drawCenteredText(row.changelog.getType().getText(), listX + (badgeWidth / 2F), rowY + 4F, Color.WHITE, 8F, Fonts.MEDIUM);

					float textX = listX + badgeWidth + 10F;
					float lineY = rowY + 2F;

					for(String line : row.lines) {
						nvg.drawText(line, textX, lineY, ModMenuTheme.onSurface(), 10.5F, Fonts.REGULAR);
						lineY += 13F;
					}
				}
			}

			rowY += row.height;
		}

		nvg.restore();

		// 滚动条
		if(contentHeight > listHeight && contentHeight > 0F) {

			float maxScroll = contentHeight - listHeight;
			float progress = Math.min(1F, Math.max(0F, (-changelogOverlayScroll.getValue()) / maxScroll));
			float barHeight = Math.max(28F, listHeight * (listHeight / contentHeight));
			float barY = listY + ((listHeight - barHeight) * progress);

			nvg.drawRoundedRect(panelX + panelWidth - 9F, listY, 3F, listHeight, 1.5F, ModMenuTheme.applyAlpha(ModMenuTheme.onSurfaceVariant(), 45));
			nvg.drawRoundedRect(panelX + panelWidth - 9F, barY, 3F, barHeight, 1.5F, ModMenuTheme.applyAlpha(ModMenuTheme.onSurfaceVariant(), 170));
		}

		nvg.setAlpha(1.0F);
	}


	private class ChangelogRow {

		// 条目行用 changelog/lines；分组标题行只填 header/headerType，changelog 为 null
		private Changelog changelog;
		private String[] lines;
		private String header;
		private ChangelogType headerType;
		private float height;

		private ChangelogRow(Changelog changelog, String[] lines) {
			this.changelog = changelog;
			this.lines = lines;
			this.height = Math.max(20F, (lines.length * 13F) + 6F);
		}

		private ChangelogRow(ChangelogType type) {
			this.headerType = type;
			this.header = type.getText();
			this.lines = new String[0];
			this.height = 26F;
		}

		private boolean isHeader() {
			return this.header != null;
		}

		private Color getHeaderColor() {
			return this.headerType == null ? ModMenuTheme.onSurfaceVariant() : this.headerType.getColor();
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		int screenMouseX = mouseX;
		int screenMouseY = mouseY;
		mouseX = toDesignX(mouseX);
		mouseY = toDesignY(mouseY);

		if(changelogOverlayOpen) {

			float panelWidth = this.getChangelogPanelWidth();
			float panelHeight = this.getChangelogPanelHeight();
			float panelX = this.getChangelogPanelX();
			float panelY = this.getChangelogPanelY();

			if(mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, panelX + panelWidth - 46F, panelY + 16F, 26F, 26F)) {
				closeChangelogOverlay();
				return;
			}

			// 点面板外任意位置关闭
			if(!MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
				closeChangelogOverlay();
			}

			return;
		}

		float offsetY = y + 92;
		float navPitch = Math.max(43F, Math.min(56F, (height - 196F) / Math.max(1F, categories.size())));

		for(Category c : categories) {

			if(MouseUtils.isInside(mouseX, mouseY, x, y + 76, railWidth, height - 178)
					&& MouseUtils.isInside(mouseX, mouseY, x + (railWidth - 58) / 2F, offsetY - 2, 58, 38)
					&& mouseButton == 0) {
				setCurrentCategory(c);
			}

			offsetY += navPitch;
		}

		if(MouseUtils.isInside(mouseX, mouseY, x + (railWidth - 40) / 2F, y + height - 92, 40, 40) && mouseButton == 0) {
			toEditHUD = true;
			introAnimation.setDirection(Direction.BACKWARDS);
		}

		if(MouseUtils.isInside(mouseX, mouseY, x + (railWidth - 30) / 2F, y + height - 42, 30, 30) && mouseButton == 0) {
			setCurrentCategory(getCategoryByClass(ProfileCategory.class));
		}

		if(MouseUtils.isInside(mouseX, mouseY, x + railWidth, y + headerHeight, width - railWidth, height - headerHeight)) {
			currentCategory.mouseClicked(mouseX, mouseY, mouseButton);
		}

		// 搜索框正在淡入淡出的那 200ms 里，它的实际位置和「静止位置」差着 headerSwapDx，
		// 命中判定必须用同一份偏移换算回去，否则会出现看得见点不着 / 点到空白。
		// 淡出超过一半的那个不给点：那时它只是残影，用户点的是即将出现的徽章位置。
		if(currentCategory.isShowSearchBox() && !currentCategory.isSubPageOpen()
				&& headerSwapAnimation.getValue() > 0.5F) {
			searchBox.mouseClicked(mouseX - Math.round(headerSwapDx), mouseY, mouseButton);
		}

		try {
			super.mouseClicked(screenMouseX, screenMouseY, mouseButton);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		if(changelogOverlayOpen) {
			return;
		}

		// Always forward the release: sliders/pickers that were dragged have to clear
		// their drag state even when the cursor leaves the content rectangle.
		currentCategory.mouseReleased(toDesignX(mouseX), toDesignY(mouseY), mouseButton);
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {

		if(changelogOverlayOpen) {

			if(keyCode == Keyboard.KEY_ESCAPE) {
				closeChangelogOverlay();
			}

			return;
		}

		// 分类自己有没有正在编辑的输入框？必须在 keyTyped 之前问：
		// keyTyped 里 ESC 会把搜索框失焦，问完再判就永远是 false，菜单会跟着一起关掉。
		boolean escapeConsumed = keyCode == Keyboard.KEY_ESCAPE && currentCategory.isEscapeConsumed();

		currentCategory.keyTyped(typedChar, keyCode);

		if(escapeConsumed) {
			return;
		}

		if(currentCategory.isShowSearchBox() && !currentCategory.isSubPageOpen()) {
			searchBox.keyTyped(typedChar, keyCode);
		}

		if(currentCategory.isShowSearchBox() && !currentCategory.isSubPageOpen() && canClose) {

			if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {

				if(!searchBox.getText().isEmpty()) {
					searchBox.setText("");
					searchBox.setFocused(false);
					return;
				}

				if(searchBox.isFocused()) {
					searchBox.setFocused(false);
					return;
				}
			}
		}

		if(keyCode == Keyboard.KEY_ESCAPE && canClose) {
			mc.displayGuiScreen(toEditHUD ? new GuiEditHUD(true) : null);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void onGuiClosed() {
		
		// 关菜单只落配置，不再碰播放状态：以前这里会把音乐 stop 掉，
		// 用户的实际体验是「一关模组菜单音乐就没了」。播放与否归音乐页自己管。
		Mix.getInstance().getProfileManager().save();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getRailWidth() {
		return railWidth;
	}

	public int getHeaderHeight() {
		return headerHeight;
	}

	public ArrayList<Category> getCategories() {
		return categories;
	}

	public Category getCategoryByClass(Class<?> clazz) {

		for(Category c : categories) {
			if(c.getClass().equals(clazz)) {
				return c;
			}
		}

		return null;
	}

	public Scroll getScroll() {
		return scroll;
	}

	public CompSearchBox getSearchBox() {
		return searchBox;
	}

	public boolean isCanClose() {
		return canClose;
	}

	public void setCanClose(boolean canClose) {
		this.canClose = canClose;
	}
}
