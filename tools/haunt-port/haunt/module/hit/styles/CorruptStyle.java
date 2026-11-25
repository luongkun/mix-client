package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.core.GameClock;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Glitch;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.PoseSnapshot;
import mix.client.mixui.integration.haunt.render.Silhouette;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import mix.client.mixui.integration.haunt.setting.TextSetting;

public final class CorruptStyle extends HitStyleBase {
   private static final String[] ERRORS = new String[]{"ERR", "NULL", "404", "FAULT", "NaN", "0xDEAD"};
   private static final String[] BLOCKS = new String[]{"█▓▒", "▒░▓", "▓█░"};
   private static final String HEX_DIGITS = "0123456789ABCDEF";
   private static final double BURST = 0.15D;
   private static final double SECOND = 0.08D;
   private static final double SWEEP_FROM = 0.15D;
   private static final double SWEEP_TO = 0.25D;
   private static final double RENOISE = 0.04D;
   private static final long MIN_GAP = 500000000L;
   private static final int TARGETS = 16;
   public final ColorSetting channelB;
   public final ColorSetting glyphColor;
   public final SliderSetting intensity;
   public final SliderSetting split;
   public final SliderSetting slices;
   public final BooleanSetting pixelSort;
   public final BooleanSetting glyphs;
   public final ModeSetting<CorruptStyle.GlyphSet> glyphSet;
   public final TextSetting text;
   private final Silhouette.Look look = new Silhouette.Look();
   private final Glitch.Plane plane = new Glitch.Plane();
   private final StringBuilder hexText = new StringBuilder(6);
   private final int[] lastTarget = new int[16];
   private final long[] lastBurst = new long[16];
   private int lastNext;
   private final double[] dir = new double[2];

   public CorruptStyle(HitEffectModule module) {
      super(module, HitStyle.CORRUPT, 0.35F, new ColorSetting("corrupt.color", "Channel A", "The first channel\'s color.", ColorSetting.Mode.STATIC, -54314, -30165, 1.0D));
      this.signature(this.color, -54314);
      this.channelB = (ColorSetting)this.setting((new ColorSetting("corrupt.channelB", "Channel B", "The second channel\'s color.", ColorSetting.Mode.STATIC, -16718337, -13893752, 1.0D)).secondary());
      this.signature(this.channelB, -16718337);
      this.glyphColor = (ColorSetting)this.setting((new ColorSetting("corrupt.glyphs", "Glyphs", "The error glyph, the scanline and the blocks\' cores.", ColorSetting.Mode.STATIC, -657931, -1, 1.0D)).secondary());
      this.signature(this.glyphColor, -657931);
      this.intensity = this.look((SliderSetting)this.setting((new SliderSetting("corrupt.intensity", "Intensity", "How strong the glitch is.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.split = this.look((SliderSetting)this.setting((new SliderSetting("corrupt.split", "Split Distance", "How far apart the two channels split.", 0.0D, 100.0D, 50.0D, 5.0D)).unit("%")));
      this.slices = (SliderSetting)this.setting(new SliderSetting("corrupt.slices", "Slices", "Bands of the body that jump sideways.", 0.0D, 8.0D, 4.0D, 1.0D));
      this.pixelSort = (BooleanSetting)this.setting(new BooleanSetting("corrupt.pixelSort", "Pixel Sort", "Blocky streaks dripping down from the hit.", true));
      this.glyphs = (BooleanSetting)this.setting(new BooleanSetting("corrupt.glyphsOn", "Glyphs", "An error glyph pops up beside the target.", true));
      this.glyphSet = (ModeSetting)this.setting((new ModeSetting("corrupt.glyphSet", "Glyph Set", "What the glyph says.", CorruptStyle.GlyphSet.class, CorruptStyle.GlyphSet.ERRORS)).style(ModeSetting.Style.CHIPS).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CorruptStyle.this.glyphs.get();
         }
      }));
      this.text = (TextSetting)this.setting((new TextSetting("corrupt.text", "Custom Text", "The glyph\'s text with Custom text.", "SYSTEM ERROR", 12)).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CorruptStyle.this.glyphs.get() && CorruptStyle.this.glyphSet.get() == CorruptStyle.GlyphSet.CUSTOM;
         }
      }));
   }

   public void start(HitFx fx) {
      boolean calm = Glitch.reduceFlashing() && !fx.preview && fx.entityId != -1;
      boolean burst = true;
      if(calm) {
         long now = GameClock.nanos();

         for(int i = 0; i < 16; ++i) {
            if(this.lastTarget[i] == fx.entityId && this.lastBurst[i] != 0L && now - this.lastBurst[i] < 500000000L) {
               burst = false;
               break;
            }
         }

         if(burst) {
            int slot = -1;

            for(int ix = 0; ix < 16; ++ix) {
               if(this.lastTarget[ix] == fx.entityId && this.lastBurst[ix] != 0L) {
                  slot = ix;
                  break;
               }
            }

            if(slot < 0) {
               slot = this.lastNext;
               this.lastNext = (this.lastNext + 1) % 16;
            }

            this.lastTarget[slot] = fx.entityId;
            this.lastBurst[slot] = now;
         }
      }

      fx.data = burst?Boolean.TRUE:Boolean.FALSE;
   }

   private float envelope(HitFx fx, double t) {
      if(fx.data != Boolean.TRUE) {
         return 0.0F;
      } else {
         float e = burstShape(t);
         if(fx.crit && !Glitch.reduceFlashing()) {
            e = Math.max(e, 1.35F * burstShape(t - 0.08D));
         }

         return e * this.intensity.getFloat() / 100.0F;
      }
   }

   private static float burstShape(double t) {
      if(t >= 0.0D && t <= 0.15D) {
         float in = Easing.smoothstep(0.0F, 0.01F, (float)t);
         float out = 1.0F - Easing.smoothstep(0.11F, 0.15F, (float)t);
         float flick = 0.75F + 0.25F * Glitch.hash((int)Math.floor(t / 0.04D), 7);
         return in * out * flick;
      } else {
         return 0.0F;
      }
   }

   private void screenDir(HitFx fx, Camera cam, double[] out) {
      double sx = fx.basis.fx * cam.rightX + fx.basis.fy * cam.rightY + fx.basis.fz * cam.rightZ;
      double sy = fx.basis.fx * cam.upX + fx.basis.fy * cam.upY + fx.basis.fz * cam.upZ;
      double l = Math.sqrt(sx * sx + sy * sy);
      if(l < 0.25D) {
         sx = fx.hit.side >= 0?1.0D:-1.0D;
         sy = 0.0D;
         l = 1.0D;
      }

      out[0] = sx / l;
      out[1] = sy / l;
   }

   public int passes(HitFx fx) {
      return FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      float alpha = fx.alpha();
      if(alpha > 0.003F) {
         double t = (double)fx.styleTime();
         Camera cam = Vfx.camera();
         this.screenDir(fx, cam, this.dir);
         float env = this.envelope(fx, t);
         int seed = fx.seed + (int)Math.floor(t / 0.04D) * 7919;
         if(pass == FxPass.GLOW) {
            this.channels(fx, f, cam, env, seed, alpha);
         } else {
            this.crisp(fx, f, cam, t, env, seed, alpha);
         }
      }

   }

   private void channels(HitFx fx, FxFrame f, Camera cam, float env, int seed, float alpha) {
      PoseSnapshot pose = fx.hit.pose;
      if(pose.valid && env > 0.01F) {
         double s = scale(fx);
         double d = (0.02D + 0.12D * (double)this.split.getFloat() / 100.0D) * s * (0.4D + 0.6D * (double)env);
         double sx = (cam.rightX * this.dir[0] + cam.upX * this.dir[1]) * d;
         double sy = (cam.rightY * this.dir[0] + cam.upY * this.dir[1]) * d;
         double sz = (cam.rightZ * this.dir[0] + cam.upZ * this.dir[1]) * d;
         this.look.reset();
         this.look.fill = 0.32F;
         this.look.rim = 0.0F;
         this.look.inflate = 0.1F;
         double ex = f.rx(fx.ix()) - cam.posX;
         double ey = f.ry(fx.iy()) - cam.posY;
         double ez = f.rz(fx.iz()) - cam.posZ;
         this.look.pullDistance = Math.max(0.3D, Math.sqrt(ex * ex + ey * ey + ez * ez) - 0.12D);
         float soft = Glitch.reduceFlashing()?0.6F:1.0F;
         Silhouette.drawGlitch(pose, f.viewX, f.viewY, f.viewZ, this.look, this.color.getColor(0.5F), this.channelB.getColor(0.5F), sx, sy, sz, this.slices.getInt(), seed, (float)(0.12D * s * (double)env), alpha * soft * Math.min(1.0F, env * 1.3F), !f.preview);
      }

   }

   private void crisp(HitFx fx, FxFrame f, Camera cam, double t, float env, int seed, float alpha) {
      double s = scale(fx);
      double px = f.rx(fx.ix() - fx.basis.fx * 0.15D);
      double py = f.ry(fx.iy() - fx.basis.fy * 0.15D);
      double pz = f.rz(fx.iz() - fx.basis.fz * 0.15D);
      this.plane.set(px, py, pz, cam.rightX, cam.rightY, cam.rightZ, cam.upX, cam.upY, cam.upZ);
      int ca = this.color.getColor(0.5F);
      int cb = this.channelB.getColor(0.5F);
      int cg = this.glyphColor.getColor(0.5F);
      double d = (0.004D + 0.04D * (double)this.split.getFloat() / 100.0D) * s * (0.35D + 0.65D * (double)env);
      float peak = Glitch.reduceFlashing()?0.6F:0.92F;
      double w = 0.32D * s;
      double h = 0.4D * s;
      if(env > 0.01F) {
         Glitch.blocks(this.plane, seed, env, w, h, d, alpha, ca, cb, cg, peak);
      }

      float fade = 1.0F - Easing.smoothstep(0.25F, fx.life / fx.timeScale, (float)t);
      if(this.pixelSort.get()) {
         float k = Math.max(env, 0.6F * fade * (float)Math.min(1.0D, t / 0.05D) * this.intensity.getFloat() / 100.0F);
         Glitch.streaks(this.plane, fx.seed, k, 6, w * 0.8D, 0.0D, 0.6D * s * (double)Easing.clamp01((float)(t / 0.2D)), alpha * fade, ca, cb, cg);
      }

      if(this.glyphs.get()) {
         this.glyph(fx, t, s, d, alpha * fade, ca, cb, cg);
      }

      if(t >= 0.15D && t <= 0.25D) {
         float k = (float)((t - 0.15D) / 0.1D);
         double top = f.ry(fx.anchor.feetY() + (double)fx.hit.height + 0.1D) - py;
         double bottom = f.ry(fx.anchor.feetY()) - py;
         double v = top + (bottom - top) * (double)k;
         double half = (double)fx.hit.width * 0.7D + 0.1D;
         float a = alpha * (float)Math.sin(3.141592653589793D * (double)k) * (Glitch.reduceFlashing()?0.5F:0.9F);
         this.plane.splitRect(-half, v - 0.006D * s, half, v + 0.006D * s, d, Glitch.alphaOf(ca, a * 0.6F), Glitch.alphaOf(cb, a * 0.6F), Glitch.alphaOf(cg, a));
      }

   }

   private void glyph(HitFx fx, double t, double s, double d, float a, int ca, int cb, int cg) {
      if(a > 0.003F && t >= 0.01D) {
         CharSequence str;
         if(fx.crit) {
            str = "CRIT";
         } else {
            switch((CorruptStyle.GlyphSet)this.glyphSet.get()) {
            case HEX:
               this.hexText.setLength(0);
               this.hexText.append('0').append('x').append("0123456789ABCDEF".charAt((int)(Glitch.hash(fx.seed, 1) * 16.0F))).append("0123456789ABCDEF".charAt((int)(Glitch.hash(fx.seed, 2) * 16.0F)));
               str = this.hexText;
               break;
            case BLOCKS:
               str = BLOCKS[(int)(Glitch.hash(fx.seed, 3) * (float)BLOCKS.length) % BLOCKS.length];
               break;
            case CUSTOM:
               str = this.text.get();
               break;
            default:
               str = ERRORS[(int)(Glitch.hash(fx.seed, 4) * (float)ERRORS.length) % ERRORS.length];
            }
         }

         if(str != null && str.length() != 0) {
            double gh = 0.13D * s * (fx.crit?1.25D:1.0D);
            float pop = Easing.outBack(Easing.clamp01((float)(t / 0.05D)), 2.5F);
            double width = Glitch.textWidth(str) * gh;
            double side = fx.hit.side >= 0?1.0D:-1.0D;
            double u = side > 0.0D?(double)fx.hit.width * 0.6D + 0.12D:-((double)fx.hit.width * 0.6D + 0.12D) - width;
            double v = 0.18D * s;
            double jitter = this.envelope(fx, t) > 0.2F?((double)Glitch.hash(fx.seed + (int)(t / 0.04D), 17) - 0.5D) * 0.06D:0.0D;
            float ga = a * Easing.clamp01(pop);
            this.plane.splitText(Glitch.atlas(), str, u + jitter, v, gh * (double)pop, d, Glitch.alphaOf(ca, ga * 0.7F), Glitch.alphaOf(cb, ga * 0.7F), Glitch.alphaOf(cg, ga * 0.95F));
         }
      }

   }

   public String stateForTest(HitFx fx) {
      double t = (double)fx.styleTime();
      Object[] objectArray = new Object[7];
      objectArray[0] = Double.valueOf(t);
      objectArray[1] = Float.valueOf(this.envelope(fx, t));
      Object object = objectArray[2] = fx.data == Boolean.TRUE?"":" (held back: Reduce Flashing)";
      objectArray[3] = t < 0.15D?"bursting":(t < 0.25D?"sweeping":"fading");
      objectArray[4] = fx.hit.pose.valid?"frozen":"none";
      objectArray[5] = Integer.valueOf(this.slices.getInt());
      objectArray[6] = Glitch.reduceFlashing()?"on":"off";
      return String.format(Locale.ROOT, "t %.3f s: glitch %.2f%s, %s, pose %s, slices %d, reduce flashing %s", objectArray);
   }

   public static enum GlyphSet implements Displayable {
      ERRORS("Errors", "ERR, NULL, 404..."),
      HEX("Hex", "0x and a byte"),
      BLOCKS("Blocks", "Shade blocks"),
      CUSTOM("Custom text", "Your own text, up to 12 characters");

      private final String displayName;
      private final String description;

      private GlyphSet(String displayName, String description) {
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
