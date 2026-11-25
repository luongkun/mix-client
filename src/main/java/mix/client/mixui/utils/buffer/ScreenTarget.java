package mix.client.mixui.utils.buffer;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import net.minecraft.client.Minecraft;

/**
 * 真正「现在画到哪里去」的帧缓冲，存一份再放回去。
 *
 * <p>所有临时绑定自己 FBO 的画法（NanoVG 的整屏缓冲、ShaderGroup 的后处理）
 * 原来都用 {@code mc.getFramebuffer().bindFramebuffer(true)} 收尾。这个调用在
 * 「主帧缓冲没有自己的 GL 对象」时<b>是空操作</b>：OptiFine 的 Fast Render 直接画到窗口，
 * 主帧缓冲的 framebufferObject 是 -1，于是 bind 什么也没做，临时 FBO 就一直留在那儿。
 * 之后所有绘制（当帧剩下的 HUD、下一帧的地形和天空）都进了那张离屏纹理，
 * 窗口上还是上次 swap 的旧画面 —— 表现就是整个屏幕卡住、天空/世界一帧一帧地闪，
 * 和真正的死机（无响应）不是一回事。</p>
 *
 * <p>只有把当前绑定读回来这一条路不会被跳过，有没有主帧缓冲都一样。</p>
 */
public final class ScreenTarget {

    private ScreenTarget() {
    }

    /** 当前绑定的帧缓冲（0 = 窗口）。 */
    public static int capture() {

        /* LWJGL 2 rejects a buffer smaller than 16 for glGetInteger even when
           only one value is asked for, so the scratch buffer is always 16. */
        IntBuffer buffer = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING, buffer);
        return buffer.get(0);
    }

    /**
     * 把帧缓冲放回去，并把视口恢复成整窗。
     *
     * 所有调用点的临时 FBO 都是 displayWidth x displayHeight，所以回到任何一个
     * 目标之后整窗视口都是对的（Fast Render 下窗口自己也是这个尺寸）。
     */
    public static void restore(int framebuffer) {

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        Minecraft mc = Minecraft.getMinecraft();
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
    }
}
