package mix.client.mixui.integration.dango;

import java.util.HashMap;

import mix.client.mixui.Mix;
import mix.client.mixui.management.language.LanguageManager;

/**
 * Dango 界面文案的中文映射。
 *
 * <p>为什么需要这个：DangoMusic 的设置项名称与说明并不写在字节码 ldc 里，而是挂在
 * {@code @me.sorihiko.dango.module.values.ModuleValue(name=, desc=)} 这个
 * RuntimeVisibleAnnotations 上；界面上的其余散落文案（"No results"、"Play all"、
 * "ROOM CODE"…）才是普通字符串常量。两边都无法用一份资源文件统一替换，
 * 所以统一在绘制收口处按「英文原文 → 中文」做精确查表。</p>
 *
 * <p>接入点：{@code me.sorihiko.dango.font.MixedText} 的 text / width / trim / wrap2
 * 四个方法入口被 ASM 注入了一层 {@code DangoI18n.t(s)}。Dango 自己的 UI 门面
 * {@code me.sorihiko.dango.musicui.Ui.text} 内部只有一条
 * {@code invokevirtual MixedText.text}，所以那一处就能覆盖设置页、侧边栏、播放条、
 * 搜索页、歌单页、队列页、一起听、登录框的全部文字。</p>
 *
 * <p>安全边界：只做精确匹配（外加少量明确的拼接前缀），歌名、歌手、歌词、搜索关键词
 * 永远不会命中，不会被误翻。世界里的 3D 歌词走 LegacyText/LegacyGfx 通路，
 * 不经过 MixedText，完全不受影响。</p>
 *
 * <p>语言门控：只有当前语言是简体中文时才翻译。切到英文/日文时原样返回 Dango 的
 * 英文原文，不会出现中英混排。</p>
 */
public final class DangoI18n {

	private static final HashMap<String, String> MAP = new HashMap<String, String>();

	private static void put(HashMap<String, String> map, String from, String to) {
		map.put(from, to);
	}

	static {
		put(MAP, "3D lyrics", "3D 歌词");
		put(MAP, "Now playing", "正在播放");
		put(MAP, "Cover", "封面");
		put(MAP, "Scale", "缩放");
		put(MAP, "Distance", "距离");
		put(MAP, "Glow", "辉光");
		put(MAP, "Glow color", "辉光颜色");
		put(MAP, "Glow strength", "辉光强度");
		put(MAP, "Glow radius", "辉光半径");
		put(MAP, "Smooth font", "平滑字体");
		put(MAP, "Wiggle", "抖动");
		put(MAP, "Wiggle fps", "抖动帧率");
		put(MAP, "Wiggle amount", "抖动幅度");
		put(MAP, "Text flash", "文字闪现");
		put(MAP, "Word pop", "逐字弹出");
		put(MAP, "Flash size", "闪现大小");
		put(MAP, "Face player", "面向玩家");
		put(MAP, "Keep away", "自动避让");
		put(MAP, "Stay on screen", "保持在屏幕内");
		put(MAP, "Above ground", "不进入地面");
		put(MAP, "Spread", "水平扩散");
		put(MAP, "Split lines", "拆分双行");
		put(MAP, "Show lyrics floating in the world", "在世界中悬浮显示歌词");
		put(MAP, "Show the current song in the top left", "在左上角显示当前歌曲");
		put(MAP, "Show the album cover in the top left", "在左上角显示专辑封面");
		put(MAP, "Show what the built-in player is playing", "显示内置播放器正在播放的内容");
		put(MAP, "Lyrics size", "歌词大小");
		put(MAP, "How far away 3D lyrics appear (blocks)", "3D 歌词出现的距离（方块）");
		put(MAP, "Deep Glow style: white-hot core, wide soft halo", "深辉光样式：白热核心，宽而柔的光晕");
		put(MAP, "Glow tint: text color, pink, blue or gold", "辉光色调：跟随文字 / 粉 / 蓝 / 金");
		put(MAP, "Glow brightness", "辉光亮度");
		put(MAP, "Halo spread: higher is wider and softer", "光晕扩散：越高越宽越柔");
		put(MAP, "Smooth font instead of the Minecraft pixel font", "用平滑字体替代 Minecraft 像素字体");
		put(MAP, "Low-fps jitter on every letter, like hand-drawn animation", "每个字母低帧抖动，像手绘动画");
		put(MAP, "Jitter frame rate: lower feels more hand-drawn", "抖动帧率：越低越像手绘");
		put(MAP, "Jitter strength", "抖动强度");
		put(MAP, "Words slam onto the screen one by one, synced to the beat", "文字按节拍逐个砸到屏幕上");
		put(MAP, "Words pop in one by one as they are sung", "文字随演唱逐字弹出");
		put(MAP, "Text flash size", "文字闪现的大小");
		put(MAP, "Lyrics keep turning to face you", "歌词始终转向你");
		put(MAP, "Lyrics back off when you walk or sprint into them", "走进或跑向歌词时它会自动后退");
		put(MAP, "Lyrics glide back into view when you turn away, so they never leave the screen", "转开视线时歌词滑回视野，不会离开屏幕");
		put(MAP, "Lyrics never appear below your eyes, so they don't sink into the ground", "歌词不会出现在视线以下，不会沉进地面");
		put(MAP, "How far left or right each line can appear (degrees)", "每行歌词可出现的左右范围（度）");
		put(MAP, "Split two lines written as one (NetEase often does this) so they appear separately", "把写在一行的两句（网易云常见）拆开分别显示");
		put(MAP, "Keep the card visible while paused", "暂停时仍保持卡片可见");
		put(MAP, "Island size", "灵动岛大小");
		put(MAP, "Liquid glass", "液态玻璃");
		put(MAP, "Liquid glass background; off is a solid black pill", "液态玻璃背景；关闭后为纯黑胶囊");
		put(MAP, "How long song notifications stay (seconds)", "切歌提醒停留的时间（秒）");
		put(MAP, "Horizontal position (or drag it with chat open)", "横向位置（也可在打开聊天栏时拖动）");
		put(MAP, "Vertical position (or drag it with chat open)", "纵向位置（也可在打开聊天栏时拖动）");
		put(MAP, "Expand to show the song on track change or pause", "切歌或暂停时展开显示歌曲");
		put(MAP, "Playback volume", "播放音量");
		put(MAP, "Play songs in random order", "随机顺序播放歌曲");
		put(MAP, "Hotkey to play or pause", "播放或暂停的快捷键");
		put(MAP, "Hotkey for the next song", "切换到下一首的快捷键");
		put(MAP, "Hotkey for the previous song", "切换到上一首的快捷键");
		put(MAP, "Hotkey to raise the volume by 5%", "音量提高 5% 的快捷键");
		put(MAP, "Hotkey to lower the volume by 5%", "音量降低 5% 的快捷键");
		put(MAP, "Hotkey to turn lyrics on or off", "开关歌词的快捷键");
		put(MAP, "Key that opens the player", "打开播放器的快捷键");
		put(MAP, "Show when paused", "暂停时显示");
		put(MAP, "Song notify", "切歌提醒");
		put(MAP, "Notify time", "提醒时长");
		put(MAP, "Pos X", "水平位置");
		put(MAP, "Pos Y", "垂直位置");
		put(MAP, "Volume", "音量");
		put(MAP, "Volume up", "音量加");
		put(MAP, "Volume down", "音量减");
		put(MAP, "Play / Pause", "播放 / 暂停");
		put(MAP, "Next", "下一首");
		put(MAP, "Previous", "上一首");
		put(MAP, "Open key", "打开按键");
		put(MAP, "Toggle lyrics", "歌词开关");
		put(MAP, "Shuffle", "随机播放");
		put(MAP, "Card size", "卡片大小");
		put(MAP, "Lyrics", "歌词");
		put(MAP, "Music", "音乐");
		put(MAP, "DynamicIsland", "动态岛");
		put(MAP, "SongCard", "歌曲卡片");
		put(MAP, "NeteaseMusic", "网易云音乐");
		put(MAP, "Music & HUD", "音乐与 HUD");
		put(MAP, "Song card", "歌曲卡片");
		put(MAP, "Other", "其它");
		put(MAP, "More", "更多");
		put(MAP, "Press a key", "按下按键");
		put(MAP, "None", "无");
		put(MAP, "NOW PLAYING", "正在播放");
		put(MAP, "PAUSED", "已暂停");
		put(MAP, "LOADING", "加载中…");
		put(MAP, "Now Playing", "正在播放");
		put(MAP, "Searching...", "搜索中…");
		put(MAP, "Search failed:", "搜索失败：");
		put(MAP, "No results", "没有结果");
		put(MAP, "Nothing in the queue yet", "队列还是空的");
		put(MAP, "Loading...", "加载中…");
		put(MAP, "Play all", "播放全部");
		put(MAP, "List", "列表");
		put(MAP, "Covers", "封面墙");
		put(MAP, "Locate", "定位");
		put(MAP, "TITLE", "标题");
		put(MAP, "TIME", "时长");
		put(MAP, "Filter this list...", "筛选此列表...");
		put(MAP, "Not playing", "未在播放");
		put(MAP, "Log in to NetEase", "登录网易云音乐");
		put(MAP, "Refresh QR", "刷新二维码");
		put(MAP, "Your playlists", "你的歌单");
		put(MAP, "Recommended playlists", "推荐歌单");
		put(MAP, "Welcome to Dango Music!", "欢迎使用 Dango 音乐");
		put(MAP, "Charts", "榜单");
		put(MAP, "Listen Together", "一起听");
		put(MAP, "Play the same song in sync with friends who also have this mod", "和同样装了此模组的好友同步听同一首歌");
		put(MAP, "You are the host · everyone in the room follows your music", "你是房主 · 房间内所有人跟随你的音乐");
		put(MAP, "Working...", "处理中…");
		put(MAP, "Create a room", "创建房间");
		put(MAP, "OR JOIN WITH A ROOM CODE", "或使用房间码加入");
		put(MAP, "ROOM CODE", "房间码");
		put(MAP, "Join", "加入");
		put(MAP, "Copy", "复制");
		put(MAP, "Copied", "已复制");
		put(MAP, "HOST", "房主");
		put(MAP, "Resync", "重新同步");
		put(MAP, "Close room", "关闭房间");
		put(MAP, "Leave room", "离开房间");
		put(MAP, "Everyone streams from their own NetEase account, so VIP songs need VIP on each side.", "每人都用自己账号的网易云播放，VIP 歌曲需要双方都有 VIP。");
		put(MAP, "Search songs...", "搜索歌曲…");
	}

	// 拼接型文案的前缀常量。Dango 用 "No songs match \"" + 关键词 + "\"" 这种拼法，
	// 整串不会命中精确表，只能按前缀识别后自己拼。
	private static final String NO_SONGS = "No songs match \"";
	private static final String SEARCH_Q = "Search  \"";
	private static final String SEARCH_FAILED = "Search failed: ";
	private static final String FOLLOWING = "Following ";
	private static final String IN_ROOM = "IN THIS ROOM · ";

	private DangoI18n() {
	}

	/**
	 * 翻译一句 Dango 界面文案。
	 *
	 * @param s 原文，null 或空串原样返回
	 * @return 中文（当前语言为中文且命中白名单时）或原文
	 */
	public static String t(String s) {

		if(s == null || s.isEmpty()) {
			return s;
		}

		if(!isChinese()) {
			return s;
		}

		String exact = MAP.get(s);

		if(exact != null) {
			return exact;
		}

		return dynamic(s);
	}

	/** 当前语言是否为简体中文。取不到语言设置时按「不是中文」处理，界面保持英文原文。 */
	public static boolean isChinese() {

		try {
			LanguageManager manager = Mix.getInstance().getLanguageManager();

			if(manager == null || manager.getCurrentLanguage() == null) {
				return false;
			}

			return "cn".equals(manager.getCurrentLanguage().getId());
		} catch(Throwable t) {
			return false;
		}
	}

	/**
	 * 拼接型文案。只在精确表未命中时调用，且每条都以 startsWith / endsWith 快速排除，
	 * 所以每帧几百次调用的开销可以忽略。
	 */
	private static String dynamic(String s) {

		if(s.startsWith(NO_SONGS)) {
			return "没有匹配「" + stripTailQuote(s.substring(NO_SONGS.length())) + "」";
		}

		if(s.startsWith(SEARCH_Q)) {
			return "搜索「" + stripTailQuote(s.substring(SEARCH_Q.length())) + "」";
		}

		if(s.startsWith(SEARCH_FAILED)) {
			return "搜索失败：" + s.substring(SEARCH_FAILED.length());
		}

		if(s.startsWith(FOLLOWING)) {
			return "正在跟随 " + s.substring(FOLLOWING.length());
		}

		if(s.startsWith(IN_ROOM)) {
			return "房间内 · " + s.substring(IN_ROOM.length());
		}

		return countSuffix(s);
	}

	/** TracksPage 的 "12 of 40 songs" / "40 songs" / "3 charts"。 */
	private static String countSuffix(String s) {

		if(s.endsWith(" songs")) {

			String body = s.substring(0, s.length() - 6);
			int of = body.indexOf(" of ");

			if(of > 0) {
				return body.substring(0, of) + " / " + body.substring(of + 4) + " 首";
			}

			return body + " 首";
		}

		if(s.endsWith(" charts")) {
			return s.substring(0, s.length() - 7) + " 个榜单";
		}

		return s;
	}

	private static String stripTailQuote(String q) {

		if(q.length() > 0 && q.charAt(q.length() - 1) == '"') {
			return q.substring(0, q.length() - 1);
		}

		return q;
	}
}
