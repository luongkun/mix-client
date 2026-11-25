package mix.client.mixui.gui.modmenu;

import java.awt.Color;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

import mix.client.mixui.management.nanovg.NanoVGManager;

/**
 * 分类标签（chip 行）外面那座「悬浮小岛」。
 *
 * <p>为什么单独一个类：功能 / 配置 / 披风 / 音乐四个页面顶部都有同一排标签，
 * 小岛的几何、配色、投影、入场动画必须完全一致，各写一份迟早画歪。</p>
 *
 * <h3>几何</h3>
 * <p>完全由标签行本身决定：x = 第一个标签的左边界 - {@link #PAD_X}，
 * y = 标签行上边界 - {@link #PAD_Y}，宽 = 整排标签的总宽 + 两侧留白，
 * 高 = 标签高 + 上下留白，圆角取 高/2 -> 跑道形（stadium）。
 * 标签数量、文案长短、窗口尺寸变化都不需要额外调整，也不会无故撑满整页。</p>
 *
 * <h3>与列表的层级（小岛浮在列表之上）</h3>
 * <p>小岛是<b>独立浮在列表区域左上角</b>的一枚胶囊，不是页面顶部的一条横栏。
 * 因此绘制顺序被固定成：<b>先画列表（卡片 / 行），再画小岛，最后画标签</b>。
 * 列表滚动时卡片会从小岛背后穿过，小岛始终完整地浮在上面。</p>
 *
 * <p>列表的滚动视口上沿 = {@link #islandTop(float)}（小岛顶边），
 * 列表内容里预留一段与小岛等高的空白 {@link #contentTop(float, float)}，
 * 这段空白跟着内容一起往上滑走，所以既能「列表顶部延伸到胶囊顶部」，
 * 又不会让小岛压住最上面那一行卡片的内容。</p>
 *
 * <h3>入场动画</h3>
 * <p>从左侧中点起笔，沿小岛边缘顺时针（上 → 右 → 下 → 左）绕行，
 * 缓动 1-(1-p)^4.5：起步很快，越接近闭合越慢，最后一段几乎是爬过去收口。
 * 只在进入菜单 / 从左侧栏切回该分类时播一次，收口后轮廓保持常亮，不旋转不重播。</p>
 *
 * <p>小岛的投影、填充、静态轮廓在动画第一帧就已经完整存在，
 * 动画只是把一圈高光沿着边缘补上——所以静态小岛本身也是完整的，不会「先空后满」。</p>
 */
public class CategoryIslandRenderer {

	/** 小岛相对标签行向外扩张的水平留白。 */
	public static final float PAD_X = 12F;

	/** 小岛相对标签行向外扩张的垂直留白（上下各一份）。 */
	public static final float PAD_Y = 7F;

	/** 小岛底边与列表首行内容之间的间距（列表内容顶部空白的一部分）。 */
	public static final float GAP_BELOW = 16F;

	private static final float STROKE = 1.3F;
	private static final long INTRO_MS = 1150L;
	private static final double INTRO_POWER = 4.5D;

	/**
	 * 入场画完一圈之后，这圈高光留在胶囊上的透明度（0-255）。
	 *
	 * <p>给到 118 是试出来的：再低（90 以下）在浅色面板上几乎看不见，等于又回到
	 * 「跑完就消失」；再高（150 以上）紫边会盖过岛面本身的亮度差，读起来像描了个
	 * 外框。118 大致等于 ringColor 本身的饱和度被压掉一半，看得见但不抢。</p>
	 */
	private static final float SETTLED_ALPHA = 118F;

	private static final int SAMPLES = 320;

	/**
	 * 接触层：贴着胶囊轮廓的一圈短程扩散，负责交代「胶囊比背景高一点点」。
	 * 层数多、步长极小（spread / layers 远小于线宽），相邻层互相重叠，
	 * 叠加出来是一段连续衰减的通道，看不出逐层的环。
	 */
	private static final int CONTACT_LAYERS = 14;

	/** 接触层向外扩的总距离（设计单位）。5px：贴着轮廓交代高度差，不铺开。 */
	private static final float CONTACT_SPREAD = 5.0F;

	/**
	 * 接触层最内那一层的透明度。最外层会衰减到接近 0。
	 *
	 * <p>第一版给的是 34/16，在白底面板上等于没画 —— 用户反馈「悬浮还是不明显」。
	 * 逐层描边叠加出来的实际浓度和单层 alpha 不成正比（同样的 alpha 反复盖十几次），
	 * 所以想读出「浮起」，峰值必须给到 78 这个量级；再多就会在紫白配色上读成脏灰。</p>
	 */
	private static final float CONTACT_PEAK_ALPHA = 32F;

	/** 扩散层：同一枚胶囊的第二层阴影，范围更大、浓度为接触层的一半。 */
	private static final int DIFFUSE_LAYERS = 26;

	/** 扩散层向外扩的总距离。16px 足够读出「浮起」，再大会糊成一片灰。 */
	private static final float DIFFUSE_SPREAD = 20.0F;

	/** 扩散层最内那一层的透明度。约接触层的一半，两层叠起来才不会发黑。 */
	private static final float DIFFUSE_PEAK_ALPHA = 20F;

	/** 扩散层整体相对胶囊向下偏移，模拟光源在正上方。 */
	private static final float DIFFUSE_OFFSET_Y = 4.2F;
	/** 接触层整体相对胶囊向下偏移：光源在上，贴地那一圈会略微往下让。 */
	private static final float CONTACT_OFFSET_Y = 1.2F;
	

	private final float[] sampleX = new float[SAMPLES + 1];
	private final float[] sampleY = new float[SAMPLES + 1];

	private final NVGColor strokeColor = NVGColor.create();

	// 阴影同样是逐帧画的，走和描边高光同一条路：复用 NVGColor，绝不碰
	// nvg.getColor(...)（那个按 getRGB() 永久缓存，每帧新建颜色会无上限增长）。
	private final NVGColor shadowColor = NVGColor.create();

	private float cachedX = Float.NaN;
	private float cachedY = Float.NaN;
	private float cachedW = Float.NaN;
	private float cachedH = Float.NaN;
	private float totalLength;

	private long startMs;
	private boolean started;

	/** 小岛顶边（= 标签行上边界 - 上留白）。列表的滚动视口上沿就在这里。 */
	public static float islandTop(float chipY) {
		return chipY - PAD_Y;
	}

	/** 小岛底边。 */
	public static float islandBottom(float chipY, float chipHeight) {
		return chipY + chipHeight + PAD_Y;
	}

	/** 小岛高度（胶囊总高）。 */
	public static float islandHeight(float chipHeight) {
		return chipHeight + PAD_Y * 2F;
	}

	/** 列表内容顶部预留的空白高度：一整个胶囊 + 一点呼吸，随内容一起滚走。 */
	public static float topSpacer(float chipHeight) {
		return islandHeight(chipHeight) + GAP_BELOW;
	}

	/**
	 * 列表内容里第一行卡片 / 行的 y。
	 *
	 * <p>= 小岛顶边 + 与小岛等高的空白 + 间距。这段空白属于滚动内容，
	 * 往上滚时会被小岛盖掉并从视口上沿消失，所以小岛永远压不到最上面一行。</p>
	 */
	public static float contentTop(float chipY, float chipHeight) {
		return islandTop(chipY) + topSpacer(chipHeight);
	}
	
	/**
	 * 内容区顶部那段「磨砂渐变还没落地」的高度。
	 *
	 * <p>GuiModMenu 的 HEADER_FROST_DEPTH 是 30：面板那层 vertical ramp 的 alpha
	 * 要到 {@code headerHeight + 30} 才是满的。列表视口上沿如果戳到它上面，
	 * 后面再叠一条 alpha=255 的遮挡带，就是 40 多级的 alpha 台阶——
	 * 也就是用户看到的那条「生硬切割线」。所以视口上沿必须整段落在它下面。</p>
	 */
	private static final float FROST_SETTLE = 30F;
	
	/**
	 * 列表视口上沿：小岛底边，且一定在磨砂渐变落地之后。
	 *
	 * <p>为什么不再是 {@link #islandTop(float)}：行被裁在小岛顶边时，裁剪线正好落在
	 * 面板还没变实的那一段上，遮挡带只能靠「瞬间给满 alpha」去盖住切口，
	 * 盖出来的就是那条横线。裁到小岛底边，遮挡带的首行 alpha 与面板相等，
	 * 接缝自然消失；代价只是小岛那一带不再让行穿过去。</p>
	 */
	public static float viewportTop(float chipY, float chipHeight) {
		return Math.max(islandBottom(chipY, chipHeight), chipY - 10F + FROST_SETTLE);
	}
	
	/** 视口上沿到首行内容之间的空白。属于滚动内容，会随滚动一起滑走。 */
	public static float viewportSpacer(float chipY, float chipHeight) {
		return Math.max(0F, contentTop(chipY, chipHeight) - viewportTop(chipY, chipHeight));
	}


	/**
	 * 鼠标是否落在小岛覆盖的区域上。落在里面时列表不应响应点击——
	 * 小岛是浮在列表上层的，被它盖住的卡片本来也点不到。
	 */
	public static boolean overIsland(int mouseX, int mouseY, float firstChipX, float chipY, float rowWidth, float chipHeight) {
		float x = firstChipX - PAD_X;
		float y = chipY - PAD_Y;
		float w = rowWidth + PAD_X * 2F;
		float h = chipHeight + PAD_Y * 2F;
		return mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h;
	}


	/**
	 * 列表顶部的渐隐遮挡带。
	 *
	 * <p>为什么需要：列表视口上沿被刻意抬到小岛顶边，行往上滚时会整片出现在
	 * 小岛上方那块区域里，和顶栏的磨砂渐变撞在一起，读起来像列表从面板外面漏出来。
	 * 所以在小岛之下叠一层从面板色渐到透明的竖带，让滚上去的行淡出。</p>
	 *
	 * <p>为什么画在 chipIsland.draw 之前：用户明确要求这条过渡带遮挡卡片、
	 * 但不要遮挡胶囊——小岛是悬浮的独立胶囊。绘制顺序固定成
	 * 「列表 -> 这条带子 -> 小岛 -> 标签」，带子只糊掉它身后的行，
	 * 小岛的轮廓高光与标签始终完整地浮在最上面。</p>
	 */
	public static void drawScrollFade(NanoVGManager nvg, float x, float width,
			float chipY, float chipHeight, Color base) {

		if(width <= 1F) {
			return;
		}

		// 上沿取 viewportTop 而不是 islandTop：viewportTop 已经被夹到
		// 「面板 ramp 落地之后」，那里面板本身是满 alpha，遮挡带首行也给满，
		// 两者相等就没有台阶；同时它也是列表的裁剪线，行在裁剪处刚好被完全盖住，
		// 切口看不出来。下沿停在首行内容顶部，行一进来就开始显形。
		float top = viewportTop(chipY, chipHeight);
		float height = contentTop(chipY, chipHeight) - top;

		if(height <= 1F) {
			return;
		}

		Color solid = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
		Color clear = new Color(base.getRed(), base.getGreen(), base.getBlue(), 0);

		nvg.drawVerticalGradientRect(x, top, width, height, solid, clear);
	}

	/**
	 * 通用「边缘渐隐带」：在给定矩形里把 base 色从实渐变到全透明。
	 *
	 * <p>为什么抽出来：一级页的 {@link #drawScrollFade} 是「视口上沿 -> 首行内容」
	 * 这一段固定几何，二级页的设置列表上下各有一条切口，几何和它完全不同，
	 * 但<b>取色必须完全一致</b>——都是「面板填充色满 alpha -> 同色 0 alpha」。
	 * 两处各写一份迟早调出两个不同的灰，叠在同一个面板上就读出两条色带。</p>
	 *
	 * <p>方向由 {@code top} 决定：</p>
	 * <ul>
	 *   <li>{@code top = true}：上实下虚。实端贴在视口上沿（= 裁剪线），
	 *       行往上滚被裁断的地方由此淡出；</li>
	 *   <li>{@code top = false}：下实上虚。实端贴在视口下沿，行往下滚
	 *       被裁断的地方由此淡出。</li>
	 * </ul>
	 *
	 * <p>它只负责糊身后的内容，所以必须画在内容之后、弹层之前——
	 * 颜色选择器这类浮层要压在这条带子上面。</p>
	 */
	public static void drawEdgeFade(NanoVGManager nvg, float x, float y, float width, float height,
			Color base, boolean top) {

		if(width <= 1F || height <= 1F) {
			return;
		}

		Color solid = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
		Color clear = new Color(base.getRed(), base.getGreen(), base.getBlue(), 0);

		// drawVerticalGradientRect 只能「从 y 渐到 y + height」。下实上虚就把两端
		// 对调，不要自己把矩形翻过来算一遍——那会让调用方分裂出两套坐标公式。
		nvg.drawVerticalGradientRect(x, y, width, height,
				top ? solid : clear, top ? clear : solid);
	}

	/** 重播入场动画（进菜单 / 切回该分类时调用）。 */
	public void reset() {
		startMs = System.currentTimeMillis();
		started = true;
	}

	/**
	 * 画小岛。必须在<b>列表之后、标签之前</b>调用：投影和岛面盖在卡片之上，
	 * 标签又盖在岛面之上，于是小岛读起来是一枚浮在列表头上的独立胶囊。
	 *
	 * @param nvg          NanoVG 管理器
	 * @param firstChipX   第一个标签的左边界
	 * @param chipY        标签行的上边界
	 * @param rowWidth     从第一个标签左边界到最后一个标签右边界的总宽
	 * @param chipHeight   标签高度
	 * @param maxRight     小岛右边界允许到达的最大 x，防止顶到计数文字或内容区外面
	 */
	public void draw(NanoVGManager nvg, float firstChipX, float chipY, float rowWidth, float chipHeight, float maxRight) {

		if(!started) {
			reset();
		}

		if(rowWidth <= 1F || chipHeight <= 1F) {
			return;
		}

		float x = firstChipX - PAD_X;
		float y = chipY - PAD_Y;
		float w = rowWidth + PAD_X * 2F;
		float h = chipHeight + PAD_Y * 2F;

		// 语言换成英文、或者标签变多时右边界可能顶出去，先收窄再画。
		if(maxRight > 0F && x + w > maxRight) {
			w = Math.max(h * 1.35F, maxRight - x);
		}

		if(w <= h * 1.2F) {
			return;
		}

		float radius = h / 2F;

		// 1) 投影：两层「逐层描边」的光雾，紧贴胶囊轮廓往外散。
		//
		//    之前这段整块被注释掉，理由是「没有面就没有边」——但被吐槽的「两条线」
		//    真正的原因不是「有面」，而是那版投影 spread 给到 16~20px、tint 只掺了
		//    primary 0.12，在一整幅同色的内容区上摊成了一条横贯左右的灰带。
		//    这里把 spread 收在胶囊高度以内、并给一个向下的偏移（光源在上），
		//    光雾就只围着胶囊自己，读起来是「浮起来」而不是「横线」。
		Color tint = shadowTint();
	
		drawSoftShadow(nvg, x, y + CONTACT_OFFSET_Y, w, h, radius,
				CONTACT_LAYERS, CONTACT_SPREAD, CONTACT_PEAK_ALPHA, tint);
		drawSoftShadow(nvg, x, y + DIFFUSE_OFFSET_Y, w, h, radius,
				DIFFUSE_LAYERS, DIFFUSE_SPREAD, DIFFUSE_PEAK_ALPHA, tint);
	
		// 2) 岛面：比面板亮一档的实色跑道形。
		//
		//    这里必须真的有一块面。胶囊背后现在是会滚动的列表，没有面就没有
		//    遮挡关系，标签和卡片会糊在一起——用户说的「胶囊的背景还是没有」
		//    指的就是这个。颜色刻意离开 surface 一档（surfaceContainerHigh），
		//    这样边界读作「一枚亮一点的胶囊」，而不是「同色矩形差几个灰度的边」。
		nvg.drawRoundedRect(x, y, w, h, radius, ModMenuTheme.surfaceContainerHigh());
	
		// 3) 静态轮廓：1px，第一帧就完整存在，不参与入场动画。
		nvg.drawOutlineRoundedRect(x, y, w, h, radius, 1F,
				ModMenuTheme.applyAlpha(ModMenuTheme.outlineVariant(), 190));
	
		// 4) 入场描绘：沿同一条轮廓补一圈高光，收口后常亮。
		buildGeometry(x, y, w, h);

		long now = System.currentTimeMillis();
		float raw = (float) (now - startMs) / (float) INTRO_MS;

		if(raw < 0F) {
			raw = 0F;
		}

		boolean done = raw >= 1F;
		float eased = done ? 1F : (float) (1.0D - Math.pow(1.0D - raw, INTRO_POWER));
		float fade = done ? 0.86F : (0.32F + 0.54F * eased);

		Color ringColor = ModMenuTheme.mix(ModMenuTheme.primary(), ModMenuTheme.onSurfaceVariant(), 0.30F);

		long ctx = nvg.getContext();

		// 注意：不能用 nvg.getColor(...) —— 那个实现按 getRGB() 做永久缓存，
		// 而这里的 alpha 每帧都在变，缓存会无上限增长。改用可复用的 NVGColor。
		//
		// 收口之后光圈要留下来。早先版本在 done 时把整圈描边一起关掉，理由是
		// 「浅色面板上一圈紫描边会被单独数成两条横线」——但真正造成「两条线」的是
		// 那时没有岛面，描边悬在同一色的内容区上，被眼睛读成了上下两条。现在岛面
		// 和静态轮廓都在，整圈描边只会读成「胶囊有一圈发光边」。所以：
		// 动画期间用较亮的 alpha 画一段弧，收口后降到 SETTLED_ALPHA 常亮整圈。
		if(!done) {
			setColor(strokeColor, ringColor, 150F * fade);
			strokePartialPath(ctx, eased, STROKE, strokeColor);
		}else {
			setColor(strokeColor, ringColor, SETTLED_ALPHA);
			strokePartialPath(ctx, 1F, STROKE, strokeColor);
		}

		// 5) 起笔处的小亮点，只属于入场：画完就收，常亮整圈不再需要它指路。
		if(!done) {
			setColor(strokeColor, ringColor, 210F * (1F - eased));
			NanoVG.nvgBeginPath(ctx);
			NanoVG.nvgCircle(ctx, x, y + radius, STROKE * 1.4F);
			NanoVG.nvgFillColor(ctx, strokeColor);
			NanoVG.nvgFill(ctx);
		}
	}

	/**
	 * 绕轮廓叠一层柔和投影。
	 *
	 * <p>做法和 {@code NanoVGManager#drawShadow} 同一族（逐层描边），但换了三件事：
	 * 颜色不再是纯黑、透明度从贴着轮廓往外单调衰减、步长远小于描边宽度，
	 * 所以相邻层互相盖住，叠出来的是一段连续的光雾，而不是一圈圈能数出来的环。</p>
	 *
	 * <p>描边始终贴着胶囊自己的跑道形轮廓走，因此阴影只出现在胶囊四周，
	 * 不会向外扩成一整条横贯内容区的阴影带。</p>
	 *
	 * @param offsetY 整层相对胶囊的垂直偏移，正值向下（光源在上）
	 * @param layers  层数
	 * @param spread  最外层相对胶囊向外扩出的距离
	 * @param peak    最内层的透明度（0-255），越往外越淡
	 */
	private void drawSoftShadow(NanoVGManager nvg, float x, float y, float w, float h, float radius,
			int layers, float spread, float peak, Color tint) {

		if(layers <= 0 || spread <= 0F || peak <= 0F) {
			return;
		}

		float step = spread / layers;
		float width = Math.max(1.1F, step * 1.7F);
		long ctx = nvg.getContext();

		for(int i = 1; i <= layers; i++) {

			float t = (float) i / (float) layers;
			float grow = spread * t;

			// 指数稍大于 1：靠近轮廓的一段保持住浓度，外缘掉得更快，
			// 看上去是「贴着胶囊散开」，而不是均匀的一层灰。
			float alpha = peak * (float) Math.pow(1F - t, 1.35D);

			if(alpha < 0.6F) {
				continue;
			}

			setColor(shadowColor, tint, alpha);

			NanoVG.nvgBeginPath(ctx);
			NanoVG.nvgRoundedRect(ctx, x - grow, y - grow, w + grow * 2F, h + grow * 2F, radius + grow);
			NanoVG.nvgStrokeWidth(ctx, width);
			NanoVG.nvgStrokeColor(ctx, shadowColor);
			NanoVG.nvgStroke(ctx);
		}
	}

	/**
	 * 阴影颜色：低饱和的浅紫和中性灰调和出来的冷墨色。
	 *
	 * <p>不用纯黑：纯黑压在紫白配色上会读成「脏」，而且越淡越显灰。
	 * 深色主题下沉到近黑再掺一点强调紫，浅色主题下从正文灰掺紫，
	 * 两种情况都不会变成发光边框。</p>
	 */
	private static Color shadowTint() {

		Color ink = ModMenuTheme.isDark()
				? new Color(6, 5, 14)
				: ModMenuTheme.mix(ModMenuTheme.onSurfaceVariant(), ModMenuTheme.surface(), 0.26F);

		return ModMenuTheme.mix(ink, ModMenuTheme.primary(), 0.12F);
	}

	private static void setColor(NVGColor out, Color rgb, float alpha) {

		float a = Math.max(0F, Math.min(255F, alpha));

		NanoVG.nvgRGBA((byte) rgb.getRed(), (byte) rgb.getGreen(), (byte) rgb.getBlue(), (byte) Math.round(a), out);
	}

	private void strokePartialPath(long ctx, float progress, float strokeWidth, NVGColor color) {

		if(progress <= 0.0005F) {
			return;
		}

		float target = totalLength * Math.min(1F, progress);

		NanoVG.nvgBeginPath(ctx);
		NanoVG.nvgMoveTo(ctx, sampleX[0], sampleY[0]);

		for(int i = 1; i <= SAMPLES; i++) {

			float travelled = totalLength * i / SAMPLES;

			if(travelled <= target) {
				NanoVG.nvgLineTo(ctx, sampleX[i], sampleY[i]);
			}else {
				float prev = totalLength * (i - 1) / SAMPLES;
				float span = Math.max(0.0001F, travelled - prev);
				float f = (target - prev) / span;

				NanoVG.nvgLineTo(ctx,
						sampleX[i - 1] + (sampleX[i] - sampleX[i - 1]) * f,
						sampleY[i - 1] + (sampleY[i] - sampleY[i - 1]) * f);
				break;
			}
		}

		NanoVG.nvgLineCap(ctx, NanoVG.NVG_ROUND);
		NanoVG.nvgLineJoin(ctx, NanoVG.NVG_ROUND);
		NanoVG.nvgStrokeWidth(ctx, strokeWidth);
		NanoVG.nvgStrokeColor(ctx, color);
		NanoVG.nvgStroke(ctx);
	}

	private void buildGeometry(float x, float y, float w, float h) {

		if(cachedX == x && cachedY == y && cachedW == w && cachedH == h && totalLength > 0F) {
			return;
		}

		cachedX = x;
		cachedY = y;
		cachedW = w;
		cachedH = h;

		float r = h / 2F;
		float leftX = x + r;
		float rightX = x + w - r;
		float midY = y + r;

		float straight = Math.max(0F, w - r * 2F);
		float quarter = (float) (Math.PI * r * 0.5D);
		float half = (float) (Math.PI * r);

		// 起点在左侧中点，顺时针：左上半弧 -> 上边直线 -> 右半弧 -> 下边直线 -> 左下半弧
		float[] pieceLength = new float[] { quarter, straight, half, straight, quarter };

		totalLength = 0F;

		for(int i = 0; i < pieceLength.length; i++) {
			totalLength += pieceLength[i];
		}

		if(totalLength <= 0F) {
			return;
		}

		for(int i = 0; i <= SAMPLES; i++) {

			float travelled = totalLength * i / SAMPLES;
			float acc = 0F;
			float px = x;
			float py = midY;

			for(int p = 0; p < pieceLength.length; p++) {

				boolean last = p == pieceLength.length - 1;

				if(travelled <= acc + pieceLength[p] || last) {

					float local = Math.min(pieceLength[p], Math.max(0F, travelled - acc));
					float f = pieceLength[p] <= 0F ? 0F : local / pieceLength[p];

					switch(p) {
						case 0:
							px = leftX + r * (float) Math.cos(Math.toRadians(180D + 90D * f));
							py = midY + r * (float) Math.sin(Math.toRadians(180D + 90D * f));
							break;
						case 1:
							px = leftX + straight * f;
							py = y;
							break;
						case 2:
							px = rightX + r * (float) Math.cos(Math.toRadians(270D + 180D * f));
							py = midY + r * (float) Math.sin(Math.toRadians(270D + 180D * f));
							break;
						case 3:
							px = rightX - straight * f;
							py = y + h;
							break;
						default:
							px = leftX + r * (float) Math.cos(Math.toRadians(90D + 90D * f));
							py = midY + r * (float) Math.sin(Math.toRadians(90D + 90D * f));
							break;
					}

					break;
				}

				acc += pieceLength[p];
			}

			sampleX[i] = px;
			sampleY[i] = py;
		}
	}
}