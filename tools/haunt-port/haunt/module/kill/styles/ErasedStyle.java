package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Sketch;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.look.BodyLook;
import mix.client.mixui.integration.haunt.render.look.EraseShader;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ErasedStyle extends KillStyleBase {
   private static final float SWEPT = 0.4F;
   private static final float BOILED = 0.8F;
   private static final float STROKE = 0.2F;
   private static final float END = 1.8F;
   private static final float X_FROM = 0.52F;
   private static final float X_TO = 0.72F;
   private static final double BAND = 0.37D;
   private static final double ACROSS = 0.78D;
   private static final double[] ROW = new double[]{0.52D, -0.15D, -0.82D};
   private static final int MAX_CRUMBS = 30;
   private static final int NIGHT = -3549953;
   public final ColorSetting highlights;
   public final ColorSetting eraserColor;
   public final ModeSetting<Sketch.Tool> tool;
   public final BooleanSetting hatching;
   public final ModeSetting<ErasedStyle.Eraser> eraser;
   public final SliderSetting boil;
   public final BooleanSetting crumbs;
   private final Strokes.Style style = new Strokes.Style();
   private final Sketch.Plane plane = new Sketch.Plane();
   private Sketch.Tool lastTool;
   private static final double[] QU = new double[]{-1.0D, 1.0D, 1.0D, -1.0D};
   private static final double[] QV = new double[]{-1.0D, -1.0D, 1.0D, 1.0D};
   private final double[] qx = new double[4];
   private final double[] qy = new double[4];
   private final double[] qz = new double[4];

   public ErasedStyle(KillEffectsModule module) {
      super(module, KillStyle.ERASED, new ColorSetting("erased.color", "Lines", "The sketch\'s lines and hatching (follows the Tool).", ColorSetting.Mode.STATIC, -12961222, -13668371, 1.0D));
      this.signature(this.color, -12961222);
      this.highlights = (ColorSetting)this.setting((new ColorSetting("erased.highlights", "Highlights", "A crit\'s X and the eraser\'s shine.", ColorSetting.Mode.STATIC, -14020, -1, 1.0D)).secondary());
      this.signature(this.highlights, -14020);
      this.eraserColor = (ColorSetting)this.setting((new ColorSetting("erased.eraser", "Eraser", "The eraser and its crumbs.", ColorSetting.Mode.STATIC, -743752, -10528, 1.0D)).secondary());
      this.signature(this.eraserColor, -743752);
      this.tool = (ModeSetting)this.setting((new ModeSetting("erased.tool", "Tool", "What the sketch is drawn with (its line color comes along).", Sketch.Tool.class, Sketch.Tool.PENCIL)).style(ModeSetting.Style.CHIPS));
      this.lastTool = (Sketch.Tool)this.tool.get();
      this.tool.addListener(new Runnable() {
         public void run() {
            Sketch.Tool now = (Sketch.Tool)ErasedStyle.this.tool.get();
            Sketch.Tool before = ErasedStyle.this.lastTool;
            ErasedStyle.this.lastTool = now;
            Sketch.followTool(ErasedStyle.this.color, before, now);
         }
      });
      this.hatching = (BooleanSetting)this.setting(new BooleanSetting("erased.hatching", "Hatching", "Cross-hatching where the body is in shadow.", true));
      this.eraser = (ModeSetting)this.setting((new ModeSetting("erased.eraserStyle", "Eraser", "A doodled pink eraser wipes it out, or only its smudges.", ErasedStyle.Eraser.class, ErasedStyle.Eraser.DOODLED)).style(ModeSetting.Style.CHIPS));
      this.boil = (SliderSetting)this.setting(new SliderSetting("erased.boil", "Boil", "How often the lines jitter (per second).", 0.0D, 12.0D, 10.0D, 1.0D));
      this.crumbs = (BooleanSetting)this.setting(new BooleanSetting("erased.crumbs", "Crumbs", "Eraser crumbs fall and fade.", true));
   }

   public float seconds(KillFx fx) {
      return 1.8F;
   }

   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | (fx.night > 0.01F && this.tool.get() == Sketch.Tool.PENCIL?FxPass.GLOW.bit():0);
   }

   public void start(KillFx fx) {
      fx.data = new ErasedStyle.State();
   }

   private void aim(KillFx fx, FxFrame f, ErasedStyle.State s, Camera cam) {
      s.aimed = true;
      BodyProxy.Part b = fx.rest[1];
      s.ox = b.x;
      s.oy = b.y;
      s.oz = b.z;
      double rx = cam.rightX;
      double rz = cam.rightZ;
      double rl = Math.sqrt(rx * rx + rz * rz);
      if(rl < 1.0E-4D) {
         rx = 1.0D;
         rz = 0.0D;
         rl = 1.0D;
      }

      s.rx = rx / rl;
      s.ry = 0.0D;
      s.rz = rz / rl;
      s.ux = 0.0D;
      s.uy = 1.0D;
      s.uz = 0.0D;
      double sx = fx.ctx.swipeX * s.rx + fx.ctx.swipeZ * s.rz;
      double sy = fx.ctx.swipeY;
      double sl = Math.sqrt(sx * sx + sy * sy);
      if(sl < 0.001D) {
         sx = 1.0D;
         sy = 0.0D;
         sl = 1.0D;
      }

      sx = sx / sl;
      sy = sy / sl;
      if(sx < 0.0D) {
         sx = -sx;
         sy = -sy;
      }

      double lean = Math.max(-0.42D, Math.min(0.42D, Math.atan2(sy, sx)));
      boolean leftward = fx.ctx.swipeX * s.rx + fx.ctx.swipeZ * s.rz < 0.0D;
      double size = (double)fx.size;

      for(int i = 0; i < 3; ++i) {
         double l = i == 0?lean:(i == 1?-lean * 0.6D - 0.12D:lean * 0.5D + 0.1D);
         boolean fromLeft = i % 2 == 0 != leftward;
         double dx = Math.cos(l) * 0.78D * size;
         double dy = Math.sin(l) * 0.78D * size;
         double my = ROW[i] * size;
         int o = i * 4;
         s.seg[o] = (float)(fromLeft?-dx:dx);
         s.seg[o + 1] = (float)(my + (fromLeft?-dy:dy));
         s.seg[o + 2] = (float)(fromLeft?dx:-dx);
         s.seg[o + 3] = (float)(my + (fromLeft?dy:-dy));
         s.radius[i] = (float)(0.37D * size);
      }

   }

   private static float stroke(KillFx fx, int i) {
      return fx.phase(0.8F + (float)i * 0.2F, 0.8F + (float)(i + 1) * 0.2F);
   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      ErasedStyle.State s = (ErasedStyle.State)fx.data;
      if(s != null) {
         Camera cam = Vfx.camera();
         if(!s.aimed) {
            this.aim(fx, f, s, cam);
         }

         if(pass == FxPass.SOLID) {
            this.body(fx, f, s);
         } else if(pass == FxPass.TRANSLUCENT) {
            this.plane.ox = f.rx(s.ox) - cam.forwardX * 0.42D;
            this.plane.oy = f.ry(s.oy) - cam.forwardY * 0.42D;
            this.plane.oz = f.rz(s.oz) - cam.forwardZ * 0.42D;
            this.plane.rx = s.rx;
            this.plane.ry = s.ry;
            this.plane.rz = s.rz;
            this.plane.ux = s.ux;
            this.plane.uy = s.uy;
            this.plane.uz = s.uz;
            if(fx.crit) {
               this.cross(fx, s, cam);
            }

            this.eraser(fx, s, cam);
            this.crumbs(fx, f, s, cam);
         } else {
            this.night(fx, f);
         }
      }

   }

   private void body(KillFx fx, FxFrame f, ErasedStyle.State s) {
      if(fx.t < 1.45F) {
         BodyProxy.Look look = fx.look;
         look.outlineColor = this.color.getColor(0.5F) | -16777216;
         look.outlineWidth = 2.0F;
         look.outlineBoil = this.boil.getInt() > 0?1.0F:0.0F;
         look.outlineBoilRate = (float)this.boil.getInt();
         look.hatching = this.hatching.get()?1.0F:0.0F;
         look.worldLight = fx.light;
         look.dissolve = -1.0F;
         if(fx.t < 0.4F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = BodyLook.SKETCH;
            look.sweep = Easing.inOutCubic(fx.t / 0.4F);
            look.sweepMode = 1;
            look.sweepOX = fx.ctx.feetX;
            look.sweepOY = fx.ctx.feetY;
            look.sweepOZ = fx.ctx.feetZ;
            look.sweepSpan = 1.95F;
            look.sweepEdgeColor = -461072;
         } else {
            look.kind = BodyLook.SKETCH;
            look.sweepTo = null;
         }

         boolean any = false;

         for(int i = 0; i < 3; ++i) {
            s.done[i] = stroke(fx, i);
            any |= s.done[i] > 0.0F;
         }

         if(any) {
            look.eraseStrokes = 3;
            look.eraseOX = s.ox;
            look.eraseOY = s.oy;
            look.eraseOZ = s.oz;
            look.eraseRX = s.rx;
            look.eraseRY = s.ry;
            look.eraseRZ = s.rz;
            look.eraseUX = s.ux;
            look.eraseUY = s.uy;
            look.eraseUZ = s.uz;
            System.arraycopy(s.seg, 0, look.eraseSeg, 0, 12);
            System.arraycopy(s.radius, 0, look.eraseRadius, 0, 3);
            System.arraycopy(s.done, 0, look.eraseDone, 0, 3);
         } else {
            look.eraseStrokes = 0;
         }

         fx.drawBody(f);
         look.eraseStrokes = 0;
         RenderState.translucent(true);
      }

   }

   private void cross(KillFx fx, ErasedStyle.State s, Camera cam) {
      float drawn = fx.phase(0.52F, 0.72F);
      float gone = Easing.smoothstep(0.0F, 1.0F, stroke(fx, 1));
      if(drawn > 0.0F && gone < 1.0F) {
         double h = 0.85D * (double)fx.size;
         double w = 0.45D * (double)fx.size;

         for(int k = 0; k < 2; ++k) {
            int n = 14;

            for(int i = 0; i < n; ++i) {
               double t = (double)i / ((double)n - 1.0D);
               double u = k == 0?-w + 2.0D * w * t:w - 2.0D * w * t;
               double v = h * 0.75D - 1.6D * h * t;
               this.plane.at(i, u, v);
               Strokes.A[i] = NearCamera.dark(cam, Strokes.X[i], Strokes.Y[i], Strokes.Z[i], 1.0F);
            }

            Strokes.wobble(n, 0.03D * (double)fx.size, fx.seed + k * 7 + (int)(fx.t * (float)this.boil.getInt()), cam.forwardX, cam.forwardY, cam.forwardZ);
            this.draw(n, this.highlights.getColor(0.5F), Sketch.Tool.MARKER, 0.06D * (double)fx.size, fx.alpha() * (1.0F - gone), Easing.clamp01(drawn * 2.0F - (float)k), fx.light);
         }
      }

   }

   private void eraser(KillFx fx, ErasedStyle.State s, Camera cam) {
      int pink = this.eraserColor.getColor(0.5F);
      float alpha = fx.alpha();

      for(int i = 0; i < 3; ++i) {
         float k = stroke(fx, i);
         if(k > 0.0F) {
            int o = i * 4;
            double ax = (double)s.seg[o];
            double ay = (double)s.seg[o + 1];
            double bx = (double)s.seg[o + 2];
            double by = (double)s.seg[o + 3];
            float fade = 1.0F - Easing.smoothstep(0.0F, 0.45F, fx.t - (0.8F + (float)(i + 1) * 0.2F));
            int n = 10;

            for(int j = 0; j < n; ++j) {
               double t = (double)(k * (float)j) / ((double)n - 1.0D);
               this.plane.at(j, ax + (bx - ax) * t, ay + (by - ay) * t + Math.sin(t * 19.0D + (double)i) * 0.03D * (double)fx.size);
            }

            this.draw(n, Sketch.smearColor(this.color.getColor(0.5F)), Sketch.Tool.CHALK, (double)s.radius[i] * 1.4D, alpha * 0.28F * fade, 1.0F, fx.light);
            if(this.eraser.get() == ErasedStyle.Eraser.DOODLED && k < 1.0F) {
               double fx0 = ax + (bx - ax) * (double)k;
               double fy0 = ay + (by - ay) * (double)k + Math.sin((double)fx.t * 60.0D) * 0.02D * (double)fx.size;
               double dx = bx - ax;
               double dy = by - ay;
               double dl = Math.sqrt(dx * dx + dy * dy);
               double hw = 0.34D * (double)fx.size;
               double hh = (double)s.radius[i] * 0.72D;
               double var41;
               double var42;
               this.quad(fx0, fy0, var41 = dx / dl, var42 = dy / dl, hw, hh, pink, alpha);
               this.quad(fx0 - var41 * hw * 0.45D, fy0 - var42 * hw * 0.45D, var41, var42, hw * 0.55D, hh * 1.06D, -11567144, alpha);
               int corners = 9;

               for(int j = 0; j < corners; ++j) {
                  int c = j % 4;
                  double cu = (double)(c != 0 && c != 3?1:-1) * hw;
                  double cv = (double)(c < 2?-1:1) * hh;
                  this.plane.at(j, fx0 + var41 * cu - var42 * cv, fy0 + var42 * cu + var41 * cv);
               }

               Strokes.wobble(corners, 0.012D * (double)fx.size, fx.seed + i, cam.forwardX, cam.forwardY, cam.forwardZ);
               this.draw(corners, this.color.getColor(0.5F), Sketch.Tool.PENCIL, 0.035D * (double)fx.size, alpha, 1.0F, fx.light);
               this.plane.at(0, fx0 - var41 * hw * 0.1D - var42 * hh * 0.5D, fy0 - var42 * hw * 0.1D + var41 * hh * 0.5D);
               this.plane.at(1, fx0 + var41 * hw * 0.8D - var42 * hh * 0.5D, fy0 + var42 * hw * 0.8D + var41 * hh * 0.5D);
               this.draw(2, this.highlights.getColor(0.5F), Sketch.Tool.MARKER, 0.02D * (double)fx.size, alpha * 0.8F, 1.0F, fx.light);
            }
         }
      }

   }

   private void quad(double u, double v, double dx, double dy, double hw, double hh, int col, float alpha) {
      for(int c = 0; c < 4; ++c) {
         double pu = QU[c] * hw;
         double pv = QV[c] * hh;
         double uu = u + dx * pu - dy * pv;
         double vv = v + dy * pu + dx * pv;
         this.qx[c] = this.plane.ox + this.plane.rx * uu + this.plane.ux * vv;
         this.qy[c] = this.plane.oy + this.plane.ry * uu + this.plane.uy * vv;
         this.qz[c] = this.plane.oz + this.plane.rz * uu + this.plane.uz * vv;
      }

      int cc = ColorUtil.mulAlpha(col, alpha);
      Vfx.quadUV(Textures.WHITE, this.qx[0], this.qy[0], this.qz[0], 0.5D, 0.5D, cc, this.qx[1], this.qy[1], this.qz[1], 0.5D, 0.5D, cc, this.qx[2], this.qy[2], this.qz[2], 0.5D, 0.5D, cc, this.qx[3], this.qy[3], this.qz[3], 0.5D, 0.5D, cc);
   }

   private void crumbs(KillFx fx, FxFrame f, ErasedStyle.State s, Camera cam) {
      if(this.crumbs.get()) {
         float t = fx.t;
         double dt = Math.max(0.0D, Math.min(0.1D, (double)(t - s.lastT)));
         s.lastT = t;
         int total = fx.lod?15:30;

         for(int i = 0; i < 3; ++i) {
            float k = stroke(fx, i);
            int want = Math.round(k * (float)total / 3.0F) + i * total / 3;
            int c;
            if(k > 0.0F) {
               for(int o = i * 4; s.spawned < Math.min(total, want) && k < 1.0F; s.crumbs = Math.max(s.crumbs, c + 1)) {
                  c = s.spawned++;
                  double u = (double)(s.seg[o] + (s.seg[o + 2] - s.seg[o]) * k);
                  double v = (double)(s.seg[o + 1] + (s.seg[o + 3] - s.seg[o + 1]) * k);
                  s.cx[c] = s.ox + s.rx * u;
                  s.cy[c] = s.oy + (v - 0.08D * (double)fx.size);
                  s.cz[c] = s.oz + s.rz * u;
                  s.vx[c] = s.rx * (hash(fx.seed, c * 3 + 1) - 0.5D) * 1.2D;
                  s.vy[c] = 0.4D + 0.8D * hash(fx.seed, c * 3 + 2);
                  s.vz[c] = s.rz * (hash(fx.seed, c * 3 + 1) - 0.5D) * 1.2D;
                  s.born[c] = t;
               }
            }
         }

         int pink = ColorUtil.lerp(this.eraserColor.getColor(0.5F), -6645094, 0.35F);
         double groundKeep = Math.pow(0.5D, dt * 60.0D);

         for(int c = 0; c < s.crumbs; ++c) {
            s.vy[c] -= 7.0D * dt;
            s.cx[c] += s.vx[c] * dt;
            s.cy[c] += s.vy[c] * dt;
            s.cz[c] += s.vz[c] * dt;
            if(s.cy[c] < fx.ctx.groundY + 0.02D) {
               s.cy[c] = fx.ctx.groundY + 0.02D;
               s.vy[c] = 0.0D;
               s.vx[c] *= groundKeep;
               s.vz[c] *= groundKeep;
            }

            float age = t - s.born[c];
            float a = fx.alpha() * (1.0F - Easing.smoothstep(0.35F, 0.8F, age)) * (1.0F - fx.phase(1.65F, 1.8F));
            if(a > 0.01F) {
               double r = 0.03D * (double)fx.size;
               double spin = (double)age * 9.0D + (double)c;

               for(int j = 0; j < 4; ++j) {
                  double a2 = spin + (double)j * 2.1D;
                  Strokes.X[j] = f.rx(s.cx[c]) + Math.cos(a2) * r * s.rx;
                  Strokes.Y[j] = f.ry(s.cy[c]) + Math.sin(a2) * r;
                  Strokes.Z[j] = f.rz(s.cz[c]) + Math.cos(a2) * r * s.rz;
               }

               this.draw(4, pink, Sketch.Tool.MARKER, 0.035D * (double)fx.size, a, 1.0F, fx.light);
            }
         }
      }

   }

   private void draw(int n, int col, Sketch.Tool t, double width, float alpha, float progress, float light) {
      this.style.reset();
      this.style.tool = t.stroke();
      this.style.color = col;
      this.style.width = width;
      this.style.alpha = alpha;
      this.style.progress = progress;
      this.style.heavyStart = 0.3F;
      this.style.thinEnd = 0.4F;
      this.style.light = light;
      Strokes.draw(n, this.style);
   }

   private void night(KillFx fx, FxFrame f) {
      float a = fx.alpha() * fx.night * 0.12F * (1.0F - fx.phase(0.8F, 1.4000001F));
      if(a > 0.003F) {
         BodyProxy.Part b = fx.rest[1];
         Vfx.sprite(Textures.GLOW, f.rx(b.x + fx.ctx.dirX * 0.3D), f.ry(b.y + 0.2D), f.rz(b.z + fx.ctx.dirZ * 0.3D), 2.2D * (double)fx.size, 0.0F, alpha(-3549953, a));
      }

   }

   public String stateForTest(KillFx fx) {
      ErasedStyle.State s = (ErasedStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.4F?"turning into a sketch":(t < 0.8F?"boiling":(t < 1.4000001F?"erasing":"the crumbs fall"));
         return String.format(Locale.ROOT, "%s; %s, hatching %s, boil %d/s, %s, wiped %.2f %.2f %.2f (%s), %d crumbs%s; light %.2f, night %.2f", new Object[]{phase, ((Sketch.Tool)this.tool.get()).displayName(), this.hatching.get()?"on":"off", Integer.valueOf(this.boil.getInt()), ((ErasedStyle.Eraser)this.eraser.get()).displayName(), Float.valueOf(stroke(fx, 0)), Float.valueOf(stroke(fx, 1)), Float.valueOf(stroke(fx, 2)), EraseShader.available()?"shader":"a fade a stroke", Integer.valueOf(s.crumbs), fx.crit?", crit X":"", Float.valueOf(fx.light), Float.valueOf(fx.night)});
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   public static enum Eraser implements Displayable {
      DOODLED("Doodled eraser", "A pink eraser swipes across"),
      SMEAR("Smear only", "Only its smudges");

      private final String displayName;
      private final String description;

      private Eraser(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }

   private static final class State {
      boolean aimed;
      double ox;
      double oy;
      double oz;
      double rx;
      double ry;
      double rz;
      double ux;
      double uy;
      double uz;
      final float[] seg;
      final float[] radius;
      final float[] done;
      int crumbs;
      final double[] cx;
      final double[] cy;
      final double[] cz;
      final double[] vx;
      final double[] vy;
      final double[] vz;
      final float[] born;
      int spawned;
      float lastT;

      private State() {
         this.seg = new float[12];
         this.radius = new float[3];
         this.done = new float[3];
         this.cx = new double[30];
         this.cy = new double[30];
         this.cz = new double[30];
         this.vx = new double[30];
         this.vy = new double[30];
         this.vz = new double[30];
         this.born = new float[30];
      }
   }
}
