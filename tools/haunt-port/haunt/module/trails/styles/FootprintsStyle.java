package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.Terrain;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.module.particles.LibraryImageSetting;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Distortion;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;

public final class FootprintsStyle extends TrailStyleBase {
   public final ModeSetting<FootprintsStyle.Variant> variant = (ModeSetting)this.setting(new ModeSetting("footprints.variant", "Variant", "How the prints look.", FootprintsStyle.Variant.class, FootprintsStyle.Variant.PRINTS));
   public final ModeSetting<FootprintsStyle.Shape> shape = (ModeSetting)this.setting((new ModeSetting("footprints.shape", "Print Shape", "The shape of each print.", FootprintsStyle.Shape.class, FootprintsStyle.Shape.SHOE)).style(ModeSetting.Style.CHIPS));
   public final LibraryImageSetting image = (LibraryImageSetting)this.setting(new LibraryImageSetting("footprints.image", "Print Image", "An image from the Particles section\'s library (a missing file falls back to Shoe)."));
   public final SliderSetting stride;
   public final SliderSetting popIn;
   private final Rng rng = new Rng();
   private final double[] point = new double[3];
   private static final int WARP_SEGMENTS = 18;

   public FootprintsStyle(TrailsModule module) {
      super(module, TrailStyle.FOOTPRINTS, spec().color("Prints", -8395777).size(0.1D, 0.6D, 0.35D, 0.01D).length(3.0D).anchor(TrailAnchor.FEET, true).fade(TrailStyleBase.FadeCurve.LATE));
      this.image.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return FootprintsStyle.this.shape.get() == FootprintsStyle.Shape.CUSTOM;
         }
      });
      this.stride = (SliderSetting)this.setting((new SliderSetting("footprints.stride", "Stride Spacing", "How far apart the prints are (100%: every step).", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%"));
      this.popIn = (SliderSetting)this.setting((new SliderSetting("footprints.popIn", "Pop-in", "The soft flash and bounce of a new print.", 0.0D, 100.0D, 60.0D, 1.0D)).unit("%"));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.popIn);
   }

   protected TrailStyleBase.Store newStore() {
      return new FootprintsStyle.Prints();
   }

   public void render(PlayerTrail p, TrailFrame f) {
      if(!p.ghost) {
         PlayerTrail.StyleState st = p.style;
         double cycle = 4.715689963359041D * (double)this.stride.getFloat() / 100.0D;
         long step = (long)Math.floor((double)p.pose.limbSwing / cycle);
         if(st.vars[1] == 0.0D) {
            st.vars[0] = (double)step;
            st.vars[1] = 1.0D;
         } else if(step != (long)st.vars[0]) {
            st.vars[0] = (double)step;
            if(p.growing && p.pose.limbAmount >= 0.1F && !this.module.piecesFull(f)) {
               if(this.density.getFloat() < 100.0F) {
                  this.rng.seed((int)step * 31 + p.entityId);
                  if(this.rng.next() > this.density.getFloat() / 100.0F) {
                     return;
                  }
               }

               if((p.state & 1) != 0 && (p.state & 8) == 0 && (p.entity == null || !p.entity.isInLava() && !p.entity.isOnLadder())) {
                  boolean leftFoot = (step & 1L) == 0L;
                  TrailAnchor.BOTH_FEET.place(p.pose, leftFoot?0:1, this.point);
                  double g = f.world == null?0.0D:Terrain.surfaceAt(f.world, this.point[0], this.point[2], p.pose.y + 0.3D, 0.35D, 1.2D);
                  if(!Double.isNaN(g) && Math.abs(g - p.pose.y) <= 0.6D) {
                     FootprintsStyle.Prints prints = (FootprintsStyle.Prints)this.store(f);
                     int i = prints.add();
                     double yaw = Math.toRadians((double)p.pose.bodyYaw);
                     prints.x[i] = this.point[0];
                     prints.y[i] = g + 0.01D;
                     prints.z[i] = this.point[2];
                     prints.fx[i] = (float)(-Math.sin(yaw));
                     prints.fz[i] = (float)Math.cos(yaw);
                     prints.size[i] = this.size.getFloat() * (1.0F + 0.35F * this.speedBoost(p.speed)) * p.widthScale;
                     prints.born[i] = prints.clock;
                     prints.life[i] = this.lengthNow(p.speed);
                     prints.alpha[i] = Math.min(1.0F, p.alpha * (1.0F + 0.3F * Math.max(0.0F, this.speedBoost(p.speed))));
                     prints.speed[i] = p.speed;
                     prints.left[i] = leftFoot;
                     prints.lite[i] = p.reduced;
                     prints.seed[i] = (int)(step * 7919L + (long)p.entityId);
                     prints.tint[i] = this.tint;
                  }
               }
            }
         }
      }

   }

   protected void drawStore(TrailFrame f, TrailStyleBase.Store store) {
      FootprintsStyle.Prints pr = (FootprintsStyle.Prints)store;
      Textures.ensureTrailsLoaded();
      int tex = this.texture();
      FootprintsStyle.Variant v = (FootprintsStyle.Variant)this.variant.get();
      float op = this.opacity.getFloat() / 100.0F;
      float g = this.glow.getFloat() / 100.0F;
      float pop = this.popIn.getFloat() / 100.0F;
      boolean warp = v == FootprintsStyle.Variant.RIPPLES && !f.preview && Distortion.refracts();
      RenderState.decal(true);
      if(warp) {
         this.warpRings(f, pr, op);
      }

      for(int pass = 0; pass < 2; ++pass) {
         if(pass == 0) {
            RenderState.translucent(false);
         } else {
            RenderState.additive();
         }

         for(int i = 0; i < pr.n; ++i) {
            float age = pr.clock - pr.born[i];
            float u = Math.min(1.0F, age / Math.max(0.05F, pr.life[i]));
            double wx = pr.x[i];
            double wy = pr.y[i];
            double wz = pr.z[i];
            float a = op * pr.alpha[i] * this.fade(u) * f.fog(wx, wy, wz) * f.nearFade(wx, wy, wz);
            if(a > 0.004F) {
               int c = lit(pr.tint[i] != 0?pr.tint[i]:this.colorAt(Rng.hash(pr.seed[i], 11), u, pr.speed[i], f.colorTime, 0.0F), f, wx, wy + 0.3D, wz);
               float s = pr.size[i];
               float flash = 0.0F;
               if(pop > 0.0F) {
                  float t = Easing.clamp01(age / 0.18F);
                  s *= 1.0F + pop * 0.35F * (1.0F - Easing.outBack(t, 2.4F));
                  flash = pop * (1.0F - Easing.clamp01(age / 0.22F));
               }

               double x = wx - f.viewX;
               double y = wy - f.viewY;
               double z = wz - f.viewZ;
               switch(v) {
               case FROST:
                  frost(pass, tex, pr, i, x, y, z, s * (1.0F + 0.15F * u), c, a * (float)Math.pow((double)(1.0F - u), 0.4D), g, flash);
                  break;
               case RIPPLES:
                  ripples(pass, tex, pr, i, x, y, z, s, c, a, u, g, warp && !pr.lite[i]);
                  break;
               case SCORCH:
                  scorch(pass, tex, pr, i, x, y, z, s, c, a, u, g);
                  break;
               default:
                  if(pass == 0) {
                     print(tex, pr, i, x, y, z, s, c, a * 0.8F);
                  } else {
                     Vfx.flatSprite(Textures.GLOW, x, y + 0.002D, z, (double)(s * (0.9F + 0.9F * g)), 0.0D, ColorUtil.mulAlpha(c, a * 0.5F * g));
                     print(tex, pr, i, x, y + 0.003D, z, s, c, a * 0.55F);
                     if(flash > 0.01F) {
                        print(tex, pr, i, x, y + 0.004D, z, s * 1.05F, ColorUtil.core(c), flash * a);
                     }
                  }
               }
            }
         }
      }

      RenderState.decal(false);
      RenderState.additive();
   }

   public String printsForTest(TrailFrame f) {
      FootprintsStyle.Prints pr = (FootprintsStyle.Prints)f.store(this.type);
      if(pr != null && f.world != null) {
         int onSurface = 0;
         int overEdge = 0;
         TreeSet<String> heights = new TreeSet();

         for(int i = 0; i < pr.n; ++i) {
            double y = pr.y[i] - 0.01D;
            double s = Terrain.surfaceAt(f.world, pr.x[i], pr.z[i], y + 0.3D, 0.35D, 1.2D);
            if(!Double.isNaN(s) && Math.abs(s - y) < 0.005D) {
               ++onSurface;
            }

            Material m;
            if((m = f.world.getBlockState(new BlockPos(pr.x[i], y - 0.02D, pr.z[i])).getBlock().getMaterial()) == Material.air || m.isLiquid()) {
               ++overEdge;
            }

            heights.add(String.format(Locale.ROOT, "%.3f", new Object[]{Double.valueOf(y - Math.floor(y + 1.0E-4D))}));
         }

         return pr.n + " prints, " + onSurface + " exactly on the surface under their middle, " + overEdge + " over air or liquid; heights " + heights;
      } else {
         return "no prints";
      }
   }

   private int texture() {
      switch((FootprintsStyle.Shape)this.shape.get()) {
      case BARE:
         return Textures.BARE_PRINT;
      case PAW:
         return Textures.PAW_PRINT;
      case STAR:
         return Textures.STAR;
      case HEART:
         return Textures.HEART;
      case HEX:
         return Textures.HEX_FILL;
      case DOT:
         return Textures.GLOW;
      case CUSTOM:
         int t = this.image.texture();
         if(t <= 0) {
            this.image.warnIfMissing("Trails › Footprints", "the shoe print shows instead");
         }

         return t > 0?t:Textures.SHOE_PRINT;
      default:
         return Textures.SHOE_PRINT;
      }
   }

   private static void print(int tex, FootprintsStyle.Prints pr, int i, double x, double y, double z, float s, int c, float a) {
      if(a > 0.003F) {
         double fx = (double)pr.fx[i];
         double fz = (double)pr.fz[i];
         double rx = -fz;
         double half = (double)s * 0.5D;
         double side = pr.left[i]?-half:half;
         Vfx.flatQuad(tex, x, y, z, rx * side, fx * side, -fx * half, -fz * half, 0.0F, 0.0F, 1.0F, 1.0F, ColorUtil.mulAlpha(c, a));
      }

   }

   private static void frost(int pass, int tex, FootprintsStyle.Prints pr, int i, double x, double y, double z, float s, int c, float a, float g, float flash) {
      int ice = ColorUtil.lerp(c, -1640705, 0.6F);
      if(pass == 0) {
         print(tex, pr, i, x, y, z, s, ice, a * 0.7F);
      } else {
         float rot = Rng.hash(pr.seed[i], 3) * 6.283F;
         Vfx.flatSprite(Textures.SNOWFLAKE, x, y + 0.002D, z, (double)(s * 0.55F), (double)rot, ColorUtil.mulAlpha(ice, a * 0.55F));
         print(tex, pr, i, x, y + 0.003D, z, s, ice, a * 0.6F);
         Vfx.flatSprite(Textures.GLOW, x, y + 0.001D, z, (double)(s * (0.8F + 0.8F * g)), 0.0D, ColorUtil.mulAlpha(c, a * 0.3F * g));
         if(flash > 0.01F) {
            print(tex, pr, i, x, y + 0.004D, z, s * 1.05F, -1, flash * a * 0.8F);
         }
      }

   }

   private static float ringT(float u, int ring) {
      return u * 1.6F - (float)ring * 0.25F;
   }

   private static double ringRadius(float s, float t) {
      return (double)s * (0.25D + 1.4D * (double)Easing.outCubic(t));
   }

   private static void ripples(int pass, int tex, FootprintsStyle.Prints pr, int i, double x, double y, double z, float s, int c, float a, float u, float g, boolean warp) {
      if(pass == 0) {
         print(tex, pr, i, x, y, z, s * 0.9F, c, a * 0.45F * (1.0F - u));
      } else {
         for(int ring = 0; ring < 2; ++ring) {
            float t = ringT(u, ring);
            if(t > 0.0F && t < 1.0F) {
               double radius = ringRadius(s, t);
               float ra = a * (1.0F - t) * (1.0F - t);
               if(!warp) {
                  Vfx.flatRing(Textures.LINE_GLOW, x, y + 0.002D + (double)ring * 5.0E-4D, z, radius, (double)s * (0.12D + 0.12D * (double)g), 40, ColorUtil.mulAlpha(c, ra));
               }

               Vfx.flatRing(Textures.LINE_CORE, x, y + 0.003D + (double)ring * 5.0E-4D, z, radius, (double)s * 0.035D, 40, ColorUtil.mulAlpha(ColorUtil.core(c), ra * (warp?0.5F:0.8F)));
            }
         }

         print(tex, pr, i, x, y + 0.001D, z, s * 0.9F, c, a * 0.35F * (1.0F - u));
      }

   }

   private void warpRings(TrailFrame f, FootprintsStyle.Prints pr, float op) {
      Distortion.begin(FxFrame.currentFrame(), 0.06F, this.color.getColor(0.5F, f.colorTime), 0.25F, 1.0F, true);

      for(int i = 0; i < pr.n && Distortion.room() >= 72; ++i) {
         if(!pr.lite[i]) {
            float age = pr.clock - pr.born[i];
            float u = Math.min(1.0F, age / Math.max(0.05F, pr.life[i]));
            double wx = pr.x[i];
            double wy = pr.y[i];
            double wz = pr.z[i];
            float a = op * pr.alpha[i] * this.fade(u) * f.fog(wx, wy, wz) * f.nearFade(wx, wy, wz);

            for(int ring = 0; ring < 2; ++ring) {
               float t = ringT(u, ring);
               if(t > 0.0F && t < 1.0F && a > 0.01F) {
                  float strength = Math.min(1.0F, a * (1.0F - t) * (1.0F - t) * 1.4F);
                  double r = ringRadius(pr.size[i], t);
                  double w = (double)pr.size[i] * 0.22D;
                  double rIn = Math.max(0.0D, r - w);
                  double rOut = r + w * 0.4D;
                  double x = wx - f.viewX;
                  double y = wy - f.viewY + 0.002D;
                  double z = wz - f.viewZ;

                  for(int k = 0; k < 18; ++k) {
                     double a0 = 6.283185307179586D * (double)k / 18.0D;
                     double a1 = 6.283185307179586D * (double)(k + 1) / 18.0D;
                     warpVertex(x, y, z, rIn, a0, -0.55D, strength);
                     warpVertex(x, y, z, rOut, a0, 0.7D, strength);
                     warpVertex(x, y, z, rOut, a1, 0.7D, strength);
                     warpVertex(x, y, z, rIn, a1, -0.55D, strength);
                  }
               }
            }
         }
      }

      Distortion.end();
   }

   private static void warpVertex(double x, double y, double z, double r, double ang, double tilt, float strength) {
      double c = Math.cos(ang);
      double s = Math.sin(ang);
      double nx = c * tilt;
      double nz = s * tilt;
      double l = Math.sqrt(nx * nx + 1.0D + nz * nz);
      Distortion.vertex(x + c * r, y, z + s * r, nx / l, 1.0D / l, nz / l, strength);
   }

   private static void scorch(int pass, int tex, FootprintsStyle.Prints pr, int i, double x, double y, double z, float s, int c, float a, float u, float g) {
      float heat = 1.0F - Easing.smoothstep(0.1F, 0.8F, u);
      int hot = u < 0.12F?ColorUtil.lerp(ColorUtil.core(c), c, u / 0.12F):ColorUtil.shade(c, 0.4F + 0.6F * heat);
      if(pass == 0) {
         print(tex, pr, i, x, y, z, s, ColorUtil.lerp(-14018030, hot, heat), a * (0.35F + 0.35F * (1.0F - heat)));
      } else {
         Vfx.flatSprite(Textures.GLOW, x, y + 0.002D, z, (double)(s * (0.9F + 0.9F * g)), 0.0D, ColorUtil.mulAlpha(hot, a * 0.5F * g * heat));
         print(tex, pr, i, x, y + 0.003D, z, s, hot, a * 0.9F * heat);
      }

   }

   static final class Prints implements TrailStyleBase.Store {
      static final int CAP = 256;
      final double[] x = new double[256];
      final double[] y = new double[256];
      final double[] z = new double[256];
      final float[] fx = new float[256];
      final float[] fz = new float[256];
      final float[] size = new float[256];
      final float[] born = new float[256];
      final float[] life = new float[256];
      final float[] alpha = new float[256];
      final float[] speed = new float[256];
      final boolean[] left = new boolean[256];
      final boolean[] lite = new boolean[256];
      final int[] seed = new int[256];
      final int[] tint = new int[256];
      int n;
      int oldest;
      float clock;

      int add() {
         int i;
         if(this.n < 256) {
            i = this.n++;
         } else {
            i = this.oldest;
            this.oldest = (this.oldest + 1) % 256;
         }

         return i;
      }

      public void update(float dt) {
         this.clock += dt;
         int w = 0;

         for(int i = 0; i < this.n; ++i) {
            if(this.clock - this.born[i] < this.life[i]) {
               if(w != i) {
                  this.copy(i, w);
               }

               ++w;
            }
         }

         this.n = w;
         this.oldest = 0;
      }

      private void copy(int from, int to) {
         this.x[to] = this.x[from];
         this.y[to] = this.y[from];
         this.z[to] = this.z[from];
         this.fx[to] = this.fx[from];
         this.fz[to] = this.fz[from];
         this.size[to] = this.size[from];
         this.born[to] = this.born[from];
         this.life[to] = this.life[from];
         this.alpha[to] = this.alpha[from];
         this.speed[to] = this.speed[from];
         this.left[to] = this.left[from];
         this.lite[to] = this.lite[from];
         this.seed[to] = this.seed[from];
         this.tint[to] = this.tint[from];
      }

      public int size() {
         return this.n;
      }

      public void clear() {
         this.n = 0;
         this.oldest = 0;
      }
   }

   public static enum Shape implements Displayable {
      SHOE("Shoe", "A shoe\'s sole"),
      BARE("Bare foot", "A bare foot with its toes"),
      PAW("Paw", "A paw print"),
      STAR("Star", "A star"),
      HEART("Heart", "A heart"),
      HEX("Hex", "A hexagon"),
      DOT("Dot", "A round dot"),
      CUSTOM("Custom image", "An image from the Particles section\'s library");

      private final String name;
      private final String description;

      private Shape(String name, String description) {
         this.name = name;
         this.description = description;
      }

      public String displayName() {
         return this.name;
      }

      public String description() {
         return this.description;
      }
   }

   public static enum Variant implements Displayable {
      PRINTS("Prints", "Glowing prints"),
      FROST("Frost", "An icy patch with crystal edges that grows a little, then melts"),
      RIPPLES("Ripples", "Water-like rings spreading from each step"),
      SCORCH("Scorch", "Hot glowing prints that cool down as they age");

      private final String name;
      private final String description;

      private Variant(String name, String description) {
         this.name = name;
         this.description = description;
      }

      public String displayName() {
         return this.name;
      }

      public String description() {
         return this.description;
      }
   }
}
