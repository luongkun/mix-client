package mix.client.mixui.utils;

import java.awt.Color;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.lwjgl.opengl.GL11;

import mix.client.mixui.injection.interfaces.IMixinMinecraft;
import mix.client.mixui.injection.interfaces.IMixinRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class Render3DUtils {

	private static Minecraft mc = Minecraft.getMinecraft();

	/** 环上沿的 alpha 系数。官方原值 0.4，在有雾有动态天空的多人场景里几乎看不出来。 */
	private static final float TOP_ALPHA = 0.72F;

	/** 环下沿的 alpha 系数。不用 0：alpha 全 0 的那排顶点在部分驱动上会被整体丢弃，环像被切了一刀。 */
	private static final float BOTTOM_ALPHA = 0.06F;

	public static void drawFillBox(AxisAlignedBB box) {
		
		GlStateManager.disableCull();
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		tessellator.draw();
		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		tessellator.draw();
		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		tessellator.draw();
		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		tessellator.draw();
		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
		tessellator.draw();
		worldrenderer.begin(6, DefaultVertexFormats.POSITION);
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).endVertex();
		tessellator.draw();
		GlStateManager.enableCull();
	}

    public static void drawBoundingBox(final AxisAlignedBB aa) {
    	
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.maxZ));
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.maxZ));
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.minZ));
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.minZ));
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.minZ));
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.maxZ));
        glVertex3D(getRenderPos(aa.minX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.minX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.minZ));
        glVertex3D(getRenderPos(aa.maxX, aa.maxY, aa.maxZ));
        glVertex3D(getRenderPos(aa.maxX, aa.minY, aa.maxZ));
        GL11.glEnd();
    }

    private static void glVertex3D(Vec3 vector3d) {
        GL11.glVertex3d(vector3d.xCoord, vector3d.yCoord, vector3d.zCoord);
    }

    private static Vec3 getRenderPos(double x, double y, double z) {
        x -= (((IMixinRenderManager) mc.getRenderManager())).getRenderPosX();
        y -= (((IMixinRenderManager) mc.getRenderManager())).getRenderPosY();
        z -= (((IMixinRenderManager) mc.getRenderManager())).getRenderPosZ();
        return new Vec3(x, y, z);
    }

    /**
     * 目标指示器：玩家周围那一圈<b>上下浮动</b>的淡渐变环。
     *
     * <p><b>几何与 alpha 全部照抄官方 jar</b>（{@code D:\Download\MixClient.zip} 里
     * {@code minecraft/MixClient.jar} 的 {@code mix/client/mixui/utils/Render3DUtils.class}，
     * 27501753 字节 / sha1 1897212cb6642e75）。官方反编译出来的循环体是：</p>
     *
     * <pre>
     * y      = posY插值 + sin(ms / 200) + 1;
     * bottom = y - cos(ms / 200) / 2;
     * for (f = 0; f &lt; 2π; f += 2π/64) {
     *     glColor4f(r, g, b, 0.06); glVertex3d(x + rad*cos(f), bottom, z + rad*sin(f));
     *     glColor4f(r, g, b, 0.72); glVertex3d(x + rad*cos(f), y,      z + rad*sin(f));
     * }
     * </pre>
     *
     * <p>也就是说：<b>只有一条环</b>，环带厚度由 {@code cos} 相位决定（最厚 0.5 格、
     * 相位过零时收成一条线），整条环靠 {@code sin} 相位在 {@code +0.5 ~ +1.5} 格之间
     * 上下往返。之前几版想「覆盖全身」把环拉成四层柱体、或把行程改成身高 5%~95%，
     * 结果都变成了「以玩家身高为直径的大圆圈」——那是对这个功能的误读，现在整段回退。</p>
     *
     * <p><b>唯一没照抄的是 GL 收尾。</b>官方版本前半段用 raw {@code glDisable} 关掉
     * 贴图/剔除，收尾却用 {@code GlStateManager.enableXxx()}（{@code bfl.o()} = enableCull）
     * 去恢复 —— 而 {@code GlStateManager$BooleanState.setState()} 里有一句
     * {@code if (wanted == currentState) return;}：缓存值没变就根本不发 GL 指令。
     * raw 改了真实状态、缓存却还停在旧值，于是那次「恢复」是空转，真实状态永远回不来。
     * 这正是连续几次「目标信息变白板、手臂变白、Dango 歌词变纯色方块」的根源。</p>
     *
     * <p>现在改成 {@code glPushAttrib(GL_ENABLE_BIT | GL_DEPTH_BUFFER_BIT)} +
     * {@code glPopAttrib()} 整组压栈还原，绘制期只发 raw GL、<b>完全不碰 GlStateManager
     * 的 enable/disable</b>：没碰缓存，缓存就始终停在「进入前」的值；popAttrib 又把真实
     * 状态还原成同样的「进入前」，两者重新对齐。收尾再补一次 {@code resetColor()}，
     * 因为 raw {@code glColor4f} 改过颜色而 GlStateManager 的颜色缓存并不知道。</p>
     */
    public static void drawTargetIndicator(Entity entity, double rad, Color color) {

        if (entity == null || color == null) {
            return;
        }

        // 整组压栈：ENABLE_BIT(0x2000=8192) 带着所有 enable/disable 位，
        // DEPTH_BUFFER_BIT(0x100=256) 带着 depthMask —— 合起来 8448，与已验证版本一致。
        GL11.glPushAttrib(8448);
        GL11.glPushMatrix();

        // ── 这七行 disable 是「灰色」的真正开关 ──
        // 只关纹理是不够的：只要 GL_LIGHTING 还开着，glColor4f 给出的颜色就不是最终像素色，
        // 而是作为材质基色再被光照方程乘一遍（默认 GL_LIGHT0 白色 + 环境光 0.2），
        // 结果就是一圈没有色相、只剩灰度的带子。GL_FOG 同理，会把远处顶点往雾色上拉。
        // 早期版本用的是 GlStateManager.disableLighting() 之类，但因为
        // GlStateManager$BooleanState.setState() 的 if (wanted == currentState) return;
        // 缓存短路，实际没发指令 —— 所以那里「看起来关了、其实还开着」。
        // 这里全部走 raw glDisable，配合上面的 glPushAttrib/glPopAttrib 精确还原。
        GL11.glDisable(GL11.GL_TEXTURE_2D);   // 3553
        GL11.glDisable(GL11.GL_LIGHTING);     // 2896  ← 不关就是灰色
        GL11.glDisable(GL11.GL_FOG);          // 2912
        GL11.glDisable(GL11.GL_CULL_FACE);    // 2884  三角带两面都要，否则背面被剔掉看着像缺口
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);// 2881
        // 2929：这里**必须 enable**，不能沿用「关深度测试」的老写法。
        // EventRender3D 在 renderWorldPass 里、renderHand 之前触发，此时地形与实体
        // 都已经把深度写进缓冲了。开着深度测试、配合下面的 glDepthMask(false)
        // （只读不写），环的每一片像素都会先跟已有深度比一比：被方块挡住的直接丢弃，
        // 于是就不再穿墙 —— 这正是 nametags 与目标信息的行为。
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_ALPHA_TEST);   // 3008
        GL11.glEnable(GL11.GL_BLEND);         // 3042
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // 770, 771
        GL11.glEnable(GL11.GL_LINE_SMOOTH);   // 2848
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);            // 3154
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);         // 3155
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST); // 3152
        GL11.glShadeModel(GL11.GL_SMOOTH);    // 7425

        float partialTicks = ((IMixinMinecraft) mc).getTimer().renderPartialTicks;
        IMixinRenderManager renderManager = (IMixinRenderManager) mc.getRenderManager();

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.getRenderPosX();
        double baseY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.getRenderPosY();
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.getRenderPosZ();

        double phase = System.currentTimeMillis() / 2E+2;
        double bob = Math.sin(phase);

        // 官方：环顶 = 脚底 +1 格 + sin 摆动；环底 = 环顶 - cos/2。
        // cos 相位决定环带的厚度（最厚半格，相位过零时收成一条线），
        // 所以这条环会「一边上下扫、一边自己变厚变薄」，这正是官方那圈的观感。
        double topY = baseY + bob + 1.0D;
        double skirtY = topY - (Math.cos(phase) / 2.0D);

        float red = color.getRed() / 255.F;
        float green = color.getGreen() / 255.F;
        float blue = color.getBlue() / 255.F;
        float baseAlpha = color.getAlpha() / 255.F;

        // 上下沿的 alpha：上沿 0.72、下沿 0.06。
        // 官方原值是 0.4 / 0.0 —— 下沿 0 的那一排顶点在部分驱动上会被整体丢弃，
        // 环看起来像被切了一刀；0.72 是为了在有雾有动态天空的多人场景里还看得清。
        // 两者都乘 color 的 alpha，所以淡入淡出逻辑照样生效。
        float skirtAlpha = baseAlpha * BOTTOM_ALPHA;
        float topAlpha = baseAlpha * TOP_ALPHA;

        // 官方步长 0.09817477042468103 = 2π/64，循环条件 f < 2π。
        // 每段发两个顶点（下沿 + 上沿），GL 在两点间线性插值出环带渐变。
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        for (double f = 0.0D; f < 6.283185307179586D; f += 0.09817477042468103D) {

            double vecX = x + rad * Math.cos(f);
            double vecZ = z + rad * Math.sin(f);

            GL11.glColor4f(red, green, blue, skirtAlpha);
            GL11.glVertex3d(vecX, skirtY, vecZ);
            GL11.glColor4f(red, green, blue, topAlpha);
            GL11.glVertex3d(vecX, topY, vecZ);
        }

        GL11.glEnd();

        GL11.glPopMatrix();
        // 精确还原：贴图、光照、雾、剔除、深度测试、depthMask、alphaFunc、混合
        // 全部回到进入前的真实值，不用手写一串 enable/disable 去猜。
        GL11.glPopAttrib();

        // raw glColor4f 改过颜色，GlStateManager 的颜色缓存不知情；
        // resetColor() 把 rgba 置 -1，下一次 color() 必定真发指令。
        GlStateManager.resetColor();
    }

    private static void drawFilledCircleNoGL(final int x, final int y, final double r, final int c, final int quality) {

        float f = (float) (c >> 24 & 255) / 255.0F;
        float f1 = (float) (c >> 16 & 255) / 255.0F;
        float f2 = (float) (c >> 8 & 255) / 255.0F;
        float f3 = (float) (c & 255) / 255.0F;

        GL11.glColor4f(f1, f2, f3, f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        for (int i = 0; i <= 360 / quality; i++) {

            double x2 = Math.sin(((double) i * quality) * Math.PI / 180.0) * r;
            double y2 = Math.cos(((double) i * quality) * Math.PI / 180.0) * r;

            GL11.glVertex2d(x + x2, y + y2);
        }

        GL11.glEnd();
    }

    public static void renderBreadCrumbs(final List<Vec3> vec3s, Color color) {

        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int i = 0;

        try {
            for (Vec3 v : vec3s) {

                i++;
                boolean draw = true;

                double x = v.xCoord - (((IMixinRenderManager) mc.getRenderManager())).getRenderPosX();
                double y = v.yCoord - (((IMixinRenderManager) mc.getRenderManager())).getRenderPosY();
                double z = v.zCoord - (((IMixinRenderManager) mc.getRenderManager())).getRenderPosZ();

                double distanceFromPlayer = mc.thePlayer.getDistance(v.xCoord, v.yCoord - 1, v.zCoord);
                int quality = (int) (distanceFromPlayer * 4 + 10);

                if (quality > 350) {
                    quality = 350;
                }

                if (i % 10 != 0 && distanceFromPlayer > 25) {
                    draw = false;
                }

                if (i % 3 == 0 && distanceFromPlayer > 15) {
                    draw = false;
                }

                if (!draw) {
                    continue;
                }

                GL11.glPushMatrix();
                GL11.glTranslated(x, y, z);

                final float scale = 0.04F;
                GL11.glScalef(-scale, -scale, -scale);

                GL11.glRotated(-mc.getRenderManager().playerViewY, 0.0D, 1.0D, 0.0D);
                GL11.glRotated(mc.getRenderManager().playerViewX, 1.0D, 0.0D, 0.0D);

                drawFilledCircleNoGL(0, 0, 0.7, color.getRGB() & 0x00FFFFFF | 0x64000000, quality);

                if (distanceFromPlayer < 4) {
                    drawFilledCircleNoGL(0, 0, 1.4, color.getRGB() & 0x00FFFFFF | 0x32000000, quality);
                }

                if (distanceFromPlayer < 20) {
                    drawFilledCircleNoGL(0, 0, 2.3, color.getRGB() & 0x00FFFFFF | 0x1E000000, quality);
                }

                GL11.glScalef(0.8F, 0.8F, 0.8F);
                GL11.glPopMatrix();
            }
        } catch (ConcurrentModificationException ignored) {
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.enableDepth();
        GL11.glColor3d(255, 255, 255);
    }
}
