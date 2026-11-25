package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.kit.Glitch;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.fx.trail.TrailStrip;
import mix.client.mixui.integration.haunt.module.hit.styles.CorruptStyle;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import mix.client.mixui.integration.haunt.setting.TextSetting;

public final class DatamoshStyle extends TrailStyleBase {
   private static final int MAX_BARS = 16;
   private static final float BAR_FILL = 0.8F;
   private static final float SPLIT = 0.28F;
   private static final double GLYPH_SHOWS = 0.32D;
   private static final double GLYPH_HEIGHT = 0.2D;
   private static final float GLYPH_BURSTS = 0.45F;
   private static final String[] ERRORS = new String[]{"ERR", "NULL", "0xDEAD", "BAD DATA", "CRC", "EOF", "404", "I/O"};
   private static final String[] BLOCKS = new String[]{"█▓▒░", "▒░▓", "▓█░▒", "░▒▓█"};
   private static final String HEX_DIGITS = "0123456789ABCDEF";
   private static final int CHANNEL_A = 1;
   public final ColorSetting channelB = this.signatureColor("channelB", "Channel B", "The second channel\'s color: the bars mix toward it.", -16718337, -13893752);
   public final ColorSetting glyphColor = this.signatureColor("glyphColor", "Glyphs", "The error glyphs, the blocks and the scanlines.", -657931, -1);
   public final SliderSetting bars;
   public final SliderSetting bandHeight;
   public final SliderSetting jitter;
   public final SliderSetting glitchRate;
   public final BooleanSetting scanlines;
   public final BooleanSetting glyphs;
   public final ModeSetting<CorruptStyle.GlyphSet> glyphSet;
   public final TextSetting text;
   private final Glitch.Bursts bursts = new Glitch.Bursts();
   private final Glitch.Plane plane = new Glitch.Plane();
   private final StringBuilder hexText = new StringBuilder(6);
   private static final double[] PX = new double[1024];
   private static final double[] PY = new double[1024];
   private static final double[] PZ = new double[1024];
   private static final float[] PW = new float[1024];
   private static final float[] PA = new float[1024];
   private static final float[] PL = new float[1024];
   private static final int[] PC = new int[1024];
   private static final int[] CA = new int[1024];
   private static final int[] CB = new int[1024];
   public boolean glyphEveryBurstForTest;
   private int drawnBars;
   private int drawnBlocks;
   private int drawnGlyphs;
   private float lastGlitch;
   private boolean counting;

   public DatamoshStyle(TrailsModule module) {
      super(module, TrailStyle.DATAMOSH, spec().color("Channel A", -54314).signature(-30165).taper(30.0D).length(1.0D).anchor(TrailAnchor.WAIST, false).animated());
      this.doesNotApply(new Setting[]{this.size, this.glow});
      this.bars = (SliderSetting)this.setting(new SliderSetting("datamosh.bars", "Bars", "How many bars of color stretch back from you.", 4.0D, 16.0D, 8.0D, 1.0D));
      this.bandHeight = (SliderSetting)this.setting((new SliderSetting("datamosh.bandHeight", "Band Height", "How tall the band of bars is.", 0.4D, 2.0D, 1.2D, 0.05D)).unit(" blocks"));
      this.jitter = (SliderSetting)this.setting((new SliderSetting("datamosh.jitter", "Jitter", "How far the bars jump and smear.", 0.0D, 100.0D, 50.0D, 1.0D)).unit("%"));
      this.glitchRate = (SliderSetting)this.setting((new SliderSetting("datamosh.glitchRate", "Glitch Rate", "How often it glitches: the bars change and jump.", 0.5D, 6.0D, 2.0D, 0.5D)).unit("/s"));
      this.scanlines = (BooleanSetting)this.setting(new BooleanSetting("datamosh.scanlines", "Scanlines", "Thin lines drifting down through the band.", true));
      this.glyphs = (BooleanSetting)this.setting(new BooleanSetting("datamosh.glyphs", "Glyphs", "Now and then an error glyph pops on the trail.", true));
      this.glyphSet = (ModeSetting)this.setting((new ModeSetting("datamosh.glyphSet", "Glyph Set", "What the glyphs say.", CorruptStyle.GlyphSet.class, CorruptStyle.GlyphSet.ERRORS)).style(ModeSetting.Style.CHIPS).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return DatamoshStyle.this.glyphs.get();
         }
      }));
      this.text = (TextSetting)this.setting((new TextSetting("datamosh.text", "Custom Text", "The glyphs\' text with Custom text.", "DATAMOSH", 12)).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return DatamoshStyle.this.glyphs.get() && DatamoshStyle.this.glyphSet.get() == CorruptStyle.GlyphSet.CUSTOM;
         }
      }));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.jitter);
   }

   public int passes() {
      return FxPass.TRANSLUCENT.bit();
   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      boolean bl = this.counting = p.self && !p.ghost && !f.preview;
      if(this.counting) {
         this.drawnGlyphs = 0;
         this.drawnBlocks = 0;
         this.drawnBars = 0;
      }

      TrailShape s = p.shape;
      if(p.shape != null && s.runs != 0) {
         boolean calm = Glitch.reduceFlashing();
         double t = f.now;
         float g = this.bursts.at(t, (double)this.glitchRate.getFloat(), p.entityId * 977 + p.side * 31 + (p.ghost?7:0), calm);
         int seed = this.bursts.seed;
         double since = this.bursts.since;
         if(this.counting) {
            this.lastGlitch = g;
         }

         int cb = this.slotColor(this.channelB, 0.5F, f.colorTime) | -16777216;
         int cg = this.slotColor(this.glyphColor, 0.5F, f.colorTime) | -16777216;
         RenderState.translucent(false);

         for(int k = 0; k < s.runs; ++k) {
            int n = this.copyRun(p, f, k);
            if(n >= 2) {
               this.bars(p, f, n, t, g, seed, calm, cb, cg);
               if(this.scanlines.get()) {
                  this.scanlines(f, n, t, cg);
               }

               if(this.glyphs.get() && !p.reduced && !p.ghost && k == s.runs - 1) {
                  this.glyph(f, n, since, seed, calm, cb, cg);
               }
            }
         }
      }

   }

   private int copyRun(PlayerTrail p, TrailFrame f, int k) {
      int n = this.fill(p, f, k, 1.0F, -1.0F, 0.0F);
      TrailShape s = p.shape;
      int from = s.runStart[k];

      for(int j = 0; j < n; ++j) {
         PX[j] = Vfx.X[j];
         PY[j] = Vfx.Y[j];
         PZ[j] = Vfx.Z[j];
         PW[j] = Vfx.W[j];
         PA[j] = POINT_ALPHA[j];
         PC[j] = Vfx.C[j] | -16777216;
         PL[j] = s.along[from + j];
      }

      return n;
   }

   private void bars(PlayerTrail p, TrailFrame f, int n, double t, float g, int seed, boolean calm, int cb, int cg) {
      int count = this.bars.getInt();
      if(p.reduced) {
         count = (count + 1) / 2;
      }

      float jit = this.jitter.getFloat() / 100.0F;
      double band = (double)this.bandHeight.getFloat();
      double head = (double)PL[n - 1];
      double runLen = (double)PL[0] - head;
      if(runLen >= 0.05D) {
         double stepT = t * (double)this.glitchRate.getFloat();
         int step = (int)Math.floor(stepT);
         float ease = calm?Easing.smoothstep(0.55F, 1.0F, (float)(stepT - Math.floor(stepT))):0.0F;
         double slot = band / (double)count;

         for(int i = 0; i < count; ++i) {
            double yo = (0.5D - ((double)i + 0.5D) / (double)count) * band;
            float th = (float)(slot * 0.800000011920929D * (0.75D + 0.5D * (double)Rng.hash(i, 7)));
            float h = stepped(i * 31, 11, step, ease);
            float back = stepped(i * 31, 17, step, ease);
            double len = runLen * (0.22D + 0.78D * (double)h);
            double start = (double)jit * 0.45D * (double)back;
            double shift = (double)(g * jit) * ((double)Rng.hash(seed, i * 3) - 0.5D) * 0.9D;
            double lift = (double)(g * jit) * ((double)Rng.hash(seed, i * 3 + 1) - 0.5D) * slot * 1.4D;
            double s0 = head + Math.max(0.0D, start + shift);
            double s1 = Math.min(head + runLen, s0 + len);
            if(s1 - s0 >= 0.04D) {
               float d = 0.28F * (1.0F + 1.5F * jit * g);
               int runs = 1 + (int)((calm?Rng.hash(i, 19):stepped(i * 31, 19, step, 0.0F)) * 2.99F);
               boolean drawn = false;
               double from = s0;

               for(int r = 0; r < runs; ++r) {
                  double to = s1;
                  double next = s1;
                  if(r < runs - 1) {
                     double cut = s0 + (s1 - s0) * ((double)r + 0.3D + 0.4D * (double)stepped(i * 31 + r * 7, 23, step, ease)) / (double)runs;
                     double gap = 0.05D + 0.15D * (double)stepped(i * 31 + r * 7, 29, step, ease);
                     to = cut - gap * 0.5D;
                     next = cut + gap * 0.5D;
                  }

                  if(to - from > 0.03D) {
                     float mix = r == 0?Rng.hash(i, 13):Rng.hash(i * 5 + r, 13);
                     drawn |= this.run(f, n, from, to, yo, lift, th, mix, cb, d);
                  }

                  from = next;
               }

               if(drawn) {
                  if(this.counting) {
                     ++this.drawnBars;
                  }

                  this.blocks(f, n, i, s0, s1, yo, lift, th, step, t, g, calm, cb, cg);
               }
            }
         }
      }

   }

   private boolean run(TrailFrame f, int n, double s0, double s1, double yo, double lift, float th, float mix, int cb, float d) {
      int m = this.strip(n, s0, s1, yo, lift, th, mix, cb, 0, 1.0F);
      if(m < 2) {
         return false;
      } else {
         TrailStrip.sides(0, m, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
         TrailStrip.draw(Textures.WHITE, 0, m, 1.0F, 0.0D, 1.0F);
         System.arraycopy(CA, 0, Vfx.C, 0, m);
         TrailStrip.draw(Textures.WHITE, 0, m, d, -(0.5D + (double)d * 0.5D), 1.0F);
         System.arraycopy(CB, 0, Vfx.C, 0, m);
         TrailStrip.draw(Textures.WHITE, 0, m, d, 0.5D + (double)d * 0.5D, 1.0F);
         return true;
      }
   }

   private static float stepped(int key, int salt, int step, float ease) {
      float h = Rng.hash(key ^ step * -1640531535, salt);
      return ease <= 0.0F?h:h + (Rng.hash(key ^ (step + 1) * -1640531535, salt) - h) * ease;
   }

   private void blocks(TrailFrame f, int n, int i, double s0, double s1, double yo, double lift, float th, int step, double t, float g, boolean calm, int cb, int cg) {
      int count = 1 + Math.round(2.0F * g);
      int flick = (int)Math.floor(t * 9.0D);

      for(int b = 0; b < count; ++b) {
         int key = i * 7 + b;
         float vis = calm?0.5F + 0.5F * (float)Math.sin(t * 2.2D + (double)key * 1.7D):(Rng.hash(key, flick) < 0.6F?1.0F:0.0F);
         if(b > 0) {
            vis *= g;
         }

         double bl;
         if(vis > 0.02F && s1 - s0 >= (bl = (double)th * (2.0D + 2.0D * (double)Rng.hash(key, 9))) * 1.2D) {
            double at = s0 + (s1 - s0 - bl) * (double)Rng.hash(key, step + 5);
            int m = this.strip(n, at, at + bl, yo, lift + ((double)Rng.hash(key, step + 6) - 0.5D) * (double)th * 0.6D, th * 1.5F, 0.0F, cb, (b & 1) == 0?cg:(Rng.hash(key, 3) < 0.5F?1:cb), vis);
            if(m >= 2) {
               TrailStrip.sides(0, m, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
               TrailStrip.draw(Textures.WHITE, 0, m, 1.0F, 0.0D, 1.0F);
               if(this.counting) {
                  ++this.drawnBlocks;
               }
            }
         }
      }

   }

   private int strip(int n, double s0, double s1, double yo, double lift, float th, float mix, int cb, int flat, float alpha) {
      int m = 0;

      for(int j = n - 1; j > 0 && m < 1022; --j) {
         double a = (double)PL[j];
         double b = (double)PL[j - 1];
         if(b > s0) {
            if(a >= s1) {
               break;
            }

            if(m == 0) {
               m = this.point(m, j, s0 > a?(s0 - a) / (b - a):0.0D, s0, s1, yo, lift, th, mix, cb, flat, alpha);
            }

            if(b >= s1) {
               m = this.point(m, j, (s1 - a) / (b - a), s0, s1, yo, lift, th, mix, cb, flat, alpha);
               break;
            }

            m = this.point(m, j - 1, 0.0D, s0, s1, yo, lift, th, mix, cb, flat, alpha);
         }
      }

      return m;
   }

   private int point(int m, int j, double w, double s0, double s1, double yo, double lift, float th, float mix, int cb, int flat, float alpha) {
      int k = Math.max(0, j - 1);
      float wf = (float)w;
      float scale = PW[j] + (PW[k] - PW[j]) * wf;
      float a = (PA[j] + (PA[k] - PA[j]) * wf) * alpha;
      double along = (double)PL[j] + (double)(PL[k] - PL[j]) * w;
      Vfx.X[m] = PX[j] + (PX[k] - PX[j]) * w;
      Vfx.Y[m] = PY[j] + (PY[k] - PY[j]) * w + yo * (double)scale + lift;
      Vfx.Z[m] = PZ[j] + (PZ[k] - PZ[j]) * w;
      Vfx.W[m] = th * scale;
      int ca = ColorUtil.lerp(PC[j], PC[k], wf);
      if(flat != 0) {
         Vfx.C[m] = ColorUtil.mulAlpha(flat == 1?ca:flat, a);
      } else {
         float v = (float)((along - s0) / Math.max(1.0E-6D, s1 - s0));
         int c = ColorUtil.lerp(ca, cb, mix);
         c = v < 0.25F?ColorUtil.lerp(c, -1, 0.5F * (0.25F - v) / 0.25F):ColorUtil.shade(c, 1.0F - 0.55F * (v - 0.25F) / 0.75F);
         Vfx.C[m] = ColorUtil.mulAlpha(c | -16777216, a);
         CA[m] = ColorUtil.mulAlpha(ca, a * 0.8F);
         CB[m] = ColorUtil.mulAlpha(cb, a * 0.8F);
      }

      return m + 1;
   }

   private void scanlines(TrailFrame f, int n, double t, int cg) {
      double band = (double)this.bandHeight.getFloat();
      double speed = this.animSpeed == null?1.0D:(double)this.animSpeed.getFloat() / 100.0D;

      for(int l = 0; l < 2; ++l) {
         double q = t * speed / 1.6D + (double)l * 0.5D;
         double pos = 0.5D - (q - Math.floor(q));
         float edge = (float)Math.sin(3.141592653589793D * (pos + 0.5D));

         for(int pass = 0; pass < 2; ++pass) {
            float w = pass == 0?0.09F:0.016F;
            float k = (pass == 0?0.12F:0.55F) * edge;

            for(int j = 0; j < n; ++j) {
               Vfx.X[j] = PX[j];
               Vfx.Y[j] = PY[j] + pos * band * 1.05D * (double)PW[j];
               Vfx.Z[j] = PZ[j];
               Vfx.W[j] = w;
               Vfx.C[j] = ColorUtil.mulAlpha(cg, PA[j] * k);
            }

            TrailStrip.sides(0, n, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);
            TrailStrip.draw(pass == 0?Textures.LINE_GLOW:Textures.WHITE, 0, n, 1.0F, 0.0D, 1.0F);
         }
      }

   }

   private void glyph(TrailFrame f, int n, double since, int seed, boolean calm, int cb, int cg) {
      if(since <= 0.32D) {
         float f2 = Rng.hash(seed, 47);
         float f3 = this.glyphEveryBurstForTest?1.0F:0.45F;
         if(f2 < f3) {
            f2 = calm?Easing.smoothstep(0.0F, 0.06F, (float)since) * (1.0F - Easing.smoothstep(0.22F, 0.32F, (float)since)):1.0F;
            CharSequence str = this.glyphText(seed);
            if(str != null && str.length() != 0 && f2 > 0.01F) {
               double head = (double)PL[n - 1];
               double along = head + ((double)PL[0] - head) * (0.15D + 0.55D * (double)Rng.hash(seed, 41));

               int j;
               for(j = n - 1; j > 1 && (double)PL[j - 1] < along; --j) {
                  ;
               }

               int k = j - 1;
               double w = (double)(PL[k] - PL[j]) > 1.0E-6D?(along - (double)PL[j]) / (double)(PL[k] - PL[j]):0.0D;
               float wf = (float)Math.max(0.0D, Math.min(1.0D, w));
               float scale = PW[j] + (PW[k] - PW[j]) * wf;
               float op = (PA[j] + (PA[k] - PA[j]) * wf) * f2;
               if(op <= 0.01F) {
                  return;
               }

               double gh = 0.2D * (double)scale;
               double x = PX[j] + (PX[k] - PX[j]) * (double)wf;
               double z = PZ[j] + (PZ[k] - PZ[j]) * (double)wf;
               double y = PY[j] + (PY[k] - PY[j]) * (double)wf + ((double)Rng.hash(seed, 43) - 0.5D) * (double)this.bandHeight.getFloat() * 0.6D * (double)scale;
               double width = Glitch.textWidth(str) * gh;
               this.plane.set(x, y, z, f.cam.rightX, f.cam.rightY, f.cam.rightZ, f.cam.upX, f.cam.upY, f.cam.upZ);
               int ca = ColorUtil.lerp(PC[j], PC[k], wf);
               this.plane.splitText(Glitch.atlas(), str, -width * 0.5D, -gh * 0.3D, gh, gh * 0.08D, Glitch.alphaOf(ca, 0.7F * op), Glitch.alphaOf(cb, 0.7F * op), Glitch.alphaOf(cg, 0.95F * op));
               if(this.counting) {
                  ++this.drawnGlyphs;
               }

               return;
            }

            return;
         }
      }

   }

   private CharSequence glyphText(int pick) {
      int q = pick & Integer.MAX_VALUE;
      switch((CorruptStyle.GlyphSet)this.glyphSet.get()) {
      case HEX:
         this.hexText.setLength(0);
         this.hexText.append('0').append('x').append("0123456789ABCDEF".charAt(q % 16)).append("0123456789ABCDEF".charAt(q / 16 % 16));
         return this.hexText;
      case BLOCKS:
         return BLOCKS[q % BLOCKS.length];
      case CUSTOM:
         return this.text.get();
      default:
         return ERRORS[q % ERRORS.length];
      }
   }

   public String stateForTest() {
      return String.format(Locale.ROOT, "%d bar(s), band %.2f, jitter %.0f%%, %.1f glitches/s, scanlines %s, glyphs %s (%s); reduce flashing %s; glitch %.2f; drawn: %d bar(s), %d block(s), %d glyph(s)", new Object[]{Integer.valueOf(this.bars.getInt()), Float.valueOf(this.bandHeight.getFloat()), Float.valueOf(this.jitter.getFloat()), Float.valueOf(this.glitchRate.getFloat()), this.scanlines.get()?"on":"off", this.glyphs.get()?"on":"off", ((CorruptStyle.GlyphSet)this.glyphSet.get()).displayName(), Glitch.reduceFlashing()?"on":"off", Float.valueOf(this.lastGlitch), Integer.valueOf(this.drawnBars), Integer.valueOf(this.drawnBlocks), Integer.valueOf(this.drawnGlyphs)});
   }
}
