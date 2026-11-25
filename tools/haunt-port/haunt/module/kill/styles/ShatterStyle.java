package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.physics.BlockCollider;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.module.kill.SkinDust;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ShatterStyle extends KillStyleBase {
   private static final int MAX_SHARDS = 160;
   private static final int CRACKS = 9;
   private static final int CRACK_POINTS = 7;
   private final SliderSetting shards = (SliderSetting)this.setting((new SliderSetting("shatter.shards", "Shard Count", "How many shards the body breaks into.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting freeze = (SliderSetting)this.setting((new SliderSetting("shatter.freeze", "Freeze Time", "How long the body stands frozen before it bursts.", 0.2D, 1.5D, 0.6D, 0.05D)).unit(" s"));
   private final SliderSetting spread = (SliderSetting)this.setting((new SliderSetting("shatter.spread", "Spread", "How far the shards fly.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final double[] m = new double[9];

   public ShatterStyle(KillEffectsModule module) {
      super(module, KillStyle.SHATTER, new ColorSetting("shatter.color", "Ice", "Color of the ice, its shine and the cracks.", -5708545));
   }

   private float freezeTime(KillFx fx) {
      return this.freeze.getFloat() * (fx.crit?0.8F:1.0F);
   }

   public float seconds(KillFx fx) {
      return this.freezeTime(fx) + 1.9F;
   }

   public void start(KillFx fx) {
      ShatterStyle.State s = new ShatterStyle.State();
      fx.data = s;
      Rng rng = fx.rng;
      double dx = fx.ctx.dirX;
      double dz = fx.ctx.dirZ;
      double hl = Math.sqrt(dx * dx + dz * dz);
      if(hl < 1.0E-4D) {
         dx = 0.0D;
         dz = 1.0D;
         hl = 1.0D;
      }

      double var34;
      double sx = -(var34 = dz / hl);
      double var33;
      double sz = var33 = dx / hl;
      double frontX = fx.ctx.feetX - var33 * 0.17D;
      double frontZ = fx.ctx.feetZ - var34 * 0.17D;
      double startY = Math.max(fx.ctx.feetY + 0.4D, Math.min(fx.ctx.feetY + 1.7D, fx.ctx.impactY));
      double startS = (fx.ctx.impactX - fx.ctx.feetX) * sx + (fx.ctx.impactZ - fx.ctx.feetZ) * sz;
      startS = Math.max(-0.3D, Math.min(0.3D, startS));

      for(int k = 0; k < 9; ++k) {
         double ang = (double)k * 0.6981317007977318D + (double)rng.signed() * 0.3D;
         double u = startS;
         double v = startY;

         for(int i = 0; i < 7; ++i) {
            int o = k * 7 + i;
            s.cx[o] = frontX + sx * u;
            s.cy[o] = v;
            s.cz[o] = frontZ + sz * u;
            double step = 0.12D + (double)rng.next() * 0.1D;
            u = Math.max(-0.42D, Math.min(0.42D, u + Math.cos(ang += (double)rng.signed() * 0.6D) * step * 0.6D));
            v = Math.max(fx.ctx.feetY + 0.05D, Math.min(fx.ctx.feetY + 1.95D, v + Math.sin(ang) * step));
         }

         s.crackDelay[k] = rng.next() * 0.25F;
      }

      s.dust.sample(fx, fx.count(70.0F * this.shards.getFloat(), true), (SkinDust.Threshold)null);
      s.count = s.dust.count;

      for(int i = 0; i < s.count; ++i) {
         s.size[i] = (0.07F + rng.next() * 0.1F) * fx.size * (float)Math.sqrt(1.0D / Math.max(0.5D, (double)this.shards.getFloat()));

         for(int c = 0; c < 3; ++c) {
            double a = (double)c * 2.1D + (double)rng.signed() * 0.6D;
            double r = 0.6D + (double)rng.next() * 0.6D;
            s.shape[i * 6 + c * 2] = (float)(Math.cos(a) * r);
            s.shape[i * 6 + c * 2 + 1] = (float)(Math.sin(a) * r);
         }

         float axl;
         while(true) {
            s.ax[i] = rng.signed();
            s.ay[i] = rng.signed();
            s.az[i] = rng.signed();
            if((axl = (float)Math.sqrt((double)(s.ax[i] * s.ax[i] + s.ay[i] * s.ay[i] + s.az[i] * s.az[i]))) >= 0.1F) {
               break;
            }
         }

         s.ax[i] /= axl;
         s.ay[i] /= axl;
         s.az[i] /= axl;
         s.angle[i] = rng.next() * 6.28F;
         s.landed[i] = -1.0F;
      }

   }

   private void burst(KillFx fx, ShatterStyle.State s) {
      s.burst = true;
      Rng rng = fx.rng;
      double k = (double)this.spread.getFloat() * (fx.crit?1.35D:1.0D) * (fx.preview?0.45D:1.0D);

      for(int i = 0; i < s.count; ++i) {
         s.x[i] = fx.ctx.feetX + (double)s.dust.x[i];
         s.y[i] = fx.ctx.feetY + (double)s.dust.y[i];
         s.z[i] = fx.ctx.feetZ + (double)s.dust.z[i];
         double ox = s.x[i] - fx.ctx.impactX;
         double oy = s.y[i] - fx.ctx.impactY;
         double oz = s.z[i] - fx.ctx.impactZ;
         double ol = Math.max(0.1D, Math.sqrt(ox * ox + oy * oy + oz * oz));
         double speed = (2.5D + (double)rng.next() * 3.5D) * k;
         s.vx[i] = (float)((fx.ctx.dirX * 0.75D + ox / ol * 0.5D + (double)rng.signed() * 0.3D) * speed);
         s.vy[i] = (float)((Math.max(-0.2D, fx.ctx.dirY) * 0.5D + oy / ol * 0.4D + 0.35D + (double)rng.next() * 0.3D) * speed);
         s.vz[i] = (float)((fx.ctx.dirZ * 0.75D + oz / ol * 0.5D + (double)rng.signed() * 0.3D) * speed);
         s.spin[i] = rng.signed() * 14.0F;
      }

      if(!fx.lod) {
         for(int var17 = 0; var17 < fx.count(30.0F, true); ++var17) {
            fx.particles.spawn(5, fx.ctx.midX() + (double)rng.signed() * 0.3D, fx.ctx.midY() + (double)rng.signed() * 0.8D, fx.ctx.midZ() + (double)rng.signed() * 0.3D, (float)(fx.ctx.dirX * 2.0D + (double)(rng.signed() * 2.0F)), rng.next() * 2.0F, (float)(fx.ctx.dirZ * 2.0D + (double)(rng.signed() * 2.0F)), 0.5F + rng.next() * 0.4F, (0.07F + rng.next() * 0.06F) * fx.size).drag(2.0F).color(this.color, rng.next()).alpha(fx.opacity).flags(2);
         }

         for(int var18 = 0; var18 < 8; ++var18) {
            fx.particles.spawn(8, fx.ctx.midX() + (double)rng.signed() * 0.3D, fx.ctx.midY() + (double)rng.signed() * 0.7D, fx.ctx.midZ() + (double)rng.signed() * 0.3D, (float)(fx.ctx.dirX * 1.2D + (double)rng.signed() * 0.5D), 0.3F + rng.next() * 0.4F, (float)(fx.ctx.dirZ * 1.2D + (double)rng.signed() * 0.5D), 0.9F, (0.3F + rng.next() * 0.3F) * fx.size).end(0.8F * fx.size).drag(1.5F).color(ColorUtil.lerp(-1, this.color.getColor(0.5F), 0.4F)).alpha(0.45F * fx.opacity);
         }
      }

   }

   public void update(KillFx fx, float dt) {
      ShatterStyle.State s = (ShatterStyle.State)fx.data;
      if(!s.burst && fx.t >= this.freezeTime(fx)) {
         this.burst(fx, s);
      }

      if(s.burst) {
         BlockCollider world = fx.collider();

         for(int i = 0; i < s.count; ++i) {
            if(s.landed[i] < 0.0F) {
               s.vy[i] -= 18.0F * dt;
               float drag = 1.0F - 0.4F * dt;
               s.vx[i] *= drag;
               s.vz[i] *= drag;
               double nx = s.x[i] + (double)(s.vx[i] * dt);
               double ny = s.y[i] + (double)(s.vy[i] * dt);
               double nz = s.z[i] + (double)(s.vz[i] * dt);
               if(world.solid(nx, ny, nz)) {
                  if(!world.solid(nx, s.y[i], nz)) {
                     ny = s.y[i];
                     if(s.vy[i] < -1.8F) {
                        s.vy[i] *= -0.28F;
                        s.vx[i] *= 0.6F;
                        s.vz[i] *= 0.6F;
                        s.spin[i] *= 0.5F;
                     } else {
                        s.landed[i] = fx.t;
                        s.vz[i] = 0.0F;
                        s.vy[i] = 0.0F;
                        s.vx[i] = 0.0F;
                     }
                  } else {
                     nx = s.x[i];
                     nz = s.z[i];
                     s.vx[i] *= -0.3F;
                     s.vz[i] *= -0.3F;
                  }
               }

               s.x[i] = nx;
               s.y[i] = ny;
               s.z[i] = nz;
               s.angle[i] += s.spin[i] * dt;
            }
         }
      }

   }

   public void render(KillFx fx, FxFrame f) {
      ShatterStyle.State s = (ShatterStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float fz = this.freezeTime(fx);
      if(s.burst) {
         this.drawShards(fx, f, s, a);
      } else {
         float ice = Easing.smoothstep(0.0F, Math.min(0.25F, fz * 0.5F), t);
         int iceColor;
         fx.look.tint = iceColor = ColorUtil.lerp(-8402689, this.color.getColor(0.5F) | -16777216, 0.25F);
         fx.look.tintAmount = 0.9F * ice;
         float shine = Easing.clamp01((t - fz * 0.15F) / (fz * 0.55F));
         float band = (float)Math.exp(-Math.pow((double)((shine - 0.5F) * 5.0F), 2.0D));
         fx.look.glow = ColorUtil.lerp(-2559233, iceColor, 0.4F);
         fx.look.glowAmount = 0.42F * ice + 0.4F * band * (shine > 0.0F && shine < 1.0F?1.0F:0.0F);
         fx.drawBody(f);
         fx.look.glowAmount = 0.0F;
         RenderState.additive();
         if(shine > 0.0F && shine < 1.0F) {
            double gx = fx.ctx.midX() + Vfx.camera().rightX * ((double)shine - 0.5D) * 0.9D;
            double gy = fx.ctx.feetY + 0.2D + (double)shine * 1.7D;
            double gz = fx.ctx.midZ() + Vfx.camera().rightZ * ((double)shine - 0.5D) * 0.9D;
            Vfx.sprite(Textures.FLARE, f.rx(gx), f.ry(gy), f.rz(gz), 0.9D * (double)fx.size, t * 3.0F, alpha(-1, 0.8F * a * band));
         }

         this.drawCracks(fx, f, s, Easing.clamp01((t - fz * 0.3F) / (fz * 0.65F)), a);
      }

   }

   private void drawCracks(KillFx fx, FxFrame f, ShatterStyle.State s, float grow, float a) {
      if(grow > 0.0F) {
         int offset = 0;

         for(int k = 0; k < 9; ++k) {
            float g = Easing.clamp01((grow - s.crackDelay[k]) / (1.0F - s.crackDelay[k]));
            if(g > 0.0F) {
               float reach = g * 6.0F;
               int full = (int)reach;
               int n = 0;

               for(int i = 0; i <= full && i < 7; ++i) {
                  int o = k * 7 + i;
                  Vfx.X[offset + n] = f.rx(s.cx[o]);
                  Vfx.Y[offset + n] = f.ry(s.cy[o]);
                  Vfx.Z[offset + n] = f.rz(s.cz[o]);
                  ++n;
               }

               if(full + 1 < 7 && reach > (float)full) {
                  int o = k * 7 + full;
                  float u = reach - (float)full;
                  Vfx.X[offset + n] = f.rx(s.cx[o] + (s.cx[o + 1] - s.cx[o]) * (double)u);
                  Vfx.Y[offset + n] = f.ry(s.cy[o] + (s.cy[o + 1] - s.cy[o]) * (double)u);
                  Vfx.Z[offset + n] = f.rz(s.cz[o] + (s.cz[o + 1] - s.cz[o]) * (double)u);
                  ++n;
               }

               if(n >= 2) {
                  for(int var15 = 0; var15 < n; ++var15) {
                     Vfx.W[offset + var15] = (float)(0.035D * (double)fx.size * (double)(1.0F - 0.6F * (float)var15 / (float)n));
                     Vfx.C[offset + var15] = alpha(ColorUtil.lerp(-1, this.color.getColor(0.5F), 0.35F), a);
                  }

                  Neon.add(offset, n, false);
                  offset += n;
               }
            }
         }

         Neon.flush(0.6F, 1.0F, 1.0F);
      }

   }

   private void drawShards(KillFx fx, FxFrame f, ShatterStyle.State s, float a) {
      RenderState.translucent(true);
      float end = 1.0F - Easing.smoothstep(fx.styleSeconds - 0.5F, fx.styleSeconds, fx.t);

      for(int i = 0; i < s.count; ++i) {
         float fade = s.landed[i] < 0.0F?1.0F:1.0F - Easing.smoothstep(0.4F, 1.2F, fx.t - s.landed[i]);
         float alpha = a * fade * end;
         if(alpha > 0.01F) {
            axisAngle((double)s.ax[i], (double)s.ay[i], (double)s.az[i], (double)s.angle[i], this.m);
            double sz = (double)s.size[i];
            double x0 = s.x[i];
            double y0 = s.y[i];
            double z0 = s.z[i];
            double px0 = x0 + (this.m[0] * (double)s.shape[i * 6] + this.m[1] * (double)s.shape[i * 6 + 1]) * sz;
            double py0 = y0 + (this.m[3] * (double)s.shape[i * 6] + this.m[4] * (double)s.shape[i * 6 + 1]) * sz;
            double pz0 = z0 + (this.m[6] * (double)s.shape[i * 6] + this.m[7] * (double)s.shape[i * 6 + 1]) * sz;
            double px1 = x0 + (this.m[0] * (double)s.shape[i * 6 + 2] + this.m[1] * (double)s.shape[i * 6 + 3]) * sz;
            double py1 = y0 + (this.m[3] * (double)s.shape[i * 6 + 2] + this.m[4] * (double)s.shape[i * 6 + 3]) * sz;
            double pz1 = z0 + (this.m[6] * (double)s.shape[i * 6 + 2] + this.m[7] * (double)s.shape[i * 6 + 3]) * sz;
            double px2 = x0 + (this.m[0] * (double)s.shape[i * 6 + 4] + this.m[1] * (double)s.shape[i * 6 + 5]) * sz;
            double py2 = y0 + (this.m[3] * (double)s.shape[i * 6 + 4] + this.m[4] * (double)s.shape[i * 6 + 5]) * sz;
            double pz2 = z0 + (this.m[6] * (double)s.shape[i * 6 + 4] + this.m[7] * (double)s.shape[i * 6 + 5]) * sz;
            int skin = s.dust.color[i] | -16777216;
            int c0 = alpha(ColorUtil.lerp(skin, -1, 0.45F), alpha * 0.95F);
            int c1 = alpha(ColorUtil.lerp(skin, this.color.getColor(0.5F), 0.15F), alpha * 0.9F);
            int c2 = alpha(ColorUtil.lerp(skin, -16777216, 0.2F), alpha * 0.9F);
            Vfx.triangle(f.rx(px0), f.ry(py0), f.rz(pz0), f.rx(px1), f.ry(py1), f.rz(pz1), f.rx(px2), f.ry(py2), f.rz(pz2), c0, c1, c2);
         }
      }

      Vfx.flush();
      RenderState.additive();

      for(int var45 = 0; var45 < s.count; ++var45) {
         float fade = s.landed[var45] < 0.0F?1.0F:1.0F - Easing.smoothstep(0.4F, 1.2F, fx.t - s.landed[var45]);
         float alpha = a * fade * end * (0.35F + 0.65F * (float)Math.abs(Math.sin((double)s.angle[var45])));
         if(alpha > 0.02F) {
            axisAngle((double)s.ax[var45], (double)s.ay[var45], (double)s.az[var45], (double)s.angle[var45], this.m);
            double sz = (double)s.size[var45];
            double x0 = s.x[var45];
            double y0 = s.y[var45];
            double z0 = s.z[var45];
            double ax = x0 + (this.m[0] * (double)s.shape[var45 * 6] + this.m[1] * (double)s.shape[var45 * 6 + 1]) * sz;
            double ay = y0 + (this.m[3] * (double)s.shape[var45 * 6] + this.m[4] * (double)s.shape[var45 * 6 + 1]) * sz;
            double az = z0 + (this.m[6] * (double)s.shape[var45 * 6] + this.m[7] * (double)s.shape[var45 * 6 + 1]) * sz;
            double bx = x0 + (this.m[0] * (double)s.shape[var45 * 6 + 2] + this.m[1] * (double)s.shape[var45 * 6 + 3]) * sz;
            double by = y0 + (this.m[3] * (double)s.shape[var45 * 6 + 2] + this.m[4] * (double)s.shape[var45 * 6 + 3]) * sz;
            double bz = z0 + (this.m[6] * (double)s.shape[var45 * 6 + 2] + this.m[7] * (double)s.shape[var45 * 6 + 3]) * sz;
            Vfx.line(Textures.LINE_CORE, f.rx(ax), f.ry(ay), f.rz(az), f.rx(bx), f.ry(by), f.rz(bz), 0.012D * (double)fx.size, alpha(-1, alpha), alpha(this.color.getColor(0.5F), alpha * 0.5F));
         }
      }

   }

   private static void axisAngle(double x, double y, double z, double angle, double[] out) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      double t = 1.0D - c;
      out[0] = t * x * x + c;
      out[1] = t * x * y - s * z;
      out[2] = t * x * z + s * y;
      out[3] = t * x * y + s * z;
      out[4] = t * y * y + c;
      out[5] = t * y * z - s * x;
      out[6] = t * x * z - s * y;
      out[7] = t * y * z + s * x;
      out[8] = t * z * z + c;
   }

   private static final class State {
      final SkinDust dust;
      final double[] x;
      final double[] y;
      final double[] z;
      final float[] vx;
      final float[] vy;
      final float[] vz;
      final float[] ax;
      final float[] ay;
      final float[] az;
      final float[] angle;
      final float[] spin;
      final float[] size;
      final float[] shape;
      final float[] landed;
      int count;
      boolean burst;
      final double[] cx;
      final double[] cy;
      final double[] cz;
      final float[] crackDelay;

      private State() {
         this.dust = new SkinDust(160);
         this.x = new double[160];
         this.y = new double[160];
         this.z = new double[160];
         this.vx = new float[160];
         this.vy = new float[160];
         this.vz = new float[160];
         this.ax = new float[160];
         this.ay = new float[160];
         this.az = new float[160];
         this.angle = new float[160];
         this.spin = new float[160];
         this.size = new float[160];
         this.shape = new float[960];
         this.landed = new float[160];
         this.cx = new double[63];
         this.cy = new double[63];
         this.cz = new double[63];
         this.crackDelay = new float[9];
      }
   }
}
