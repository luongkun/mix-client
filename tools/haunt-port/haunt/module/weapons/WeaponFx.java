package mix.client.mixui.integration.haunt.module.weapons;

import java.nio.FloatBuffer;
import java.util.Locale;
import java.util.Random;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.weapons.Mat4;
import mix.client.mixui.integration.haunt.module.weapons.ViewModels;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponShader;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Distortion;
import mix.client.mixui.integration.haunt.render.Procedural;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class WeaponFx {
   static final int MAX = 160;
   private static final int ATLAS = 128;
   private static final int ATLAS_H = 256;
   private final WeaponFx.Particle[] live = new WeaponFx.Particle[160];
   private int count;
   private final float[][] beams = new float[8][10];
   private int beamCount;
   private final float[][] arcs = new float[6][14];
   private int arcCount;
   static final int BOTH = 0;
   static final int HD = 1;
   static final int PIXEL = 2;
   int side;
   int drawSide;
   private final int[] beamSide = new int[8];
   private final int[] arcSide = new int[6];
   private final float[][] shownArcs = new float[6][];
   private final float[][] heats = new float[2][8];
   private int heatCount;
   private final WeaponFx.Trail[] trails = new WeaponFx.Trail[]{new WeaponFx.Trail(), new WeaponFx.Trail()};
   private long now;
   private final Random random = new Random();
   private double ex;
   private double ey;
   private double ez;
   private final Mat4 turn = new Mat4();
   private final float[] v = new float[4];
   private DynamicTexture atlas;
   private final float[] clip = new float[4];
   private static final Rng RNG = new Rng();
   private static final double[] CX = new double[3];
   private static final double[] CY = new double[3];
   private static final double[] CZ = new double[3];
   private static final int ARC_POINTS = 40;
   private static final int[] MAIN = new int[6];
   private static final int[] FORK = new int[6];
   private static final float[] BASE_W = new float[240];
   private static final Camera EYE = new Camera();
   private static final float[][] HERE = new float[1][14];
   private static final int SHIMMER_ALONG = 16;
   private static final int SHIMMER_UP = 6;
   private static long shimmerFrame = -2305843009213693952L;
   static float testShimmer = 1.0F;
   private float clock;
   private static final float[] ACROSS = new float[]{-1.0F, -0.42F, 0.0F, 0.42F, 1.0F};
   private static final float[] WEIGHT = new float[]{0.0F, 0.42F, 1.0F, 0.42F, 0.0F};
   private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
   private static final Mat4 MV = new Mat4();
   private static final float[] ALONG = new float[]{-1.0F, 0.0F, 1.0F, 2.0F};
   private static final float[] ALONG_WEIGHT = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
   private static final float[][] ALONG_POINTS = new float[4][3];

   WeaponFx() {
      for(int i = 0; i < 160; ++i) {
         this.live[i] = new WeaponFx.Particle();
      }

   }

   void frame(Entity e, float pt, long now) {
      this.now = now;
      this.beamCount = 0;
      this.arcCount = 0;
      this.heatCount = 0;
      this.ex = e.prevPosX + (e.posX - e.prevPosX) * (double)pt;
      this.ey = e.prevPosY + (e.posY - e.prevPosY) * (double)pt + (double)e.getEyeHeight();
      this.ez = e.prevPosZ + (e.posZ - e.prevPosZ) * (double)pt;
      float pitch = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * pt;
      float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * pt;
      this.turn.identity().rotate(pitch, 1.0F, 0.0F, 0.0F).rotate(yaw + 180.0F, 0.0F, 1.0F, 0.0F);
   }

   void frameStill(long now) {
      this.now = now;
      this.beamCount = 0;
      this.arcCount = 0;
      this.heatCount = 0;
      this.ez = 0.0D;
      this.ey = 0.0D;
      this.ex = 0.0D;
      this.turn.identity();
   }

   void smoke(float[] at, float[] dir, float amount, float scale) {
      int n = 1 + Math.round(3.0F * Math.min(1.0F, amount));

      for(int i = 0; i < n; ++i) {
         WeaponFx.Particle p = this.next();
         float speed = (0.45F + 0.45F * this.random.nextFloat()) * (0.4F + 0.6F * Math.min(1.0F, amount)) * scale * 0.5F;
         float jx = (this.random.nextFloat() - 0.5F) * 0.35F;
         float jy = (this.random.nextFloat() - 0.5F) * 0.35F;
         float jz = (this.random.nextFloat() - 0.5F) * 0.35F;
         this.place(p, at[0] + jx * 0.04F * scale, at[1] + jy * 0.04F * scale, at[2] + jz * 0.04F * scale, (dir[0] + jx) * speed, (dir[1] + jy) * speed, (dir[2] + jz) * speed);
         p.kind = WeaponFx.Kind.SMOKE;
         p.variant = this.random.nextInt(4);
         p.age = 0.0F;
         p.life = 1.2F + 0.8F * this.random.nextFloat();
         p.size = (0.03F + 0.02F * this.random.nextFloat()) * scale;
         p.grow = (0.1F + 0.07F * this.random.nextFloat()) * scale;
         p.angle = this.random.nextFloat() * 6.2832F;
         p.spin = (this.random.nextFloat() - 0.5F) * 1.2F;
         float grey = 0.42F + 0.1F * this.random.nextFloat();
         p.r = grey * 0.95F;
         p.g = grey;
         p.b = grey * 1.08F;
         p.a = (0.45F + 0.2F * this.random.nextFloat()) * (0.25F + 0.75F * Math.min(1.0F, amount));
      }

   }

   void mote(float[] at, float scale, int rgb, float rise, String kind) {
      this.emitMote(at, scale, rgb, rise, kind);
   }

   void mote(float[] at, float scale, int rgb, float rise, String kind, float[] push) {
      WeaponFx.Particle p = this.emitMote(at, scale, rgb, rise, kind);
      float k = 0.6F + 0.8F * this.random.nextFloat();
      float spread = 0.35F * (Math.abs(push[0]) + Math.abs(push[1]) + Math.abs(push[2]));
      float vx = push[0] * k + (this.random.nextFloat() - 0.5F) * spread;
      float vy = push[1] * k + (this.random.nextFloat() - 0.5F) * spread;
      float vz = push[2] * k + (this.random.nextFloat() - 0.5F) * spread;
      float[] m = this.turn.m;
      p.px = m[0] * vx + m[1] * vy + m[2] * vz;
      p.py = m[4] * vx + m[5] * vy + m[6] * vz;
      p.pz = m[8] * vx + m[9] * vy + m[10] * vz;
   }

   private WeaponFx.Particle emitMote(float[] at, float scale, int rgb, float rise, String kind) {
      WeaponFx.Particle p = this.next();
      this.place(p, at[0], at[1], at[2], 0.0F, 0.0F, 0.0F);
      float r = (float)(rgb >> 16 & 255) / 255.0F;
      float g = (float)(rgb >> 8 & 255) / 255.0F;
      float b = (float)(rgb & 255) / 255.0F;
      p.angle = this.random.nextFloat() * 6.2832F;
      p.spin = (this.random.nextFloat() - 0.5F) * 0.8F;
      p.age = 0.0F;
      p.variant = this.random.nextInt(4);
      if("glint".equals(kind)) {
         p.kind = WeaponFx.Kind.GLINT;
         p.variant = 4;
         p.life = 0.3F + 0.3F * this.random.nextFloat();
         p.size = 0.0042F * scale * (0.6F + 0.8F * this.random.nextFloat());
         p.grow = 0.0F;
         p.r = 0.7F + 0.3F * r;
         p.g = 0.7F + 0.3F * g;
         p.b = 0.7F + 0.3F * b;
         p.a = 1.0F;
         p.vx = (this.random.nextFloat() - 0.5F) * 0.02F * scale;
         p.vy = -0.01F * scale;
         p.vz = (this.random.nextFloat() - 0.5F) * 0.02F * scale;
         return p;
      } else {
         if("mist".equals(kind)) {
            p.kind = WeaponFx.Kind.MIST;
            p.life = 1.6F + 1.0F * this.random.nextFloat();
            p.size = (0.012F + 0.01F * this.random.nextFloat()) * scale;
            p.grow = (0.05F + 0.03F * this.random.nextFloat()) * scale;
            p.r = 0.82F + 0.12F * r;
            p.g = 0.88F + 0.1F * g;
            p.b = 0.95F + 0.05F * b;
            p.a = 0.16F + 0.08F * this.random.nextFloat();
         } else {
            boolean ember = "ember".equals(kind);
            p.kind = ember?WeaponFx.Kind.EMBER:WeaponFx.Kind.MOTE;
            p.life = ember?0.7F + 0.6F * this.random.nextFloat():1.4F + 1.1F * this.random.nextFloat();
            p.size = (ember?0.0045F:0.004F) * scale * (0.7F + 0.6F * this.random.nextFloat());
            p.grow = 0.0F;
            p.r = ember?1.0F:0.35F + 0.65F * r;
            p.g = ember?0.42F + 0.25F * this.random.nextFloat():0.35F + 0.65F * g;
            p.b = ember?0.08F:0.35F + 0.65F * b;
            p.a = 1.0F;
         }

         p.vx = (this.random.nextFloat() - 0.5F) * 0.04F * scale;
         p.vy = rise * scale * (0.7F + 0.6F * this.random.nextFloat());
         p.vz = (this.random.nextFloat() - 0.5F) * 0.04F * scale;
         return p;
      }
   }

   void dust(float[] at, float scale, boolean calm) {
      int n = calm?5:11;

      for(int i = 0; i < n; ++i) {
         WeaponFx.Particle p = this.next();
         this.place(p, at[0], at[1], at[2], 0.0F, 0.0F, 0.0F);
         double a = ((double)i + 0.7D * this.random.nextDouble()) / (double)n * 3.141592653589793D * 2.0D;
         float speed = (0.22F + 0.22F * this.random.nextFloat()) * scale;
         p.vx = (float)Math.cos(a) * speed;
         p.vz = (float)Math.sin(a) * speed;
         p.vy = (0.04F + 0.06F * this.random.nextFloat()) * scale;
         p.kind = WeaponFx.Kind.DUST;
         p.variant = this.random.nextInt(4);
         p.age = 0.0F;
         p.life = 0.9F + 0.5F * this.random.nextFloat();
         p.size = (0.026F + 0.016F * this.random.nextFloat()) * scale;
         p.grow = (0.07F + 0.05F * this.random.nextFloat()) * scale;
         p.angle = this.random.nextFloat() * 6.2832F;
         p.spin = (this.random.nextFloat() - 0.5F) * 1.0F;
         float tone = 0.62F + 0.1F * this.random.nextFloat();
         p.r = tone * 1.06F;
         p.g = tone;
         p.b = tone * 0.86F;
         p.a = 0.56F + 0.16F * this.random.nextFloat();
      }

   }

   void leaves(float[] a, float[] b, int n, float scale) {
      for(int i = 0; i < n; ++i) {
         WeaponFx.Particle p = this.next();
         float t = this.random.nextFloat();
         float dir = this.random.nextFloat() * 6.2832F;
         float speed = (0.2F + 0.25F * this.random.nextFloat()) * scale;
         this.place(p, a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t, (float)Math.cos((double)dir) * speed, (0.12F + 0.2F * this.random.nextFloat()) * scale, (float)Math.sin((double)dir) * speed);
         p.kind = WeaponFx.Kind.LEAF;
         p.variant = 6 + this.random.nextInt(2);
         p.age = 0.0F;
         p.life = 1.6F + 0.9F * this.random.nextFloat();
         p.size = (0.022F + 0.01F * this.random.nextFloat()) * scale;
         p.grow = 0.0F;
         p.angle = this.random.nextFloat() * 6.2832F;
         p.spin = (this.random.nextFloat() - 0.5F) * 7.0F;
         float g = this.random.nextFloat();
         boolean yellow = this.random.nextFloat() < 0.2F;
         p.r = yellow?0.62F:0.22F + 0.12F * g;
         p.g = yellow?0.66F:0.48F + 0.16F * g;
         p.b = yellow?0.16F:0.12F + 0.06F * g;
         p.a = 0.95F;
      }

   }

   void sparks(float[] at, float[] dir, int n, float scale, int rgb) {
      for(int i = 0; i < n; ++i) {
         WeaponFx.Particle p = this.next();
         float speed = 0.8F + 1.4F * this.random.nextFloat();
         float jx = (this.random.nextFloat() - 0.5F) * 0.9F;
         float jy = (this.random.nextFloat() - 0.5F) * 0.9F;
         float jz = (this.random.nextFloat() - 0.5F) * 0.9F;
         this.place(p, at[0], at[1], at[2], (dir[0] + jx) * speed, (dir[1] + jy) * speed, (dir[2] + jz) * speed);
         p.kind = WeaponFx.Kind.SPARK;
         p.variant = 4;
         p.age = 0.0F;
         p.life = 0.25F + 0.3F * this.random.nextFloat();
         p.size = (0.006F + 0.004F * this.random.nextFloat()) * scale;
         p.grow = -p.size * 0.8F;
         p.angle = 0.0F;
         p.spin = 0.0F;
         p.r = (float)(rgb >> 16 & 255) / 255.0F;
         p.g = (float)(rgb >> 8 & 255) / 255.0F;
         p.b = (float)(rgb & 255) / 255.0F;
         p.a = 1.0F;
      }

   }

   void beam(float[] a, float[] b, float width, int rgb, float intensity) {
      if(this.beamCount < this.beams.length && intensity > 0.0F) {
         this.beamSide[this.beamCount] = this.side;
         float[] x = this.beams[this.beamCount++];
         x[0] = a[0];
         x[1] = a[1];
         x[2] = a[2];
         x[3] = b[0];
         x[4] = b[1];
         x[5] = b[2];
         x[6] = width;
         x[7] = (float)(rgb >> 16 & 255) / 255.0F * intensity;
         x[8] = (float)(rgb >> 8 & 255) / 255.0F * intensity;
         x[9] = (float)(rgb & 255) / 255.0F * intensity;
      }

   }

   void arc(float[] a, float[] b, float[] bow, float width, int rgb, float strength, int seed, float jag) {
      if(this.arcCount < this.arcs.length && strength > 0.01F) {
         this.arcSide[this.arcCount] = this.side;
         float[] x = this.arcs[this.arcCount++];
         System.arraycopy(a, 0, x, 0, 3);
         System.arraycopy(b, 0, x, 3, 3);
         System.arraycopy(bow, 0, x, 6, 3);
         x[9] = width;
         x[10] = Float.intBitsToFloat(rgb);
         x[11] = Math.min(1.0F, strength);
         x[12] = Float.intBitsToFloat(seed);
         x[13] = jag;
      }

   }

   void shimmer(float[] a, float[] b, float height, float heat) {
      if(this.heatCount < this.heats.length && heat > 0.01F) {
         float[] x = this.heats[this.heatCount++];
         System.arraycopy(a, 0, x, 0, 3);
         System.arraycopy(b, 0, x, 3, 3);
         x[6] = height;
         x[7] = Math.min(1.0F, heat);
      }

   }

   boolean hasShimmer() {
      return this.heatCount > 0;
   }

   void trail(int hand, float[] tip, float[] base, int rgb, float strength, float life) {
      WeaponFx.Trail t = this.trails[hand];
      if(t.size == 0 || t.time[t.head] != this.now) {
         t.head = (t.head + 1) % 64;
         t.size = Math.min(64, t.size + 1);
      }

      System.arraycopy(tip, 0, t.tip[t.head], 0, 3);
      System.arraycopy(base, 0, t.base[t.head], 0, 3);
      t.time[t.head] = this.now;
      t.r = (float)(rgb >> 16 & 255) / 255.0F;
      t.g = (float)(rgb >> 8 & 255) / 255.0F;
      t.b = (float)(rgb & 255) / 255.0F;
      t.strength = strength;
      t.life = life;
   }

   void cutTrail(int hand) {
      this.trails[hand].size = 0;
   }

   boolean hasAnything() {
      if(this.count <= 0 && this.beamCount <= 0 && this.arcCount <= 0) {
         for(WeaponFx.Trail t : this.trails) {
            if(t.size > 1 && (float)(this.now - t.time[t.head]) / 1.0E9F < t.life) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private WeaponFx.Particle next() {
      if(this.count < 160) {
         return this.live[this.count++];
      } else {
         int oldest = 0;
         float most = -1.0F;

         for(int i = 0; i < this.count; ++i) {
            float k = this.live[i].age / this.live[i].life;
            if(k > most) {
               most = k;
               oldest = i;
            }
         }

         return this.live[oldest];
      }
   }

   private void place(WeaponFx.Particle p, float x, float y, float z, float vx, float vy, float vz) {
      p.pz = 0.0F;
      p.py = 0.0F;
      p.px = 0.0F;
      float[] m = this.turn.m;
      p.x = this.ex + (double)(m[0] * x) + (double)(m[1] * y) + (double)(m[2] * z);
      p.y = this.ey + (double)(m[4] * x) + (double)(m[5] * y) + (double)(m[6] * z);
      p.z = this.ez + (double)(m[8] * x) + (double)(m[9] * y) + (double)(m[10] * z);
      p.vx = m[0] * vx + m[1] * vy + m[2] * vz;
      p.vy = m[4] * vx + m[5] * vy + m[6] * vz;
      p.vz = m[8] * vx + m[9] * vy + m[10] * vz;
   }

   void update(float dt) {
      if(dt > 0.0F) {
         this.clock = (this.clock + dt) % 1000.0F;
         int i = 0;

         while(i < this.count) {
            WeaponFx.Particle p = this.live[i];
            p.age += dt;
            if(p.age >= p.life) {
               this.live[i] = this.live[--this.count];
               this.live[this.count] = p;
            } else {
               if(p.kind == WeaponFx.Kind.SMOKE) {
                  float drag = (float)Math.exp((double)(-2.2F * dt));
                  p.vx *= drag;
                  p.vy = p.vy * drag + 0.22F * dt;
                  p.vz *= drag;
               } else if(p.kind == WeaponFx.Kind.SPARK) {
                  p.vy -= 3.5F * dt;
               } else if(p.kind == WeaponFx.Kind.MIST) {
                  float drag = (float)Math.exp((double)(-0.8F * dt));
                  p.vx *= drag;
                  p.vz *= drag;
               } else if(p.kind == WeaponFx.Kind.DUST) {
                  float drag = (float)Math.exp((double)(-3.2F * dt));
                  p.vx *= drag;
                  p.vz *= drag;
                  p.vy = p.vy * drag - 0.015F * dt;
               } else if(p.kind == WeaponFx.Kind.LEAF) {
                  float drag = (float)Math.exp((double)(-2.6F * dt));
                  p.vx = p.vx * drag + MathHelper.cos(p.age * 5.5F + p.angle) * 0.3F * dt;
                  p.vz = p.vz * drag + MathHelper.sin(p.age * 4.7F + p.angle) * 0.3F * dt;
                  p.vy += (-0.16F - p.vy) * (1.0F - (float)Math.exp((double)(-3.0F * dt)));
               } else {
                  p.vx += (this.random.nextFloat() - 0.5F) * 0.06F * dt;
                  p.vz += (this.random.nextFloat() - 0.5F) * 0.06F * dt;
               }

               p.x += (double)(p.vx * dt);
               p.y += (double)(p.vy * dt);
               p.z += (double)(p.vz * dt);
               if(p.px != 0.0F || p.py != 0.0F || p.pz != 0.0F) {
                  float fade = (float)Math.exp((double)(-4.0F * dt));
                  p.x += (double)(p.px * dt);
                  p.y += (double)(p.py * dt);
                  p.z += (double)(p.pz * dt);
                  p.px *= fade;
                  p.py *= fade;
                  p.pz *= fade;
                  if(Math.abs(p.px) + Math.abs(p.py) + Math.abs(p.pz) < 1.0E-4F) {
                     p.pz = 0.0F;
                     p.py = 0.0F;
                     p.px = 0.0F;
                  }
               }

               p.size = Math.max(0.001F, p.size + p.grow * dt);
               p.angle += p.spin * dt;
               ++i;
            }
         }
      }

   }

   boolean isEmpty() {
      return this.count == 0;
   }

   int count() {
      return this.count;
   }

   String describe() {
      StringBuilder b = new StringBuilder();

      for(int i = 0; i < this.count && i < 6; ++i) {
         WeaponFx.Particle p = this.live[i];
         boolean front = this.toView(p);
         b.append(String.format(Locale.ROOT, "[%s view (%.2f, %.2f, %.2f)%s size %.3f age %.2f/%.2f a %.2f] ", new Object[]{p.kind, Float.valueOf(this.v[0]), Float.valueOf(this.v[1]), Float.valueOf(this.v[2]), front?"":" behind", Float.valueOf(p.size), Float.valueOf(p.age), Float.valueOf(p.life), Float.valueOf(p.a)}));
      }

      return b.toString();
   }

   void clear() {
      this.count = 0;
   }

   void bounds(Mat4 projection, float[] box) {
      for(int i = 0; i < this.count; ++i) {
         WeaponFx.Particle p = this.live[i];
         if(this.toView(p)) {
            float s = p.size * (p.kind == WeaponFx.Kind.GLINT?3.2F:1.0F);

            for(int k = 0; k < 4; ++k) {
               this.grow(projection, this.v[0] + ((k & 1) == 0?-s:s), this.v[1] + ((k & 2) == 0?-s:s), this.v[2], box);
            }
         }
      }

      for(int var16 = 0; var16 < this.beamCount; ++var16) {
         float[] x = this.beams[var16];
         float w = x[6];

         for(int k = 0; k < 4; ++k) {
            int end = (k & 1) * 3;
            this.grow(projection, x[end] + ((k & 2) == 0?-w:w), x[end + 1] + ((k & 2) == 0?-w:w), x[end + 2], box);
         }
      }

      for(int var17 = 0; var17 < this.arcCount; ++var17) {
         float[] x = this.arcs[var17];
         float w = x[9] * 3.0F + x[13] * 0.5F * (float)Math.sqrt((double)((x[3] - x[0]) * (x[3] - x[0]) + (x[4] - x[1]) * (x[4] - x[1]) + (x[5] - x[2]) * (x[5] - x[2])));

         for(int end = 0; end <= 6; end += 3) {
            for(int k2 = 0; k2 < 4; ++k2) {
               this.grow(projection, x[end] + ((k2 & 1) == 0?-w:w), x[end + 1] + ((k2 & 2) == 0?-w:w), x[end + 2], box);
            }
         }
      }

      int i2;
      for(WeaponFx.Trail t : this.trails) {
         for(int j = 0; j < t.size && (float)(this.now - t.time[i2 = (t.head - j + 64) % 64]) / 1.0E9F < t.life; ++j) {
            this.grow(projection, t.tip[i2][0], t.tip[i2][1], t.tip[i2][2], box);
            this.grow(projection, t.base[i2][0], t.base[i2][1], t.base[i2][2], box);
         }
      }

   }

   private void grow(Mat4 projection, float x, float y, float z, float[] box) {
      projection.transform(x, y, z, this.clip);
      if(this.clip[3] > 1.0E-4F) {
         box[0] = Math.min(box[0], this.clip[0] / this.clip[3]);
         box[1] = Math.min(box[1], this.clip[1] / this.clip[3]);
         box[2] = Math.max(box[2], this.clip[0] / this.clip[3]);
         box[3] = Math.max(box[3], this.clip[1] / this.clip[3]);
      }

   }

   private boolean toView(WeaponFx.Particle p) {
      float dx = (float)(p.x - this.ex);
      float dy = (float)(p.y - this.ey);
      float dz = (float)(p.z - this.ez);
      this.turn.direction(dx, dy, dz, this.v);
      return this.v[2] < -0.03F;
   }

   void draw(float[] light) {
      if(this.hasAnything()) {
         if(this.atlas == null) {
            this.atlas = makeAtlas();
         }

         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.disableTexture2D();
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.enableTexture2D();
         GlStateManager.bindTexture(this.atlas.getGlTextureId());
         GlStateManager.disableFog();
         GlStateManager.disableLighting();
         GlStateManager.disableAlpha();
         GlStateManager.disableCull();
         GlStateManager.enableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         Tessellator t = Tessellator.getInstance();
         WorldRenderer wr = t.getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         this.sortByDepth();

         for(int i = 0; i < this.count; ++i) {
            WeaponFx.Particle p = this.live[i];
            if(this.toView(p)) {
               float k = p.age / p.life;
               float cr = p.r;
               float cg = p.g;
               float cb = p.b;
               boolean additive = p.kind == WeaponFx.Kind.SPARK || p.kind == WeaponFx.Kind.MOTE || p.kind == WeaponFx.Kind.EMBER || p.kind == WeaponFx.Kind.GLINT;
               float alpha;
               if(p.kind == WeaponFx.Kind.SPARK) {
                  alpha = (1.0F - k) * (1.0F - k);
               } else if(p.kind == WeaponFx.Kind.GLINT) {
                  alpha = (float)Math.pow((double)(1.0F - k), 1.5D) * (0.6F + 0.4F * (float)Math.sin((double)(p.age * 38.0F + p.angle * 7.0F)));
               } else if(p.kind == WeaponFx.Kind.MOTE) {
                  alpha = (float)Math.sin(3.141592653589793D * (double)k) * (0.7F + 0.3F * (float)Math.sin((double)(p.age * 11.0F + p.angle * 3.0F)));
               } else if(p.kind == WeaponFx.Kind.EMBER) {
                  alpha = (1.0F - k) * (0.65F + 0.35F * (float)Math.sin((double)(p.age * 27.0F + p.angle * 5.0F)));
               } else if(p.kind == WeaponFx.Kind.LEAF) {
                  alpha = p.a * Math.min(1.0F, k / 0.04F) * Math.min(1.0F, (1.0F - k) / 0.3F);
                  cr *= light[0];
                  cg *= light[1];
                  cb *= light[2];
               } else {
                  float in = p.kind == WeaponFx.Kind.MIST?0.2F:0.1F;
                  alpha = p.a * Math.min(1.0F, k / in) * (float)Math.pow((double)(1.0F - k), 1.2999999523162842D);
                  cr *= light[0];
                  cg *= light[1];
                  cb *= light[2];
               }

               float ca = additive?0.0F:alpha;
               float rr = cr * alpha;
               float gg = cg * alpha;
               float bb = cb * alpha;
               int cell = p.variant;
               float u0 = (float)(cell % 2) * 0.5F;
               float v0 = (float)(cell / 2) * 0.25F;
               if(cell == 4) {
                  u0 = 0.0F;
                  v0 = 0.0F;
               }

               float u1 = u0 + 0.5F;
               float v1 = v0 + 0.25F;
               float s = p.size;
               float c = (float)Math.cos((double)p.angle) * s;
               float sn = (float)Math.sin((double)p.angle) * s;
               float x = this.v[0];
               float y = this.v[1];
               float z = this.v[2];
               wr.pos((double)(x - c + sn), (double)(y - sn - c), (double)z).tex((double)u0, (double)v1).color(rr, gg, bb, ca).endVertex();
               wr.pos((double)(x + c + sn), (double)(y + sn - c), (double)z).tex((double)u1, (double)v1).color(rr, gg, bb, ca).endVertex();
               wr.pos((double)(x + c - sn), (double)(y + sn + c), (double)z).tex((double)u1, (double)v0).color(rr, gg, bb, ca).endVertex();
               wr.pos((double)(x - c - sn), (double)(y - sn + c), (double)z).tex((double)u0, (double)v0).color(rr, gg, bb, ca).endVertex();
               if(p.kind == WeaponFx.Kind.GLINT) {
                  for(int arm = 0; arm < 2; ++arm) {
                     float hw = arm == 0?s * 3.2F:s * 0.3F;
                     float hh = arm == 0?s * 0.3F:s * 3.2F;
                     wr.pos((double)(x - hw), (double)(y - hh), (double)z).tex((double)u0, (double)v1).color(rr * 0.8F, gg * 0.8F, bb * 0.8F, 0.0F).endVertex();
                     wr.pos((double)(x + hw), (double)(y - hh), (double)z).tex((double)u1, (double)v1).color(rr * 0.8F, gg * 0.8F, bb * 0.8F, 0.0F).endVertex();
                     wr.pos((double)(x + hw), (double)(y + hh), (double)z).tex((double)u1, (double)v0).color(rr * 0.8F, gg * 0.8F, bb * 0.8F, 0.0F).endVertex();
                     wr.pos((double)(x - hw), (double)(y + hh), (double)z).tex((double)u0, (double)v0).color(rr * 0.8F, gg * 0.8F, bb * 0.8F, 0.0F).endVertex();
                  }
               }
            }
         }

         t.draw();
         GlStateManager.disableTexture2D();
         wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

         for(int var31 = 0; var31 < this.beamCount; ++var31) {
            if(this.shows(this.beamSide[var31])) {
               drawBeam(wr, this.beams[var31]);
            }
         }

         for(WeaponFx.Trail tr : this.trails) {
            this.drawTrail(wr, tr, this.now);
         }

         t.draw();
         GlStateManager.enableTexture2D();
         if(this.drawSide == 0) {
            drawArcs(this.arcs, this.arcCount);
         } else {
            int n = 0;

            for(int i2 = 0; i2 < this.arcCount; ++i2) {
               if(this.shows(this.arcSide[i2])) {
                  this.shownArcs[n++] = this.arcs[i2];
               }
            }

            drawArcs(this.shownArcs, n);
         }

         GlStateManager.depthMask(true);
      }

   }

   private boolean shows(int s) {
      return this.drawSide == 0 || s == 0 || s == this.drawSide;
   }

   static void drawArcs(float[][] list, int n) {
      if(n > 0) {
         Textures.ensureLoaded();

         for(int i = 0; i < n; ++i) {
            shape(list[i], i);
         }

         Camera before = Vfx.camera();
         Vfx.setCamera(EYE);
         Vfx.resetTextureBinding();
         GlStateManager.enableBlend();
         GlStateManager.depthMask(false);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

         for(int layer = 0; layer < 3; ++layer) {
            if(layer == 0) {
               GlStateManager.tryBlendFuncSeparate(770, 1, 0, 1);
            } else if(layer == 1) {
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            }

            float width = layer == 0?4.2F:(layer == 1?1.0F:0.42F);

            for(int i = 0; i < n; ++i) {
               float[] x = list[i];
               int rgb = Float.floatToRawIntBits(x[10]);
               float alpha = x[11] * (layer == 0?0.5F:(layer == 1?0.8F:1.0F));
               int c = layer == 0?rgb:mixWhite(rgb, layer == 1?0.3F:0.85F);
               int argb = Math.round(Math.min(1.0F, alpha) * 255.0F) << 24 | c & 16777215;

               int off;
               for(int k = off = i * 40; k < off + MAIN[i] + FORK[i]; ++k) {
                  Vfx.W[k] = BASE_W[k] * width;
                  Vfx.C[k] = argb;
               }

               int tex = layer == 2?Textures.LINE_CORE:Textures.LINE_GLOW;
               Vfx.ribbon(tex, off, MAIN[i], false);
               if(FORK[i] > 1) {
                  Vfx.ribbon(tex, off + MAIN[i], FORK[i], false);
               }
            }

            Vfx.flush();
         }

         Vfx.setCamera(before);
         Vfx.resetTextureBinding();
      }

   }

   private static void shape(float[] x, int i) {
      int off = i * 40;
      RNG.seed(Float.floatToRawIntBits(x[12]), 7);
      CX[0] = (double)x[0];
      CY[0] = (double)x[1];
      CZ[0] = (double)x[2];
      CX[1] = (double)x[6];
      CY[1] = (double)x[7];
      CZ[1] = (double)x[8];
      CX[2] = (double)x[3];
      CY[2] = (double)x[4];
      CZ[2] = (double)x[5];
      int m = Procedural.jagged(RNG, off, CX, CY, CZ, 3, 3, (double)x[13], false);

      for(int k = 0; k < m; ++k) {
         BASE_W[off + k] = x[9] * (0.6F + 0.4F * (float)Math.sin(3.141592653589793D * (double)k / (double)(m - 1)));
      }

      MAIN[i] = m;
      FORK[i] = 0;
      if(m > 6 && RNG.next() < 0.5F) {
         int from = off + m / 4 + RNG.nextInt(m / 2);
         double span = Math.sqrt((double)((x[3] - x[0]) * (x[3] - x[0]) + (x[4] - x[1]) * (x[4] - x[1]) + (x[5] - x[2]) * (x[5] - x[2])));
         double ox = Vfx.X[from] - (double)(x[0] + x[3]) * 0.5D;
         double oy = Vfx.Y[from] - (double)(x[1] + x[4]) * 0.5D;
         double oz = Vfx.Z[from] - (double)(x[2] + x[5]) * 0.5D;
         double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
         double k = ol < 1.0E-9D?0.0D:span * 0.45D / ol;
         double ex = Vfx.X[from] + ox * k + (double)RNG.signed() * span * 0.25D;
         double ey = Vfx.Y[from] + oy * k + (double)RNG.signed() * span * 0.25D;
         double ez = Vfx.Z[from] + oz * k + (double)RNG.signed() * span * 0.25D;
         int b = Procedural.bolt(RNG, off + m, Vfx.X[from], Vfx.Y[from], Vfx.Z[from], ex, ey, ez, 2, 0.4D);

         for(int j = 0; j < b; ++j) {
            BASE_W[off + m + j] = x[9] * 0.55F * (1.0F - 0.8F * (float)j / (float)Math.max(1, b - 1));
         }

         FORK[i] = b;
      }

   }

   static void drawArcsHere(float[] arcs, int rgb, float strength, int salt, long ms) {
      drawArcsHere(arcs, rgb, strength, salt, (WeaponShader.Program)null, ms);
   }

   static void drawArcsHere(float[] arcs, int rgb, float strength, int salt, WeaponShader.Program beam, long ms) {
      if(arcs != null && strength > 0.0F) {
         boolean calm = FuriahsVisuals.instance != null && FuriahsVisuals.instance.isReduceFlashing();
         float on = crackle(ms, salt, arcs[7]);
         if(on > 0.0F) {
            if(calm) {
               on = 0.55F;
            }

            float span = (float)Math.sqrt((double)((arcs[3] - arcs[0]) * (arcs[3] - arcs[0]) + (arcs[4] - arcs[1]) * (arcs[4] - arcs[1]) + (arcs[5] - arcs[2]) * (arcs[5] - arcs[2])));
            float[] x = HERE[0];
            float bowX = (arcs[0] + arcs[3]) * 0.5F;
            float bowY = (arcs[1] + arcs[4]) * 0.5F + span * 0.35F;
            float bowZ = (arcs[2] + arcs[5]) * 0.5F;
            if(beam != null) {
               System.arraycopy(arcs, 0, x, 0, 6);
               x[6] = bowX;
               x[7] = bowY;
               x[8] = bowZ;
               x[9] = arcs[6];
            } else {
               MODELVIEW.clear();
               GL11.glGetFloat(2982, MODELVIEW);
               MODELVIEW.get(MV.m);
               float[] a = new float[4];
               float[] b = new float[4];
               float[] bow = new float[4];
               MV.transform(arcs[0], arcs[1], arcs[2], a);
               MV.transform(arcs[3], arcs[4], arcs[5], b);
               MV.transform(bowX, bowY, bowZ, bow);
               float scale = (float)Math.sqrt((double)(MV.m[0] * MV.m[0] + MV.m[1] * MV.m[1] + MV.m[2] * MV.m[2]));
               System.arraycopy(a, 0, x, 0, 3);
               System.arraycopy(b, 0, x, 3, 3);
               System.arraycopy(bow, 0, x, 6, 3);
               x[9] = arcs[6] * scale;
            }

            x[10] = Float.intBitsToFloat(rgb);
            x[11] = Math.min(1.0F, strength * on);
            x[12] = Float.intBitsToFloat((int)(ms / (calm?120L:45L)) * 31 + salt);
            x[13] = 0.32F;
            GlStateManager.pushMatrix();
            if(beam == null) {
               GlStateManager.loadIdentity();
            }

            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();
            GlStateManager.disableFog();
            GlStateManager.enableDepth();
            if(beam != null) {
               drawArcsTurned(HERE, 1, beam);
            } else {
               drawArcs(HERE, 1);
            }

            GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
         }
      }

   }

   private static void drawArcsTurned(float[][] list, int n, WeaponShader.Program beam) {
      Textures.ensureLoaded();

      for(int i = 0; i < n; ++i) {
         shape(list[i], i);
      }

      GlStateManager.enableBlend();
      GlStateManager.depthMask(false);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      beam.use();
      beam.set1f("uTextured", 1.0F);
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();

      for(int layer = 0; layer < 3; ++layer) {
         if(layer == 0) {
            GlStateManager.tryBlendFuncSeparate(770, 1, 0, 1);
         } else if(layer == 1) {
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         }

         float width = layer == 0?4.2F:(layer == 1?1.0F:0.42F);
         GlStateManager.bindTexture(layer == 2?Textures.LINE_CORE:Textures.LINE_GLOW);
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);

         for(int i = 0; i < n; ++i) {
            float[] x = list[i];
            int rgb = Float.floatToRawIntBits(x[10]);
            float alpha = x[11] * (layer == 0?0.5F:(layer == 1?0.8F:1.0F));
            int c = layer == 0?rgb:mixWhite(rgb, layer == 1?0.3F:0.85F);
            int argb = Math.round(Math.min(1.0F, alpha) * 255.0F) << 24 | c & 16777215;

            int off;
            for(int k = off = i * 40; k < off + MAIN[i] + FORK[i]; ++k) {
               Vfx.W[k] = BASE_W[k] * width;
               Vfx.C[k] = argb;
            }

            turnedRibbon(wr, off, MAIN[i]);
            if(FORK[i] > 1) {
               turnedRibbon(wr, off + MAIN[i], FORK[i]);
            }
         }

         t.draw();
      }

      GL20.glUseProgram(0);
      Vfx.resetTextureBinding();
   }

   private static void turnedRibbon(WorldRenderer wr, int offset, int n) {
      int end = offset + n - 1;

      for(int i = offset + 1; i <= end; ++i) {
         if((Vfx.C[i - 1] | Vfx.C[i]) >>> 24 != 0) {
            turnedVertex(wr, i - 1, offset, end, -1.0F);
            turnedVertex(wr, i - 1, offset, end, 1.0F);
            turnedVertex(wr, i, offset, end, 1.0F);
            turnedVertex(wr, i, offset, end, -1.0F);
         }
      }

   }

   private static void turnedVertex(WorldRenderer wr, int i, int offset, int end, float side) {
      int ip = Math.max(offset, i - 1);
      int in = Math.min(end, i + 1);
      float tx = (float)(Vfx.X[in] - Vfx.X[ip]);
      float tz;
      float ty;
      float l = (float)Math.sqrt((double)(tx * tx + (ty = (float)(Vfx.Y[in] - Vfx.Y[ip])) * ty + (tz = (float)(Vfx.Z[in] - Vfx.Z[ip])) * tz));
      if(l < 1.0E-9F) {
         tx = 0.0F;
         ty = 1.0F;
         tz = 0.0F;
      } else {
         tx = tx / l;
         ty = ty / l;
         tz = tz / l;
      }

      int c = Vfx.C[i];
      wr.pos(Vfx.X[i], Vfx.Y[i], Vfx.Z[i]).tex((double)(side * Vfx.W[i] * 0.5F), 0.0D).color((float)(c >> 16 & 255) / 255.0F, (float)(c >> 8 & 255) / 255.0F, (float)(c & 255) / 255.0F, (float)(c >>> 24) / 255.0F).normal(tx, ty, tz).endVertex();
   }

   void drawShimmer() {
      if(this.heatCount != 0 && Distortion.refracts()) {
         long frameId = ++shimmerFrame;
         Camera before = Vfx.camera();
         Vfx.setCamera(EYE);

         try {
            for(int i = 0; i < this.heatCount; ++i) {
               float[] x = this.heats[i];
               Distortion.begin(frameId, 0.009F * x[7] * testShimmer, 16777215, 0.0F, 1.0F, true);

               for(int iu = 0; iu < 16; ++iu) {
                  for(int iv = 0; iv < 6; ++iv) {
                     this.shimmerCorner(x, iu, iv);
                     this.shimmerCorner(x, iu + 1, iv);
                     this.shimmerCorner(x, iu + 1, iv + 1);
                     this.shimmerCorner(x, iu, iv + 1);
                  }
               }

               Distortion.end();
            }
         } finally {
            Vfx.setCamera(before);
         }
      }

   }

   private void shimmerCorner(float[] x, int iu, int iv) {
      float u = (float)iu / 16.0F;
      float v = (float)iv / 6.0F;
      float px = x[0] + (x[3] - x[0]) * u;
      float py = x[1] + (x[4] - x[1]) * u + x[6] * (v - 0.15F);
      float pz = x[2] + (x[5] - x[2]) * u;
      float l = (float)Math.sqrt((double)(px * px + py * py + pz * pz));
      float t = this.clock;
      float amp = (float)Math.pow(Math.sin(3.141592653589793D * (double)u), 0.6D) * smooth(0.0F, 0.15F, v) * smooth(1.0F, 0.7F, v);
      float wx = (float)(Math.sin((double)(u * 21.0F + t * 5.3F) + Math.sin((double)(v * 7.0F - t * 3.1F)) * 1.7000000476837158D) * 0.6000000238418579D + Math.sin((double)(u * 9.0F - t * 2.2F + v * 5.0F)) * 0.4000000059604645D);
      float wy = (float)(Math.sin((double)(v * 13.0F - t * 7.9F) + Math.sin((double)(u * 11.0F + t * 2.4F)) * 1.2999999523162842D) * 0.699999988079071D + Math.sin((double)(u * 17.0F + t * 4.1F)) * 0.30000001192092896D);
      float nx = -px / l + wx * amp * 0.9F;
      float ny = -py / l + wy * amp * 0.9F;
      float nz = -pz / l;
      float nl = (float)Math.sqrt((double)(nx * nx + ny * ny + nz * nz));
      Distortion.vertex((double)px, (double)py, (double)pz, (double)(nx / nl), (double)(ny / nl), (double)(nz / nl));
   }

   static float crackle(long ms, int salt, float rate) {
      if(ViewModels.testArcs) {
         return 1.0F;
      } else {
         long slot = ms / 70L;
         float chance = Math.min(0.9F, rate * 0.11F);
         return Rng.hash((int)slot, salt) >= chance?0.0F:0.55F + 0.45F * Rng.hash((int)(ms / 35L), salt + 1);
      }
   }

   static void drawGlowHere(WeaponDef def, int rgb, float strength) {
      float[] g = def.glow;
      if(g != null && strength > 0.0F) {
         MODELVIEW.clear();
         GL11.glGetFloat(2982, MODELVIEW);
         MODELVIEW.get(MV.m);
         float[] a = new float[4];
         float[] b = new float[4];
         MV.transform(g[0], g[1], g[2], a);
         MV.transform(g[3], g[4], g[5], b);
         float scale = (float)Math.sqrt((double)(MV.m[0] * MV.m[0] + MV.m[1] * MV.m[1] + MV.m[2] * MV.m[2]));
         float[] wide = beamOf(a, b, g[6] * scale, rgb, 0.42F * strength);
         float[] core = beamOf(a, b, g[6] * scale * 0.38F, mixWhite(rgb, 0.45F), 0.38F * strength);
         GlStateManager.pushMatrix();
         GlStateManager.loadIdentity();
         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.disableTexture2D();
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.disableTexture2D();
         GlStateManager.disableLighting();
         GlStateManager.disableAlpha();
         GlStateManager.disableCull();
         GlStateManager.disableFog();
         GlStateManager.enableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
         Tessellator t = Tessellator.getInstance();
         WorldRenderer wr = t.getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
         drawBeam(wr, wide);
         drawBeam(wr, core);
         t.draw();
         GlStateManager.depthMask(true);
         GlStateManager.enableTexture2D();
         GlStateManager.popMatrix();
      }

   }

   static void drawGlowHere(WeaponDef def, int rgb, float strength, WeaponShader.Program beam) {
      float[] g = def.glow;
      if(beam == null) {
         drawGlowHere(def, rgb, strength);
      } else if(g != null && strength > 0.0F) {
         float dx = g[3] - g[0];
         float dy = g[4] - g[1];
         float dz = g[5] - g[2];
         float len = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
         if(len >= 1.0E-6F) {
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();
            GlStateManager.disableFog();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
            beam.use();
            beam.set1f("uTextured", 0.0F);
            Tessellator t = Tessellator.getInstance();
            WorldRenderer wr = t.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
            float[] dir = new float[]{dx / len, dy / len, dz / len};
            beamQuads(wr, g, dir, g[6] * 0.5F, rgb, 0.42F * strength);
            beamQuads(wr, g, dir, g[6] * 0.19F, mixWhite(rgb, 0.45F), 0.38F * strength);
            t.draw();
            GL20.glUseProgram(0);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
         }
      }

   }

   private static void beamQuads(WorldRenderer wr, float[] g, float[] dir, float half, int rgb, float intensity) {
      float r = (float)(rgb >> 16 & 255) / 255.0F * intensity;
      float gr = (float)(rgb >> 8 & 255) / 255.0F * intensity;
      float b = (float)(rgb & 255) / 255.0F * intensity;

      for(int j = 0; j < ALONG.length - 1; ++j) {
         float g0 = ALONG_WEIGHT[j];
         float g1 = ALONG_WEIGHT[j + 1];

         for(int k = 0; k < ACROSS.length - 1; ++k) {
            float a0 = ACROSS[k] * half;
            float a1 = ACROSS[k + 1] * half;
            float w0 = WEIGHT[k];
            float w1 = WEIGHT[k + 1];
            beamVertex(wr, g, dir, j, a0, half, r * w0 * g0, gr * w0 * g0, b * w0 * g0);
            beamVertex(wr, g, dir, j + 1, a0, half, r * w0 * g1, gr * w0 * g1, b * w0 * g1);
            beamVertex(wr, g, dir, j + 1, a1, half, r * w1 * g1, gr * w1 * g1, b * w1 * g1);
            beamVertex(wr, g, dir, j, a1, half, r * w1 * g0, gr * w1 * g0, b * w1 * g0);
         }
      }

   }

   private static void beamVertex(WorldRenderer wr, float[] g, float[] dir, int j, float across, float half, float r, float gr, float b) {
      int o = j <= 1?0:3;
      float past = j == 0?-half:(j == 3?half:0.0F);
      wr.pos((double)g[o], (double)g[o + 1], (double)g[o + 2]).tex((double)across, (double)past).color(r, gr, b, 0.0F).normal(dir[0], dir[1], dir[2]).endVertex();
   }

   private static float[] beamOf(float[] a, float[] b, float width, int rgb, float intensity) {
      return new float[]{a[0], a[1], a[2], b[0], b[1], b[2], width, (float)(rgb >> 16 & 255) / 255.0F * intensity, (float)(rgb >> 8 & 255) / 255.0F * intensity, (float)(rgb & 255) / 255.0F * intensity};
   }

   static int mixWhite(int rgb, float k) {
      int r = Math.round((float)(rgb >> 16 & 255) + (float)(255 - (rgb >> 16 & 255)) * k);
      int g = Math.round((float)(rgb >> 8 & 255) + (float)(255 - (rgb >> 8 & 255)) * k);
      int b = Math.round((float)(rgb & 255) + (float)(255 - (rgb & 255)) * k);
      return r << 16 | g << 8 | b;
   }

   private static void drawBeam(WorldRenderer wr, float[] x) {
      float dx = x[3] - x[0];
      float dy = x[4] - x[1];
      float dz = x[5] - x[2];
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
      float mz = (x[2] + x[5]) * 0.5F;
      float my = (x[1] + x[4]) * 0.5F;
      float sx = dy * mz - dz * my;
      float mx = (x[0] + x[3]) * 0.5F;
      float sy = dz * mx - dx * mz;
      float sz = dx * my - dy * mx;
      float l = (float)Math.sqrt((double)(sx * sx + sy * sy + sz * sz));
      if(l >= 1.0E-6F && len >= 1.0E-6F) {
         float h = x[6] * 0.5F / l;
         sx = sx * h;
         sy = sy * h;
         sz = sz * h;
         float e = x[6] * 0.5F / len;
         float ex = dx * e;
         float ey = dy * e;
         float ez = dz * e;

         for(int j = 0; j < ALONG.length - 1; ++j) {
            float[] p0 = alongPoint(x, j, ex, ey, ez);
            float[] p1 = alongPoint(x, j + 1, ex, ey, ez);
            float g0 = ALONG_WEIGHT[j];
            float g1 = ALONG_WEIGHT[j + 1];

            for(int k = 0; k < ACROSS.length - 1; ++k) {
               float a0 = ACROSS[k];
               float a1 = ACROSS[k + 1];
               float w0 = WEIGHT[k];
               float w1 = WEIGHT[k + 1];
               wr.pos((double)(p0[0] + sx * a0), (double)(p0[1] + sy * a0), (double)(p0[2] + sz * a0)).color(x[7] * w0 * g0, x[8] * w0 * g0, x[9] * w0 * g0, 0.0F).endVertex();
               wr.pos((double)(p1[0] + sx * a0), (double)(p1[1] + sy * a0), (double)(p1[2] + sz * a0)).color(x[7] * w0 * g1, x[8] * w0 * g1, x[9] * w0 * g1, 0.0F).endVertex();
               wr.pos((double)(p1[0] + sx * a1), (double)(p1[1] + sy * a1), (double)(p1[2] + sz * a1)).color(x[7] * w1 * g1, x[8] * w1 * g1, x[9] * w1 * g1, 0.0F).endVertex();
               wr.pos((double)(p0[0] + sx * a1), (double)(p0[1] + sy * a1), (double)(p0[2] + sz * a1)).color(x[7] * w1 * g0, x[8] * w1 * g0, x[9] * w1 * g0, 0.0F).endVertex();
            }
         }
      }

   }

   private static float[] alongPoint(float[] x, int j, float ex, float ey, float ez) {
      float[] p = ALONG_POINTS[j];
      if(j == 0) {
         p[0] = x[0] - ex;
         p[1] = x[1] - ey;
         p[2] = x[2] - ez;
      } else if(j == 1) {
         p[0] = x[0];
         p[1] = x[1];
         p[2] = x[2];
      } else if(j == 2) {
         p[0] = x[3];
         p[1] = x[4];
         p[2] = x[5];
      } else {
         p[0] = x[3] + ex;
         p[1] = x[4] + ey;
         p[2] = x[5] + ez;
      }

      return p;
   }

   private void drawTrail(WorldRenderer wr, WeaponFx.Trail t, long now) {
      if(t.size >= 2) {
         int prev = t.head;
         float prevFade = fade(t, prev, now);

         for(int j = 1; j < t.size; ++j) {
            int i = (t.head - j + 64) % 64;
            float f = fade(t, i, now);
            if(prevFade <= 0.0F) {
               break;
            }

            float r0 = t.r * prevFade;
            float g0 = t.g * prevFade;
            float b0 = t.b * prevFade;
            float r1 = t.r * f;
            float g1 = t.g * f;
            float b1 = t.b * f;
            float[] tp = t.tip[prev];
            float[] bp = t.base[prev];
            float[] ti = t.tip[i];
            float[] bi = t.base[i];
            wr.pos((double)bp[0], (double)bp[1], (double)bp[2]).color(0.0F, 0.0F, 0.0F, 0.0F).endVertex();
            wr.pos((double)tp[0], (double)tp[1], (double)tp[2]).color(r0, g0, b0, 0.0F).endVertex();
            wr.pos((double)ti[0], (double)ti[1], (double)ti[2]).color(r1, g1, b1, 0.0F).endVertex();
            wr.pos((double)bi[0], (double)bi[1], (double)bi[2]).color(0.0F, 0.0F, 0.0F, 0.0F).endVertex();
            prev = i;
            prevFade = f;
         }
      }

   }

   private static float fade(WeaponFx.Trail t, int i, long now) {
      float age = (float)(now - t.time[i]) / 1.0E9F;
      float k = 1.0F - age / t.life;
      return k <= 0.0F?0.0F:k * k * t.strength;
   }

   private void sortByDepth() {
      for(int i = 1; i < this.count; ++i) {
         WeaponFx.Particle p = this.live[i];
         float d = this.depth(p);

         int j;
         for(j = i - 1; j >= 0 && this.depth(this.live[j]) > d; --j) {
            this.live[j + 1] = this.live[j];
         }

         this.live[j + 1] = p;
      }

   }

   private float depth(WeaponFx.Particle p) {
      float dx = (float)(p.x - this.ex);
      float dy = (float)(p.y - this.ey);
      float dz = (float)(p.z - this.ez);
      float[] m = this.turn.m;
      return m[2] * dx + m[6] * dy + m[10] * dz;
   }

   private static DynamicTexture makeAtlas() {
      DynamicTexture tex = new DynamicTexture(128, 256);
      int[] data = tex.getTextureData();
      int cellSize = 64;

      for(int cell = 6; cell < 8; ++cell) {
         int ox = cell % 2 * cellSize;
         int oy = cell / 2 * cellSize;
         float bend = cell == 7?0.22F:0.0F;

         for(int y = 0; y < cellSize; ++y) {
            for(int x = 0; x < cellSize; ++x) {
               float px = ((float)x + 0.5F) / (float)cellSize * 2.0F - 1.0F;
               float py = ((float)y + 0.5F) / (float)cellSize * 2.0F - 1.0F;
               float mid = bend * (1.0F - py * py);
               float along = (py + 0.9F) / 1.8F;
               float w = along > 0.0F && along < 1.0F?0.46F * (float)Math.pow(Math.sin(3.141592653589793D * Math.pow((double)along, 0.8D)), 0.85D):0.0F;
               float d = Math.abs(px - mid);
               float a = w <= 0.0F?0.0F:1.0F - smooth(w - 0.06F, w + 0.02F, d);
               float rib = 1.0F - smooth(0.0F, 0.05F, d);
               float vein = (float)Math.pow(Math.abs(Math.sin((double)(d * 1.4F + py) * 9.0D)), 14.0D) * 0.5F;
               float lum = 0.95F - 0.25F * rib - 0.15F * vein;
               int av = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
               int lv = Math.round(Math.max(0.0F, Math.min(1.0F, a * lum)) * 255.0F);
               data[(oy + y) * 128 + ox + x] = av << 24 | lv << 16 | lv << 8 | lv;
            }
         }
      }

      for(int var21 = 0; var21 < 4; ++var21) {
         Random r = new Random(4410L + (long)var21 * 7919L);
         float[] lobes = new float[12];

         for(int i = 0; i < lobes.length; ++i) {
            lobes[i] = r.nextFloat();
         }

         int ox = var21 % 2 * cellSize;
         int oy = var21 / 2 * cellSize;

         for(int y = 0; y < cellSize; ++y) {
            for(int x = 0; x < cellSize; ++x) {
               float px = ((float)x + 0.5F) / (float)cellSize * 2.0F - 1.0F;
               float py = ((float)y + 0.5F) / (float)cellSize * 2.0F - 1.0F;
               float d = (float)Math.sqrt((double)(px * px + py * py));
               float ang = (float)Math.atan2((double)py, (double)px);
               float edge = 0.78F;

               for(int k = 0; k < 4; ++k) {
                  edge += 0.07F * (float)Math.sin((double)(ang * (float)(k + 2) + lobes[k] * 6.28F)) * lobes[4 + k];
               }

               float f = 1.0F - smooth(0.15F * edge, edge, d);
               float n = 0.65F + 0.35F * (float)(Math.sin((double)px * 7.1D + (double)lobes[8] * 6.0D) * Math.sin((double)py * 6.3D + (double)lobes[9] * 6.0D) + 0.5D * Math.sin((double)(px + py) * 11.0D + (double)lobes[10] * 6.0D)) / 1.5F;
               float a = Math.max(0.0F, Math.min(1.0F, f * n));
               int av = Math.round(a * 255.0F);
               data[(oy + y) * 128 + ox + x] = av << 24 | av << 16 | av << 8 | av;
            }
         }
      }

      tex.updateDynamicTexture();
      GlStateManager.bindTexture(tex.getGlTextureId());
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 10496);
      GL11.glTexParameteri(3553, 10243, 10496);
      return tex;
   }

   private static float smooth(float a, float b, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
      return t * t * (3.0F - 2.0F * t);
   }

   void release() {
      if(this.atlas != null) {
         this.atlas.deleteGlTexture();
         this.atlas = null;
      }

   }

   static {
      EYE.rightX = 1.0D;
      EYE.upY = 1.0D;
      EYE.forwardZ = -1.0D;
   }

   static enum Kind {
      SMOKE,
      SPARK,
      MOTE,
      EMBER,
      MIST,
      GLINT,
      DUST,
      LEAF;
   }

   private static final class Particle {
      double x;
      double y;
      double z;
      float vx;
      float vy;
      float vz;
      float px;
      float py;
      float pz;
      float age;
      float life;
      float size;
      float grow;
      float angle;
      float spin;
      float r;
      float g;
      float b;
      float a;
      WeaponFx.Kind kind;
      int variant;

      private Particle() {
      }
   }

   private static final class Trail {
      static final int SAMPLES = 64;
      final float[][] tip;
      final float[][] base;
      final long[] time;
      int head;
      int size;
      float r;
      float g;
      float b;
      float strength;
      float life;

      private Trail() {
         this.tip = new float[64][3];
         this.base = new float[64][3];
         this.time = new long[64];
      }
   }
}
