package mix.client.mixui.integration.haunt.render;

import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.PlayerModel;
import mix.client.mixui.integration.haunt.render.Vfx;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class SkinSampler {
   private static final int MAX_CACHED = 24;
   private static final Map<ResourceLocation, SkinSampler.Image> CACHE = new LinkedHashMap<ResourceLocation, SkinSampler.Image>(16, 0.75F, true) {
      protected boolean removeEldestEntry(Entry<ResourceLocation, SkinSampler.Image> eldest) {
         return this.size() > 24;
      }
   };
   private static boolean failed;
   private static final float[] C0 = new float[3];
   private static final float[] C1 = new float[3];
   private static final float[] C2 = new float[3];
   private static final float[] C3 = new float[3];
   private static final float[] UV = new float[4];

   public static SkinSampler.Image image(ResourceLocation r) {
      if(r != null && !failed) {
         SkinSampler.Image img = (SkinSampler.Image)CACHE.get(r);
         if(img != null) {
            return img;
         } else {
            try {
               int id = BodyProxy.texture(r);
               GlStateManager.bindTexture(id);
               Vfx.resetTextureBinding();
               int w = GL11.glGetTexLevelParameteri(3553, 0, 4096);
               int h = GL11.glGetTexLevelParameteri(3553, 0, 4097);
               if(w > 0 && h > 0 && w <= 1024 && h <= 1024) {
                  IntBuffer buf = BufferUtils.createIntBuffer(w * h);
                  GL11.glGetTexImage(3553, 0, '胡', '荧', buf);
                  int[] argb = new int[w * h];
                  buf.get(argb);
                  img = new SkinSampler.Image(w, h, argb);
                  CACHE.put(r, img);
                  return img;
               } else {
                  return null;
               }
            } catch (LinkageError | RuntimeException var7) {
               failed = true;
               FuriahsVisuals.LOG.warn("Skin colors could not be read; kill effects use plain colors instead", var7);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static int sample(BodyProxy.Snapshot s, int part, int count, Rng rng, float[] lx, float[] ly, float[] lz, int[] color, int offset, int fallback) {
      SkinSampler.Image img = image(s.skin);
      PlayerModel.Box base = PlayerModel.base(part, s.slim);
      PlayerModel.Box wear = PlayerModel.wear(part, s.slim);
      float aw = base.w * base.h;
      float ad = base.d * base.h;
      float at = base.w * base.d;
      float total = 2.0F * (aw + ad + at);
      int n = 0;

      for(int k = 0; k < count && offset + n < color.length; ++k) {
         float pick = rng.next() * total;
         int face = pick < aw?4:(pick < 2.0F * aw?5:(pick < 2.0F * aw + ad?0:(pick < 2.0F * aw + 2.0F * ad?1:(pick < 2.0F * aw + 2.0F * ad + at?2:3))));
         float a = rng.next();
         float b = rng.next();
         int[] fc = PlayerModel.FACE_CORNERS[face];
         PlayerModel.corner(base, fc[0], C0);
         PlayerModel.corner(base, fc[1], C1);
         PlayerModel.corner(base, fc[2], C2);
         PlayerModel.corner(base, fc[3], C3);
         float px = C0[0] + (C1[0] - C0[0]) * a + (C3[0] - C0[0]) * b;
         float py = C0[1] + (C1[1] - C0[1]) * a + (C3[1] - C0[1]) * b;
         float pz = C0[2] + (C1[2] - C0[2]) * a + (C3[2] - C0[2]) * b;
         int i = offset + n;
         lx[i] = (float)((double)(px - base.cx()) * 0.05859375D);
         ly[i] = (float)((double)(py - base.cy()) * 0.05859375D);
         lz[i] = (float)((double)(pz - base.cz()) * 0.05859375D);
         int c = fallback;
         if(img != null) {
            PlayerModel.faceUV(wear, face, UV);
            double wu = (double)((UV[2] + (UV[0] - UV[2]) * a) / (float)wear.texW);
            double wv = (double)((UV[1] + (UV[3] - UV[1]) * b) / (float)wear.texH);
            int over = img.at(wu, wv);
            if(over >>> 24 > 128) {
               c = over | -16777216;
            } else {
               PlayerModel.faceUV(base, face, UV);
               double u = (double)((UV[2] + (UV[0] - UV[2]) * a) / (float)base.texW);
               double v = (double)((UV[1] + (UV[3] - UV[1]) * b) / (float)base.texH);
               c = img.at(u, v) | -16777216;
            }
         }

         color[i] = c;
         ++n;
      }

      return n;
   }

   public static int average(BodyProxy.Snapshot s, int part, int fallback) {
      SkinSampler.Image img = image(s.skin);
      if(img == null) {
         return fallback;
      } else {
         PlayerModel.Box base = PlayerModel.base(part, s.slim);
         PlayerModel.faceUV(base, 4, UV);
         long r = 0L;
         long g = 0L;
         long b = 0L;
         int n = 0;

         for(int k = 0; k < 16; ++k) {
            double u = ((double)UV[0] + (double)(UV[2] - UV[0]) * ((double)(k % 4) + 0.5D) / 4.0D) / (double)base.texW;
            double v = ((double)UV[1] + (double)(UV[3] - UV[1]) * ((double)(k / 4) + 0.5D) / 4.0D) / (double)base.texH;
            int c = img.at(u, v);
            r += (long)(c >> 16 & 255);
            g += (long)(c >> 8 & 255);
            b += (long)(c & 255);
            ++n;
         }

         return -16777216 | (int)(r / (long)n) << 16 | (int)(g / (long)n) << 8 | (int)(b / (long)n);
      }
   }

   public static final class Image {
      public final int width;
      public final int height;
      final int[] argb;

      Image(int width, int height, int[] argb) {
         this.width = width;
         this.height = height;
         this.argb = argb;
      }

      public int[] pixels() {
         return this.argb;
      }

      public int at(double u, double v) {
         int x = Math.max(0, Math.min(this.width - 1, (int)(u * (double)this.width)));
         int y = Math.max(0, Math.min(this.height - 1, (int)(v * (double)this.height)));
         return this.argb[y * this.width + x];
      }
   }
}
