package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.physics.Ragdoll;
import mix.client.mixui.integration.haunt.fx.physics.RigidBox;
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

public final class TornadoStyle extends KillStyleBase {
   private static final int LINES = 40;
   private static final int PTS = 14;
   private static final int TRAIL = 24;
   private final SliderSetting duration = (SliderSetting)this.setting((new SliderSetting("tornado.duration", "Duration", "How long the whole effect lasts.", 3.0D, 5.0D, 4.2D, 0.1D)).unit(" s"));
   private final SliderSetting height = (SliderSetting)this.setting((new SliderSetting("tornado.height", "Height", "How tall the funnel grows.", 0.6D, 1.8D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting spin = (SliderSetting)this.setting((new SliderSetting("tornado.spin", "Rotation Speed", "How fast the strokes and the body turn.", 0.4D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting throwStrength = (SliderSetting)this.setting((new SliderSetting("tornado.throw", "Throw", "How far the body is hurled at the end.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting density = (SliderSetting)this.setting((new SliderSetting("tornado.density", "Strokes", "How many brush strokes and particles make the funnel.", 0.4D, 1.0D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting ragdoll = (BooleanSetting)this.setting(new BooleanSetting("tornado.ragdoll", "Ragdoll", "Arms and legs flail freely inside the funnel.", true));
   private final ColorSetting shade = (ColorSetting)this.setting((new ColorSetting("tornado.shade", "Inner Strokes", "Color of the strokes deep inside the funnel.", -6052957)).secondary());

   public TornadoStyle(KillEffectsModule module) {
      super(module, KillStyle.TORNADO, new ColorSetting("tornado.color", "Strokes", "Color of the brush strokes and the streaks.", -1));
   }

   public float seconds(KillFx fx) {
      return this.duration.getFloat() * (fx.preview?0.85F:1.0F);
   }

   private float formEnd(KillFx fx) {
      return this.seconds(fx) * 0.22F;
   }

   private float grabAt(KillFx fx) {
      return this.seconds(fx) * 0.15F;
   }

   private float throwAt(KillFx fx) {
      return this.seconds(fx) * 0.7F;
   }

   public boolean leavesBody() {
      return false;
   }

   public void start(KillFx fx) {
      HauntTextures.warmUp();
      TornadoStyle.State s = new TornadoStyle.State();
      fx.data = s;
      s.cx = fx.ctx.feetX;
      s.cz = fx.ctx.feetZ;
      s.gy = fx.ctx.groundY;

      for(int k = 0; k < 40; ++k) {
         s.phase[k] = fx.rng.next() * 6.2832F;
         s.speed[k] = 0.6F + fx.rng.next() * 1.3F;
         s.h0[k] = fx.rng.next() * 0.75F;
         s.span[k] = 0.18F + fx.rng.next() * 0.32F;
         s.width[k] = 0.03F + fx.rng.next() * 0.09F;
         s.appear[k] = fx.rng.next();
         s.bulge[k] = 0.8F + fx.rng.next() * 0.5F;
         s.arc[k] = 1.2F + fx.rng.next() * 2.2F;
      }

      double dx = fx.ctx.dirX;
      double dz = fx.ctx.dirZ;
      double l = Math.sqrt(dx * dx + dz * dz);
      if(l < 1.0E-4D) {
         double a = (double)fx.rng.next() * 6.2832D;
         dx = Math.cos(a);
         dz = Math.sin(a);
         l = 1.0D;
      }

      s.tx = dx / l;
      s.tz = dz / l;
   }

   private double funnelHeight(KillFx fx) {
      return 3.6D * (double)this.height.getFloat() * (double)fx.size;
   }

   private double radiusAt(KillFx fx, double h, float grow) {
      double base = 0.18D + 1.25D * h * h + 0.35D * h;
      return base * (double)fx.size * (0.4D + 0.6D * (double)grow);
   }

   public void update(KillFx fx, float dt) {
      TornadoStyle.State s = (TornadoStyle.State)fx.data;
      float t = fx.t;
      float sp = this.spin.getFloat();
      float fade = 1.0F - Easing.smoothstep(this.throwAt(fx), this.seconds(fx), t);
      s.turn += dt * sp * (3.0F + 7.0F * Easing.smoothstep(0.0F, this.throwAt(fx), t)) * (0.4F + 0.6F * fade);
      if(!fx.lod && fade > 0.05F && fx.rng.next() < dt * 55.0F * this.density.getFloat()) {
         double h = (double)fx.rng.next();
         double a = (double)fx.rng.next() * 6.2832D;
         double r = this.radiusAt(fx, h, 1.0F) * (0.9D + (double)fx.rng.next() * 0.3D);
         double y = s.gy + h * this.funnelHeight(fx);
         float tan = (float)(r * 6.0D * (double)sp);
         fx.particles.spawn(fx.rng.next() < 0.3F?8:3, s.cx + Math.cos(a) * r, y, s.cz + Math.sin(a) * r, (float)(-Math.sin(a)) * tan, 1.2F + fx.rng.next() * 1.5F, (float)Math.cos(a) * tan, 0.6F + fx.rng.next() * 0.5F, (0.04F + fx.rng.next() * 0.05F) * fx.size).drag(0.8F).color(ColorUtil.lerp(this.color.getColor(fx.rng.next()), this.shade.getColor(0.5F), fx.rng.next() * 0.5F)).alpha(0.6F * fx.opacity * fade);
      }

      if(!s.grabbed && t >= this.grabAt(fx)) {
         s.grabbed = true;
         Ragdoll r = fx.ragdoll();
         r.gravity = 6.0F;
         r.stiffness = this.ragdoll.get()?0.05F:0.9F;

         for(RigidBox b : r.parts) {
            b.restitution = 0.2F;
            b.friction = 0.3F;
         }

         r.fling(fx.ctx.feetX, fx.ctx.feetY + 0.9D, fx.ctx.feetZ, 0.0D, 0.0D, 0.0D, -s.tz, 0.8D, s.tx, 2.0D, 3.0D);
      }

      if(s.grabbed && !s.thrown && fx.hasRagdoll()) {
         this.swirl(fx, s, t, dt);
      }

      if(!s.thrown && t >= this.throwAt(fx)) {
         s.thrown = true;
         if(fx.hasRagdoll()) {
            Ragdoll r = fx.ragdoll();
            r.gravity = 22.0F;
            double k = 9.0D * (double)this.throwStrength.getFloat() * (fx.preview?0.45D:1.0D) * (fx.crit?1.3D:1.0D);
            RigidBox body = r.parts[1];
            double vx = s.tx * k;
            double vy = 0.9D * k;
            double vz = s.tz * k;

            for(RigidBox b : r.parts) {
               b.push(b.cx, b.cy, b.cz, vx, vy, vz, 0.008333333333333333D);
            }

            body.push(body.cx + 0.1D, body.cy + 0.3D, body.cz, -s.tz * 6.0D, 0.0D, s.tx * 6.0D, 0.008333333333333333D);
         }

         if(!fx.lod) {
            for(int i = 0; i < fx.count(26.0F * this.density.getFloat(), true); ++i) {
               double a = (double)fx.rng.next() * 6.2832D;
               fx.particles.spawn(3, s.bx, s.by, s.bz, (float)(Math.cos(a) * 3.0D + s.tx * 4.0D), 2.0F + fx.rng.next() * 2.0F, (float)(Math.sin(a) * 3.0D + s.tz * 4.0D), 0.6F + fx.rng.next() * 0.4F, 0.06F * fx.size).drag(2.0F).color(this.color, fx.rng.next()).alpha(fx.opacity).hot(0.5F);
            }
         }
      }

   }

   private void swirl(KillFx fx, TornadoStyle.State s, float t, float dt) {
      Ragdoll r = fx.ragdoll();
      float p = Easing.clamp01((t - this.grabAt(fx)) / (this.throwAt(fx) - this.grabAt(fx)));
      double top = s.gy + this.funnelHeight(fx) * (0.25D + 0.5D * (double)Easing.inOutSine(p));
      double sp = (double)this.spin.getFloat();

      for(int i = 0; i < r.parts.length; ++i) {
         RigidBox b = r.parts[i];
         double dx = b.cx - s.cx;
         double dz = b.cz - s.cz;
         double d = Math.sqrt(dx * dx + dz * dz);
         double h = Math.max(0.0D, Math.min(1.0D, (b.cy - s.gy) / this.funnelHeight(fx)));
         double want = this.radiusAt(fx, h, 1.0F) * 0.35D;
         double radial = d > 1.0E-4D?(want - d) / d * 14.0D:0.0D;
         double tx = d > 1.0E-4D?-dz / d:0.0D;
         double tz = d > 1.0E-4D?dx / d:0.0D;
         double tan = (5.0D + 9.0D * (double)p) * sp;
         double lift = (top - b.cy) * 9.0D + 4.0D;
         double k = (double)dt;
         b.push(b.cx, b.cy, b.cz, (dx * radial + tx * tan) * k, lift * k, (dz * radial + tz * tan) * k, 0.008333333333333333D);
         if(i != 1 && this.ragdoll.get()) {
            b.push(b.cx + (double)fx.rng.signed() * 0.1D, b.cy, b.cz + (double)fx.rng.signed() * 0.1D, (double)(fx.rng.signed() * 6.0F) * k, (double)(fx.rng.signed() * 6.0F) * k, (double)(fx.rng.signed() * 6.0F) * k, 0.008333333333333333D);
         }
      }

   }

   public void render(KillFx fx, FxFrame f) {
      TornadoStyle.State s = (TornadoStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      if(!HauntTextures.ensure()) {
         fx.drawBody(f);
      } else {
         float bodyFade = 1.0F - Easing.smoothstep(this.throwAt(fx) + (this.seconds(fx) - this.throwAt(fx)) * 0.35F, this.seconds(fx) * 0.97F, t);
         if(fx.hasRagdoll()) {
            fx.applyRagdoll();
         } else {
            for(int i = 0; i < 6; ++i) {
               fx.parts[i].copy(fx.rest[i]);
            }
         }

         for(int i = 0; i < 6; ++i) {
            fx.parts[i].alpha = bodyFade;
            fx.parts[i].visible = bodyFade > 0.01F;
         }

         s.bx = fx.parts[1].x;
         s.by = fx.parts[1].y;
         s.bz = fx.parts[1].z;
         this.recordTrail(s);
         fx.look.glow = this.color.getColor(0.5F);
         fx.look.glowAmount = 0.12F * Easing.smoothstep(this.grabAt(fx), this.throwAt(fx), t);
         fx.drawParts(f, fx.parts);
         fx.look.glowAmount = 0.0F;
         RenderState.translucent(false);
         this.drawFunnel(fx, f, s, t, a);
         Vfx.flush();
         RenderState.additive();
         this.drawGround(fx, f, s, t, a);
         if(s.thrown) {
            this.drawTrail(fx, f, s, t, a * bodyFade);
         }

         Vfx.flush();
      }

   }

   private void drawFunnel(KillFx fx, FxFrame f, TornadoStyle.State s, float t, float a) {
      float grow = Easing.outCubic(Easing.clamp01(t / this.formEnd(fx)));
      float end = Easing.smoothstep(this.throwAt(fx), this.seconds(fx), t);
      double H = this.funnelHeight(fx);
      int count = Math.round(40.0F * this.density.getFloat());

      for(int k = 0; k < count; ++k) {
         float on = Easing.clamp01((grow * 1.15F - s.appear[k] * 0.9F) / 0.25F);
         float off = Easing.clamp01((end * 1.25F - (1.0F - s.appear[k]) * 0.6F) / 0.4F);
         float vis = on * (1.0F - off);
         if(vis > 0.01F) {
            double hStart = (double)s.h0[k] * (double)(0.3F + 0.7F * grow);
            double hSpan = (double)s.span[k] * (double)(0.5F + 0.5F * grow);
            double ang0 = (double)(s.phase[k] + s.turn * s.speed[k]);
            double unravel = (double)end * 1.6D;

            for(int i = 0; i < 14; ++i) {
               double u = (double)i / 13.0D;
               double h = Math.min(1.0D, hStart + hSpan * u);
               double r = this.radiusAt(fx, h, grow) * (double)s.bulge[k] * (1.0D + unravel * u);
               double ang = ang0 + u * (double)s.arc[k];
               double wob = Math.sin((double)(t * 3.0F) + h * 9.0D + (double)k) * 0.06D * (double)fx.size;
               Vfx.X[i] = f.rx(s.cx + Math.cos(ang) * (r + wob));
               Vfx.Y[i] = f.ry(s.gy + h * H + unravel * u * 0.4D);
               Vfx.Z[i] = f.rz(s.cz + Math.sin(ang) * (r + wob));
               float taper = (float)Math.sin(3.141592653589793D * u);
               Vfx.W[i] = (float)((double)s.width[k] * (double)fx.size * (0.4D + 0.6D * h) * (double)(0.3F + 0.7F * taper));
               float depth = 0.55F + 0.45F * (float)Math.cos(ang - Math.atan2(Vfx.camera().posZ - f.rz(s.cz), Vfx.camera().posX - f.rx(s.cx)));
               int col = ColorUtil.lerp(this.shade.getColor((float)k / (float)count), this.color.getColor((float)k / (float)count), depth);
               Vfx.C[i] = alpha(col, a * vis * (0.35F + 0.65F * taper) * (0.55F + 0.45F * depth));
            }

            Vfx.ribbon(HauntTextures.BRUSH, 14, false);
         }
      }

   }

   private void drawGround(KillFx fx, FxFrame f, TornadoStyle.State s, float t, float a) {
      float grow = Easing.outCubic(Easing.clamp01(t / this.formEnd(fx)));
      float end = 1.0F - Easing.smoothstep(this.throwAt(fx), this.seconds(fx), t);
      double gy = f.ry(s.gy + 0.03D);
      double cx = f.rx(s.cx);
      double cz = f.rz(s.cz);
      int col = this.color.getColor(0.5F);

      for(int i = 0; i < 3; ++i) {
         double r = (0.4D + (double)i * 0.45D) * (double)fx.size * (double)(0.5F + 0.5F * grow);
         Vfx.flatBand(Textures.STREAKS, cx, gy, cz, r * 0.7D, r, (double)(-s.turn * (1.0F + (float)i * 0.3F)), 6.283185307179586D, 32, alpha(col, 0.22F * a * grow * end));
      }

   }

   private void recordTrail(TornadoStyle.State s) {
      if(s.thrown) {
         System.arraycopy(s.trailX, 0, s.trailX, 1, 23);
         System.arraycopy(s.trailY, 0, s.trailY, 1, 23);
         System.arraycopy(s.trailZ, 0, s.trailZ, 1, 23);
         s.trailX[0] = s.bx;
         s.trailY[0] = s.by;
         s.trailZ[0] = s.bz;
         if(s.trailCount < 24) {
            ++s.trailCount;
         }
      }

   }

   private void drawTrail(KillFx fx, FxFrame f, TornadoStyle.State s, float t, float a) {
      int n = s.trailCount;
      if(n >= 3) {
         for(int lane = 0; lane < 3; ++lane) {
            double off = (double)(lane - 1) * 0.18D * (double)fx.size;

            for(int i = 0; i < n; ++i) {
               float k = (float)i / (float)(n - 1);
               Vfx.X[i] = f.rx(s.trailX[i] + off * -s.tz);
               Vfx.Y[i] = f.ry(s.trailY[i] + (double)(lane - 1) * 0.1D);
               Vfx.Z[i] = f.rz(s.trailZ[i] + off * s.tx);
               Vfx.W[i] = (float)(0.16D * (double)fx.size * (double)(1.0F - k));
               Vfx.C[i] = alpha(this.color.getColor(k), a * (1.0F - k) * 0.8F);
            }

            Vfx.ribbon(HauntTextures.BRUSH, n, false);
         }
      }

   }

   public boolean focus(KillFx fx, double[] out) {
      TornadoStyle.State s = (TornadoStyle.State)fx.data;
      if(s == null) {
         return false;
      } else {
         out[0] = s.cx;
         out[1] = s.cz;
         return true;
      }
   }

   public String stateForTest(KillFx fx) {
      TornadoStyle.State s = (TornadoStyle.State)fx.data;
      return s == null?null:(s.thrown?"thrown":(s.grabbed?"spinning":"forming"));
   }

   private static final class State {
      double cx;
      double cz;
      double gy;
      double tx;
      double tz;
      double bx;
      double by;
      double bz;
      float turn;
      boolean grabbed;
      boolean thrown;
      final float[] phase;
      final float[] speed;
      final float[] h0;
      final float[] span;
      final float[] width;
      final float[] appear;
      final float[] bulge;
      final float[] arc;
      final double[] trailX;
      final double[] trailY;
      final double[] trailZ;
      int trailCount;

      private State() {
         this.phase = new float[40];
         this.speed = new float[40];
         this.h0 = new float[40];
         this.span = new float[40];
         this.width = new float[40];
         this.appear = new float[40];
         this.bulge = new float[40];
         this.arc = new float[40];
         this.trailX = new double[24];
         this.trailY = new double[24];
         this.trailZ = new double[24];
      }
   }
}
