package mix.client.mixui.utils.render;

import org.lwjgl.opengl.GL11;

import mix.client.mixui.injection.interfaces.IMixinMinecraft;
import mix.client.mixui.injection.interfaces.IMixinShaderGroup;
import mix.client.mixui.utils.buffer.ScreenTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.util.ResourceLocation;

/**
 * 整屏后处理模糊。
 *
 * <p>底层仍然是 {@code shaders/post/blurArea.json} 的两趟 separable blur（水平一趟、垂直一趟），
 * 半径和纵向遮罩（{@code MaskY} / {@code MaskStrength} / {@code MaskInvert}）的观感与以前一致。</p>
 *
 * <p>Khi OpenGlHelper tắt framebuffer (OptiFine Fast Render), lớp này bỏ qua blur để
 * tránh bind texture không tồn tại. Khi framebuffer khả dụng, một texture nguồn riêng
 * được copy từ mục tiêu đang vẽ trước khi đưa vào ShaderGroup.</p>
 */
public class BlurUtils {

	private static final ResourceLocation BLUR_GROUP = new ResourceLocation("shaders/post/blurArea.json");

	private static Minecraft mc = Minecraft.getMinecraft();

	private static ShaderGroup blurShader;

	/**
	 * 本类自带的「当前帧」纹理，只当 ShaderGroup 的输入用。
	 * 没有深度附件，也从不被当作绘制目标（只采样）。
	 */
	private static Framebuffer source;

	private static int sourceWidth;
	private static int sourceHeight;

	/** 遮罩起点（屏幕纵向比例，0 = 顶）。起点以上是「全模糊」。 */
	private static float maskStart = 0F;

	/** 遮罩终点。终点以下是「原图」，中间按 smoothstep 过渡。 */
	private static float maskEnd = 1F;

	/** 遮罩最大混合量（0..1）。 */
	private static float maskStrength = 0F;

	/** texCoord 朝向修正：1 = 上下翻转。 */
	private static float maskInvert = 0F;

	/** 一次性开关：设了遮罩之后，只在下一帧的 drawBlurScreen 里用一次。 */
	private static boolean maskPending = false;

	/**
	 * 设置「渐变磨砂」遮罩，只对紧接着的一次 {@link #drawBlurScreen(float)} 生效。
	 *
	 * <p>纵向语义（比例，0 = 屏幕顶）：</p>
	 * <ul>
	 *   <li>{@code from} 以上：完全用模糊图（磨砂最强）</li>
	 *   <li>{@code from} → {@code to}：smoothstep 过渡，越往下越接近原图</li>
	 *   <li>{@code to} 以下：完全用原图（磨砂化开，看不出边界）</li>
	 * </ul>
	 *
	 * @param from     起点比例
	 * @param to       终点比例，必须大于 {@code from}
	 * @param strength 起点处的模糊量（0..1）。1 = 完全模糊图
	 */
	public static void setFrostMask(float from, float to, float strength) {
		maskStart = Math.max(0F, Math.min(1F, from));
		maskEnd = Math.max(maskStart + 0.001F, Math.min(1F, to));
		maskStrength = Math.max(0F, Math.min(1F, strength));
		maskPending = true;
	}

	/** 关掉遮罩（下一次 drawBlurScreen 走旧的整屏模糊）。 */
	public static void clearFrostMask() {
		maskStrength = 0F;
		maskPending = false;
	}

	/** 调试用：翻转遮罩纵向朝向（不同 vertex shader 的 texCoord 起点不一样）。 */
	public static void setFrostInvert(boolean invert) {
		maskInvert = invert ? 1F : 0F;
	}

	/**
	 * 重建输入纹理与 ShaderGroup。
	 *
	 * <p>只在分辨率变化时需要。旧的帧缓冲必须删掉：不删就会在显存里留一张整屏纹理，
	 * 换几次分辨率/全屏就吃满。ShaderGroup 与仓库里其它调用点一致，直接换新（旧的交给 GC，
	 * 它内部的 FBO 数量固定且很小）。</p>
	 */
	private static void rebuild() {

		int width = Math.max(1, mc.displayWidth);
		int height = Math.max(1, mc.displayHeight);

		if (source != null && (sourceWidth != width || sourceHeight != height)) {
			source.deleteFramebuffer();
			source = null;
			blurShader = null;
		}

		try {
			if (source == null) {
				/* 只需要那张颜色纹理当输入；深度/模板都不用，省显存也省一次清屏 */
				source = new Framebuffer(width, height, false);
				source.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);

				if (source.framebufferTexture <= 0) {
					source.deleteFramebuffer();
					source = null;
					blurShader = null;
					return;
				}

				GlStateManager.bindTexture(source.framebufferTexture);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
				GlStateManager.bindTexture(0);
			}

			if (blurShader == null) {
				blurShader = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(),
						source, BLUR_GROUP);
				blurShader.createBindFramebuffers(width, height);
			}

			sourceWidth = width;
			sourceHeight = height;
		} catch (Throwable throwable) {
			/* 驱动编不了这两趟 shader 时不画模糊，而不是把整个界面拖下水 */
			if (source != null) {
				try {
					source.deleteFramebuffer();
				} catch (Throwable ignored) {
				}
				source = null;
			}
			blurShader = null;
			sourceWidth = 0;
			sourceHeight = 0;
		}
	}

	public static void drawBlurScreen(float radius) {

		if (!OpenGlHelper.isFramebufferEnabled()) {
			clearFrostMask();
			return;
		}

		ScaledResolution sr = new ScaledResolution(mc);

		int factor = sr.getScaleFactor();
		int displayWidth = Math.max(1, mc.displayWidth);
		int displayHeight = Math.max(1, mc.displayHeight);

		if (blurShader == null || source == null
				|| sourceWidth != displayWidth || sourceHeight != displayHeight) {
			int restore = ScreenTarget.capture();
			try {
				rebuild();
			} finally {
				ScreenTarget.restore(restore);
			}
		}

		if (!isReady()) {
			maskPending = false;
			return;
		}

		int target = ScreenTarget.capture();

		try {
			copyLiveFrame(displayWidth, displayHeight);

			float halfScale = factor / 2.0F;
			Shader horizontal = ((IMixinShaderGroup) blurShader).getListShaders().get(0);
			Shader vertical = ((IMixinShaderGroup) blurShader).getListShaders().get(1);

			setUniform(horizontal.getShaderManager(), "BlurXY", 0F, 0F);
			setUniform(vertical.getShaderManager(), "BlurXY", 0F, 0F);
			setUniform(horizontal.getShaderManager(), "BlurCoord", displayWidth * halfScale, displayHeight * halfScale);
			setUniform(vertical.getShaderManager(), "BlurCoord", displayWidth * halfScale, displayHeight * halfScale);
			setUniform(horizontal.getShaderManager(), "Radius", radius);
			setUniform(vertical.getShaderManager(), "Radius", radius);

			applyFrostUniforms();
			blurShader.loadShaderGroup(((IMixinMinecraft) mc).getTimer().renderPartialTicks);
		} catch (Throwable throwable) {
			blurShader = null;
			if (source != null) {
				try {
					source.deleteFramebuffer();
				} catch (Throwable ignored) {
				}
				source = null;
			}
			sourceWidth = 0;
			sourceHeight = 0;
		} finally {
			ScreenTarget.restore(target);
			maskPending = false;
		}
	}

	private static boolean isReady() {
		return blurShader != null && source != null && source.framebufferTexture > 0
				&& ((IMixinShaderGroup) blurShader).getListShaders().size() >= 2;
	}

	private static void setUniform(ShaderManager manager, String name, float... values) {
		if (manager != null && manager.getShaderUniform(name) != null) {
			manager.getShaderUniform(name).set(values);
		}
	}

	/**
	 * 把「这一帧现在屏幕上的内容」拷进 {@link #source}。
	 *
	 * <p>读的是当前绑定的帧缓冲的 read buffer：Fast Render 下是窗口（0），
	 * 正常情况下是主帧缓冲，两者尺寸都等于 displayWidth x displayHeight，
	 * 所以一次整幅拷贝就够，不需要判断游戏到底用了哪条路径。</p>
	 */
	private static void copyLiveFrame(int width, int height) {

		if (source == null || source.framebufferTexture <= 0) {
			throw new IllegalStateException("Blur source texture is not available");
		}

		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.bindTexture(source.framebufferTexture);
		try {
			GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
		} finally {
			GlStateManager.bindTexture(0);
			GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		}
	}

	/**
	 * 把遮罩参数写进两趟 pass 的 shader，然后把「一次性」标记复位。
	 *
	 * <p>两趟都得写：水平那一趟和垂直那一趟各持有一份独立的 uniform，
	 * 只写第一趟的话第二趟会拿 json 里的默认值，遮罩被算成两种不同结果。</p>
	 *
	 * <p>uniform 可能不存在（玩家换回了不带遮罩的旧 fsh），所以逐个判空，</p>
	 */
	private static void applyFrostUniforms() {

		float strength = maskPending ? maskStrength : 0F;

		for (int pass = 0; pass < 2; pass++) {

			Shader shader = ((IMixinShaderGroup) blurShader).getListShaders().get(pass);

			if (shader == null) {
				continue;
			}

			ShaderManager manager = shader.getShaderManager();

			if (manager == null) {
				continue;
			}

			if (manager.getShaderUniform("MaskY") != null) {
				manager.getShaderUniform("MaskY").set(maskStart, maskEnd);
			}

			if (manager.getShaderUniform("MaskStrength") != null) {
				manager.getShaderUniform("MaskStrength").set(strength);
			}

			if (manager.getShaderUniform("MaskInvert") != null) {
				manager.getShaderUniform("MaskInvert").set(maskInvert);
			}
		}

		maskPending = false;
	}
}
