package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.kit.Paper;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class PopUpStyle extends LandingStyleBase {
   private static final float RIPPLE = 0.2F;
   private static final float POP = 0.15F;
   private static final float WOBBLE_TO = 0.85F;
   private static final float FOLD = 0.3F;
   private static final float FOLDED = 1.25F;
   private static final float END = 1.6F;
   private static final float JUMP_END = 0.6F;
   private static final int MAX_CUTOUTS = 39;
   private static final double HEIGHT = 0.62D;
   private static final double THICK = 0.008D;
   private static final double EDGE = 1.07D;
   private static final double LX = 0.35D;
   private static final double LY = 0.86D;
   private static final double LZ = 0.37D;
   public final ModeSetting<PopUpStyle.PaperColors> paperColors;
   public final SliderSetting cutouts;
   public final ModeSetting<PopUpStyle.Shapes> shapes;
   public final SliderSetting radius;
   public final SliderSetting wobble;
   public final BooleanSetting texture;
   private final float[] hsb = new float[3];

   public PopUpStyle(LandingImpactModule module) {
      super(module, LandingStyle.POP_UP, 1.6F, 4.9F, new ColorSetting("popup.color", "Paper", "The paper\'s color (One color, Gradient).", ColorSetting.Mode.STATIC, -19294, -4345089, 1.0D));
      this.signature(this.color, -19294);
      this.paperColors = (ModeSetting)this.setting((new ModeSetting("popup.paperColors", "Paper Colors", "A pastel each, the Paper color, or its hue turning round the ring.", PopUpStyle.PaperColors.class, PopUpStyle.PaperColors.PASTEL)).style(ModeSetting.Style.CHIPS));
      this.cutouts = this.look((SliderSetting)this.setting(new SliderSetting("popup.cutouts", "Cutouts", "How many cutouts stand in the ring.", 6.0D, 16.0D, 10.0D, 1.0D)));
      this.shapes = (ModeSetting)this.setting((new ModeSetting("popup.shapes", "Shapes", "Which cutouts pop up.", PopUpStyle.Shapes.class, PopUpStyle.Shapes.MIXED)).style(ModeSetting.Style.CHIPS));
      this.radius = this.look((SliderSetting)this.setting((new SliderSetting("popup.radius", "Ring Radius", "How far round the feet the ring stands.", 0.8D, 3.0D, 1.5D, 0.1D)).unit(" blocks")));
      this.wobble = (SliderSetting)this.setting((new SliderSetting("popup.wobble", "Wobble", "How much they wobble on their hinges.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%"));
      this.texture = (BooleanSetting)this.setting(new BooleanSetting("popup.texture", "Paper Texture", "The paper\'s fibers printed on the cutouts.", true));
   }

   public int passes(LandingFx fx) {
      return FxPass.TRANSLUCENT.bit();
   }

   public void start(LandingFx fx) {
      PopUpStyle.State s = new PopUpStyle.State();
      s.jump = fx.land.jump;
      double r = (double)this.radius.getFloat() * Math.sqrt((double)fx.size) * (s.jump?0.5D:1.0D);
      double h = 0.62D * (double)fx.size * (s.jump?0.7D:1.0D);
      int inner = s.jump?6:this.cutouts.getInt();
      int outer = !s.jump && fx.land.fallHeight >= 4.0F?Math.round((float)inner * 1.4F):0;
      s.inner = inner;
      s.n = Math.min(39, inner + outer);
      double turn = (double)fx.rand(1) * 6.283185307179586D;
      int base = this.color.getColor(0.5F);

      for(int i = 0; i < s.n; ++i) {
         boolean second = i >= inner;
         int k = second?i - inner:i;
         int count = second?outer:inner;
         s.angle[i] = turn + 6.283185307179586D * ((double)k + (second?0.5D:0.0D)) / (double)count;
         s.ring[i] = second?r * 1.6D:r;
         s.height[i] = h * (second?1.15D:1.0D) * (0.9D + 0.2D * (double)fx.rand(10 + i));
         s.delay[i] = (second?0.16000001F:0.0F) + 0.120000005F * (float)k / (float)Math.max(1, count);
         s.phase[i] = fx.rand(30 + i) * 6.28F;
         s.kind[i] = this.kind(i);
         s.color[i] = this.cutoutColor(i, count, k, base);
      }

      fx.data = s;
   }

   private int kind(int i) {
      switch((PopUpStyle.Shapes)this.shapes.get()) {
      case TREES:
         return 0;
      case FLOWERS:
         return 1;
      case STARS:
         return 2;
      case HEARTS:
         return 3;
      default:
         return i % 4;
      }
   }

   private int cutoutColor(int i, int count, int k, int base) {
      switch((PopUpStyle.PaperColors)this.paperColors.get()) {
      case ONE:
         return base | -16777216;
      case GRADIENT:
         return Paper.hueTurn(base, (float)k / (float)Math.max(1, count), this.hsb) | -16777216;
      default:
         return Paper.PASTEL_SET[i % Paper.PASTEL_SET.length];
      }
   }

   private double stand(LandingFx fx, PopUpStyle.State s, int i) {
      float t = fx.styleTime() - s.delay[i];
      if(t <= 0.0F) {
         return 0.0D;
      } else if(s.jump) {
         return Math.sin(3.141592653589793D * (double)Easing.clamp01(t / 0.45000002F)) * 1.05D;
      } else {
         double up = (double)Easing.outBack(Easing.clamp01(t / 0.15F), 2.2F);
         double w = (double)this.wobble.getFloat() / 100.0D;
         double since = (double)(t - 0.15F);
         double sway = since > 0.0D?w * 0.22D * Math.sin(since * 19.0D + (double)s.phase[i]) * Math.exp(-since * 3.2D):0.0D;
         double down = (double)Easing.inOutCubic(Easing.clamp01((t - (0.85F - s.delay[i] * 0.0F)) / 0.3F));
         return (up + sway) * (1.0D - down);
      }
   }

   private static float fade(LandingFx fx, PopUpStyle.State s) {
      return s.jump?1.0F - Easing.smoothstep(0.5F, 0.6F, fx.styleTime()):1.0F - Easing.smoothstep(1.25F, 1.6F, fx.styleTime());
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      PopUpStyle.State s = (PopUpStyle.State)fx.data;
      if(s != null) {
         float alpha = fx.alpha() * fade(fx, s);
         if(alpha > 0.003F) {
            Camera cam = Vfx.camera();
            int atlas = Paper.cutouts(this.texture.get());
            float light = Math.max(0.35F, fx.light);
            RenderState.translucent(true);

            for(int i = 0; i < s.n; ++i) {
               double stand = this.stand(fx, s, i);
               double ca = Math.cos(s.angle[i]);
               double sa = Math.sin(s.angle[i]);
               double x = fx.x() + ca * s.ring[i];
               double z = fx.z() + sa * s.ring[i];
               double gy = Ground.y(fx.ground, x, z);
               if(Ground.lastValid()) {
                  double fx0 = -ca;
                  double fz0 = -sa;
                  double h;
                  double w = h = s.height[i];
                  double rx = f.rx(x);
                  double rz;
                  double ry;
                  float a = alpha * NearCamera.fade(cam, rx, (ry = f.ry(gy + 0.004D)) + h * 0.5D, rz = f.rz(z));
                  if(a > 0.003F) {
                     double lean = (1.0D - (double)Easing.clamp01((float)stand)) * 3.141592653589793D * 0.5D;
                     double nx = fx0 * Math.cos(lean);
                     double ny = Math.sin(lean);
                     double nz = fz0 * Math.cos(lean);
                     float front = (float)(0.8D + 0.2D * Math.max(0.0D, nx * 0.35D + ny * 0.86D + nz * 0.37D));
                     float back = (float)(0.8D + 0.2D * Math.max(0.0D, -nx * 0.35D - ny * 0.86D - nz * 0.37D));
                     int col = s.color[i];
                     int faceFront = ColorUtil.mulAlpha(ColorUtil.shade(col, front * light) | -16777216, a);
                     if(fx.lod) {
                        Paper.popup(atlas, s.kind[i], rx, ry, rz, fx0, fz0, w, h, stand, faceFront);
                     } else {
                        int faceBack = ColorUtil.mulAlpha(ColorUtil.shade(col, back * light) | -16777216, a);
                        int edge = ColorUtil.mulAlpha(ColorUtil.shade(-1, Math.max(front, back) * light) | -16777216, a);
                        double ox = nx * 0.008D;
                        double oy = ny * 0.008D;
                        double oz = nz * 0.008D;
                        Paper.popup(atlas, s.kind[i], rx - ox, ry - oy, rz - oz, fx0, fz0, w, h, stand, faceBack);
                        Paper.popup(atlas, s.kind[i], rx - ox * 0.5D, ry - oy * 0.5D, rz - oz * 0.5D, fx0, fz0, w * 1.07D, h * 1.07D, stand, edge);
                        Paper.popup(atlas, s.kind[i], rx, ry, rz, fx0, fz0, w, h, stand, faceFront);
                     }
                  }
               }
            }

            RenderState.translucent(false);
         }
      }

   }

   public String stateForTest(LandingFx fx) {
      PopUpStyle.State s = (PopUpStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         int up = 0;

         for(int i = 0; i < s.n; ++i) {
            if(this.stand(fx, s, i) > 0.8D) {
               ++up;
            }
         }

         String phase = s.jump?(t < 0.6F?"popping up and straight down":"gone"):(t < 0.35000002F?"popping up":(t < 0.85F?"wobbling":(t < 1.25F?"folding down":(t < 1.6F?"fading":"gone"))));
         return String.format(Locale.ROOT, "%s; %d cutouts (%d in a second ring), %d upright, %s, %s, ring %.2f, wobble %.0f%%, texture %s; fall %.1f (x%.2f)%s; %s, light %.2f", new Object[]{phase, Integer.valueOf(s.n), Integer.valueOf(s.n - s.inner), Integer.valueOf(up), ((PopUpStyle.Shapes)this.shapes.get()).displayName(), ((PopUpStyle.PaperColors)this.paperColors.get()).displayName(), Double.valueOf(s.ring[0]), Float.valueOf(this.wobble.getFloat()), this.texture.get()?"on":"off", Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light)});
      }
   }

   public static enum PaperColors implements Displayable {
      PASTEL("Pastel set", "Each cutout a different pastel"),
      ONE("One color", "All in the Paper color"),
      GRADIENT("Gradient around the ring", "The Paper color, its hue turning round the ring");

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
      MIXED("Mixed", "Trees, flowers, stars and hearts in turn"),
      TREES("Trees", "Little fir trees"),
      FLOWERS("Flowers", "Little flowers"),
      STARS("Stars", "Stars"),
      HEARTS("Hearts", "Hearts");

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
      boolean jump;
      int n;
      int inner;
      final double[] angle;
      final double[] ring;
      final double[] height;
      final float[] delay;
      final float[] phase;
      final int[] kind;
      final int[] color;

      private State() {
         this.angle = new double[39];
         this.ring = new double[39];
         this.height = new double[39];
         this.delay = new float[39];
         this.phase = new float[39];
         this.kind = new int[39];
         this.color = new int[39];
      }
   }
}
