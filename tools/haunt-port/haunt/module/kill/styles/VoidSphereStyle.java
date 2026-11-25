package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.WarpShell;
import mix.client.mixui.integration.haunt.fx.physics.Ragdoll;
import mix.client.mixui.integration.haunt.fx.physics.RigidBox;
import mix.client.mixui.integration.haunt.haunt.fx.HauntKit;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Distortion;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class VoidSphereStyle extends KillStyleBase {
   private static final int SEG = 56;
   private final SliderSetting duration = (SliderSetting)this.setting((new SliderSetting("void.duration", "Duration", "How long the whole effect lasts.", 3.0D, 4.5D, 3.6D, 0.1D)).unit(" s"));
   private final SliderSetting sphereSize = (SliderSetting)this.setting((new SliderSetting("void.size", "Sphere Size", "How big the black sphere grows.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting pullStrength = (SliderSetting)this.setting((new SliderSetting("void.pull", "Pull Strength", "How hard the core drags the body in.", 0.4D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting particles = (SliderSetting)this.setting((new SliderSetting("void.particles", "Particles", "How many particles and fragments it throws and swallows.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting glowAmount = (SliderSetting)this.setting((new SliderSetting("void.glow", "Light", "How bright the rim, the rupture and the last flash are.", 0.2D, 1.6D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting ragdoll = (BooleanSetting)this.setting(new BooleanSetting("void.ragdoll", "Ragdoll", "The body goes limp while it is pulled (off: it drifts in stiffly).", true));
   private final ColorSetting rim = (ColorSetting)this.setting((new ColorSetting("void.rim", "Rim Color", "Color of the sphere\'s edge and the energy lines.", -1)).secondary());

   public VoidSphereStyle(KillEffectsModule module) {
      super(module, KillStyle.VOID_SPHERE, new ColorSetting("void.color", "Energy", "Color of the rupture ring, the particles and the flash.", -3355393));
   }

   public float seconds(KillFx fx) {
      return this.duration.getFloat() * (fx.preview?0.85F:1.0F);
   }

   private float form(KillFx fx) {
      return this.seconds(fx) * 0.15F;
   }

   private float rupture(KillFx fx) {
      return this.seconds(fx) * 0.24F;
   }

   private float swallowed(KillFx fx) {
      return this.seconds(fx) * 0.8F;
   }

   public void start(KillFx fx) {
      VoidSphereStyle.State s = new VoidSphereStyle.State();
      fx.data = s;
      s.cx = fx.ctx.feetX - fx.ctx.dirX * 0.6D;
      s.cy = fx.ctx.feetY + 1.25D;
      s.cz = fx.ctx.feetZ - fx.ctx.dirZ * 0.6D;

      for(int i = 0; i < 56; ++i) {
         s.wobble[i] = fx.rng.signed();
      }

      for(int k = 0; k < s.lineA.length; ++k) {
         s.lineA[k] = fx.rng.next() * 6.2832F;
         s.lineE[k] = fx.rng.signed() * 1.1F;
         s.lineT[k] = fx.rng.next();
      }

   }

   private double radius(KillFx fx, float t) {
      double full = 0.32D * (double)this.sphereSize.getFloat() * (double)fx.size * (fx.crit?1.2D:1.0D);
      float grow = Easing.outBack(Easing.clamp01(t / this.form(fx)), 1.6F);
      float pulse = 1.0F + 0.06F * (float)Math.sin((double)(t * 14.0F));
      float swell = 1.0F + 0.35F * Easing.smoothstep(this.rupture(fx), this.swallowed(fx), t);
      float end = 1.0F - Easing.inCubic(Easing.clamp01((t - this.swallowed(fx)) / (this.seconds(fx) * 0.1F)));
      return full * (double)Math.max(0.0F, grow) * (double)pulse * (double)swell * (double)end;
   }

   public void update(KillFx fx, float dt) {
      VoidSphereStyle.State s = (VoidSphereStyle.State)fx.data;
      float t = fx.t;
      float amt = this.particles.getFloat();
      if(!fx.lod && t < this.form(fx) && fx.rng.next() < dt * 40.0F * amt) {
         double a = (double)fx.rng.next() * 6.2832D;
         double e = (double)fx.rng.signed() * 1.2D;
         double r = this.radius(fx, t) * 1.6D + 0.1D;
         fx.particles.spawn(12, s.cx + Math.cos(a) * Math.cos(e) * r, s.cy + Math.sin(e) * r, s.cz + Math.sin(a) * Math.cos(e) * r, (float)(-Math.sin(a)) * 2.5F, fx.rng.signed() * 0.5F, (float)Math.cos(a) * 2.5F, 0.35F, 0.035F * fx.size).drag(1.5F).color(this.rim, fx.rng.next()).alpha(fx.opacity).hot(0.7F);
      }

      if(!s.ruptured && t >= this.rupture(fx)) {
         s.ruptured = true;
         this.rupture(fx, s);
      }

      if(s.ruptured && t < this.swallowed(fx)) {
         if(this.ragdoll.get()) {
            this.pull(fx, s, t, dt);
         }

         if(!fx.lod && fx.rng.next() < dt * 70.0F * amt) {
            double a = (double)fx.rng.next() * 6.2832D;
            double e = (double)fx.rng.signed() * 1.3D;
            double r = 1.6D + (double)fx.rng.next() * 1.6D;
            double x = s.cx + Math.cos(a) * Math.cos(e) * r;
            double y = s.cy + Math.sin(e) * r;
            double z = s.cz + Math.sin(a) * Math.cos(e) * r;
            float life = 0.5F + fx.rng.next() * 0.25F;
            fx.particles.spawn(fx.rng.next() < 0.5F?3:12, x, y, z, (float)((s.cx - x) / (double)life), (float)((s.cy - y) / (double)life), (float)((s.cz - z) / (double)life), life, 0.05F * fx.size).end(0.005F).color(fx.rng.next() < 0.5F?this.rim.getColor(fx.rng.next()):this.color.getColor(fx.rng.next())).alpha(fx.opacity).hot(0.6F);
         }
      }

      if(!s.imploded && t >= this.swallowed(fx) + this.seconds(fx) * 0.1F) {
         s.imploded = true;
         if(!fx.lod) {
            for(int i = 0; i < fx.count(22.0F * amt, true); ++i) {
               double a = (double)fx.rng.next() * 6.2832D;
               double e = (double)fx.rng.signed() * 1.4D;
               double r = 0.8D + (double)fx.rng.next() * 0.6D;
               double x = s.cx + Math.cos(a) * Math.cos(e) * r;
               double y = s.cy + Math.sin(e) * r;
               double z = s.cz + Math.sin(a) * Math.cos(e) * r;
               fx.particles.spawn(3, x, y, z, (float)((s.cx - x) / 0.25D), (float)((s.cy - y) / 0.25D), (float)((s.cz - z) / 0.25D), 0.25F, 0.05F * fx.size).end(0.0F).color(this.rim, fx.rng.next()).alpha(fx.opacity).hot(0.9F);
            }
         }
      }

   }

   private void rupture(KillFx fx, VoidSphereStyle.State s) {
      float amt = this.particles.getFloat();
      double k = fx.preview?0.5D:1.0D;

      for(int i = 0; i < fx.count(46.0F * amt, true); ++i) {
         double a = (double)fx.rng.next() * 6.2832D;
         double e = Math.asin((double)fx.rng.signed());
         double sp = (5.0D + (double)fx.rng.next() * 6.0D) * k;
         boolean dark = fx.rng.next() < 0.4F;
         fx.particles.spawn(dark?10:1, s.cx, s.cy, s.cz, (float)(Math.cos(a) * Math.cos(e) * sp), (float)(Math.sin(e) * sp * 0.7D), (float)(Math.sin(a) * Math.cos(e) * sp), 0.3F + fx.rng.next() * 0.35F, (dark?0.06F + fx.rng.next() * 0.05F:0.04F + fx.rng.next() * 0.03F) * fx.size).drag(4.0F).color(dark?-16250872:this.rim.getColor(fx.rng.next())).alpha(fx.opacity).hot(dark?0.0F:0.9F);
      }

      Ragdoll r = fx.ragdoll();
      r.gravity = this.ragdoll.get()?7.0F:0.0F;
      r.stiffness = this.ragdoll.get()?0.08F:0.95F;

      for(RigidBox b : r.parts) {
         b.restitution = 0.1F;
         b.friction = 0.2F;
      }

      double ux = s.cx - fx.ctx.feetX;
      double uz = s.cz - fx.ctx.feetZ;
      r.fling(fx.ctx.feetX, fx.ctx.feetY + 1.0D, fx.ctx.feetZ, 0.0D, 0.0D, 0.0D, ux, 0.6D, uz, 1.6D * (double)this.pullStrength.getFloat(), 2.5D);
   }

   private void pull(KillFx fx, VoidSphereStyle.State s, float t, float dt) {
      Ragdoll r = fx.ragdoll();
      float p = Easing.clamp01((t - this.rupture(fx)) / (this.swallowed(fx) - this.rupture(fx)));
      double accel = (6.0D + 34.0D * (double)(p * p)) * (double)this.pullStrength.getFloat();

      for(int i = 0; i < r.parts.length; ++i) {
         RigidBox b = r.parts[i];
         double dx = s.cx - b.cx;
         double dy = s.cy - b.cy;
         double dz = s.cz - b.cz;
         double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if(l > 0.02D) {
            double k = accel * (double)dt / l * (i == 1?1.0D:1.35D);
            double sx = -dz / l;
            double sz = dx / l;
            double swirl = (double)(2.5F * p) * (double)dt;
            b.push(b.cx, b.cy, b.cz, dx * k + sx * swirl, dy * k + 0.35D * (double)dt, dz * k + sz * swirl, 0.008333333333333333D);
         }
      }

   }

   public void render(KillFx fx, FxFrame f) {
      VoidSphereStyle.State s = (VoidSphereStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float light = this.glowAmount.getFloat();
      double hr = this.radius(fx, t);
      double cx = f.rx(s.cx);
      double cy = f.ry(s.cy);
      double cz = f.rz(s.cz);
      this.drawBody(fx, f, s, t);
      RenderState.additive();
      float rup = Easing.clamp01((t - this.rupture(fx)) / (this.seconds(fx) * 0.12F));
      if(rup > 0.0F && rup < 1.0F) {
         float e = Easing.outExpo(rup);
         double rr = (0.2D + 3.2D * (double)e) * (double)fx.size;
         float fade = 1.0F - rup;
         HauntKit.ring(Textures.LINE_GLOW, cx, cy, cz, rr, 0.5D * (double)fx.size * (double)fade, alpha(this.color.getColor(0.5F), 0.9F * a * fade * light));
         HauntKit.ring(Textures.LINE_CORE, cx, cy, cz, rr * 0.97D, 0.08D * (double)fx.size * (double)fade, alpha(this.rim.getColor(0.5F), a * fade * light));
         Vfx.flatRing(Textures.LINE_GLOW, cx, f.ry(fx.ctx.groundY + 0.03D), cz, rr * 1.1D, 0.4D * (double)fx.size * (double)fade, 48, alpha(this.color.getColor(0.5F), 0.6F * a * fade * light));
         Vfx.sprite(Textures.FLARE, cx, cy, cz, 3.6D * (double)fx.size * (double)fade, rup * 2.0F, alpha(-1, a * fade * light));
         this.cracks(fx, f, s, rup, a * light);
      }

      float warp = a * Easing.smoothstep(0.0F, this.form(fx), t) * (1.0F - Easing.smoothstep(this.swallowed(fx), this.seconds(fx) * 0.92F, t));
      if(warp > 0.02F && !fx.lod && !f.preview && Distortion.refracts() && hr > 0.01D) {
         Vfx.flush();
         WarpShell.draw(f, s.cx, s.cy, s.cz, hr * 2.6D + 0.15D, 0.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.0D, 1.0D, 0.0D, 0.0D, 1.0D, false, -1.3F, -1, 0.0F, warp * 0.75F, true);
         RenderState.additive();
      }

      this.drawLines(fx, f, s, t, a * light);
      if(hr > 0.003D) {
         Vfx.sprite(Textures.GLOW, cx, cy, cz, hr * 4.0D, 0.0F, alpha(this.color.getColor(0.5F), 0.3F * a * light));
         Vfx.flush();
         RenderState.translucent(false);
         Vfx.sprite(Textures.GLOW, cx, cy, cz, hr * 3.0D, 0.0F, alpha(-16777216, 0.8F * a));
         Vfx.sprite(Textures.BUBBLE, cx, cy, cz, hr * 2.08D, 0.0F, alpha(-16777216, a));
         Vfx.flush();
         RenderState.additive();
         this.rimRing(fx, s, cx, cy, cz, hr, t, a * light);
      }

      float flashAt = this.swallowed(fx);
      float ft = (t - flashAt) / (this.seconds(fx) * 0.2F);
      if(ft > 0.0F && ft < 1.0F) {
         float fl = 1.0F - ft;
         Vfx.sprite(Textures.GLOW, cx, cy, cz, 2.6D * (double)fx.size * (double)(0.4F + 0.6F * Easing.outCubic(Math.min(1.0F, ft * 3.0F))), 0.0F, alpha(-1, 0.9F * a * fl * fl * light));
         Vfx.sprite(Textures.FLARE, cx, cy, cz, 2.0D * (double)fx.size * (double)fl, ft * 4.0F, alpha(-1, a * fl * light));
         double rr = (1.8D - 1.6D * (double)Easing.outCubic(ft)) * (double)fx.size;
         HauntKit.ring(Textures.LINE_GLOW, cx, cy, cz, rr, 0.18D * (double)fx.size * (double)fl, alpha(this.rim.getColor(0.5F), 0.8F * a * fl * light));
      }

      Vfx.flush();
   }

   private void drawBody(KillFx fx, FxFrame f, VoidSphereStyle.State s, float t) {
      if(t < this.swallowed(fx) + 0.05F) {
         if(fx.hasRagdoll()) {
            fx.applyRagdoll();
         } else {
            for(int i = 0; i < 6; ++i) {
               fx.parts[i].copy(fx.rest[i]);
            }
         }

         float p = s.ruptured?Easing.clamp01((t - this.rupture(fx)) / (this.swallowed(fx) - this.rupture(fx))):0.0F;
         if(!this.ragdoll.get() && s.ruptured) {
            for(int i = 0; i < 6; ++i) {
               BodyProxy.Part part = fx.parts[i];
               float e = Easing.inCubic(Easing.clamp01(p * 1.15F - (float)i * 0.03F));
               part.x += (s.cx - part.x) * (double)e;
               part.y += (s.cy - part.y) * (double)e;
               part.z += (s.cz - part.z) * (double)e;
            }
         }

         float shrink = Easing.inQuad(Easing.clamp01((p - 0.35F) / 0.65F));

         for(int i = 0; i < 6; ++i) {
            BodyProxy.Part part = fx.parts[i];
            double dx = part.x - s.cx;
            double dy = part.y - s.cy;
            double dz = part.z - s.cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float near = (float)Math.max(0.0D, Math.min(1.0D, 1.0D - dist / 1.6D));
            float k = Math.max(0.02F, 1.0F - Math.max(shrink, near * near * p) * 0.98F);
            part.x = s.cx + dx * (double)k;
            part.y = s.cy + dy * (double)k;
            part.z = s.cz + dz * (double)k;
            part.scale = k;
            part.alpha = 1.0F - Easing.smoothstep(0.8F, 1.0F, p);
            part.visible = part.alpha > 0.01F;
         }

         fx.look.glow = this.color.getColor(0.5F);
         fx.look.glowAmount = 0.25F * p;
         fx.look.tint = -15724527;
         fx.look.tintAmount = 0.55F * p;
         fx.drawParts(f, fx.parts);
         fx.look.glowAmount = 0.0F;
         fx.look.tintAmount = 0.0F;
      }

   }

   private void rimRing(KillFx fx, VoidSphereStyle.State s, double cx, double cy, double cz, double hr, float t, float a) {
      double rx = Vfx.camera().rightX;
      double ry = Vfx.camera().rightY;
      double rz = Vfx.camera().rightZ;
      double ux = Vfx.camera().upX;
      double uy = Vfx.camera().upY;
      double uz = Vfx.camera().upZ;

      for(int pass = 0; pass < 2; ++pass) {
         for(int i = 0; i < 56; ++i) {
            double ang = 6.283185307179586D * (double)i / 56.0D;
            double wob = 1.0D + 0.07D * (double)s.wobble[i] * Math.sin((double)(t * 9.0F) + (double)i * 0.7D) + 0.04D * Math.sin(ang * 5.0D - (double)(t * 6.0F));
            double r = hr * 1.04D * wob * (pass == 0?1.0D:1.08D);
            double c = Math.cos(ang);
            double sn = Math.sin(ang);
            Vfx.X[i] = cx + (rx * c + ux * sn) * r;
            Vfx.Y[i] = cy + (ry * c + uy * sn) * r;
            Vfx.Z[i] = cz + (rz * c + uz * sn) * r;
            Vfx.W[i] = (float)(hr * (pass == 0?0.12D:0.45D));
            float flick = 0.7F + 0.3F * (float)Math.sin((double)(t * 23.0F) + (double)i);
            Vfx.C[i] = alpha(this.rim.getColor((float)i / 56.0F), (pass == 0?1.0F:0.4F) * a * flick);
         }

         Vfx.ribbon(pass == 0?Textures.LINE_CORE:Textures.LINE_GLOW, 56, true);
      }

   }

   private void cracks(KillFx fx, FxFrame f, VoidSphereStyle.State s, float rup, float a) {
      if(!fx.lod) {
         int n = 8;
         float fade = 1.0F - rup;

         for(int k = 0; k < n; ++k) {
            double ang = (double)s.lineA[k];
            double el = (double)s.lineE[k] * 0.6D;
            double len = (0.6D + 1.4D * (double)Easing.outCubic(Math.min(1.0F, rup * 2.5F))) * (double)fx.size;
            int pts = 6;

            for(int i = 0; i < pts; ++i) {
               double d = len * (double)i / (double)(pts - 1);
               double j = (i % 2 == 0?0.07D:-0.07D) * d;
               Vfx.X[i] = f.rx(s.cx + Math.cos(ang) * Math.cos(el) * d + Math.sin(ang) * j);
               Vfx.Y[i] = f.ry(s.cy + Math.sin(el) * d);
               Vfx.Z[i] = f.rz(s.cz + Math.sin(ang) * Math.cos(el) * d - Math.cos(ang) * j);
               Vfx.W[i] = (float)(0.06D * (double)fx.size * (1.0D - (double)i / (double)pts));
               Vfx.C[i] = alpha(this.rim.getColor(0.5F), a * fade);
            }

            Vfx.ribbon(Textures.LINE_CORE, pts, false);
         }
      }

   }

   private void drawLines(KillFx fx, FxFrame f, VoidSphereStyle.State s, float t, float a) {
      if(!fx.lod && s.ruptured && t < this.swallowed(fx)) {
         float p = Easing.clamp01((t - this.rupture(fx)) / (this.swallowed(fx) - this.rupture(fx)));
         int n = s.lineA.length;

         for(int k = 0; k < n; ++k) {
            float u = (p * 2.6F + s.lineT[k]) % 1.0F;
            double r0 = (2.4D - 2.2D * (double)u) * (double)fx.size;
            double r1 = Math.max(0.05D, r0 - 0.7D * (double)fx.size);
            double ang = (double)s.lineA[k] + (double)(t * 0.8F);
            double el = (double)s.lineE[k];
            int pts = 5;

            for(int i = 0; i < pts; ++i) {
               double r = r0 + (r1 - r0) * (double)i / (double)(pts - 1);
               double tw = ang + (double)i * 0.12D;
               Vfx.X[i] = f.rx(s.cx + Math.cos(tw) * Math.cos(el) * r);
               Vfx.Y[i] = f.ry(s.cy + Math.sin(el) * r);
               Vfx.Z[i] = f.rz(s.cz + Math.sin(tw) * Math.cos(el) * r);
               Vfx.W[i] = (float)(0.035D * (double)fx.size);
               float edge = (float)Math.sin(3.141592653589793D * (double)u);
               Vfx.C[i] = alpha(ColorUtil.lerp(this.rim.getColor((float)k / (float)n), this.color.getColor(0.5F), (float)i / (float)pts), a * edge * 0.8F);
            }

            Vfx.ribbon(Textures.LINE_GLOW, pts, false);
         }
      }

   }

   public boolean focus(KillFx fx, double[] out) {
      VoidSphereStyle.State s = (VoidSphereStyle.State)fx.data;
      if(s == null) {
         return false;
      } else {
         out[0] = s.cx;
         out[1] = s.cz;
         return true;
      }
   }

   public String stateForTest(KillFx fx) {
      VoidSphereStyle.State s = (VoidSphereStyle.State)fx.data;
      return s == null?null:(s.imploded?"imploded":(s.ruptured?"pulling":"forming"));
   }

   private static final class State {
      double cx;
      double cy;
      double cz;
      boolean ruptured;
      boolean imploded;
      final float[] wobble;
      final float[] lineA;
      final float[] lineE;
      final float[] lineT;

      private State() {
         this.wobble = new float[56];
         this.lineA = new float[18];
         this.lineE = new float[18];
         this.lineT = new float[18];
      }
   }
}
