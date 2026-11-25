package mix.client.mixui.management.nanovg;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;

import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.nanovg.asset.AssetManager;
import mix.client.mixui.management.nanovg.font.Font;
import mix.client.mixui.management.nanovg.font.FontManager;
import mix.client.mixui.management.nanovg.font.IconInk;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.MathUtils;
import mix.client.mixui.utils.buffer.ScreenTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class NanoVGManager {

	private Minecraft mc = Minecraft.getMinecraft();
	
	private HashMap<Integer, NVGColor> colorCache = new HashMap<Integer, NVGColor>();

	private static final boolean LOG_TARGET = Boolean.getBoolean("mixclient.uicapture.target");
	private static int targetLogCount;
	
	private long nvg;
	
	private FontManager fontManager;
	private AssetManager assetManager;
	
	// 用来给绘制异常做去重，避免同一个异常每帧刷一条日志
	private String lastDrawError;
	private int drawErrorCount;
	
	public NanoVGManager() {
		
		nvg = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS);
		
		if(nvg == 0) {
			MixLogger.error("Failed to create NanoVG context");
			mc.shutdown();
		}
		
		fontManager = new FontManager();
		fontManager.init(nvg);
		
		assetManager = new AssetManager();
	}
    public void setupAndDraw(Runnable task, boolean scale) {

    	ScaledResolution sr = new ScaledResolution(mc);
    	
        
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glMatrixMode(matrixMode);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        /* 游戏没有主帧缓冲时（OptiFine Fast Render，或玩家把 fboEnable 关了）就
           直接画到窗口上。不这样的话，一旦之前有谁把某个 FBO 留在绑定位上，
           整套界面都会画进那张离屏纹理，窗口只剩一片空白 —— 玩家的任何设置
           都不该让客户端变成白屏。 */
        bindPresentedTarget();

        if (LOG_TARGET) {
            int currentTarget = ScreenTarget.capture();
            if (currentTarget != 0 && targetLogCount++ < 12) {
                MixLogger.info("[uicapture] NanoVG target=" + currentTarget);
            }
        }

        NanoVG.nvgBeginFrame(nvg, mc.displayWidth, mc.displayHeight, 1);
        
        if(scale) {
        	NanoVG.nvgScale(nvg, sr.getScaleFactor(), sr.getScaleFactor());
        }
        
        // 任何绘制任务抛异常都不能让 nvgEndFrame / glPopAttrib 被跳过，
        // 否则 NanoVG 帧状态和 GL 属性栈会永久错乱，之后整片 UI 都会画歪或直接崩。
        try {
        	task.run();
        } catch(Throwable t) {
        	reportDrawError(t);
        } finally {
        	GL11.glDisable(GL11.GL_ALPHA_TEST);
        	try {
        		NanoVG.nvgEndFrame(nvg);
        	} finally {
        		GL11.glPopAttrib();
        		GL11.glMatrixMode(GL11.GL_TEXTURE);
        		GL11.glPopMatrix();
        		GL11.glMatrixMode(GL11.GL_PROJECTION);
        		GL11.glPopMatrix();
        		GL11.glMatrixMode(GL11.GL_MODELVIEW);
        		GL11.glPopMatrix();
        		GL11.glMatrixMode(matrixMode);
        	}
        }
    }
    
    /** Giữ nguyên FBO tường minh mà ScreenAnimation/ScreenAlpha/ScreenStencil vừa bind. */
    private void bindPresentedTarget() {

        if (ScreenTarget.capture() != 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
    }

    // 同一个异常只打印一次，避免每帧刷屏把日志撑爆
    private void reportDrawError(Throwable t) {
    	
    	String key = t.getClass().getName() + ": " + t.getMessage();
    	
    	if(lastDrawError != null && lastDrawError.equals(key)) {
    		drawErrorCount++;
    		return;
    	}
    	
    	lastDrawError = key;
    	drawErrorCount = 1;
    	
    	MixLogger.error("NanoVG draw task failed: " + key);
    	
    	if(t instanceof Exception) {
    		MixLogger.error("NanoVG draw task stack", (Exception)t);
    	}else {
    		t.printStackTrace();
    	}
    }
    
    public void setupAndDraw(Runnable task) {
    	setupAndDraw(task, true);
    }
    
    public void drawAlphaBar(float x, float y, float width, float height, float radius, Color color) {
    	
        NVGPaint bg = NVGPaint.create();
        
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
        NVGColor nvgColor = getColor(color);
        NVGColor nvgColor2 = getColor(new Color(0, 0, 0, 0));
        NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x, y, x + width, y, nvgColor2, nvgColor, bg));
        NanoVG.nvgFill(nvg);
    }
    
    public void drawHSBBox(float x, float y, float width, float height, float radius, Color color) {
    	
        drawRoundedRect(x, y, width, height, radius, color);

        NVGPaint bg = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
        NVGColor nvgColor = getColor(Color.WHITE);
        NVGColor nvgColor2 = getColor(new Color(0, 0, 0, 0));
        NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x + 8, y + 8, x + width, y, nvgColor, nvgColor2, bg));
        NanoVG.nvgFill(nvg);

        NVGPaint bg2 = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
        NVGColor nvgColor3 = getColor(new Color(0, 0, 0, 0));
        NVGColor nvgColor4 = getColor(Color.BLACK);
        
        NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x + 8, y + 8, x, y + height, nvgColor3, nvgColor4, bg2));
        NanoVG.nvgFill(nvg);
    }
    
	public void drawRect(float x, float y, float width, float height, Color color) {
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRect(nvg, x, y, width, height);
		
		NVGColor nvgColor = getColor(color);
		
		NanoVG.nvgFillColor(nvg, nvgColor);
		NanoVG.nvgFill(nvg);
	}
	
	public void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
		
		NVGColor nvgColor = getColor(color);
		
		NanoVG.nvgFillColor(nvg, nvgColor);
		NanoVG.nvgFill(nvg);
	}
	
	public void drawRoundedRectVarying(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, Color color) {
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRectVarying(nvg, x, y, width, height, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius);
		
		NVGColor nvgColor = getColor(color);
		
		NanoVG.nvgFillColor(nvg, nvgColor);
		NanoVG.nvgFill(nvg);
	}
	
	public void drawGradientRect(float x, float y, float width, float height, Color color1, Color color2) {
		
		NVGPaint bg = NVGPaint.create();
		
		float tick = ((System.currentTimeMillis() % 3600) / 570F);
		float max = Math.max(width, height);
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRect(nvg, x, y, width, height);
		
		NVGColor nvgColor1 = getColor(color1);
		NVGColor nvgColor2 = getColor(color2);
		
		NanoVG.nvgFillColor(nvg, nvgColor1);
		NanoVG.nvgFillColor(nvg, nvgColor2);
		
		NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x + width / 2 - (max / 2) * MathUtils.cos(tick), y + height / 2 - (max / 2) * MathUtils.sin(tick), x + width / 2 + (max / 2) * MathUtils.cos(tick), y + height / 2 + (max + 2f) * MathUtils.sin(tick), nvgColor1, nvgColor2, bg));
		NanoVG.nvgFill(nvg);
	}
	
	/**
	 * 真正的竖直渐变（从 top 色过渡到 bottom 色），不随 tick 旋转。
	 *
	 * <p>和 {@link #drawGradientRect} 的区别：那个的渐变方向每帧都在转，只适合做
	 * 强调色块；做「内容渐隐 / 顶部柔和过渡」必须方向固定，否则整块会闪烁。</p>
	 */
	public void drawVerticalGradientRect(float x, float y, float width, float height, Color colorTop, Color colorBottom) {

		NVGPaint paint = NVGPaint.create();

		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRect(nvg, x, y, width, height);
		NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x, y, x, y + height,
				getColor(colorTop), getColor(colorBottom), paint));
		NanoVG.nvgFill(nvg);
	}

	/**
	 * 竖直渐变 + 四角独立圆角。用来做「面板顶部渐隐」：只让贴着面板外缘的那个角
	 * 跟着面板圆角收边，靠内侧的角保持直角，避免在面板内部露出多余的圆角弧。
	 */
	public void drawVerticalGradientRoundedRectVarying(float x, float y, float width, float height,
			float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius,
			Color colorTop, Color colorBottom) {

		NVGPaint paint = NVGPaint.create();

		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRectVarying(nvg, x, y, width, height, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius);
		NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x, y, x, y + height,
				getColor(colorTop), getColor(colorBottom), paint));
		NanoVG.nvgFill(nvg);
	}

	/** 与 {@link #drawVerticalGradientRect} 相同，但按圆角矩形裁形——做玻璃高光、内阴影用。 */
	public void drawVerticalGradientRoundedRect(float x, float y, float width, float height, float radius, Color colorTop, Color colorBottom) {

		NVGPaint paint = NVGPaint.create();

		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
		NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x, y, x, y + height,
				getColor(colorTop), getColor(colorBottom), paint));
		NanoVG.nvgFill(nvg);
	}

	public void drawGradientRoundedRect(float x, float y, float width, float height, float radius, Color color1, Color color2) {
		
		NVGPaint bg = NVGPaint.create();
		
		float tick = ((System.currentTimeMillis() % 3600) / 570F);
		float max = Math.max(width, height);
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
		
		NVGColor nvgColor1 = getColor(color1);
		NVGColor nvgColor2 = getColor(color2);
		
		NanoVG.nvgFillColor(nvg, nvgColor1);
		NanoVG.nvgFillColor(nvg, nvgColor2);
		
		NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x + width / 2 - (max / 2) * MathUtils.cos(tick), y + height / 2 - (max / 2) * MathUtils.sin(tick), x + width / 2 + (max / 2) * MathUtils.cos(tick), y + height / 2 + (max + 2f) * MathUtils.sin(tick), nvgColor1, nvgColor2, bg));
		NanoVG.nvgFill(nvg);
	}
	
	public void drawGradientRoundedRectVarying(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, Color color1, Color color2) {
		
		NVGPaint bg = NVGPaint.create();
		
		float tick = ((System.currentTimeMillis() % 3600) / 570F);
		float max = Math.max(width, height);
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRectVarying(nvg, x, y, width, height, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius);
		
		NVGColor nvgColor1 = getColor(color1);
		NVGColor nvgColor2 = getColor(color2);
		
		NanoVG.nvgFillColor(nvg, nvgColor1);
		NanoVG.nvgFillColor(nvg, nvgColor2);
		
		NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x + width / 2 - (max / 2) * MathUtils.cos(tick), y + height / 2 - (max / 2) * MathUtils.sin(tick), x + width / 2 + (max / 2) * MathUtils.cos(tick), y + height / 2 + (max + 2f) * MathUtils.sin(tick), nvgColor1, nvgColor2, bg));
		NanoVG.nvgFill(nvg);
	}
	
	public void drawOutlineRoundedRect(float x, float y, float width, float height, float radius, float strokeWidth, Color color) {
		
		NVGColor nvgColor = getColor(color);
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
		NanoVG.nvgStrokeWidth(nvg, strokeWidth);
		NanoVG.nvgStrokeColor(nvg, nvgColor);
		NanoVG.nvgStroke(nvg);
	}
	
	public void drawGradientOutlineRoundedRect(float x, float y, float width, float height, float radius, float strokeWidth, Color color1, Color color2) {
		
		NVGPaint bg = NVGPaint.create();
		
		float tick = ((System.currentTimeMillis() % 3600) / 570F);
		float max = Math.max(width, height);
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
		
		NVGColor nvgColor1 = getColor(color1);
		NVGColor nvgColor2 = getColor(color2);
		
		NanoVG.nvgFillColor(nvg, nvgColor1);
		NanoVG.nvgFillColor(nvg, nvgColor2);
		
		NanoVG.nvgStrokeWidth(nvg, strokeWidth);
		NanoVG.nvgStrokePaint(nvg, NanoVG.nvgLinearGradient(nvg, x + width / 2 - (max / 2) * MathUtils.cos(tick), y + height / 2 - (max / 2) * MathUtils.sin(tick), x + width / 2 + (max / 2) * MathUtils.cos(tick), y + height / 2 + (max + 2f) * MathUtils.sin(tick), nvgColor1, nvgColor2, bg));
		NanoVG.nvgStroke(nvg);
	}
	
	public void drawArrow(float x, float y, float size, float angle, Color color) {

	    save();
	    
	    NanoVG.nvgBeginPath(nvg);
	    
	    float offsetX = (float) (size * Math.cos(Math.toRadians(angle)));
	    float offsetY = (float) (size * Math.sin(Math.toRadians(angle)));
	    
	    float diffX = x + (offsetX / 2);
	    float diffY = y + (offsetY / 2);
	    
	    NanoVG.nvgTranslate(nvg, diffX, diffY);
	    NanoVG.nvgRotate(nvg, (float) Math.toRadians(angle));
	    
	    NanoVG.nvgMoveTo(nvg, -size, -size / 2);
	    NanoVG.nvgLineTo(nvg, 0, 0);
	    NanoVG.nvgLineTo(nvg, -size, size / 2);
	    
	    NanoVG.nvgStrokeWidth(nvg, 0.8F);
	    NanoVG.nvgStrokeColor(nvg, getColor(color));
	    NanoVG.nvgStroke(nvg);
	    
	    restore();
	}

	
	public void drawShadow(float x, float y, float width, float height, float radius, int strength) {
		
		int alpha = 1;
		
		for(float f = strength; f > 0; f--) {
			drawOutlineRoundedRect(x - (f / 2), y - (f / 2), width + f, height + f, radius + 2, f, new Color(0, 0, 0, alpha));

			alpha+=2;
		}
	}
	
	public void drawShadow(float x, float y, float width, float height, float radius) {
		drawShadow(x, y, width, height, radius, 7);
	}
	
	public void drawGradientShadow(float x, float y, float width, float height, float radius, Color color1, Color color2) {
		
		int alpha = 1;
		
		for(float f = 10; f > 0; f--) {
			drawGradientOutlineRoundedRect(x - (f / 2), y - (f / 2), width + f, height + f, radius + 2, f, ColorUtils.applyAlpha(color1, alpha), ColorUtils.applyAlpha(color2, alpha));

			alpha+=3;
		}
	}
	
	public void drawCircle(float x, float y, float radius, Color color) {
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgCircle(nvg, x, y, radius);
		
		NVGColor nvgColor = getColor(color);
		
		NanoVG.nvgFillColor(nvg, nvgColor);
		NanoVG.nvgFill(nvg);
	}
	
	public void drawArc(float x, float y, float radius, float startAngle, float endAngle, float strokeWidth, Color color) {
		
		NVGColor nvgColor = getColor(color);
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgArc(nvg, x, y, radius, (float) Math.toRadians(startAngle), (float) Math.toRadians(endAngle), NanoVG.NVG_CW);
		NanoVG.nvgStrokeWidth(nvg, strokeWidth);
		NanoVG.nvgStrokeColor(nvg, nvgColor);
		NanoVG.nvgStroke(nvg);
	}
	
	public void drawGradientCircle(float x, float y, float radius, Color color1, Color color2) {
		
		NVGPaint bg = NVGPaint.create();
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgCircle(nvg, x, y, radius);
		
		NVGColor nvgColor1 = getColor(color1);
		NVGColor nvgColor2 = getColor(color2);
		
		NanoVG.nvgFillColor(nvg, nvgColor1);
		NanoVG.nvgFillColor(nvg, nvgColor2);
		
		NanoVG.nvgFillPaint(nvg, NanoVG.nvgLinearGradient(nvg, x, y, radius, radius, nvgColor1, nvgColor2, bg));
		NanoVG.nvgFill(nvg);
	}
	
	public void drawText(String text, float x, float y, Color color, float size, Font font) {

		y+=size / 2;
		
		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgFontSize(nvg, size);
		NanoVG.nvgFontFace(nvg, font.getName());
		NanoVG.nvgTextAlign(nvg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
		
		NVGColor nvgColor = getColor(color);
		
		NanoVG.nvgFillColor(nvg, nvgColor);
		NanoVG.nvgText(nvg, x, y, text);
	}
	
	/**
	 * 按<b>字形包围盒</b>垂直居中，而不是按字体的 ascender/descender 居中。
	 *
	 * <p>图标字体和文本字体的可用高度完全不同，用 {@link #drawText} 的
	 * {@code size / 2} 约定摆放时，同一个 y 会让图标和汉字一个偏高一个偏低。
	 * 这里先用同样的对齐方式量出字形盒，再把盒中心对到 {@code centerY}，
	 * 两者就能真正落在同一条中线上。</p>
	 */
	public void drawTextMid(String text, float x, float centerY, Color color, float size, Font font) {

		// 图标字体：走编译期查表。本方法约定 x 是「墨迹左沿」，而查表给出的是
		// 「墨迹中心」的 pen，所以先把锚点补上半个墨迹宽（图标都是单字符、
		// advanceWidth 恰好 1 em，半宽就是 size / 2）再反解 pen。
		if (IconInk.isIconFont(font.getName())) {
			float[] leftPen = IconInk.penFor(text, font.getName(), x + (size / 2F), centerY, size);
			drawTextPen(text, leftPen[0], leftPen[1], color, size, font);
			return;
		}

		float[] bounds = measureInk(text, size, font);

		// x 是墨迹左沿，centerY 是墨迹盒的垂直中心。原来的实现把 pen 直接摆在
		// x 上（于是墨迹左沿被字形的左侧 bearing 推右了 bounds[0] 那么多），
		// 垂直方向又叠加了一次 NVG_ALIGN_MIDDLE 的行高偏移（(ascender+descender)/2），
		// 图标字体在这种双偏差下会稳定地偏右下 —— 所以这里先量盒子再反解 pen 坐标。
		drawTextPen(text, x - bounds[0], centerY - ((bounds[1] + bounds[3]) / 2F), color, size, font);
	}

	/**
	 * 水平 + 垂直都按<b>墨迹包围盒</b>居中的版本，且完全绕开字体行高。
	 *
	 * <p>垂直方向不再使用 {@code NVG_ALIGN_MIDDLE}：那个对齐量是按字体
	 * ascender/descender 的行高算的，而 Material Symbols 这类图标字形的墨迹在
	 * em 盒里明显偏上，按行高居中会把图标顶到方块左上角去。这里统一切到
	 * {@code NVG_ALIGN_LEFT}（无垂直对齐位，pen 的 y 就是基线），度量与绘制
	 * 共用同一套字体/字号/对齐/原点，于是 bounds 与墨迹是同一坐标系，
	 * 相加就能精确对上，bearing 与行高两类偏差同时归零。</p>
	 */
	public void drawCenteredTextMid(String text, float centerX, float centerY, Color color, float size, Font font) {

		// 图标字体走编译期查表：nvgTextBounds 在图标字体上返回的是 stb_truetype 的
		// 位图盒近似值，每个字形的误差都不一样（Material Symbols 里 3396 个图标有
		// 1488 个偏离墨迹盒中心超过 0.004 em），逐个反解 pen 坐标反而更歪。
		// IconInk 里的值是从 TTF 的 glyf 表直接读出来的真实墨迹中心，一次乘法定位。
		float[] iconPen = IconInk.penFor(text, font.getName(), centerX, centerY, size);

		if (iconPen != null) {
			drawTextPen(text, iconPen[0], iconPen[1], color, size, font);
			return;
		}

		float[] bounds = measureInk(text, size, font);

		drawTextPen(text, centerX - ((bounds[0] + bounds[2]) / 2F),
				centerY - ((bounds[1] + bounds[3]) / 2F), color, size, font);
	}

	/** 量出字形墨迹包围盒（[minX, minY, maxX, maxY]，相对基线 pen 原点），单位与 size 一致。 */
	private float[] measureInk(String text, float size, Font font) {

		float[] bounds = new float[4];

		NanoVG.nvgFontSize(nvg, size);
		NanoVG.nvgFontFace(nvg, font.getName());
		NanoVG.nvgTextAlign(nvg, NanoVG.NVG_ALIGN_LEFT);
		NanoVG.nvgTextBounds(nvg, 0, 0, text, bounds);

		return bounds;
	}

	/** 以裸 pen 坐标画字：penX/penY 就是 ALIGN_LEFT 下的起点（penY 是基线）。 */
	private void drawTextPen(String text, float penX, float penY, Color color, float size, Font font) {

		NanoVG.nvgBeginPath(nvg);
		NanoVG.nvgFontSize(nvg, size);
		NanoVG.nvgFontFace(nvg, font.getName());
		NanoVG.nvgTextAlign(nvg, NanoVG.NVG_ALIGN_LEFT);
		NanoVG.nvgFillColor(nvg, getColor(color));

		NanoVG.nvgText(nvg, penX, penY, text);
	}

	public void drawCenteredText(String text, float x, float y, Color color, float size, Font font) {
		
		int textWidth = (int) getTextWidth(text, size, font);
		
		drawText(text, x - (textWidth >> 1), y, color, size, font);
	}
	
	public float getTextWidth(String text, float size, Font font) {
		
	    float[] bounds = new float[4];
	    
	    NanoVG.nvgFontSize(nvg, size);
	    NanoVG.nvgFontFace(nvg, font.getName());
	    NanoVG.nvgTextBounds(nvg, 0, 0, text, bounds);
	    NanoVG.nvgTextAlign(nvg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
	    
	    return bounds[2] - bounds[0];
	}
	
	public float getTextHeight(String text, float size, Font font) {
		
	    float[] bounds = new float[4];
	    
	    NanoVG.nvgFontSize(nvg, size);
	    NanoVG.nvgFontFace(nvg, font.getName());
	    NanoVG.nvgTextBounds(nvg, 0, 0, text, bounds);

	    return bounds[3] - bounds[1];
	}
	
	public void scale(float x, float y, float scale) {
		NanoVG.nvgTranslate(nvg, x, y);
		NanoVG.nvgScale(nvg, scale, scale);
		NanoVG.nvgTranslate(nvg, -x, -y);
	}
	
	public void scale(float x, float y, float width, float height, float scale) {
		NanoVG.nvgTranslate(nvg, (x + (x + width)) / 2, (y + (y + height)) / 2);
		NanoVG.nvgScale(nvg, scale, scale);
		NanoVG.nvgTranslate(nvg, -(x + (x + width)) / 2, -(y + (y + height)) / 2);
	}
	
	public void rotate(float x, float y, float width, float height, float angle) {
		NanoVG.nvgTranslate(nvg, (x + (x + width)) / 2, (y + (y + height)) / 2);
		NanoVG.nvgRotate(nvg, angle);
		NanoVG.nvgTranslate(nvg, -(x + (x + width)) / 2, -(y + (y + height)) / 2);
	}
	
	public void translate(float x, float y) {
		NanoVG.nvgTranslate(nvg, x, y);
	}
	
	public void setAlpha(float alpha) {
		NanoVG.nvgGlobalAlpha(nvg, alpha);
	}
	
	public void scissor(float x, float y, float width, float height) {
		NanoVG.nvgScissor(nvg, x, y, width, height);
	}
	
	public void drawSvg(ResourceLocation location, float x, float y, float width, float height, Color color) {
		
        if (assetManager.loadSvg(nvg, location, width, height)) {
        	
            NVGPaint imagePaint = NVGPaint.calloc();
            
            int image = assetManager.getSvg(location, width, height);
            
            NanoVG.nvgBeginPath(nvg);
            NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, 1, imagePaint);
            
            imagePaint.innerColor(getColor(color));
            imagePaint.outerColor(getColor(color));
            
            NanoVG.nvgRect(nvg, x, y, width, height);
            NanoVG.nvgFillPaint(nvg, imagePaint);
            NanoVG.nvgFill(nvg);            

            imagePaint.free();
        }
	}
	
	public void drawImage(ResourceLocation location, float x, float y, float width, float height) {
		
		if(assetManager.loadImage(nvg, location)) {
			
			NVGPaint imagePaint = NVGPaint.calloc();
			
			int image = assetManager.getImage(location);
			
			NanoVG.nvgBeginPath(nvg);
			NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, 1, imagePaint);
			
			NanoVG.nvgRect(nvg, x, y, width, height);
			NanoVG.nvgFillPaint(nvg, imagePaint);
			NanoVG.nvgFill(nvg);
			
			imagePaint.free();
		}
	}
	
	public void drawImage(File file, float x, float y, float width, float height) {
		
		if(assetManager.loadImage(nvg, file)) {
			
			NVGPaint imagePaint = NVGPaint.calloc();
			
			int image = assetManager.getImage(file);
			
			NanoVG.nvgBeginPath(nvg);
			NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, 1, imagePaint);
			
			NanoVG.nvgRect(nvg, x, y, width, height);
			NanoVG.nvgFillPaint(nvg, imagePaint);
			NanoVG.nvgFill(nvg);
			
			imagePaint.free();
		}
	}

	public void drawImage(int texture, float x, float y, float width, float height, float alpha) {
		
		if(assetManager.loadImage(nvg, texture, width, height)) {
			
			int image = assetManager.getImage(texture);
			
			NanoVG.nvgImageSize(nvg, image, new int[]{(int) width}, new int[]{-(int) height});
	        NVGPaint p = NVGPaint.calloc();
	        
	        NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, alpha, p);
	        NanoVG.nvgBeginPath(nvg);
	        NanoVG.nvgRect(nvg, x, y, width, height);
	        NanoVG.nvgFillPaint(nvg, p);
	        NanoVG.nvgFill(nvg);
	        NanoVG.nvgClosePath(nvg);
	        
	        p.free();
		}
	}
	
	public void drawImage(int texture, float x, float y, float width, float height) {
		drawImage(texture, x, y, width, height, 1.0F);
	}
	
	public void drawRoundedImage(int texture, float x, float y, float width, float height, float radius, float alpha) {
		
		if(assetManager.loadImage(nvg, texture, width, height)) {
			
			int image = assetManager.getImage(texture);
			
			NanoVG.nvgImageSize(nvg, image, new int[]{(int) width}, new int[]{-(int) height});
	        NVGPaint p = NVGPaint.calloc();
	        
	        NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, alpha, p);
	        NanoVG.nvgBeginPath(nvg);
	        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
	        NanoVG.nvgFillPaint(nvg, p);
	        NanoVG.nvgFill(nvg);
	        NanoVG.nvgClosePath(nvg);
	        
	        p.free();
		}
	}
	
	public void drawRoundedImage(int texture, float x, float y, float width, float height, float radius) {
		drawRoundedImage(texture, x, y, width, height, radius, 1.0F);
	}
	
	public void drawPlayerHead(ResourceLocation location, float x, float y, float width, float height, float radius, float alpha) {
		
		if(location == null || mc.getTextureManager().getTexture(location) == null) {
			return;
		}
		
		int texture = mc.getTextureManager().getTexture(location).getGlTextureId();
		
		if(assetManager.loadImage(nvg, texture, width, height)) {
			
			int image = assetManager.getImage(texture);
			
			NanoVG.nvgImageSize(nvg, image, new int[]{(int) width}, new int[]{-(int) height});
	        NVGPaint p = NVGPaint.calloc();
	        
	        float sizeMultiplier = 8;
	        
	        NanoVG.nvgImagePattern(nvg, x - width / 4 * sizeMultiplier / 2, y - height / 4 * sizeMultiplier / 2, width * sizeMultiplier, height * sizeMultiplier, 0, image, alpha, p);
	        NanoVG.nvgBeginPath(nvg);
	        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
	        NanoVG.nvgFillPaint(nvg, p);
	        NanoVG.nvgFill(nvg);
	        NanoVG.nvgClosePath(nvg);
	        
	        NanoVG.nvgImagePattern(nvg, x - width * 3.25F * sizeMultiplier / 2, y - height / 4 * sizeMultiplier / 2, width * sizeMultiplier, height * sizeMultiplier, 0, image, alpha, p);
	        NanoVG.nvgBeginPath(nvg);
	        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
	        NanoVG.nvgFillPaint(nvg, p);
	        NanoVG.nvgFill(nvg);
	        NanoVG.nvgClosePath(nvg);
	        
	        p.free();
		}
	}
	
	public void drawPlayerHead(ResourceLocation location, float x, float y, float width, float height, float radius) {
		drawPlayerHead(location, x, y, width, height, radius, 1.0F);
	}
	
	/**
	 * 画一张已经注册在 TextureManager 里的「整张」动态纹理（封面、头像等）。
	 *
	 * <p>为什么不能直接用 drawRoundedImage(ResourceLocation,...)：那条路走
	 * AssetManager.loadImage(nvg, location)，底层是 IOUtils.resourceToByteBuffer，只认资源包里的文件。
	 * Dango 的封面是运行期用 DynamicTexture 上传到 TextureManager 的动态纹理，资源包里
	 * 根本没有对应文件，所以走那条路永远拿不到图像。这里和 drawPlayerHead 一样，
	 * 直接取 getGlTextureId() 交给 AssetManager.loadImage(nvg, int texture, ...)。
	 *
	 * <p>与 drawPlayerHead 的差别只有一个：不套 skin 的 8 倍图集偏移，
	 * 整张纹理铺满目标矩形。</p>
	 */
	public void drawRoundedTexture(ResourceLocation location, float x, float y, float width, float height, float radius, float alpha) {
		
		if(location == null || mc.getTextureManager().getTexture(location) == null) {
			return;
		}
		
		int texture = mc.getTextureManager().getTexture(location).getGlTextureId();
		
		if(texture <= 0) {
			return;
		}
		
		if(assetManager.loadImage(nvg, texture, width, height)) {
			
			int image = assetManager.getImage(texture);
			
			NanoVG.nvgImageSize(nvg, image, new int[]{(int) width}, new int[]{-(int) height});
			NVGPaint p = NVGPaint.calloc();
			
			NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, alpha, p);
			NanoVG.nvgBeginPath(nvg);
			NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
			NanoVG.nvgFillPaint(nvg, p);
			NanoVG.nvgFill(nvg);
			NanoVG.nvgClosePath(nvg);
			
			p.free();
		}
	}
	
	public void drawRoundedTexture(ResourceLocation location, float x, float y, float width, float height, float radius) {
		drawRoundedTexture(location, x, y, width, height, radius, 1.0F);
	}
	
	public void drawRoundedImage(ResourceLocation location, float x, float y, float width, float height, float radius, float alpha) {
		
		if(assetManager.loadImage(nvg, location)) {
			
			NVGPaint imagePaint = NVGPaint.calloc();
			
			int image = assetManager.getImage(location);
			
			NanoVG.nvgBeginPath(nvg);
			NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, alpha, imagePaint);
			
			NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
			NanoVG.nvgFillPaint(nvg, imagePaint);
			NanoVG.nvgFill(nvg);
			
			imagePaint.free();
		}
	}
	
	public void drawRoundedImage(ResourceLocation location, float x, float y, float width, float height, float radius) {
		drawRoundedImage(location, x, y, width, height, radius, 1.0F);
	}
	
	public void drawRoundedImage(File file, float x, float y, float width, float height, float radius, float alpha) {
		
		if(assetManager.loadImage(nvg, file)) {
			
			NVGPaint imagePaint = NVGPaint.calloc();
			
			int image = assetManager.getImage(file);
			
			NanoVG.nvgBeginPath(nvg);
			NanoVG.nvgImagePattern(nvg, x, y, width, height, 0, image, alpha, imagePaint);
			
			NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
			NanoVG.nvgFillPaint(nvg, imagePaint);
			NanoVG.nvgFill(nvg);
			
			imagePaint.free();
		}
	}
	
	public void drawRoundedImage(File file, float x, float y, float width, float height, float radius) {
		drawRoundedImage(file, x, y, width, height, radius, 1.0F);
	}
	
	public void loadImage(File file) {
		assetManager.loadImage(nvg, file);
	}
	
	public void loadImage(ResourceLocation location) {
		assetManager.loadImage(nvg, location);
	}
	
	public AssetManager getAssetManager() {
		return assetManager;
	}

	public void save() {
		NanoVG.nvgSave(nvg);
	}
	
	public void restore() {
		NanoVG.nvgRestore(nvg);
	}
	
	public NVGColor getColor(Color color) {
		
		if(color == null) {
			color = Color.RED;
		}
		
		if(colorCache.containsKey(color.getRGB())) {
			return colorCache.get(color.getRGB());
		}
		
		NVGColor nvgColor = NVGColor.create();
		
		NanoVG.nvgRGBA((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha(), nvgColor);
		
		colorCache.put(color.getRGB(), nvgColor);
		
		return nvgColor;
	}
	
	public String getLimitText(String inputText, float fontSize, Font font, float width) {
		
		String text = inputText;
		boolean isInRange = false;
		boolean isRemoved = false;
		
		while(!isInRange) {
			
			if(getTextWidth(text, fontSize, font) > width) {
				text = text.substring(0, text.length() - 1);
				isRemoved = true;
			} else {
				isInRange = true;
			}
		}
		
		return text + (isRemoved ? "..." : "");
	}
	
	public long getContext() {
		return nvg;
	}
}
