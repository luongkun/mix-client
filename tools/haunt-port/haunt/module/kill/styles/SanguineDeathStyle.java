package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.haunt.fx.HauntKit;
import mix.client.mixui.integration.haunt.haunt.fx.HauntTextures;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class SanguineDeathStyle extends KillStyleBase {
   private static final float APPEAR = 0.1F;
   private static final float HOLD = 0.24F;
   private static final float AFTER = 1.15F;
   private static final int STREAKS = 12;
   private final SliderSetting skullSize = (SliderSetting)this.setting((new SliderSetting("sanguine.size", "Skull Size", "How big the skull gets before it bursts.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting grow = (SliderSetting)this.setting((new SliderSetting("sanguine.grow", "Grow Time", "How long the skull takes to swell to its full size.", 0.5D, 2.2D, 1.1D, 0.05D)).unit(" s"));
   private final SliderSetting burst = (SliderSetting)this.setting((new SliderSetting("sanguine.burst", "Burst Strength", "How far the sparks and droplets fly.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting amount = (SliderSetting)this.setting((new SliderSetting("sanguine.amount", "Particles", "How many sparks, droplets and embers the burst throws.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting laugh = (BooleanSetting)this.setting(new BooleanSetting("sanguine.laugh", "Laugh", "The jaw chatters while the skull grows.", true));
   private final BooleanSetting stain = (BooleanSetting)this.setting(new BooleanSetting("sanguine.stain", "Ground Stain", "A blood splash stays on the ground for a moment.", true));
   private final ColorSetting sparks = (ColorSetting)this.setting((new ColorSetting("sanguine.sparks", "Spark Color", "Color of the hottest sparks and the flash.", -34238)).secondary());

   public SanguineDeathStyle(KillEffectsModule module) {
      super(module, KillStyle.SANGUINE_DEATH, new ColorSetting("sanguine.color", "Blood", "Color of the skull\'s glow, the mist and the droplets.", -2876372));
   }

   private float growTime(KillFx fx) {
      return this.grow.getFloat() * (fx.preview?0.8F:1.0F);
   }

   private float burstAt(KillFx fx) {
      return 0.1F + this.growTime(fx) + 0.24F;
   }

   public float seconds(KillFx fx) {
      return this.burstAt(fx) + 1.15F;
   }

   public void start(KillFx fx) {
      HauntTextures.warmUp();
      SanguineDeathStyle.State s = new SanguineDeathStyle.State();
      fx.data = s;
      s.cx = fx.ctx.feetX;
      s.cy = fx.ctx.feetY + 1.2D;
      s.cz = fx.ctx.feetZ;

      for(int k = 0; k < 12; ++k) {
         s.streakAngle[k] = fx.rng.next() * 6.2832F;
         s.streakStart[k] = fx.rng.next() * 0.7F;
         s.streakRadius[k] = 0.9F + fx.rng.next() * 0.9F;
         s.streakTilt[k] = fx.rng.signed() * 0.9F;
      }

      for(int k = 0; k < 3; ++k) {
         s.fragSpin[k] = fx.rng.signed() * 9.0F;
      }

   }

   private double size(KillFx fx, float t) {
      double full = 1.0D * (double)this.skullSize.getFloat() * (double)fx.size * (fx.crit?1.2D:1.0D);
      float g = Easing.clamp01((t - 0.1F) / this.growTime(fx));
      float e = Easing.inOutSine(g) + 0.06F * (float)Math.sin((double)(g * 25.0F)) * g * (1.0F - g);
      double small = 0.14D;
      double sz = full * (small + (1.0D - small) * (double)Math.max(0.0F, e));
      float pre = Easing.clamp01((t - (this.burstAt(fx) - 0.24F)) / 0.24F);
      sz = sz * (1.0D + 0.12D * (double)Easing.inQuad(pre));
      return t < 0.1F?full * small * (double)Easing.outCubic(t / 0.1F):sz;
   }

   public void update(KillFx fx, float dt) {
      SanguineDeathStyle.State s = (SanguineDeathStyle.State)fx.data;
      float t = fx.t;
      float b = this.burstAt(fx);
      if(!s.misted && t > 0.02F) {
         s.misted = true;
         fx.spawnSkinDust(fx.count(55.0F, false), 0.6F, 0.7F);
      }

      if(!fx.lod && t > 0.1F && t < b && fx.rng.next() < dt * 26.0F * this.amount.getFloat()) {
         double a = (double)fx.rng.next() * 6.2832D;
         double e = (double)fx.rng.signed() * 0.9D;
         double r = 1.1D + (double)fx.rng.next() * 0.9D;
         double x = s.cx + Math.cos(a) * Math.cos(e) * r;
         double y = s.cy + Math.sin(e) * r * 0.7D;
         double z = s.cz + Math.sin(a) * Math.cos(e) * r;
         float life = 0.42F + fx.rng.next() * 0.2F;
         fx.particles.spawn(3, x, y, z, (float)((s.cx - x) / (double)life), (float)((s.cy - y) / (double)life), (float)((s.cz - z) / (double)life), life, 0.05F * fx.size).end(0.01F).color(this.color, fx.rng.next()).alpha(fx.opacity).hot(0.5F);
      }

      if(!s.burst && t >= b) {
         s.burst = true;
         this.explode(fx, s);
      }

   }

   private void explode(KillFx fx, SanguineDeathStyle.State s) {
      double k = (double)this.burst.getFloat() * (fx.crit?1.3D:1.0D) * (fx.preview?0.55D:1.0D);
      double r = this.size(fx, this.burstAt(fx)) * 0.45D;
      float amt = this.amount.getFloat();
      int sparksN = fx.count(70.0F * amt, true);

      for(int i = 0; i < sparksN; ++i) {
         double a = (double)fx.rng.next() * 6.2832D;
         double e = Math.asin((double)fx.rng.signed());
         double sp = (3.5D + (double)fx.rng.next() * 6.0D) * k;
         double dx = Math.cos(a) * Math.cos(e);
         double dy = Math.sin(e) * 0.85D + 0.15D;
         double dz = Math.sin(a) * Math.cos(e);
         fx.particles.spawn(1, s.cx + dx * r, s.cy + dy * r, s.cz + dz * r, (float)(dx * sp), (float)(dy * sp), (float)(dz * sp), 0.45F + fx.rng.next() * 0.5F, (0.05F + fx.rng.next() * 0.05F) * fx.size).drag(2.2F).gravity(4.0F).color(fx.rng.next() < 0.35F?this.sparks.getColor(fx.rng.next()):this.color.getColor(fx.rng.next())).alpha(fx.opacity).hot(0.9F);
      }

      int drops = fx.count(46.0F * amt, true);

      for(int i = 0; i < drops; ++i) {
         double a = (double)fx.rng.next() * 6.2832D;
         double sp = (1.6D + (double)fx.rng.next() * 3.4D) * k;
         double up = 1.5D + (double)fx.rng.next() * 3.2D;
         fx.particles.spawn(6, s.cx + (double)(fx.rng.signed() * 0.15F), s.cy + (double)(fx.rng.signed() * 0.15F), s.cz + (double)(fx.rng.signed() * 0.15F), (float)(Math.cos(a) * sp), (float)(up * k), (float)(Math.sin(a) * sp), 0.9F + fx.rng.next() * 0.6F, (0.04F + fx.rng.next() * 0.05F) * fx.size).gravity(18.0F).drag(0.4F).ground(fx.ctx.groundY + 0.01D, 1, 0.15F).color(ColorUtil.lerp(this.color.getColor(fx.rng.next()), -16777216, 0.25F * fx.rng.next())).alpha(fx.opacity);
      }

      if(!fx.lod) {
         for(int i = 0; i < fx.count(26.0F * amt, false); ++i) {
            fx.particles.spawn(7, s.cx + (double)(fx.rng.signed() * 0.4F), s.cy + (double)(fx.rng.signed() * 0.4F), s.cz + (double)(fx.rng.signed() * 0.4F), fx.rng.signed() * 1.2F, 0.6F + fx.rng.next() * 1.4F, fx.rng.signed() * 1.2F, 1.1F + fx.rng.next() * 0.9F, (0.03F + fx.rng.next() * 0.03F) * fx.size).drag(1.4F).color(this.sparks.getColor(fx.rng.next())).alpha(fx.opacity).hot(1.0F).flags(2);
         }

         for(int i = 0; i < 10; ++i) {
            fx.particles.spawn(8, s.cx + (double)(fx.rng.signed() * 0.3F), s.cy + (double)(fx.rng.signed() * 0.3F), s.cz + (double)(fx.rng.signed() * 0.3F), fx.rng.signed() * 1.6F, 0.2F + fx.rng.next() * 0.8F, fx.rng.signed() * 1.6F, 1.0F, (0.35F + fx.rng.next() * 0.3F) * fx.size).end(1.1F * fx.size).drag(1.8F).color(ColorUtil.lerp(this.color.getColor(0.5F), -16777216, 0.45F)).alpha(0.55F * fx.opacity);
         }
      }

      for(int i = 0; i < 3; ++i) {
         double a = 1.5707963267948966D + (double)i * 2.0944D + (double)(fx.rng.signed() * 0.3F);
         s.fragVx[i] = (float)(Math.cos(a) * 2.6D * k);
         s.fragVy[i] = (float)(Math.sin(a) * 2.0D * k + 2.2D);
      }

   }

   public void render(KillFx fx, FxFrame f) {
      SanguineDeathStyle.State s = (SanguineDeathStyle.State)fx.data;
      float t = fx.t;
      float bodyGone = Easing.clamp01(t / 0.38F);
      if(bodyGone < 1.0F) {
         for(int i = 0; i < 6; ++i) {
            fx.parts[i].copy(fx.rest[i]);
            fx.parts[i].y -= 0.15D * (double)Easing.inQuad(bodyGone);
            fx.parts[i].alpha = 1.0F - Easing.smoothstep(0.35F, 1.0F, bodyGone);
         }

         fx.look.tint = this.color.getColor(0.5F) | -16777216;
         fx.look.tintAmount = 0.85F * Easing.smoothstep(0.0F, 0.6F, bodyGone);
         fx.look.glow = this.color.getColor(0.5F);
         fx.look.glowAmount = 0.5F * Easing.smoothstep(0.2F, 1.0F, bodyGone);
         fx.drawBody(f);
         fx.look.tintAmount = 0.0F;
         fx.look.glowAmount = 0.0F;
      }

      if(HauntTextures.ensure()) {
         float a = fx.alpha();
         float b = this.burstAt(fx);
         double cx = f.rx(s.cx);
         double cyBase = f.ry(s.cy);
         double cz = f.rz(s.cz);
         int blood = this.color.getColor(0.5F) | -16777216;
         if(t < b) {
            float g = Easing.clamp01((t - 0.1F) / this.growTime(fx));
            float pre = Easing.clamp01((t - (b - 0.24F)) / 0.24F);
            double size = this.size(fx, t);
            double bob = Math.sin((double)(t * 5.0F)) * 0.04D * (double)fx.size;
            double shake = (double)pre * 0.035D * (double)fx.size;
            double sx = (double)fx.rng.signed() * shake;
            double sy = (double)fx.rng.signed() * shake;
            double cy = cyBase + bob;
            float roll = (float)Math.sin((double)(t * 2.3F)) * 7.0F + fx.rng.signed() * 10.0F * pre;
            RenderState.additive();
            this.drawRitual(fx, f, s, t, a, g, blood);
            this.drawStreaks(fx, f, s, t, a);
            Vfx.sprite(Textures.GLOW, cx, cy, cz, size * (3.2D + 0.6D * Math.sin((double)(t * 9.0F))), 0.0F, alpha(blood, (0.35F + 0.4F * pre) * a));
            Vfx.flush();
            RenderState.translucent(false);
            float jawOpen = this.laugh.get()?0.05F + 0.06F * Math.abs((float)Math.sin((double)(t * 19.0F))) * Math.min(1.0F, g * 2.0F):0.04F;
            jawOpen = jawOpen + 0.07F * Easing.outCubic(pre);
            float heat = 0.45F * pre;
            int tint = ColorUtil.lerp(-1, -1068, heat);
            billboard(HauntTextures.SKULL_JAW, cx + sx, cy + sy, cz, size, -0.5D, -0.2D - (double)jawOpen - 0.5D, 0.5D, -0.2D - (double)jawOpen, 0.0F, 0.0F, 1.0F, 1.0F, roll, alpha(tint, a));
            billboard(HauntTextures.SKULL_HEAD, cx + sx, cy + sy, cz, size, -0.5D, -0.5D, 0.5D, 0.5D, 0.0F, 0.0F, 1.0F, 1.0F, roll, alpha(tint, a));
            Vfx.flush();
            RenderState.additive();
            billboard(HauntTextures.SKULL_FLAT, cx + sx, cy + sy, cz, size * 1.08D, -0.5D, -0.5D, 0.5D, 0.5D, 0.0F, 0.0F, 1.0F, 1.0F, roll, alpha(blood, (0.18F + 0.55F * pre) * a));
            Vfx.flush();
         } else {
            float k = (t - b) / 1.15F;
            double size = this.size(fx, b);
            RenderState.additive();
            float fl = 1.0F - Easing.clamp01(k / 0.35F);
            if(fl > 0.0F) {
               Vfx.sprite(Textures.GLOW, cx, cyBase, cz, (5.0D + 2.0D * (double)(1.0F - fl)) * (double)fx.size, 0.0F, alpha(blood, 0.8F * a * fl * fl * Math.max(0.4F, fx.glow + 0.4F)));
               Vfx.sprite(Textures.FLARE, cx, cyBase, cz, 3.2D * (double)fx.size, k * 3.0F, alpha(ColorUtil.core(this.sparks.getColor(0.5F)), a * fl));
            }

            float ring = Easing.outCubic(Easing.clamp01(k / 0.55F));
            if(ring < 1.0F) {
               double rr = (0.25D + 2.4D * (double)ring) * (double)fx.size * (double)this.burst.getFloat();
               HauntKit.ring(Textures.LINE_GLOW, cx, cyBase, cz, rr, 0.35D * (double)fx.size * (double)(1.0F - ring), alpha(blood, 0.8F * a * (1.0F - ring)));
               Vfx.flatRing(Textures.LINE_GLOW, cx, f.ry(fx.ctx.groundY + 0.03D), cz, rr * 1.2D, 0.4D * (double)fx.size * (double)(1.0F - ring), 48, alpha(blood, 0.6F * a * (1.0F - ring)));
            }

            Vfx.flush();
            RenderState.translucent(false);
            float fa = a * (1.0F - Easing.smoothstep(0.45F, 1.0F, k));
            double tt = (double)(t - b);

            for(int i = 0; i < 3; ++i) {
               double fx0 = (double)s.fragVx[i] * tt;
               double fy0 = (double)s.fragVy[i] * tt - 4.5D * tt * tt;
               float u0 = i == 0?0.0F:(i == 1?0.0F:0.5F);
               float v0 = i == 0?0.0F:0.5F;
               float u1 = i == 0?1.0F:(i == 1?0.5F:1.0F);
               float v1 = i == 0?0.5F:1.0F;
               double lx0 = (double)u0 - 0.5D;
               double lx1 = (double)u1 - 0.5D;
               double ly0 = 0.5D - (double)v1;
               double ly1 = 0.5D - (double)v0;
               billboard(HauntTextures.SKULL_HEAD, cx + fx0 * (double)fx.size, cyBase + fy0 * (double)fx.size, cz, size * (1.0D - 0.3D * (double)k), lx0, ly0, lx1, ly1, u0, v0, u1, v1, s.fragSpin[i] * (float)tt * 57.0F, alpha(-1, fa));
            }

            if(this.stain.get()) {
               float sa = Easing.smoothstep(0.05F, 0.25F, k) * (1.0F - Easing.smoothstep(0.6F, 1.0F, k));
               if(sa > 0.01F) {
                  RenderState.translucent(false);
                  Vfx.flatSprite(HauntTextures.SPLASH, cx, f.ry(fx.ctx.groundY + 0.015D), cz, (0.7D + 0.5D * (double)Easing.outCubic(Easing.clamp01(k * 3.0F))) * (double)fx.size * (double)this.burst.getFloat(), (double)s.streakAngle[0], alpha(ColorUtil.lerp(blood, -16777216, 0.35F), 0.75F * sa * a));
               }
            }

            Vfx.flush();
         }
      }

   }

   private void drawRitual(KillFx fx, FxFrame f, SanguineDeathStyle.State s, float t, float a, float g, int blood) {
      double gy = f.ry(fx.ctx.groundY + 0.03D);
      double cx = f.rx(s.cx);
      double cz = f.rz(s.cz);
      float in = Easing.outCubic(Easing.clamp01(t / 0.5F));
      double r = (0.65D + 0.35D * (double)g) * (double)fx.size;
      Vfx.flatSprite(Textures.GROUND_GLOW, cx, gy, cz, r * 1.4D, (double)(t * 0.6F), alpha(blood, 0.35F * a * in));
      Vfx.flatRing(Textures.LINE_GLOW, cx, gy, cz, r, 0.06D * (double)fx.size, 48, alpha(blood, 0.75F * a * in));
      Vfx.flatRing(Textures.LINE_GLOW, cx, gy, cz, r * 0.72D, 0.04D * (double)fx.size, 48, alpha(blood, 0.5F * a * in));
      Vfx.flatBand(Textures.RUNES, cx, gy, cz, r * 0.76D, r * 0.96D, (double)(t * 0.8F), 6.283185307179586D, 40, alpha(blood, 0.7F * a * in));
   }

   private void drawStreaks(KillFx fx, FxFrame f, SanguineDeathStyle.State s, float t, float a) {
      float p = Easing.clamp01((t - 0.1F) / this.growTime(fx));
      if(p > 0.0F && p < 1.0F && !fx.lod) {
         int offset = 0;

         for(int k = 0; k < 12; ++k) {
            float u = (p - s.streakStart[k] * 0.55F) / 0.45F;
            if(u > 0.0F && u < 1.0F) {
               for(int i = 0; i < 9; ++i) {
                  float q = Easing.clamp01(u - (float)i * 0.04F);
                  double r = (double)(s.streakRadius[k] * fx.size * (1.0F - Easing.inCubic(q))) + 0.05D;
                  double ang = (double)s.streakAngle[k] + (double)q * 3.8D;
                  Vfx.X[offset + i] = f.rx(s.cx + Math.cos(ang) * r);
                  Vfx.Y[offset + i] = f.ry(s.cy + Math.sin(ang * 0.5D + (double)s.streakTilt[k]) * r * 0.35D);
                  Vfx.Z[offset + i] = f.rz(s.cz + Math.sin(ang) * r);
                  Vfx.W[offset + i] = (float)(0.045D * (double)fx.size * (double)(1.0F - (float)i / 9.0F));
                  Vfx.C[offset + i] = alpha(this.color.getColor((float)k / 12.0F), a * (1.0F - (float)i / 9.0F) * (1.0F - u));
               }

               Vfx.ribbon(Textures.LINE_GLOW, offset, 9, false);
               offset += 9;
            }
         }
      }

   }

   static void billboard(int tex, double cx, double cy, double cz, double size, double x0, double y0, double x1, double y1, float u0, float v0, float u1, float v1, float rollDeg, int color) {
      double rx = Vfx.camera().rightX;
      double ry = Vfx.camera().rightY;
      double rz = Vfx.camera().rightZ;
      double ux = Vfx.camera().upX;
      double uy = Vfx.camera().upY;
      double uz = Vfx.camera().upZ;
      double c = Math.cos(Math.toRadians((double)rollDeg));
      double sn = Math.sin(Math.toRadians((double)rollDeg));
      double[] xs = new double[]{x0, x1, x1, x0};
      double[] ys = new double[]{y0, y0, y1, y1};
      double[] px = new double[4];
      double[] py = new double[4];
      double[] pz = new double[4];

      for(int i = 0; i < 4; ++i) {
         double lx = (xs[i] * c - ys[i] * sn) * size;
         double ly = (xs[i] * sn + ys[i] * c) * size;
         px[i] = cx + rx * lx + ux * ly;
         py[i] = cy + ry * lx + uy * ly;
         pz[i] = cz + rz * lx + uz * ly;
      }

      Vfx.quadUV(tex, px[0], py[0], pz[0], (double)u0, (double)v1, color, px[1], py[1], pz[1], (double)u1, (double)v1, color, px[2], py[2], pz[2], (double)u1, (double)v0, color, px[3], py[3], pz[3], (double)u0, (double)v0, color);
   }

   public String stateForTest(KillFx fx) {
      SanguineDeathStyle.State s = (SanguineDeathStyle.State)fx.data;
      return s == null?null:(s.burst?"burst":"growing " + String.format(Locale.ROOT, "%.2f", new Object[]{Double.valueOf(this.size(fx, fx.t))}));
   }

   private static final class State {
      double cx;
      double cy;
      double cz;
      boolean misted;
      boolean burst;
      final float[] streakAngle;
      final float[] streakStart;
      final float[] streakRadius;
      final float[] streakTilt;
      final float[] fragVx;
      final float[] fragVy;
      final float[] fragSpin;

      private State() {
         this.streakAngle = new float[12];
         this.streakStart = new float[12];
         this.streakRadius = new float[12];
         this.streakTilt = new float[12];
         this.fragVx = new float[3];
         this.fragVy = new float[3];
         this.fragSpin = new float[3];
      }
   }
}
