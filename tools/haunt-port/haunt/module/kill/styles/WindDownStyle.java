package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Chains;
import mix.client.mixui.integration.haunt.fx.kit.Clockwork;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.physics.PhysicsClock;
import mix.client.mixui.integration.haunt.fx.physics.RigidBox;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.PlayerModel;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.look.BodyLook;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class WindDownStyle extends KillStyleBase {
   private static final float SWEPT = 0.4F;
   private static final float APART = 0.5F;
   private static final float SETTLE = 0.4F;
   private static final int MAX = 40;
   private static final int PART = 0;
   private static final int GEAR = 1;
   private static final int PLATE = 2;
   private static final int SPRING = 3;
   private static final double GRAVITY = 24.0D;
   private static final double DAMPING = 0.004D;
   private static final float RIM = 0.2F;
   private static final float DUST_WIND = 0.4F;
   public final ModeSetting<Material> material;
   public final ModeSetting<Finish> finish;
   public final BooleanSetting gears;
   public final BooleanSetting key;
   public final SliderSetting pieces;
   public final SliderSetting scatter;
   public final SliderSetting windTime;
   private static final double[] ROT = new double[9];

   public WindDownStyle(KillEffectsModule module) {
      super(module, KillStyle.WIND_DOWN, new ColorSetting("winddown.color", "Glow", "The glints and the last spark.", ColorSetting.Mode.STATIC, -6237, -15253, 1.0D));
      this.signature(this.color, -6237);
      this.material = this.materialSetting("winddown", Material.BRASS, "What the automaton is made of. Custom takes its Tint, with a finish.");
      this.finish = this.finishSetting("winddown", Finish.METALLIC, this.material);
      this.gears = (BooleanSetting)this.setting(new BooleanSetting("winddown.gears", "Gears at Joints", "Small gears turn at the neck, the shoulders and the hips.", true));
      this.key = (BooleanSetting)this.setting(new BooleanSetting("winddown.key", "Wind-Up Key", "A wind-up key turns on its back.", true));
      this.pieces = this.look((SliderSetting)this.setting(new SliderSetting("winddown.pieces", "Pieces", "How many pieces it falls apart into (its six parts among them).", 10.0D, 40.0D, 24.0D, 1.0D)));
      this.scatter = this.look((SliderSetting)this.setting((new SliderSetting("winddown.scatter", "Scatter", "How far the pieces fly.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%")));
      this.windTime = this.look((SliderSetting)this.setting((new SliderSetting("winddown.windTime", "Wind-Down Time", "How long it takes to wind down.", 0.4D, 2.0D, 0.9D, 0.1D)).unit("s")));
   }

   private float wind() {
      return this.windTime.getFloat();
   }

   private float windEnd() {
      return 0.4F + this.wind();
   }

   public float seconds(KillFx fx) {
      return this.windEnd() + 0.5F + 0.4F;
   }

   public boolean leavesBody() {
      return true;
   }

   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.GLOW.bit();
   }

   public void start(KillFx fx) {
      WindDownStyle.State s = new WindDownStyle.State();
      double ax = fx.ctx.dirX;
      double az = fx.ctx.dirZ;
      double l = Math.sqrt(ax * ax + az * az);
      if(l < 1.0E-4D) {
         ax = 0.0D;
         az = 1.0D;
         l = 1.0D;
      }

      s.awayX = ax / l;
      s.awayZ = az / l;
      int n = Math.min(40, Math.max(6, fx.lod?this.pieces.getInt() / 2:this.pieces.getInt()));

      for(int i = 0; i < n; ++i) {
         WindDownStyle.Piece p = s.pieces[i];
         if(i < 6) {
            p.kind = 0;
            p.part = i;
         } else {
            double h = hash(fx.seed, i * 7 + 1);
            p.kind = h < 0.55D?1:(h < 0.9D?2:3);
            p.part = (int)(hash(fx.seed, i * 7 + 2) * 6.0D) % 6;
            p.size = (0.05D + 0.07D * hash(fx.seed, i * 7 + 3)) * (double)fx.size;
            p.teeth = 8 + (int)(hash(fx.seed, i * 7 + 4) * 9.0D);
            p.hx = (0.07D + 0.08D * hash(fx.seed, i * 7 + 5)) * (double)fx.size;
            p.hy = (0.05D + 0.06D * hash(fx.seed, i * 7 + 6)) * (double)fx.size;
            p.hz = 0.012D * (double)fx.size;
         }
      }

      s.count = n;
      fx.data = s;
   }

   private float winding(KillFx fx) {
      return fx.phase(0.4F, this.windEnd());
   }

   private static double speed(float k) {
      return Math.max(0.0D, 1.0D - (double)k) * Math.max(0.0D, 1.0D - (double)k);
   }

   private double ticked(KillFx fx, float k, double perSecond, double step) {
      double runs = (1.0D - Math.pow(1.0D - (double)k, 3.0D)) / 3.0D * (double)this.wind();
      return Clockwork.ratchet(runs, 1.0D / perSecond, step, false);
   }

   private static double twitch(float k) {
      double sum = 0.0D;

      for(double a : new double[]{0.45D, 0.68D, 0.86D}) {
         double d = (double)k - a;
         if(d >= 0.0D && d < 0.12D) {
            sum += Math.sin(d / 0.12D * 3.141592653589793D) * Math.exp(-d * 18.0D);
         }
      }

      return sum;
   }

   private static void rotateAbout(BodyProxy.Part q, double px, double py, double pz, double kx, double ky, double kz, double a) {
      double c = Math.cos(a);
      double s = Math.sin(a);
      double t = 1.0D - c;
      ROT[0] = c + kx * kx * t;
      ROT[1] = kx * ky * t - kz * s;
      ROT[2] = kx * kz * t + ky * s;
      ROT[3] = ky * kx * t + kz * s;
      ROT[4] = c + ky * ky * t;
      ROT[5] = ky * kz * t - kx * s;
      ROT[6] = kz * kx * t - ky * s;
      ROT[7] = kz * ky * t + kx * s;
      ROT[8] = c + kz * kz * t;
      double ox = q.x - px;
      double oy = q.y - py;
      double oz = q.z - pz;
      q.x = px + ROT[0] * ox + ROT[1] * oy + ROT[2] * oz;
      q.y = py + ROT[3] * ox + ROT[4] * oy + ROT[5] * oz;
      q.z = pz + ROT[6] * ox + ROT[7] * oy + ROT[8] * oz;
      PlayerModel.mul9(ROT, q.r, q.r);
   }

   private void pose(KillFx fx, BodyProxy.Part[] parts) {
      for(int i = 0; i < 6; ++i) {
         parts[i].copy(fx.rest[i]);
      }

      float k = this.winding(fx);
      if(k > 0.0F) {
         double sag = (double)Easing.inOutCubic(k);
         double jerk = twitch(k);
         BodyProxy.Part b = parts[1];
         double lx = b.r[0];
         double ly = b.r[3];
         double lz = b.r[6];
         double dx = b.r[1];
         double dy = b.r[4];
         double dz = b.r[7];
         double px = 0.05859375D;
         BodyProxy.Part h = parts[0];
         double nx = h.x + h.r[1] * 4.0D * px;
         double ny = h.y + h.r[4] * 4.0D * px;
         double nz = h.z + h.r[7] * 4.0D * px;
         rotateAbout(h, nx, ny, nz, lx, ly, lz, Math.toRadians(24.0D * sag - 12.0D * jerk));

         for(int arm = 2; arm <= 3; ++arm) {
            BodyProxy.Part a = parts[arm];
            double sx = a.x - a.r[1] * 4.0D * px;
            double sy = a.y - a.r[4] * 4.0D * px;
            double sz = a.z - a.r[7] * 4.0D * px;
            double side = arm == 2?-1.0D:1.0D;
            rotateAbout(a, sx, sy, sz, dz * side, 0.0D, -dx * side, Math.toRadians(10.0D * jerk));
            rotateAbout(a, sx, sy, sz, lx, ly, lz, Math.toRadians(8.0D * sag));
         }

         double hx = b.x + dx * 6.0D * px;
         double hy = b.y + dy * 6.0D * px;
         double hz = b.z + dz * 6.0D * px;

         for(int i = 0; i < 6; ++i) {
            if(i != 4 && i != 5) {
               rotateAbout(parts[i], hx, hy, hz, lx, ly, lz, Math.toRadians(9.0D * sag + 3.0D * jerk));
            }
         }

         for(BodyProxy.Part p : parts) {
            p.y -= 0.07D * sag * (double)fx.size;
         }
      }

   }

   public void update(KillFx fx, float dt) {
      WindDownStyle.State s = (WindDownStyle.State)fx.data;
      if(s != null) {
         if(!s.apart && fx.t >= this.windEnd()) {
            this.fallApart(fx, s);
         }

         if(s.apart) {
            int steps = s.clock.advance((double)dt);

            for(int i = 0; i < s.count; ++i) {
               WindDownStyle.Piece p = s.pieces[i];

               for(int k = 0; k < steps; ++k) {
                  p.box.step(fx.collider(), 24.0D, 0.004D);
               }
            }
         }
      }

   }

   private void fallApart(KillFx fx, WindDownStyle.State s) {
      s.apart = true;
      BodyProxy.Part[] parts = s.drawn;
      this.pose(fx, parts);
      double force = 0.5D + (double)this.scatter.getFloat() / 100.0D * 2.5D;

      for(int i = 0; i < s.count; ++i) {
         WindDownStyle.Piece p = s.pieces[i];
         BodyProxy.Part q = parts[p.part];
         if(p.kind == 0) {
            PlayerModel.Box b = PlayerModel.base(p.part, fx.ctx.body.slim);
            double k = 0.029296875D;
            p.box.place(q.x, q.y, q.z, q.r, (double)b.w * k, (double)b.h * k, (double)b.d * k);
         } else {
            double ox = (hash(fx.seed, i * 11 + 1) - 0.5D) * 0.25D;
            double oy = (hash(fx.seed, i * 11 + 2) - 0.5D) * 0.5D;
            double oz = (hash(fx.seed, i * 11 + 3) - 0.5D) * 0.15D;
            double x = q.x + q.r[0] * ox + q.r[1] * oy + q.r[2] * oz;
            double y = q.y + q.r[3] * ox + q.r[4] * oy + q.r[5] * oz;
            double z = q.z + q.r[6] * ox + q.r[7] * oy + q.r[8] * oz;
            double hx = p.kind == 1?p.size:(p.kind == 3?p.size * 1.2D:p.hx);
            double hy = p.kind == 1?p.size:(p.kind == 3?p.size * 0.35D:p.hy);
            double hz = p.kind == 1?p.size * 0.18D:(p.kind == 3?p.size * 0.35D:p.hz);
            p.box.place(x, y, z, q.r, hx, hy, hz);
         }

         p.box.restitution = p.kind == 0?0.25F:0.4F;
         double spread = (hash(fx.seed, i * 11 + 4) - 0.5D) * 2.2D;
         double side = -s.awayZ * spread;
         double sideZ = s.awayX * spread;
         double out = force * (0.6D + 0.8D * hash(fx.seed, i * 11 + 5)) * (p.kind == 0?0.6D:1.0D);
         p.vx = (s.awayX * out + side) * (double)fx.size;
         p.vy = (1.0D + 2.2D * hash(fx.seed, i * 11 + 6)) * (p.kind == 0?0.6D:1.0D) * (double)fx.size;
         p.vz = (s.awayZ * out + sideZ) * (double)fx.size;
         double spin = p.kind == 0?4.0D:12.0D;
         p.wx = (hash(fx.seed, i * 11 + 7) - 0.5D) * spin;
         p.wy = (hash(fx.seed, i * 11 + 8) - 0.5D) * spin;
         p.wz = (hash(fx.seed, i * 11 + 9) - 0.5D) * spin;
         p.box.setVelocity(p.vx, p.vy, p.vz, p.wx, p.wy, p.wz, 0.008333333333333333D);
      }

   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      WindDownStyle.State s = (WindDownStyle.State)fx.data;
      if(s != null) {
         if(pass == FxPass.SOLID) {
            this.solid(fx, f, s);
         } else {
            this.glow(fx, f, s);
         }
      }

   }

   private float end(KillFx fx) {
      return fx.endPhase();
   }

   private void solid(KillFx fx, FxFrame f, WindDownStyle.State s) {
      float t = fx.t;
      float end = this.end(fx);
      BodyProxy.Look look = fx.look;
      look.material = (Material)this.material.get();
      look.finish = (Finish)this.finish.get();
      look.materialTint = -2513855;
      look.glint = 0.1F;
      look.roundness = 0.9F;
      look.reflectTilt = 0.35F;
      look.worldLight = fx.light;
      look.detail = 1;
      look.dissolve = -1.0F;
      if(t < 0.4F) {
         look.kind = BodyLook.SKIN;
         look.sweepTo = BodyLook.MATERIAL;
         look.sweep = Easing.inOutCubic(t / 0.4F);
         look.sweepMode = 1;
         look.sweepOX = fx.ctx.feetX;
         look.sweepOY = fx.ctx.feetY;
         look.sweepOZ = fx.ctx.feetZ;
         look.sweepSpan = 1.95F;
         look.sweepEdgeColor = this.color.getColor(0.5F) | -16777216;
      } else {
         look.kind = BodyLook.MATERIAL;
         look.sweepTo = null;
      }

      BodyProxy.Part[] parts = s.drawn;
      if(!s.apart) {
         this.pose(fx, parts);
      } else {
         double a = s.clock.alpha();

         for(int i = 0; i < s.count; ++i) {
            WindDownStyle.Piece p = s.pieces[i];
            if(p.kind == 0) {
               BodyProxy.Part d = parts[p.part];
               d.copy(fx.rest[p.part]);
               p.box.interpolate(a, s.c, s.r);
               d.x = s.c[0];
               d.y = s.c[1];
               d.z = s.c[2];
               System.arraycopy(s.r, 0, d.r, 0, 9);
            }
         }
      }

      float alpha = 1.0F;
      double sink = 0.0D;
      if(end > 0.0F) {
         switch(fx.bodyEnd) {
         case SINK:
            sink = (double)Easing.inCubic(end) * 0.6D;
            alpha = 1.0F - Easing.smoothstep(0.7F, 1.0F, end);
            break;
         case DUST:
            if(!s.dusted) {
               s.dusted = true;
               this.brassDust(fx, s);
            }

            alpha = 1.0F - Easing.smoothstep(0.0F, 0.6F, end);
            break;
         default:
            alpha = 1.0F - Easing.smoothstep(0.0F, 1.0F, end);
         }
      }

      if(alpha > 0.003F) {
         if(sink > 0.0D) {
            for(BodyProxy.Part p : parts) {
               p.y -= sink;
            }
         }

         float before = look.alpha;
         look.alpha = alpha * fx.alpha();
         look.lightmap = !fx.preview;
         look.now = f.now;
         BodyProxy.draw(fx.ctx.body, parts, look, f.viewX, f.viewY, f.viewZ);
         look.alpha = before;
         RenderState.translucent(true);
         Material m = (Material)this.material.get();
         int tint = m == Material.CUSTOM?look.materialTint:-1;
         MeshBatch.begin(m, (Finish)this.finish.get(), fx.light, 0.2F, -1, true, alpha >= 0.999F);
         int col = ColorUtil.mulAlpha(tint, alpha * fx.alpha());
         if(!s.apart) {
            this.joints(fx, f, parts, col);
         } else {
            this.loose(fx, f, s, col, sink);
         }

         MeshBatch.end();
         RenderState.translucent(true);
      }

   }

   private void joints(KillFx fx, FxFrame f, BodyProxy.Part[] parts, int col) {
      float k = this.winding(fx);
      double px = 0.05859375D;
      double sc = (double)fx.size;
      BodyProxy.Part b = parts[1];
      if(this.gears.get()) {
         double turn = this.ticked(fx, k, 6.0D, 0.5235987755982988D);
         gearAt(f, b, 0.0D, -6.0D * px, -2.3D * px, b.r[2], b.r[5], b.r[8], 0.095D * sc, turn, 12, col);

         for(int side = -1; side <= 1; side += 2) {
            gearAt(f, b, (double)side * 4.5D * px, -4.0D * px, 0.0D, b.r[0] * (double)side, b.r[3] * (double)side, b.r[6] * (double)side, 0.085D * sc, -turn * (double)side, 10, col);
            gearAt(f, b, (double)side * 2.0D * px, 6.0D * px, -2.3D * px, b.r[2], b.r[5], b.r[8], 0.07D * sc, turn * 1.3D * (double)side, 9, col);
         }
      }

      if(this.key.get()) {
         double turn = this.ticked(fx, k, 3.0D, 0.7853981633974483D);
         if(fx.crit && k > 0.55F) {
            turn -= (double)Easing.outCubic(Easing.clamp01((k - 0.55F) / 0.2F)) * 3.141592653589793D * 3.0D;
         }

         double pop = fx.crit?(double)Easing.clamp01((k - 0.78F) / 0.22F):0.0D;
         double bx = b.r[2];
         double by = b.r[5];
         double bz = b.r[8];
         double ox = b.x + bx * 2.2D * px - b.r[1] * 1.5D * px;
         double oy = b.y + by * 2.2D * px - b.r[4] * 1.5D * px;
         double oz = b.z + bz * 2.2D * px - b.r[7] * 1.5D * px;
         double fly = pop * 0.9D * sc;
         double kx = ox + bx * (0.12D * sc + fly);
         double ky = oy + by * (0.12D * sc + fly) + pop * pop * 0.4D * sc;
         double kz = oz + bz * (0.12D * sc + fly);
         if(pop > 0.0D) {
            Clockwork.spring(f.rx(ox), f.ry(oy), f.rz(oz), f.rx(kx), f.ry(ky), f.rz(kz), 0.025D * sc, 0.005D * sc, 6, col, 0.2F);
         } else {
            Chains.box(f.rx(ox + bx * 0.06D * sc), f.ry(oy + by * 0.06D * sc), f.rz(oz + bz * 0.06D * sc), bx, by, bz, b.r[0], b.r[3], b.r[6], b.r[1], b.r[4], b.r[7], 0.06D * sc, 0.012D * sc, 0.012D * sc, col, 0.15F);
         }

         double c = Math.cos(turn);
         double s2 = Math.sin(turn);
         double ux = b.r[0] * c + b.r[1] * s2;
         double uy = b.r[3] * c + b.r[4] * s2;
         double uz = b.r[6] * c + b.r[7] * s2;
         double vx = by * uz - bz * uy;
         double vy = bz * ux - bx * uz;
         double vz = bx * uy - by * ux;

         for(int lobe = -1; lobe <= 1; lobe += 2) {
            double lx = kx + ux * 0.06D * sc * (double)lobe;
            double ly = ky + uy * 0.06D * sc * (double)lobe;
            double lz = kz + uz * 0.06D * sc * (double)lobe;
            Clockwork.ring(f.rx(lx), f.ry(ly), f.rz(lz), vx, vy, vz, bx, by, bz, ux, uy, uz, 0.018D * sc, 0.045D * sc, 0.014D * sc, 12, col, 0.2F);
         }
      }

   }

   private static void gearAt(FxFrame f, BodyProxy.Part b, double u, double v, double w, double ax, double ay, double az, double r, double turn, int teeth, int col) {
      double x = b.x + b.r[0] * u + b.r[1] * v + b.r[2] * w;
      double y = b.y + b.r[3] * u + b.r[4] * v + b.r[5] * w;
      double z = b.z + b.r[6] * u + b.r[7] * v + b.r[8] * w;
      Clockwork.gear(f.rx(x), f.ry(y), f.rz(z), ax, ay, az, r, r * 0.3D, turn, teeth, teeth > 9?4:0, col, 0.2F);
   }

   private void loose(KillFx fx, FxFrame f, WindDownStyle.State s, int col, double sink) {
      double a = s.clock.alpha();

      for(int i = 0; i < s.count; ++i) {
         WindDownStyle.Piece p = s.pieces[i];
         if(p.kind != 0) {
            p.box.interpolate(a, s.c, s.r);
            double x = f.rx(s.c[0]);
            double y = f.ry(s.c[1] - sink);
            double z = f.rz(s.c[2]);
            double[] r = s.r;
            switch(p.kind) {
            case 1:
               Clockwork.gear(x, y, z, r[2], r[5], r[8], p.size, p.size * 0.3D, 0.0D, p.teeth, p.teeth > 12?4:0, col, 0.2F);
               break;
            case 2:
               Chains.box(x, y, z, r[0], r[3], r[6], r[1], r[4], r[7], r[2], r[5], r[8], p.hx, p.hy, p.hz, col, 0.15F);
               break;
            default:
               double l = p.size * 1.1D;
               Clockwork.spring(x - r[0] * l, y - r[3] * l, z - r[6] * l, x + r[0] * l, y + r[3] * l, z + r[6] * l, p.size * 0.3D, p.size * 0.06D, 5, col, 0.2F);
            }
         }
      }

   }

   private void brassDust(KillFx fx, WindDownStyle.State s) {
      int each = Math.max(2, fx.count(6.0F, false));

      for(int i = 0; i < s.count; ++i) {
         WindDownStyle.Piece p = s.pieces[i];

         for(int k = 0; k < (p.kind == 0?each * 3:each); ++k) {
            int col = ColorUtil.lerp(-4684242, -863622, fx.rng.next());
            fx.particles.spawn(12, p.box.cx + (double)fx.rng.signed() * 0.15D, p.box.cy + (double)fx.rng.signed() * 0.1D, p.box.cz + (double)fx.rng.signed() * 0.15D, fx.rng.signed() * 0.25F, 0.5F + fx.rng.next() * 0.6F, fx.rng.signed() * 0.25F, 1.1F, 0.035F * fx.size).drag(0.8F).color(col).end(0.01F).delay(fx.rng.next() * 0.5F).wind(0.4F);
         }
      }

   }

   private void glow(KillFx fx, FxFrame f, WindDownStyle.State s) {
      if(!s.apart && this.key.get()) {
         boolean calm = FuriahsVisuals.instance.isReduceFlashing();
         int col = this.color.getColor(0.5F);
         BodyProxy.Part b = fx.rest[1];
         double px = 0.05859375D;
         double x = b.x + b.r[2] * (2.2D * px + 0.12D * (double)fx.size);
         double y = b.y + b.r[5] * (2.2D * px + 0.12D * (double)fx.size);
         double z = b.z + b.r[8] * (2.2D * px + 0.12D * (double)fx.size);
         float k = this.winding(fx);
         float last = 1.0F - Easing.clamp01(Math.abs(k - 0.97F) / 0.05F);
         if(last > 0.0F) {
            Vfx.sprite(Textures.SPARK, f.rx(x), f.ry(y), f.rz(z), 0.35D * (double)fx.size, 0.3F, core(col, (calm?0.4F:0.9F) * last * fx.alpha()));
         }

         float glint;
         if((glint = (float)(0.5D + 0.5D * Math.sin((double)fx.t * 9.0D)) * (float)speed(k) * 0.35F) > 0.02F) {
            Vfx.sprite(Textures.GLOW, f.rx(x), f.ry(y), f.rz(z), 0.25D * (double)fx.size, 0.0F, alpha(col, glint * fx.alpha()));
         }
      }

   }

   public String stateForTest(KillFx fx) {
      WindDownStyle.State s = (WindDownStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.4F?"turning to brass":(t < this.windEnd()?"winding down":(t < this.windEnd() + 0.5F?"falling apart":(t < this.seconds(fx)?"settling":"the pile")));
         int asleep = 0;

         for(int i = 0; i < s.count; ++i) {
            asleep += s.pieces[i].box.sleeping?1:0;
         }

         return String.format(Locale.ROOT, "%s; %s, %d pieces (%d at rest), wind-down %.1f s, speed %.2f, gears %s, key %s%s", new Object[]{phase, ((Material)this.material.get()).displayName(), Integer.valueOf(s.count), Integer.valueOf(asleep), Float.valueOf(this.wind()), Double.valueOf(speed(this.winding(fx))), this.gears.get()?"on":"off", this.key.get()?"on":"off", fx.crit?", crit key":""});
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   private static final class Piece {
      int kind;
      int part;
      final RigidBox box;
      double size;
      double hx;
      double hy;
      double hz;
      int teeth;
      double vx;
      double vy;
      double vz;
      double wx;
      double wy;
      double wz;

      private Piece() {
         this.box = new RigidBox();
      }
   }

   private static final class State {
      final PhysicsClock clock = new PhysicsClock();
      final WindDownStyle.Piece[] pieces = new WindDownStyle.Piece[40];
      int count;
      boolean apart;
      double awayX;
      double awayZ;
      double keyTurn;
      boolean sparked;
      boolean dusted;
      final BodyProxy.Part[] drawn = BodyProxy.newParts();
      final double[] c = new double[3];
      final double[] r = new double[9];

      State() {
         for(int i = 0; i < 40; ++i) {
            this.pieces[i] = new WindDownStyle.Piece();
         }

      }
   }
}
