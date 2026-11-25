package mix.client.mixui.management.mods;

import java.awt.Color;
import java.io.File;

import mix.client.mixui.Mix;
import mix.client.mixui.gui.GuiEditHUD;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.color.ColorManager;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.impl.GlobalSettingsMod;
import mix.client.mixui.management.mods.settings.impl.ComboSetting;
import mix.client.mixui.management.mods.settings.impl.combo.Option;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Font;
import mix.client.mixui.utils.ColorUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class HUDMod extends Mod {

    public static final float HUD_FONT_SIZE = 9.0F;
    public static final float HUD_ICON_SIZE = 10.4F;
    public static final float HUD_HEIGHT = 18.0F;
    public static final float HUD_PADDING_X = 5.5F;
    public static final float HUD_PADDING_Y = 5.5F;
    public static final float HUD_ICON_GAP = 4.0F;

	private int x, y, draggingX, draggingY, width, height;
	private float scale;
	private boolean dragging, draggable;

	/**
	 * 位置是否已经有「真实来源」（用户拖过，或者从 profile 里读回来了）。
	 *
	 * <p>构造时 {@code x/y = 100} 是直接赋字段、不走 setter，所以初始为 false。
	 * {@link #setX(int)} / {@link #setY(int)} 一被调用就置 true —— 这两条路正好覆盖
	 * 「GuiEditHUD 拖动」与「ProfileManager.load 读回坐标」两种合法来源。</p>
	 *
	 * <p>存在的意义：{@code DangoIslandMod} / {@code DangoSongCardMod} 原本各自用一个
	 * 私有的 {@code placed} 布尔，在第一次 draw 时把坐标强行改成「顶部居中 / 右下角」。
	 * 那个布尔只看「画过没有」，不看「profile 里有没有坐标」，于是每次重进游戏都会
	 * 把已经保存的位置覆盖成默认值 —— 用户拖完退出重进就回到原位。改成问这里之后，
	 * 只有真的没有来源的第一次才落默认位置。</p>
	 */
	private boolean positionSettled;

	/**
	 * 尺寸变化时要重新夹紧的锚点。
	 *
	 * <p>计分板这类 HUD 的宽高是每帧根据内容现算的（换服务器后条目多少/字串长短都不一样），
	 * 而 x/y 只在拖动那一瞬间被按「当时的宽度」夹过一次。于是用户把小计分板拖到右边缘后，
	 * 去了另一个条目更长的服务器，宽度变大而 x 没动，右边缘就被推出屏幕。
	 * 这里记下「拖完时距离哪个边近」，尺寸变了就按锚点重算坐标。</p>
	 */
	/** 距离屏幕边超过这个值就不算「贴边」，尺寸变了也不重算。 */
	private static final int ANCHOR_SNAP = 6;

	private boolean anchorRight, anchorBottom;

	/** 锚定时距离对应边的像素距离（右/下）。 */
	private int anchorGapX, anchorGapY;
	
	public HUDMod(TranslateText nameTranslate, TranslateText descriptionText) {
		super(nameTranslate, descriptionText, ModCategory.HUD);
		
		this.x = 100;
		this.y = 100;
		this.width = 100;
		this.height = 100;
		this.scale = 1.0F;
		this.draggable = true;
	}
	
	public void save() {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.save();
	}
	
	public void restore() {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.restore();
	}
	
	public void scissor(float addX, float addY, float width, float height) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.scissor(x + (addX * scale), y + (addY * scale), width * scale, height * scale);
	}
	
	public void drawPlayerHead(ResourceLocation location, float addX, float addY, float width, float height, float radius) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawPlayerHead(location, x + (addX * scale), y + (addY * scale), width * scale, height * scale, radius * scale);
	}
	
	public void drawRoundedImage(int texture, float addX, float addY, float width, float height, float radius) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawRoundedImage(texture, x + (addX * scale), y + (addY * scale), width * scale, height * scale, radius * scale);
	}
	
	public void drawRoundedImage(File file, float addX, float addY, float width, float height, float radius, float alpha) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawRoundedImage(file, x + (addX * scale), y + (addY * scale), width * scale, height * scale, radius * scale, alpha);
	}
	
	public void drawRoundedImage(File file, float addX, float addY, float width, float height, float radius) {
		drawRoundedImage(file, addX, addY, width, height, radius, 1.0F);
	}
	
	public void drawRoundedImage(ResourceLocation location, float addX, float addY, float width, float height, float radius) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawRoundedImage(location, x + (addX * scale), y + (addY * scale), width * scale, height * scale, radius * scale);
	}
	
	public void drawRoundedTexture(ResourceLocation location, float addX, float addY, float width, float height, float radius) {
		drawRoundedTexture(location, addX, addY, width, height, radius, 1.0F);
	}
	
	public void drawRoundedTexture(ResourceLocation location, float addX, float addY, float width, float height, float radius, float alpha) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawRoundedTexture(location, x + (addX * scale), y + (addY * scale), width * scale, height * scale, radius * scale, alpha);
	}
	
	public void drawArc(float addX, float addY, float radius, float startAngle, float endAngle, float strokeWidth, Color color) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawArc(x + (addX * scale), y + (addY * scale), radius * scale, startAngle, endAngle, strokeWidth * scale, color);
	}
	
	public void drawArc(float addX, float addY, float radius, float startAngle, float endAngle, float strokeWidth) {
		drawArc(addX, addY, radius, startAngle, endAngle, strokeWidth, this.getFontColor());
	}
	
	public void drawShadow(float addX, float addY, float width, float height, float radius) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawShadow(x + (addX * scale), y + (addY * scale), width * scale, height * scale, radius * scale);
	}
	
	public void drawRect(float addX, float addY, float width, float height, Color color) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		nvg.drawRect(x + (addX * scale), y + (addY * scale), width * scale, height * scale, color);
	}
	
	public void drawRect(float addX, float addY, float width, float height) {
		drawRect(addX, addY, width, height, this.getFontColor());
	}
	
	public void drawRoundedRect(float addX, float addY, float width, float height, float radius, Color color) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		
		if(width < 0) {
			return;
		}
		
		if(height < 0) {
			return;
		}
		
		nvg.drawRoundedRect(x + (addX * scale), y + (addY * scale), width * scale, height * scale, radius * scale, color);
	}
	
	public void drawRoundedRect(float addX, float addY, float width, float height, float radius) {
		drawRoundedRect(addX, addY, width, height, radius, this.getFontColor());
	}
	
	public void drawBackground(float addX, float addY, float width, float height, float radius) {
		drawBackground(addX, addY, width, height, radius, 1.0F);
	}

	public void drawBackground(float addX, float addY, float width, float height, float radius, float alpha) {
		
		Mix instance = Mix.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		AccentColor currentColor = colorManager.getCurrentColor();
		ComboSetting setting = GlobalSettingsMod.getInstance().getModThemeSetting();
		Option theme = setting.getOption();
		
		boolean isNormal = theme.getTranslate().equals(TranslateText.NORMAL);
		boolean isVanilla = theme.getTranslate().equals(TranslateText.VANILLA);
		boolean isGlow = theme.getTranslate().equals(TranslateText.GLOW);
		boolean isVanillaGlow = theme.getTranslate().equals(TranslateText.VANILLA_GLOW);
		boolean isOutline = theme.getTranslate().equals(TranslateText.OUTLINE);
		boolean isOutlineGlow = theme.getTranslate().equals(TranslateText.OUTLINE_GLOW);
		boolean isShadow = theme.getTranslate().equals(TranslateText.SHADOW);
		boolean isDark = theme.getTranslate().equals(TranslateText.DARK);
		boolean isLight = theme.getTranslate().equals(TranslateText.LIGHT);
		
		float lastWidth = width * scale;
		float lastHeight = height * scale;
		float x = this.x + (addX * scale);
		float y = this.y + (addY * scale);
		
		if(alpha > 0.01F && (isNormal || isVanilla || isShadow || isDark || isLight)) {
			nvg.drawShadow(x, y, lastWidth, lastHeight, radius, Math.max(1, Math.round(7.0F * alpha)));
		}else if(alpha > 0.01F && (isGlow || isVanillaGlow)) {
			nvg.drawGradientShadow(x, y, lastWidth, lastHeight, radius, currentColor.getColor1(), currentColor.getColor2());
		}else if(alpha > 0.01F && (isOutline || isOutlineGlow)) {
			if(isOutline) {
				nvg.drawShadow(x - 2, y - 2, lastWidth + 4, lastHeight + 4, radius + 2, Math.max(1, Math.round(7.0F * alpha)));
			}else if(isOutlineGlow) {
				nvg.drawGradientShadow(x - 2, y - 2, lastWidth + 4, lastHeight + 4, radius + 2, currentColor.getColor1(), currentColor.getColor2());
			}
		}

		if(isOutline || isOutlineGlow) {
			nvg.drawGradientOutlineRoundedRect(x - 1, y - 1, lastWidth + 2, lastHeight + 2, radius + 1, 1.5F,
					ColorUtils.applyAlpha(currentColor.getColor1(), (int)(255 * alpha)),
					ColorUtils.applyAlpha(currentColor.getColor2(), (int)(255 * alpha)));
		}
		
		if(isVanilla || isVanillaGlow || isOutline || isOutlineGlow) {
			nvg.drawRoundedRect(x, y, lastWidth, lastHeight, radius, new Color(0, 0, 0, (int)(100 * alpha)));
		}else if(isNormal || isGlow) {
			nvg.drawGradientRoundedRect(x, y, lastWidth, lastHeight, radius,
					ColorUtils.applyAlpha(currentColor.getColor1(), (int)(220 * alpha)),
					ColorUtils.applyAlpha(currentColor.getColor2(), (int)(220 * alpha)));
		} else if(isLight) {
			nvg.drawRoundedRect(x, y, lastWidth, lastHeight, radius, new Color(240, 240, 240, (int)(220 * alpha)));
		} else if(isDark) {
			nvg.drawRoundedRect(x, y, lastWidth, lastHeight, radius, new Color(20, 20, 20, (int)(220 * alpha)));
		}
	}
	
	public void drawBackground(float width, float height) {
		drawBackground(0, 0, width, height, 6 * scale);
	}
	
	public void drawBackground(float addX, float addY, float width, float height) {
		drawBackground(addX, addY, width, height, 6 * scale);
	}
	
	public void drawBackground(float width, float height, float radius) {
		drawBackground(0, 0, width, height, radius);
	}
	
	public void drawText(String text, float addX, float addY, float size, Font font, Color color) {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		float lastSize = size * scale;
		
		nvg.drawText(text, x + (addX * scale), y + (addY * scale), color, lastSize, font);
	}
	
	public void scale(float addX, float addY, float width, float height, float nvgScale) {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		
		nvg.scale(x + (addX * scale), y + (addY * scale), width * scale, height * scale, nvgScale);
	}
	
	public void drawText(String text, float addX, float addY, float size, Font font) {
		drawText(text, addX, addY, size, font, getFontColor());
	}
	
	public void drawCenteredText(String text, float addX, float addY, float size, Font font, Color color) {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		float lastSize = size * scale;
		
		nvg.drawCenteredText(text, x + (addX * scale), y + (addY * scale), color, lastSize, font);
	}
	
	public void drawCenteredText(String text, float addX, float addY, float size, Font font) {
		drawCenteredText(text, addX, addY, size, font, getFontColor());
	}
	
	public float getTextWidth(String text, float size, Font font) {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		
		return nvg.getTextWidth(text, size, font);
	}
	
	public Color getFontColor(int alpha) {
		
		ComboSetting setting = GlobalSettingsMod.getInstance().getModThemeSetting();
		Option theme = setting.getOption();
		
		boolean isDark = theme.getTranslate().equals(TranslateText.DARK);
		boolean isLight = theme.getTranslate().equals(TranslateText.LIGHT);
		
		if(isDark || isLight) {
			return Mix.getInstance().getColorManager().getCurrentColor().getInterpolateColor(alpha);
		}
		
		return new Color(255, 255, 255, alpha);
	}
	
	public Color getFontColor() {
		return getFontColor(255);
	}
	
	public boolean isEditing() {
		return mc.currentScreen instanceof GuiEditHUD;
	}
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
		this.positionSettled = true;
	}

	/**
	 * 位置是否已经有来源（拖过 / 从 profile 读回）。
	 *
	 * <p>给 {@code DangoIslandMod} / {@code DangoSongCardMod} 这类「首次出现要贴默认角」
	 * 的 HUD 用：它们不能在第一次 draw 时无条件覆盖坐标，否则会把 profile 里读回来的
	 * 位置冲掉。</p>
	 */
	public boolean hasPosition() {
		return positionSettled;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
		this.positionSettled = true;
	}

	public int getDraggingX() {
		return draggingX;
	}

	public void setDraggingX(int draggingX) {
		this.draggingX = draggingX;
	}

	public int getDraggingY() {
		return draggingY;
	}

	public void setDraggingY(int draggingY) {
		this.draggingY = draggingY;
	}

	public int getWidth() {
		return (int) (width * scale);
	}

	public void setWidth(int width) {
		this.width = width;
		this.reanchor();
	}

	public int getHeight() {
		return (int) (height * scale);
	}

	public void setHeight(int height) {
		this.height = height;
		this.reanchor();
	}

	/**
	 * 记下当前位置是「贴哪个边」，以及距离那个边多远。
	 *
	 * <p>调用时机：用户松开鼠标结束拖动时（见 GuiEditHUD.mouseReleased）。
	 * 只有真的贴着边（距离 ≤ {@link #ANCHOR_SNAP} 像素）才算锚定，
	 * 否则就是一个自由坐标，尺寸变了也不该动它。</p>
	 */
	public void captureAnchor() {

		if(mc == null) {
			return;
		}

		ScaledResolution sr = new ScaledResolution(mc);

		int rightGap = sr.getScaledWidth() - (getX() + getWidth());
		int bottomGap = sr.getScaledHeight() - (getY() + getHeight());

		anchorRight = rightGap >= 0 && rightGap <= ANCHOR_SNAP;
		anchorBottom = bottomGap >= 0 && bottomGap <= ANCHOR_SNAP;

		anchorGapX = anchorRight ? rightGap : 0;
		anchorGapY = anchorBottom ? bottomGap : 0;
	}

	/**
	 * 尺寸变了，按锚点重算坐标。
	 *
	 * <p>计分板换服务器后条目数不同，{@code getWidth()/getHeight()} 每帧都会变。
	 * 如果用户当初把它拖到了右下角，宽高一变右边缘就会被推出屏幕。
	 * 这里把「距右边多少像素」作为不变量，重算 x，贴底同理。</p>
	 *
	 * <p>没锚定的一边只做保底夹紧（不让它跑出屏幕），
	 * 不会把用户自己放的位置拉到边上。</p>
	 */
	private void reanchor() {

		if(mc == null || (!anchorRight && !anchorBottom)) {
			return;
		}

		ScaledResolution sr = new ScaledResolution(mc);

		if(anchorRight) {
			this.x = Math.max(0, sr.getScaledWidth() - getWidth() - anchorGapX);
		}

		if(anchorBottom) {
			this.y = Math.max(0, sr.getScaledHeight() - getHeight() - anchorGapY);
		}
	}

	public boolean isDragging() {
		return dragging;
	}

	public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		
		if(scale > 5.0 || scale < 0.2) {
			
			if(scale > 5.0) {
				this.scale = 5.0F;
			}
			
			if(scale < 0.2) {
				this.scale = 0.2F;
			}
			
			return;
		}
		
		this.scale = scale;
	}

	public boolean isDraggable() {
		return draggable;
	}

	public void setDraggable(boolean draggable) {
		this.draggable = draggable;
	}
}
