package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.ArrayDeque;
import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.Breeze;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Paper;
import mix.client.mixui.integration.haunt.fx.kit.Surface;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class PaperBurstStyle extends HitStyleBase {
   private static final float BURST = 0.1F;
   private static final float FOLD = 0.2F;
   private static final float FADE_FROM = 0.82F;
   private static final int MAX_PIECES = 16;
   private static final int POOL = 8;
   private static final double PIECE = 0.14D;
   private static final double FLUTTER_SPEED = 1.5D;
   private static final float RIM = 0.08F;
   private static final float PAPER_WIND = 0.6F;
   public final SliderSetting pieces;
   public final ModeSetting<PaperBurstStyle.PaperColors> paperColors;
   public final ModeSetting<PaperBurstStyle.Shapes> shapes;
   public final BooleanSetting fold;
   public final SliderSetting flutter;
   public final BooleanSetting paperTexture;
   private final ArrayDeque<PaperBurstStyle.State> pool = new ArrayDeque();
   private final Paper.Pose pose = new Paper.Pose();
   private final float[] hsb = new float[3];
   private static final double[] TMP = new double[3];

   public PaperBurstStyle(HitEffectModule module) {
      super(module, HitStyle.PAPER_BURST, 0.9F, new ColorSetting("paperburst.color", "Paper", "The paper\'s color (One color, and where the Gradient starts).", ColorSetting.Mode.STATIC, -19294, -4345089, 1.0D));
      this.signature(this.color, -19294);
      this.paperColors = (ModeSetting)this.setting(new ModeSetting("paperburst.paperColors", "Paper Colors", "Each piece a different pastel, all the Paper color, or the Paper color\'s hue turning across the burst.", PaperBurstStyle.PaperColors.class, PaperBurstStyle.PaperColors.PASTEL));
      this.pieces = this.look((SliderSetting)this.setting(new SliderSetting("paperburst.pieces", "Pieces", "How many paper squares burst out.", 4.0D, 16.0D, 8.0D, 1.0D)));
      this.shapes = (ModeSetting)this.setting((new ModeSetting("paperburst.shapes", "Shapes", "What the squares fold into.", PaperBurstStyle.Shapes.class, PaperBurstStyle.Shapes.MIXED)).style(ModeSetting.Style.CHIPS));
      this.fold = (BooleanSetting)this.setting(new BooleanSetting("paperburst.fold", "Fold in Flight", "The squares fold themselves as they fly (off: they burst out already folded).", true));
      this.flutter = this.look((SliderSetting)this.setting((new SliderSetting("paperburst.flutter", "Flutter", "How much they sway and tumble on the way down.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.paperTexture = (BooleanSetting)this.setting(new BooleanSetting("paperburst.paperTexture", "Paper Texture", "The fine fibers of the paper.", true));
   }

   public void start(HitFx fx) {
      PaperBurstStyle.State s = (PaperBurstStyle.State)this.pool.poll();
      if(s == null) {
         s = new PaperBurstStyle.State();
      }

      Basis b = fx.basis;
      int n;
      s.n = n = Math.min(16, fx.count((float)this.pieces.getInt(), false));
      s.simTime = 0.0D;
      s.floor.y = fx.hit.groundY;
      double sc = scale(fx);
      float sway = this.flutter.getFloat() / 100.0F;
      double ix = fx.ix() - b.fx * 0.08D;
      double iy = fx.iy() - b.fy * 0.08D;
      double iz = fx.iz() - b.fz * 0.08D;

      for(int i = 0; i < n; ++i) {
         int id = fx.seed + i * 43;
         s.shape[i] = this.shape(i, id);
         double a = 6.283185307179586D * ((double)i + 0.35D * hash(id, 1)) / (double)n + hash(fx.seed, 2) * 6.283185307179586D;
         double dr = Math.cos(a);
         double du = Math.sin(a);
         double out = (2.0D + 1.0D * hash(id, 3)) * sc;
         double along = (1.2D + 1.0D * hash(id, 4)) * sc;
         double up = (0.2D + 0.7D * hash(id, 5)) * sc;
         double vx = b.fx * along + (b.rx * dr + b.ux * du) * out;
         double vy = b.fy * along + (b.ry * dr + b.uy * du) * out + up;
         double vz = b.fz * along + (b.rz * dr + b.uz * du) * out;
         s.fl[i].start(ix, iy, iz, vx, vy, vz, (float)hash(id, 6), sway * (0.6F + 0.8F * (float)hash(id, 7)), (hash(id, 8) - 0.5D) * 6.0D);
         s.foldAt[i] = 0.05000000074505806D + (double)(0.2F * (float)i) / Math.max(1.0D, (double)n - 1.0D) * 0.8D + 0.03D * hash(id, 9);
         double tx = hash(id, 10) - 0.5D;
         double ty = hash(id, 11) - 0.5D;
         double tz = hash(id, 12) - 0.5D;
         double tl = Math.sqrt(tx * tx + ty * ty + tz * tz) + 1.0E-9D;
         s.tumble[i * 3] = tx / tl;
         s.tumble[i * 3 + 1] = ty / tl;
         s.tumble[i * 3 + 2] = tz / tl;
         s.tumbleRate[i] = (6.0D + 8.0D * hash(id, 13)) * (0.4D + 0.6D * (double)sway);
         s.size[i] = 0.14D * sc * (0.85D + 0.3D * hash(id, 14));
         s.along[i] = n <= 1?0.0F:(float)i / (float)(n - 1);
      }

      Breeze.at(ix, iy, iz, fx.preview, s.wind);

      int n2;
      for(int k = 0; k < 3; s.wind[n2] *= 0.4F) {
         n2 = k++;
      }

      fx.data = s;
   }

   private Paper.Shape shape(int i, int id) {
      switch((PaperBurstStyle.Shapes)this.shapes.get()) {
      case CRANES:
         return Paper.CRANE;
      case STARS:
         return Paper.STAR;
      case HEARTS:
         return Paper.HEART;
      case PLANES:
         return Paper.PLANE;
      default:
         int k = (i + (int)(hash(id, 15) * 2.0D)) % 4;
         return k == 0?Paper.CRANE:(k == 1?Paper.STAR:(k == 2?Paper.HEART:Paper.PLANE));
      }
   }

   public void finish(HitFx fx) {
      if(fx.data instanceof PaperBurstStyle.State && this.pool.size() < 8) {
         this.pool.push((PaperBurstStyle.State)fx.data);
      }

      fx.data = null;
   }

   public int passes(HitFx fx) {
      return FxPass.SOLID.bit();
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      PaperBurstStyle.State s = (PaperBurstStyle.State)fx.data;
      if(s != null) {
         float p = fx.progress();
         float alpha = fx.alpha() * (1.0F - Easing.smoothstep(0.82F, 1.0F, p));
         if(alpha > 0.003F) {
            advance(s, (double)fx.styleTime() * 1.5D);
            double t = (double)fx.styleTime();
            boolean folded = !this.fold.get();
            int base = this.color.getColor(0.5F);
            MeshBatch.begin(Material.CUSTOM, Finish.MATTE, fx.light, 0.08F, -1, false, true);

            for(int i = 0; i < s.n; ++i) {
               if(this.place(fx, f, s, i, t, folded)) {
                  int col = this.pieceColor(s, i, base);
                  float a = alpha * Easing.clamp01((float)(t / 0.04D)) * fx.keep.cover(this.pose.x, this.pose.y, this.pose.z);
                  Paper.facets(s.shape[i], this.pose, Math.round(255.0F * Easing.clamp01(a)) << 24 | col & 16777215);
               }
            }

            if(fx.crit) {
               this.crane(fx, f, t, alpha, base);
            }

            MeshBatch.end();
            if(this.paperTexture.get()) {
               RenderState.decal(true);

               try {
                  for(int var19 = 0; var19 < s.n; ++var19) {
                     if(this.place(fx, f, s, var19, t, folded)) {
                        int col = this.pieceColor(s, var19, base);
                        float a = alpha * 0.55F * fx.keep.cover(this.pose.x, this.pose.y, this.pose.z);
                        if(a > 0.01F) {
                           Paper.fibers(s.shape[var19], this.pose, Math.round(255.0F * a) << 24 | ColorUtil.shade(col, 0.55F * fx.light) & 16777215);
                        }
                     }
                  }
               } finally {
                  RenderState.decal(false);
               }
            }
         }
      }

   }

   private static void advance(PaperBurstStyle.State s, double t) {
      while(s.simTime < t - 1.0E-6D) {
         double dt = Math.min(0.008333333333333333D, t - s.simTime);

         for(int i = 0; i < s.n; ++i) {
            s.fl[i].step(dt, s.floor, 0.0D, 0.0D, 0.0D, (double)s.wind[0], (double)s.wind[1], (double)s.wind[2]);
         }

         s.simTime += dt;
      }

   }

   private int pieceColor(PaperBurstStyle.State s, int i, int base) {
      switch((PaperBurstStyle.PaperColors)this.paperColors.get()) {
      case ONE:
         return base;
      case GRADIENT:
         return Paper.hueTurn(base, (s.along[i] - 0.5F) * 0.33F, this.hsb);
      default:
         return Paper.PASTEL_SET[i % Paper.PASTEL_SET.length];
      }
   }

   private boolean place(HitFx fx, FxFrame f, PaperBurstStyle.State s, int i, double t, boolean folded) {
      Paper.Flutter fl = s.fl[i];
      double fold = folded?1.0D:(double)Easing.smoothstep((float)s.foldAt[i], (float)(s.foldAt[i] + 0.20000000298023224D), (float)t);
      double grow = 0.35D + 0.65D * (double)Easing.outCubic(Easing.clamp01((float)(t / 0.10000000149011612D)));
      if(s.shape[i] == Paper.PLANE && !fl.landed) {
         double vl = Math.sqrt(fl.vx * fl.vx + fl.vy * fl.vy + fl.vz * fl.vz);
         if(vl < 1.0E-6D) {
            fl.pose(this.pose, s.size[i] * grow, fold);
         } else {
            double fx0 = fl.vx / vl;
            double fy0 = fl.vy / vl;
            double fz0 = fl.vz / vl;
            double rx = -fz0;
            double rz = fx0;
            double rl = Math.sqrt(rx * rx + fx0 * fx0);
            if(rl < 1.0E-6D) {
               rx = 1.0D;
               rz = 0.0D;
               rl = 1.0D;
            }

            double var41;
            double ux = -(var41 = rz / rl) * fy0;
            double var40;
            double uy = var41 * fx0 - (var40 = rx / rl) * fz0;
            double uz = var40 * fy0;
            double bank = 0.4D * fl.rock();
            double cb = Math.cos(bank);
            double sb = Math.sin(bank);
            this.pose.x = fl.x;
            this.pose.y = fl.y;
            this.pose.z = fl.z;
            this.pose.fx = fx0;
            this.pose.fy = fy0;
            this.pose.fz = fz0;
            this.pose.ux = ux * cb + var40 * sb;
            this.pose.uy = uy * cb;
            this.pose.uz = uz * cb + var41 * sb;
            this.pose.rx = var40 * cb - ux * sb;
            this.pose.ry = -uy * sb;
            this.pose.rz = var41 * cb - uz * sb;
            this.pose.scale = s.size[i] * grow;
            this.pose.fold = fold;
            this.pose.flap = 0.0D;
         }
      } else {
         fl.pose(this.pose, s.size[i] * grow, fold);
         if(!fl.landed) {
            double ang = s.tumbleRate[i] * (1.0D - Math.exp(-2.5D * t)) / 2.5D;
            this.tumble(s.tumble[i * 3], s.tumble[i * 3 + 1], s.tumble[i * 3 + 2], ang);
         }
      }

      this.pose.x = f.rx(this.pose.x);
      this.pose.y = f.ry(this.pose.y);
      this.pose.z = f.rz(this.pose.z);
      return this.pose.scale > 1.0E-4D;
   }

   private void tumble(double kx, double ky, double kz, double ang) {
      double c = Math.cos(ang);
      double sn = Math.sin(ang);
      double[] v = TMP;
      v[0] = this.pose.fx;
      v[1] = this.pose.fy;
      v[2] = this.pose.fz;
      rotate(v, kx, ky, kz, c, sn);
      this.pose.fx = v[0];
      this.pose.fy = v[1];
      this.pose.fz = v[2];
      v[0] = this.pose.ux;
      v[1] = this.pose.uy;
      v[2] = this.pose.uz;
      rotate(v, kx, ky, kz, c, sn);
      this.pose.ux = v[0];
      this.pose.uy = v[1];
      this.pose.uz = v[2];
      v[0] = this.pose.rx;
      v[1] = this.pose.ry;
      v[2] = this.pose.rz;
      rotate(v, kx, ky, kz, c, sn);
      this.pose.rx = v[0];
      this.pose.ry = v[1];
      this.pose.rz = v[2];
   }

   private static void rotate(double[] v, double kx, double ky, double kz, double c, double s) {
      double x = v[0];
      double y = v[1];
      double z = v[2];
      double d = kx * x + ky * y + kz * z;
      v[0] = x * c + (ky * z - kz * y) * s + kx * d * (1.0D - c);
      v[1] = y * c + (kz * x - kx * z) * s + ky * d * (1.0D - c);
      v[2] = z * c + (kx * y - ky * x) * s + kz * d * (1.0D - c);
   }

   private void crane(HitFx fx, FxFrame f, double t, float alpha, int base) {
      Basis b = fx.basis;
      double sc = scale(fx);
      double life = 0.9D;
      double k = Math.min(1.0D, t / life);
      double ix = fx.ix() - b.fx * 0.08D;
      double iy = fx.iy() - b.fy * 0.08D;
      double iz = fx.iz() - b.fz * 0.08D;
      double along = 1.4D * sc * t;
      double up = (3.0D * t - 1.6D * t * t) * sc;
      double side = 0.55D * sc * Math.sin(3.141592653589793D * k);
      double x = ix + b.fx * along + b.rx * side;
      double y = iy + b.fy * along + up;
      double z = iz + b.fz * along + b.rz * side;
      double vx = b.fx * 1.4D + b.rx * 0.55D * 3.141592653589793D / life * Math.cos(3.141592653589793D * k);
      double vy = b.fy * 1.4D + (3.0D - 3.2D * t);
      double vz = b.fz * 1.4D + b.rz * 0.55D * 3.141592653589793D / life * Math.cos(3.141592653589793D * k);
      double vl = Math.sqrt(vx * vx + vy * vy + vz * vz) + 1.0E-9D;
      double fx0 = vx / vl;
      double fy0 = vy / vl;
      double fz0 = vz / vl;
      double rx = -fz0;
      double rl = Math.sqrt(rx * rx + fx0 * fx0) + 1.0E-9D;
      rx = rx / rl;
      double rz = fx0 / rl;
      this.pose.x = f.rx(x);
      this.pose.y = f.ry(y);
      this.pose.z = f.rz(z);
      this.pose.fx = fx0;
      this.pose.fy = fy0;
      this.pose.fz = fz0;
      this.pose.ux = -rz * fy0;
      this.pose.uy = rz * fx0 - rx * fz0;
      this.pose.uz = rx * fy0;
      this.pose.rx = rx;
      this.pose.ry = 0.0D;
      this.pose.rz = rz;
      this.pose.scale = 0.30800000000000005D * sc * (0.4D + 0.6D * (double)Easing.outBack(Easing.clamp01((float)(t / 0.12D)), 1.6F));
      this.pose.fold = 1.0D;
      double beat = Math.min(1.0D, t / (life * 0.7D));
      this.pose.flap = 0.65D * Math.sin(beat * 3.141592653589793D * 4.0D) * (1.0D - beat * 0.3D);
      float a = alpha * fx.keep.cover(this.pose.x, this.pose.y, this.pose.z);
      int col = this.paperColors.get() == PaperBurstStyle.PaperColors.PASTEL?-1:base;
      Paper.facets(Paper.CRANE, this.pose, Math.round(255.0F * Easing.clamp01(a)) << 24 | col & 16777215);
   }

   public String stateForTest(HitFx fx) {
      PaperBurstStyle.State s = (PaperBurstStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         double t = (double)fx.styleTime();
         int folding = 0;
         int folded = 0;
         int landed = 0;

         for(int i = 0; i < s.n; ++i) {
            double k = this.fold.get()?(double)Easing.smoothstep((float)s.foldAt[i], (float)(s.foldAt[i] + 0.20000000298023224D), (float)t):1.0D;
            if(k >= 0.999D) {
               ++folded;
            } else if(k > 0.001D) {
               ++folding;
            }

            if(s.fl[i].landed) {
               ++landed;
            }
         }

         return String.format(Locale.ROOT, "%s, %s, %d pieces: %d folding, %d folded, %d landed; flutter %.0f%%%s", new Object[]{((PaperBurstStyle.Shapes)this.shapes.get()).displayName(), ((PaperBurstStyle.PaperColors)this.paperColors.get()).displayName(), Integer.valueOf(s.n), Integer.valueOf(folding), Integer.valueOf(folded), Integer.valueOf(landed), Float.valueOf(this.flutter.getFloat()), fx.crit?"; crit crane":""});
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   private static final class Floor implements Surface {
      double y;

      private Floor() {
      }

      public double at(double x, double y, double z) {
         return this.y;
      }
   }

   public static enum PaperColors implements Displayable {
      PASTEL("Pastel set", "Each piece a different pastel"),
      ONE("One color", "Every piece the Paper color"),
      GRADIENT("Gradient across the burst", "The Paper color, its hue turning piece by piece");

      private final String displayName;
      private final String description;

      private PaperColors(String displayName, String description) {
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

   public static enum Shapes implements Displayable {
      MIXED("Mixed", "Cranes, stars, hearts and planes"),
      CRANES("Cranes", "Paper cranes"),
      STARS("Stars", "Puffy folded stars"),
      HEARTS("Hearts", "Folded hearts"),
      PLANES("Planes", "Paper planes that glide");

      private final String displayName;
      private final String description;

      private Shapes(String displayName, String description) {
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
      final Paper.Flutter[] fl = new Paper.Flutter[16];
      final Paper.Shape[] shape = new Paper.Shape[16];
      final double[] foldAt = new double[16];
      final double[] tumble = new double[48];
      final double[] tumbleRate = new double[16];
      final double[] size = new double[16];
      final float[] along = new float[16];
      final PaperBurstStyle.Floor floor = new PaperBurstStyle.Floor();
      int n;
      double simTime;
      final float[] wind = new float[3];

      State() {
         for(int i = 0; i < 16; ++i) {
            this.fl[i] = new Paper.Flutter();
         }

      }
   }
}
