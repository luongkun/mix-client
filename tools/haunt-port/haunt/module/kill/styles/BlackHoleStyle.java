package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.WarpShell;
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
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class BlackHoleStyle extends KillStyleBase {
   private static final float FORM = 0.35F;
   private static final float COLLAPSE = 0.22F;
   private static final int STREAKS = 14;
   private static final int STREAK_POINTS = 10;
   private final SliderSetting holeSize = (SliderSetting)this.setting((new SliderSetting("blackhole.size", "Size", "How big the hole grows.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting pull = (SliderSetting)this.setting((new SliderSetting("blackhole.pull", "Pull Time", "How long it takes to swallow the body.", 0.6D, 2.5D, 1.3D, 0.05D)).unit(" s"));
   private final ColorSetting ringColor = (ColorSetting)this.setting((new ColorSetting("blackhole.ring", "Ring Color", "Color of the accretion ring.", -26052)).secondary());

   public BlackHoleStyle(KillEffectsModule module) {
      super(module, KillStyle.BLACK_HOLE, new ColorSetting("blackhole.color", "Glow", "Color of the light falling in and the flash.", -4948737));
   }

   private float pullTime(KillFx fx) {
      return this.pull.getFloat() * (fx.crit?0.85F:1.0F);
   }

   public float seconds(KillFx fx) {
      return 0.35F + this.pullTime(fx) + 0.22F + 0.9F;
   }

   private double radius(KillFx fx) {
      return 0.28D * (double)this.holeSize.getFloat() * (double)fx.size * (fx.crit?1.3D:1.0D);
   }

   public void start(KillFx fx) {
      BlackHoleStyle.State s = new BlackHoleStyle.State();
      fx.data = s;
      s.hx = fx.ctx.feetX - fx.ctx.dirX * 0.1D;
      s.hy = fx.ctx.feetY + 1.15D;
      s.hz = fx.ctx.feetZ - fx.ctx.dirZ * 0.1D;
      float[] order = new float[]{0.28F, 0.4F, 0.03F, 0.08F, 0.15F, 0.21F};

      for(int i = 0; i < 6; ++i) {
         s.start[i] = order[i] + fx.rng.next() * 0.04F;
         s.turn[i] = fx.rng.signed() * 5.0F;
      }

      for(int k = 0; k < 14; ++k) {
         s.streakAngle[k] = fx.rng.next() * 6.2832F;
         s.streakStart[k] = fx.rng.next() * 0.85F;
         s.streakRadius[k] = 1.6F + fx.rng.next() * 1.4F;
      }

   }

   private float pulling(KillFx fx) {
      return Easing.clamp01((fx.t - 0.35F) / this.pullTime(fx));
   }

   private double holeRadius(KillFx fx) {
      double r = this.radius(fx);
      float t = fx.t;
      float grow = Easing.outBack(Easing.clamp01(t / 0.35F));
      float end = 0.35F + this.pullTime(fx);
      float shrink = 1.0F - Easing.inCubic(Easing.clamp01((t - end) / 0.22F));
      return r * (double)grow * (double)shrink * (1.0D + 0.3D * (double)this.pulling(fx));
   }

   public void update(KillFx fx, float dt) {
      BlackHoleStyle.State s = (BlackHoleStyle.State)fx.data;
      float end = 0.35F + this.pullTime(fx) + 0.22F;
      if(!s.flashed && fx.t >= end) {
         s.flashed = true;
         if(!fx.lod) {
            for(int i = 0; i < fx.count(34.0F, true); ++i) {
               double a = (double)fx.rng.next() * 6.2832D;
               double e = (double)fx.rng.signed() * 1.2D;
               float sp = 3.0F + fx.rng.next() * 4.0F;
               fx.particles.spawn(1, s.hx, s.hy, s.hz, (float)(Math.cos(a) * Math.cos(e)) * sp, (float)Math.sin(e) * sp, (float)(Math.sin(a) * Math.cos(e)) * sp, 0.45F + fx.rng.next() * 0.35F, 0.06F * fx.size).drag(2.5F).color(this.color, fx.rng.next()).alpha(fx.opacity).hot(0.8F);
            }
         }
      }

      if(!fx.lod && fx.t > 0.175F && fx.t < end && fx.rng.next() < dt * 30.0F) {
         double a = (double)fx.rng.next() * 6.2832D;
         double e = (double)fx.rng.signed() * 0.8D;
         double r = 1.5D + (double)fx.rng.next();
         double x = s.hx + Math.cos(a) * Math.cos(e) * r;
         double y = s.hy + Math.sin(e) * r;
         double z = s.hz + Math.sin(a) * Math.cos(e) * r;
         float life = 0.5F;
         fx.particles.spawn(3, x, y, z, (float)((s.hx - x) / (double)life), (float)((s.hy - y) / (double)life), (float)((s.hz - z) / (double)life), life, 0.05F * fx.size).end(0.005F).color(this.color, fx.rng.next()).alpha(fx.opacity).hot(0.6F);
      }

   }

   public void render(KillFx fx, FxFrame f) {
      BlackHoleStyle.State s = (BlackHoleStyle.State)fx.data;
      float a = fx.alpha();
      float p = this.pulling(fx);
      double hr = this.holeRadius(fx);

      for(int i = 0; i < 6; ++i) {
         BodyProxy.Part part = fx.parts[i];
         BodyProxy.Part rest = fx.rest[i];
         part.copy(rest);
         float u = Easing.clamp01((p - s.start[i]) / 0.55F);
         if(u > 0.0F) {
            float e = Easing.inCubic(u);
            double ox = rest.x - s.hx;
            double oy = rest.y - s.hy;
            double oz = rest.z - s.hz;
            double dist = Math.sqrt(ox * ox + oy * oy + oz * oz);
            double ang = (double)e * (2.4D + (double)s.turn[i] * 0.1D);
            double c = Math.cos(ang);
            double sn = Math.sin(ang);
            double rx = ox * c - oz * sn;
            double rz = ox * sn + oz * c;
            double k = (double)(1.0F - e);
            part.x = s.hx + rx * k;
            part.y = s.hy + oy * k;
            part.z = s.hz + rz * k;
            if(dist > 1.0E-4D) {
               part.stretch = 1.0F + 2.5F * (float)Math.sin(3.141592653589793D * (double)Math.min(1.0F, u * 1.2F));
               part.sx = s.hx - part.x;
               part.sy = s.hy - part.y;
               part.sz = s.hz - part.z;
               double l = Math.sqrt(part.sx * part.sx + part.sy * part.sy + part.sz * part.sz);
               if(l > 1.0E-4D) {
                  part.sx /= l;
                  part.sy /= l;
                  part.sz /= l;
                  part.ox = part.x;
                  part.oy = part.y;
                  part.oz = part.z;
               } else {
                  part.stretch = 1.0F;
               }
            }

            part.scale = (float)Math.max(0.05D, 1.0D - 0.9D * (double)e);
            part.alpha = 1.0F - Easing.smoothstep(0.75F, 1.0F, u);
            part.visible = u < 1.0F;
            double t = (double)(s.turn[i] * e);
            double ct = Math.cos(t);
            double st = Math.sin(t);

            for(int row = 0; row < 3; ++row) {
               double r0 = rest.r[row * 3];
               double r2 = rest.r[row * 3 + 2];
               part.r[row * 3] = r0 * ct - r2 * st;
               part.r[row * 3 + 2] = r0 * st + r2 * ct;
            }
         }
      }

      fx.look.glow = this.color.getColor(0.5F);
      fx.look.glowAmount = 0.35F * Easing.smoothstep(0.0F, 0.3F, p);
      fx.drawBody(f);
      fx.look.glowAmount = 0.0F;
      RenderState.additive();
      double cx = f.rx(s.hx);
      double cy = f.ry(s.hy);
      double cz = f.rz(s.hz);
      float end = 0.35F + this.pullTime(fx);
      float warp = a * Easing.smoothstep(0.0F, 0.35F, fx.t) * (1.0F - Easing.smoothstep(end, end + 0.22F, fx.t));
      if(warp > 0.02F && !fx.lod && !f.preview && Distortion.refracts()) {
         WarpShell.draw(f, s.hx, s.hy, s.hz, hr * 2.4D + 0.1D, 0.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.0D, 1.0D, 0.0D, 0.0D, 1.0D, false, -1.1F, -1, 0.0F, warp * 0.7F, true);
         RenderState.additive();
      }

      if(hr > 0.002D) {
         this.drawRing(fx, f, s, hr, a);
         RenderState.translucent(false);
         Vfx.sprite(Textures.GLOW, cx, cy, cz, hr * 3.2D, 0.0F, alpha(-16777216, 0.85F * a));
         Vfx.sprite(Textures.BUBBLE, cx, cy, cz, hr * 2.05D, 0.0F, alpha(-16777216, a));
         Vfx.flush();
         RenderState.additive();
         circle(cx, cy, cz, hr * 1.05D, 0.03D * (double)fx.size, alpha(ColorUtil.core(this.color.getColor(0.5F)), 0.8F * a));
      }

      this.drawStreaks(fx, f, s, a);
      float ft = (fx.t - (end + 0.132F)) / 0.6F;
      if(ft > 0.0F && ft < 1.0F) {
         float fl = 1.0F - ft;
         float g = Math.max(0.35F, fx.glow);
         Vfx.sprite(Textures.GLOW, cx, cy, cz, 4.5D * (double)fx.size, 0.0F, alpha(this.color.getColor(0.5F), 0.8F * a * fl * fl * g));
         Vfx.sprite(Textures.FLARE, cx, cy, cz, 3.0D * (double)fx.size, ft * 2.0F, core(this.color.getColor(0.5F), a * fl));
         double rr = (0.3D + 2.6D * (double)Easing.outCubic(ft)) * (double)fx.size;
         circle(cx, cy, cz, rr, 0.25D * (double)fx.size * (double)fl, alpha(this.color.getColor(0.3F), 0.7F * a * fl));
      }

   }

   private static void circle(double cx, double cy, double cz, double r, double w, int color) {
      int n = 48;
      double rx = Vfx.camera().rightX;
      double ry = Vfx.camera().rightY;
      double rz = Vfx.camera().rightZ;
      double ux = Vfx.camera().upX;
      double uy = Vfx.camera().upY;
      double uz = Vfx.camera().upZ;

      for(int i = 0; i < n; ++i) {
         double a = 6.283185307179586D * (double)i / (double)n;
         double c = Math.cos(a);
         double s = Math.sin(a);
         Vfx.X[i] = cx + (rx * c + ux * s) * r;
         Vfx.Y[i] = cy + (ry * c + uy * s) * r;
         Vfx.Z[i] = cz + (rz * c + uz * s) * r;
         Vfx.W[i] = (float)w;
         Vfx.C[i] = color;
      }

      Vfx.ribbon(Textures.LINE_GLOW, n, true);
   }

   private void drawRing(KillFx fx, FxFrame f, BlackHoleStyle.State s, double hr, float a) {
      int n = 64;
      double spinA = (double)fx.t * 3.4D;
      double hx = f.rx(s.hx);
      double tx = Vfx.camera().posX - hx;
      double hz;
      double tz;
      double hy;
      double ty;
      double tl = Math.sqrt(tx * tx + (ty = Vfx.camera().posY - (hy = f.ry(s.hy))) * ty + (tz = Vfx.camera().posZ - (hz = f.rz(s.hz))) * tz);
      if(tl < 1.0E-4D) {
         tx = 0.0D;
         ty = 0.0D;
         tz = 1.0D;
         tl = 1.0D;
      }

      double nx = tx / tl * 0.8D;
      double ny = ty / tl * 0.8D + 0.6D;
      double nz = tz / tl * 0.8D;
      double ez;
      double ey;
      double nl;
      double ex;
      double el;
      if((el = Math.sqrt((ex = (ny = ny / (nl = Math.sqrt(nx * nx + ny * ny + nz * nz))) * 0.0D - (nz = nz / nl) * 1.0D) * ex + (ey = nz * 0.0D - (nx = nx / nl) * 0.0D) * ey + (ez = nx * 1.0D - ny * 0.0D) * ez)) < 1.0E-4D) {
         ex = 1.0D;
         ey = 0.0D;
         ez = 0.0D;
         el = 1.0D;
      }

      double var60;
      double var61;
      double gx = ny * (var60 = ez / el) - nz * (var61 = ey / el);
      double var62;
      double gy = nz * (var62 = ex / el) - nx * var60;
      double gz = nx * var61 - ny * var62;

      for(int band = 0; band < 2; ++band) {
         double r = hr * (band == 0?2.2D:1.55D);
         double w = hr * (band == 0?1.1D:0.5D);

         for(int i = 0; i < n; ++i) {
            double ang = 6.283185307179586D * (double)i / (double)n;
            double c = Math.cos(ang) * r;
            double sn = Math.sin(ang) * r;
            Vfx.X[i] = hx + var62 * c + gx * sn;
            Vfx.Y[i] = hy + var61 * c + gy * sn;
            Vfx.Z[i] = hz + var60 * c + gz * sn;
            Vfx.W[i] = (float)w;
            float along = (float)((ang + spinA) / 6.283185307179586D % 1.0D);
            float bright = 0.55F + 0.45F * (float)Math.sin(ang + spinA);
            Vfx.C[i] = alpha(band == 0?this.ringColor.getColor(along):ColorUtil.core(this.ringColor.getColor(along)), a * bright * (band == 0?0.7F:0.9F));
         }

         Vfx.ribbon(Textures.LINE_GLOW, n, true);
      }

   }

   private void drawStreaks(KillFx fx, FxFrame f, BlackHoleStyle.State s, float a) {
      float p = this.pulling(fx);
      if(p > 0.0F && p < 1.0F && !fx.lod) {
         int offset = 0;

         for(int k = 0; k < 14; ++k) {
            float u = (p - s.streakStart[k] * 0.6F) / 0.4F;
            if(u > 0.0F && u < 1.0F) {
               for(int i = 0; i < 10; ++i) {
                  float q = Easing.clamp01(u - (float)i * 0.035F);
                  double r = (double)(s.streakRadius[k] * fx.size * (1.0F - Easing.inCubic(q))) + 0.05D;
                  double ang = (double)s.streakAngle[k] + (double)q * 4.5D;
                  Vfx.X[offset + i] = f.rx(s.hx + Math.cos(ang) * r);
                  Vfx.Y[offset + i] = f.ry(s.hy + Math.sin(ang * 0.7D + (double)k) * r * 0.25D);
                  Vfx.Z[offset + i] = f.rz(s.hz + Math.sin(ang) * r);
                  Vfx.W[offset + i] = (float)(0.05D * (double)fx.size * (double)(1.0F - (float)i / 10.0F));
                  Vfx.C[offset + i] = alpha(this.color.getColor((float)k / 14.0F), a * (1.0F - (float)i / 10.0F) * (1.0F - u));
               }

               Vfx.ribbon(Textures.LINE_GLOW, offset, 10, false);
               offset += 10;
            }
         }
      }

   }

   private static final class State {
      double hx;
      double hy;
      double hz;
      final float[] start;
      final float[] turn;
      final float[] streakAngle;
      final float[] streakStart;
      final float[] streakRadius;
      boolean flashed;

      private State() {
         this.start = new float[6];
         this.turn = new float[6];
         this.streakAngle = new float[14];
         this.streakStart = new float[14];
         this.streakRadius = new float[14];
      }
   }
}
