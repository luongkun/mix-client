package mix.client.mixui.management.mods.impl;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import javax.imageio.ImageIO;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventAttackEntity;
import mix.client.mixui.management.event.impl.EventRender2D;
import mix.client.mixui.management.event.impl.EventRender3D;
import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.HUDMod;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.management.mods.settings.impl.combo.Option;
import mix.client.mixui.management.nanovg.NanoVGManager;
import mix.client.mixui.management.nanovg.font.Fonts;
import mix.client.mixui.management.nanovg.font.Icon;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.ServerUtils;
import mix.client.mixui.utils.animation.simple.SimpleAnimation;
import mix.client.mixui.utils.buffer.ScreenAnimation;
import mix.client.mixui.utils.buffer.ScreenTarget;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

public class TargetInfoMod extends HUDMod {

	private static final double MAX_DISTANCE = 10.0D;
	private static final long HIT_TIMEOUT = 6000L;
	// 面板显示/消失动画时长（与未修改之前一致）
	private static final long APPEAR_TIME = 330L;
	private static final long HIDE_TIME = 330L;
	private static final long GRAFFITI_TIME = 1000L;
	private static final int MAX_TARGETS = 8;
	private static final int PAD_LEFT = 120;
	private static final int PAD_TOP = 110;
	private static final int PAD_BOTTOM = 24;
	// 性能：遮挡射线按固定间隔检测，不必每帧每目标都做
	private static final long VISIBLE_INTERVAL = 100L;
	// 被方块挡住时不再整块跳过绘制，而是降成半透明的"遮掩"
	private static final float OCCLUDED_ALPHA = 0.35F;
	private static final float OCCLUSION_FADE_SPEED = 4.5F;
	// NanoVG 画进 FBO 的文字不吃 glColor，把透明度按 1/32 台阶烘进纹理，剩余交给 glColor
	private static final int ALPHA_STEPS = 32;
	// 一次受击的无敌帧是 10 tick（0.5 秒），火花的存活时间以此为基准再放大
	private static final int HURT_TIME_TICKS = 10;
	private static final float HURT_LIFE = HURT_TIME_TICKS / 20.0F;
	// 火花自身存活时长：取无敌帧的 2.3 倍（约 1.15 秒），够长才看得清自然消散，再叠一点随机避免每次整齐消失
	private static final float SPARK_LIFE_BASE = HURT_LIFE * 2.3F;
	private static final float SPARK_LIFE_RANDOM = 0.35F;
	// 最多同时存在三拨火花；超出时最老的一拨用 REPLACE_FADE_MS 缓慢淡完，不是瞬灭
	private static final int MAX_BURSTS = 3;
	private static final long REPLACE_FADE_MS = 260L;
	// 3D 面板在世界里的基础高度（格）。以前取 entity.height * 0.75 算出来接近 1.35 格，
	// 比玩家肩膀还高，看上去就是"大小出错"；现在换成固定基准，再乘菜单里的"面板大小"。
	private static final float BASE_PANEL_HEIGHT = 0.42F;

	private static final String[] WORDS = new String[]{"BOOM!", "NICE!", "ULTRA!", "SMASH!"};

	private static final String[] TEXTURE_PATHS = new String[]{
			"/assets/mixclient/targetinfo/boom.png",
			"/assets/mixclient/targetinfo/nice.png",
			"/assets/mixclient/targetinfo/ultra.png",
			"/assets/mixclient/targetinfo/smash.png"
	};

	private static final int[] TEXTURE_IDS = new int[]{-1, -1, -1, -1};

	private static final Color[][] PALETTES = new Color[][]{
			{new Color(255, 196, 30), new Color(200, 55, 15), new Color(255, 120, 40)},
			{new Color(86, 196, 255), new Color(80, 50, 210), new Color(225, 240, 255)},
			{new Color(255, 80, 110), new Color(140, 10, 40), new Color(255, 210, 90)},
			{new Color(70, 220, 160), new Color(10, 90, 70), new Color(200, 255, 230)},
			{new Color(255, 90, 200), new Color(110, 15, 105), new Color(255, 220, 255)},
			{new Color(255, 150, 40), new Color(160, 25, 10), new Color(255, 240, 150)},
			{new Color(120, 230, 255), new Color(30, 80, 160), new Color(255, 255, 255)}
	};

	private static final float[] ASPECTS = new float[]{1.9112F, 1.9228F, 1.8333F, 1.7057F};

	// 围绕目标玩家画的光圈，以及命中时的火花/涂鸦特效，都可以在菜单里单独关掉
	private BooleanSetting ringSetting = new BooleanSetting(TranslateText.TARGET_INFO_RING, this, true);
	private BooleanSetting hitEffectSetting = new BooleanSetting(TranslateText.TARGET_INFO_HIT_EFFECT, this, true);
	// 围绕玩家：开 = 面板贴在目标身侧跟着转（3D billboard）；关 = 回到屏幕固定的 HUD，可拖动、可缩放
	private BooleanSetting followSetting = new BooleanSetting(TranslateText.TARGET_INFO_FOLLOW, this, true);
	// 面板大小只影响 3D 那一层；屏幕固定 HUD 的尺寸由 GuiEditHUD 的缩放决定
	// 区间放宽到 0.4–6.0：原来的 0.5–2.5 上限太低，拉到顶也不够大，滑块默认值还落在很靠左的位置
	// 区间 1.0-6.0、默认 2.5：
	// 原来 0.5-2.5 两个毛病——下限 0.5 把滑块手柄挤在最左边（默认 1.0 只占满 25% 的行程），
	// 上限 2.5 拉到顶也只有 1.05 格高，看着像"调不动"。现在下限抬到 1.0、上限放到 6.0，
	// 默认 2.5（面板约 1.05 格高）落在行程 30% 处，往右还有一倍多余量。
	private NumberSetting sizeSetting = new NumberSetting(TranslateText.TARGET_INFO_SCALE, this, 2.5D, 1.0D, 6.0D, false);

	private final Map<AbstractClientPlayer, TargetEntry> entries = new LinkedHashMap<AbstractClientPlayer, TargetEntry>();
	private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
	private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
	private final FloatBuffer screenPos = BufferUtils.createFloatBuffer(3);
	private ScreenAnimation screenAnimation = new ScreenAnimation();
	// 屏幕固定模式下自己的淡入淡出，跟 3D 目标列表互不影响
	private SimpleAnimation fixedAnimation = new SimpleAnimation();
	private TargetEntry editorEntry;
	private String lastRenderError;

	public TargetInfoMod() {
		super(TranslateText.TARGET_INFO, TranslateText.TARGET_INFO_DESCRIPTION);
	}

	// 关掉模组时把所有目标面板的 FBO 一起释放，
	// 否则切来切去会在显存里留下越来越少的空位，最后创建 FBO 失败直接崩渲染。
	@Override
	public void onDisable() {
		super.onDisable();
		this.releaseAll();
	}

	private void releaseAll() {

		for(TargetEntry entry : entries.values()) {
			releaseEntry(entry);
		}

		entries.clear();

		if(editorEntry != null) {
			releaseEntry(editorEntry);
			editorEntry = null;
		}

		screenAnimation.close();
	}

	@EventTarget
	public void onAttackEntity(EventAttackEntity event) {

		Entity attacked = event.getEntity();

		if(!(attacked instanceof AbstractClientPlayer) || mc.thePlayer == null) {
			return;
		}

		AbstractClientPlayer player = (AbstractClientPlayer) attacked;

		if(player == mc.thePlayer || !ServerUtils.isInTablist(player)) {
			return;
		}

		TargetEntry entry = entries.get(player);

		if(entry == null) {

			if(entries.size() >= MAX_TARGETS) {
				AbstractClientPlayer oldest = null;
				long oldestTime = Long.MAX_VALUE;

				for(Map.Entry<AbstractClientPlayer, TargetEntry> pair : entries.entrySet()) {
					if(pair.getValue().lastHitTime < oldestTime) {
						oldestTime = pair.getValue().lastHitTime;
						oldest = pair.getKey();
					}
				}

				if(oldest != null) {
					removeEntry(oldest);
				}
			}

			entry = new TargetEntry(player);
			entries.put(player, entry);
		}

		entry.lastHitTime = System.currentTimeMillis();
		entry.hurt = 1.0F;
		entry.shakeStart = entry.lastHitTime;
		entry.shakeSeed = (long) (Math.random() * Long.MAX_VALUE);
		entry.hiding = false;
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {

		if(this.isEditing() && mc.thePlayer != null) {

			if(editorEntry == null) {
				editorEntry = new TargetEntry(mc.thePlayer);
			}

			editorEntry.entity = mc.thePlayer;
			editorEntry.updateData();
			editorEntry.progress = 1.0F;
			editorEntry.hurt = 0.0F;
			editorEntry.healthAnimation.setAnimation(editorEntry.health, 16);
			editorEntry.armorAnimation.setAnimation(editorEntry.armor, 16);

			int panelWidth = getPanelWidth(editorEntry.name);

			this.setWidth(panelWidth);
			this.setHeight(46);

			screenAnimation.wrap(() -> drawPanel(editorEntry), this.getX(), this.getY(), this.getWidth(), this.getHeight(), 1.0F, 1.0F);

			return;
		}

		// 关掉"围绕玩家"之后，面板回到最开始那种屏幕固定 HUD：位置/缩放都由 GuiEditHUD 管
		if(followSetting.isToggled()) {
			fixedAnimation.setAnimation(0.0F, 16);
			return;
		}

		TargetEntry fixed = getLatestEntry();

		if(fixed == null) {
			fixedAnimation.setAnimation(0.0F, 16);
			return;
		}

		fixed.updateData();
		fixed.healthAnimation.setAnimation(fixed.health, 16);
		fixed.armorAnimation.setAnimation(fixed.armor, 16);
		fixedAnimation.setAnimation(1.0F, 16);

		float fixedProgress = Math.max(0.0F, Math.min(1.0F, fixedAnimation.getValue()));

		if(fixedProgress <= 0.01F) {
			return;
		}

		final TargetEntry target = fixed;

		this.setWidth(getPanelWidth(fixed.name));
		this.setHeight(46);

		screenAnimation.wrap(() -> drawPanel(target), this.getX(), this.getY(), this.getWidth(), this.getHeight(), 2.0F - fixedProgress, fixedProgress);
	}

	private TargetEntry getLatestEntry() {

		TargetEntry latest = null;

		for(TargetEntry entry : entries.values()) {

			if(entry.hiding || entry.progress <= 0.0F) {
				continue;
			}

			if(latest == null || entry.lastHitTime >= latest.lastHitTime) {
				latest = entry;
			}
		}

		return latest;
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {

		if(mc.thePlayer == null) {
			return;
		}

		long now = System.currentTimeMillis();
		Iterator<Map.Entry<AbstractClientPlayer, TargetEntry>> iterator = entries.entrySet().iterator();

		while(iterator.hasNext()) {

			Map.Entry<AbstractClientPlayer, TargetEntry> pair = iterator.next();
			TargetEntry entry = pair.getValue();
			AbstractClientPlayer entity = pair.getKey();

			if(entity == null || entity.isDead || !entity.isEntityAlive()) {
				entry.hiding = true;
			}

			double distance = entity == null ? Double.MAX_VALUE : mc.thePlayer.getDistanceToEntity(entity);
			boolean timeout = now - entry.lastHitTime > HIT_TIMEOUT;

			if(distance > MAX_DISTANCE || timeout) {
				entry.hiding = true;
			}

			float delta = (now - entry.lastUpdate) / 1000.0F;
			entry.lastUpdate = now;

			if(delta > 0.25F) {
				delta = 0.25F;
			}

			if(entry.hiding) {
				entry.progress -= delta * (1000.0F / HIDE_TIME);
			} else {
				entry.progress += delta * (1000.0F / APPEAR_TIME);
			}

			entry.progress = Math.max(0.0F, Math.min(1.0F, entry.progress));

			if(entry.hiding && entry.progress <= 0.0F) {
				iterator.remove();
				releaseEntry(entry);
				continue;
			}

			entry.hurt *= (float) Math.pow(0.78D, delta * 60.0D);

			if(entry.hurt < 0.01F) {
				entry.hurt = 0.0F;
			}

			updateGraffiti(entry, now);

			if(entity == null) {
				continue;
			}

			// 遮挡改为"遮掩"：被方块挡住只是降成半透明，仍然继续刷新数据与绘制
			if(now >= entry.nextVisibleCheck) {
				entry.visible = isVisible(entity);
				entry.nextVisibleCheck = now + VISIBLE_INTERVAL;
			}

			float occlusionTarget = entry.visible ? 1.0F : OCCLUDED_ALPHA;
			entry.occlusionAlpha += (occlusionTarget - entry.occlusionAlpha) * Math.min(1.0F, delta * OCCLUSION_FADE_SPEED);

			if(entry.occlusionAlpha < 0.01F) {
				entry.occlusionAlpha = 0.01F;
			}

			entry.updateData();
			entry.checkHurt(entity);

			if(hitEffectSetting.isToggled()) {
				entry.updateBlood();
			} else if(!entry.particles.isEmpty()) {
				// 关掉打击特效后立刻清场，不留上一击的残影
				entry.particles.clear();
				entry.bursts.clear();
			}

			entry.healthAnimation.setAnimation(entry.health, 16);
			entry.armorAnimation.setAnimation(entry.armor, 16);

			if(followSetting.isToggled()) {
				renderEntry(entry, entity, event.getPartialTicks());
			}

			// 玩家脚下那圈「灰色光圈」已按需求删除：它和目标指示器观感重复，
			// 且和目标信息面板同时出现/消失，容易被当成目标指示器渲染坏了。
			// drawRing 方法体保留但不再调用，方便将来想恢复时一行就能接回来。
		}
	}

	// 围绕目标玩家脚下的光圈：受击时会亮一下，平时是缓慢呼吸的描边
	private void drawRing(TargetEntry entry, AbstractClientPlayer entity, float partialTicks, long now) {

		float alpha = Math.max(0.0F, Math.min(1.0F, entry.progress)) * entry.occlusionAlpha;

		if(alpha <= 0.01F) {
			return;
		}

		RenderManager renderManager = mc.getRenderManager();

		double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX;
		double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY;
		double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ;

		AccentColor accent = Mix.getInstance().getColorManager().getCurrentColor();
		float pulse = 0.85F + 0.15F * (float) Math.sin(now / 260.0D + entity.getEntityId());
		float radius = entity.width * 0.75F + 0.18F;
		float width = 2.0F + entry.hurt * 2.4F;
		Color base = ColorUtils.applyAlpha(accent.getInterpolateColor(), (int) (alpha * 190.0F * pulse));
		Color glow = ColorUtils.applyAlpha(accent.getColor2(), (int) (alpha * 70.0F * pulse));

		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.disableCull();
		GL11.glEnable(GL11.GL_LINE_SMOOTH);

		GL11.glTranslated(x, y + 0.02D, z);

		// 外圈：宽一点的暗色底，让光圈在亮地面上也看得清
		GL11.glLineWidth(width * 2.0F + 2.0F);
		ColorUtils.setColor(glow.getRGB());
		GL11.glBegin(GL11.GL_LINE_LOOP);

		for(int i = 0; i < 48; i++) {
			double angle = i * Math.PI * 2.0D / 48.0D;
			GL11.glVertex3d(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
		}

		GL11.glEnd();

		GL11.glLineWidth(width);
		ColorUtils.setColor(base.getRGB());
		GL11.glBegin(GL11.GL_LINE_LOOP);

		for(int i = 0; i < 48; i++) {
			double angle = i * Math.PI * 2.0D / 48.0D;
			GL11.glVertex3d(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
		}

		GL11.glEnd();

		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GlStateManager.enableCull();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.resetColor();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private void updateGraffiti(TargetEntry entry, long now) {

		Iterator<GraffitiInstance> iterator = entry.graffitiList.iterator();

		while(iterator.hasNext()) {

			GraffitiInstance graffiti = iterator.next();

			if(now - graffiti.startTime >= GRAFFITI_TIME) {
				iterator.remove();
				if(graffiti.framebuffer != null) {
					NanoVGGL2.nvgluDeleteFramebuffer(Mix.getInstance().getNanoVGManager().getContext(), graffiti.framebuffer);
					graffiti.framebuffer = null;
				}
			}
		}
	}

	private void drawGraffiti2D(TargetEntry entry) {

		long now = System.currentTimeMillis();
		ScaledResolution sr = new ScaledResolution(mc);
		int slot = 0;

		for(GraffitiInstance graffiti : entry.graffitiList) {

			float progress = (now - graffiti.startTime) / (float) GRAFFITI_TIME;

			if(progress < 0.0F || progress >= 1.0F) {
				slot++;
				continue;
			}

			float alpha;
			float scale;
			float rise;

			if(progress < 0.2F) {
				float q = progress / 0.2F;
				alpha = q;
				scale = q < 0.5F ? 0.5F + 0.7F * easeOutCubic(q * 2.0F) : 1.2F - 0.2F * easeInOutCubic((q - 0.5F) * 2.0F);
				rise = 0.35F * easeOutCubic(q);
			} else if(progress < 0.7F) {
				float q = (progress - 0.2F) / 0.5F;
				alpha = 1.0F;
				scale = 1.0F + 0.05F * (float) Math.sin(q * Math.PI * 2.0F);
				rise = 0.35F + 0.25F * q;
			} else {
				float q = (progress - 0.7F) / 0.3F;
				alpha = 1.0F - q;
				scale = 1.0F - 0.1F * q;
				rise = 0.60F + 0.35F * q;
			}

			int texture = getGraffitiTexture(graffiti.index);

			if(texture == 0 || alpha <= 0.01F) {
				slot++;
				continue;
			}

			float width = 150.0F * graffiti.sizeScale * scale;
			float height = width / graffiti.aspect;
			float x = entry.screenX + 60.0F;
			float y = entry.screenY - 55.0F - slot * 36.0F - rise * 55.0F;

			x = Math.max(width / 2.0F + 4.0F, Math.min(sr.getScaledWidth() - width / 2.0F - 4.0F, x));
			y = Math.max(height / 2.0F + 4.0F, Math.min(sr.getScaledHeight() - height / 2.0F - 4.0F, y));

			drawSprite(texture, x, y, width, height, alpha, graffiti.angle + (float) Math.sin(progress * Math.PI * 2.0F) * 4.0F);

			slot++;
		}
	}

	private void drawSprite(int texture, float centerX, float centerY, float width, float height, float alpha, float angle) {

		ScaledResolution sr = new ScaledResolution(mc);

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableAlpha();
		GlStateManager.disableDepth();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0D, sr.getScaledWidth(), sr.getScaledHeight(), 0.0D, -1000.0D, 1000.0D);

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GlStateManager.bindTexture(texture);
		GL11.glTranslatef(centerX, centerY, 0.0F);
		GL11.glRotatef(angle, 0.0F, 0.0F, 1.0F);

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex3f(-width / 2.0F, -height / 2.0F, 0.0F);
		GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex3f(width / 2.0F, -height / 2.0F, 0.0F);
		GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex3f(width / 2.0F, height / 2.0F, 0.0F);
		GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex3f(-width / 2.0F, height / 2.0F, 0.0F);
		GL11.glEnd();

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();

		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopAttrib();
	}

	private static int getGraffitiTexture(int index) {

		if(TEXTURE_IDS[index] != -1) {
			return TEXTURE_IDS[index];
		}

		try {

			InputStream stream = TargetInfoMod.class.getResourceAsStream(TEXTURE_PATHS[index]);

			if(stream == null) {
				return 0;
			}

			BufferedImage image = ImageIO.read(stream);
			stream.close();

			int width = image.getWidth();
			int height = image.getHeight();
			int[] pixels = new int[width * height];
			image.getRGB(0, 0, width, height, pixels, 0, width);

			ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					int pixel = pixels[y * width + x];
					buffer.put((byte) (pixel >> 16 & 255));
					buffer.put((byte) (pixel >> 8 & 255));
					buffer.put((byte) (pixel & 255));
					buffer.put((byte) (pixel >> 24 & 255));
				}
			}

			buffer.flip();

			int texture = GL11.glGenTextures();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

			TEXTURE_IDS[index] = texture;

			return texture;
		} catch(Exception e) {
			return 0;
		}
	}

	private static float easeOutCubic(float value) {
		return 1.0F - (float) Math.pow(1.0F - value, 3);
	}

	private static float easeInOutCubic(float value) {
		return value < 0.5F ? 4.0F * value * value * value : 1.0F - (float) Math.pow(-2.0F * value + 2.0F, 3.0D) / 2.0F;
	}

	// 原版 TargetInfo 的 EaseBackIn(320ms, endPoint 1.0, easeAmount 2.0)：先冲过头再回弹
	private static float easeBackIn(float value) {
		double t = Math.max(0.0D, Math.min(1.0D, value));
		double shrink = 3.0D;
		double result = 1.0D + shrink * Math.pow(t - 1.0D, 3.0D) + 2.0D * Math.pow(t - 1.0D, 2.0D);
		return (float) Math.max(0.0D, result);
	}

	private boolean isVisible(AbstractClientPlayer entity) {

		if(mc.theWorld == null) {
			return true;
		}

		Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
		Vec3 target = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
		MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyes, target, false, true, false);

		return hit == null || hit.hitVec.distanceTo(eyes) > eyes.distanceTo(target) - 0.35D;
	}

	private void project(TargetEntry entry, AbstractClientPlayer entity, float partialTicks) {

		ScaledResolution sr = new ScaledResolution(mc);

		double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + entity.height * 0.55D;
		double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

		double dx = x - mc.thePlayer.posX;
		double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
		double dz = z - mc.thePlayer.posZ;

		double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
		double pitch = Math.toRadians(mc.thePlayer.rotationPitch);
		double cos = Math.cos(yaw);
		double sin = Math.sin(yaw);

		double right = -(dx * cos + dz * sin);
		double forward = -dx * sin + dz * cos;
		double up = dy * Math.cos(pitch) - forward * Math.sin(pitch);
		double depth = forward * Math.cos(pitch) - dy * Math.sin(pitch);

		if(depth <= 0.15D) {
			entry.screenX = Float.NaN;
			return;
		}

		double scale = (sr.getScaledHeight() / 2.0D) / Math.tan(Math.toRadians(mc.gameSettings.fovSetting) / 2.0D);

		entry.screenX = (float) (sr.getScaledWidth() / 2.0D + right * scale / depth);
		entry.screenY = (float) (sr.getScaledHeight() / 2.0D - up * scale / depth);
	}

	private void renderEntry(TargetEntry entry, AbstractClientPlayer entity, float partialTicks) {

		// 动画恢复为未修改之前的 EaseBackIn：alpha = 动画值，scale = 2 - 动画值
		float eased = easeBackIn(entry.progress);
		float animationAlpha = Math.max(0.0F, Math.min(1.0F, eased));
		float alpha = animationAlpha * entry.occlusionAlpha;

		if(alpha <= 0.01F) {
			return;
		}

		// NanoVG 画进 FBO 的内容（文字/图标）不吃 glColor 的 alpha，所以把透明度按台阶烘进纹理，
		// 只在跨台阶时重画一次，剩余部分仍然交给 glColor 补齐。
		int alphaStep = Math.round(alpha * ALPHA_STEPS);

		if(alphaStep < 1) {
			alphaStep = 1;
		}

		entry.bakedAlpha = alphaStep / (float) ALPHA_STEPS;
		entry.renderAlpha = alpha;
		entry.bakedAlphaStep = alphaStep;

		RenderManager renderManager = mc.getRenderManager();

		double entityX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		double entityY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
		double entityZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

		double cameraYaw = Math.toRadians(renderManager.playerViewY);
		double cameraRightX = -Math.cos(cameraYaw);
		double cameraRightZ = -Math.sin(cameraYaw);

		int panelWidth = entry.getCachedPanelWidth();

		if(!renderToTexture(entry, panelWidth)) {
			return;
		}

		// 进入时从 2 倍收到 1 倍（EaseBackIn 的过冲），再乘菜单里的"面板大小"
		float scaleFactor = Math.max(0.05F, 2.0F - eased) * sizeSetting.getValueFloat();
		float quadHeight = BASE_PANEL_HEIGHT * scaleFactor;
		float quadWidth = quadHeight * ((float) panelWidth / 46.0F);
		double offset = 0.50D + quadWidth / 2.0D;

		int texWidth = panelWidth + PAD_LEFT * 2;
		int texHeight = 46 + PAD_TOP + PAD_BOTTOM;
		float fullWidth = quadWidth * (texWidth / (float) panelWidth);
		float fullHeight = quadHeight * (texHeight / 46.0F);
		float left = -(PAD_LEFT + panelWidth / 2.0F) * (fullWidth / texWidth);
		float right = (texWidth - PAD_LEFT - panelWidth / 2.0F) * (fullWidth / texWidth);
		float top = (PAD_TOP + 23.0F) * (fullHeight / texHeight);
		float bottom = -(texHeight - PAD_TOP - 23.0F) * (fullHeight / texHeight);

		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableAlpha();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.enableTexture2D();
		GlStateManager.resetColor();
		GlStateManager.color(1.0F, 1.0F, 1.0F, entry.renderAlpha / entry.bakedAlpha);
		GlStateManager.bindTexture(entry.framebuffer.texture());

		GlStateManager.translate(entityX - renderManager.viewerPosX + cameraRightX * offset,
				entityY + entity.height * 0.52D - renderManager.viewerPosY,
				entityZ - renderManager.viewerPosZ + cameraRightZ * offset);
		GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex3d(left, top, 0.0D);
		GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex3d(right, top, 0.0D);
		GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex3d(right, bottom, 0.0D);
		GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex3d(left, bottom, 0.0D);
		GL11.glEnd();

		GlStateManager.depthMask(true);
		GlStateManager.enableAlpha();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.resetColor();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private boolean renderToTexture(TargetEntry entry, int panelWidth) {

		// 性能：面板内容没有变化时直接复用上一帧的 FBO，不再每帧重绘 1344x720 纹理
		if(entry.particles.isEmpty()) {

			String signature = getTextureSignature(entry, panelWidth);

			if(entry.framebuffer != null && signature.equals(entry.textureSignature)) {
				return true;
			}

			entry.textureSignature = signature;
		} else {
			entry.textureSignature = null;
		}

		NanoVGManager nvgManager = Mix.getInstance().getNanoVGManager();
		long context = nvgManager.getContext();

		// 纹理分辨率跟着"面板大小"走：面板在世界里被放大多少倍，FBO 就多画多少像素，
		// 否则拉到 6 倍也只是把一张小图硬拉大，字全是糊的。
		// 注意必须是整数倍——nvgBeginFrame 的 pixelRatio 和纹理像素尺寸用的是同一个值，
		// 两边一旦对不齐（比如 4 和 5.33），画面会被硬裁掉一块。
		int pixelRatio = Math.max(4, Math.min(8, Math.round(sizeSetting.getValueFloat() * 1.6F)));

		while(pixelRatio > 4 && ((panelWidth + PAD_LEFT * 2) * pixelRatio > 2048
				|| (46 + PAD_TOP + PAD_BOTTOM) * pixelRatio > 2048)) {
			pixelRatio--;
		}

		float pixelRatioF = pixelRatio;
		int textureWidth = (panelWidth + PAD_LEFT * 2) * pixelRatio;
		int textureHeight = (46 + PAD_TOP + PAD_BOTTOM) * pixelRatio;

		if(entry.framebuffer == null || entry.framebufferWidth != textureWidth || entry.framebufferHeight != textureHeight) {

			if(entry.framebuffer != null) {
				NanoVGGL2.nvgluDeleteFramebuffer(context, entry.framebuffer);
			}

			entry.framebuffer = NanoVGGL2.nvgluCreateFramebuffer(context, textureWidth, textureHeight, 0);
			entry.framebufferWidth = textureWidth;
			entry.framebufferHeight = textureHeight;
			entry.textureSignature = null;
		}

		if(entry.framebuffer == null) {
			return false;
		}

		int oldX = this.getX();
		int oldY = this.getY();
		int oldWidth = this.getWidth();
		int oldHeight = this.getHeight();

		int target = ScreenTarget.capture();

		NanoVGGL2.nvgluBindFramebuffer(context, entry.framebuffer);
		GL11.glViewport(0, 0, textureWidth, textureHeight);
		GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		this.setX(PAD_LEFT);
		this.setY(PAD_TOP);
		this.setWidth(panelWidth);
		this.setHeight(46);

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

		boolean drawn = true;

		// 别的客户端（比如热注入进来的外挂）改了 GL 状态时，这里随时可能抛异常。
		// 不管怎样都要把 FBO 解绑、视口和 HUD 坐标恢复回去，不然整个渲染就废了。
		try {
			NanoVG.nvgBeginFrame(context, panelWidth + PAD_LEFT * 2, 46 + PAD_TOP + PAD_BOTTOM, pixelRatioF);
			NanoVG.nvgGlobalAlpha(context, entry.bakedAlpha);
			drawPanelContent(entry, panelWidth);
			NanoVG.nvgEndFrame(context);
		} catch(Throwable t) {
			entry.textureSignature = null;
			drawn = false;
			reportRenderError(t);
		} finally {
			GL11.glPopAttrib();

			this.setX(oldX);
			this.setY(oldY);
			this.setWidth(oldWidth);
			this.setHeight(oldHeight);

			ScreenTarget.restore(target);
		}

		return drawn;
	}

	// 同一个异常只打一次日志，避免每帧刷屏
	private void reportRenderError(Throwable t) {

		String key = t.getClass().getName() + ": " + t.getMessage();

		if(lastRenderError != null && lastRenderError.equals(key)) {
			return;
		}

		lastRenderError = key;

		MixLogger.error("TargetInfo render failed: " + key);

		if(t instanceof Exception) {
			MixLogger.error("TargetInfo render stack", (Exception) t);
		}
	}

	private String getTextureSignature(TargetEntry entry, int panelWidth) {

		Option theme = GlobalSettingsMod.getInstance().getModThemeSetting().getOption();
		AccentColor currentColor = Mix.getInstance().getColorManager().getCurrentColor();

		return panelWidth + "|" + entry.name + "|" + (entry.head == null ? "null" : entry.head.toString()) + "|"
				+ Math.round(entry.healthAnimation.getValue() * 100.0F) + "|"
				+ Math.round(entry.armorAnimation.getValue() * 100.0F) + "|"
				+ Math.round(entry.hurt * 100.0F) + "|"
				+ theme.getName() + "|" + currentColor.getColor1().getRGB() + "|" + currentColor.getColor2().getRGB() + "|"
				+ entry.bakedAlphaStep + "|" + Math.round(sizeSetting.getValueFloat() * 100.0F);
	}

	private static final int[] GRAFFITI_IMAGES = new int[]{-1, -1, -1, -1};

	private void renderGraffiti(TargetEntry entry, float partialTicks, long now) {

		RenderManager renderManager = mc.getRenderManager();

		for(GraffitiInstance graffiti : entry.graffitiList) {

			float progress = (now - graffiti.startTime) / (float) GRAFFITI_TIME;

			if(progress < 0.0F || progress >= 1.0F) {
				continue;
			}

			float alpha;
			float scale;
			float rise;

			if(progress < 0.2F) {
				float q = progress / 0.2F;
				alpha = q;
				scale = q < 0.5F ? 0.5F + 0.7F * easeOutCubic(q * 2.0F) : 1.2F - 0.2F * easeInOutCubic((q - 0.5F) * 2.0F);
				rise = 0.35F * easeOutCubic(q);
			} else if(progress < 0.7F) {
				float q = (progress - 0.2F) / 0.5F;
				alpha = 1.0F;
				scale = 1.0F;
				rise = 0.35F + 0.25F * q;
			} else {
				float q = (progress - 0.7F) / 0.3F;
				alpha = 1.0F - q;
				scale = 1.0F - 0.1F * q;
				rise = 0.60F + 0.35F * q;
			}

			if(alpha <= 0.01F || !ensureGraffitiFramebuffer(graffiti)) {
				continue;
			}

			float height = 0.55F * graffiti.sizeScale * scale;
			float width = height * graffiti.aspect;

			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
			GlStateManager.disableLighting();
			GlStateManager.disableCull();
			GlStateManager.disableAlpha();
			GlStateManager.disableDepth();
			GlStateManager.enableTexture2D();
			GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
			GlStateManager.bindTexture(graffiti.framebuffer.texture());

			GlStateManager.translate(graffiti.x - renderManager.viewerPosX,
					graffiti.y + rise - renderManager.viewerPosY,
					graffiti.z - renderManager.viewerPosZ);
			GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
			GlStateManager.rotate(graffiti.angle, 0.0F, 0.0F, 1.0F);

			GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex3d(-width / 2.0F, -height / 2.0F, 0.0D);
			GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex3d(width / 2.0F, -height / 2.0F, 0.0D);
			GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex3d(width / 2.0F, height / 2.0F, 0.0D);
			GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex3d(-width / 2.0F, height / 2.0F, 0.0D);
			GL11.glEnd();

			GlStateManager.enableDepth();
			GlStateManager.enableAlpha();
			GlStateManager.enableCull();
			GlStateManager.enableLighting();
			GlStateManager.disableBlend();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.popMatrix();
		}
	}

	private boolean ensureGraffitiFramebuffer(GraffitiInstance graffiti) {

		if(graffiti.framebuffer != null) {
			return true;
		}

		int image = getGraffitiImage(graffiti.index);

		if(image == 0) {
			return false;
		}

		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		long context = nvg.getContext();

		graffiti.framebuffer = NanoVGGL2.nvgluCreateFramebuffer(context, 768, 480, 0);

		if(graffiti.framebuffer == null) {
			return false;
		}

		int target = ScreenTarget.capture();

		NanoVGGL2.nvgluBindFramebuffer(context, graffiti.framebuffer);
		GL11.glViewport(0, 0, 768, 480);
		GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		NanoVG.nvgBeginFrame(context, 256.0F, 160.0F, 3.0F);

		NVGPaint paint = NVGPaint.create();
		NanoVG.nvgImagePattern(context, 0.0F, 0.0F, 256.0F, 160.0F, 0.0F, image, 1.0F, paint);
		NanoVG.nvgBeginPath(context);
		NanoVG.nvgRect(context, 0.0F, 0.0F, 256.0F, 160.0F);
		NanoVG.nvgFillPaint(context, paint);
		NanoVG.nvgFill(context);

		NanoVG.nvgEndFrame(context);
		GL11.glPopAttrib();

		ScreenTarget.restore(target);

		return true;
	}

	private static int getGraffitiImage(int index) {

		if(GRAFFITI_IMAGES[index] != -1) {
			return GRAFFITI_IMAGES[index];
		}

		try {

			InputStream stream = TargetInfoMod.class.getResourceAsStream(TEXTURE_PATHS[index]);

			if(stream == null) {
				return 0;
			}

			BufferedImage image = ImageIO.read(stream);
			stream.close();

			int width = image.getWidth();
			int height = image.getHeight();
			int[] pixels = new int[width * height];
			image.getRGB(0, 0, width, height, pixels, 0, width);

			ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

			for(int pixel : pixels) {
				buffer.put((byte) (pixel >> 16 & 255));
				buffer.put((byte) (pixel >> 8 & 255));
				buffer.put((byte) (pixel & 255));
				buffer.put((byte) (pixel >> 24 & 255));
			}

			buffer.flip();

			int imageId = NanoVG.nvgCreateImageRGBA(Mix.getInstance().getNanoVGManager().getContext(), width, height, 0, buffer);
			GRAFFITI_IMAGES[index] = imageId;

			return imageId;
		} catch(Exception e) {
			return 0;
		}
	}

	private int getPanelWidth(String name) {

		float nameWidth = this.getTextWidth(name, 10.2F, Fonts.MEDIUM);
		int width = 96;

		if(nameWidth + 52F > width) {
			width = (int) Math.ceil(nameWidth + 52F);
		}

		return width;
	}

	private void drawPanel(TargetEntry entry) {
		drawPanelContent(entry, getPanelWidth(entry.name));
	}

	private void drawPanelContent(TargetEntry entry, int width) {

		this.drawBackground(width, 46);

		Random random = new Random(entry.shakeSeed + (System.currentTimeMillis() / 25L));
		float shake = hitEffectSetting.isToggled() ? entry.hurt * entry.hurt : 0.0F;
		float shakeX = (random.nextFloat() - 0.5F) * 20.0F * shake;
		float shakeY = (random.nextFloat() - 0.5F) * 16.0F * shake;

		this.drawPlayerHead(entry.head, 5 + shakeX, 5 + shakeY, 36, 36, 6);
		this.drawText(entry.name, 45.5F, 8F, 10.2F, Fonts.MEDIUM);

		this.drawText(Icon.HEART_FILL, 52, 26.5F, 9, Fonts.ICON);
		this.drawArc(56.5F, 30.5F, 9F, -90F, -90F + 360, 1.6F, this.getFontColor(120));
		this.drawArc(56.5F, 30.5F, 9F, -90F, -90F + (18 * entry.healthAnimation.getValue()), 1.6F);

		this.drawText(Icon.SHIELD_FILL, 76F, 26.5F, 9, Fonts.ICON);
		this.drawArc(80.5F, 30.5F, 9F, -90F, -90F + 360, 1.6F, this.getFontColor(120));
		this.drawArc(80.5F, 30.5F, 9F, -90F, -90F + (18 * entry.armorAnimation.getValue()), 1.6F);

		for(BloodParticle particle : entry.particles) {

			float alpha = Math.max(0.0F, Math.min(1.0F, particle.life / particle.maxLife)) * Math.max(0.0F, Math.min(1.0F, particle.fade));
			int a = (int) (alpha * 235.0F);

			if(particle.glow) {
				this.drawRect(particle.x - particle.size * 0.35F, particle.y - particle.size * 0.35F,
						particle.size * 1.7F, particle.size * 1.7F,
						new Color((int) (particle.r * 255.0F), (int) (particle.g * 255.0F), (int) (particle.b * 255.0F), a / 3));
			}

			Color base = particle.glow ? new Color((int) (particle.r * 255.0F), (int) (particle.g * 255.0F), (int) (particle.b * 255.0F), a)
					: new Color(190, 20, 25, a);
			// 拖尾：沿速度反向拉一小段，强化"迸发"的速度感
			float tailScale = 0.012F;

			if(particle.glow) {
				this.drawRect(particle.x - particle.vx * tailScale, particle.y - particle.vy * tailScale,
						particle.size * 0.75F, particle.size * 0.75F,
						new Color((int) (particle.r * 255.0F), (int) (particle.g * 255.0F), (int) (particle.b * 255.0F), a / 2));
			}

			this.drawRect(particle.x, particle.y, particle.size, particle.size, base);

			if(particle.glow) {
				this.drawRect(particle.x + particle.size * 0.25F, particle.y + particle.size * 0.25F,
						particle.size * 0.5F, particle.size * 0.5F, new Color(255, 255, 255, a));
			}
		}

		this.setWidth(width);
		this.setHeight(46);
	}

	private void removeEntry(AbstractClientPlayer player) {

		TargetEntry entry = entries.remove(player);

		if(entry != null) {
			releaseEntry(entry);
		}
	}

	private void releaseEntry(TargetEntry entry) {

		if(entry.framebuffer != null) {
			NanoVGManager nvgManager = Mix.getInstance().getNanoVGManager();
			NanoVGGL2.nvgluDeleteFramebuffer(nvgManager.getContext(), entry.framebuffer);
			entry.framebuffer = null;
		}

		entry.graffitiList.clear();
	}

	private class GraffitiInstance {

		private final int index;
		private final float aspect;
		private final long startTime;
		private final double x, y, z;
		private final float angle;
		private final float size;
		private final float sizeScale;
		private NVGLUFramebuffer framebuffer;

		private GraffitiInstance(int index, float aspect, long startTime, double x, double y, double z, float angle, float size, float sizeScale) {
			this.index = index;
			this.aspect = aspect;
			this.startTime = startTime;
			this.x = x;
			this.y = y;
			this.z = z;
			this.angle = angle;
			this.size = size;
			this.sizeScale = sizeScale;
		}
	}

	private class TargetEntry {

		private AbstractClientPlayer entity;
		private String name;
		private ResourceLocation head;
		private float health, armor;
		private final SimpleAnimation healthAnimation = new SimpleAnimation();
		private final SimpleAnimation armorAnimation = new SimpleAnimation();
		private final List<BloodParticle> particles = new ArrayList<BloodParticle>();
		private final List<GraffitiInstance> graffitiList = new ArrayList<GraffitiInstance>();

		private long lastHitTime;
		private long lastUpdate;
		private long shakeStart;
		private long shakeSeed;
		private float progress;
		private boolean hiding;
		private boolean visible = true;
		private boolean wasVisible = true;
		private long nextVisibleCheck;
		// 遮挡遮掩用的乘数（1.0 = 没有遮挡，0.35 = 被方块挡住）
		private float occlusionAlpha = 1.0F;
		// 烘进纹理的透明度台阶与这一帧真正想要的透明度
		private int bakedAlphaStep = -1;
		private float bakedAlpha = 1.0F;
		private float renderAlpha = 1.0F;
		private float hurt;

		private long lastParticleUpdate;
		private int lastHurtTime;
		private long burstCounter;
		private final List<Long> bursts = new ArrayList<Long>();

		private float screenX = Float.NaN;
		private float screenY = Float.NaN;

		private NVGLUFramebuffer framebuffer;
		private int framebufferWidth = -1;
		private int framebufferHeight = -1;
		private String textureSignature;

		private String panelWidthName;
		private int panelWidthCache;

		private TargetEntry(AbstractClientPlayer entity) {
			this.entity = entity;
			this.lastHitTime = System.currentTimeMillis();
			this.lastUpdate = this.lastHitTime;
			this.updateData();
		}

		private void updateData() {

			if(entity == null) {
				return;
			}

			this.name = entity.getName();
			this.head = entity.getLocationSkin();
			this.health = Math.min(entity.getHealth(), 20.0F);
			this.armor = Math.min(entity.getTotalArmorValue(), 20);
		}

		private int getCachedPanelWidth() {

			if(panelWidthName == null || !panelWidthName.equals(name)) {
				panelWidthName = name;
				panelWidthCache = TargetInfoMod.this.getPanelWidth(name);
			}

			return panelWidthCache;
		}

		private void spawnGraffiti(AbstractClientPlayer entity) {

			int index = (int) (Math.random() * WORDS.length);

			RenderManager renderManager = mc.getRenderManager();
			double cameraYaw = Math.toRadians(renderManager.playerViewY);
			double cameraRightX = -Math.cos(cameraYaw);
			double cameraRightZ = -Math.sin(cameraYaw);

			int panelWidth = getPanelWidth(entity.getName());
			float panelHeight = BASE_PANEL_HEIGHT * sizeSetting.getValueFloat();
			float panelWidthWorld = panelHeight * (panelWidth / 46.0F);
			double panelOffset = 0.50D + panelWidthWorld / 2.0D;

			if(graffitiList.size() >= 4) {
				graffitiList.remove(0);
			}

			int slot = graffitiList.size();

			double x = entity.posX + cameraRightX * panelOffset + (Math.random() - 0.5D) * 0.6D;
			double y = entity.posY + entity.height * 0.52D + 0.30D + slot * 0.45D + (Math.random() - 0.5D) * 0.10D;
			double z = entity.posZ + cameraRightZ * panelOffset + (Math.random() - 0.5D) * 0.6D;
			float angle = (float) ((Math.random() - 0.5D) * 14.0D);
			float sizeScale = 0.85F + (float) Math.random() * 0.3F;

			graffitiList.add(new GraffitiInstance(index, ASPECTS[index], System.currentTimeMillis(), x, y, z, angle, 0.55F, sizeScale));
		}

		private void checkHurt(AbstractClientPlayer entity) {

			if(entity == null) {
				return;
			}

			if(entity.hurtTime == 10 && lastHurtTime != 10 && hitEffectSetting.isToggled()) {
				spawnSparks();
			}

			lastHurtTime = entity.hurtTime;
		}

		private void spawnSparks() {

			burstCounter++;
			bursts.add(burstCounter);

			// 允许同时存在三拨：超出时只把最老的一拨标成 dying，让它自己慢慢淡下去
			while(bursts.size() > MAX_BURSTS) {
				long oldest = bursts.remove(0);

				for(BloodParticle existing : particles) {
					if(existing.burst == oldest) {
						existing.dying = true;
						existing.fadeSpeed = 1000.0F / REPLACE_FADE_MS;
					}
				}
			}

			// 迸发：30-40 个火花从面板中心瞬间炸开，速度高，之后各自自然消散
			int sparkCount = 30 + (int) (Math.random() * 11.0D);

			for(int i = 0; i < sparkCount; i++) {
				BloodParticle particle = new BloodParticle();
				particle.burst = burstCounter;
				double direction = Math.random() * Math.PI * 2.0D;
				double startRadius = Math.random() * 3.0D;
				double speed = 150.0D + Math.random() * 130.0D;
				particle.x = 23.0F + (float) (Math.cos(direction) * startRadius);
				particle.y = 23.0F + (float) (Math.sin(direction) * startRadius);
				particle.vx = (float) (Math.cos(direction) * speed);
				particle.vy = (float) (Math.sin(direction) * speed);
				particle.maxLife = SPARK_LIFE_BASE + (float) Math.random() * SPARK_LIFE_RANDOM;
				particle.life = particle.maxLife;
				particle.size = 3.0F + (float) Math.random() * 2.0F;
				particle.glow = true;

				int index = (int) (Math.random() * 4.0D);

				if(index == 0) {
					particle.r = 1.0F; particle.g = 0.85F; particle.b = 0.2F;
				} else if(index == 1) {
					particle.r = 0.35F; particle.g = 0.85F; particle.b = 1.0F;
				} else if(index == 2) {
					particle.r = 1.0F; particle.g = 0.35F; particle.b = 0.35F;
				} else {
					particle.r = 1.0F; particle.g = 1.0F; particle.b = 1.0F;
				}

				particles.add(particle);
			}
		}

		private void spawnBlood() {

			for(int i = 0; i < 40; i++) {
				BloodParticle particle = new BloodParticle();
				particle.x = 18.0F + (float) (Math.random() * 10.0D);
				particle.y = 18.0F + (float) (Math.random() * 10.0D);
				particle.vx = (float) ((Math.random() - 0.5D) * 260.0D);
				particle.vy = (float) (-40.0D - Math.random() * 140.0D);
				particle.maxLife = 0.35F + (float) Math.random() * 0.35F;
				particle.life = particle.maxLife;
				particle.size = 2.0F + (float) Math.random() * 3.0F;
				particle.r = 0.75F; particle.g = 0.10F; particle.b = 0.12F;
				particles.add(particle);
			}
		}

		private void updateBlood() {

			long now = System.currentTimeMillis();
			float delta = (now - lastParticleUpdate) / 1000.0F;
			lastParticleUpdate = now;

			if(delta <= 0.0F || delta > 0.1F) {
				delta = 1.0F / 60.0F;
			}

			Iterator<BloodParticle> iterator = particles.iterator();

			while(iterator.hasNext()) {

				BloodParticle particle = iterator.next();
				particle.life -= delta;

				if(particle.dying) {
					particle.fade -= delta * particle.fadeSpeed;
				}

				if(particle.life <= 0.0F || particle.fade <= 0.0F) {
					iterator.remove();
					continue;
				}

				// 迸发手感：初速瞬间拉满后快速衰减（与帧率无关的指数阻尼），只留一点点上飘
				float drag = (float) Math.pow(0.015D, delta);
				particle.x += particle.vx * delta;
				particle.y += particle.vy * delta;
				particle.vx *= drag;
				particle.vy *= drag;
				particle.vy += 26.0F * delta;
			}

			// 一拨火花全部消散后把它的编号从队列里摘掉，否则队列会越攒越长、把新的一拨误判成最老
			if(!bursts.isEmpty()) {
				for(int i = bursts.size() - 1; i >= 0; i--) {
					long id = bursts.get(i);
					boolean alive = false;

					for(BloodParticle particle : particles) {
						if(particle.burst == id) {
							alive = true;
							break;
						}
					}

					if(!alive) {
						bursts.remove(i);
					}
				}
			}
		}
	}

	private static class BloodParticle {
		private float x, y, z, vx, vy, vz, life, maxLife, size;
		private float r, g, b;
		private boolean glow;
		private long burst;
		private boolean dying;
		private float fade = 1.0F;
		private float fadeSpeed = 3.0F;
	}
}
