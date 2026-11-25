package mix.client.mixui.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.input.Keyboard;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.CategoryIslandRenderer;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.ModIcons;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.ModManager;
import mix.client.mixui.management.mods.settings.Setting;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.ColorSetting;
import mix.client.mixui.management.mods.settings.impl.ComboSetting;
import mix.client.mixui.management.mods.settings.impl.ImageSetting;
import mix.client.mixui.management.mods.settings.impl.KeybindSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.management.mods.settings.impl.SoundSetting;
import mix.client.mixui.management.mods.settings.impl.TextSetting;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.nanovg.font.MaterialIcons;
import mix.client.mixui.management.profile.MenuPrefs;
import mix.client.mixui.ui.comp.Comp;
import mix.client.mixui.ui.comp.impl.CompColorPicker;
import mix.client.mixui.ui.comp.impl.CompComboBox;
import mix.client.mixui.ui.comp.impl.CompImageSelect;
import mix.client.mixui.ui.comp.impl.CompKeybind;
import mix.client.mixui.ui.comp.impl.CompSlider;
import mix.client.mixui.ui.comp.impl.CompSoundSelect;
import mix.client.mixui.ui.comp.impl.CompToggleButton;
import mix.client.mixui.ui.comp.impl.field.CompModTextBox;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.SearchUtils;
import mix.client.mixui.utils.animation.normal.Animation;
import mix.client.mixui.utils.animation.normal.Direction;
import mix.client.mixui.utils.animation.normal.other.SmoothStepAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;
import mix.client.mixui.utils.mouse.Scroll;

public class ModuleCategory extends Category {

	// 分类标签外面那座悬浮小岛，逻辑在 CategoryIslandRenderer 里，四个页面共用
	private final CategoryIslandRenderer chipIsland = new CategoryIslandRenderer();

	private static final float CARD_HEIGHT = 136F;
	private static final float CARD_FOOTER = 36F;
	private static final float CARD_GAP = 18F;
	private static final float CONTENT_MARGIN = 24F;
	private static final float CHIP_HEIGHT = 26F;
	private static final float ROW_HEIGHT = 46F;
	private static final float STAR_SIZE = 18F;

	// 二级页设置列表上下两条渐隐带的高度。和一级页 drawScrollFade 的带子
	// （视口上沿 -> 首行内容，约 16F）同一个量级：短到不会吃掉整行内容，
	// 长到足以把「行被裁断的那一线」糊开，读者看到的是淡出而不是切口。
	private static final float SETTING_FADE_HEIGHT = 20F;

	// Headroom kept above the first row and below the last one so the outer hover
	// halo (~9.5px) is never sliced by the viewport clip or the panel edge.
	private static final float CARD_EDGE_PAD = 12F;

	private ModCategory currentCategory = ModCategory.ALL;

	// Horizontal browser slide used by the settings transition.  The card
	// scissor has to subtract it or the translated browser paints over the rail.
	private float lastBrowserOffset;

	private Scroll settingScroll = new Scroll();
	private boolean openSetting;
	private Animation settingAnimation;
	private Mod currentMod;

	// 开关切换时那一下"卡片有反应"的脉冲。
	//
	// 不能复用 m.getAnimation()：那条动画的语义是「当前是否开启」，它是双向的、
	// 并且会一直停在 0 或 1 上 —— 只能驱动滑块的最终位置，驱动不了"刚刚被切换"
	// 这个瞬时事件。所以单独记一条一次性的脉冲：状态真的变了那一刻点亮，
	// 之后 TOGGLE_PULSE_MS 内衰减回 0，用来画扩散描边、卡片内的一记染色、
	// 以及图标 tile 的弹一下。
	private static final float TOGGLE_PULSE_MS = 460F;

	private HashMap<Mod, Float> settingScrollPositions = new HashMap<Mod, Float>();
	private final HashMap<Mod, Float> cardHover = new HashMap<Mod, Float>();
	private final HashMap<Mod, Boolean> toggleState = new HashMap<Mod, Boolean>();
	private final HashMap<Mod, Long> togglePulseStart = new HashMap<Mod, Long>();
	private HashMap<Mod, ArrayList<Setting>> settingsByMod = new HashMap<Mod, ArrayList<Setting>>();

	private ArrayList<ModuleSetting> comps = new ArrayList<ModuleSetting>();

	public ModuleCategory(GuiModMenu parent) {
		super(parent, TranslateText.MODULE, Icon.ARCHIVE, true);
	}

	@Override
	public void onShown() {
		// 从左侧栏切回这个分类时重播一次圆环入场动画
		chipIsland.reset();
	}

	@Override
	public void initGui() {
		currentCategory = ModCategory.ALL;
		cacheSettings();
		resetSettingScene();
	}

	@Override
	public void initCategory() {
		cacheSettings();
		resetSettingScene();
	}

	private void resetSettingScene() {

		if(openSetting) {
			saveSettingScrollPosition();
		}

		openSetting = false;
		currentMod = null;
		settingAnimation = new SmoothStepAnimation(240, 1.0);
		settingAnimation.setValue(1.0);
		setCanClose(true);
	}

	@Override
	public boolean isSubPageOpen() {
		return openSetting;
	}

	public void setFilterCategory(ModCategory category) {

		if(category == null) {
			return;
		}

		currentCategory = category;
		resetSettingScene();
		scroll.setScrollPosition(0);
	}

	private void cacheSettings() {

		settingsByMod.clear();

		for(Mod m : Mix.getInstance().getModManager().getMods()) {
			settingsByMod.put(m, Mix.getInstance().getModManager().getSettingsByMod(m));
		}
	}

	public void saveSettingScrollPosition() {

		if(currentMod != null) {
			settingScrollPositions.put(currentMod, settingScroll.getScrollPosition());
		}
	}

	private float getSavedSettingScroll(Mod m) {

		if(m == null) {
			return 0.0F;
		}

		Float saved = settingScrollPositions.get(m);
		return saved == null ? 0.0F : saved;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();

		settingAnimation.setDirection(openSetting ? Direction.BACKWARDS : Direction.FORWARDS);

		if(!openSetting && currentMod != null && settingAnimation.isDone(Direction.FORWARDS)) {
			saveSettingScrollPosition();
			currentMod = null;
			setCanClose(true);
		}

		float browserOffset = -(1.0F - (float) settingAnimation.getValue()) * getWidth() * 0.10F;
		lastBrowserOffset = browserOffset;

		nvg.save();
		nvg.translate(browserOffset, 0);
		drawBrowser(mouseX, mouseY, partialTicks);
		nvg.restore();

		if(currentMod != null) {
			nvg.save();
			nvg.translate((float) settingAnimation.getValue() * getWidth(), 0);
			drawSettingPage(mouseX, mouseY, partialTicks);
			nvg.restore();
		}
	}

	private void drawBrowser(int mouseX, int mouseY, float partialTicks) {

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		ArrayList<Mod> mods = getFilteredMods();

		float chipY = getY() + 10;
		float chipStartX = getX() + CONTENT_MARGIN;
		float chipX = chipStartX;

		// 小岛宽度完全由标签本身决定：先量一遍总宽。绘制放到列表之后，
		// 小岛才会浮在卡片上层，而不是被卡片顶边拼成一条横栏。
		float chipRowWidth = 0F;

		for(ModCategory category : ModCategory.values()) {
			chipRowWidth += nvg.getTextWidth(category.getName(), 10.5F, Fonts.MEDIUM) + 26F + 8F;
		}

		chipRowWidth -= 8F;

		// 右上角的 "3 / 129" 计数不参与小岛，小岛右边界给它让路。
		String count = mods.size() + " / " + getTotalMods();
		float ringRight = getX() + getWidth() - CONTENT_MARGIN
				- nvg.getTextWidth(count, 9.5F, Fonts.REGULAR) - 14F;

		int columns = getColumns();
		float gap = CARD_GAP;
		float cardWidth = (getWidth() - (CONTENT_MARGIN * 2F) - (gap * (columns - 1))) / columns;

		// 视口上沿 = 小岛底边（且一定落在顶栏磨砂 ramp 落地之后）。裁到小岛底边，
		// 遮挡带的首行 alpha 才和面板相等，接缝不会读成一条横线。
		float listTop = CategoryIslandRenderer.viewportTop(chipY, CHIP_HEIGHT);
		// 内容起点仍在胶囊下方，中间那段空白属于内容，会随滚动一起滑走。
		float cardsTop = CategoryIslandRenderer.contentTop(chipY, CHIP_HEIGHT);
		float viewportHeight = getY() + getHeight() - 12F - listTop;
		int rows = (mods.size() + columns - 1) / columns;
		float contentHeight = CategoryIslandRenderer.topSpacer(CHIP_HEIGHT)
				+ Math.max(0, rows * (CARD_HEIGHT + gap) - gap);

		// The hover halo grows up to 9.5px outside the card, so the last row needs
		// real slack at the bottom or its light gets sliced by the panel edge.
		scroll.setMaxScroll(Math.max(0, contentHeight + CARD_EDGE_PAD - viewportHeight));

		// 裁剪上沿严格等于视口上沿：卡片滚上来时被小岛盖住，而不是在视口上沿
		// 被硬切成一条横贯内容区的水平分界。
		nvg.save();
		// The browser is already translated by lastBrowserOffset.  Compensate
		// in the clip rectangle so cards can never slide over the navigation rail.
		nvg.scissor(getX() - lastBrowserOffset, listTop, getWidth(), viewportHeight + CARD_EDGE_PAD);
		nvg.translate(0, scroll.getValue());

		for(int i = 0; i < mods.size(); i++) {

			Mod m = mods.get(i);
			int column = i % columns;
			int row = i / columns;

			float cardX = getX() + CONTENT_MARGIN + column * (cardWidth + gap);
			float cardY = cardsTop + row * (CARD_HEIGHT + gap);

			float cardScreenY = cardY + scroll.getValue();

			if(cardScreenY + CARD_HEIGHT + CARD_EDGE_PAD < listTop || cardScreenY > listTop + viewportHeight + CARD_EDGE_PAD) {
				continue;
			}

			// Cards that slid past the clip rectangle must not react: the pixels are
			// hidden, so a hover there would light up something invisible.
			boolean mouseInViewport = mouseY >= listTop && mouseY <= listTop + viewportHeight
					&& !CategoryIslandRenderer.overIsland(mouseX, mouseY, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT);

			boolean hasSettings = hasSettings(m);
			boolean bodyHovered = mouseInViewport && MouseUtils.isInside(mouseX, mouseY, cardX, cardScreenY, cardWidth, CARD_HEIGHT - CARD_FOOTER);
			boolean footerHovered = mouseInViewport && MouseUtils.isInside(mouseX, mouseY, cardX, cardScreenY + CARD_HEIGHT - CARD_FOOTER, cardWidth, CARD_FOOTER);
			boolean starHovered = mouseInViewport && isStarHovered(mouseX, mouseY, cardX, cardScreenY, cardWidth);

			drawCard(nvg, m, cardX, cardY, cardWidth, bodyHovered, footerHovered, starHovered, hasSettings, i);
		}

		nvg.restore();

		// 列表画完再画小岛：它是浮在列表头上的独立胶囊，卡片从它背后滚过去。
		// 渐隐带铺满内容区全宽（含左右两个 margin）：它盖的是「行被裁切的地方」，
		// 只糊中间一段会在左右各留一条空白竖条，和面板拼成生硬的直角。
		CategoryIslandRenderer.drawScrollFade(nvg, getX() - lastBrowserOffset, getWidth(), chipY, CHIP_HEIGHT, ModMenuTheme.surface());
		chipIsland.draw(nvg, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT, ringRight);

		for(ModCategory category : ModCategory.values()) {

			String name = category.getName();
			float chipWidth = nvg.getTextWidth(name, 10.5F, Fonts.MEDIUM) + 26F;
			boolean selected = category.equals(currentCategory);
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, chipX, chipY, chipWidth, CHIP_HEIGHT);

			if(selected) {
				AccentColor accent = Mix.getInstance().getColorManager().getCurrentColor();
				nvg.drawGradientRoundedRect(chipX, chipY, chipWidth, CHIP_HEIGHT, CHIP_HEIGHT / 2F, accent.getColor1(), accent.getColor2());
				nvg.drawText(name, chipX + 13, chipY + 8, Color.WHITE, 10.5F, Fonts.MEDIUM);
			}else {
				nvg.drawRoundedRect(chipX, chipY, chipWidth, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
						hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
				nvg.drawText(name, chipX + 13, chipY + 8, ModMenuTheme.onSurfaceVariant(), 10.5F, Fonts.MEDIUM);
			}

			chipX += chipWidth + 8F;
		}

		nvg.drawText(count, getX() + getWidth() - CONTENT_MARGIN - nvg.getTextWidth(count, 9.5F, Fonts.REGULAR),
				chipY + 9, ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);
	}

	/**
	 * 开关被切换那一下的一次性脉冲，1 -> 0（约 460ms 衰减完）。
	 *
	 * <p>每帧被调用，靠 {@link #toggleState} 记上一帧的开关状态：只有状态真的
	 * 翻了才把起点打到 {@link #togglePulseStart}，其余帧只做衰减。这样
	 * resize、切分类、翻页回来都不会误触发 —— 只有用户真的按下去才有反应。</p>
	 *
	 * <p>衰减用 1-t 的三次方而不是线性：线性衰减的收尾会拖一条长长的尾巴，
	 * 410ms 之后还看得见一丝描边在动，读起来像卡帧；三次方在最后 200ms 里
	 * 已经掉到 5% 以下，收得干净。</p>
	 */
	private float togglePulse(Mod m) {

		Boolean previous = toggleState.put(m, m.isToggled());
		long now = System.currentTimeMillis();

		if(previous == null) {
			// 第一次见到这个卡片：只记状态，不放脉冲，否则一进菜单满屏都在闪。
			togglePulseStart.put(m, now - (long) TOGGLE_PULSE_MS);
			return 0F;
		}

		if(previous.booleanValue() != m.isToggled()) {
			togglePulseStart.put(m, now);
			return 1F;
		}

		Long start = togglePulseStart.get(m);

		if(start == null) {
			return 0F;
		}

		float t = (now - start.longValue()) / TOGGLE_PULSE_MS;

		if(t >= 1F) {
			return 0F;
		}

		float remain = 1F - t;

		return remain * remain * remain;
	}

	/** 脉冲的线性进度 0 -> 1（1 表示已经放完或从未放过）。扩散环的半径要用它，不能用三次方那条。 */
	private float togglePulseProgress(Mod m) {

		Long start = togglePulseStart.get(m);

		if(start == null) {
			return 1F;
		}

		float t = (System.currentTimeMillis() - start.longValue()) / TOGGLE_PULSE_MS;

		return t < 0F ? 0F : (t > 1F ? 1F : t);
	}

	private void drawCard(NanoVGManager nvg, Mod m, float x, float y, float width, boolean bodyHovered, boolean footerHovered, boolean starHovered, boolean hasSettings, int index) {

		AccentColor accent = Mix.getInstance().getColorManager().getCurrentColor();
		float bodyHeight = CARD_HEIGHT - CARD_FOOTER;
		float animation = m.getAnimation().getValue();
		long now = System.currentTimeMillis();

		// 16 档的时间常数约 89ms，肉眼只觉得「闪一下」；11 档约 145ms，
		// 20 帧（333ms）走完 93.6%，正好是用户要的 330ms 量级。
		m.getAnimation().setAnimation(m.isToggled() ? 1.0F : 0.0F, 11);

		// 状态翻转那一下的瞬时脉冲（1 -> 0），跟上面那条「当前是否开启」的
		// 双向动画是两回事：这条只在用户真的按下去的那一帧点亮。
		float pulse = togglePulse(m);
		float pulseProgress = togglePulseProgress(m);

		// Eased hover progress so the outer glow fades in instead of popping.
		boolean hovered = bodyHovered || footerHovered || starHovered;
		Float cachedHover = cardHover.get(m);
		float hover = cachedHover == null ? 0F : cachedHover.floatValue();
		float hoverTarget = hovered ? 1F : 0F;

		hover += (hoverTarget - hover) * 0.2F;

		if(Math.abs(hoverTarget - hover) < 0.005F) {
			hover = hoverTarget;
		}

		cardHover.put(m, hover);

		// Hover feedback lives on the outside only: a band of light with real thickness
		// (~10px) that breathes, so the hovered card is unmistakably fatter than the
		// 1px accent line an enabled mod keeps.
		if(hover > 0.01F) {

			float flow = 0.80F + 0.20F * (float) Math.sin((now / 260.0) + index * 0.7F);

			for(int layer = 5; layer >= 1; layer--) {

				float grow = layer * 1.9F;
				float wave = 0.74F + 0.26F * (float) Math.sin((now / 220.0) - (layer * 0.85F) + index * 0.7F);
				float falloff = 1F - ((layer - 1) * 0.15F);
				int alpha = (int) (78F * hover * flow * wave * falloff);

				if(alpha <= 3) {
					continue;
				}

				Color layerColor = (layer % 2 == 0) ? accent.getColor2() : accent.getColor1();

				nvg.drawRoundedRect(x - grow, y - grow, width + (grow * 2F), CARD_HEIGHT + (grow * 2F),
						12F + grow, ModMenuTheme.applyAlpha(layerColor, Math.min(alpha, 120)));
			}

			nvg.drawOutlineRoundedRect(x - 1F, y - 1F, width + 2F, CARD_HEIGHT + 2F, 13F, 1.4F,
					ColorUtils.applyAlpha(accent.getColor1(), (int) (150F * hover)));
		}

		// 开关切换时从卡片边缘扩散出去的一圈环。
		//
		// 为什么画在卡片外侧而不是内部：卡片内部已经被 hover 光晕、开启染色和
		// 图标 tile 占满，任何新增的填充都会把已经调好的层次搅浑；而「向外扩散」
		// 在语义上也正好对应「这个功能被推开了」。半径跟着 pulseProgress 线性
		// 增长，透明度跟 pulse（三次方）衰减，所以环边往外跑边消失。
		if(pulse > 0.01F) {

			float ringGrow = pulseProgress * 11F;
			int ringAlpha = (int) (135F * pulse);

			nvg.drawOutlineRoundedRect(x - ringGrow, y - ringGrow, width + (ringGrow * 2F),
					CARD_HEIGHT + (ringGrow * 2F), 12F + ringGrow, 1.6F,
					ColorUtils.applyAlpha(accent.getColor1(), ringAlpha));

			// 第二条错开 22% 出发，两层叠起来才有"波"的厚度，单圈看着像描边闪了一下。
			float secondProgress = pulseProgress - 0.22F;

			if(secondProgress > 0F) {
				float secondGrow = secondProgress * 11F;
				float secondPulse = 1F - secondProgress;
				int secondAlpha = (int) (110F * secondPulse * secondPulse * secondPulse);

				if(secondAlpha > 2) {
					nvg.drawOutlineRoundedRect(x - secondGrow, y - secondGrow, width + (secondGrow * 2F),
							CARD_HEIGHT + (secondGrow * 2F), 12F + secondGrow, 1.6F,
							ColorUtils.applyAlpha(accent.getColor2(), secondAlpha));
				}
			}
		}

		// Only a whisper of tint inside the card: the middle must not glow and must
		// not get darker either, so the palette stays close to the resting state.
		Color bodyColor = ModMenuTheme.mix(ModMenuTheme.surfaceContainerLow(), ModMenuTheme.surfaceContainerHigh(), hover * 0.30F);
		Color footerColor = ModMenuTheme.mix(ModMenuTheme.surfaceContainer(), ModMenuTheme.surfaceContainerHighest(), hover * 0.30F);

		nvg.drawRoundedRectVarying(x, y, width, bodyHeight, 12, 12, 0, 0, bodyColor);
		nvg.drawRoundedRectVarying(x, y + bodyHeight, width, CARD_FOOTER, 0, 0, 12, 12, footerColor);

		// Enabled mods keep the thin accent line - this is the state that has to stay
		// distinguishable from the thick hover halo.
		if(animation > 0.01F) {
			nvg.drawGradientRoundedRectVarying(x, y, width, bodyHeight, 12, 12, 0, 0,
					ColorUtils.applyAlpha(accent.getColor1(), (int) (24 * animation)),
					ColorUtils.applyAlpha(accent.getColor2(), (int) (24 * animation)));
			// 描边也乘 animation：原来是常数 110，于是卡片底色还在 20% 灰的时候
			// 外框已经整条亮紫了 —— 这是「生硬」观感的另一半来源。
			nvg.drawOutlineRoundedRect(x, y, width, CARD_HEIGHT, 12, 1F,
					ColorUtils.applyAlpha(accent.getColor1(), (int) (110 * animation)));
		}else if(hover > 0.01F) {
			nvg.drawGradientRoundedRectVarying(x, y, width, bodyHeight, 12, 12, 0, 0,
					ColorUtils.applyAlpha(accent.getColor1(), (int) (10 * hover)),
					ColorUtils.applyAlpha(accent.getColor2(), (int) (10 * hover)));
		}

		// 切换瞬间在卡片内部铺一层薄染色。
		//
		// 单独一段而不是并进上面的 if(animation > 0.01F)：那段只在"开启"分支里
		// 生效，关掉功能时整个分支都不进 —— 而关掉恰恰是最需要视觉回应的一侧。
		// 这一层跟开关方向无关，只跟"刚刚被点过"有关，最多 26/255 的 alpha，
		// 不会盖住卡片底色。
		if(pulse > 0.01F) {
			nvg.drawGradientRoundedRectVarying(x, y, width, bodyHeight, 12, 12, 0, 0,
					ColorUtils.applyAlpha(accent.getColor1(), (int) (26F * pulse)),
					ColorUtils.applyAlpha(accent.getColor2(), (int) (26F * pulse)));
		}

		// The icon and the mod name are one centred group: the name belongs to the
		// icon, not to the footer.
		float iconSize = Math.min(48F, width * 0.27F);
		float iconX = x + ((width - iconSize) / 2F);
		float iconY = y + 15F;

		// 灰色遮罩 → 紫色必须跟着 animation 一起走，不能用「animation > 0.01F ? 紫 : 灰」。
		// 三元表达式在 animation 跨过 0.01 的那一帧从灰直接跳成紫，就是用户说的
		// 「生硬的变成紫色」：动画其实一直在跑，但只有 tile 底色这一项没接上它。
		// 这里先按 animation 在「灰 → 紫」之间插值，再把 hover 的 22% 提亮叠在后面。
		Color tileColor = ModMenuTheme.mix(
				ModMenuTheme.mix(ModMenuTheme.surfaceContainerHigh(), ModMenuTheme.primaryContainer(), animation),
				ModMenuTheme.surfaceContainerHighest(), hover * 0.22F);

		// 图标 tile 在切换瞬间轻微放大再回落 —— 用 scale 包住 tile 与图标本身，
		// 只放大背景不放大图标会露边。放大 9%，460ms 内回落到 1.0。
		// 缩放到圆角半径一起放，否则放大后圆角看起来会变"方"。
		float tileScale = 1F + 0.09F * pulse;
		boolean tileScaled = tileScale > 1.001F;

		if(tileScaled) {
			nvg.save();
			nvg.scale(iconX, iconY, iconSize, iconSize, tileScale);
		}

		nvg.drawRoundedRect(iconX, iconY, iconSize, iconSize, 15, tileColor);

		// No bright wash over the tile any more: hovering only adds a hairline edge.
		if(hover > 0.01F) {
			nvg.drawOutlineRoundedRect(iconX - 0.5F, iconY - 0.5F, iconSize + 1F, iconSize + 1F, 15F, 1F,
					ModMenuTheme.applyAlpha(accent.getColor1(), (int) (95F * hover)));
		}

		// 图标前景色同理：按 animation 从「灰字」走到「深紫字」，hover 提亮系数也用
		// animation 在 0.10 ~ 0.28 之间插值，不再根据开关状态二选一。
		Color iconColor = ModMenuTheme.mix(ModMenuTheme.onSurfaceVariant(), ModMenuTheme.onPrimaryContainer(), animation);

		iconColor = ModMenuTheme.mix(iconColor, Color.WHITE, hover * (0.28F - 0.18F * animation));

		// 原来这里是 drawCenteredText 加一段手写的「- iconSize * 0.2F」经验偏移：
		// drawCenteredText 走的是 drawText 的 y += size/2 基线约定，图标字形盒的中心
		// 从来就没有落在 tile 中心上，那个 0.2 只是把某一种字形的偏差勉强对上了。
		// drawCenteredTextMid 会先用 nvgTextBounds 拿字形包围盒、把盒中心对到 tile 中心，
		// 所以偏移量可以直接删掉 —— 换字体（Icon.ttf 换成 Material Symbols）也不用重调。
		nvg.drawCenteredTextMid(getModIcon(m), iconX + (iconSize / 2F), iconY + (iconSize / 2F),
				iconColor, iconSize * 0.52F, Fonts.MATERIAL);

		if(tileScaled) {
			nvg.restore();
		}

		String name = trimText(nvg, m.getName(), width - 24F, 11.5F, Fonts.DEMIBOLD);
		float nameY = iconY + iconSize + 8F;

		nvg.drawCenteredText(name, x + (width / 2F), nameY,
				ModMenuTheme.mix(ModMenuTheme.onSurface(), accent.getColor1(), hover * 0.45F), 11.5F, Fonts.DEMIBOLD);

		if(hasSettings) {
			float gearX = x + width - 28F;
			float gearY = y + 8F;
			// Hovering the body used to wash the gear out into a light grey tile; keep it
			// readable by tinting the tile with the accent and darkening the glyph.
			Color gearTile = bodyHovered
					? ModMenuTheme.mix(ModMenuTheme.surfaceContainer(), accent.getColor1(), 0.18F)
					: ModMenuTheme.applyAlpha(ModMenuTheme.surfaceContainer(), 90);
			Color gearGlyph = bodyHovered ? ModMenuTheme.onSurface() : ModMenuTheme.onSurfaceVariant();
			nvg.drawRoundedRect(gearX, gearY, 20, 20, 10, gearTile);
			nvg.drawCenteredTextMid(MaterialIcons.SETTINGS, gearX + 10, gearY + 10, gearGlyph, 12, Fonts.MATERIAL);
		}

		float footerY = y + bodyHeight;
		float switchX = x + width - 48F;
		float switchY = footerY + ((CARD_FOOTER - 18F) / 2F);
		float starX = getStarCenterX(x, width);
		float starY = getStarCenterY(y);

		nvg.drawText(trimText(nvg, m.getCategory().getName(), width - 92F, 9.5F, Fonts.REGULAR), x + 14F, footerY + 13F,
				ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.REGULAR);

		boolean favorite = MenuPrefs.getInstance().isFavorite(m.getNameKey());
		Color starColor = favorite ? ModMenuTheme.primary() : (starHovered ? ModMenuTheme.onSurface() : ModMenuTheme.onSurfaceVariant());

		nvg.drawCenteredTextMid(favorite ? MaterialIcons.STAR : MaterialIcons.STAR_OUTLINE, starX, starY, starColor, 15F, Fonts.MATERIAL);

		if(hover > 0.01F) {
			nvg.drawOutlineRoundedRect(switchX - 1.5F, switchY - 1.5F, 37F, 21F, 10.5F, 1F,
					ColorUtils.applyAlpha(accent.getColor2(), (int) (110 * hover)));
		}

		nvg.drawRoundedRect(switchX, switchY, 34, 18, 9,
				ModMenuTheme.mix(ModMenuTheme.surfaceContainerHighest(), ModMenuTheme.primary(), animation));
		nvg.drawRoundedRect(switchX + 2 + (animation * 16), switchY + 2, 14, 14, 7, Color.WHITE);
	}
	private String trimText(NanoVGManager nvg, String text, float maxWidth, float size, mix.client.mixui.management.nanovg.font.Font font) {

		if(nvg.getTextWidth(text, size, font) <= maxWidth) {
			return text;
		}

		String result = text;

		while(result.length() > 1 && nvg.getTextWidth(result + "...", size, font) > maxWidth) {
			result = result.substring(0, result.length() - 1);
		}

		return result + "...";
	}

	private float getStarCenterX(float cardX, float cardWidth) {

		return cardX + cardWidth - 62F;
	}

	private float getStarCenterY(float cardY) {

		return cardY + CARD_HEIGHT - CARD_FOOTER + (CARD_FOOTER / 2F);
	}

	private boolean isStarHovered(int mouseX, int mouseY, float cardX, float cardY, float cardWidth) {

		return MouseUtils.isInside(mouseX, mouseY, getStarCenterX(cardX, cardWidth) - (STAR_SIZE / 2F),
				getStarCenterY(cardY) - (STAR_SIZE / 2F), STAR_SIZE, STAR_SIZE);
	}

	private void drawSettingPage(int mouseX, int mouseY, float partialTicks) {

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		float panelX = getX() + 24F;
		float panelY = getY() + 8F;
		float panelWidth = getWidth() - 48F;
		float panelHeight = getHeight() - 16F;
		float listTop = panelY + 66F;
		float listHeight = panelHeight - 78F;

		// 页面底色不能从内容区顶边「一刀切」铺下去 —— 那正是用户两次反馈的
		// 「跟顶部模糊区之间的分割线」。成因在 GuiModMenu：
		//   顶栏渐变（:371）的落地点是 y + headerHeight + 30，而 getY() 正好
		//   = y + headerHeight。所以内容区顶边那条线上方，磨砂只有 ~76% 的
		//   alpha（smoothstep 的尾巴还在爬），下方被一整块不透明 surface() 直接
		//   顶到 100%，一个像素之内跳完 —— 一整幅宽的横线。
		//
		// 也不能干脆不铺：底下的浏览器这一帧仍在画（GuiModMenu:544 无条件调
		// c.drawScreen -> 本类 drawScreen），左侧 24px 面板留白里会露出小岛的
		// 胶囊边缘和它的投影，读起来像「两层叠着没盖住」。
		//
		// 所以铺的不是一个矩形，而是「磨砂尾段的一条延长线」：用同色 surface()
		// 从 alpha 0 拉到 255，之后才是不透明底。
		//
		// 新层必须从 alpha 0 起步：页底叠在磨砂之上，复合 alpha 是
		// 1-(1-磨砂)(1-页底)。上沿若不是 0，就会在边缘凭空加厚一层 ——
		// 那还是一条线，只是从亮线换成暗线。
		//
		// 长度取到 panelY（getY()+8），不是磨砂真正的落地点（getY()+30）。
		// 两个理由：
		//   1) 面板从 panelY 起就不透明，再往下画纯属浪费；
		//   2) 更关键 —— 渐变在面板顶边处必须已经到 255。否则 panelY 以下
		//      面板两侧那两条 24px 留白里，页底仍是半透明，底下浏览器的小岛
		//      会一直透到落地点。渐变越短，露得越少。
		// 8px 内把 0.764 -> 1.0 这 23.6% 的差摊开，等价于 4 段每像素
		// 3.9% / 3.3% / 2.6% / 2.0% 的递减坡度（复合值 0.764 / 0.842 / 0.907 /
		// 0.960 / 1.000），全程单调、没有一格跳变。原代码是 0.764 直接跳 1.0，
		// 那一格就是用户看到的那条线。
		//
		// 残留代价：getY()..panelY 这 8px 里，左侧小岛的顶部投影会透出一部分
		// （panelY 处即完全遮住）。它只有 8px 高、越往下越淡，读起来是「从
		// 上方压下来的过渡」，不再是横贯整幅的一条线。
		float frostBand = Math.max(0F, panelY - getY());

		nvg.drawVerticalGradientRect(getX(), getY(), getWidth(), frostBand,
				ModMenuTheme.applyAlpha(ModMenuTheme.surface(), 0), ModMenuTheme.surface());

		// 兜底底色上沿两个角保持直角、下沿两个角给 18px —— 与 GuiModMenu:375
		// 画主体面时的参数完全一致（同样是 0, 0, 18, 18）。下沿是窗口真正的
		// 圆角，用直角铺到底会把它刮成方角；上沿必须直角，因为它要接的是
		// 一条横贯全宽的渐变，圆角会在接缝两端各留一个小缺口。
		nvg.drawRoundedRectVarying(getX(), panelY, getWidth(), Math.max(0F, getY() + getHeight() - panelY),
				0, 0, 18, 18, ModMenuTheme.surface());
		nvg.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 14, ModMenuTheme.surfaceContainer());

		// Back button
		boolean backHovered = MouseUtils.isInside(mouseX, mouseY, panelX + 14, panelY + 15, 34, 34);
		nvg.drawRoundedRect(panelX + 14, panelY + 15, 34, 34, 17,
				backHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
		nvg.drawCenteredTextMid(MaterialIcons.ARROW_BACK, panelX + 31, panelY + 23, ModMenuTheme.onSurface(), 16, Fonts.MATERIAL);

		nvg.drawText(currentMod.getName(), panelX + 60, panelY + 15, ModMenuTheme.onSurface(), 16, Fonts.DEMIBOLD);
		nvg.drawText(trimText(nvg, currentMod.getDescription(), panelWidth - 190F, 9, Fonts.REGULAR),
				panelX + 60, panelY + 40, ModMenuTheme.onSurfaceVariant(), 9, Fonts.REGULAR);

		// Reset
		boolean resetHovered = MouseUtils.isInside(mouseX, mouseY, panelX + panelWidth - 92, panelY + 18, 24, 24);
		nvg.drawRoundedRect(panelX + panelWidth - 92, panelY + 18, 24, 24, 12,
				resetHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
		nvg.drawCenteredTextMid(MaterialIcons.REFRESH, panelX + panelWidth - 80, panelY + 24, ModMenuTheme.onSurfaceVariant(), 13, Fonts.MATERIAL);

		// Enable switch
		float toggleX = panelX + panelWidth - 52F;
		float toggleY = panelY + 21F;
		currentMod.getAnimation().setAnimation(currentMod.isToggled() ? 1.0F : 0.0F, 11);
		nvg.drawRoundedRect(toggleX, toggleY, 34, 18, 9,
				ModMenuTheme.mix(ModMenuTheme.surfaceContainerHighest(), ModMenuTheme.primary(), currentMod.getAnimation().getValue()));
		nvg.drawRoundedRect(toggleX + 2 + (currentMod.getAnimation().getValue() * 16), toggleY + 2, 14, 14, 7, Color.WHITE);

		// 页头分隔线整条删掉，不是调淡。
		//
		// 上一版把它压到 alpha 46 想「留一点版式暗示」，用户二次反馈那条线还在：
		// 只要它是一条 1px 的整宽实色，落在面板正中间就会读成一条硬边，
		// 和它上方渐隐、下方滚动的行都接不上。页头的分界由「圆角面板顶边 +
		// 行距留白」自己交代，不需要额外画一笔。
		//
		// 注意这里不要用「画一条几乎全透明的线」来替代：alpha 低到看不见时它
		// 仍在参与光栅化，缩放比例一变就可能闪出一行脏像素。

		if(comps.isEmpty()) {
			nvg.drawCenteredText(TranslateText.NO_SETTINGS.getText(), panelX + (panelWidth / 2F), listTop + 28,
					ModMenuTheme.onSurfaceVariant(), 11, Fonts.REGULAR);
			return;
		}

		float contentHeight = layoutSettings(panelX, panelWidth, listTop);
		float maxScroll = Math.max(0, contentHeight - listHeight);
		settingScroll.setMaxScroll(maxScroll);

		if(MouseUtils.isInside(mouseX, mouseY, panelX + 10, listTop, panelWidth - 20, listHeight)) {
			settingScroll.onScroll();
		}

		settingScroll.onAnimation();
		layoutSettings(panelX, panelWidth, listTop);

		nvg.save();
		nvg.scissor(panelX + 10, listTop, panelWidth - 20, listHeight);

		// Row backgrounds and labels
		for(ModuleSetting s : comps) {

			if(s.rowY + ROW_HEIGHT < listTop || s.rowY > listTop + listHeight) {
				continue;
			}

			nvg.drawRoundedRect(panelX + 16, s.rowY + 2, panelWidth - 32, ROW_HEIGHT - 4, 9,
					ModMenuTheme.surfaceContainerLow());
			nvg.drawText(trimText(nvg, s.setting.getName(), panelWidth - 300F, 10.5F, Fonts.MEDIUM),
					panelX + 32, s.rowY + 15, ModMenuTheme.onSurface(), 10.5F, Fonts.MEDIUM);
		}

		// Controls。打开的色彩选择器弹层留到渐隐带之后单独画，原因见下面的注释。
		for(ModuleSetting s : comps) {
			if(s.comp instanceof CompColorPicker && ((CompColorPicker) s.comp).isOpen()) {
				continue;
			}
			drawComponent(s, mouseX, mouseY, partialTicks, listTop, listHeight);
		}

		nvg.restore();

		// 列表上下两条渐隐带。必须画在这里，两个原因：
		//   1) 在 nvg.restore() 之后 —— 裁剪块里画的话，带子自己也会被那条
		//      scissor 切掉，等于什么都没盖；
		//   2) 在行内容之后 —— 带子要盖的就是「被裁断的那一行」，画在行之前无效。
		// base 用 surfaceContainer()：二级页主体就是那块圆角面板，它填的是这个色。
		// 沿用 surface() 会在面板上糊出一片比底色更深的灰。
		//
		// 只在「这个方向真的还有行被裁在外面」时才画。无条件画是错的：
		// scroll = 0 时首行的圆角顶边就在 listTop + 2，一条 20px 的上带会把首行
		// 上沿糊成半透，读起来是「蒙了层灰」；滚到底（scroll == -maxScroll）时
		// 末行底边正好贴住视口下沿，下带会把它的收边擦掉。0.5F 是滚动动画
		// 停在边界值附近时的抖动容差，避免带子在静止状态下闪一下。
		boolean hasContentAbove = settingScroll.getValue() < -0.5F;
		boolean hasContentBelow = settingScroll.getValue() > -maxScroll + 0.5F;

		// 窗口压得很矮时（listHeight 小于两倍带高）两条带会互相重叠，
		// 重叠处 alpha 叠加成一块更深的影。按列表高的一半封顶就不会撞上。
		float fadeHeight = Math.min(SETTING_FADE_HEIGHT, listHeight * 0.5F);

		if(hasContentAbove) {
			CategoryIslandRenderer.drawEdgeFade(nvg, panelX + 10, listTop, panelWidth - 20,
					fadeHeight, ModMenuTheme.surfaceContainer(), true);
		}

		if(hasContentBelow) {
			CategoryIslandRenderer.drawEdgeFade(nvg, panelX + 10, listTop + listHeight - fadeHeight,
					panelWidth - 20, fadeHeight, ModMenuTheme.surfaceContainer(), false);
		}

		// 打开的色彩选择器弹层压在渐隐带之上：否则弹层下半截会被底部那条
		// 「下实上虚」的带子糊掉，看起来像弹层缺了一块。它仍然需要列表的裁剪，
		// 所以这里再开一次 scissor，而不是直接裸画。
		nvg.save();
		nvg.scissor(panelX + 10, listTop, panelWidth - 20, listHeight);

		for(ModuleSetting s : comps) {
			if(s.comp instanceof CompColorPicker && ((CompColorPicker) s.comp).isOpen()) {
				drawComponent(s, mouseX, mouseY, partialTicks, listTop, listHeight);
			}
		}

		nvg.restore();
	}

	private void drawComponent(ModuleSetting s, int mouseX, int mouseY, float partialTicks, float listTop, float listHeight) {

		if(s.rowY + ROW_HEIGHT < listTop || s.rowY > listTop + listHeight) {
			return;
		}

		s.comp.draw(mouseX, mouseY, partialTicks);
	}

	private float layoutSettings(float panelX, float panelWidth, float listTop) {

		float contentY = listTop + settingScroll.getValue();
		float total = 0;

		float rowRight = panelX + panelWidth - 30F;

		for(ModuleSetting s : comps) {

			float rowY = contentY + total;
			s.rowY = rowY;

			if(s.comp instanceof CompToggleButton) {
				CompToggleButton toggle = (CompToggleButton) s.comp;
				toggle.setX(rowRight - 34);
				toggle.setY(rowY + 15);
				toggle.setScale(0.95F);
			}

			if(s.comp instanceof CompSlider) {
				CompSlider slider = (CompSlider) s.comp;
				slider.setWidth(174);
				slider.setX(rowRight - 182);
				slider.setY(rowY + 22);
			}

			if(s.comp instanceof CompComboBox) {
				CompComboBox combo = (CompComboBox) s.comp;
				combo.setX(rowRight - 164);
				combo.setY(rowY + 15);
			}

			if(s.comp instanceof CompKeybind) {
				CompKeybind keybind = (CompKeybind) s.comp;
				keybind.setX(rowRight - 134);
				keybind.setY(rowY + 15);
			}

			if(s.comp instanceof CompModTextBox) {
				CompModTextBox textBox = (CompModTextBox) s.comp;
				textBox.setWidth(184);
				textBox.setHeight(20);
				textBox.setX(rowRight - 194);
				textBox.setY(rowY + 13);
			}

			if(s.comp instanceof CompColorPicker) {
				CompColorPicker picker = (CompColorPicker) s.comp;
				picker.setScale(0.8F);
				picker.setX(rowRight - 104);
				picker.setY(rowY + 15);
			}

			if(s.comp instanceof CompImageSelect) {
				CompImageSelect imageSelect = (CompImageSelect) s.comp;
				imageSelect.setX(rowRight - 18);
				imageSelect.setY(rowY + 15);
			}

			if(s.comp instanceof CompSoundSelect) {
				CompSoundSelect soundSelect = (CompSoundSelect) s.comp;
				soundSelect.setX(rowRight - 18);
				soundSelect.setY(rowY + 15);
			}

			total += ROW_HEIGHT;

			if(s.comp instanceof CompColorPicker) {
				CompColorPicker picker = (CompColorPicker) s.comp;
				if(picker.isOpen()) {
					total += picker.isShowAlpha() ? 100 : 86;
				}
			}
		}

		return total;
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		if(openSetting && settingAnimation.isDone(Direction.BACKWARDS)) {
			handleSettingClick(mouseX, mouseY, mouseButton);
			return;
		}

		if(openSetting) {
			return;
		}

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		float chipY = getY() + 10;
		float chipX = getX() + CONTENT_MARGIN;

		for(ModCategory category : ModCategory.values()) {

			float chipWidth = nvg.getTextWidth(category.getName(), 10.5F, Fonts.MEDIUM) + 26F;

			if(MouseUtils.isInside(mouseX, mouseY, chipX, chipY, chipWidth, CHIP_HEIGHT) && mouseButton == 0) {
				currentCategory = category;
				scroll.setScrollPosition(0);
				return;
			}

			chipX += chipWidth + 8F;
		}

		int columns = getColumns();
		float cardWidth = (getWidth() - (CONTENT_MARGIN * 2F) - (CARD_GAP * (columns - 1))) / columns;
		// 视口上沿是小岛顶边，但小岛浮在列表上层：落在小岛覆盖区里的点击
		// 属于标签，不该穿透到下面的卡片，所以卡片命中从 cardsTop 起算。
		float listTop = CategoryIslandRenderer.viewportTop(getY() + 10F, CHIP_HEIGHT);
		float cardsTop = CategoryIslandRenderer.contentTop(getY() + 10F, CHIP_HEIGHT);
		float viewportHeight = getY() + getHeight() - 12F - listTop;
		ArrayList<Mod> mods = getFilteredMods();

		for(int i = 0; i < mods.size(); i++) {

			Mod m = mods.get(i);
			int column = i % columns;
			int row = i / columns;
			float cardX = getX() + CONTENT_MARGIN + column * (cardWidth + CARD_GAP);
			float cardY = cardsTop + row * (CARD_HEIGHT + CARD_GAP) + scroll.getValue();

			if(mouseY >= cardsTop && mouseY <= listTop + viewportHeight
					&& MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, CARD_HEIGHT)) {

				boolean starClicked = isStarHovered(mouseX, mouseY, cardX, cardY, cardWidth);
				boolean settingsClicked = hasSettings(m) && MouseUtils.isInside(mouseX, mouseY, cardX + cardWidth - 30, cardY + 6, 24, 24);
				boolean footerClicked = MouseUtils.isInside(mouseX, mouseY, cardX, cardY + CARD_HEIGHT - CARD_FOOTER, cardWidth, CARD_FOOTER);

				if(mouseButton == 0) {
					if(starClicked) {
						MenuPrefs.getInstance().toggleFavorite(m.getNameKey());
					}else if(settingsClicked || (!footerClicked && hasSettings(m))) {
						openSettings(m);
					}else if(footerClicked) {
						m.toggle();
					}else {
						m.toggle();
					}
				}else if(mouseButton == 1) {
					m.toggle();
				}

				return;
			}
		}
	}

	private void handleSettingClick(int mouseX, int mouseY, int mouseButton) {

		float panelX = getX() + 24F;
		float panelY = getY() + 8F;
		float panelWidth = getWidth() - 48F;
		float panelHeight = getHeight() - 16F;
		float listTop = panelY + 66F;
		float listHeight = panelHeight - 78F;

		if(mouseButton == 3) {
			closeSettings();
			return;
		}

		if(mouseButton != 0) {
			return;
		}

		if(MouseUtils.isInside(mouseX, mouseY, panelX + 12, panelY + 13, 38, 38)) {
			closeSettings();
			return;
		}

		if(MouseUtils.isInside(mouseX, mouseY, panelX + panelWidth - 94, panelY + 16, 28, 28)) {
			for(ModuleSetting s : comps) {
				s.setting.reset();
			}
			return;
		}

		if(MouseUtils.isInside(mouseX, mouseY, panelX + panelWidth - 54, panelY + 19, 38, 24)) {
			currentMod.toggle();
			return;
		}

		if(!MouseUtils.isInside(mouseX, mouseY, panelX + 10, listTop, panelWidth - 20, listHeight)) {
			return;
		}

		// Let an open color picker consume its popup area first.
		for(ModuleSetting s : comps) {
			if(s.comp instanceof CompColorPicker) {
				CompColorPicker picker = (CompColorPicker) s.comp;
				if(picker.isOpen() && picker.isInsideOpen(mouseX, mouseY)) {
					picker.mouseClicked(mouseX, mouseY, mouseButton);
					return;
				}
			}
		}

		for(ModuleSetting s : comps) {
			if(s.rowY + ROW_HEIGHT < listTop || s.rowY > listTop + listHeight) {
				continue;
			}
			s.comp.mouseClicked(mouseX, mouseY, mouseButton);
		}
	}

	private void closeSettings() {

		if(!openSetting) {
			return;
		}

		saveSettingScrollPosition();
		openSetting = false;
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {

		if(!openSetting || !settingAnimation.isDone(Direction.BACKWARDS)) {
			return;
		}

		for(ModuleSetting s : comps) {
			s.comp.mouseReleased(mouseX, mouseY, mouseButton);
		}
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {

		if(!openSetting) {
			return;
		}

		boolean binding = false;

		for(ModuleSetting s : comps) {

			if(s.comp instanceof CompKeybind && ((CompKeybind) s.comp).isBinding()) {
				binding = true;
			}

			s.comp.keyTyped(typedChar, keyCode);
		}

		if(!binding && keyCode == Keyboard.KEY_ESCAPE) {
			closeSettings();
		}
	}

	private void openSettings(Mod m) {

		ArrayList<Setting> settings = settingsByMod.get(m);

		if(settings == null) {
			settings = Mix.getInstance().getModManager().getSettingsByMod(m);
		}

		currentMod = m;
		settingScroll.setScrollPosition(getSavedSettingScroll(m));
		buildComponents(settings);
		openSetting = true;
		setCanClose(false);
		settingAnimation = new SmoothStepAnimation(240, 1.0);
		settingAnimation.setValue(1.0);
		settingAnimation.setDirection(Direction.BACKWARDS);
	}

	private void buildComponents(ArrayList<Setting> settings) {

		comps.clear();

		if(settings == null) {
			return;
		}

		for(Setting setting : settings) {

			if(setting instanceof BooleanSetting) {
				BooleanSetting booleanSetting = (BooleanSetting) setting;
				CompToggleButton toggle = new CompToggleButton(booleanSetting);
				toggle.setScale(0.95F);
				comps.add(new ModuleSetting(setting, toggle));
			}

			if(setting instanceof NumberSetting) {
				NumberSetting numberSetting = (NumberSetting) setting;
				CompSlider slider = new CompSlider(numberSetting);
				slider.setWidth(174);
				comps.add(new ModuleSetting(setting, slider));
			}

			if(setting instanceof ComboSetting) {
				ComboSetting comboSetting = (ComboSetting) setting;
				comps.add(new ModuleSetting(setting, new CompComboBox(164, comboSetting)));
			}

			if(setting instanceof ImageSetting) {
				ImageSetting imageSetting = (ImageSetting) setting;
				comps.add(new ModuleSetting(setting, new CompImageSelect(imageSetting)));
			}

			if(setting instanceof SoundSetting) {
				SoundSetting soundSetting = (SoundSetting) setting;
				comps.add(new ModuleSetting(setting, new CompSoundSelect(soundSetting)));
			}

			if(setting instanceof KeybindSetting) {
				KeybindSetting keybindSetting = (KeybindSetting) setting;
				comps.add(new ModuleSetting(setting, new CompKeybind(134, keybindSetting)));
			}

			if(setting instanceof TextSetting) {
				TextSetting textSetting = (TextSetting) setting;
				CompModTextBox textBox = new CompModTextBox(textSetting);
				textBox.setWidth(184);
				textBox.setHeight(20);
				comps.add(new ModuleSetting(setting, textBox));
			}

			if(setting instanceof ColorSetting) {
				ColorSetting colorSetting = (ColorSetting) setting;
				CompColorPicker picker = new CompColorPicker(colorSetting);
				picker.setScale(0.8F);
				comps.add(new ModuleSetting(setting, picker));
			}
		}
	}

	private ArrayList<Mod> getFilteredMods() {

		ArrayList<Mod> result = new ArrayList<Mod>();
		ModManager modManager = Mix.getInstance().getModManager();

		if(currentCategory.equals(ModCategory.ALL)) {
			for(ModCategory category : ModCategory.values()) {
				if(category.equals(ModCategory.ALL) || category.equals(ModCategory.FAVORITE)) {
					continue;
				}
				for(Mod m : modManager.getMods()) {
					if(m.getCategory().equals(category) && !filterMod(m)) {
						result.add(m);
					}
				}
			}
		}else if(currentCategory.equals(ModCategory.FAVORITE)) {
			for(Mod m : modManager.getMods()) {
				if(MenuPrefs.getInstance().isFavorite(m.getNameKey()) && !filterMod(m)) {
					result.add(m);
				}
			}
		}else {
			for(Mod m : modManager.getMods()) {
				if(m.getCategory().equals(currentCategory) && !filterMod(m)) {
					result.add(m);
				}
			}
		}

		return result;
	}

	private boolean filterMod(Mod m) {

		if(m.isHide()) {
			return true;
		}

		String search = this.getSearchBox().getText();

		if(!search.isEmpty() && !SearchUtils.isSimillar(Mix.getInstance().getModManager().getWords(m), search)) {
			return true;
		}

		return false;
	}

	public int getTotalMods() {

		int count = 0;

		for(Mod m : Mix.getInstance().getModManager().getMods()) {
			if(!m.isHide()) {
				count++;
			}
		}

		return count;
	}

	private boolean hasSettings(Mod m) {

		ArrayList<Setting> settings = settingsByMod.get(m);
		return settings != null && !settings.isEmpty();
	}

	private int getColumns() {

		return Math.max(1, (int) ((getWidth() - (CONTENT_MARGIN * 2F) + CARD_GAP) / 232F));
	}

	public String getModIconFor(Mod m) {
		return getModIcon(m);
	}

	private String getModIcon(Mod m) {

		String name = m.getClass().getSimpleName();

		String dedicated = ModIcons.iconFor(name);

		if(dedicated != null) {
			return dedicated;
		}

		if(name.contains("FPS")) {
			return Icon.MONITOR;
		}

		if(name.contains("Clock")) {
			return Icon.CLOCK;
		}

		if(name.contains("Coords") || name.contains("Compass")) {
			return Icon.COMPASS;
		}

		if(name.contains("Music")) {
			return Icon.MUSIC;
		}

		if(name.contains("Screenshot")) {
			return Icon.CAMERA;
		}

		if(name.contains("Key")) {
			return Icon.EDIT;
		}

		switch(m.getCategory()) {
			case HUD:
				return Icon.LAYOUT;
			case RENDER:
				return Icon.IMAGE;
			case PLAYER:
				return Icon.USER;
			case WORLD:
				return Icon.MAP;
			default:
				return Icon.SLIDERS;
		}
	}

	private class ModuleSetting {

		private Setting setting;
		private Comp comp;
		private float rowY;

		public ModuleSetting(Setting setting, Comp comp) {
			this.setting = setting;
			this.comp = comp;
			this.rowY = 0;
		}
	}
}
