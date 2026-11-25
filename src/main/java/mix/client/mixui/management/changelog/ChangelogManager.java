package mix.client.mixui.management.changelog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import mix.client.mixui.Mix;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.utils.JsonUtils;
import mix.client.mixui.utils.Multithreading;
import mix.client.mixui.utils.network.HttpUtils;

/**
 * Changelog shown on the home screen.
 *
 * The original upstream endpoint is gone, so a local editable file wins:
 * mixclient/changelog.txt
 *
 * One entry per line:
 *   added | Token login works from the main menu
 *   fixed | Menu scale is saved
 * Plain lines default to "added", lines starting with # are comments.
 */
public class ChangelogManager {

	private static final String FILE_NAME = "changelog.txt";

	/** Bumped whenever TEMPLATE changes, so untouched installs get the new list. */
	private static final String TEMPLATE_VERSION = "2026-11-06";

	// 排版规则：先注释头，再「新增」一段，再「修复」一段，段落之间空一行。
	// 界面本身也会按 新增 -> 修复 -> 移除 -> 重要 重新归组，这里保持同样顺序只是为了文件好读。
	private static final String[] TEMPLATE = new String[] {
			"# MixClient 更新内容（本地自定义文件）",
			"#",
			"# 每行一条：类型 | 文本",
			"# 类型可以写 added / fixed / removed / error，也可以写 新增 / 修复 / 移除 / 错误",
			"# 不带类型的行默认按 added 显示；以 # 开头的行是注释",
			"# 改完保存，再打开一次模组菜单就能看到，不会影响客户端的其它设置",
			"# 版本: " + TEMPLATE_VERSION,
			"",
			"# ---- 新增 ----",
			"",
			"added | 模组开关的配色改成真正的淡入淡出：图标底色在「灰 → 紫」之间按开关进度插值，描边和图标前景色也一起走，时长从约 90ms 拉到约 330ms（原来只有滑块在动，底色是一帧跳过去的）",
			"added | 图标居中改成编译期查表：两套图标字体共 3478 个字形的墨迹中心从 TTF 的 glyf 表直接读出、打进字节码，运行时一次二分 + 一次乘加定位，不再依赖 nvgTextBounds 的像素近似",
			"added | 目标指示器改成会被方块遮掩（不再穿墙）：绘制时打开 GL_DEPTH_TEST、只读不写深度，被挡住的像素由 GPU 直接丢弃；再按 nametags 同一套参数（120ms 打一次射线、被挡压到 0.38 alpha、4.5 的渐变速度）整体压暗一层。几何与配色保持上一条不变",
			"added | dango 界面全面中文化：设置页 / 播放条 / 派对页 / 登录框 / 列表标题一次翻完，歌词与歌名不会误翻",
			"added | 模组卡片开关动画：切换时外圈扩散两道光环、卡片内染色淡去、图标底色弹一下；关掉功能也有反馈，以前关掉时只剩滑块回位",
			"added | 模组菜单图标换成 v8.0 那套 Material Symbols：132 个功能各有专属图形，v8.0 没有的按功能含义自己选",
			"added | 目标指示器淡入淡出：单程 380ms，进度按真实时间线性推进再过 smoothstep（两端斜率为 0）；不用指数靠拢是因为它头 20% 时间就走完 63%，剩下的长尾肉眼看不出；丢目标后仍然留在目标最后的位置上自然淡掉",
			"added | 音乐时机：主菜单出现时只把正在播的那一路按下暂停，等真正进世界/进服务器（世界下载界面出现）才恢复；以前点了单人/多人、还没挑存档就恢复播放了",
			"added | Dango 歌曲卡片与顶部胶囊：曲目出现/消失时淡入淡出，换歌时旧内容淡出、新内容淡入并带轻微位移",
			"added | 音乐页新增「收藏」页：直接列出网易云「我喜欢的音乐」，点行就播，不用再进 Dango 那套整屏界面",
			"added | 音乐页的播放条现在可以直接拖进度：按下 seek 一次、拖动只改显示位置、松手再落一次（网易云连续 seek 会爆音，本地播放器会反复重缓冲）",
			"added | 音乐页的音量滑条同时作用于网易云那一路：以前它只喂本地播放器，想调网易云音量必须进 Dango 界面",
			"added | 音乐页整合网易云：多一个「网易云」标签，进去后列表下方带搜索框，回车搜歌、点行直接播",
			"added | 音乐页网易云歌单：队列空的时候列表改成歌单（推荐 / 榜单 / 我创建的），点开就看曲目、点曲目直接播",
			"added | 音乐页顶部那座悬浮胶囊小岛：宽度随分类标签自适应，带柔和投影和一圈描边入场动画",
			"added | 名字标签「缩放」区间放宽到 1.0-6.0 倍：纹理密度按 sqrt(scale) 提升，调大后不会越调越糊",
			"added | 主菜单右上角三颗按钮、模组菜单右上角搜索栏↔版本徽章：切换时淡入淡出并带轻微位移，不再硬切",
			"added | 目标信息：贴实体的 3D 悬浮面板，支持多目标、进出场动画、三拨命中火花",
			"added | 脚本 HUD：mixclient/scripts 里放 JSON 就能自定义文字，12 个占位符可用，缩放/行距/背景/图标/重载键均可调",
			"added | 模组菜单：每个模组独立图标，可收藏并置顶到首页；主菜单头像可换成自定义图片",
			"added | 主菜单 Token 登录：直接用微软 Access Token 登录，不必经过启动器",
			"",
			"# ---- 修复 ----",
			"",
			"fixed | 目标指示器变成灰色圆圈：绘制前只关了 GL_TEXTURE_2D，没关 GL_LIGHTING(2896) 与 GL_FOG(2912)——开着光照时 glColor4f 给的颜色只是材质基色，还要再被默认的 GL_LIGHT0 白光照乘一遍（环境光 0.2），色相被抹平只剩灰度。现在七条 disable 一次到位，与已验证版本逐条一致",
			"fixed | 模组图标偏右上、且不同功能偏得不一样多：旧实现用 size/2 当垂直偏移，而 Material Symbols 的墨迹中线在 0.5 em、旧 Icon.ttf 在 0.3 em，两套字体差 0.2 em，48px 方块上就是固定 4.8px 下沉；改为查每个字形自己的墨迹中心",
			"fixed | 开启功能时图标底色「啪」一下变紫、没有过渡：三元表达式在开关进度跨过 0.01 的那一帧从灰直接跳到紫，描边还是常数 alpha；三处都改成按开关进度插值",
			"fixed | 关掉网易云岛或歌曲封面后，dango 的原生界面又会自己冒出来：两个 HUD 的 onDisable 里原本会反向把 dango 原生那颗打开，现已删掉",
			"fixed | 计分板换服务器后不贴屏幕边缘：把「贴边」记成锚点并在宽高变化时重新贴一次，不再死记像素坐标",
			"fixed | 目标指示器绘制完会把光照/雾/贴图永久关掉（表现：目标信息变白板、手臂与手持物变白、Dango 歌词变成纯色方块）：GL 状态改成 glPushAttrib(GL_ENABLE_BIT|GL_DEPTH_BUFFER_BIT) 整组压栈 + glPopAttrib 精确还原，绘制期只发 raw GL、完全不碰 GlStateManager 的 enable/disable —— 因为 GlStateManager$BooleanState.setState() 里有一句 if (wanted == currentState) return;，缓存没变就不发 GL 指令，raw 改过真实状态后那次「恢复」是空转",
			"fixed | 歌曲卡片与网易云岛的位置拖完重进游戏会回到默认角：它们原本用一个只看「画过没有」的 placed 布尔，第一次绘制时无条件覆盖坐标，把 profile 里读回来的位置冲掉；改成问 HUDMod.hasPosition()",
			"fixed | 二级设置页（模组详情）顶部与 GUI 磨砂区之间的那条硬分割线：整页铺底由一刀切改成 8px 渐变接缝，那条 1px 线整条删除",
			"fixed | 名字标签缩放拖了没反应：纹理缓存 key 不含 scale，命中短路后从不重烘，广告牌被拉大但贴图还是原来那张",
			"fixed | 点音乐页那颗「网易云音乐」按钮不再跳 Dango 的独立整屏界面（那条路会崩在 Sidebar 渲染上），改成切到本页的网易云音源",
			"fixed | 网易云界面崩溃的根因已修：汉化时误改了被字段/方法引用指向的常量池字符串，导致 NoSuchFieldError",
			"fixed | 胶囊外围的描边动画跑完一圈后会常亮保留，不再画完就消失",
			"fixed | 列表顶部的渐隐带铺满内容区宽度，与上方磨砂区平滑衔接，不再左右留白或出现切割线",
			"fixed | 关闭模组菜单不再停止音乐：观感上「关掉面板」不等于「不再听」，播放状态保持原样",
			"fixed | 配置菜单：点「新增配置」改成整页滑入，第二层完整盖住第一层",
			"fixed | 分类标签的小岛不再被下方卡片遮挡，功能内容整体下移，从小岛下方开始排",
			"fixed | 与热注入类外挂共存时不再崩渲染：绘制异常不再破坏 GL 状态栈，面板 FBO 会被回收",
			"fixed | 目标信息与名字标签被方块挡住时改为半透明遮掩，渐入渐出时文字和背景一起淡",
			"fixed | 模组卡片外圈流光不再被上下栏裁掉；启动器已登录时不再弹登录页；头像不再首帧显示全身皮肤",
			"",
			"# ---- 已移除 ----",
			"",
			"removed | 目标信息的「玩家光圈」（玩家脚底那圈细轮廓）已删除：观感上和目标指示器重复，且总和目标信息面板同进同退",
			"removed | 名字标签的穿墙显示选项已去掉",
	};

	/**
	 * Templates shipped by earlier builds. Used to detect files the player never edited:
	 * only a byte-for-byte match against one of these lists gets rewritten.
	 */
	private static final String[][] LEGACY_TEMPLATES = new String[][] {
		// 上一版（2026-11-06，模组开关淡入淡出 / 图标查表居中 / 指示器单环扫动 那批）
		new String[] {
			"added | 模组开关的配色改成真正的淡入淡出：图标底色在「灰 → 紫」之间按开关进度插值，描边和图标前景色也一起走，时长从约 90ms 拉到约 330ms（原来只有滑块在动，底色是一帧跳过去的）",
			"added | 图标居中改成编译期查表：两套图标字体共 3478 个字形的墨迹中心从 TTF 的 glyf 表直接读出、打进字节码，运行时一次二分 + 一次乘加定位，不再依赖 nvgTextBounds 的像素近似",
			"added | 目标指示器恢复成「一条环围绕玩家上下扫动」：环心在身高 5%~95% 之间往返，环带厚度固定为身高的 14%，不再是一根和玩家一样高的圆筒",
			"added | dango 界面全面中文化：设置页 / 播放条 / 派对页 / 登录框 / 列表标题一次翻完，歌词与歌名不会误翻",
			"added | 模组卡片开关动画：切换时外圈扩散两道光环、卡片内染色淡去、图标底色弹一下；关掉功能也有反馈，以前关掉时只剩滑块回位",
			"added | 模组菜单图标换成 v8.0 那套 Material Symbols：132 个功能各有专属图形，v8.0 没有的按功能含义自己选",
			"added | 目标指示器淡入淡出：单程 380ms，进度按真实时间线性推进再过 smoothstep（两端斜率为 0）；不用指数靠拢是因为它头 20% 时间就走完 63%，剩下的长尾肉眼看不出；丢目标后仍然留在目标最后的位置上自然淡掉",
			"added | 音乐时机：主菜单出现时只把正在播的那一路按下暂停，等真正进世界/进服务器（世界下载界面出现）才恢复；以前点了单人/多人、还没挑存档就恢复播放了",
			"added | Dango 歌曲卡片与顶部胶囊：曲目出现/消失时淡入淡出，换歌时旧内容淡出、新内容淡入并带轻微位移",
			"added | 音乐页新增「收藏」页：直接列出网易云「我喜欢的音乐」，点行就播，不用再进 Dango 那套整屏界面",
			"added | 音乐页的播放条现在可以直接拖进度：按下 seek 一次、拖动只改显示位置、松手再落一次（网易云连续 seek 会爆音，本地播放器会反复重缓冲）",
			"added | 音乐页的音量滑条同时作用于网易云那一路：以前它只喂本地播放器，想调网易云音量必须进 Dango 界面",
			"added | 音乐页整合网易云：多一个「网易云」标签，进去后列表下方带搜索框，回车搜歌、点行直接播",
			"added | 音乐页网易云歌单：队列空的时候列表改成歌单（推荐 / 榜单 / 我创建的），点开就看曲目、点曲目直接播",
			"added | 音乐页顶部那座悬浮胶囊小岛：宽度随分类标签自适应，带柔和投影和一圈描边入场动画",
			"added | 名字标签「缩放」区间放宽到 1.0-6.0 倍：纹理密度按 sqrt(scale) 提升，调大后不会越调越糊",
			"added | 主菜单右上角三颗按钮、模组菜单右上角搜索栏↔版本徽章：切换时淡入淡出并带轻微位移，不再硬切",
			"added | 目标信息：贴实体的 3D 悬浮面板，支持多目标、进出场动画、三拨命中火花",
			"added | 脚本 HUD：mixclient/scripts 里放 JSON 就能自定义文字，12 个占位符可用，缩放/行距/背景/图标/重载键均可调",
			"added | 模组菜单：每个模组独立图标，可收藏并置顶到首页；主菜单头像可换成自定义图片",
			"added | 主菜单 Token 登录：直接用微软 Access Token 登录，不必经过启动器",
			"fixed | 目标指示器变成一个以玩家身高为直径的大圆圈：上一版把「一条会动的环」误读成「一根和玩家一样高的圆筒」（四层高度 3%/34%/66%/97% 各铺一条带），已改回单条环 + sin 相位上下扫动",
			"fixed | 模组图标偏右上、且不同功能偏得不一样多：旧实现用 size/2 当垂直偏移，而 Material Symbols 的墨迹中线在 0.5 em、旧 Icon.ttf 在 0.3 em，两套字体差 0.2 em，48px 方块上就是固定 4.8px 下沉；改为查每个字形自己的墨迹中心",
			"fixed | 开启功能时图标底色「啪」一下变紫、没有过渡：三元表达式在开关进度跨过 0.01 的那一帧从灰直接跳到紫，描边还是常数 alpha；三处都改成按开关进度插值",
			"fixed | 关掉网易云岛或歌曲封面后，dango 的原生界面又会自己冒出来：两个 HUD 的 onDisable 里原本会反向把 dango 原生那颗打开，现已删掉",
			"fixed | 计分板换服务器后不贴屏幕边缘：把「贴边」记成锚点并在宽高变化时重新贴一次，不再死记像素坐标",
			"fixed | 目标指示器那圈几乎看不见：(1) alpha 从 0.40/0 拉到 0.72/0.06；(2) GL 状态改成 glPushAttrib(GL_ENABLE_BIT|GL_DEPTH_BUFFER_BIT) 整组压栈 + glPopAttrib 精确还原，期间不碰任何 GlStateManager 的 enable/disable —— GlStateManager$BooleanState.setState() 有一句 if(wanted==currentState) return;，缓存没变就不发 GL 指令，上一版正是因此把光照/雾/贴图永久关掉了（表现为目标信息白板、手臂变白、Dango 歌词变纯色方块）",
			"fixed | 歌曲卡片与网易云岛的位置拖完重进游戏会回到默认角：它们原本用一个只看「画过没有」的 placed 布尔，第一次绘制时无条件覆盖坐标，把 profile 里读回来的位置冲掉；改成问 HUDMod.hasPosition()",
			"fixed | 二级设置页（模组详情）顶部与 GUI 磨砂区之间的那条硬分割线：整页铺底由一刀切改成 8px 渐变接缝，那条 1px 线整条删除",
			"fixed | 名字标签缩放拖了没反应：纹理缓存 key 不含 scale，命中短路后从不重烘，广告牌被拉大但贴图还是原来那张",
			"fixed | 点音乐页那颗「网易云音乐」按钮不再跳 Dango 的独立整屏界面（那条路会崩在 Sidebar 渲染上），改成切到本页的网易云音源",
			"fixed | 网易云界面崩溃的根因已修：汉化时误改了被字段/方法引用指向的常量池字符串，导致 NoSuchFieldError",
			"fixed | 胶囊外围的描边动画跑完一圈后会常亮保留，不再画完就消失",
			"fixed | 列表顶部的渐隐带铺满内容区宽度，与上方磨砂区平滑衔接，不再左右留白或出现切割线",
			"fixed | 关闭模组菜单不再停止音乐：观感上「关掉面板」不等于「不再听」，播放状态保持原样",
			"fixed | 配置菜单：点「新增配置」改成整页滑入，第二层完整盖住第一层",
			"fixed | 分类标签的小岛不再被下方卡片遮挡，功能内容整体下移，从小岛下方开始排",
			"fixed | 与热注入类外挂共存时不再崩渲染：绘制异常不再破坏 GL 状态栈，面板 FBO 会被回收",
			"fixed | 目标信息与名字标签被方块挡住时改为半透明遮掩，渐入渐出时文字和背景一起淡",
			"fixed | 模组卡片外圈流光不再被上下栏裁掉；启动器已登录时不再弹登录页；头像不再首帧显示全身皮肤",
			"removed | 目标信息的「玩家光圈」（玩家脚底那圈细轮廓）已删除：观感上和目标指示器重复，且总和目标信息面板同进同退",
			"removed | 名字标签的穿墙显示选项已去掉",
		},
		// 上一版（2026-11-05，dango 中文化 / 图标墨迹居中 / 目标指示器全身 / 开关动画 那批）
		new String[] {
			"added | dango 界面全面中文化：设置页 / 播放条 / 派对页 / 登录框 / 列表标题一次翻完，歌词与歌名不会误翻",
			"added | 模组卡片开关动画：切换时外圈扩散两道光环、卡片内染色淡去、图标底色弹一下；关掉功能也有反馈，以前关掉时只剩滑块回位",
			"added | 模组菜单图标换成 v8.0 那套 Material Symbols：132 个功能各有专属图形，v8.0 没有的按功能含义自己选",
			"added | 目标指示器淡入淡出：单程 380ms，进度按真实时间线性推进再过 smoothstep（两端斜率为 0）；不用指数靠拢是因为它头 20% 时间就走完 63%，剩下的长尾肉眼看不出；丢目标后仍然留在目标最后的位置上自然淡掉",
			"added | 音乐时机：主菜单出现时只把正在播的那一路按下暂停，等真正进世界/进服务器（世界下载界面出现）才恢复；以前点了单人/多人、还没挑存档就恢复播放了",
			"added | Dango 歌曲卡片与顶部胶囊：曲目出现/消失时淡入淡出，换歌时旧内容淡出、新内容淡入并带轻微位移",
			"added | 音乐页新增「收藏」页：直接列出网易云「我喜欢的音乐」，点行就播，不用再进 Dango 那套整屏界面",
			"added | 音乐页的播放条现在可以直接拖进度：按下 seek 一次、拖动只改显示位置、松手再落一次（网易云连续 seek 会爆音，本地播放器会反复重缓冲）",
			"added | 音乐页的音量滑条同时作用于网易云那一路：以前它只喂本地播放器，想调网易云音量必须进 Dango 界面",
			"added | 音乐页整合网易云：多一个「网易云」标签，进去后列表下方带搜索框，回车搜歌、点行直接播",
			"added | 音乐页网易云歌单：队列空的时候列表改成歌单（推荐 / 榜单 / 我创建的），点开就看曲目、点曲目直接播",
			"added | 音乐页顶部那座悬浮胶囊小岛：宽度随分类标签自适应，带柔和投影和一圈描边入场动画",
			"added | 名字标签「缩放」区间放宽到 1.0-6.0 倍：纹理密度按 sqrt(scale) 提升，调大后不会越调越糊",
			"added | 主菜单右上角三颗按钮、模组菜单右上角搜索栏↔版本徽章：切换时淡入淡出并带轻微位移，不再硬切",
			"added | 目标信息：贴实体的 3D 悬浮面板，支持多目标、进出场动画、三拨命中火花",
			"added | 脚本 HUD：mixclient/scripts 里放 JSON 就能自定义文字，12 个占位符可用，缩放/行距/背景/图标/重载键均可调",
			"added | 模组菜单：每个模组独立图标，可收藏并置顶到首页；主菜单头像可换成自定义图片",
			"added | 主菜单 Token 登录：直接用微软 Access Token 登录，不必经过启动器",
			"fixed | 目标指示器只圈到胸部到腿：几何从写死的 +1 到 -0.5 改成按实体碰撞箱高度分四层（3% / 34% / 66% / 97%），玩家身上正好从脚踝到头顶，宽度也跟着实体体宽走",
			"fixed | 目标指示器的圆圈像没渲染完：竖直方向由两点线性插值改成四层 alpha（0.06 → 0.20 → 0.46 → 0.72），中段不再淡成一片雾；三段条带各自独立绘制，不再有贯穿整条柱面的竖缝",
			"fixed | 模组图标偏右上：改为按字形墨迹盒居中，度量与绘制共用同一套字号/字体/对齐/原点，水平和竖直的 bearing 偏差同时归零",
			"fixed | 关掉网易云岛或歌曲封面后，dango 的原生界面又会自己冒出来：两个 HUD 的 onDisable 里原本会反向把 dango 原生那颗打开，现已删掉",
			"fixed | 计分板换服务器后不贴屏幕边缘：把「贴边」记成锚点并在宽高变化时重新贴一次，不再死记像素坐标",
			"fixed | 目标指示器那圈几乎看不见：(1) alpha 从 0.40/0 拉到 0.72/0.06；(2) GL 状态改成 glPushAttrib(GL_ENABLE_BIT|GL_DEPTH_BUFFER_BIT) 整组压栈 + glPopAttrib 精确还原，期间不碰任何 GlStateManager 的 enable/disable —— GlStateManager$BooleanState.setState() 有一句 if(wanted==currentState) return;，缓存没变就不发 GL 指令，上一版正是因此把光照/雾/贴图永久关掉了（表现为目标信息白板、手臂变白、Dango 歌词变纯色方块）",
			"fixed | 歌曲卡片与网易云岛的位置拖完重进游戏会回到默认角：它们原本用一个只看「画过没有」的 placed 布尔，第一次绘制时无条件覆盖坐标，把 profile 里读回来的位置冲掉；改成问 HUDMod.hasPosition()",
			"fixed | 二级设置页（模组详情）顶部与 GUI 磨砂区之间的那条硬分割线：整页铺底由一刀切改成 8px 渐变接缝，那条 1px 线整条删除",
			"fixed | 名字标签缩放拖了没反应：纹理缓存 key 不含 scale，命中短路后从不重烘，广告牌被拉大但贴图还是原来那张",
			"fixed | 点音乐页那颗「网易云音乐」按钮不再跳 Dango 的独立整屏界面（那条路会崩在 Sidebar 渲染上），改成切到本页的网易云音源",
			"fixed | 网易云界面崩溃的根因已修：汉化时误改了被字段/方法引用指向的常量池字符串，导致 NoSuchFieldError",
			"fixed | 胶囊外围的描边动画跑完一圈后会常亮保留，不再画完就消失",
			"fixed | 列表顶部的渐隐带铺满内容区宽度，与上方磨砂区平滑衔接，不再左右留白或出现切割线",
			"fixed | 关闭模组菜单不再停止音乐：观感上「关掉面板」不等于「不再听」，播放状态保持原样",
			"fixed | 配置菜单：点「新增配置」改成整页滑入，第二层完整盖住第一层",
			"fixed | 分类标签的小岛不再被下方卡片遮挡，功能内容整体下移，从小岛下方开始排",
			"fixed | 与热注入类外挂共存时不再崩渲染：绘制异常不再破坏 GL 状态栈，面板 FBO 会被回收",
			"fixed | 目标信息与名字标签被方块挡住时改为半透明遮掩，渐入渐出时文字和背景一起淡",
			"fixed | 模组卡片外圈流光不再被上下栏裁掉；启动器已登录时不再弹登录页；头像不再首帧显示全身皮肤",
			"removed | 目标信息的「玩家光圈」（玩家脚底那圈细轮廓）已删除：观感上和目标指示器重复，且总和目标信息面板同进同退",
			"removed | 名字标签的穿墙显示选项已去掉",
		},
		// 上一版（2026-10-28，网易云搜索 / 光圈常亮那批）
		new String[] {
			"added | 音乐页整合网易云：多一个「网易云」标签，进去后列表下方带搜索框，回车搜歌、点行直接播",
			"added | 音乐页网易云歌单：队列空的时候列表改成歌单（推荐 / 榜单 / 我创建的），点开就看曲目、点曲目直接播",
			"added | 音乐页顶部那座悬浮胶囊小岛：宽度随分类标签自适应，带柔和投影和一圈描边入场动画",
			"added | 目标信息：贴实体的 3D 悬浮面板，支持多目标、进出场动画、三拨命中火花",
			"added | 脚本 HUD：mixclient/scripts 里放 JSON 就能自定义文字，12 个占位符可用，缩放/行距/背景/图标/重载键均可调",
			"added | 模组菜单：每个模组独立图标，可收藏并置顶到首页；主菜单头像可换成自定义图片",
			"added | 主菜单 Token 登录：直接用微软 Access Token 登录，不必经过启动器",
			"fixed | 点音乐页那颗「网易云音乐」按钮不再跳 Dango 的独立整屏界面（那条路会崩在 Sidebar 渲染上），改成切到本页的网易云音源",
			"fixed | 网易云界面崩溃的根因已修：汉化时误改了被字段/方法引用指向的常量池字符串，导致 NoSuchFieldError",
			"fixed | 胶囊外围的描边动画跑完一圈后会常亮保留，不再画完就消失",
			"fixed | 列表顶部的渐隐带铺满内容区宽度，与上方磨砂区平滑衔接，不再左右留白或出现切割线",
			"fixed | 关闭模组菜单时正在播放的音乐会一起停掉，不会在后台继续放",
			"fixed | 配置菜单：点「新增配置」改成整页滑入，第二层完整盖住第一层",
			"fixed | 分类标签的小岛不再被下方卡片遮挡，功能内容整体下移，从小岛下方开始排",
			"fixed | 与热注入类外挂共存时不再崩渲染：绘制异常不再破坏 GL 状态栈，面板 FBO 会被回收",
			"fixed | 目标信息与名字标签被方块挡住时改为半透明遮掩，渐入渐出时文字和背景一起淡",
			"fixed | 模组卡片外圈流光不再被上下栏裁掉；启动器已登录时不再弹登录页；头像不再首帧显示全身皮肤",
			"removed | 名字标签的穿墙显示选项已去掉",
		},
		new String[] {
			"added | 主菜单（单人游戏/多人游戏那个界面）出现时自动暂停音乐，离开主菜单自动续播；只暂停「正在播」的那一路，用户自己手动暂停的不会被复活",
			"added | Dango 歌曲卡片与顶部胶囊：曲目出现/消失时淡入淡出，换歌时旧内容淡出、新内容淡入并带轻微位移",
			"added | 音乐页整合网易云：多一个「网易云」标签，进去后列表下方带搜索框，回车搜歌、点行直接播",
			"added | 音乐页网易云歌单：队列空的时候列表改成歌单（推荐 / 榜单 / 我创建的），点开就看曲目、点曲目直接播",
			"added | 音乐页顶部那座悬浮胶囊小岛：宽度随分类标签自适应，带柔和投影和一圈描边入场动画",
			"added | 目标信息：贴实体的 3D 悬浮面板，支持多目标、进出场动画、三拨命中火花",
			"added | 脚本 HUD：mixclient/scripts 里放 JSON 就能自定义文字，12 个占位符可用，缩放/行距/背景/图标/重载键均可调",
			"added | 模组菜单：每个模组独立图标，可收藏并置顶到首页；主菜单头像可换成自定义图片",
			"added | 主菜单 Token 登录：直接用微软 Access Token 登录，不必经过启动器",
			"fixed | 点音乐页那颗「网易云音乐」按钮不再跳 Dango 的独立整屏界面（那条路会崩在 Sidebar 渲染上），改成切到本页的网易云音源",
			"fixed | 网易云界面崩溃的根因已修：汉化时误改了被字段/方法引用指向的常量池字符串，导致 NoSuchFieldError",
			"fixed | 胶囊外围的描边动画跑完一圈后会常亮保留，不再画完就消失",
			"fixed | 列表顶部的渐隐带铺满内容区宽度，与上方磨砂区平滑衔接，不再左右留白或出现切割线",
			"fixed | 关闭模组菜单不再停止音乐：观感上「关掉面板」不等于「不再听」，播放状态保持原样",
			"fixed | 配置菜单：点「新增配置」改成整页滑入，第二层完整盖住第一层",
			"fixed | 分类标签的小岛不再被下方卡片遮挡，功能内容整体下移，从小岛下方开始排",
			"fixed | 与热注入类外挂共存时不再崩渲染：绘制异常不再破坏 GL 状态栈，面板 FBO 会被回收",
			"fixed | 目标信息与名字标签被方块挡住时改为半透明遮掩，渐入渐出时文字和背景一起淡",
			"fixed | 模组卡片外圈流光不再被上下栏裁掉；启动器已登录时不再弹登录页；头像不再首帧显示全身皮肤",
			"fixed | 二级设置页（模组详情）顶部与底部换成和一级页一致的渐变遮挡带，滚到边的行是淡出而不是被硬切断；那条 1px 全亮分割线降成发丝线",
			"removed | 名字标签的穿墙显示选项已去掉",
		},
		// 2026-10-27 那版（只到「小岛 / 内容区去分隔线」为止，还没有网易云搜索、光圈常亮等条目）。
		new String[] {
			"added | 模组菜单：顶部改成独立的悬浮胶囊小岛，宽度随分类标签自适应，带柔和投影和描边入场动画",
			"added | 目标信息：贴实体的 3D 悬浮面板，支持多目标、进出场动画、三拨命中火花",
			"added | 目标信息：「面板大小」1.0-6.0 倍可调；另加「围绕玩家」开关，关掉回到屏幕固定面板",
			"added | 脚本 HUD：mixclient/scripts 里放 JSON 就能自定义文字，12 个占位符可用",
			"added | 脚本 HUD 可调：缩放 / 行间距 / 背景 / 图标 / 重载按键",
			"added | 模组菜单：每个模组独立图标，可收藏并置顶到首页；主菜单头像可换成自定义图片",
			"added | 主菜单 Token 登录：直接用微软 Access Token 登录，不必经过启动器",
			"fixed | 配置菜单：点「新增配置」改成整页滑入，第二层完整盖住第一层",
			"fixed | 模组菜单：内容区顶部那条横贯整幅的硬分割线已去掉，边界交给悬浮小岛自己交代",
			"fixed | 分类标签的圆环不再被下方卡片遮挡，功能内容整体下移，从小岛下方开始排",
			"fixed | 与热注入类外挂共存时不再崩渲染：绘制异常不再破坏 GL 状态栈，面板 FBO 会被回收",
			"fixed | 目标信息与名字标签被方块挡住时改为半透明遮掩，渐入渐出时文字和背景一起淡",
			"fixed | 模组卡片外圈流光不再被上下栏裁掉；启动器已登录时不再弹登录页；头像不再首帧显示全身皮肤",
			"removed | 名字标签的穿墙显示选项已去掉"
		},
		// 2026-10-25 那版（自由视角过渡动画、ForceMainHand / Pitch 等早已删掉的条目还留在里面）。
		new String[] {
			"added | 自由视角：切出/切回时有先快后慢的拉出动画，设置里可调过渡速度（5-28）",
			"added | 脚本 HUD：在 mixclient/scripts 放 JSON 就能自定义文字，支持 {fps} {cps} {ping} {x} {y} {z} {name} {biome} {dimension} {time} {health} {armor}",
			"added | 脚本 HUD 可调：整体缩放 / 行间距 / 背景 / 图标，可绑一个重载按键，改完 JSON 按键即生效",
			"added | 目标信息：贴实体的 3D 悬浮面板，多目标 / 进出场动画",
			"added | 命中火花按 hurtTime 计时（每击 0.5 秒），最多三拨同时存在，下一拨到来时最旧的一拨淡出",
			"added | 模组菜单：每个模组独立图标，可收藏并在首页直接开关",
			"added | 模组菜单：引进 Mix v8.0 样式",
			"added | 功能：SnapTap / NoJumpDelay / ForceMainHand / Pitch / Yaw / JumpResetIndicator",
			"added | 主菜单账号头像：解析正版皮肤，可换成自定义图片",
			"added | Token 登录：主菜单可直接用微软 Access Token 登录",
			"fixed | 与热注入类外挂共存时不再崩渲染：NanoVG 绘制异常不再破坏 GL 状态栈，面板 FBO 会被正确回收",
			"fixed | 目标信息与名字标签被方块挡住时不再整块消失，改为半透明遮掩",
			"fixed | 名字标签的渐入渐出现在文字和背景一起淡，不再只有背景淡、文字直接消失",
			"fixed | 模组卡片首行/末行的外圈流光不再被上下栏裁掉",
			"fixed | 启动器已登录时不再弹登录页，欢迎流程结束直接进主菜单",
			"fixed | 头像第一次显示成全身皮肤、第二次才是正常头像的问题",
			"fixed | 目标指示器颜色跟随菜单强调色，不再发白",
			"fixed | 手臂与目标面板被打后变色、半透明",
			"fixed | Ping 每 5 秒刷新；名字标签 11 格淡出、15 格消失",
			"removed | 名字标签的穿墙显示选项已去掉"
		},
		// 上一版（小岛之前的芯片圆环版）。
		new String[] {
			"added | 目标信息：「面板大小」区间放宽到 1.0-6.0 倍，默认 2.5，调大后字也不再糊",
			"added | 目标信息：新增「围绕玩家」开关，关掉后回到屏幕固定面板，可拖动可缩放",
			"added | 脚本 HUD：在 mixclient/scripts 放 JSON 就能自定义文字，支持 {fps} {cps} {ping} {x} {y} {z} {name} {biome} {dimension} {time} {health} {armor}",
			"added | 脚本 HUD 可调：整体缩放 / 行间距 / 背景 / 图标，可绑一个重载按键，改完 JSON 按键即生效",
			"added | 目标信息：贴实体的 3D 悬浮面板，多目标 / 进出场动画",
			"added | 命中火花按 hurtTime 计时（每击 0.5 秒），最多三拨同时存在，下一拨到来时最旧的一拨淡出",
			"added | 模组菜单：每个模组独立图标，可收藏并在首页直接开关",
			"added | 主菜单账号头像：解析正版皮肤，可换成自定义图片",
			"fixed | 配置菜单：点「新增配置」后列表会整页滑出，不再两层叠在一起看不出新旧",
			"fixed | 与热注入类外挂共存时不再崩渲染：NanoVG 绘制异常不再破坏 GL 状态栈，面板 FBO 会被正确回收",
			"fixed | 目标信息与名字标签被方块挡住时不再整块消失，改为半透明遮掩",
			"fixed | 名字标签的渐入渐出现在文字和背景一起淡，不再只有背景淡、文字直接消失",
			"fixed | 模组卡片首行/末行的外圈流光不再被上下栏裁掉",
			"fixed | 启动器已登录时不再弹登录页，欢迎流程结束直接进主菜单",
			"fixed | 头像第一次显示成全身皮肤、第二次才是正常头像的问题",
			"fixed | 目标指示器颜色跟随菜单强调色，不再发白",
			"fixed | 手臂与目标面板被打后变色、半透明",
			"fixed | Ping 每 5 秒刷新；名字标签 11 格淡出、15 格消失",
			"removed | 名字标签的穿墙显示选项已去掉",
			"removed | 自由视角的拉出过渡动画已去掉，切出/切回恢复成直接切换"
		},
		new String[] {
			"added | 自由视角：切出/切回时有先快后慢的拉出动画，可在设置里调过渡速度（5-28）",
			"added | 脚本 HUD：在 mixclient/scripts 放 JSON 就能自定义文字，支持 {fps} {cps} {ping} {x} {y} {z} {name} {biome} {dimension} {time} {health} {armor}",
			"added | 脚本 HUD 可调：整体缩放 / 行间距 / 背景 / 图标，可绑一个重载按键，改完 JSON 按键即生效",
			"added | 目标信息：贴实体的 3D 悬浮面板，多目标 / 进出场动画",
			"added | 命中火花按 hurtTime 计时（每击 0.5 秒），最多三拨同时存在，下一拨到来时最旧的一拨淡出",
			"added | 模组菜单：每个模组独立图标，可收藏并在首页直接开关",
			"added | 主菜单账号头像：解析正版皮肤，可换成自定义图片",
			"fixed | 与热注入类外挂共存时不再崩渲染：NanoVG 绘制异常不再破坏 GL 状态栈，面板 FBO 会被正确回收",
			"fixed | 目标信息与名字标签被方块挡住时不再整块消失，改为半透明遮掩",
			"fixed | 名字标签的渐入渐出现在文字和背景一起淡，不再只有背景淡、文字直接消失",
			"fixed | 模组卡片首行/末行的外圈流光不再被上下栏裁掉",
			"fixed | 启动器已登录时不再弹登录页，欢迎流程结束直接进主菜单",
			"fixed | 头像第一次显示成全身皮肤、第二次才是正常头像的问题",
			"fixed | 目标指示器颜色跟随菜单强调色，不再发白",
			"fixed | 手臂与目标面板被打后变色、半透明",
			"fixed | Ping 每 5 秒刷新；名字标签 11 格淡出、15 格消失",
			"removed | 名字标签的穿墙显示选项已去掉"
		},
		new String[] {
			"added | Token 登录：主菜单可直接用微软 Access Token 登录",
			"added | 目标信息：贴实体的 3D 悬浮面板，多目标 / 遮挡隐藏 / 进出场动画",
			"added | 目标信息：命中火花迸发，每下随机 30-40 颗",
			"added | 模组菜单：每个模组独立图标，可收藏并在首页直接开关",
			"added | 主菜单账号头像：解析正版皮肤，可换成自定义图片",
			"fixed | 目标指示器颜色跟随菜单强调色，不再发白",
			"fixed | 手臂与目标面板被打后变色、半透明",
			"fixed | Ping 每 5 秒刷新；名字标签 11 格淡出、15 格消失"
		},
		new String[] {
			"added | 脚本 HUD：在 mixclient/scripts 放 JSON 就能自定义 HUD 文字，支持 {fps} {cps} {ping} {x} {y} {z} {name} {biome} {dimension} {time} {health} {armor}",
			"added | 脚本 HUD 可调：整体缩放 / 行间距 / 背景 / 图标，并可绑一个重载按键，改完 JSON 按键即生效",
			"added | 目标信息：贴实体的 3D 悬浮面板，多目标 / 进出场动画",
			"added | 命中火花按 hurtTime 计时（每击 0.5 秒），下一击到来时上一击的粒子在 100ms 内淡完",
			"added | 模组菜单：每个模组独立图标，可收藏并在首页直接开关",
			"added | 主菜单账号头像：解析正版皮肤，可换成自定义图片",
			"fixed | 目标信息与名字标签被方块挡住时不再整块消失，改为半透明遮掩",
			"fixed | 名字标签的渐入渐出现在文字和背景一起淡，不再只有背景淡、文字直接消失",
			"fixed | 模组卡片首行/末行的外圈流光不再被上下栏裁掉",
			"fixed | 启动器已登录时不再弹登录页，欢迎流程结束直接进主菜单",
			"fixed | 头像第一次显示成全身皮肤、第二次才是正常头像的问题",
			"fixed | 目标指示器颜色跟随菜单强调色，不再发白",
			"fixed | 手臂与目标面板被打后变色、半透明",
			"fixed | Ping 每 5 秒刷新；名字标签 11 格淡出、15 格消失"
		}
	};

	private CopyOnWriteArrayList<Changelog> changelogs = new CopyOnWriteArrayList<Changelog>();

	public ChangelogManager() {
		Multithreading.runAsync(() -> loadChangelog());
	}

	private void loadChangelog() {

		if(loadLocalChangelog()) {
			return;
		}

		loadRemoteChangelog();
	}

	private File getChangelogFile() {
		return new File(Mix.getInstance().getFileManager().getMixDir(), FILE_NAME);
	}

	private boolean loadLocalChangelog() {

		CopyOnWriteArrayList<Changelog> local = readLocalEntries();

		if(local.isEmpty()) {
			return false;
		}

		// Clear first: the async load from the constructor and the reload that runs
		// when the menu opens must not stack up duplicate rows.
		changelogs.clear();
		changelogs.addAll(local);
		sortChangelogs();
		return true;
	}

	/**
	 * 按类型归组：新增 -> 修复 -> 移除 -> 重要，同类型内部保持文件里的原始顺序。
	 * 这样界面上不会再出现"新增 / 修复 / 新增 / 修复"这种跳来跳去的排版。
	 */
	private void sortChangelogs() {

		ArrayList<Changelog> sorted = new ArrayList<Changelog>(changelogs);

		// Collections.sort 是稳定排序，同类型的相对顺序不会被打乱
		Collections.sort(sorted, new Comparator<Changelog>() {
			@Override
			public int compare(Changelog a, Changelog b) {
				return getSortWeight(a.getType()) - getSortWeight(b.getType());
			}
		});

		changelogs.clear();
		changelogs.addAll(sorted);
	}

	private static int getSortWeight(ChangelogType type) {

		if(type == null) {
			return ChangelogType.ADDED.getId();
		}

		if(type == ChangelogType.ERROR) {
			// 重要的放最后，别顶在第一条
			return 99;
		}

		return type.getId();
	}

	/**
	 * Reads mixclient/changelog.txt, creating the commented template on first run.
	 * Returns an empty list when the file has no usable entries.
	 */
	private CopyOnWriteArrayList<Changelog> readLocalEntries() {

		CopyOnWriteArrayList<Changelog> entries = new CopyOnWriteArrayList<Changelog>();
		File file = getChangelogFile();

		try {

			if(!file.exists()) {
				writeTemplate(file);
			} else {
				upgradeTemplateIfUntouched(file);
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

			try {

				String line;

				while((line = reader.readLine()) != null) {

					line = line.trim();

					if(line.isEmpty() || line.startsWith("#")) {
						continue;
					}

					ChangelogType type = ChangelogType.ADDED;
					String text = line;
					int split = line.indexOf('|');

					if(split > 0) {
						type = getTypeByName(line.substring(0, split));
						text = line.substring(split + 1).trim();
					}

					if(text.isEmpty()) {
						continue;
					}

					entries.add(new Changelog(text, type));
				}

			} finally {
				reader.close();
			}

			if(entries.isEmpty()) {
				MixLogger.info("[MixPatch] changelog: " + file.getPath() + " has no entries, using remote list");
			}else {
				MixLogger.info("[MixPatch] changelog: loaded " + entries.size() + " local entries from " + file.getPath());
			}

		} catch (Exception e) {
			MixLogger.error("[MixPatch] changelog: failed to read " + file.getPath(), e);
		}

		return entries;
	}

	/**
	 * Rewrites mixclient/changelog.txt when it still holds exactly the list this client used
	 * to ship. Anybody who edited the file keeps their own text: only a byte-for-byte
	 * match against the old template is replaced.
	 */
	private void upgradeTemplateIfUntouched(File file) {

		try {

			List<String> lines = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

			try {

				String line;

				while((line = reader.readLine()) != null) {

					String trimmed = line.trim();

					if(trimmed.isEmpty() || trimmed.startsWith("#")) {
						continue;
					}

					lines.add(trimmed);
				}

			} finally {
				reader.close();
			}

			if(!matchesLegacyTemplate(lines)) {
				return;
			}

			writeTemplate(file);

			MixLogger.info("[MixPatch] changelog: upgraded untouched template at " + file.getPath());

		} catch(Exception e) {
			MixLogger.error("[MixPatch] changelog: failed to upgrade " + file.getPath(), e);
		}
	}

	private static boolean matchesLegacyTemplate(List<String> lines) {

		for(String[] template : LEGACY_TEMPLATES) {

			if(lines.size() != template.length) {
				continue;
			}

			boolean same = true;

			for(int i = 0; i < template.length; i++) {

				if(!template[i].equals(lines.get(i))) {
					same = false;
					break;
				}
			}

			if(same) {
				return true;
			}
		}

		return false;
	}

	private void writeTemplate(File file) {

		File parent = file.getParentFile();

		if(parent != null) {
			parent.mkdirs();
		}

		Writer writer = null;

		try {

			writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);

			for(String line : TEMPLATE) {
				writer.write(line);
				writer.write("\n");
			}

			writer.flush();

			MixLogger.info("[MixPatch] changelog: created template at " + file.getPath());

		} catch (IOException e) {
			MixLogger.error("[MixPatch] changelog: failed to create " + file.getPath(), e);
		} finally {

			if(writer != null) {

				try {
					writer.close();
				} catch (IOException e) {
					MixLogger.error("[MixPatch] changelog: failed to close " + file.getPath(), e);
				}
			}
		}
	}

	private ChangelogType getTypeByName(String name) {

		String value = name.trim().toLowerCase(Locale.ENGLISH);

		if(value.equals("fixed") || value.equals("fix") || value.equals("修复")) {
			return ChangelogType.FIXED;
		}

		if(value.equals("removed") || value.equals("remove") || value.equals("移除")) {
			return ChangelogType.REMOVED;
		}

		if(value.equals("error") || value.equals("错误") || value.equals("重要")) {
			return ChangelogType.ERROR;
		}

		return ChangelogType.ADDED;
	}

	private void loadRemoteChangelog() {

		/* No Mix host by default: a blank property means "no changelog feed", so
		   the client makes no outbound call. */
		String url = System.getProperty("mixclient.changelogUrl", "");
		
		if(url.isEmpty()) {
			return;
		}
		
		JsonObject jsonObject = HttpUtils.readJson(url, null);

		if(jsonObject != null) {

			JsonArray jsonArray = JsonUtils.getArrayProperty(jsonObject, "changelogs");

			if(jsonArray != null) {

				Iterator<JsonElement> iterator = jsonArray.iterator();

				while(iterator.hasNext()) {

					JsonElement jsonElement = (JsonElement) iterator.next();
					Gson gson = new Gson();
					JsonObject changelogJsonObject = gson.fromJson(jsonElement, JsonObject.class);

					changelogs.add(new Changelog(JsonUtils.getStringProperty(changelogJsonObject, "text", "null"),
							ChangelogType.getTypeById(JsonUtils.getIntProperty(changelogJsonObject, "type", 999))));
				}
			}
		}
	}

	public CopyOnWriteArrayList<Changelog> getChangelogs() {
		return changelogs;
	}

	/**
	 * Re-reads the local file (no network). The mod menu calls this every time it
	 * opens, so edits to changelog.txt show up without restarting the game.
	 */
	public void reload() {

		CopyOnWriteArrayList<Changelog> local = readLocalEntries();

		if(local.isEmpty()) {
			return;
		}

		changelogs.clear();
		changelogs.addAll(local);
		sortChangelogs();
	}

	public File getFile() {
		return getChangelogFile();
	}
}
