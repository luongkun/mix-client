package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.kit.Chains;
import mix.client.mixui.integration.haunt.fx.kit.Surface;
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
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class GildedChainStyle extends TrailStyleBase {
   private static final double LINK = 0.2D;
   private static final float LOOSE_FROM = 0.8F;
   private static final float RIM = 0.25F;
   private static final int MAX_LINKS = 140;
   private static final double FULL_WITHIN = 3.5D;
   private static final double COARSE_WITHIN = 8.0D;
   private static final double TINY_WITHIN = 13.0D;
   private static final double OTHERS_COARSE_WITHIN = 3.0D;
   private static final double OTHERS_TINY_WITHIN = 5.0D;
   private static final double OTHERS_GLINT_WITHIN = 8.0D;
   public final ModeSetting<Material> material = this.materialSetting(Material.GOLD, "What the chain is made of. Custom takes the Tint, with a finish.");
   public final ModeSetting<Finish> finish;
   public final ModeSetting<GildedChainStyle.Variant> variant;
   public final SliderSetting linkSize;
   public final BooleanSetting loose;
   public final BooleanSetting glint;
   public final SliderSetting chainLength;
   private final GroundCache ground = new GroundCache();
   private final Map<PlayerTrail, GildedChainStyle.Drag> drags = new IdentityHashMap();
   private TrailFrame groundFrame;
   private final Surface floor = new Surface() {
      public double at(double x, double y, double z) {
         return GildedChainStyle.this.ground.surface(GildedChainStyle.this.groundFrame == null?null:GildedChainStyle.this.groundFrame.world, x, y + 0.5D, z, 1.0D, 3.0D);
      }
   };
   private final double[] at = new double[8];
   private int cursor;

   public GildedChainStyle(TrailsModule module) {
      super(module, TrailStyle.GILDED_CHAIN, spec().color("Tint", -1525686).signature(-3656).taper(0.0D).length(1.2D).anchor(TrailAnchor.WAIST, false));
      this.finish = this.finishSetting(Finish.METALLIC, this.material);
      this.color.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return GildedChainStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.doesNotApply(new Setting[]{this.size, this.glow});
      this.variant = (ModeSetting)this.setting((new ModeSetting("gilded_chain.variant", "Variant", "Along your path at the anchor\'s height, or hanging from it and dragging along the ground.", GildedChainStyle.Variant.class, GildedChainStyle.Variant.FLOATING)).style(ModeSetting.Style.CHIPS));
      this.linkSize = (SliderSetting)this.setting((new SliderSetting("gilded_chain.linkSize", "Link Size", "How big the links are.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%"));
      this.loose = (BooleanSetting)this.setting(new BooleanSetting("gilded_chain.loose", "Loose Links at the Tail", "The last links come loose one by one, drop and fade.", true));
      this.glint = (BooleanSetting)this.setting(new BooleanSetting("gilded_chain.glint", "Glint", "The links glint as they catch the light.", true));
      this.chainLength = (SliderSetting)this.setting((new SliderSetting("gilded_chain.chainLength", "Chain Length", "How long the dragging chain is.", 1.0D, 8.0D, 3.0D, 0.5D)).unit(" blocks").visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return GildedChainStyle.this.variant.get() == GildedChainStyle.Variant.DRAGGING;
         }
      }));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.linkSize);
   }

   public int passes() {
      int body = ((Material)this.material.get()).translucent((Finish)this.finish.get())?FxPass.TRANSLUCENT.bit():FxPass.SOLID.bit();
      return this.glint.get()?body | FxPass.GLOW.bit():body;
   }

   private double linkScale(float u) {
      float tp = this.taper == null?0.0F:this.taper.getFloat() / 100.0F;
      return 0.2D * (double)this.linkSize.getFloat() / 100.0D * (1.0D - (double)(tp * u) * 0.6D);
   }

   public void update(TrailFrame f) {
      super.update(f);
      if(!this.drags.isEmpty()) {
         Iterator<Entry<PlayerTrail, GildedChainStyle.Drag>> it = this.drags.entrySet().iterator();

         while(it.hasNext()) {
            GildedChainStyle.Drag d = (GildedChainStyle.Drag)((Entry)it.next()).getValue();
            if(d.scene == f && f.now - d.seen > 2.0D) {
               it.remove();
            }
         }
      }

   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      boolean dragging = this.variant.get() == GildedChainStyle.Variant.DRAGGING && !p.reduced && !p.ghost;
      if(pass != FxPass.GLOW) {
         Material m = this.materialFor((Material)this.material.get());
         int vc = m == Material.CUSTOM?(this.tint != 0?this.tint:this.color.getColor(0.5F)) | -16777216:-1;
         MeshBatch.begin(m, (Finish)this.finish.get(), this.matLight, 0.25F + 0.3F * this.night, -1, true, pass == FxPass.SOLID);

         try {
            if(dragging) {
               this.drag(p, f, vc, this.spawnPass(pass));
            } else {
               this.floating(p, f, vc);
            }
         } finally {
            MeshBatch.end();
         }
      } else {
         double gx = p.headX - f.eyeX;
         double gy = p.headY - f.eyeY;
         double gz = p.headZ - f.eyeZ;
         if(!p.reduced && (p.self || gx * gx + gy * gy + gz * gz < 64.0D)) {
            this.glints(p, f, dragging);
         }
      }

   }

   private boolean pointAt(TrailShape s, int k, double along, double[] out, boolean fromHead) {
      int from = s.runStart[k];
      int to = s.runEnd[k];
      if(fromHead || this.cursor > to - 1 || this.cursor <= from) {
         this.cursor = to - 1;
      }

      for(int i = this.cursor; i > from; --i) {
         double a0 = (double)s.along[i];
         double a1 = (double)s.along[i - 1];
         if(along >= a0 && along <= a1) {
            this.cursor = i;
            double t = a1 - a0 < 1.0E-9D?0.0D:(along - a0) / (a1 - a0);
            out[0] = s.x[i] + (s.x[i - 1] - s.x[i]) * t;
            out[1] = s.y[i] + (s.y[i - 1] - s.y[i]) * t;
            out[2] = s.z[i] + (s.z[i - 1] - s.z[i]) * t;
            double tx = s.x[i] - s.x[i - 1];
            double ty = s.y[i] - s.y[i - 1];
            double tz = s.z[i] - s.z[i - 1];
            double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
            if(tl < 1.0E-6D) {
               tx = 1.0D;
               ty = 0.0D;
               tz = 0.0D;
               tl = 1.0D;
            }

            out[3] = tx / tl;
            out[4] = ty / tl;
            out[5] = tz / tl;
            out[6] = (double)s.age[i] + (double)(s.age[i - 1] - s.age[i]) * t;
            return true;
         }
      }

      return false;
   }

   private void floating(PlayerTrail p, TrailFrame f, int vc) {
      TrailShape s = p.shape;
      if(s != null && s.runs != 0) {
         double full = p.self && !p.reduced?12.25D:-1.0D;
         double coarse = p.reduced?-1.0D:(p.self?64.0D:9.0D);
         double tiny = p.reduced?-1.0D:(p.self?169.0D:25.0D);
         boolean drops = this.loose.get() && !p.reduced;
         float total = Math.max(0.001F, s.length);
         float cap = this.maxLength == null?Float.MAX_VALUE:this.maxLength.getFloat();
         float op = this.opacity.getFloat() / 100.0F * p.alpha;
         double traveled = !p.ghost && p.path != null?p.path.traveled() + p.path.sinceLast(p.headX, p.headY, p.headZ):0.0D;
         double time = f.now;

         for(int k = 0; k < s.runs; ++k) {
            double head = (double)s.along[s.runEnd[k] - 1];
            double runEnd = (double)s.along[s.runStart[k]];
            double scale0 = this.linkScale(0.0F);
            double pitch = scale0 * 0.72D;
            double phase = traveled % pitch;
            int j = 0;

            for(double a = head + phase; a <= runEnd && j < 140; ++j) {
               if(this.pointAt(s, k, a, this.at, j == 0)) {
                  float u = (float)Math.min(1.0D, a / (double)total);
                  float alpha = op * smooth(0.0F, 0.25F, (float)a) * (1.0F - smooth(cap * 0.85F, cap, (float)a)) * f.nearFade(this.at[0], this.at[1], this.at[2]) * f.fog(this.at[0], this.at[1], this.at[2]);
                  double x = this.at[0];
                  double y = this.at[1];
                  double z = this.at[2];
                  double fx = this.at[3];
                  double fy = this.at[4];
                  double fz = this.at[5];
                  boolean turned = ((long)Math.floor((traveled - a) / pitch) & 1L) == 1L;
                  double spin = 0.0D;
                  if(u > 0.8F) {
                     float q = (u - 0.8F) / 0.19999999F;
                     alpha *= this.fade(q);
                     if(drops) {
                        y -= (double)(q * q) * 0.7D;
                        spin = (double)q * 1.4D * (double)(turned?-1:1);
                     }
                  }

                  if(alpha > 0.01F) {
                     double ex = x - f.viewX;
                     double ey = y - f.viewY;
                     double ez = z - f.viewZ;
                     double d22 = ex * ex + ey * ey + ez * ez;
                     this.link(f, x, y, z, fx, fy, fz, turned, spin, this.linkScale(u), ColorUtil.mulAlpha(vc, Math.min(1.0F, alpha)), this.glintAt(j, time), d22 < full?0:(d22 < coarse?1:(d22 < tiny?2:3)));
                  }
               }

               a += pitch;
            }
         }
      }

   }

   private float glintAt(int j, double time) {
      if(!this.glint.get()) {
         return 0.0F;
      } else {
         double v = time * 6.0D - (double)j * 0.35D;
         double run = v - 9.0D * (double)((long)(v / 9.0D));
         return run >= 0.0D && run <= 1.0D?(float)Math.sin(3.141592653589793D * run) * 0.9F:0.0F;
      }
   }

   private void link(TrailFrame f, double x, double y, double z, double fx, double fy, double fz, boolean turned, double spin, double scale, int col, float shine, int detail) {
      double wx = -fz;
      double wy = 0.0D;
      double wz = fx;
      double wl = Math.sqrt(wx * wx + fx * fx);
      if(wl < 1.0E-6D) {
         wx = 1.0D;
         wz = 0.0D;
         wl = 1.0D;
      }

      double var53;
      double ux = fy * (var53 = wz / wl) - fz * wy;
      double var52;
      double uy = fz * (var52 = wx / wl) - fx * var53;
      double uz = fx * wy - fy * var52;
      double sn;
      double c;
      if(spin == 0.0D) {
         c = turned?0.0D:1.0D;
         sn = turned?1.0D:0.0D;
      } else {
         double ang = (turned?1.5707963267948966D:0.0D) + spin;
         c = Math.cos(ang);
         sn = Math.sin(ang);
      }

      double bx = var52 * c + ux * sn;
      double by = wy * c + uy * sn;
      double bz = var53 * c + uz * sn;
      double qx = ux * c - var52 * sn;
      double qy = uy * c - wy * sn;
      double qz = uz * c - var53 * sn;
      Chains.linkDetail(x - f.viewX, y - f.viewY, z - f.viewZ, fx, fy, fz, bx, by, bz, qx, qy, qz, scale, col, shine, detail);
   }

   private void drag(PlayerTrail p, TrailFrame f, int vc, boolean step) {
      GildedChainStyle.Drag d = (GildedChainStyle.Drag)this.drags.get(p);
      double scale = this.linkScale(0.0F);
      double rest = scale * 0.72D;
      int n = Math.max(3, Math.min(40, (int)Math.round((double)this.chainLength.getFloat() / rest)));
      if(d == null) {
         d = new GildedChainStyle.Drag();
         this.drags.put(p, d);
      }

      if(!d.ready || d.rope.count() != n || Math.abs(d.rope.rest() - rest) > 1.0E-6D) {
         d.rope.reset(n, rest, p.headX, p.headY, p.headZ, 0.0D, -1.0D, 0.0D);
         d.ready = true;
      }

      d.seen = f.now;
      d.scene = f;
      Chains.Rope r = d.rope;
      if(step && f.dt > 0.0F) {
         this.groundFrame = f;
         r.pin(0, true, p.headX, p.headY, p.headZ);
         r.step(Math.min(0.05D, (double)f.dt), this.floor);
      }

      float op = this.opacity.getFloat() / 100.0F * p.alpha;
      double full = p.self?12.25D:-1.0D;
      double coarse = p.self?64.0D:9.0D;
      double time = f.now;

      for(int i = 0; i + 1 < r.count(); ++i) {
         double x0 = r.x(i);
         double y0 = r.y(i);
         double z0 = r.z(i);
         double x1 = r.x(i + 1);
         double y1 = r.y(i + 1);
         double z1 = r.z(i + 1);
         float alpha = op * f.nearFade(x0, y0, z0) * f.fog(x0, y0, z0);
         if(alpha > 0.01F) {
            double ex = x0 - f.viewX;
            double ey = y0 - f.viewY;
            double ez = z0 - f.viewZ;
            double d2 = ex * ex + ey * ey + ez * ez;
            Chains.linkBetween(ex, ey, ez, x1 - f.viewX, y1 - f.viewY, z1 - f.viewZ, 0.0D, 1.0D, 0.0D, (i & 1) == 1, scale, ColorUtil.mulAlpha(vc, Math.min(1.0F, alpha)), this.glintAt(i, time), d2 < full?0:(d2 < coarse?1:2));
         }
      }

   }

   private void glints(PlayerTrail p, TrailFrame f, boolean dragging) {
      float op = this.opacity.getFloat() / 100.0F * p.alpha;
      double time = f.now;
      RenderState.additive();
      if(dragging) {
         GildedChainStyle.Drag d = (GildedChainStyle.Drag)this.drags.get(p);
         if(d != null && d.ready) {
            for(int i = 0; i < d.rope.count(); ++i) {
               float g = this.glintAt(i, time);
               if(g > 0.4F) {
                  this.sparkle(f, d.rope.x(i), d.rope.y(i), d.rope.z(i), g * op, i);
               }
            }
         }
      } else {
         TrailShape s = p.shape;
         if(s != null && s.runs != 0) {
            double traveled = !p.ghost && p.path != null?p.path.traveled() + p.path.sinceLast(p.headX, p.headY, p.headZ):0.0D;
            double pitch = this.linkScale(0.0F) * 0.72D;
            float total = Math.max(0.001F, s.length);

            for(int k = 0; k < s.runs; ++k) {
               double head = (double)s.along[s.runEnd[k] - 1];
               double runEnd = (double)s.along[s.runStart[k]];
               int j = 0;
               boolean first = true;

               for(double a = head + traveled % pitch; a <= runEnd && j < 140; ++j) {
                  float g = this.glintAt(j, time);
                  if(g > 0.4F && this.pointAt(s, k, a, this.at, first)) {
                     first = false;
                     float u = (float)Math.min(1.0D, a / (double)total);
                     this.sparkle(f, this.at[0], this.at[1], this.at[2], g * op * this.fade((float)(this.at[6] / (double)Math.max(0.05F, p.maxAge))) * (1.0F - u), j);
                  }

                  a += pitch;
               }
            }
         }
      }

   }

   private void sparkle(TrailFrame f, double x, double y, double z, float a, int i) {
      float k = a * f.nearFade(x, y, z) * f.fog(x, y, z);
      if(k > 0.01F) {
         double size = this.linkScale(0.0F) * 1.1D;
         Vfx.sprite(Textures.SPARKLE, x - f.viewX, y - f.viewY, z - f.viewZ, size, (float)((double)i * 0.7D + f.now * 2.0D), ColorUtil.mulAlpha(-2858, Math.min(1.0F, k)));
      }

   }

   public String stateForTest() {
      return String.format(Locale.ROOT, "%s, %s, link %.0f%%, loose %s, glint %s, %d dragging chain(s)", new Object[]{((GildedChainStyle.Variant)this.variant.get()).displayName(), ((Material)this.material.get()).displayName(), Float.valueOf(this.linkSize.getFloat()), this.loose.get()?"on":"off", this.glint.get()?"on":"off", Integer.valueOf(this.drags.size())});
   }

   private static final class Drag {
      final Chains.Rope rope;
      double seen;
      TrailFrame scene;
      boolean ready;

      private Drag() {
         this.rope = new Chains.Rope();
      }
   }

   public static enum Variant implements Displayable {
      FLOATING("Floating", "Along your path at the anchor\'s height"),
      DRAGGING("Dragging", "Hanging from the anchor, dragging along the ground");

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
}
