package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.kit.Ink;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.trail.GroundCache;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.stroke.Strokes;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.client.Minecraft;

public final class BrushstrokeStyle extends TrailStyleBase {
   private static final int MAX_DROPS = 48;
   private static final int NIGHT = -1448744;
   private static final float FLIGHT = 0.22F;
   public final ColorSetting seal = this.signatureColor("seal", "Seal", "The red seal stamped where you stop.", -3660241, -8776672);
   public final ModeSetting<BrushstrokeStyle.Orientation> orientation;
   public final SliderSetting dryness;
   public final SliderSetting splatter;
   public final BooleanSetting sealOn;
   public final ModeSetting<Ink.SealDesign> sealDesign;
   private final Strokes.Style brush = new Strokes.Style();
   private final GroundCache ground = new GroundCache();

   public BrushstrokeStyle(TrailsModule module) {
      super(module, TrailStyle.BRUSHSTROKE, spec().color("Ink", -15461356).signature(-10855846).size(0.05D, 0.6D, 0.25D, 0.01D).taper(25.0D).length(1.6D).anchor(TrailAnchor.FEET, false).fade(TrailStyleBase.FadeCurve.LATE));
      this.doesNotApply(new Setting[]{this.glow});
      this.orientation = (ModeSetting)this.setting((new ModeSetting("brushstroke.orientation", "Orientation", "Flat on the ground, turned to you, or standing along your path.", BrushstrokeStyle.Orientation.class, BrushstrokeStyle.Orientation.GROUND)).style(ModeSetting.Style.CHIPS));
      this.dryness = (SliderSetting)this.setting((new SliderSetting("brushstroke.dryness", "Dryness", "How soon the brush runs dry into bristle streaks.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%"));
      this.splatter = (SliderSetting)this.setting((new SliderSetting("brushstroke.splatter", "Splatter on Turns", "How many ink droplets sharp turns fling.", 0.0D, 100.0D, 40.0D, 5.0D)).unit("%"));
      this.sealOn = (BooleanSetting)this.setting(new BooleanSetting("brushstroke.sealOn", "Seal When I Stop", "A red seal stamps at the head of the stroke where you stop.", true));
      this.sealDesign = (ModeSetting)this.setting((new ModeSetting("brushstroke.sealDesign", "Seal Design", "The seal\'s crest.", Ink.SealDesign.class, Ink.SealDesign.SUN)).style(ModeSetting.Style.CHIPS).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return BrushstrokeStyle.this.sealOn.get();
         }
      }));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.splatter);
   }

   public int passes() {
      return FxPass.TRANSLUCENT.bit();
   }

   protected TrailStyleBase.Store newStore() {
      return new BrushstrokeStyle.Drops();
   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      TrailShape s = p.shape;
      boolean mine = p.self && !f.preview && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
      if(s != null) {
         for(int k = 0; k < s.runs; ++k) {
            this.stroke(p, f, k, mine);
         }
      }

      if(!p.ghost) {
         if(this.splatter.getFloat() > 0.0F && !p.reduced && this.spawnPass(pass)) {
            this.turns(p, f);
         }

         if(this.sealOn.get()) {
            this.stamp(p, f);
         }
      }

   }

   private void stroke(PlayerTrail p, TrailFrame f, int k, boolean mine) {
      int filled = this.fill(p, f, k, this.size.getFloat(), -1.0F, 0.0F);
      if(filled >= 2) {
         int n = Math.min(filled, 512);
         BrushstrokeStyle.Orientation o = (BrushstrokeStyle.Orientation)this.orientation.get();
         double last = Double.NaN;

         for(int j = 0; j < n; ++j) {
            int i = filled - 1 - j;
            double x = Vfx.X[i];
            double y = Vfx.Y[i];
            double z = Vfx.Z[i];
            if(o == BrushstrokeStyle.Orientation.GROUND) {
               double wx = x + f.viewX;
               double feet = y + f.viewY - 0.06D;
               double wz = z + f.viewZ;
               double g = this.ground.surface(f.world, wx, feet, wz, 0.6D, 3.0D);
               if(Double.isNaN(g)) {
                  g = Double.isNaN(last)?feet:last;
               }

               last = g;
               y = g - f.viewY + 0.008D;
            }

            Strokes.X[j] = x;
            Strokes.Y[j] = y;
            Strokes.Z[j] = z;
            Strokes.PC[j] = Vfx.C[i] | -16777216;
            float a = POINT_ALPHA[i];
            if(mine) {
               a = NearCamera.dark(Vfx.camera(), x, y, z, a);
            }

            Strokes.A[j] = a;
         }

         if(o == BrushstrokeStyle.Orientation.UPRIGHT) {
            for(int var26 = 0; var26 < n; ++var26) {
               int a = Math.max(0, var26 - 1);
               int b = Math.min(n - 1, var26 + 1);
               double tx = Strokes.X[b] - Strokes.X[a];
               double tz;
               double tl = Math.sqrt(tx * tx + (tz = Strokes.Z[b] - Strokes.Z[a]) * tz);
               Strokes.NX[var26] = tl < 1.0E-6D?1.0D:-tz / tl;
               Strokes.NY[var26] = 0.0D;
               Strokes.NZ[var26] = tl < 1.0E-6D?0.0D:tx / tl;
            }
         }

         Strokes.Style b = this.brush.reset();
         b.tool = Strokes.Tool.BRUSH;
         b.color = -1;
         b.pointColor = true;
         b.pointAlpha = true;
         b.width = (double)(this.size.getFloat() * p.widthScale);
         b.heavyStart = 1.0F;
         b.thinEnd = 0.7F;
         b.dryness = this.dryness.getFloat() / 100.0F;
         b.light = this.matLight;
         b.flat = o != BrushstrokeStyle.Orientation.FACING;
         b.pointNormal = o == BrushstrokeStyle.Orientation.UPRIGHT;
         b.normalX = 0.0D;
         b.normalY = 1.0D;
         b.normalZ = 0.0D;
         if(this.night > 0.01F) {
            b.alpha = 0.16F * this.night;
            b.width *= 1.5D;
            b.pointColor = false;
            b.color = -1448744;
            Strokes.draw(n, b);
            b.alpha = 1.0F;
            b.width = (double)(this.size.getFloat() * p.widthScale);
            b.pointColor = true;
            b.color = -1;
         }

         Strokes.draw(n, b);
         int from = n * 3 / 5;
         if(n - from >= 2) {
            for(int j2 = from; j2 < n; ++j2) {
               int t = j2 - from;
               Strokes.X[t] = Strokes.X[j2];
               Strokes.Y[t] = Strokes.Y[j2];
               Strokes.Z[t] = Strokes.Z[j2];
               Strokes.PC[t] = Strokes.PC[j2];
               Strokes.A[t] = Strokes.A[j2] * 0.5F * (float)t / (float)(n - from);
               Strokes.NX[t] = Strokes.NX[j2];
               Strokes.NY[t] = Strokes.NY[j2];
               Strokes.NZ[t] = Strokes.NZ[j2];
            }

            b.diffuse = 0.55F;
            b.heavyStart = 0.0F;
            b.thinEnd = 0.0F;
            Strokes.draw(n - from, b);
            b.diffuse = 0.0F;
         }
      }

   }

   private void turns(PlayerTrail p, TrailFrame f) {
      TrailShape s = p.shape;
      if(s != null && s.runs != 0 && p.attached) {
         PlayerTrail.StyleState st = p.style;
         int k = s.runs - 1;
         int end = s.runEnd[k] - 1;
         int from = s.runStart[k];

         int b;
         for(b = end; b > from && s.along[b] < 0.4F; --b) {
            ;
         }

         int c;
         for(c = b; c > from && s.along[c] < 0.8F; --c) {
            ;
         }

         if(c != b && b != end) {
            double h1x = s.x[end] - s.x[b];
            double h1z = s.z[end] - s.z[b];
            double h0x = s.x[b] - s.x[c];
            double h0z = s.z[b] - s.z[c];
            double l1 = Math.sqrt(h1x * h1x + h1z * h1z);
            double l0 = Math.sqrt(h0x * h0x + h0z * h0z);
            if(l1 >= 0.001D && l0 >= 0.001D) {
               double cos = (h1x * h0x + h1z * h0z) / (l1 * l0);
               if(cos <= 0.82D && f.now - st.vars[0] >= 0.35D) {
                  st.vars[0] = f.now;
                  double cross = h0x * h1z - h0z * h1x;
                  double ox = (double)(cross > 0.0D?1:-1) * h1z / l1;
                  double oz = (double)(cross > 0.0D?-1:1) * h1x / l1;
                  BrushstrokeStyle.Drops d = (BrushstrokeStyle.Drops)this.store(f);
                  int count = Math.round(2.0F + 5.0F * this.splatter.getFloat() / 100.0F);
                  float life = this.lengthNow(p.speed) * 0.8F + 0.22F;
                  int col = this.colorAt(0.0F, 0.0F, p.speed, f.colorTime, 0.0F) | -16777216;

                  for(int i = 0; i < count && d.n < 48 && !this.module.piecesFull(f); ++i) {
                     int j = d.n++;
                     double spread = (Math.random() - 0.5D) * 0.9D;
                     double sx = ox * Math.cos(spread) - oz * Math.sin(spread);
                     double sz = ox * Math.sin(spread) + oz * Math.cos(spread);
                     double speed = 1.2D + Math.random() * 1.6D;
                     d.x[j] = p.headX;
                     d.y[j] = p.headY + 0.05D;
                     d.z[j] = p.headZ;
                     d.vx[j] = sx * speed;
                     d.vz[j] = sz * speed;
                     d.vy[j] = 1.0D + Math.random() * 1.2D;
                     double g = this.ground.surface(f.world, p.headX + sx * speed * 0.2199999988079071D, p.headY + 0.3D, p.headZ + sz * speed * 0.2199999988079071D, 0.6D, 3.0D);
                     d.ground[j] = Double.isNaN(g)?p.headY:g + 0.006D;
                     d.age[j] = 0.0F;
                     d.life[j] = life;
                     d.size[j] = (float)((double)this.size.getFloat() * (0.12D + 0.18D * Math.random()));
                     d.turn[j] = (float)(Math.random() * 3.141592653589793D * 2.0D);
                     d.tint[j] = col;
                  }
               }
            }
         }
      }

   }

   private void stamp(PlayerTrail p, TrailFrame f) {
      PlayerTrail.StyleState st = p.style;
      if(p.growing) {
         st.vars[1] = 0.0D;
      } else {
         if(st.vars[1] == 0.0D) {
            st.vars[1] = Math.max(1.0E-6D, f.now);
            st.vars[2] = p.headX;
            st.vars[3] = p.headZ;
         }

         double z;
         double x;
         double g;
         double y = Double.isNaN(g = this.ground.surface(f.world, x = st.vars[2], p.headY + 0.3D, z = st.vars[3], 0.6D, 3.0D))?p.headY:g;
         float since = (float)(f.now - st.vars[1]);
         float press = 1.0F + 0.35F * (1.0F - Easing.outCubic(Easing.clamp01(since / 0.12F)));
         float a = Easing.clamp01(since / 0.05F) * p.alpha * this.opacity.getFloat() / 100.0F * f.nearFade(x, y, z);
         if(a > 0.003F) {
            double half = Math.max(0.12D, (double)this.size.getFloat() * 0.8D) * (double)press;
            int red = ColorUtil.shade(this.tint != 0?this.tint:this.seal.getColor(0.5F), Math.max(0.45F, this.matLight)) | -16777216;
            RenderState.decal(true);
            Ink.quad(Ink.seals(), ((Ink.SealDesign)this.sealDesign.get()).ordinal(), 4, x - f.viewX, y + 0.01D - f.viewY, z - f.viewZ, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, half * 2.0D, half * 2.0D, (float)((double)p.entityId * 0.37D), ColorUtil.mulAlpha(red, a));
            RenderState.decal(false);
         }
      }

   }

   public void renderPiecesPass(TrailFrame f, FxPass pass) {
      BrushstrokeStyle.Drops d = (BrushstrokeStyle.Drops)f.store(this.type);
      if(d != null && d.n != 0) {
         RenderState.decal(true);

         for(int i = 0; i < d.n; ++i) {
            float k = 1.0F - Easing.smoothstep(0.55F, 1.0F, d.age[i] / d.life[i]);
            boolean flying = d.age[i] < 0.22F;
            double r = (double)d.size[i] * (flying?0.6D:1.0D);
            float a = k * f.nearFade(d.x[i], d.y[i], d.z[i]) * f.fog(d.x[i], d.y[i], d.z[i]) * this.opacity.getFloat() / 100.0F;
            if(a > 0.003F) {
               int col = ColorUtil.mulAlpha(lit(d.tint[i], f, d.x[i], d.y[i], d.z[i]), a);
               Ink.quad(Ink.drops(), flying?1:0, 2, d.x[i] - f.viewX, (flying?d.y[i]:d.ground[i]) - f.viewY, d.z[i] - f.viewZ, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, r * 2.0D, r * 2.0D, d.turn[i], col);
            }
         }

         RenderState.decal(false);
      }

   }

   private static final class Drops implements TrailStyleBase.Store {
      final double[] x;
      final double[] y;
      final double[] z;
      final double[] vx;
      final double[] vy;
      final double[] vz;
      final double[] ground;
      final float[] age;
      final float[] life;
      final float[] size;
      final float[] turn;
      final int[] tint;
      int n;

      private Drops() {
         this.x = new double[48];
         this.y = new double[48];
         this.z = new double[48];
         this.vx = new double[48];
         this.vy = new double[48];
         this.vz = new double[48];
         this.ground = new double[48];
         this.age = new float[48];
         this.life = new float[48];
         this.size = new float[48];
         this.turn = new float[48];
         this.tint = new int[48];
      }

      public void update(float dt) {
         int i = 0;

         while(i < this.n) {
            this.age[i] += dt;
            if(this.age[i] >= this.life[i]) {
               int last = --this.n;
               this.copy(last, i);
            } else {
               if(this.age[i] < 0.22F) {
                  this.x[i] += this.vx[i] * (double)dt;
                  this.z[i] += this.vz[i] * (double)dt;
                  this.vy[i] -= 14.0D * (double)dt;
                  this.y[i] = Math.max(this.ground[i], this.y[i] + this.vy[i] * (double)dt);
               }

               ++i;
            }
         }

      }

      private void copy(int from, int to) {
         this.x[to] = this.x[from];
         this.y[to] = this.y[from];
         this.z[to] = this.z[from];
         this.vx[to] = this.vx[from];
         this.vy[to] = this.vy[from];
         this.vz[to] = this.vz[from];
         this.ground[to] = this.ground[from];
         this.age[to] = this.age[from];
         this.life[to] = this.life[from];
         this.size[to] = this.size[from];
         this.turn[to] = this.turn[from];
         this.tint[to] = this.tint[from];
      }

      public int size() {
         return this.n;
      }

      public void clear() {
         this.n = 0;
      }
   }

   public static enum Orientation implements Displayable {
      GROUND("On the ground", "Flat on the ground under your path"),
      FACING("Facing camera", "Always turned to you"),
      UPRIGHT("Upright", "A ribbon standing along your path");

      private final String displayName;
      private final String description;

      private Orientation(String displayName, String description) {
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
}
