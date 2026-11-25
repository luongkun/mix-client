package mix.client.mixui.management.mods.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender3D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.management.mods.settings.impl.combo.Option;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.buffer.ScreenTarget;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Hitbox driven nametags: every tag is a NanoVG panel rendered into a framebuffer and drawn
 * as a world space billboard, so it follows the entity hitbox (size and anchor derive from
 * Entity#height) instead of being projected into the 2D GUI layer.
 */
public class NametagsMod extends Mod {

	private static NametagsMod instance;

	private static final double MAX_DISTANCE = 64.0D;
	// 11 格以内全不透明，11→15 格线性淡出，15 格完全消失（靠近时按同样曲线反向渐显）
	private static final double FADE_START = 11.0D;
	private static final double FADE_END = 15.0D;
	private static final float FADE_SPEED = 4.5F;
	private static final float HEAD_GAP = 0.28F;
	private static final float HITBOX_HEIGHT_RATIO = 0.30F;
	// 面板先按逻辑尺寸画进 FBO，再当作世界广告牌拉伸，所以"清不清晰"只由这里的像素密度决定。
	// 密度低于世界放大倍数时，字号/血条/边框会被线性插值糊掉——观感上就是"调大就发虚"。
	private static final float PIXEL_SCALE = 4.0F;
	// 密度跟着缩放走：广告牌在世界里被拉大几倍，纹理就得有几倍像素，否则文字边缘会被线性插值抹平。
	// 但显存按面积增长，6 倍线性就是 36 倍占用，同屏十几个标签足以把显存吃穿，所以这里封顶。
	// 12 只是硬上限（当前公式最大只用到 10），留一点余量给以后调参。
	private static final float MAX_PIXEL_SCALE = 12.0F;
	// 老显卡对纹理边长很挑，超过这个值可能创建失败；超了就等比降密度，而不是硬截断导致破框。
	private static final int MAX_TEXTURE_DIM = 2048;
	// 缩放项自身的上下限，用来钳制存档里带过来的历史值（旧版本区间是 0.5~2.0）
	private static final float MIN_SCALE = 1.0F;
	private static final float MAX_SCALE = 6.0F;
	// 性能：遮挡射线不必每帧每人都做
	private static final long OCCLUSION_INTERVAL = 120L;

	private static final float PADDING = 4.0F;
	private static final float NAME_SIZE = 9.0F;
	private static final float INFO_SIZE = 6.5F;
	private static final float BAR_HEIGHT = 2.0F;
	private static final float BAR_GAP = 2.0F;
	private static final float INFO_GAP = 1.5F;
	// 被方块挡住时不再整块跳过绘制，而是保留一层"遮掩"的半透明轮廓
	private static final float OCCLUDED_ALPHA = 0.38F;
	// NanoVG 画进 FBO 的文字不吃 glColor，把透明度按 1/32 台阶烘进纹理，剩下用 glColor 补齐
	private static final int ALPHA_STEPS = 32;

	// 区间 1.0-6.0、默认 1.0，与"目标信息"的缩放手感对齐：
	// 原来 0.5-2.0 的毛病是上限太低（拉满只是 1.08 格高），下限又把滑块手柄挤在最左端，
	// 现在下限收到 1.0（保住默认观感），上限放到 6.0，滑块整段行程都有可见变化。
	private NumberSetting scaleSetting = new NumberSetting(TranslateText.NAMETAGS_SCALE, this, 1.0D, 1.0D, 6.0D, false);
	private BooleanSetting healthSetting = new BooleanSetting(TranslateText.NAMETAGS_HEALTH, this, true);
	private BooleanSetting distanceSetting = new BooleanSetting(TranslateText.NAMETAGS_DISTANCE, this, true);
	private BooleanSetting pingSetting = new BooleanSetting(TranslateText.NAMETAGS_PING, this, true);
	private BooleanSetting backgroundSetting = new BooleanSetting(TranslateText.NAMETAGS_BACKGROUND, this, true);
	private BooleanSetting ownNameSetting = new BooleanSetting(TranslateText.NAMETAGS_OWN_NAME, this, false);

	private final Map<EntityPlayer, Tag> tags = new HashMap<EntityPlayer, Tag>();
	private long lastUpdate;

	public NametagsMod() {
		super(TranslateText.NAMETAGS, TranslateText.NAMETAGS_DESCRIPTION, ModCategory.RENDER);

		instance = this;
	}

	public static NametagsMod getInstance() {
		return instance;
	}

	@Override
	public void onDisable() {
		super.onDisable();
		this.releaseAll();
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {

		if (mc.theWorld == null || mc.thePlayer == null || mc.gameSettings.hideGUI) {
			this.releaseAll();
			return;
		}

		List<EntityPlayer> players = mc.theWorld.playerEntities;

		if (players.isEmpty()) {
			this.releaseAll();
			return;
		}

		long now = System.currentTimeMillis();
		float delta = this.lastUpdate == 0L ? 0.016F : Math.min(0.25F, (now - this.lastUpdate) / 1000.0F);
		this.lastUpdate = now;

		float partialTicks = event.getPartialTicks();
		// 存档里可能还留着旧版本（0.5~2.0）的数值，这里按新区间重新钳一次，
		// 否则老配置会得到 0.5 倍这种比默认还小的标签。
		float scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scaleSetting.getValueFloat()));
		// 密度按档位量化且每帧只算一次：同一帧内所有人的标签共用同一档，
		// 拖动滑块时纹理尺寸也只在跨档时重建，不会每 1% 就删建一次 FBO。
		float pixelRatio = this.getPixelRatio(scale);

		for (EntityPlayer player : players) {

			if (player == null || player.isDead || player.getHealth() <= 0.0F) {
				continue;
			}

			if (player == mc.thePlayer && !ownNameSetting.isToggled()) {
				continue;
			}

			double distance = mc.thePlayer.getDistanceToEntity(player);

			if (distance > MAX_DISTANCE) {
				continue;
			}

			Tag tag = this.tags.get(player);

			if (tag == null) {
				tag = new Tag();
				// 新出现的标签直接对齐当前距离对应的透明度，避免出现瞬间全亮再淡出的跳变
				tag.alpha = this.getTargetAlpha(distance);
				this.tags.put(player, tag);
			}

			// 遮挡改为"遮掩"：被挡住只是降成半透明，仍然继续走后续的淡化与绘制。
			// 标签始终按深度测试绘制（不再提供穿墙开关），因此被方块挡住时本来就看不见，
			// 这里额外压暗一层，让露在方块边缘外的那部分也不会突然全亮。
			if (now >= tag.nextOcclusionCheck) {
				tag.occluded = this.isOccluded(player, partialTicks);
				tag.nextOcclusionCheck = now + OCCLUSION_INTERVAL;
			}

			boolean occludedNow = tag.occluded;

			float occlusionTarget = occludedNow ? OCCLUDED_ALPHA : 1.0F;
			tag.occlusionAlpha += (occlusionTarget - tag.occlusionAlpha) * Math.min(1.0F, delta * FADE_SPEED);

			float targetAlpha = this.getTargetAlpha(distance);
			tag.alpha += (targetAlpha - tag.alpha) * Math.min(1.0F, delta * FADE_SPEED);

			float finalAlpha = Math.max(0.0F, Math.min(1.0F, tag.alpha * tag.occlusionAlpha));

			if (finalAlpha < 0.01F) {
				continue;
			}

			// 文字是用 NanoVG 画进 FBO 的，不受 GlStateManager.color 的 alpha 调制，
			// 所以把一段透明度烘进纹理（只在台阶变化时重画），剩余部分交给 glColor。
			int alphaStep = Math.round(finalAlpha * ALPHA_STEPS);

			if (alphaStep < 1) {
				alphaStep = 1;
			}

			if (alphaStep != tag.bakedAlphaStep) {
				tag.bakedAlphaStep = alphaStep;
				tag.signature = null;
			}

			tag.bakedAlpha = alphaStep / (float) ALPHA_STEPS;
			tag.renderAlpha = finalAlpha;

			if (!this.updateTexture(tag, player, distance, pixelRatio)) {
				continue;
			}

			this.drawTag(tag, player, partialTicks, scale);
		}

		this.pruneTags(players);
	}

	private float getTargetAlpha(double distance) {

		if (distance <= FADE_START) {
			return 1.0F;
		}

		if (distance >= FADE_END) {
			return 0.0F;
		}

		return (float) ((FADE_END - distance) / (FADE_END - FADE_START));
	}

	private void drawTag(Tag tag, EntityPlayer player, float partialTicks, float scale) {

		RenderManager renderManager = mc.getRenderManager();

		double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.height + HEAD_GAP;
		double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

		float height = player.height * HITBOX_HEIGHT_RATIO * scale;
		float width = height * ((float) tag.textureWidth / (float) tag.textureHeight);

		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableAlpha();
		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(false);

		GlStateManager.enableDepth();

		GlStateManager.resetColor();
		GlStateManager.color(1.0F, 1.0F, 1.0F, tag.renderAlpha / tag.bakedAlpha);
		GlStateManager.bindTexture(tag.framebuffer.texture());

		GlStateManager.translate(x - renderManager.viewerPosX, y - renderManager.viewerPosY, z - renderManager.viewerPosZ);
		GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

		float halfWidth = width / 2.0F;

		// NanoVG 的 framebuffer 纹理在 GL 采样时 UV 双向翻转，这里按 TargetInfo 的约定映射
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex3f(-halfWidth, height, 0.0F);
		GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex3f(halfWidth, height, 0.0F);
		GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex3f(halfWidth, 0.0F, 0.0F);
		GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex3f(-halfWidth, 0.0F, 0.0F);
		GL11.glEnd();

		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.enableAlpha();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private boolean updateTexture(Tag tag, EntityPlayer player, double distance, float pixelRatio) {

		String info = this.getInfoText(player, distance);
		// 密度必须进签名：面板的逻辑宽高与文字内容在缩放时完全没变，
		// 只有烘纹理的像素密度变了。签名不含密度的话，拖动滑块会命中"签名相同"的短路分支，
		// 纹理永不重烘——表现就是滑块能动、标签却一直糊，这正是之前缩放调不动的根因。
		String signature = this.getSignature(player, info, pixelRatio);

		if (tag.framebuffer != null && signature.equals(tag.signature)) {
			return true;
		}

		int width = Math.max(16, (int) Math.ceil(this.getPanelWidth(player, info)));
		int height = Math.max(8, (int) Math.ceil(this.getPanelHeight()));

		int textureWidth = Math.max(1, (int) (width * pixelRatio));
		int textureHeight = Math.max(1, (int) (height * pixelRatio));

		// 超长宽的显卡会让 FBO 创建直接失败，标签整块消失比略糊更糟：
		// 这里等比降密度保住宽高比，宁可牺牲一点锐度。
		int longestSide = Math.max(textureWidth, textureHeight);

		if (longestSide > MAX_TEXTURE_DIM) {

			float fit = (float) MAX_TEXTURE_DIM / (float) longestSide;

			textureWidth = Math.max(1, (int) (textureWidth * fit));
			textureHeight = Math.max(1, (int) (textureHeight * fit));
			// 密度必须与实际纹理宽高一致，否则 NanoVG 会把逻辑坐标压扁到剩下的像素里，
			// 结果不是"略糊"而是整体挤成一条。取两者较小值，保证内容不会溢出到 FBO 之外被裁。
			pixelRatio = Math.min(textureWidth / (float) width, textureHeight / (float) height);
		}

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		long context = nvg.getContext();

		if (tag.framebuffer == null || tag.textureWidth != textureWidth || tag.textureHeight != textureHeight) {

			this.deleteFramebuffer(tag);

			tag.framebuffer = NanoVGGL2.nvgluCreateFramebuffer(context, textureWidth, textureHeight, 0);
			tag.textureWidth = textureWidth;
			tag.textureHeight = textureHeight;

			if (tag.framebuffer == null) {
				return false;
			}
		}

		int target = ScreenTarget.capture();

		NanoVGGL2.nvgluBindFramebuffer(context, tag.framebuffer);
		GL11.glViewport(0, 0, textureWidth, textureHeight);
		GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		// 第三个参数是设备像素比：逻辑坐标仍按 width x height 排布，
		// 但实际光栅化到 textureWidth x textureHeight，所以放大后文字依旧锐利。
		NanoVG.nvgBeginFrame(context, width, height, pixelRatio);
		NanoVG.nvgGlobalAlpha(context, tag.bakedAlpha);
		this.drawPanel(nvg, player, info, width, height);
		NanoVG.nvgEndFrame(context);
		GL11.glPopAttrib();

		ScreenTarget.restore(target);

		tag.signature = signature;

		return true;
	}

	private void drawPanel(NanoVGManager nvg, EntityPlayer player, String info, int width, int height) {

		AccentColor currentColor = Mix.getInstance().getColorManager().getCurrentColor();

		if (backgroundSetting.isToggled()) {
			this.drawBackground(nvg, currentColor, 0.0F, 0.0F, width, height, 3.0F);
		}

		float centerX = width / 2.0F;
		float currentY = PADDING;

		this.drawNameText(nvg, player.getName(), centerX, currentY, NAME_SIZE);

		currentY += NAME_SIZE + BAR_GAP;

		if (healthSetting.isToggled()) {

			float ratio = Math.min(1.0F, Math.max(0.0F, player.getHealth()) / Math.max(1.0F, player.getMaxHealth()));
			float barX = PADDING;
			float barWidth = width - PADDING * 2.0F;

			nvg.drawRoundedRect(barX, currentY, barWidth, BAR_HEIGHT, BAR_HEIGHT / 2.0F, new Color(0, 0, 0, 120));

			if (ratio > 0.0F) {
				nvg.drawGradientRoundedRect(barX, currentY, barWidth * ratio, BAR_HEIGHT, BAR_HEIGHT / 2.0F,
						currentColor.getColor1(), currentColor.getColor2());
			}

			currentY += BAR_HEIGHT + INFO_GAP;
		}

		if (info != null) {
			nvg.drawCenteredText(info, centerX, currentY, new Color(255, 255, 255, 215), INFO_SIZE, Fonts.REGULAR);
		}
	}

	private float getPanelWidth(EntityPlayer player, String info) {

		Tag tag = this.tags.get(player);
		String key = player.getName() + "|" + String.valueOf(info);

		if (tag != null && key.equals(tag.widthKey)) {
			return tag.widthCache;
		}

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();

		float nameWidth = nvg.getTextWidth(player.getName(), NAME_SIZE, Fonts.MEDIUM);
		float infoWidth = info == null ? 0.0F : nvg.getTextWidth(info, INFO_SIZE, Fonts.REGULAR);
		float width = Math.max(nameWidth, infoWidth) + PADDING * 2.0F;

		if (tag != null) {
			tag.widthKey = key;
			tag.widthCache = width;
		}

		return width;
	}

	private float getPanelHeight() {

		float height = PADDING + NAME_SIZE + PADDING;

		if (healthSetting.isToggled()) {
			height += BAR_GAP + BAR_HEIGHT;
		}

		if (this.hasInfo()) {
			height += INFO_GAP + INFO_SIZE;
		}

		return height;
	}

	private boolean hasInfo() {
		return distanceSetting.isToggled() || pingSetting.isToggled();
	}

	private String getSignature(EntityPlayer player, String info, float pixelRatio) {

		Option theme = GlobalSettingsMod.getInstance().getModThemeSetting().getOption();
		AccentColor currentColor = Mix.getInstance().getColorManager().getCurrentColor();

		int health = (int) (player.getHealth() * 2.0F) / 2;

		// 密度已经是量化过的整数档，这里再乘 10 取整，纯粹为了不让浮点尾数抖动
		// 把同一个档位判成两档、导致每帧反复重烘。
		int ratioKey = Math.round(pixelRatio * 10.0F);

		return player.getName() + "|" + health + "|" + String.valueOf(info) + "|" + theme.getName() + "|"
				+ currentColor.getColor1().getRGB() + "|" + currentColor.getColor2().getRGB() + "|" + backgroundSetting.isToggled()
				+ "|" + ratioKey;
	}

	/**
	 * 烘纹理用的像素密度。
	 *
	 * 标签的逻辑尺寸（宽高、字号、血条、圆角、边框）全部固定，缩放只做两件事：
	 * 把世界广告牌拉大、把纹理密度抬高。要让放大后不糊，密度就得追上"广告牌在屏幕上占多少像素"。
	 *
	 * 粗算：面板高 0.54*scale 格，FOV 70 时 1 格在 d 格外约占 450/d 屏幕像素，
	 * 所以面板屏幕高度约 243*scale/d 像素，而纹理高度是 29*ratio，
	 * 于是 ratio ≈ 8.4*scale/d。名字标签只在 11 格内全亮（FADE_START），
	 * 取 d=5 这个"近身看"的距离，ratio ≈ 1.7*scale——正好接近开方曲线。
	 * 线性抬到 6 倍会让显存涨 36 倍（同屏十几个标签就炸），平方根只涨 6 倍，取平方根。
	 *
	 * 再量化到 0.5 一档（1.0→4.0、6.0→10.0，整段 13 档）：拖动滑块时纹理尺寸最多重建 13 次，
	 * 不会每动 1% 就删建一个 FBO；档距小于 0.5 则密度涨幅盖不住广告牌涨幅，放大后仍会觉得糊。
	 */
	private float getPixelRatio(float scale) {

		float ideal = PIXEL_SCALE * (float) Math.sqrt(Math.max(1.0F, scale));
		float clamped = Math.max(PIXEL_SCALE, Math.min(MAX_PIXEL_SCALE, ideal));

		return Math.round(clamped * 2.0F) / 2.0F;
	}

	private void drawNameText(NanoVGManager nvg, String name, float centerX, float topY, float size) {

		Option theme = GlobalSettingsMod.getInstance().getModThemeSetting().getOption();
		boolean isDark = theme.getTranslate().equals(TranslateText.DARK);
		boolean isLight = theme.getTranslate().equals(TranslateText.LIGHT);

		Color color = (isDark || isLight) ? Mix.getInstance().getColorManager().getCurrentColor().getInterpolateColor(255) : new Color(255, 255, 255, 255);

		nvg.drawCenteredText(name, centerX, topY, color, size, Fonts.MEDIUM);
	}

	private String getInfoText(EntityPlayer player, double distance) {

		String distanceText = distanceSetting.isToggled() ? (((int) (distance + 0.5D)) + "m") : null;

		NetworkPlayerInfo playerInfo = mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfo(player.getUniqueID());
		int ping = -1;

		if (pingSetting.isToggled() && playerInfo != null) {
			ping = playerInfo.getResponseTime();
		}

		if (distanceText != null && ping >= 0) {
			return distanceText + " | " + ping + "ms";
		}

		if (distanceText != null) {
			return distanceText;
		}

		if (ping >= 0) {
			return ping + "ms";
		}

		return null;
	}

	private void drawBackground(NanoVGManager nvg, AccentColor currentColor, float x, float y, float width, float height, float radius) {

		Option theme = GlobalSettingsMod.getInstance().getModThemeSetting().getOption();

		boolean isNormal = theme.getTranslate().equals(TranslateText.NORMAL);
		boolean isGlow = theme.getTranslate().equals(TranslateText.GLOW);
		boolean isVanilla = theme.getTranslate().equals(TranslateText.VANILLA);
		boolean isVanillaGlow = theme.getTranslate().equals(TranslateText.VANILLA_GLOW);
		boolean isOutline = theme.getTranslate().equals(TranslateText.OUTLINE);
		boolean isOutlineGlow = theme.getTranslate().equals(TranslateText.OUTLINE_GLOW);
		boolean isLight = theme.getTranslate().equals(TranslateText.LIGHT);
		boolean isDark = theme.getTranslate().equals(TranslateText.DARK);

		if (isLight) {
			nvg.drawRoundedRect(x, y, width, height, radius, new Color(240, 240, 240, 215));
			return;
		}

		if (isDark) {
			nvg.drawRoundedRect(x, y, width, height, radius, new Color(20, 20, 20, 215));
			return;
		}

		if (isVanilla || isVanillaGlow || isOutline || isOutlineGlow) {

			nvg.drawRoundedRect(x, y, width, height, radius, new Color(0, 0, 0, 110));

			if (isOutline || isOutlineGlow) {
				nvg.drawGradientOutlineRoundedRect(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, radius + 1.0F, 1.5F,
						currentColor.getColor1(), currentColor.getColor2());
			}

			if (isVanillaGlow || isOutlineGlow) {
				nvg.drawShadow(x, y, width, height, radius);
			}

			return;
		}

		if (isNormal || isGlow) {

			nvg.drawGradientRoundedRect(x, y, width, height, radius,
					ColorUtils.applyAlpha(currentColor.getColor1(), 215), ColorUtils.applyAlpha(currentColor.getColor2(), 215));

			if (isGlow) {
				nvg.drawGradientShadow(x, y, width, height, radius, currentColor.getColor1(), currentColor.getColor2());
			}

			return;
		}

		nvg.drawGradientRoundedRect(x, y, width, height, radius,
				ColorUtils.applyAlpha(currentColor.getColor1(), 215), ColorUtils.applyAlpha(currentColor.getColor2(), 215));
	}

	private boolean isOccluded(EntityPlayer player, float partialTicks) {

		Vec3 eye = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
		Vec3 head = new Vec3(player.posX, player.posY + player.height, player.posZ);

		MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(eye, head, false, true, false);

		return movingObjectPosition != null;
	}

	private void pruneTags(List<EntityPlayer> players) {

		Iterator<Map.Entry<EntityPlayer, Tag>> iterator = this.tags.entrySet().iterator();

		while (iterator.hasNext()) {

			Map.Entry<EntityPlayer, Tag> entry = iterator.next();

			EntityPlayer player = entry.getKey();
			boolean remove = player == null || !players.contains(player) || player.isDead;

			if (!remove && mc.thePlayer != null && mc.thePlayer.getDistanceToEntity(player) > MAX_DISTANCE) {
				remove = true;
			}

			if (remove) {
				this.deleteFramebuffer(entry.getValue());
				iterator.remove();
			}
		}
	}

	private void releaseAll() {

		for (Tag tag : this.tags.values()) {
			this.deleteFramebuffer(tag);
		}

		this.tags.clear();
	}

	private void deleteFramebuffer(Tag tag) {

		if (tag.framebuffer != null) {
			NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
			NanoVGGL2.nvgluDeleteFramebuffer(nvg.getContext(), tag.framebuffer);
			tag.framebuffer = null;
		}

		tag.signature = null;
	}

	private class Tag {

		private NVGLUFramebuffer framebuffer;
		private int textureWidth, textureHeight;
		private String signature;
		private float alpha = 1.0F;

		// 遮挡遮掩用的乘数（0.38 = 被方块挡住），与距离淡化相乘得到最终透明度
		private float occlusionAlpha = 1.0F;
		// 已经烘进纹理的透明度台阶与数值，renderAlpha 是这一帧真正想要的透明度
		private int bakedAlphaStep = -1;
		private float bakedAlpha = 1.0F;
		private float renderAlpha = 1.0F;

		private long nextOcclusionCheck;
		private boolean occluded;

		private String widthKey;
		private float widthCache;
	}
}
