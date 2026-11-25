package mix.client.mixui.gui.modmenu.category.impl;

import java.awt.Color;
import java.io.File;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.modmenu.CategoryIslandRenderer;
import mix.client.mixui.gui.modmenu.GuiModMenu;
import mix.client.mixui.gui.modmenu.ModMenuTheme;
import mix.client.mixui.gui.modmenu.category.Category;
import mix.client.mixui.integration.dango.DangoMusicApi;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.impl.GlobalSettingsMod;
import mix.client.mixui.management.music.Music;
import mix.client.mixui.management.music.MusicManager;
import mix.client.mixui.management.music.MusicType;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.management.notification.NotificationType;
import mix.client.mixui.ui.comp.impl.CompSlider;
import mix.client.mixui.ui.comp.impl.field.CompTextBox;
import mix.client.mixui.utils.Multithreading;
import mix.client.mixui.utils.SearchUtils;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import mix.client.mixui.utils.mouse.MouseUtils;
import net.minecraft.util.ResourceLocation;

/**
 * 音乐页。
 *
 * <h3>这一版的形态</h3>
 * <p>之前顶部是「本地两个筛选 + 一颗『网易云』音源开关」，用户要的是把 DangoMusic 那套
 * 音乐界面搬进这一页里，而不是在页顶多挂一颗标签。所以现在那排胶囊不再是音源开关，
 * 而是 <b>页面导航</b>：本地 / 首页 / 搜索 / 歌单 / 队列，一页一颗。视觉完全沿用原来的
 * 胶囊 + 悬浮小岛（{@link CategoryIslandRenderer}），选中态仍然是 accent 渐变，
 * 只是语义从「切换音源」变成「切换页面」。切页面不动播放——播放状态属于播放器，
 * 不属于页面。</p>
 *
 * <h3>精简移植的取舍：只搬数据与页面结构，不搬控件</h3>
 * <ul>
 *   <li><b>不搬</b> Dango 的 GlassRenderContext / shader / 自绘控件栈。那套 canvas 管线
 *       自带字体加载与 GL 状态管理，塞进本页的 NanoVG 菜单会互相踩状态（之前「一进
 *       Dango 整屏界面就崩」走的就是这条路）。本页所有行、卡片统一用现成样式：
 *       {@link ModMenuTheme} 的 surface 分层 + accent 渐变，行高 / 圆角 / 字号与原来的
 *       本地音乐行一模一样。</li>
 *   <li><b>搬</b>：HOME（推荐歌单 + 榜单）、SEARCH（搜索 → 点行开播）、
 *       PLAYLIST（点歌单 → 看曲目 → 点曲目开播）、QUEUE（当前播放队列）。</li>
 *   <li><b>砍</b>：Dango 的 PARTY（一起听）与 SETTINGS。前者绑着它自己的 ListenParty
 *       房间栈与长连接状态机，后者就是它自己的登录 / 主题设置页，两者都离不开那套
 *       GuiCanvas，也不属于「音乐页」该有的东西。CHARTS（榜单）不单独占一页，
 *       并入 HOME 的第二个分组——它和推荐歌单是同一种行，单开一页只是把同样的行抄一遍，
 *       还会和「首页」互相抢首屏。</li>
 * </ul>
 *
 * <h3>本地筛选为什么不占顶部那排</h3>
 * <p>顶部那排已经是「页面」语义，再把「全部 / 收藏」塞进去，用户会分不清哪些胶囊换页、
 * 哪些只是过滤。所以本地页的两个筛选降级成内容区第一行的次级胶囊，随列表一起滚走：
 * 既不额外占用垂直空间，也不会和导航混淆，而且天然属于「本地这一页」。</p>
 *
 * <h3>播放条</h3>
 * <p>播放条不再跟「当前分区」绑定，而是跟「现在谁在出声」绑定：网易云在播就显示网易云，
 * 本地在播就显示本地；两边状态打平时再看最后一次开播用的是哪一路。否则在首页翻歌单时，
 * 播放条会突然变成「没有正在播放」，把用户正在听的东西藏起来。</p>
 *
 * <h3>拖动进度</h3>
 * <p>进度条本体只有 4 高，直接点几乎点不中，所以命中带上下各外扩 {@link #SEEK_HIT_PAD}。
 * 拖动采用「按下立即定位一次 → 拖动只改显示位置 → 松手再定位一次」，而不是边拖边 seek：
 * 网易云的 seek 是反射进它自己的音频管线，连续 seek 会有明显的爆音与卡顿，而用户的
 * 心理模型本来就是「拖到哪、松手从哪开始」。拖动期间高亮点与时间文字按手指位置画，
 * 不等 seek 回来。拖动中的位置刷新放在 drawScreen 里做，和 {@code CompSlider} 是同一套
 * 做法——GuiModMenu 不转发 mouseDragged，靠绘制帧读鼠标位置是这个仓库既有的惯例；
 * 松手用 {@code Mouse.isButtonDown(0)} 兜底，即使 release 事件没送到也能落一次 seek。</p>
 *
 * <p><b>本地源也能拖</b>：{@link MusicManager#seek(double)} 是这一版补上的定位入口
 * （内部就是 {@code mediaPlayer.seek(Duration.seconds(s))}，并把目标钳在
 * {@code [0, 总时长 - 0.25s]}，否则拖到最右端会立刻触发 {@code onEndOfMedia} 而直接换歌）。
 * 所以进度条上两个音源走同一套交互，区别只在最后那一次调用的名字。
 * 真正没有播放头时（列表空 / 时长还没解析出来）才不响应，用 {@code total <= 0} 判定。</p>
 */
public class MusicCategory extends Category {

	// 分类标签外面那座悬浮小岛，逻辑在 CategoryIslandRenderer 里，四个页面共用
	private final CategoryIslandRenderer chipIsland = new CategoryIslandRenderer();

	private static final float MARGIN = 24F;
	private static final float ROW_HEIGHT = 54F;
	private static final float ROW_GAP = 8F;
	private static final float BAR_HEIGHT = 64F;
	private static final float CHIP_HEIGHT = 26F;

	// 本地页内容区顶部的次级筛选行（全部 / 收藏）。它随列表滚走，高度要算进内容高度。
	private static final float LOCAL_FILTER_HEIGHT = 24F;
	private static final float LOCAL_FILTER_OFFSET = LOCAL_FILTER_HEIGHT + 12F;

	// 首页的分组标题行：只是一行小字，不该占掉一整行卡片的高度
	private static final float GROUP_HEADER_HEIGHT = 20F;
	private static final float GROUP_HEADER_GAP = 8F;

	// 进度条命中带在细条上下各外扩多少：4 高的条直接点几乎点不中
	private static final float SEEK_HIT_PAD = 8F;

	private static final float DANGO_SEARCH_HEIGHT = 26F;
	private static final float DANGO_SEARCH_GAP = 16F;
	private static final float DANGO_SEARCH_MIN_WIDTH = 132F;
	private static final float DANGO_SEARCH_MAX_WIDTH = 220F;
	private static final int DANGO_SEARCH_LIMIT = 30;

	/**
	 * 页面枚举：这一版的核心状态机。
	 *
	 * <p>原来那套 {@code dangoSource / dangoPlaylistMode / dangoResultMode /
	 * dangoPlaylistPickerMode} 几个布尔互相牵着走，同一时刻能表达出「在网易云源、
	 * 在歌单里、又在看搜索结果」这种自相矛盾的状态。现在只留一个枚举，每个页面自己
	 * 持有自己的数据，「当前看到什么」永远只有一种答案。</p>
	 */
	private enum Section {

		LOCAL(TranslateText.DANGO_NAV_LOCAL),
		HOME(TranslateText.DANGO_NAV_HOME),
		SEARCH(TranslateText.DANGO_NAV_SEARCH),
		PLAYLIST(TranslateText.DANGO_NAV_PLAYLIST),
		QUEUE(TranslateText.DANGO_NAV_QUEUE),
		// 「我喜欢的音乐」是网易云的系统歌单（specialType=5），Dango 的 NeteasePlaylist
		// 把它当普通歌单返回，所以这一页和 PLAYLIST 内层长得一样，只是数据源固定。
		FAVORITES(TranslateText.DANGO_NAV_FAVORITES);

		private final TranslateText label;

		private Section(TranslateText label) {
			this.label = label;
		}

		public String getLabel() {
			return label.getText();
		}
	}

	/**
	 * 首页的行模型访问器。
	 *
	 * <p>首页有两种行：分组标题（20 高的一行小字）和歌单行（54 高的卡片）。绘制、命中、
	 * 内容高度三处全部走 {@link #walkHomeRows}，几何只有这一份，改行距不会出现
	 * 「画的位置和点的位置差半行」。</p>
	 */
	private interface RowSink {
		void row(Object playlist, String header, float y);
	}

	private Section section = Section.LOCAL;

	private CompSlider volumeSlider;
	private MusicType currentType;
	private boolean openDownloader;
	private SimpleAnimation downloaderAnimation = new SimpleAnimation();
	private CompTextBox textBox = new CompTextBox();

	// ── 搜索页 ────────────────────────────────────────────────────────
	private final CompTextBox dangoSearchBox = new CompTextBox();
	private volatile java.util.List<Object> dangoResults = java.util.Collections.emptyList();
	private volatile boolean dangoSearching;
	private String dangoSearchMessage;

	// ── 首页 / 歌单页共用的歌单表 ──────────────────────────────────────
	// 推荐与榜单分开缓存：首页要把它们画成两个分组，合并后的那张表给「歌单」页用。
	private volatile java.util.List<Object> dangoRecommendPlaylists = java.util.Collections.emptyList();
	private volatile java.util.List<Object> dangoChartPlaylists = java.util.Collections.emptyList();
	private volatile java.util.List<Object> dangoAllPlaylists = java.util.Collections.emptyList();
	private volatile boolean dangoPlaylistsLoading;
	// 拉过一次就不再自动重试：否则「没登录 / 拉回来是空表」会让 drawScreen 每帧发一次网络请求。
	// 想重试就重新进一次页面（setSection 会把闸门放开）。
	private volatile boolean dangoPlaylistsTried;

	// ── 歌单页当前打开的那张歌单 ──────────────────────────────────────
	private Object dangoOpenPlaylist;
	// 整表替换而不是原地 clear+addAll：绘制线程在遍历它，原地改会抛 ConcurrentModificationException
	private volatile java.util.List<Object> dangoPlaylistTracks = java.util.Collections.emptyList();
	private volatile boolean dangoPlaylistTracksLoading;

	// ── 收藏页（网易云「我喜欢的音乐」）──────────────────────────────
	private volatile java.util.List<Object> dangoLikedTracks = java.util.Collections.emptyList();
	private volatile boolean dangoLikedLoading;
	// 与 dangoPlaylistsTried 同理：没登录时每帧重试会把渲染线程拖死
	private volatile boolean dangoLikedTried;

	// ── 播放条 ────────────────────────────────────────────────────────
	// 两路音源都可能「有内容」，播放条只能显示一个。lastStartedDango 记最后一次开播
	// 用的是哪一路，用来在两边状态完全打平时做决断。
	private boolean lastStartedDango;

	// 拖动进度：拖动期间只改本地显示位置，松手才真的 seek（理由见类注释）
	private boolean seeking;
	private float seekFraction;


	public MusicCategory(GuiModMenu parent) {
		super(parent, TranslateText.MUSIC, Icon.MUSIC, true);
	}

	@Override
	public void onShown() {
		// 从左侧栏切回这个分类时重播一次圆环入场动画
		chipIsland.reset();
	}

	@Override
	public void initGui() {
		// 打开菜单时回到本地页：那是这个页面「什么都没有时」的默认落点
		section = Section.LOCAL;
		currentType = MusicType.ALL;
		openDownloader = false;
		dangoResults = java.util.Collections.emptyList();
		dangoSearching = false;
		dangoSearchMessage = null;
		dangoRecommendPlaylists = java.util.Collections.emptyList();
		dangoChartPlaylists = java.util.Collections.emptyList();
		dangoAllPlaylists = java.util.Collections.emptyList();
		dangoPlaylistsLoading = false;
		dangoPlaylistsTried = false;
		dangoOpenPlaylist = null;
		dangoPlaylistTracks = java.util.Collections.emptyList();
		dangoPlaylistTracksLoading = false;
		dangoLikedTracks = java.util.Collections.emptyList();
		dangoLikedLoading = false;
		dangoLikedTried = false;
		seeking = false;
		seekFraction = 0F;
		dangoSearchBox.setText("");
		dangoSearchBox.setFocused(false);
		dangoSearchBox.setDefaultText(TranslateText.DANGO_SEARCH_HINT.getText());
		if(volumeSlider == null) {
			volumeSlider = new CompSlider(GlobalSettingsMod.getInstance().getVolumeSetting());
		}
	}

	@Override
	public void initCategory() {
		// 滚动位置由 GuiModMenu 负责恢复
	}

	@Override
	public boolean isSubPageOpen() {
		// 搜索框聚焦时算「子页面」：否则顶栏搜索框会被 Ctrl+F 同时点亮，
		// 一次按键进两个输入框，两边都变。
		return openDownloader || (section == Section.SEARCH && dangoSearchBox.isFocused());
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		AccentColor accent = instance.getColorManager().getCurrentColor();
		MusicManager musicManager = instance.getMusicManager();

		float chipY = getY() + 10F;
		// 视口上沿 / 内容顶部与小岛严格对齐，不另造坐标系
		float viewTop = CategoryIslandRenderer.viewportTop(chipY, CHIP_HEIGHT);
		float listTop = CategoryIslandRenderer.contentTop(chipY, CHIP_HEIGHT);

		float barX = barX();
		float barY = barY();
		float barWidth = barWidth();

		// 搜索页多一条内联搜索行，列表底部要让位，否则最后几行压在输入框底下点不到
		float listBottom = listBottomFor(barY);
		float listHeight = listBottom - viewTop - 8F;
		float dangoSearchY = dangoSearchRowY(barY);
		float dangoSearchWidth = dangoSearchFieldWidth();

		// 拖动中：每帧按鼠标 x 刷新显示位置；鼠标键一松就落一次 seek。
		// GuiModMenu 不转发 mouseDragged，CompSlider 也是这么做的（同一套既有惯例）。
		updateSeekDrag(mouseX);

		float chipRowWidth = chipRowWidth(nvg);
		float chipStartX = getX() + MARGIN;

		// 首页 / 歌单页要的数据：内部有「已加载 / 正在加载」闸门，每帧调也只发一次请求
		if(section == Section.HOME || section == Section.PLAYLIST) {
			loadDangoPlaylists();
		}
		if(section == Section.FAVORITES) {
			loadDangoLiked();
		}

		// 滚动上限 = 小岛那段空白 + 内容高度 - 视口高度
		float contentHeight = sectionContentHeight(musicManager, listTop);
		scroll.setMaxScroll(Math.max(0F,
				CategoryIslandRenderer.topSpacer(CHIP_HEIGHT) + contentHeight - listHeight));

		// 列表：裁剪上沿严格等于视口上沿，行向上滚时被小岛盖住，而不是被硬切一条线
		nvg.save();
		nvg.scissor(getX(), viewTop, getWidth(), listHeight);
		nvg.translate(0, scroll.getValue());
		drawSectionRows(nvg, mouseX, mouseY, listTop, chipStartX, chipY, chipRowWidth);
		nvg.restore();

		// 列表画完再画小岛与渐隐带：小岛是浮在列表上的独立胶囊，行从它背后滚过去
		CategoryIslandRenderer.drawScrollFade(nvg, getX(), getWidth(), chipY, CHIP_HEIGHT, ModMenuTheme.surface());
		drawSectionChips(nvg, mouseX, mouseY, accent, chipStartX, chipY, chipRowWidth);

		// 歌单页进了某张歌单之后，右侧多一颗「返回」：它是退出歌单的鼠标入口（ESC 也行）
		if(section == Section.PLAYLIST && dangoOpenPlaylist != null) {
			drawBackChip(nvg, mouseX, mouseY, chipY);
		}

		// 搜索页的内联搜索行：只在搜索页出现，它是这一页的主控件
		if(section == Section.SEARCH) {
			drawDangoSearchRow(nvg, mouseX, mouseY, barX, dangoSearchY, dangoSearchWidth);
		}

		// Downloader popover：从播放条上方弹出来，必须画在播放条之前
		downloaderAnimation.setAnimation(openDownloader ? 1.0F : 0.0F, 16);
		if(openDownloader || downloaderAnimation.getValue() > 0.01F) {
			nvg.save();
			nvg.translate(0, 50 - (downloaderAnimation.getValue() * 50));
			float downloaderX = barX + barWidth - 260F;
			float downloaderY = barY - 46F;
			nvg.drawRoundedRect(downloaderX, downloaderY, 250, 38, 12, ModMenuTheme.surfaceContainer());
			textBox.setPosition(downloaderX + 8, downloaderY + 7, 198, 24);
			textBox.draw(mouseX, mouseY, partialTicks);
			nvg.drawRoundedRect(downloaderX + 214, downloaderY + 7, 24, 24, 12, ModMenuTheme.primary());
			nvg.drawCenteredText(Icon.DOWNLOAD, downloaderX + 226, downloaderY + 12, Color.WHITE, 12, Fonts.ICON);
			nvg.restore();
		}

		drawPlayerBar(nvg, mouseX, mouseY, partialTicks, musicManager, accent, barX, barY, barWidth);
		musicManager.setVolume();
	}

	// ══════════════════════════════════════════════════════════════════
	//  顶部导航（原「音源开关」，现在是「页面」）
	// ══════════════════════════════════════════════════════════════════

	/** 单个导航胶囊的宽度：文案宽 + 左右各 13 的内边距。 */
	private float chipWidth(NanoVGManager nvg, Section s) {
		return nvg.getTextWidth(s.getLabel(), 10.5F, Fonts.MEDIUM) + 26F;
	}

	/** 整排胶囊的总宽（含间距），小岛的宽度完全由它决定。 */
	private float chipRowWidth(NanoVGManager nvg) {
		Section[] all = Section.values();
		float width = 0F;
		for(int i = 0; i < all.length; i++) {
			width += chipWidth(nvg, all[i]);
			if(i < all.length - 1) {
				width += 8F;
			}
		}
		return width;
	}

	/**
	 * 画顶部导航。
	 *
	 * <p>视觉与原来的 chip 行完全一致：未选中是 surfaceContainer 胶囊、悬停抬一档，
	 * 选中是 accent 渐变 + 白字。区别只在语义——这里换的是页面，不是音源。</p>
	 */
	private void drawSectionChips(NanoVGManager nvg, int mouseX, int mouseY, AccentColor accent,
			float chipStartX, float chipY, float chipRowWidth) {

		chipIsland.draw(nvg, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT, getX() + getWidth() - MARGIN);

		float chipX = chipStartX;
		for(Section s : Section.values()) {
			float width = chipWidth(nvg, s);
			boolean selected = s == section;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, chipX, chipY, width, CHIP_HEIGHT);
			String label = s.getLabel();
			if(selected) {
				nvg.drawGradientRoundedRect(chipX, chipY, width, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
						accent.getColor1(), accent.getColor2());
				nvg.drawText(label, chipX + 13F, chipY + 8F, Color.WHITE, 10.5F, Fonts.MEDIUM);
			}else {
				nvg.drawRoundedRect(chipX, chipY, width, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
						hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
				nvg.drawText(label, chipX + 13F, chipY + 8F, ModMenuTheme.onSurfaceVariant(), 10.5F, Fonts.MEDIUM);
			}
			chipX += width + 8F;
		}
	}

	/** 歌单页内的「返回」：靠右独立一颗，不挤进导航小岛（否则进一次歌单小岛就变宽一次）。 */
	private void drawBackChip(NanoVGManager nvg, int mouseX, int mouseY, float chipY) {
		String label = TranslateText.DANGO_PLAYLIST_BACK.getText();
		float width = nvg.getTextWidth(label, 10.5F, Fonts.MEDIUM) + 26F;
		float x = getX() + getWidth() - MARGIN - width;
		boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, chipY, width, CHIP_HEIGHT);
		nvg.drawRoundedRect(x, chipY, width, CHIP_HEIGHT, CHIP_HEIGHT / 2F,
				hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainer());
		nvg.drawText(label, x + 13F, chipY + 8F, ModMenuTheme.onSurfaceVariant(), 10.5F, Fonts.MEDIUM);
	}

	/**
	 * 切换页面。
	 *
	 * <p>不碰播放状态：播放属于播放器，不属于页面——这正是「切个页歌就没了」要根治的地方。
	 * 只收两类东西：输入焦点（离开搜索页就失焦）和已打开的歌单（离开歌单页才丢，
	 * 在页内去队列看一眼再回来还在）。</p>
	 */
	private void setSection(Section next) {
		if(next == null || next == section) {
			return;
		}
		section = next;
		if(section != Section.SEARCH) {
			dangoSearchBox.setFocused(false);
		}
		if(section != Section.PLAYLIST) {
			dangoOpenPlaylist = null;
			dangoPlaylistTracks = java.util.Collections.emptyList();
			dangoPlaylistTracksLoading = false;
		}
		// 重新进首页 / 歌单页时放开「已拉过一次」的闸门：登录成功之后不用重启就能看到歌单
		if((section == Section.HOME || section == Section.PLAYLIST)
				&& dangoRecommendPlaylists.isEmpty() && dangoChartPlaylists.isEmpty()) {
			dangoPlaylistsTried = false;
		}
		// 收藏页同理：先收藏再切页回来、或者中途才登录，都该重新拉一次
		if(section == Section.FAVORITES && dangoLikedTracks.isEmpty()) {
			dangoLikedTried = false;
		}
		scroll.setScrollPosition(0F);
	}

	// ══════════════════════════════════════════════════════════════════
	//  列表
	// ══════════════════════════════════════════════════════════════════

	/** 内容总高度（不含小岛那段空白）：滚动上限按它算。 */
	private float sectionContentHeight(MusicManager musicManager, float listTop) {
		switch(section) {
			case LOCAL:
				return LOCAL_FILTER_OFFSET + listSpan(countLocalRows(musicManager));
			case HOME:
				return Math.max(0F, walkHomeRows(listTop, null) - listTop - ROW_GAP);
			case PLAYLIST:
				if(dangoOpenPlaylist != null) {
					return listSpan(dangoPlaylistTracks.size());
				}
				return listSpan(dangoAllPlaylists.size());
			case SEARCH:
				return listSpan(dangoResults.size());
			case QUEUE:
				return listSpan(DangoMusicApi.queue().size());
			case FAVORITES:
				return listSpan(dangoLikedTracks.size());
			default:
				return 0F;
		}
	}

	/** n 行的总高度；n = 0 时是 0（没有尾行间距）。 */
	private float listSpan(int count) {
		return count <= 0 ? 0F : count * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
	}

	private int countLocalRows(MusicManager musicManager) {
		int count = 0;
		for(Music m : musicManager.getMusics()) {
			if(!filter(m)) {
				count++;
			}
		}
		return count;
	}

	private void drawSectionRows(NanoVGManager nvg, int mouseX, int mouseY,
			float listTop, float chipStartX, float chipY, float chipRowWidth) {
		switch(section) {
			case LOCAL:
				drawLocalFilterRow(nvg, mouseX, mouseY, listTop);
				drawLocalRows(nvg, mouseX, mouseY, Mix.getInstance().getMusicManager(),
						listTop + LOCAL_FILTER_OFFSET, chipStartX, chipY, chipRowWidth);
				break;
			case HOME:
				drawHomeRows(nvg, mouseX, mouseY, listTop, chipStartX, chipY, chipRowWidth);
				break;
			case PLAYLIST:
				if(dangoOpenPlaylist != null) {
					drawTrackRows(nvg, mouseX, mouseY, listTop, chipStartX, chipY, chipRowWidth,
							dangoPlaylistTracks, false);
					if(dangoPlaylistTracks.isEmpty()) {
						drawEmptyHint(nvg, listTop, dangoPlaylistTracksLoading
								? TranslateText.DANGO_SEARCHING.getText()
								: TranslateText.DANGO_PLAYLIST_TRACKS_EMPTY.getText());
					}
				}else {
					drawPlaylistRows(nvg, mouseX, mouseY, listTop, chipStartX, chipY, chipRowWidth,
							dangoAllPlaylists);
				}
				break;
			case SEARCH:
				drawTrackRows(nvg, mouseX, mouseY, listTop, chipStartX, chipY, chipRowWidth,
						dangoResults, false);
				if(dangoResults.isEmpty()) {
					drawEmptyHint(nvg, listTop, dangoSearchMessage != null
							? dangoSearchMessage : TranslateText.DANGO_SEARCH_EMPTY.getText());
				}
				break;
			case QUEUE:
				java.util.List<Object> queue = DangoMusicApi.queue();
				drawTrackRows(nvg, mouseX, mouseY, listTop, chipStartX, chipY, chipRowWidth, queue, true);
				if(queue.isEmpty()) {
					drawEmptyHint(nvg, listTop, TranslateText.DANGO_QUEUE_EMPTY.getText());
				}
				break;
			case FAVORITES:
				java.util.List<Object> liked = dangoLikedTracks;
				drawTrackRows(nvg, mouseX, mouseY, listTop, chipStartX, chipY, chipRowWidth, liked, false);
				if(liked.isEmpty()) {
					// 未登录时先提示去登录：空表在这个页面上有两种完全不同的原因
					drawEmptyHint(nvg, listTop, !DangoMusicApi.isNeteaseLoggedIn()
							? TranslateText.DANGO_FAVORITES_LOGIN.getText()
							: (dangoLikedLoading
									? TranslateText.DANGO_SEARCHING.getText()
									: TranslateText.DANGO_FAVORITES_EMPTY.getText()));
				}
				break;
			default:
				break;
		}
	}

	/**
	 * 本地页的次级筛选（全部 / 收藏）。
	 *
	 * <p>它不再是顶层导航：顶部那排现在是「页面」，混进筛选会让用户分不清哪些胶囊换页、
	 * 哪些只过滤。放在内容区第一行、跟着列表一起滚，既不额外占用垂直空间，
	 * 也天然属于「本地这一页」。</p>
	 */
	private void drawLocalFilterRow(NanoVGManager nvg, int mouseX, int mouseY, float listTop) {
		AccentColor accent = Mix.getInstance().getColorManager().getCurrentColor();
		float x = getX() + MARGIN;
		for(MusicType type : MusicType.values()) {
			String name = type.getName();
			float width = nvg.getTextWidth(name, 9.5F, Fonts.MEDIUM) + 22F;
			boolean selected = type.equals(currentType);
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, listTop + scroll.getValue(),
					width, LOCAL_FILTER_HEIGHT);
			if(selected) {
				nvg.drawGradientRoundedRect(x, listTop, width, LOCAL_FILTER_HEIGHT, LOCAL_FILTER_HEIGHT / 2F,
						accent.getColor1(), accent.getColor2());
				nvg.drawText(name, x + 11F, listTop + 7F, Color.WHITE, 9.5F, Fonts.MEDIUM);
			}else {
				nvg.drawRoundedRect(x, listTop, width, LOCAL_FILTER_HEIGHT, LOCAL_FILTER_HEIGHT / 2F,
						hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
				nvg.drawText(name, x + 11F, listTop + 7F, ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.MEDIUM);
			}
			x += width + 6F;
		}
	}

	/** 本地音乐行：封面 / 标题 / 分类 / 收藏 / 删除，和改造前一致。 */
	private void drawLocalRows(NanoVGManager nvg, int mouseX, int mouseY, MusicManager musicManager,
			float listTop, float chipStartX, float chipY, float chipRowWidth) {

		int index = 0;
		for(Music m : musicManager.getMusics()) {
			if(filter(m)) {
				continue;
			}
			float rowY = listTop + index * (ROW_HEIGHT + ROW_GAP);
			float rowX = getX() + MARGIN;
			float rowWidth = getWidth() - MARGIN * 2F;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, rowX, rowY + scroll.getValue(), rowWidth, ROW_HEIGHT)
					&& !CategoryIslandRenderer.overIsland(mouseX, mouseY, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT);
			nvg.drawRoundedRect(rowX, rowY, rowWidth, ROW_HEIGHT, 12,
					hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
			if(m.getIcon() == null) {
				nvg.drawRoundedImage(new ResourceLocation("mixclient/music.png"), rowX + 8, rowY + 8, 38, 38, 10);
			}else {
				nvg.drawRoundedImage(m.getIcon(), rowX + 8, rowY + 8, 38, 38, 10);
			}
			nvg.drawText(nvg.getLimitText(m.getName(), 11, Fonts.MEDIUM, Math.max(140F, rowWidth - 220F)),
					rowX + 58, rowY + 11, ModMenuTheme.onSurface(), 11, Fonts.MEDIUM);
			nvg.drawText(m.getType().getName(), rowX + 58, rowY + 30,
					ModMenuTheme.onSurfaceVariant(), 9, Fonts.REGULAR);
			m.getFavoriteAnimation().setAnimation(m.getType().equals(MusicType.FAVORITE) ? 1.0F : 0.0F, 16);
			nvg.drawText(Icon.STAR, rowX + rowWidth - 58, rowY + 20,
					ModMenuTheme.onSurfaceVariant(), 13, Fonts.ICON);
			nvg.drawText(Icon.STAR_FILL, rowX + rowWidth - 58, rowY + 20,
					new Color(255, 193, 74, (int) (m.getFavoriteAnimation().getValue() * 255)), 13, Fonts.ICON);
			nvg.drawText(Icon.TRASH, rowX + rowWidth - 34, rowY + 20, ModMenuTheme.error(), 13, Fonts.ICON);
			index++;
		}

		if(index == 0) {
			drawEmptyHint(nvg, listTop, TranslateText.DANGO_LOCAL_EMPTY.getText());
		}
	}

	/**
	 * 曲目行：搜索页、歌单内、队列页共用同一个画法。
	 *
	 * <p>三种页面的数据形状完全一样（都是 NeteaseTrack 列表），区别只在「当前播放的那一行
	 * 要不要高亮」——只有队列页的下标和播放器下标在同一套坐标系里，所以在搜索页 /
	 * 歌单页按歌名匹配会误标，干脆不高亮。</p>
	 */
	private void drawTrackRows(NanoVGManager nvg, int mouseX, int mouseY,
			float listTop, float chipStartX, float chipY, float chipRowWidth,
			java.util.List<Object> tracks, boolean markCurrent) {

		int playingIndex = markCurrent ? DangoMusicApi.queueIndex() : -1;
		for(int i = 0; i < tracks.size(); i++) {
			Object track = tracks.get(i);
			float rowY = listTop + i * (ROW_HEIGHT + ROW_GAP);
			float rowX = getX() + MARGIN;
			float rowWidth = getWidth() - MARGIN * 2F;
			boolean active = markCurrent && i == playingIndex;
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, rowX, rowY + scroll.getValue(), rowWidth, ROW_HEIGHT)
					&& !CategoryIslandRenderer.overIsland(mouseX, mouseY, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT);
			nvg.drawRoundedRect(rowX, rowY, rowWidth, ROW_HEIGHT, 12, hovered
					? ModMenuTheme.surfaceContainerHigh()
					: (active ? ModMenuTheme.mix(ModMenuTheme.surfaceContainerLow(), ModMenuTheme.primary(), 0.14F)
							: ModMenuTheme.surfaceContainerLow()));
			ResourceLocation cover = DangoMusicApi.coverOf(track);
			if(cover == null) {
				nvg.drawRoundedImage(new ResourceLocation("mixclient/music.png"), rowX + 8, rowY + 8, 38, 38, 10);
			}else {
				nvg.drawRoundedTexture(cover, rowX + 8, rowY + 8, 38, 38, 10);
			}
			float textMax = Math.max(140F, rowWidth - 220F);
			nvg.drawText(nvg.getLimitText(DangoMusicApi.nameOf(track), 11, Fonts.MEDIUM, textMax),
					rowX + 58, rowY + 11, ModMenuTheme.onSurface(), 11, Fonts.MEDIUM);
			nvg.drawText(nvg.getLimitText(DangoMusicApi.artistsOf(track), 9, Fonts.REGULAR, textMax),
					rowX + 58, rowY + 30, ModMenuTheme.onSurfaceVariant(), 9, Fonts.REGULAR);
			String duration = formatDuration(DangoMusicApi.durationOf(track));
			nvg.drawText(duration, rowX + rowWidth - 20 - nvg.getTextWidth(duration, 9, Fonts.REGULAR),
					rowY + 21, ModMenuTheme.onSurfaceVariant(), 9, Fonts.REGULAR);
			if(active) {
				nvg.drawText(Icon.MUSIC, rowX + rowWidth - 40, rowY + 20,
						ModMenuTheme.primary(), 11, Fonts.ICON);
			}
		}
	}

	/** 歌单行列表：首页两个分组与歌单页共用。 */
	private void drawPlaylistRows(NanoVGManager nvg, int mouseX, int mouseY,
			float listTop, float chipStartX, float chipY, float chipRowWidth,
			java.util.List<Object> playlists) {

		for(int i = 0; i < playlists.size(); i++) {
			drawPlaylistRow(nvg, mouseX, mouseY, playlists.get(i),
					listTop + i * (ROW_HEIGHT + ROW_GAP), chipStartX, chipY, chipRowWidth);
		}
		if(playlists.isEmpty()) {
			drawEmptyHint(nvg, listTop, dangoPlaylistsLoading
					? TranslateText.DANGO_SEARCHING.getText()
					: TranslateText.DANGO_PLAYLIST_EMPTY.getText());
		}
	}

	private void drawPlaylistRow(NanoVGManager nvg, int mouseX, int mouseY, Object playlist, float rowY,
			float chipStartX, float chipY, float chipRowWidth) {

		float rowX = getX() + MARGIN;
		float rowWidth = getWidth() - MARGIN * 2F;
		boolean hovered = MouseUtils.isInside(mouseX, mouseY, rowX, rowY + scroll.getValue(), rowWidth, ROW_HEIGHT)
				&& !CategoryIslandRenderer.overIsland(mouseX, mouseY, chipStartX, chipY, chipRowWidth, CHIP_HEIGHT);
		nvg.drawRoundedRect(rowX, rowY, rowWidth, ROW_HEIGHT, 12,
				hovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
		// 拿不到歌单封面（要另外拉图），用一枚图标交代「这是一叠歌」
		nvg.drawRoundedRect(rowX + 8, rowY + 8, 38, 38, 10,
				ModMenuTheme.mix(ModMenuTheme.surfaceContainer(), ModMenuTheme.primary(), 0.18F));
		nvg.drawCenteredText(Icon.LIST, rowX + 27F, rowY + 19F, ModMenuTheme.primary(), 14, Fonts.ICON);
		String name = DangoMusicApi.playlistNameOf(playlist);
		if(name == null || name.isEmpty()) {
			name = TranslateText.DANGO_PLAYLIST_UNNAMED.getText();
		}
		nvg.drawText(nvg.getLimitText(name, 11, Fonts.MEDIUM, Math.max(140F, rowWidth - 120F)),
				rowX + 58, rowY + 11, ModMenuTheme.onSurface(), 11, Fonts.MEDIUM);
		String count = DangoMusicApi.playlistTrackCount(playlist) + " "
				+ TranslateText.DANGO_PLAYLIST_SONGS.getText();
		nvg.drawText(count, rowX + 58, rowY + 30, ModMenuTheme.onSurfaceVariant(), 9, Fonts.REGULAR);
		nvg.drawText(Icon.FORWARD, rowX + rowWidth - 30, rowY + 20,
				ModMenuTheme.onSurfaceVariant(), 11, Fonts.ICON);
	}

	/**
	 * 首页：两组（推荐 / 榜单），每组前面一行分组标题。
	 *
	 * <p>分组标题不是卡片，只是 20 高的一行小字；它和歌单行走同一个步进函数
	 * {@link #walkPlaylistGroup}，保证「画到哪」和「点到哪」永远一致。</p>
	 */
	private void drawHomeRows(NanoVGManager nvg, int mouseX, int mouseY,
			float listTop, float chipStartX, float chipY, float chipRowWidth) {

		float y = listTop;
		y = drawPlaylistGroup(y, TranslateText.DANGO_HOME_RECOMMEND.getText(),
				dangoRecommendPlaylists, nvg, mouseX, mouseY, chipStartX, chipY, chipRowWidth);
		drawPlaylistGroup(y, TranslateText.DANGO_HOME_CHARTS.getText(),
				dangoChartPlaylists, nvg, mouseX, mouseY, chipStartX, chipY, chipRowWidth);

		if(dangoRecommendPlaylists.isEmpty() && dangoChartPlaylists.isEmpty()) {
			drawEmptyHint(nvg, listTop, dangoPlaylistsLoading
					? TranslateText.DANGO_SEARCHING.getText()
					: (DangoMusicApi.isNeteaseLoggedIn()
							? TranslateText.DANGO_PLAYLIST_EMPTY.getText()
							: TranslateText.DANGO_SEARCH_LOGIN.getText()));
		}
	}

	/** 画一组「标题 + 歌单」，返回下一个空位的 y。与 {@link #walkPlaylistGroup} 步进一致。 */
	private float drawPlaylistGroup(float y, String header, java.util.List<Object> group,
			NanoVGManager nvg, int mouseX, int mouseY,
			float chipStartX, float chipY, float chipRowWidth) {

		if(group.isEmpty()) {
			return y;
		}
		nvg.drawText(header, getX() + MARGIN, y + 4F, ModMenuTheme.onSurfaceVariant(), 9.5F, Fonts.MEDIUM);
		y += GROUP_HEADER_HEIGHT + GROUP_HEADER_GAP;
		for(int i = 0; i < group.size(); i++) {
			drawPlaylistRow(nvg, mouseX, mouseY, group.get(i), y, chipStartX, chipY, chipRowWidth);
			y += ROW_HEIGHT + ROW_GAP;
		}
		return y;
	}

	/**
	 * 遍历首页全部行（分组标题 + 歌单），返回下一个空位的 y。
	 *
	 * <p>滚动高度和命中判定都调它：几何只有这一份，改行距不会出现「画的位置和点的位置
	 * 差半行」。sink 为 null 时只算高度不回调。</p>
	 */
	private float walkHomeRows(float listTop, RowSink sink) {
		float y = listTop;
		y = walkPlaylistGroup(y, TranslateText.DANGO_HOME_RECOMMEND.getText(), dangoRecommendPlaylists, sink);
		y = walkPlaylistGroup(y, TranslateText.DANGO_HOME_CHARTS.getText(), dangoChartPlaylists, sink);
		return y;
	}

	private float walkPlaylistGroup(float y, String header, java.util.List<Object> group, RowSink sink) {
		if(group.isEmpty()) {
			return y;
		}
		if(sink != null) {
			sink.row(null, header, y);
		}
		y += GROUP_HEADER_HEIGHT + GROUP_HEADER_GAP;
		for(int i = 0; i < group.size(); i++) {
			if(sink != null) {
				sink.row(group.get(i), null, y);
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		return y;
	}

	/** 空列表提示：一句明确的话，而不是一片空白。 */
	private void drawEmptyHint(NanoVGManager nvg, float y, String hint) {
		nvg.drawText(nvg.getLimitText(hint, 10.5F, Fonts.REGULAR, getWidth() - MARGIN * 2F),
				getX() + MARGIN, y + 14F, ModMenuTheme.onSurfaceVariant(), 10.5F, Fonts.REGULAR);
	}

	// ══════════════════════════════════════════════════════════════════
	//  播放条几何（绘制与命中共用同一份，避免两处各算一套）
	// ══════════════════════════════════════════════════════════════════

	private float barX() {
		return getX() + MARGIN;
	}

	private float barY() {
		return getY() + getHeight() - BAR_HEIGHT - 8F;
	}

	private float barWidth() {
		return getWidth() - MARGIN * 2F;
	}

	/** 进度条长度跟标题宽度一致：两者共用同一条左边线，看起来是一列。 */
	private float progressWidth() {
		return Math.max(100F, Math.min(barWidth() * 0.28F, 220F));
	}

	private float progressX() {
		return barX() + 64F;
	}

	private float progressY() {
		return barY() + 40F;
	}

	// ══════════════════════════════════════════════════════════════════
	//  播放条（与音源解耦）
	// ══════════════════════════════════════════════════════════════════

	/**
	 * 播放条该显示哪一路。
	 *
	 * <p>判断依据是「现在谁在出声」，不是「当前在哪个页面」：在首页翻歌单时，
	 * 用户正在听的本地歌不能因为页面切到网易云就变成「没有正在播放」。</p>
	 */
	private boolean isDangoBar(MusicManager musicManager) {
		boolean dangoHas = DangoMusicApi.isAvailable() && DangoMusicApi.current() != null;
		boolean localHas = musicManager.getCurrentMusic() != null;
		boolean dangoPlaying = dangoHas && DangoMusicApi.isPlaying();
		boolean localPlaying = localHas && musicManager.isPlaying();
		if(dangoPlaying != localPlaying) {
			return dangoPlaying;
		}
		if(dangoHas != localHas) {
			return dangoHas;
		}
		// 两边状态完全打平（都在播 / 都停着 / 都没有内容）时，按最后一次开播的那一路算
		return lastStartedDango;
	}

	/** 播放条总时长（毫秒）；没有内容时为 0。两个音源的单位在这里统一。 */
	private long playerTotalMs(boolean dangoBar, MusicManager musicManager) {
		long total = dangoBar ? DangoMusicApi.durationMs()
				: (long) (musicManager.getEndTime() * 1000F);
		return Math.max(0L, total);
	}

	/** 播放条播放头（毫秒）；拖动期间返回手指位置，这样高亮点是跟手的。 */
	private long playerNowMs(boolean dangoBar, MusicManager musicManager, float barProgress) {
		long total = playerTotalMs(dangoBar, musicManager);
		if(total <= 0L) {
			return 0L;
		}
		if(seeking) {
			return (long) (seekFraction * total);
		}
		long now = dangoBar ? DangoMusicApi.positionMs()
				: (long) (musicManager.getCurrentTime() * 1000F);
		return Math.max(0L, Math.min(total, now));
	}

	private void drawPlayerBar(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks,
			MusicManager musicManager, AccentColor accent, float barX, float barY, float barWidth) {

		nvg.drawRoundedRect(barX, barY, barWidth, BAR_HEIGHT, 14, ModMenuTheme.surfaceContainer());

		boolean dangoBar = isDangoBar(musicManager);
		Object dangoTrack = dangoBar ? DangoMusicApi.current() : null;
		Music localTrack = dangoBar ? null : musicManager.getCurrentMusic();
		boolean barIdle = dangoBar ? dangoTrack == null : localTrack == null;
		boolean barPlaying = dangoBar ? DangoMusicApi.isPlaying() : musicManager.isPlaying();

		String barTitle;
		if(dangoBar) {
			barTitle = dangoTrack == null ? TranslateText.DANGO_EMPTY.getText() : DangoMusicApi.nameOf(dangoTrack);
		}else {
			barTitle = localTrack == null ? TranslateText.NOTHING_IS_PLAYING.getText() : localTrack.getName();
		}

		long totalMs = playerTotalMs(dangoBar, musicManager);
		// 拖动期间用手指位置，不等 seek 回来：高亮点与时间文字必须是跟手的
		float barProgress;
		if(seeking) {
			barProgress = seekFraction;
		}else if(totalMs <= 0L) {
			barProgress = 0F;
		}else {
			long now = dangoBar ? DangoMusicApi.positionMs()
					: (long) (musicManager.getCurrentTime() * 1000F);
			barProgress = Math.min(1F, Math.max(0F, (float) now / (float) totalMs));
		}
		long nowMs = playerNowMs(dangoBar, musicManager, barProgress);

		// 封面：网易云是运行期纹理（ResourceLocation），本地是磁盘文件（File），分开取、分开画
		ResourceLocation dangoCoverArt = (dangoBar && dangoTrack != null) ? DangoMusicApi.coverOf(dangoTrack) : null;
		File localCoverArt = (!dangoBar && localTrack != null) ? localTrack.getIcon() : null;
		if(dangoCoverArt != null) {
			nvg.drawRoundedTexture(dangoCoverArt, barX + 10, barY + 10, 44, 44, 11);
		}else if(localCoverArt != null) {
			nvg.drawRoundedImage(localCoverArt, barX + 10, barY + 10, 44, 44, 11);
		}else {
			nvg.drawRoundedImage(new ResourceLocation("mixclient/music.png"), barX + 10, barY + 10, 44, 44, 11);
		}

		float titleWidth = progressWidth();
		nvg.drawText(nvg.getLimitText(barTitle, 11.5F, Fonts.MEDIUM, titleWidth), barX + 64, barY + 12,
				ModMenuTheme.onSurface(), 11.5F, Fonts.MEDIUM);

		// ── 进度条 ────────────────────────────────────────────────────
		// 两个音源都能拖：本地走 MusicManager.seek(秒)，网易云走 DangoMusicApi.seekTo(毫秒)
		boolean seekable = !barIdle && totalMs > 0L;
		boolean seekHovered = insideSeekBar(mouseX, mouseY);

		nvg.drawRoundedRect(progressX(), progressY(), progressWidth(), 4, 2,
				ModMenuTheme.surfaceContainerHighest());
		if(!barIdle && barProgress > 0F) {
			nvg.drawGradientRoundedRect(progressX(), progressY(),
					Math.max(4F, progressWidth() * barProgress), 4, 2,
					accent.getColor1(), accent.getColor2());
		}
		// 高亮点：只有能拖的时候才画；悬停时露出、拖动时放大并跟着手指走
		if(seekable && (seekHovered || seeking)) {
			float knobX = progressX() + Math.max(0F, Math.min(progressWidth(), progressWidth() * barProgress));
			nvg.drawGradientCircle(knobX, progressY() + 2F, seeking ? 5.5F : 4.2F,
					accent.getColor1(), accent.getColor2());
			nvg.drawCircle(knobX, progressY() + 2F, seeking ? 2.2F : 1.6F, ModMenuTheme.surfaceContainer());
		}
		// 时间文字：拖动时按手指位置换算，松手后回到真实位置
		if(totalMs > 0L) {
			nvg.drawText(formatDuration(nowMs) + " / " + formatDuration(totalMs),
					progressX() + progressWidth() + 10F, progressY() - 2F,
					ModMenuTheme.onSurfaceVariant(), 8.5F, Fonts.REGULAR);
		}

		// ── 中间控制 ──────────────────────────────────────────────────
		float centerX = barX + (barWidth / 2F);
		drawBarControl(nvg, Icon.BACK, centerX - 58, barY + 19, 26, false);
		drawBarControl(nvg, barIdle || !barPlaying ? Icon.PLAY : Icon.PAUSE, centerX - 17, barY + 15, 34, true);
		drawBarControl(nvg, Icon.FORWARD, centerX + 32, barY + 19, 26, false);

		// ── 添加歌曲 ──────────────────────────────────────────────────
		float addWidth = 86F;
		float addX = barX + barWidth - addWidth - 10F;
		boolean addHovered = MouseUtils.isInside(mouseX, mouseY, addX, barY + 18, addWidth, 28);
		nvg.drawRoundedRect(addX, barY + 18, addWidth, 28, 14,
				addHovered ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
		nvg.drawCenteredText("+ " + TranslateText.ADD_SONG.getText(), addX + (addWidth / 2F), barY + 25,
				ModMenuTheme.onSurface(), 9, Fonts.MEDIUM);

		// ── 音量 ──────────────────────────────────────────────────────
		float volumeX = addX - 92F;
		float volume = volumeSlider.getSetting().getValueFloat();
		String volumeIcon = Icon.VOLUME;
		if(volume == 0) {
			volumeIcon = Icon.VOLUME_X;
		}else if(volume > 0.8F) {
			volumeIcon = Icon.VOLUME_2;
		}else if(volume > 0.4F) {
			volumeIcon = Icon.VOLUME_1;
		}
		nvg.drawText(volumeIcon, volumeX, barY + 30, ModMenuTheme.onSurfaceVariant(), 16, Fonts.ICON);
		volumeSlider.setX(volumeX + 22);
		volumeSlider.setY(barY + 37);
		volumeSlider.setWidth(58);
		volumeSlider.setHeight(4.5);
		volumeSlider.setCircle(false);
		volumeSlider.setShowValue(false);
		volumeSlider.draw(mouseX, mouseY, partialTicks);

		// 这颗滑条以前只喂本地 JavaFX 播放器（MusicManager.setVolume），
		// 网易云在播时拖它完全没有反应，必须跑去 Dango 自己的界面才有用。
		// 现在按「谁在播就控谁」分派：播放条显示的是网易云，就写 Dango 的音量。
		applyVolumeToActiveSource(musicManager, dangoBar);
	}

	/**
	 * 把滑条的值落到真正在播的那一路。
	 *
	 * <p>两条播放器的音量是完全独立的：本地是 JavaFX {@code MediaPlayer.setVolume}，
	 * 网易云是 Dango {@code MusicPlayer} 里那个 static volatile float。
	 * 只写一路的话，另一路在播时滑条就是死的。</p>
	 *
	 * <p>每帧都写而不是只在拖动时写：Dango 那边是「比对后发现不同才重新应用到
	 * SourceDataLine」，重复写同一个值没有代价；而漏写就会表现成
	 * 「切歌之后音量回到 0.5」——{@code MusicPlayer} 的默认值是 0.5。</p>
	 */
	private void applyVolumeToActiveSource(MusicManager musicManager, boolean dangoBar) {

		float volume = volumeSlider.getSetting().getValueFloat();

		if(dangoBar) {
			DangoMusicApi.setVolume(volume);
		}else {
			musicManager.setVolume();
		}
	}

	private void drawBarControl(NanoVGManager nvg, String icon, float x, float y, float size, boolean primary) {
		if(primary) {
			AccentColor accent = Mix.getInstance().getColorManager().getCurrentColor();
			nvg.drawGradientRoundedRect(x, y, size, size, size / 2F, accent.getColor1(), accent.getColor2());
			nvg.drawCenteredText(icon, x + (size / 2F), y + (size / 2F) - 8, Color.WHITE, 15, Fonts.ICON);
		}else {
			nvg.drawRoundedRect(x, y, size, size, size / 2F, ModMenuTheme.surfaceContainerHigh());
			nvg.drawCenteredText(icon, x + (size / 2F), y + (size / 2F) - 8,
					ModMenuTheme.onSurface(), 13, Fonts.ICON);
		}
	}

	// ══════════════════════════════════════════════════════════════════
	//  拖动进度
	// ══════════════════════════════════════════════════════════════════

	/**
	 * 进度条命中区。
	 *
	 * <p>上下各扩 {@link #SEEK_HIT_PAD}、左右各扩 4：4 高的细条本身几乎不可能被点中，
	 * 而它周围那一带没有别的可点东西，扩进来不会抢走任何既有命中。</p>
	 */
	private boolean insideSeekBar(int mouseX, int mouseY) {
		return MouseUtils.isInside(mouseX, mouseY, progressX() - 4F, progressY() - SEEK_HIT_PAD,
				progressWidth() + 8F, 4F + SEEK_HIT_PAD * 2F);
	}

	/** 把鼠标 x 折算成 0~1 的进度比例。 */
	private float seekFractionAt(int mouseX) {
		float width = progressWidth();
		if(width <= 0F) {
			return 0F;
		}
		return Math.max(0F, Math.min(1F, (mouseX - progressX()) / width));
	}

	/**
	 * 拖动中的每帧更新。
	 *
	 * <p>GuiModMenu 不转发 mouseDragged，所以位置刷新只能挂在绘制帧上——这不是权宜之计，
	 * 而是这个仓库既有惯例：{@code CompSlider} 也是在 {@code draw()} 里读 mouseX 更新值。
	 * 松手用 {@link Mouse#isButtonDown(int)} 判：即使松手发生在窗口外、release 事件没送到，
	 * 这里也能兜住并落一次 seek。</p>
	 */
	private void updateSeekDrag(int mouseX) {
		if(!seeking) {
			return;
		}
		if(!Mouse.isButtonDown(0)) {
			commitSeek();
			return;
		}
		seekFraction = seekFractionAt(mouseX);
	}

	/**
	 * 松手（或每帧检测到键已松开）：把显示位置一次性落到播放器上。
	 *
	 * <p>为什么只在这里 seek、拖动中不 seek：连续 seek 会让网易云那条反射进音频管线的
	 * 路径爆音、也会让本地 MediaPlayer 反复重缓冲；而用户的心理模型本来就是
	 * 「拖到哪、松手从哪开始」。代价只是拖动时听不到声音变化，这一条可以接受。</p>
	 */
	private void commitSeek() {
		if(!seeking) {
			return;
		}
		seeking = false;
		MusicManager musicManager = Mix.getInstance().getMusicManager();
		long total = playerTotalMs(isDangoBar(musicManager), musicManager);
		if(total <= 0L) {
			return;
		}
		long target = (long) (seekFraction * total);
		if(isDangoBar(musicManager)) {
			DangoMusicApi.seekTo(target);
		}else {
			// MusicManager.seek 收秒；越界由它自己钳到 [0, 总时长-0.25s]
			musicManager.seek(target / 1000.0D);
		}
	}

	// ══════════════════════════════════════════════════════════════════
	//  搜索页
	// ══════════════════════════════════════════════════════════════════

	/** 搜索框宽度：给右侧按钮与状态文案留 150 余量，再夹在 132~220 之间。 */
	private float dangoSearchFieldWidth() {
		return Math.max(DANGO_SEARCH_MIN_WIDTH,
				Math.min(DANGO_SEARCH_MAX_WIDTH, getWidth() - MARGIN * 2F - 150F));
	}

	/** 搜索行的纵坐标：贴着播放条往上抬。 */
	private float dangoSearchRowY(float barY) {
		return barY - DANGO_SEARCH_GAP - DANGO_SEARCH_HEIGHT;
	}

	/** 列表真正的底边：搜索页要让出一条搜索行的高度。 */
	private float listBottomFor(float barY) {
		return section == Section.SEARCH ? dangoSearchRowY(barY) - DANGO_SEARCH_GAP : barY;
	}

	/**
	 * 内联搜索行：一枚胶囊输入框 + 搜索按钮 + 一行状态文案。
	 *
	 * <p>这里不用 {@code CompTextBox.draw}：它自带的面是小圆角方框、填色取调色板里的
	 * 纯背景色，塞进这排胶囊化控件里会像贴错图层。所以只借它的输入能力（焦点、光标、
	 * 剪贴板、按键过滤），外观全部自己画，圆角与高度和顶部导航胶囊一致。</p>
	 */
	private void drawDangoSearchRow(NanoVGManager nvg, int mouseX, int mouseY,
			float fieldX, float fieldY, float fieldWidth) {

		dangoSearchBox.setPosition(fieldX + 25F, fieldY, Math.max(24F, fieldWidth - 33F), DANGO_SEARCH_HEIGHT);
		boolean focused = dangoSearchBox.isFocused();
		float radius = DANGO_SEARCH_HEIGHT / 2F;

		nvg.drawRoundedRect(fieldX, fieldY, fieldWidth, DANGO_SEARCH_HEIGHT, radius,
				focused ? ModMenuTheme.surfaceContainerHigh() : ModMenuTheme.surfaceContainerLow());
		nvg.drawOutlineRoundedRect(fieldX, fieldY, fieldWidth, DANGO_SEARCH_HEIGHT, radius, 1F,
				ModMenuTheme.applyAlpha(focused ? ModMenuTheme.primary() : ModMenuTheme.outlineVariant(),
						focused ? 170 : 140));
		nvg.drawTextMid(Icon.SEARCH, fieldX + 9F, fieldY + radius,
				focused ? ModMenuTheme.primary() : ModMenuTheme.onSurfaceVariant(), 11, Fonts.ICON);

		String text = dangoSearchBox.getText();
		if(text == null) {
			text = "";
		}
		float textLeft = fieldX + 25F;
		float textWidth = fieldWidth - 34F;
		if(text.isEmpty() && !focused) {
			nvg.drawText(nvg.getLimitText(TranslateText.DANGO_SEARCH_HINT.getText(), 9.5F, Fonts.REGULAR, textWidth),
					textLeft, fieldY + 8F, ModMenuTheme.applyAlpha(ModMenuTheme.onSurfaceVariant(), 150),
					9.5F, Fonts.REGULAR);
		}else {
			nvg.drawText(nvg.getLimitText(text, 9.5F, Fonts.REGULAR, textWidth), textLeft, fieldY + 8F,
					ModMenuTheme.onSurface(), 9.5F, Fonts.REGULAR);
			// 光标按输入框自己的游标位置算，别一律贴在最右边
			if(focused && (System.currentTimeMillis() % 1000L) < 520L) {
				int cursor = Math.max(0, Math.min(dangoSearchBox.getCursorPosition(), text.length()));
				float caretX = textLeft + nvg.getTextWidth(text.substring(0, cursor), 9.5F, Fonts.REGULAR);
				nvg.drawRect(Math.min(caretX, fieldX + fieldWidth - 8F), fieldY + 7F, 1F,
						DANGO_SEARCH_HEIGHT - 14F, ModMenuTheme.primary());
			}
		}

		// 搜索按钮：回车也能触发，这里给鼠标一个明确的落点
		float buttonX = fieldX + fieldWidth + 8F;
		float buttonSize = DANGO_SEARCH_HEIGHT;
		boolean buttonHovered = MouseUtils.isInside(mouseX, mouseY, buttonX, fieldY, buttonSize, buttonSize);
		if(buttonHovered) {
			AccentColor accent = Mix.getInstance().getColorManager().getCurrentColor();
			nvg.drawGradientRoundedRect(buttonX, fieldY, buttonSize, buttonSize, radius,
					accent.getColor1(), accent.getColor2());
			nvg.drawCenteredText(dangoSearching ? Icon.REFRESH : Icon.SEARCH,
					buttonX + radius, fieldY + radius - 8F, Color.WHITE, 12, Fonts.ICON);
		}else {
			nvg.drawRoundedRect(buttonX, fieldY, buttonSize, buttonSize, radius,
					ModMenuTheme.surfaceContainerLow());
			nvg.drawOutlineRoundedRect(buttonX, fieldY, buttonSize, buttonSize, radius, 1F,
					ModMenuTheme.applyAlpha(ModMenuTheme.outlineVariant(), 140));
			nvg.drawCenteredText(dangoSearching ? Icon.REFRESH : Icon.SEARCH,
					buttonX + radius, fieldY + radius - 8F, ModMenuTheme.onSurfaceVariant(), 12, Fonts.ICON);
		}

		// 状态文案：搜索中 / 需要登录 / 搜不到，都写在同一行，不弹通知
		String status = null;
		if(dangoSearching) {
			status = TranslateText.DANGO_SEARCHING.getText();
		}else if(dangoSearchMessage != null) {
			status = dangoSearchMessage;
		}else if(!DangoMusicApi.isNeteaseLoggedIn()) {
			status = TranslateText.DANGO_SEARCH_LOGIN.getText();
		}
		if(status != null) {
			float statusX = buttonX + buttonSize + 10F;
			float statusWidth = (getX() + getWidth() - MARGIN) - statusX;
			if(statusWidth > 40F) {
				nvg.drawText(nvg.getLimitText(status, 9F, Fonts.REGULAR, statusWidth), statusX, fieldY + 9F,
						ModMenuTheme.onSurfaceVariant(), 9F, Fonts.REGULAR);
			}
		}
	}

	/**
	 * 触发一次搜索。
	 *
	 * <p>结果写入用 volatile 字段，搜索线程只做一次整体赋值，绘制线程读到的要么是旧结果、
	 * 要么是新结果，不会读到半张表。</p>
	 */
	private void triggerDangoSearch() {
		String keyword = dangoSearchBox.getText();
		if(keyword == null) {
			keyword = "";
		}
		keyword = keyword.trim();
		if(keyword.isEmpty()) {
			clearDangoSearch();
			return;
		}
		if(dangoSearching) {
			return;
		}
		dangoSearchBox.setFocused(false);
		dangoSearchMessage = null;
		if(!DangoMusicApi.isAvailable() || !DangoMusicApi.isNeteaseLoggedIn()) {
			// 没登录就不发请求，直接把「先去登录」写在状态行上
			dangoResults = java.util.Collections.emptyList();
			dangoSearchMessage = TranslateText.DANGO_SEARCH_LOGIN.getText();
			return;
		}
		final String query = keyword;
		dangoSearching = true;
		dangoResults = java.util.Collections.emptyList();
		Multithreading.runAsync(() -> {
			java.util.List<Object> found = DangoMusicApi.search(query, DANGO_SEARCH_LIMIT);
			dangoResults = found;
			if(found.isEmpty()) {
				String error = DangoMusicApi.lastSearchError();
				dangoSearchMessage = (error != null && !error.isEmpty())
						? TranslateText.DANGO_SEARCH_FAILED.getText()
						: TranslateText.DANGO_SEARCH_EMPTY.getText();
			}else {
				scroll.setScrollPosition(0F);
			}
			dangoSearching = false;
		});
	}

	/** 清空搜索：结果、状态文案、输入框、焦点全部归位。 */
	private void clearDangoSearch() {
		dangoResults = java.util.Collections.emptyList();
		dangoSearching = false;
		dangoSearchMessage = null;
		dangoSearchBox.setFocused(false);
		dangoSearchBox.setText("");
		scroll.setScrollPosition(0F);
	}

	// ══════════════════════════════════════════════════════════════════
	//  工具
	// ══════════════════════════════════════════════════════════════════

	/** 毫秒转 m:ss，播放条与队列行共用。 */
	private String formatDuration(long ms) {
		long total = Math.max(0L, ms) / 1000L;
		return (total / 60L) + ":" + ((total % 60L) < 10L ? "0" : "") + (total % 60L);
	}

	private boolean filter(Music m) {
		if(!currentType.equals(MusicType.ALL) && !m.getType().equals(currentType)) {
			return true;
		}
		if(!this.getSearchBox().getText().isEmpty()
				&& !SearchUtils.isSimillar(m.getName(), this.getSearchBox().getText())) {
			return true;
		}
		return false;
	}

	// ══════════════════════════════════════════════════════════════════
	//  输入
	// ══════════════════════════════════════════════════════════════════

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		if(mouseButton != 0) {
			return;
		}

		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		MusicManager musicManager = instance.getMusicManager();

		float chipY = getY() + 10F;
		float listTop = CategoryIslandRenderer.contentTop(chipY, CHIP_HEIGHT);
		float listBottom = listBottomFor(barY());
		float listHeight = listBottom - listTop - 8F;
		float barX = barX();
		float barY = barY();
		float barWidth = barWidth();

		// 1) 进度条：优先级最高。它躺在播放条里、与其它控件不重叠，先判它不会被抢；
		//    按下就 seek 一次并进入拖动态，后续拖动只改显示，松手再落一次。
		if(insideSeekBar(mouseX, mouseY)) {
			handleSeekClick(mouseX, musicManager);
			return;
		}

		// 2) 搜索行：CompTextBox 失焦也有副作用，命中就吃掉这次点击
		if(section == Section.SEARCH) {
			float fieldX = barX;
			float fieldY = dangoSearchRowY(barY);
			float fieldWidth = dangoSearchFieldWidth();
			float buttonX = fieldX + fieldWidth + 8F;
			boolean inField = MouseUtils.isInside(mouseX, mouseY, fieldX, fieldY,
					fieldWidth + 8F + DANGO_SEARCH_HEIGHT, DANGO_SEARCH_HEIGHT);
			if(inField) {
				dangoSearchBox.mouseClicked(mouseX, mouseY, mouseButton);
				if(MouseUtils.isInside(mouseX, mouseY, buttonX, fieldY,
						DANGO_SEARCH_HEIGHT, DANGO_SEARCH_HEIGHT)) {
					triggerDangoSearch();
				}
				return;
			}
			dangoSearchBox.setFocused(false);
		}

		// 3) 下载弹层：它盖在播放条上方，开着的时候先处理它
		if(openDownloader) {
			textBox.mouseClicked(mouseX, mouseY, mouseButton);
			float downloaderX = barX + barWidth - 260F;
			float downloaderY = barY - 46F;
			if(MouseUtils.isInside(mouseX, mouseY, downloaderX + 214, downloaderY + 7, 24, 24)) {
				openDownloader = false;
				textBox.setFocused(false);
				Multithreading.runAsync(() -> {
					instance.getNotificationManager().post(TranslateText.MUSIC,
							TranslateText.ADDED_MUSIC_QUEUE, NotificationType.INFO);
					if(musicManager.getYtdlp().download(textBox.getText())) {
						instance.getNotificationManager().post(TranslateText.MUSIC,
								TranslateText.MUSIC_DOWNLOAD_COMPLETE, NotificationType.SUCCESS);
						musicManager.load();
					}else {
						instance.getNotificationManager().post(TranslateText.MUSIC,
								TranslateText.MUSIC_DOWNLOAD_FAILED, NotificationType.ERROR);
					}
				});
				return;
			}
			if(!MouseUtils.isInside(mouseX, mouseY, downloaderX, downloaderY, 250, 38)) {
				openDownloader = false;
				textBox.setFocused(false);
				return;
			}
		}

		// 4) 顶部导航
		float chipX = getX() + MARGIN;
		for(Section s : Section.values()) {
			float width = chipWidth(nvg, s);
			if(MouseUtils.isInside(mouseX, mouseY, chipX, chipY, width, CHIP_HEIGHT)) {
				setSection(s);
				return;
			}
			chipX += width + 8F;
		}

		// 5) 歌单页的「返回」：比导航更靠右，先判它才不会被导航吃掉
		if(section == Section.PLAYLIST && dangoOpenPlaylist != null) {
			float backWidth = nvg.getTextWidth(TranslateText.DANGO_PLAYLIST_BACK.getText(),
					10.5F, Fonts.MEDIUM) + 26F;
			float backX = getX() + getWidth() - MARGIN - backWidth;
			if(MouseUtils.isInside(mouseX, mouseY, backX, chipY, backWidth, CHIP_HEIGHT)) {
				closeDangoPlaylist();
				return;
			}
		}

		// 6) 列表行
		if(MouseUtils.isInside(mouseX, mouseY, getX() + MARGIN, listTop,
				getWidth() - MARGIN * 2F, listHeight) && handleRowClick(mouseX, mouseY, listTop, musicManager)) {
			return;
		}

		// 7) 播放条控件
		float centerX = barX + (barWidth / 2F);
		if(MouseUtils.isInside(mouseX, mouseY, centerX - 17, barY + 15, 34, 34)) {
			if(isDangoBar(musicManager)) {
				lastStartedDango = true;
				DangoMusicApi.togglePlayPause();
			}else {
				lastStartedDango = false;
				musicManager.switchPlayBack();
			}
			return;
		}
		if(MouseUtils.isInside(mouseX, mouseY, centerX - 58, barY + 19, 26, 26)) {
			if(isDangoBar(musicManager)) {
				DangoMusicApi.playPrev();
			}else {
				musicManager.back();
			}
			return;
		}
		if(MouseUtils.isInside(mouseX, mouseY, centerX + 32, barY + 19, 26, 26)) {
			if(isDangoBar(musicManager)) {
				DangoMusicApi.playNext();
			}else {
				musicManager.next();
			}
			return;
		}

		// 8) 添加歌曲
		float addWidth = 86F;
		float addX = barX + barWidth - addWidth - 10F;
		if(MouseUtils.isInside(mouseX, mouseY, addX, barY + 18, addWidth, 28)) {
			if(openDownloader) {
				openDownloader = false;
				textBox.setFocused(false);
			}else {
				textBox.setText("");
				textBox.setFocused(true);
				openDownloader = true;
			}
			return;
		}

		// 9) 音量
		volumeSlider.mouseClicked(mouseX, mouseY, mouseButton);
	}

	/**
	 * 点了进度条。
	 *
	 * <p>按下立刻定位一次（用户看到的是「跳到这儿」），同时进入拖动状态；后面继续拖只改
	 * 显示位置，松手时 {@link #commitSeek()} 再落一次。两个音源都能拖：
	 * 网易云走 {@code DangoMusicApi.seekTo(ms)}，本地走 {@code MusicManager.seek(seconds)}。
	 * 真正没有播放头（列表空 / 时长还没解析出来）时才不作响应。</p>
	 */
	private void handleSeekClick(int mouseX, MusicManager musicManager) {
		boolean dangoBar = isDangoBar(musicManager);
		long total = playerTotalMs(dangoBar, musicManager);
		if(total <= 0L) {
			return;
		}
		seekFraction = seekFractionAt(mouseX);
		seeking = true;
		lastStartedDango = dangoBar;
		musicManager.setVolume();
		long target = (long) (seekFraction * total);
		if(dangoBar) {
			DangoMusicApi.seekTo(target);
		}else {
			musicManager.seek(target / 1000.0D);
		}
	}

	/** 列表行点击：按页面分派。返回 true 表示这次点击被吃掉了。 */
	private boolean handleRowClick(int mouseX, int mouseY, float listTop, MusicManager musicManager) {
		switch(section) {
			case LOCAL:
				return handleLocalRowClick(mouseX, mouseY, listTop, musicManager);
			case HOME:
				return handleHomeRowClick(mouseX, mouseY, listTop);
			case PLAYLIST:
				if(dangoOpenPlaylist != null) {
					return handleTrackRowClick(mouseX, mouseY, listTop, dangoPlaylistTracks);
				}
				return handlePlaylistRowClick(mouseX, mouseY, listTop, dangoAllPlaylists);
			case SEARCH:
				return handleTrackRowClick(mouseX, mouseY, listTop, dangoResults);
			case QUEUE:
				return handleQueueRowClick(mouseX, mouseY, listTop);
			case FAVORITES:
				return handleTrackRowClick(mouseX, mouseY, listTop, dangoLikedTracks);
			default:
				return false;
		}
	}

	/** 本地页第一行的次级筛选：只在它那一带命中，其余位置继续交给行处理。 */
	private boolean handleLocalFilterClick(int mouseX, int mouseY, float listTop) {
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		float x = getX() + MARGIN;
		float y = listTop + scroll.getValue();
		for(MusicType type : MusicType.values()) {
			float width = nvg.getTextWidth(type.getName(), 9.5F, Fonts.MEDIUM) + 22F;
			if(MouseUtils.isInside(mouseX, mouseY, x, y, width, LOCAL_FILTER_HEIGHT)) {
				currentType = type;
				scroll.setScrollPosition(0F);
				return true;
			}
			x += width + 6F;
		}
		return false;
	}

	private boolean handleLocalRowClick(int mouseX, int mouseY, float listTop, MusicManager musicManager) {
		if(handleLocalFilterClick(mouseX, mouseY, listTop)) {
			return true;
		}
		float rowsTop = listTop + LOCAL_FILTER_OFFSET;
		int index = 0;
		for(Music m : musicManager.getMusics()) {
			if(filter(m)) {
				continue;
			}
			float rowY = rowsTop + index * (ROW_HEIGHT + ROW_GAP) + scroll.getValue();
			float rowX = getX() + MARGIN;
			float rowWidth = getWidth() - MARGIN * 2F;
			boolean inside = MouseUtils.isInside(mouseX, mouseY, rowX, rowY, rowWidth, ROW_HEIGHT);
			boolean favorite = MouseUtils.isInside(mouseX, mouseY, rowX + rowWidth - 61, rowY + 16, 20, 20);
			boolean trash = MouseUtils.isInside(mouseX, mouseY, rowX + rowWidth - 37, rowY + 16, 20, 20);
			if(inside && !favorite && !trash) {
				musicManager.stop();
				musicManager.setCurrentMusic(m);
				musicManager.play();
				lastStartedDango = false;
				return true;
			}
			if(favorite) {
				m.setType(m.getType().equals(MusicType.ALL) ? MusicType.FAVORITE : MusicType.ALL);
				musicManager.saveData();
				return true;
			}
			if(trash) {
				musicManager.delete(m);
				return true;
			}
			index++;
		}
		return false;
	}

	/** 首页：分组标题行不可点，歌单行点开就进 PLAYLIST 页并加载曲目。 */
	private boolean handleHomeRowClick(int mouseX, int mouseY, float listTop) {
		Object target = findHomePlaylistAt(mouseX, mouseY, listTop);
		if(target == null) {
			return false;
		}
		openDangoPlaylist(target);
		setSection(Section.PLAYLIST);
		return true;
	}

	/** 首页命中：走同一个 walkHomeRows，几何与绘制严格一致。 */
	private Object findHomePlaylistAt(final int mouseX, final int mouseY, float listTop) {
		final Object[] hit = new Object[1];
		walkHomeRows(listTop, new RowSink() {
			@Override
			public void row(Object playlist, String header, float y) {
				if(header != null || playlist == null) {
					return;
				}
				float rowX = getX() + MARGIN;
				float rowWidth = getWidth() - MARGIN * 2F;
				if(MouseUtils.isInside(mouseX, mouseY, rowX, y + scroll.getValue(), rowWidth, ROW_HEIGHT)) {
					hit[0] = playlist;
				}
			}
		});
		return hit[0];
	}

	private boolean handlePlaylistRowClick(int mouseX, int mouseY, float listTop,
			java.util.List<Object> playlists) {
		for(int i = 0; i < playlists.size(); i++) {
			float rowY = listTop + i * (ROW_HEIGHT + ROW_GAP) + scroll.getValue();
			float rowX = getX() + MARGIN;
			float rowWidth = getWidth() - MARGIN * 2F;
			if(MouseUtils.isInside(mouseX, mouseY, rowX, rowY, rowWidth, ROW_HEIGHT)) {
				openDangoPlaylist(playlists.get(i));
				return true;
			}
		}
		return false;
	}

	/** 搜索页 / 歌单内的曲目：点哪一行，就把整张表当成新队列交给 Dango。 */
	private boolean handleTrackRowClick(int mouseX, int mouseY, float listTop,
			java.util.List<Object> tracks) {
		for(int i = 0; i < tracks.size(); i++) {
			float rowY = listTop + i * (ROW_HEIGHT + ROW_GAP) + scroll.getValue();
			float rowX = getX() + MARGIN;
			float rowWidth = getWidth() - MARGIN * 2F;
			if(MouseUtils.isInside(mouseX, mouseY, rowX, rowY, rowWidth, ROW_HEIGHT)) {
				// playQueue 而不是 playAt：这两张表不是播放器现有的队列，下标对不上
				DangoMusicApi.playQueue(tracks, i);
				lastStartedDango = true;
				return true;
			}
		}
		return false;
	}

	/** 队列页：这张表就是播放器自己的队列，用 playAt 按真实下标跳。 */
	private boolean handleQueueRowClick(int mouseX, int mouseY, float listTop) {
		java.util.List<Object> queue = DangoMusicApi.queue();
		for(int i = 0; i < queue.size(); i++) {
			float rowY = listTop + i * (ROW_HEIGHT + ROW_GAP) + scroll.getValue();
			float rowX = getX() + MARGIN;
			float rowWidth = getWidth() - MARGIN * 2F;
			if(MouseUtils.isInside(mouseX, mouseY, rowX, rowY, rowWidth, ROW_HEIGHT)) {
				DangoMusicApi.playAt(i);
				lastStartedDango = true;
				return true;
			}
		}
		return false;
	}

	// ── 歌单数据装载 ──────────────────────────────────────────────────

	/**
	 * 拉歌单（推荐 / 榜单 / 合并表），一次请求拉齐三份。
	 *
	 * <p>两个闸门：{@code dangoPlaylistsLoading} 防并发，{@code dangoPlaylistsTried}
	 * 防「没登录 / 拉回来是空表」时每帧都打一次网络。想重试就切页
	 * （{@link #setSection} 会在两张表都空时把闸门放开）。</p>
	 */
	private void loadDangoPlaylists() {
		if(dangoPlaylistsLoading || dangoPlaylistsTried) {
			return;
		}
		if(!DangoMusicApi.isAvailable()) {
			return;
		}
		dangoPlaylistsLoading = true;
		dangoPlaylistsTried = true;
		Multithreading.runAsync(() -> {
			java.util.List<Object> all = DangoMusicApi.playlists();
			java.util.List<Object> recommend = DangoMusicApi.recommendedPlaylists();
			java.util.List<Object> charts = DangoMusicApi.chartPlaylists();
			if(recommend.isEmpty() && charts.isEmpty()) {
				// 分组接口取不到时退化成一张合并表：首页至少不是一片空白
				recommend = all;
			}
			dangoRecommendPlaylists = recommend;
			dangoChartPlaylists = charts;
			dangoAllPlaylists = all;
			dangoPlaylistsLoading = false;
		});
	}

	/**
	 * 拉「我喜欢的音乐」。
	 *
	 * <p>它与 {@link #loadDangoPlaylists} 一样是双闸门：{@code loading} 防并发、
	 * {@code tried} 防「没登录 / 里面确实没歌」时每帧都打一次网络。数据来自
	 * {@link DangoMusicApi#likedTracks()}，那里面已经处理了「按名字找我喜欢的音乐」这件事。</p>
	 */
	private void loadDangoLiked() {
		if(dangoLikedLoading || dangoLikedTried) {
			return;
		}
		if(!DangoMusicApi.isAvailable() || !DangoMusicApi.isNeteaseLoggedIn()) {
			return;
		}
		dangoLikedLoading = true;
		dangoLikedTried = true;
		Multithreading.runAsync(() -> {
			java.util.List<Object> tracks = DangoMusicApi.likedTracks();
			dangoLikedTracks = tracks;
			dangoLikedLoading = false;
		});
	}

	/** 打开一张歌单：曲目在后台拉，拉回来再填表。 */
	private void openDangoPlaylist(Object playlist) {
		if(playlist == null) {
			return;
		}
		dangoOpenPlaylist = playlist;
		dangoPlaylistTracks = java.util.Collections.emptyList();
		dangoPlaylistTracksLoading = true;
		scroll.setScrollPosition(0F);
		Multithreading.runAsync(() -> {
			java.util.List<Object> tracks = DangoMusicApi.tracksOfPlaylist(playlist);
			if(dangoOpenPlaylist != playlist) {
				// 期间用户已经退出这张歌单，这份结果丢掉，不要往正在画的表里塞
				return;
			}
			dangoPlaylistTracks = tracks;
			dangoPlaylistTracksLoading = false;
			scroll.setScrollPosition(0F);
		});
	}

	/** 关掉歌单，回到歌单列表那一层。 */
	private void closeDangoPlaylist() {
		dangoOpenPlaylist = null;
		dangoPlaylistTracks = java.util.Collections.emptyList();
		dangoPlaylistTracksLoading = false;
		scroll.setScrollPosition(0F);
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		volumeSlider.mouseReleased(mouseX, mouseY, mouseButton);
		// 松手必须落一次 seek：拖动期间只改了显示位置
		if(mouseButton == 0) {
			commitSeek();
		}
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if(openDownloader) {
			textBox.keyTyped(typedChar, keyCode);
			return;
		}
		if(section == Section.SEARCH && dangoSearchBox.isFocused()) {
			if(keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
				triggerDangoSearch();
				return;
			}
			if(keyCode == Keyboard.KEY_ESCAPE) {
				// ESC 先退搜索（失焦 + 清结果），再按一次才关菜单
				clearDangoSearch();
				return;
			}
			dangoSearchBox.keyTyped(typedChar, keyCode);
			return;
		}
		if(section == Section.PLAYLIST && dangoOpenPlaylist != null && keyCode == Keyboard.KEY_ESCAPE) {
			// 歌单里按 ESC 退回上一层，不要直接把整个菜单关掉
			closeDangoPlaylist();
		}
	}

	@Override
	public boolean isEscapeConsumed() {
		// 搜索框聚焦、或者正看着某个歌单时，ESC 属于这个页面：先退搜索 / 退歌单，按到底才轮到关菜单
		return (section == Section.SEARCH && dangoSearchBox.isFocused())
				|| (section == Section.PLAYLIST && dangoOpenPlaylist != null);
	}
}