package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxParticles;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.kit.Glass;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.module.trails.GroundLayout;
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
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class StainedGlassStyle extends TrailStyleBase {
   private static final double TILE_U = 2.0D;
   private static final double TILE_V = 1.0D;
   private static final double UNIT = 0.75D;
   private static final int CELLS = 10;
   private static final long SEED = 5743078L;
   private static final double MOTTLE = 2.0D;
   private static final double STEP = 0.125D;
   private static final double OTHERS_STEP = 0.2D;
   private static final double FAR_STEP = 0.3D;
   private static final double LEAD = 0.035D;
   private static final double RISE = 0.6D;
   private static final double LIT_BELOW = 0.4D;
   private static final int MAX_POLY = 40;
   private static final float MOTE_WIND = 0.5F;
   public final ModeSetting<Glass.GlassColors> glassColors = (ModeSetting)this.setting((new ModeSetting("stained_glass.glassColors", "Glass Colors", "Cathedral: ruby, sapphire, emerald and gold. Tinted: the three glass colors.", Glass.GlassColors.class, Glass.GlassColors.CATHEDRAL)).style(ModeSetting.Style.CHIPS));
   public final ColorSetting glass2 = this.signatureColor("glass2Color", "Glass 2", "The second glass color (Tinted).", -4184513, -38273);
   public final ColorSetting glass3 = this.signatureColor("glass3Color", "Glass 3", "The third glass color (Tinted).", -868018, -6496);
   public final ColorSetting lead = this.signatureColor("leadColor", "Lead", "The lead lines between the cells.", -15066594, -12961212);
   public final ModeSetting<StainedGlassStyle.Variant> variant;
   public final SliderSetting height;
   public final SliderSetting cellSize;
   public final SliderSetting leadWidth;
   public final SliderSetting glassOpacity;
   public final BooleanSetting groundLight;
   public final BooleanSetting motes;
   private final Glass.Cells cells = Glass.periodic(10, 5743078L, 2.0D, 1.0D);
   private final double[] minU = new double[10];
   private final double[] maxU = new double[10];
   private final double[] minV = new double[10];
   private final double[] maxV = new double[10];
   private final int[] palette = new int[4];
   private final Rng rng = new Rng();
   private final Map<PlayerTrail, StainedGlassStyle.Wall[]> wallsOf = new IdentityHashMap();
   private final double[] pu = new double[40];
   private final double[] pv = new double[40];
   private final double[] qu = new double[40];
   private final double[] qv = new double[40];
   private final double[] mx = new double[41];
   private final double[] my = new double[41];
   private final double[] mz = new double[41];
   private int drawnCells;
   private int droppingCells;
   private boolean counting;

   public StainedGlassStyle(TrailsModule module) {
      super(module, TrailStyle.STAINED_GLASS, spec().color("Glass 1", -13935399).signature(-9794305).length(1.6D).anchor(TrailAnchor.FEET, true).fade(TrailStyleBase.FadeCurve.LATE));
      this.glassColors.group("Colors");
      BooleanSupplier tinted = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return StainedGlassStyle.this.glassColors.get() == Glass.GlassColors.TINTED;
         }
      };
      this.color.visibleWhen(tinted);
      this.glass2.visibleWhen(tinted);
      this.glass3.visibleWhen(tinted);
      this.doesNotApply(new Setting[]{this.size, this.taper});
      this.variant = (ModeSetting)this.setting((new ModeSetting("stained_glass.variant", "Variant", "A low wall standing along your path, or a band lying on the ground.", StainedGlassStyle.Variant.class, StainedGlassStyle.Variant.WALL)).style(ModeSetting.Style.CHIPS));
      this.height = (SliderSetting)this.setting((new SliderSetting("stained_glass.height", "Height", "How tall the wall stands (Floor: how wide the band is).", 0.2D, 2.0D, 0.7D, 0.05D)).unit(" blocks"));
      this.cellSize = (SliderSetting)this.setting((new SliderSetting("stained_glass.cellSize", "Cell Size", "How big the glass cells are.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%"));
      this.leadWidth = (SliderSetting)this.setting((new SliderSetting("stained_glass.leadWidth", "Lead Width", "How wide the lead lines are.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%"));
      this.glassOpacity = (SliderSetting)this.setting((new SliderSetting("stained_glass.glassOpacity", "Glass Opacity", "How solid the glass looks.", 40.0D, 100.0D, 75.0D, 1.0D)).unit("%"));
      this.groundLight = (BooleanSetting)this.setting(new BooleanSetting("stained_glass.groundLight", "Ground Light", "Colored light falls on the ground on both sides.", true));
      this.motes = (BooleanSetting)this.setting(new BooleanSetting("stained_glass.motes", "Motes", "Dust motes drift slowly up in the light.", false));

      for(int c = 0; c < this.cells.n; ++c) {
         double a0 = Double.MAX_VALUE;
         double a1 = -1.7976931348623157E308D;
         double b0 = Double.MAX_VALUE;
         double b1 = -1.7976931348623157E308D;

         for(int k = this.cells.start[c]; k < this.cells.start[c] + this.cells.count[c]; ++k) {
            a0 = Math.min(a0, this.cells.u[k]);
            a1 = Math.max(a1, this.cells.u[k]);
            b0 = Math.min(b0, this.cells.v[k]);
            b1 = Math.max(b1, this.cells.v[k]);
         }

         this.minU[c] = a0;
         this.maxU[c] = a1;
         this.minV[c] = b0;
         this.maxV[c] = b1;
      }

   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.height);
      out.add(this.glassOpacity);
   }

   public int passes() {
      return FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   public void update(TrailFrame f) {
      super.update(f);
      if(!this.wallsOf.isEmpty()) {
         Iterator<Entry<PlayerTrail, StainedGlassStyle.Wall[]>> it = this.wallsOf.entrySet().iterator();

         while(it.hasNext()) {
            double seen = -1.0D;
            TrailFrame scene = null;

            for(StainedGlassStyle.Wall w : (StainedGlassStyle.Wall[])((Entry)it.next()).getValue()) {
               if(w != null) {
                  seen = Math.max(seen, w.seen);
                  scene = w.scene;
               }
            }

            if(scene == f && f.now - seen > 2.0D) {
               it.remove();
            }
         }
      }

   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      TrailShape s = p.shape;
      if(s != null && s.runs != 0) {
         StainedGlassStyle.Wall[] slots = (StainedGlassStyle.Wall[])this.wallsOf.get(p);
         if(slots == null) {
            slots = new StainedGlassStyle.Wall[4];
            this.wallsOf.put(p, slots);
         }

         int slot;
         if(slots[slot = p.side * 2 + (p.ghost?1:0)] == null) {
            slots[slot] = new StainedGlassStyle.Wall();
         }

         StainedGlassStyle.Wall w = slots[slot];
         w.seen = f.now;
         w.scene = f;
         if(pass == FxPass.GLOW) {
            if(w.stamp == f.now) {
               RenderState.additive();
               this.light(w);
            }
         } else {
            boolean bl = this.counting = p.self && !p.ghost && !f.preview;
            if(this.counting) {
               this.droppingCells = 0;
               this.drawnCells = 0;
            }

            w.stamp = f.now;
            w.cells = 0;
            w.points = 0;
            w.floor = this.variant.get() == StainedGlassStyle.Variant.FLOOR;
            w.even = !p.self;
            this.palette(f);
            double head = traveledAtHead(p);
            double step = p.reduced?0.3D:(p.self?0.125D:0.2D);
            RenderState.translucent(false);

            for(int k = 0; k < s.runs; ++k) {
               if(GroundLayout.lay(this, p, f, k, head, 1.0F, step) >= 2 && GroundLayout.pieces > 0) {
                  this.wall(p, f, w, head, Math.max(0.001F, s.length));
               }
            }

            this.leads(w, 0.035D * (double)this.leadWidth.getFloat() / 100.0D * 0.5D, this.slotColor(this.lead, 0.5F, f.colorTime) | -16777216);
            if(this.motes.get() && !p.reduced && !p.ghost && this.spawnPass(pass)) {
               this.motes(p, f);
            }
         }
      }

   }

   private void palette(TrailFrame f) {
      if(this.tint != 0) {
         for(int i = 0; i < 4; ++i) {
            this.palette[i] = ColorUtil.shade(this.tint | -16777216, 0.7F + 0.1F * (float)i);
         }
      } else if(this.glassColors.get() == Glass.GlassColors.TINTED) {
         Glass.tinted(this.color.getColor(0.5F, f.colorTime), this.glass2.getColor(0.5F, f.colorTime), this.glass3.getColor(0.5F, f.colorTime), this.palette);
      } else {
         System.arraycopy(Glass.CATHEDRAL_SET, 0, this.palette, 0, 4);
      }

      if(this.light < 0.999F) {
         for(int i = 0; i < 4; ++i) {
            this.palette[i] = ColorUtil.shade(this.palette[i], this.light);
         }
      }

   }

   private void wall(PlayerTrail p, TrailFrame f, StainedGlassStyle.Wall w, double head, float total) {
      boolean floor = w.floor;
      double unit = 0.75D * (double)this.cellSize.getFloat() / 100.0D;
      double hb = (double)(this.height.getFloat() * p.widthScale);
      double hu = hb / unit;
      double u0 = GroundLayout.DIST[0] / unit;
      double u1 = GroundLayout.DIST[GroundLayout.n - 1] / unit;
      long ku0 = (long)Math.floor(u0 / 2.0D) - 1L;
      long ku1 = (long)Math.floor(u1 / 2.0D) + 1L;
      int kv1 = (int)Math.floor(hu / 1.0D) + 1;
      boolean tintedGlass1 = this.tint == 0 && this.glassColors.get() == Glass.GlassColors.TINTED;
      boolean patches = this.groundLight.get() && !p.reduced && !floor;
      float opacityK = this.glassOpacity.getFloat() / 100.0F;
      w.spread = 0.3D + 0.2D * hb;
      w.size = 0.5D + 0.25D * hb;

      for(long ku = ku0; ku <= ku1; ++ku) {
         double ou = (double)ku * 2.0D;

         for(int kv = -1; kv <= kv1; ++kv) {
            double ov = (double)kv * 1.0D;

            for(int c = 0; c < this.cells.n; ++c) {
               int np;
               if(this.maxU[c] + ou > u0 && this.minU[c] + ou < u1 && this.maxV[c] + ov > 0.0D && this.minV[c] + ov < hu && (np = this.clip(c, ou, ov, u0, u1, hu)) >= 3) {
                  double cu = 0.0D;
                  double cv = 0.0D;

                  for(int k = 0; k < np; ++k) {
                     cu += this.pu[k];
                     cv += this.pv[k];
                  }

                  cv = cv / (double)np;
                  double var54;
                  double dc = (var54 = cu / (double)np) * unit;
                  int qc = GroundLayout.indexAt(dc);
                  int piece = GroundLayout.PIECE[qc];
                  if(piece >= 0 && GroundLayout.PIECE[qc + 1] == piece) {
                     float share = (float)((head - dc) / (double)total);
                     int id = (int)(ku * 31L + (long)kv * 977L + (long)c * 7919L);
                     float t0 = 0.62F + 0.3F * Rng.hash(id, 5);
                     float dark = Easing.smoothstep(t0, t0 + 0.06F, share);
                     float drop = Easing.clamp01((share - t0 - 0.06F) / 0.12F);
                     if(drop < 1.0F) {
                        double fall = (double)(drop * drop) * 0.9D;
                        double tip = (double)drop * 0.9D * (double)(Rng.hash(id, 7) < 0.5F?-1:1);
                        float a;
                        if(this.place(np, var54, cv, tip, unit, hb, head, floor, piece, fall) && (a = alphaAt(dc) * (1.0F - drop)) > 0.01F) {
                           int col = this.palette[this.cells.color[c]];
                           if(tintedGlass1 && this.cells.color[c] == 1) {
                              col = this.colorAt(Easing.clamp01(share), Easing.clamp01(share), 0.0F, f.colorTime, 0.0F);
                           }

                           col = ColorUtil.shade(col, 1.0F - 0.75F * dark) | -16777216;
                           this.glass(np, ou, ov, ColorUtil.mulAlpha(col, a * opacityK));
                           this.keep(w, np, col, a, patches && drop <= 0.0F && cv * unit <= 0.4D, qc);
                           if(this.counting) {
                              ++this.drawnCells;
                              if(drop > 0.0F) {
                                 ++this.droppingCells;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private int clip(int c, double ou, double ov, double u0, double u1, double hu) {
      int n = Math.min(40, this.cells.count[c]);

      for(int k = 0; k < n; ++k) {
         this.pu[k] = this.cells.u[this.cells.start[c] + k] + ou;
         this.pv[k] = this.cells.v[this.cells.start[c] + k] + ov;
      }

      n = this.clipSide(n, 1.0D, 0.0D, -u0);
      n = this.clipSide(n, -1.0D, 0.0D, u1);
      n = this.clipSide(n, 0.0D, 1.0D, 0.0D);
      return this.clipSide(n, 0.0D, -1.0D, hu);
   }

   private int clipSide(int n, double a, double b, double d) {
      if(n < 3) {
         return n;
      } else {
         int m = 0;

         for(int k = 0; k < n; ++k) {
            int kn = (k + 1) % n;
            double e0 = a * this.pu[k] + b * this.pv[k] + d;
            double e1 = a * this.pu[kn] + b * this.pv[kn] + d;
            if(e0 >= 0.0D && m < 40) {
               this.qu[m] = this.pu[k];
               this.qv[m] = this.pv[k];
               ++m;
            }

            if(e0 < 0.0D != e1 < 0.0D && m < 40) {
               double t = e0 / (e0 - e1);
               this.qu[m] = this.pu[k] + (this.pu[kn] - this.pu[k]) * t;
               this.qv[m] = this.pv[k] + (this.pv[kn] - this.pv[k]) * t;
               ++m;
            }
         }

         System.arraycopy(this.qu, 0, this.pu, 0, m);
         System.arraycopy(this.qv, 0, this.pv, 0, m);
         return m;
      }
   }

   private static float alphaAt(double d) {
      int q = GroundLayout.indexAt(d);
      double b = GroundLayout.DIST[q + 1];
      double a;
      float t = b - (a = GroundLayout.DIST[q]) > 1.0E-9D?Easing.clamp01((d - a) / (b - a)):0.0F;
      return GroundLayout.ALPHA[q] + (GroundLayout.ALPHA[q + 1] - GroundLayout.ALPHA[q]) * t;
   }

   private boolean place(int np, double cu, double cv, double tip, double unit, double hb, double head, boolean floor, int piece, double fall) {
      double ct = Math.cos(tip);
      double st = Math.sin(tip);

      int n;
      for(int k = 0; k <= np; this.my[n] -= fall) {
         double u = k == np?cu:this.pu[k];
         double v = k == np?cv:this.pv[k];
         if(tip != 0.0D) {
            double du = u - cu;
            double dv = v - cv;
            u = cu + du * ct - dv * st;
            v = cv + du * st + dv * ct;
         }

         if(!this.map(u, v, unit, hb, head, floor, piece, k)) {
            return false;
         }

         n = k++;
      }

      return true;
   }

   private boolean map(double u, double v, double unit, double hb, double head, boolean floor, int piece, int i) {
      double d = u * unit;
      int q = GroundLayout.indexAt(d);
      if(GroundLayout.PIECE[q] == piece && GroundLayout.PIECE[q + 1] == piece) {
         double b = GroundLayout.DIST[q + 1];
         double a = GroundLayout.DIST[q];
         double t = b - a > 1.0E-9D?Math.max(0.0D, Math.min(1.0D, (d - a) / (b - a))):0.0D;
         double x = GroundLayout.X[q] + (GroundLayout.X[q + 1] - GroundLayout.X[q]) * t;
         double z = GroundLayout.Z[q] + (GroundLayout.Z[q + 1] - GroundLayout.Z[q]) * t;
         double ground = GroundLayout.GROUND[q] + (GroundLayout.GROUND[q + 1] - GroundLayout.GROUND[q]) * t;
         double h = v * unit;
         if(floor) {
            double sx = GroundLayout.SIDE_X[q] + (GroundLayout.SIDE_X[q + 1] - GroundLayout.SIDE_X[q]) * t;
            double sz = GroundLayout.SIDE_Z[q] + (GroundLayout.SIDE_Z[q + 1] - GroundLayout.SIDE_Z[q]) * t;
            double across = h - hb * 0.5D;
            this.mx[i] = x + sx * across;
            this.my[i] = ground + 0.012D;
            this.mz[i] = z + sz * across;
         } else {
            double rise = (double)Easing.smoothstep(0.0F, 0.6F, (float)(head - d));
            this.mx[i] = x;
            this.my[i] = ground + h - (1.0D - rise) * (hb + 0.05D);
            this.mz[i] = z;
         }

         return true;
      } else {
         return false;
      }
   }

   private void glass(int np, double ou, double ov, int col) {
      int tex = Glass.texture();
      double u0 = (this.pu[0] - ou) * 2.0D;
      double v0 = (this.pv[0] - ov) * 2.0D;

      for(int k = 1; k + 1 < np; k += 2) {
         int k2 = k + 1;
         int k3 = Math.min(np - 1, k + 2);
         Vfx.quadUV(tex, this.mx[0], this.my[0], this.mz[0], u0, v0, col, this.mx[k], this.my[k], this.mz[k], (this.pu[k] - ou) * 2.0D, (this.pv[k] - ov) * 2.0D, col, this.mx[k2], this.my[k2], this.mz[k2], (this.pu[k2] - ou) * 2.0D, (this.pv[k2] - ov) * 2.0D, col, this.mx[k3], this.my[k3], this.mz[k3], (this.pu[k3] - ou) * 2.0D, (this.pv[k3] - ov) * 2.0D, col);
      }

   }

   private void leads(StainedGlassStyle.Wall w, double hw, int leadCol) {
      double lift = w.floor?0.002D:0.0D;

      for(int c = 0; c < w.cells; ++c) {
         int at = w.start[c];
         int np = w.count[c];
         int col = ColorUtil.mulAlpha(leadCol, Math.min(1.0F, w.alpha[c] * 1.15F));
         double nx = w.floor?0.0D:w.nx[c];
         double ny = w.floor?1.0D:0.0D;
         double nz = w.floor?0.0D:w.nz[c];

         for(int k = 0; k < np; ++k) {
            int i2 = at + (k + 1) % np;
            int i = at + k;
            double ex = w.x[i2] - w.x[i];
            double ey = w.y[i2] - w.y[i];
            double ez = w.z[i2] - w.z[i];
            double el = Math.sqrt(ex * ex + ey * ey + ez * ez);
            double wz;
            double wy;
            double wx;
            double wl;
            if(el >= 1.0E-6D && (wl = Math.sqrt((wx = (ey = ey / el) * nz - (ez = ez / el) * ny) * wx + (wy = ez * nx - (ex = ex / el) * nz) * wy + (wz = ex * ny - ey * nx) * wz)) >= 1.0E-6D) {
               wx = wx * (hw / wl);
               wy = wy * (hw / wl);
               wz = wz * (hw / wl);
               double ax = w.x[i] - ex * hw;
               double ay = w.y[i] - ey * hw + lift;
               double az = w.z[i] - ez * hw;
               double bx = w.x[i2] + ex * hw;
               double by = w.y[i2] + ey * hw + lift;
               double bz = w.z[i2] + ez * hw;
               Vfx.quadUV(Textures.WHITE, ax - wx, ay - wy, az - wz, 0.5D, 0.5D, col, bx - wx, by - wy, bz - wz, 0.5D, 0.5D, col, bx + wx, by + wy, bz + wz, 0.5D, 0.5D, col, ax + wx, ay + wy, az + wz, 0.5D, 0.5D, col);
            }
         }
      }

   }

   private void keep(StainedGlassStyle.Wall w, int np, int col, float a, boolean lights, int q) {
      int c = w.add(np);
      int at = w.start[c];

      for(int k = 0; k <= np; ++k) {
         w.x[at + k] = this.mx[k];
         w.y[at + k] = this.my[k];
         w.z[at + k] = this.mz[k];
      }

      w.color[c] = col;
      w.alpha[c] = a;
      w.nx[c] = GroundLayout.SIDE_X[q];
      w.nz[c] = GroundLayout.SIDE_Z[q];
      if(lights) {
         w.gx[c] = this.mx[np];
         w.gy[c] = GroundLayout.GROUND[q] + 0.02D;
         w.gz[c] = this.mz[np];
         w.sx[c] = GroundLayout.SIDE_X[q];
         w.sz[c] = GroundLayout.SIDE_Z[q];
      } else {
         w.gy[c] = Double.NaN;
      }

   }

   private void light(StainedGlassStyle.Wall w) {
      float glowAmount = this.glow.getFloat() / 100.0F;
      if(glowAmount > 0.005F) {
         float k = glowAmount * (w.floor?0.55F:0.3F);

         for(int c = 0; c < w.cells; ++c) {
            int at = w.start[c];
            int np = w.count[c];
            int mid = at + np;
            float a = w.alpha[c] * k;
            if(a > 0.005F) {
               int col = w.color[c];
               if(w.even) {
                  int even = ColorUtil.mulAlpha(col, a * 0.7F);

                  for(int i = 1; i + 1 < np; i += 2) {
                     int i2 = i + 1;
                     int i3 = Math.min(np - 1, i + 2);
                     Vfx.quadUV(Textures.WHITE, w.x[at], w.y[at], w.z[at], 0.5D, 0.5D, even, w.x[at + i], w.y[at + i], w.z[at + i], 0.5D, 0.5D, even, w.x[at + i2], w.y[at + i2], w.z[at + i2], 0.5D, 0.5D, even, w.x[at + i3], w.y[at + i3], w.z[at + i3], 0.5D, 0.5D, even);
                  }
               } else {
                  int center = ColorUtil.mulAlpha(col, a);
                  int edge = ColorUtil.mulAlpha(col, a * 0.4F);

                  for(int i = 0; i < np; ++i) {
                     int i2 = (i + 1) % np;
                     Vfx.quadUV(Textures.WHITE, w.x[mid], w.y[mid], w.z[mid], 0.5D, 0.5D, center, w.x[at + i], w.y[at + i], w.z[at + i], 0.5D, 0.5D, edge, w.x[at + i2], w.y[at + i2], w.z[at + i2], 0.5D, 0.5D, edge, w.x[at + i2], w.y[at + i2], w.z[at + i2], 0.5D, 0.5D, edge);
                  }
               }
            }
         }

         for(int var14 = 0; var14 < w.cells; ++var14) {
            if(!Double.isNaN(w.gy[var14])) {
               int patch = ColorUtil.mulAlpha(w.color[var14], w.alpha[var14] * glowAmount * 0.3F);

               for(int side = -1; side <= 1; side += 2) {
                  Glass.patch(w.gx[var14] + w.sx[var14] * w.spread * (double)side, w.gy[var14], w.gz[var14] + w.sz[var14] * w.spread * (double)side, (float)w.size, patch);
               }
            }
         }
      }

   }

   private void motes(PlayerTrail p, TrailFrame f) {
      int due = this.piecesDue(p, 1.2D, 2);
      if(due != 0 && GroundLayout.n >= 2) {
         FxParticles pool = f.pieces(this.type);
         double hb = (double)this.height.getFloat();

         for(int i = 0; i < due && !this.module.piecesFull(f); ++i) {
            this.rng.seed((int)(f.now * 3917.0D) + i * 113 + p.entityId * 19);
            int q = (int)(this.rng.next() * (float)(GroundLayout.n - 1));
            if(!Double.isNaN(GroundLayout.GROUND[q])) {
               double side = (double)this.rng.signed() * 0.4D;
               double x = GroundLayout.X[q] + GroundLayout.SIDE_X[q] * side + f.viewX;
               double y = GroundLayout.GROUND[q] + (double)this.rng.next() * hb + f.viewY;
               double z = GroundLayout.Z[q] + GroundLayout.SIDE_Z[q] * side + f.viewZ;
               pool.spawn(3, x, y, z, this.rng.signed() * 0.05F, 0.08F + this.rng.next() * 0.08F, this.rng.signed() * 0.05F, 1.6F + this.rng.next(), 0.025F + this.rng.next() * 0.02F).alpha(0.7F * this.opacity.getFloat() / 100.0F * p.alpha).flags(2).color(this.palette[(int)(this.rng.next() * 4.0F) & 3]).wind(0.5F);
            }
         }
      }

   }

   public void renderPiecesPass(TrailFrame f, FxPass pass) {
      if(pass == FxPass.GLOW) {
         FxParticles pool = f.piecesIfAny(this.type);
         if(pool != null) {
            pool.render(f.viewX, f.viewY, f.viewZ);
         }
      }

   }

   public String stateForTest() {
      return String.format(Locale.ROOT, "%s, %s, height %.2f, cells %.0f%%, lead %.0f%%, glass %.0f%%, ground light %s, motes %s; drawn: %d cell(s), %d dropping out", new Object[]{((StainedGlassStyle.Variant)this.variant.get()).displayName(), ((Glass.GlassColors)this.glassColors.get()).displayName(), Float.valueOf(this.height.getFloat()), Float.valueOf(this.cellSize.getFloat()), Float.valueOf(this.leadWidth.getFloat()), Float.valueOf(this.glassOpacity.getFloat()), this.groundLight.get()?"on":"off", this.motes.get()?"on":"off", Integer.valueOf(this.drawnCells), Integer.valueOf(this.droppingCells)});
   }

   public static enum Variant implements Displayable {
      WALL("Wall", "A low wall of glass standing along your path"),
      FLOOR("Floor", "A band of glass lying on the ground, lit from below");

      private final String displayName;
      private final String description;

      private Variant(String displayName, String description) {
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

   private static final class Wall {
      double stamp;
      double seen;
      TrailFrame scene;
      int cells;
      int points;
      boolean floor;
      boolean even;
      int[] start;
      int[] count;
      int[] color;
      float[] alpha;
      double[] nx;
      double[] nz;
      double[] gx;
      double[] gy;
      double[] gz;
      double[] sx;
      double[] sz;
      double spread;
      double size;
      double[] x;
      double[] y;
      double[] z;

      private Wall() {
         this.stamp = -1.0D;
         this.start = new int[64];
         this.count = new int[64];
         this.color = new int[64];
         this.alpha = new float[64];
         this.nx = new double[64];
         this.nz = new double[64];
         this.gx = new double[64];
         this.gy = new double[64];
         this.gz = new double[64];
         this.sx = new double[64];
         this.sz = new double[64];
         this.x = new double[512];
         this.y = new double[512];
         this.z = new double[512];
      }

      int add(int n) {
         if(this.cells == this.start.length) {
            int cap = this.cells * 2;
            this.start = Arrays.copyOf(this.start, cap);
            this.count = Arrays.copyOf(this.count, cap);
            this.color = Arrays.copyOf(this.color, cap);
            this.alpha = Arrays.copyOf(this.alpha, cap);
            this.nx = Arrays.copyOf(this.nx, cap);
            this.nz = Arrays.copyOf(this.nz, cap);
            this.gx = Arrays.copyOf(this.gx, cap);
            this.gy = Arrays.copyOf(this.gy, cap);
            this.gz = Arrays.copyOf(this.gz, cap);
            this.sx = Arrays.copyOf(this.sx, cap);
            this.sz = Arrays.copyOf(this.sz, cap);
         }

         if(this.points + n + 1 > this.x.length) {
            int cap = Math.max(this.x.length * 2, this.points + n + 1);
            this.x = Arrays.copyOf(this.x, cap);
            this.y = Arrays.copyOf(this.y, cap);
            this.z = Arrays.copyOf(this.z, cap);
         }

         int c = this.cells++;
         this.start[c] = this.points;
         this.count[c] = n;
         this.points += n + 1;
         return c;
      }
   }
}
