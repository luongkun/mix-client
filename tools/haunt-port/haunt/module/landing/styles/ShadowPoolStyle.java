package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.Breeze;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.kit.Smoke;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
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
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ShadowPoolStyle extends LandingStyleBase {
   private static final int MAX_PUFFS = 44;
   private static final int MAX_EMBERS = 24;
   private static final int POOL_PATCHES = 5;
   private static final double REACH = 2.1D;
   private static final double RIDE = 0.22D;
   private static final double EDGE = 0.42D;
   private static final float SMOKE_WIND = 0.25F;
   public final ColorSetting embers;
   public final ColorSetting edgeColor;
   public final SliderSetting ringSize;
   public final SliderSetting density;
   public final SliderSetting emberAmount;
   public final BooleanSetting pool;
   public final ModeSetting<Smoke.EdgeLight> edgeLight;
   private final double[] flow = new double[3];

   public ShadowPoolStyle(LandingImpactModule module) {
      super(module, LandingStyle.SHADOW_POOL, 1.6F, 3.4F, new ColorSetting("shadowpool.color", "Smoke", "The smoke and the pool.", ColorSetting.Mode.STATIC, -15593964, -14017744, 1.0D));
      this.signature(this.color, -15593964);
      this.embers = (ColorSetting)this.setting((new ColorSetting("shadowpool.embers", "Embers", "The sparks inside the smoke.", ColorSetting.Mode.STATIC, -45778, -23737, 1.0D)).secondary());
      this.signature(this.embers, -45778);
      this.edgeColor = (ColorSetting)this.setting((new ColorSetting("shadowpool.edge", "Edge Light", "The thin rim of light round the smoke.", ColorSetting.Mode.STATIC, -4124129, -50373, 1.0D)).secondary());
      this.signature(this.edgeColor, -4124129);
      this.ringSize = this.look((SliderSetting)this.setting((new SliderSetting("shadowpool.ringSize", "Ring Size", "How far the ring of smoke rolls out.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
      this.density = this.look((SliderSetting)this.setting((new SliderSetting("shadowpool.density", "Density", "How thick the smoke is.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.emberAmount = (SliderSetting)this.setting((new SliderSetting("shadowpool.emberAmount", "Embers", "How many sparks ride in the smoke.", 0.0D, 100.0D, 40.0D, 5.0D)).unit("%"));
      this.pool = (BooleanSetting)this.setting(new BooleanSetting("shadowpool.pool", "Pool", "A pool of dark mist spreads under the feet.", true));
      this.edgeLight = (ModeSetting)this.setting((new ModeSetting("shadowpool.edgeLight", "Edge Light", "A thin red rim round the smoke: where it is dark (Auto), always, or never.", Smoke.EdgeLight.class, Smoke.EdgeLight.AUTO)).style(ModeSetting.Style.CHIPS));
   }

   public int passes(LandingFx fx) {
      return FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   private static double driftX(LandingFx fx, ShadowPoolStyle.State s) {
      return (double)(s.wind[0] * fx.styleTime());
   }

   private static double driftZ(LandingFx fx, ShadowPoolStyle.State s) {
      return (double)(s.wind[2] * fx.styleTime());
   }

   public void start(LandingFx fx) {
      ShadowPoolStyle.State s = new ShadowPoolStyle.State();
      s.jump = fx.land.jump;
      float d = this.density.getFloat() / 100.0F;
      s.reach = 2.1D * (double)this.ringSize.getFloat() / 100.0D * (double)fx.size * (s.jump?0.3D:1.0D);
      int n = s.jump?6:Math.round((18.0F + 22.0F * d) * (fx.lod?0.5F:1.0F) * Math.min(1.5F, fx.density));
      s.n = Math.max(4, Math.min(44, n));

      for(int i = 0; i < s.n; ++i) {
         s.angle[i] = 6.283185307179586D * ((double)i + 0.5D * (double)fx.rand(10 + i)) / (double)s.n;
         s.far[i] = s.reach * (0.82D + 0.3D * (double)fx.rand(30 + i));
         s.size[i] = (0.62D + 0.4D * (double)fx.rand(50 + i)) * Math.sqrt((double)fx.size) * (s.jump?0.7D:1.0D) * (0.8D + 0.4D * (double)d);
         s.spin[i] = (double)fx.rand(70 + i) * 6.283185307179586D;
      }

      int e = !s.jump && !fx.lod?Math.round(24.0F * this.emberAmount.getFloat() / 100.0F * Math.min(1.5F, fx.density)):0;
      s.embers = Math.min(24, e);

      for(int i = 0; i < s.embers; ++i) {
         s.eAngle[i] = (double)fx.rand(100 + i) * 6.283185307179586D;
         s.eLag[i] = 0.2D + 0.5D * (double)fx.rand(130 + i);
         s.eHeight[i] = 0.06D + 0.25D * (double)fx.rand(160 + i);
      }

      Breeze.at(fx.x(), fx.y() + 0.3D, fx.z(), fx.preview, s.wind);
      s.wind[0] *= 0.25F;
      s.wind[1] = 0.0F;
      s.wind[2] *= 0.25F;
      fx.data = s;
   }

   private static float out(LandingFx fx) {
      return Easing.outQuart(Easing.clamp01(fx.progress() / 0.7F));
   }

   private static float thin(LandingFx fx) {
      return 1.0F - Easing.smoothstep(0.35F, 0.95F, fx.progress());
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      ShadowPoolStyle.State s = (ShadowPoolStyle.State)fx.data;
      if(s != null) {
         Camera cam = Vfx.camera();
         if(pass == FxPass.GLOW) {
            this.light(fx, f, s, cam);
         } else {
            if(this.pool.get()) {
               this.poolMist(fx, f, s, cam);
            }

            this.ring(fx, f, s, cam);
         }
      }

   }

   private boolean place(LandingFx fx, FxFrame f, ShadowPoolStyle.State s, Camera cam, int i, int k) {
      float o = out(fx);
      float th = thin(fx);
      if(th <= 0.003F) {
         return false;
      } else {
         double r = 0.15D * s.reach + (s.far[i] - 0.15D * s.reach) * (double)o;
         double a = s.angle[i];
         double x = fx.x() + Math.cos(a) * r + driftX(fx, s);
         double z = fx.z() + Math.sin(a) * r + driftZ(fx, s);
         double age = (double)fx.styleTime();
         Smoke.curl(x, fx.y(), z, age, 1.3D, this.flow);
         double size = s.size[i] * (0.6D + 0.7D * (double)o);
         double var36;
         double var37;
         double ground = Ground.y(fx.ground, var36 = x + this.flow[0] * 0.18D * (double)o, var37 = z + this.flow[2] * 0.18D * (double)o);
         if(!Ground.lastValid()) {
            return false;
         } else {
            double y = ground + Math.max(0.08D, size * 0.22D + this.flow[1] * 0.05D);
            double rx = f.rx(var36);
            double ry = f.ry(y);
            double rz = f.rz(var37);
            float a8 = NearCamera.dark(cam, rx, ry, rz, 0.74F * th);
            double close = Math.sqrt(cam.distanceSq(rx, ry, rz));
            if(close < 2.0D) {
               double cap = f.ry(fx.y() + 0.45D);
               ry = Math.min(ry, cap);
               size *= 0.6D + 0.4D * (double)Easing.smoothstep(0.8F, 2.0F, (float)close);
            }

            Smoke.Puffs p = s.puffs;
            p.x[k] = rx;
            p.y[k] = ry;
            p.z[k] = rz;
            p.size[k] = (float)size;
            p.alpha[k] = a8;
            p.turn[k] = (float)(s.spin[i] + (double)o * 3.0D);
            p.cell[k] = i % 4;
            return true;
         }
      }
   }

   private void ring(LandingFx fx, FxFrame f, ShadowPoolStyle.State s, Camera cam) {
      Smoke.Puffs p = s.puffs;
      p.count = 0;

      for(int i = 0; i < s.n; ++i) {
         if(this.place(fx, f, s, cam, i, p.count)) {
            ++p.count;
         }
      }

      if(p.count != 0) {
         p.sort(cam);
         int smoke = ColorUtil.shade(this.color.getColor(0.5F), Math.max(0.5F, fx.light)) | -16777216;
         RenderState.translucent(false);
         p.draw(smoke, fx.alpha(), (KeepVisible)null, cam, 0.0D, 0.0D, 1.0D);
      }

   }

   private void poolMist(LandingFx fx, FxFrame f, ShadowPoolStyle.State s, Camera cam) {
      float pr = fx.progress();
      float spread = Easing.outCubic(Easing.clamp01(pr / 0.45F));
      float thinning = 1.0F - Easing.smoothstep(0.55F, 1.0F, pr);
      float a = 0.8F * thinning * fx.alpha();
      if(a > 0.003F) {
         double half = s.reach * (s.jump?0.9D:0.62D) * (0.4D + 0.6D * (double)spread);
         int smoke = ColorUtil.shade(this.color.getColor(0.5F), Math.max(0.5F, fx.light)) | -16777216;
         RenderState.decal(true);
         RenderState.translucent(false);

         for(int k = 0; k < 5; ++k) {
            double ang = 6.283185307179586D * (double)k / 5.0D + (double)fx.rand(200 + k);
            double off = k == 0?0.0D:half * 0.35D;
            double x = fx.x() + Math.cos(ang) * off;
            double z = fx.z() + Math.sin(ang) * off;
            double y = Ground.y(fx.ground, x, z) + 0.004D * (double)(k + 1);
            if(Ground.lastValid()) {
               double rx = f.rx(x);
               double ry = f.ry(y);
               double rz = f.rz(z);
               float ka = NearCamera.dark(cam, rx, ry, rz, a * (k == 0?1.0F:0.7F));
               Smoke.mist(rx, ry, rz, half * (k == 0?1.0D:0.75D), (double)fx.styleTime() * 0.15D * (double)(k % 2 == 0?1:-1) + (double)k, k % 4, Smoke.alphaOf(smoke, ka));
            }
         }

         RenderState.decal(false);
      }

   }

   private void light(LandingFx fx, FxFrame f, ShadowPoolStyle.State s, Camera cam) {
      float o = out(fx);
      float th = thin(fx);
      float alpha = fx.alpha();
      int ember = this.embers.getColor(0.5F);
      double time = (double)fx.styleTime();

      for(int i = 0; i < s.embers; ++i) {
         double r = Math.max(0.1D, s.reach * (double)o * (1.0D - 0.35D * s.eLag[i]));
         double x = fx.x() + Math.cos(s.eAngle[i]) * r + driftX(fx, s);
         double z = fx.z() + Math.sin(s.eAngle[i]) * r + driftZ(fx, s);
         double y = Ground.y(fx.ground, x, z) + s.eHeight[i] * (0.6D + 0.4D * Math.sin(time * 5.0D + (double)i));
         float flick = 0.55F + 0.45F * (float)Math.sin(time * 17.0D + (double)i * 2.3D);
         float k = th * flick * alpha;
         double rx = f.rx(x);
         double ry = f.ry(y);
         double rz = f.rz(z);
         Smoke.ember(rx, ry, rz, (float)(0.22D * Math.sqrt((double)fx.size)), (float)(0.06D * Math.sqrt((double)fx.size)), ember, k * NearCamera.fade(cam, rx, ry, rz));
      }

      Smoke.EdgeLight mode = (Smoke.EdgeLight)this.edgeLight.get();
      float edge = mode == Smoke.EdgeLight.ON?1.0F:(mode == Smoke.EdgeLight.AUTO?fx.night:0.0F);
      if(edge > 0.01F) {
         double r = 0.15D * s.reach + (s.reach * 0.95D - 0.15D * s.reach) * (double)o + s.reach * 0.08D;
         int col = alpha(this.edgeColor.getColor(0.5F), 0.55F * edge * th * alpha);
         RenderState.decal(true);
         Ground.ring(f, fx.ground, Textures.LINE_GLOW, fx.x() + driftX(fx, s), fx.z() + driftZ(fx, s), r, 0.12D * Math.sqrt((double)fx.size), 64, (ColorSetting)null, col, 1.0F, 0.0F);
         RenderState.decal(false);
      }

   }

   public String stateForTest(LandingFx fx) {
      ShadowPoolStyle.State s = (ShadowPoolStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float pr = fx.progress();
         String phase = pr < 0.35F?"rolling out":(pr < 0.7F?"slowing, thinning":(pr < 1.0F?"the pool lingering":"gone"));
         Smoke.EdgeLight mode = (Smoke.EdgeLight)this.edgeLight.get();
         return String.format(Locale.ROOT, "%s; ring %.2f of %.2f, %d puffs (%d drawn), %d embers, pool %s, edge %s (%.2f); fall %.1f (x%.2f)%s; %s, light %.2f, night %.2f", new Object[]{phase, Double.valueOf(s.reach * (double)out(fx)), Double.valueOf(s.reach), Integer.valueOf(s.n), Integer.valueOf(s.puffs.count), Integer.valueOf(s.embers), this.pool.get()?"on":"off", mode.displayName(), Float.valueOf(mode == Smoke.EdgeLight.ON?1.0F:(mode == Smoke.EdgeLight.AUTO?fx.night:0.0F)), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light), Float.valueOf(fx.night)});
      }
   }

   private static final class State {
      final Smoke.Puffs puffs;
      int n;
      int embers;
      double reach;
      boolean jump;
      final double[] angle;
      final double[] far;
      final double[] size;
      final double[] spin;
      final double[] eAngle;
      final double[] eLag;
      final double[] eHeight;
      final float[] wind;

      private State() {
         this.puffs = new Smoke.Puffs(49);
         this.angle = new double[44];
         this.far = new double[44];
         this.size = new double[44];
         this.spin = new double[44];
         this.eAngle = new double[24];
         this.eLag = new double[24];
         this.eHeight = new double[24];
         this.wind = new float[3];
      }
   }
}
