package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Procedural;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class EnergyFissuresStyle extends LandingStyleBase {
   private static final int MAX_POINTS = 17;
   private static final int BRANCH_POINTS = 9;
   public final SliderSetting cracks = (SliderSetting)this.setting(new SliderSetting("fissures.cracks", "Cracks", "How many cracks split out.", 3.0D, 10.0D, 6.0D, 1.0D));
   public final SliderSetting length = (SliderSetting)this.setting((new SliderSetting("fissures.length", "Length", "How long the cracks are.", 0.8D, 3.0D, 1.6D, 0.05D)).unit(" blocks"));
   private final Rng rng = new Rng();
   private final double[] cx = new double[3];
   private final double[] cy = new double[3];
   private final double[] cz = new double[3];

   public EnergyFissuresStyle(LandingImpactModule module) {
      super(module, LandingStyle.ENERGY_FISSURES, 1.4F, 2.6F, new ColorSetting("fissures.color", "Color", "Crack glow (gradient: centre to tip).", -38357));
   }

   public void start(LandingFx fx) {
      this.rng.seed(fx.seed);
      float k = fx.land.fallScale;
      int main = Math.max(2, Math.round(this.cracks.getFloat() * (0.8F + 0.35F * k)));
      EnergyFissuresStyle.Web web = new EnergyFissuresStyle.Web();
      web.x = new float[main * 3][];
      web.z = new float[main * 3][];
      web.n = new int[main * 3];
      web.branch = new boolean[main * 3];

      for(int i = 0; i < main; ++i) {
         double ang = 6.283185307179586D * ((double)i + (double)this.rng.signed() * 0.35D) / (double)main;
         double len = this.length.get() * (double)fx.size * (double)this.rng.range(0.7F, 1.15F);
         this.cx[0] = fx.x() + Math.cos(ang) * 0.12D;
         this.cz[0] = fx.z() + Math.sin(ang) * 0.12D;
         this.cx[1] = fx.x() + Math.cos(ang + (double)this.rng.signed() * 0.25D) * len * 0.55D;
         this.cz[1] = fx.z() + Math.sin(ang + (double)this.rng.signed() * 0.25D) * len * 0.55D;
         this.cx[2] = fx.x() + Math.cos(ang + (double)this.rng.signed() * 0.3D) * len;
         this.cz[2] = fx.z() + Math.sin(ang + (double)this.rng.signed() * 0.3D) * len;
         int n = Procedural.jagged(this.rng, 0, this.cx, this.cy, this.cz, 3, 3, 0.2D, true);
         store(web, n, false);
         int branches = this.rng.nextInt(3);

         for(int b = 0; b < branches && web.count < web.n.length; ++b) {
            int from = 4 + this.rng.nextInt(Math.max(1, n - 8));
            float[] px = web.x[web.count - 1 - b];
            float[] pz = web.z[web.count - 1 - b];
            double bx = (double)px[Math.min(from, px.length - 1)];
            double bz = (double)pz[Math.min(from, pz.length - 1)];
            double bang = ang + (double)((float)(this.rng.next() < 0.5F?-1:1) * this.rng.range(0.5F, 1.0F));
            double blen = len * (double)this.rng.range(0.25F, 0.45F);
            int bn = Procedural.bolt(this.rng, 0, bx, 0.0D, bz, bx + Math.cos(bang) * blen, 0.0D, bz + Math.sin(bang) * blen, 3, 0.25D);
            store(web, bn, true);
         }
      }

      fx.data = web;
      if(!fx.lod) {
         int embers = fx.count(10.0F * (0.8F + 0.4F * k));

         for(int i = 0; i < embers; ++i) {
            int c = this.rng.nextInt(web.count);
            int idx = this.rng.nextInt(web.n[c]);
            double ex = (double)web.x[c][idx];
            double ez = (double)web.z[c][idx];
            fx.particles.spawn(7, ex, fx.ground.at(ex, ez) + 0.05D, ez, this.rng.signed() * 0.6F, this.rng.range(1.6F, 3.2F), this.rng.signed() * 0.6F, fx.seconds(this.rng.range(0.4F, 0.65F)), this.rng.range(0.04F, 0.065F)).gravity(8.0F).drag(0.8F).color(this.color, (float)idx / (float)web.n[c]).alpha(fx.opacity).delay(fx.life * this.rng.range(0.14F, 0.65F)).ground(fx.y() + 0.03D, 1, 0.3F);
         }
      }

   }

   private static void store(EnergyFissuresStyle.Web web, int n, boolean branch) {
      int n2 = Math.min(n, 17);
      float[] x = new float[n2];
      float[] z = new float[n2];

      for(int i = 0; i < n2; ++i) {
         x[i] = (float)Vfx.X[i];
         z[i] = (float)Vfx.Z[i];
      }

      web.x[web.count] = x;
      web.z[web.count] = z;
      web.n[web.count] = n2;
      web.branch[web.count] = branch;
      ++web.count;
   }

   public void render(LandingFx fx, FxFrame f) {
      if(fx.data instanceof EnergyFissuresStyle.Web) {
         EnergyFissuresStyle.Web web = (EnergyFissuresStyle.Web)fx.data;
         float p = fx.progress();
         float spread = Easing.outQuad(Easing.clamp01(p / 0.11F));
         float pulse = (float)Math.exp(-Math.pow((double)((p - 0.16F) / 0.06F), 2.0D));
         float cool = Easing.smoothstep(0.22F, 0.9F, p);
         float a = fx.alpha() * (1.0F - Easing.smoothstep(0.62F, 1.0F, p));
         if(a > 0.003F && spread > 0.0F) {
            RenderState.decal(true);
            int crater = this.color.getColor(0.0F);
            RenderState.translucent(false);
            Ground.disc(f, fx.ground, Textures.GLOW, fx.x(), fx.z(), 0.8D * (double)fx.size, 0.0D, alpha(ColorUtil.lerp(crater, ColorUtil.shade(crater, 0.3F), cool), 0.35F * a), 3, 20);
            RenderState.additive();
            Ground.disc(f, fx.ground, Textures.GLOW, fx.x(), fx.z(), 0.85D * (double)fx.size, 0.0D, alpha(crater, (0.25F + 0.4F * pulse) * a * (1.0F - cool)), 3, 20);
            int offset = 0;

            for(int c = 0; c < web.count; ++c) {
               int n = web.n[c];
               int shown = Math.max(2, Math.round((float)n * spread));
               float taperBase = web.branch[c]?0.6F:1.0F;

               for(int i = 0; i < shown; ++i) {
                  Vfx.X[offset + i] = (double)web.x[c][i];
                  Vfx.Z[offset + i] = (double)web.z[c][i];
                  float s = (float)i / (float)(n - 1);
                  int col = this.color.getColor(s);
                  col = ColorUtil.lerp(ColorUtil.lerp(col, Neon.tint(col), pulse * 0.6F), ColorUtil.shade(col, 0.3F), cool);
                  Vfx.W[offset + i] = (float)(0.1D * Math.sqrt((double)fx.size) * (double)taperBase * (double)(1.0F - 0.7F * s) * (double)(1.0F + 0.3F * pulse));
                  Vfx.C[offset + i] = alpha(col, a);
               }

               Ground.toRender(f, fx.ground, offset, shown);
               Neon.addFlat(offset, shown);
               offset += shown;
            }

            Neon.flush(1.0F + pulse, 1.0F, 1.0F - 0.6F * cool);
            RenderState.decal(false);
         }
      }

   }

   private static final class Web {
      int count;
      float[][] x;
      float[][] z;
      int[] n;
      boolean[] branch;

      private Web() {
      }
   }
}
