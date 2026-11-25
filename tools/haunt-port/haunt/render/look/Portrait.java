package mix.client.mixui.integration.haunt.render.look;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.GlSave;
import mix.client.mixui.integration.haunt.render.GlTarget;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class Portrait {
   public static final int W = 512;
   public static final int H = 768;
   public static final float VIEW_W = 1.4F;
   public static final float VIEW_H = 2.1F;
   public static final float FEET_AT = 0.08F;
   private static final int POOL = 4;
   private static final GlTarget[] TARGETS = new GlTarget[4];
   private static int next;
   private static boolean failed;
   private static final GlSave SAVE = new GlSave();
   private static final Camera CAM = new Camera();
   private static float lastMillis;

   public static int capture(BodyProxy.Snapshot s, BodyProxy.Part[] parts, BodyProxy.Look look, double feetX, double feetY, double feetZ, float yaw) {
      if(!failed && OpenGlHelper.framebufferSupported) {
         long start = System.nanoTime();
         Vfx.flush();
         Camera before = Vfx.camera();
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         SAVE.save();

         int var36;
         try {
            GlTarget t = TARGETS[next];
            if(t == null && (t = TARGETS[next] = GlTarget.create(512, 768, '聘', 6408, true, true)) == null) {
               failed = true;
               FuriahsVisuals.LOG.warn("Portraits are off: no offscreen target could be made here");
               int n = -1;
               int var35 = n;
               return var35;
            }

            next = (next + 1) % 4;
            t.bind();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            Vfx.resetTextureBinding();
            GL20.glUseProgram(0);
            GL11.glDisable(3089);
            GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager.clear(16640);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(-0.699999988079071D, 0.699999988079071D, -0.07999999821186066D, 2.0199999809265137D, -20.0D, 20.0D);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            double rad = Math.toRadians((double)yaw);
            double fx = -Math.sin(rad);
            double fz = Math.cos(rad);
            CAM.posX = fx * 12.0D;
            CAM.posY = 0.95D;
            CAM.posZ = fz * 12.0D;
            CAM.forwardX = -fx;
            CAM.forwardY = 0.0D;
            CAM.forwardZ = -fz;
            CAM.rightX = -fz;
            CAM.rightY = 0.0D;
            CAM.rightZ = fx;
            CAM.upX = 0.0D;
            CAM.upY = 1.0D;
            CAM.upZ = 0.0D;
            Vfx.setCamera(CAM);
            Textures.ensureLoaded();
            RenderState.begin();

            try {
               GlStateManager.enableDepth();
               lit = look.lit;
               boolean lightmap = look.lightmap;
               look.lit = false;
               look.lightmap = false;
               BodyProxy.draw(s, parts, look, feetX, feetY, feetZ);
               look.lit = (boolean)var36;
               look.lightmap = lightmap;
               Vfx.flush();
            } finally {
               RenderState.end();
            }

            var36 = t.texture;
         } catch (LinkageError | RuntimeException var32) {
            failed = true;
            FuriahsVisuals.LOG.warn("Portraits are off: drawing one failed", var32);
            byte rad = -1;
            return rad;
         } finally {
            SAVE.restore();
            Vfx.resetTextureBinding();
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            Vfx.setCamera(before);
            lastMillis = (float)(System.nanoTime() - start) / 1000000.0F;
         }

         return var36;
      } else {
         return -1;
      }
   }

   public static BufferedImage readForTest(int texture) {
      if(texture < 0) {
         return null;
      } else {
         GlStateManager.bindTexture(texture);
         Vfx.resetTextureBinding();
         IntBuffer buf = BufferUtils.createIntBuffer(393216);
         GL11.glGetTexImage(3553, 0, '胡', '荧', buf);
         int[] px = new int[393216];
         buf.get(px);
         BufferedImage img = new BufferedImage(512, 768, 2);

         for(int y = 0; y < 768; ++y) {
            img.setRGB(0, 767 - y, 512, 1, px, y * 512, 512);
         }

         return img;
      }
   }

   public static float lastMillisForTest() {
      return lastMillis;
   }

   public static boolean available() {
      return !failed && OpenGlHelper.framebufferSupported;
   }

   public static void failForTest(boolean on) {
      failed = on;
   }
}
