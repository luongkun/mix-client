package mix.client.mixui.utils.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.nanovg.NVGLUFramebuffer;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;

import mix.client.mixui.Mix;
import mix.client.mixui.management.nanovg.NanoVGManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class ScreenAnimation {

	private Minecraft mc = Minecraft.getMinecraft();
	
	private int fbWidth, fbHeight;
	private NVGLUFramebuffer fb;
	
	public void wrap(Runnable glRender, Runnable task, float x, float y, float width, float height, float animationProgress, float alphaProgress, boolean stencil) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		int factor = sr.getScaleFactor();
		
		if(fbWidth != mc.displayWidth || fbHeight != mc.displayHeight) {
			close();
		}
		
		if (fb == null) {
			fbWidth = mc.displayWidth;
			fbHeight = mc.displayHeight;
			fb = NanoVGGL2.nvgluCreateFramebuffer(nvg.getContext(), mc.displayWidth, mc.displayHeight, 0);
		}
		
		// 显存不足或 GL 上下文被别的客户端扰动时这里会返回 null。
		// 以前直接往下走会 bind 一个空句柄，然后在原生层崩掉，现在改成这一帧不画。
		if(fb == null) {
			fbWidth = 0;
			fbHeight = 0;
			return;
		}
		
		int target = ScreenTarget.capture();

		NanoVGGL2.nvgluBindFramebuffer(nvg.getContext(), fb);

		GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);

		// 原来用的是 org.lwjgl3.BufferUtils（只存在于 upstream 私有库），这里换成纯 java.nio，
		// 免得编译期还要额外挂 upstream 的 lwjgl 包。行为与 createFloatBuffer 完全一致。
		FloatBuffer floaty = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		
		GlStateManager.enableTexture2D();;
		
		GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, floaty);

		GL11.glClearColor(0, 0, 0, 0);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		GL11.glClearColor(floaty.get(0), floaty.get(1), floaty.get(2), floaty.get(3));
		
		nvg.setupAndDraw(task);
		
		if(glRender != null) {
			glRender.run();
		}
		
		ScreenTarget.restore(target);
		
		nvg.setupAndDraw(() -> {
			
			nvg.setAlpha(Math.min(alphaProgress, 1.0F));
			nvg.scale(x * factor, y * factor, width * factor, height * factor, animationProgress);

			NVGPaint paint = NVGPaint.create();
			
			NanoVG.nvgBeginPath(nvg.getContext());
			
			if(stencil) {
				NanoVG.nvgRect(nvg.getContext(), x * factor, y * factor, width * factor, height * factor);
			}else {
				NanoVG.nvgRect(nvg.getContext(), 0, 0, mc.displayWidth, mc.displayHeight);
			}
			
			NanoVG.nvgFillPaint(nvg.getContext(), NanoVG.nvgImagePattern(nvg.getContext(), 0, 0, mc.displayWidth,  mc.displayHeight, 0, fb.image(), 1, paint));
			NanoVG.nvgFill(nvg.getContext());
		}, false);
	}
	
	public void wrap(Runnable task, float x, float y, float width, float height, float animationProgress, float alphaProgress) {
		wrap(null, task, x, y, width, height, animationProgress, alphaProgress, false);
	}
	
	public void wrap(Runnable glRender, Runnable task, float x, float y, float width, float height, float animationProgress, float alphaProgress) {
		wrap(glRender, task, x, y, width, height, animationProgress, alphaProgress, false);
	}
	
	public void wrap(Runnable task, float x, float y, float width, float height, float animationProgress, float alphaProgress, boolean stencil) {
		wrap(null, task, x, y, width, height, animationProgress, alphaProgress, stencil);
	}
	
	public void wrap(Runnable task, float animationProgress, float alphaProgress) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		wrap(null, task, 0, 0, sr.getScaledWidth(), sr.getScaledHeight(), animationProgress, alphaProgress, false);
	}
	
	public void wrap(Runnable task, float x, float y, float width, float height, float progress) {
		wrap(null, task, x, y, width, height, progress, progress, false);
	}
	
	public void wrap(Runnable task, float progress) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		wrap(null, task, 0, 0, sr.getScaledWidth(), sr.getScaledHeight(), progress, progress, false);
	}
	
	public void close() {
		
		NanoVGManager nvg = Mix.getInstance().getNanoVGManager();
		
		if(fb != null) {
			NanoVGGL2.nvgluDeleteFramebuffer(nvg.getContext(), fb);
			fb = null;
		}
	}
}
