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

public final class NeuralNetStyle extends LandingStyleBase {
   private static final int POINTS = 26;
   private static final int FORK_POINTS = 12;
   private static final float GROW_END = 0.55F;
   private static final float RETRACT_END = 0.86F;
   public final SliderSetting tendrils = (SliderSetting)this.setting(new SliderSetting("neural.tendrils", "Tendrils", "How many tendrils crawl out.", 8.0D, 14.0D, 11.0D, 1.0D));
   public final SliderSetting length = (SliderSetting)this.setting((new SliderSetting("neural.length", "Length", "How far they reach.", 1.0D, 3.0D, 2.0D, 0.05D)).unit(" blocks"));
   public final SliderSetting wiggle = (SliderSetting)this.setting((new SliderSetting("neural.wiggle", "Wiggle", "How much they wriggle.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final Rng rng = new Rng();

   public NeuralNetStyle(LandingImpactModule module) {
      super(module, LandingStyle.NEURAL_NET, 1.3F, 2.4F, new ColorSetting("neural.color", "Color", "Tendril glow (gradient: a different mix per tendril).", -12717313));
   }

   private int count(LandingFx fx) {
      return this.tendrils.getInt();
   }

   private double angle(LandingFx fx, int i, int n) {
      return 6.283185307179586D * ((double)i + 0.35D * (double)fx.srand(i * 3 + 1)) / (double)n;
   }

   private void path(LandingFx fx, int i, int n, int offset) {
      double step = this.length.get() * (double)fx.size / 25.0D * (0.8D + 0.4D * (double)fx.rand(i * 3 + 2));
      Procedural.tendril(offset, 26, fx.x(), fx.y(), fx.z(), this.angle(fx, i, n), step, 0.95F * this.wiggle.getFloat(), 1.3F, fx.seed + i * 101);
   }

   public void start(LandingFx fx) {
      if(!fx.lod) {
         this.rng.seed(fx.seed);
         int n = this.count(fx);

         for(int i = 0; i < n; ++i) {
            this.path(fx, i, n, 0);
            int sparks = fx.count(4.0F);

            for(int s = 0; s < sparks; ++s) {
               float along = 0.3F + 0.7F * this.rng.next();
               int idx = Math.min(25, (int)(along * 25.0F));
               double x = Vfx.X[idx];
               double z = Vfx.Z[idx];
               double dx = x - fx.x();
               double dz = z - fx.z();
               double dl = Math.max(1.0E-4D, Math.sqrt(dx * dx + dz * dz));
               float out = this.rng.range(0.8F, 1.8F) * (float)Math.sqrt((double)fx.size);
               float when = 0.55F + 0.31F * (float)Math.pow((double)along, 1.4D);
               fx.particles.spawn(7, x, fx.ground.at(x, z) + 0.06D, z, (float)(dx / dl * (double)out), this.rng.range(0.3F, 0.9F), (float)(dz / dl * (double)out), fx.seconds(this.rng.range(0.45F, 0.65F)), this.rng.range(0.045F, 0.07F) * fx.size).gravity(3.0F).drag(2.6F).color(this.color, (float)i / (float)Math.max(1, n - 1)).alpha(fx.opacity).flags(2).sway(0.0F, this.rng.next()).delay(fx.life * when).ground(fx.y() + 0.04D, 0, 0.3F);
            }
         }
      }

   }

   public void render(LandingFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha();
      int n = this.count(fx);
      float flash = 1.0F - Easing.smoothstep(0.0F, 0.14F, p);
      if(flash > 0.003F) {
         double x = f.rx(fx.x());
         double y = f.ry(fx.y() + 0.15D);
         double z = f.rz(fx.z());
         int c = this.color.getColor(0.5F);
         RenderState.translucent(false);
         Vfx.sprite(Textures.FLARE, x, y, z, 1.2D * (double)fx.size * (double)(1.2F - 0.2F * flash), 0.3F, alpha(c, 0.7F * flash * a));
         RenderState.additive();
         Vfx.sprite(Textures.GLOW, x, y, z, 1.6D * (double)fx.size, 0.0F, alpha(c, 0.5F * flash * a));
         if(this.color.getEffectiveMode() == ColorSetting.Mode.CHROMA) {
            for(int k = 0; k < 8; ++k) {
               double ang = 6.283185307179586D * (double)k / 8.0D + 0.2D;
               double len = (0.5D + 0.3D * (double)(k % 2)) * (double)fx.size * (double)(1.2F - 0.2F * flash);
               int rc = this.color.getColor((float)k / 8.0F * 4.0F);
               Vfx.line(Textures.LINE_GLOW, x, y, z, x + Math.cos(ang) * len, y + 0.05D, z + Math.sin(ang) * len, 0.07D * (double)fx.size, alpha(rc, flash * a), rc & 16777215);
            }
         }

         Vfx.sprite(Textures.FLARE, x, y, z, 0.8D * (double)fx.size * (double)(1.2F - 0.2F * flash), 0.3F, alpha(Neon.tint(c), flash * a));
      }

      float grow = Easing.outCubic(Easing.clamp01((p - 0.04F) / 0.51F));
      float retract = Easing.inQuad(Easing.clamp01((p - 0.55F) / 0.31F));
      if(grow > 0.0F && retract < 1.0F) {
         RenderState.decal(true);
         int offset = 0;

         for(int i = 0; i < n; ++i) {
            offset = this.addTendril(fx, f, i, n, (float)i / (float)Math.max(1, n - 1), grow, retract, a, offset);
         }

         Neon.flush();
         RenderState.decal(false);
      }

   }

   private int addTendril(LandingFx fx, FxFrame f, int i, int n, float t, float grow, float retract, float a, int offset) {
      this.path(fx, i, n, offset);
      int head = Math.max(1, Math.round(grow * 25.0F));
      int tail = Math.round(retract * 25.0F);
      int c = this.color.getColor(t);
      double baseW = 0.095D * Math.sqrt((double)fx.size);
      int from = 7 + (int)(fx.rand(i * 5 + 7) * 8.0F);
      double rootX = Vfx.X[offset + from];
      double rootZ = Vfx.Z[offset + from];
      int count = fill(offset, tail, head, c, baseW, a);
      if(count >= 2) {
         Ground.toRender(f, fx.ground, offset + tail, count);
         Neon.addFlat(offset + tail, count);
      }

      int next = offset + 26;
      if(head > from + 1 && tail < from) {
         double dir = this.angle(fx, i, n) + (fx.rand(i * 5 + 9) < 0.5F?-0.7D:0.7D);
         double step = this.length.get() * (double)fx.size / 25.0D * 0.8D;
         int forkLen = Math.max(2, Math.round((float)(head - from) / (float)(25 - from) * 12.0F));
         Procedural.tendril(next, forkLen, rootX, fx.y(), rootZ, dir, step, 0.8F * this.wiggle.getFloat(), 1.6F, fx.seed + i * 211);
         int fc = fill(next, 0, forkLen - 1, c, baseW * 0.6D, a * 0.85F);
         if(fc >= 2) {
            Ground.toRender(f, fx.ground, next, fc);
            Neon.addFlat(next, fc);
         }

         next += 12;
      }

      return next;
   }

   private static int fill(int offset, int tail, int head, int c, double baseW, float a) {
      for(int k = tail; k <= head; ++k) {
         float s = (float)k / (float)Math.max(1, head);
         float taper = 1.0F - 0.7F * s;
         float tipGlow = 1.0F + 0.5F * (float)Math.exp((double)(-(head - k)) * 0.6D);
         Vfx.W[offset + k] = (float)(baseW * (double)taper);
         Vfx.C[offset + k] = ColorUtil.mulAlpha(c, Math.min(1.0F, a * tipGlow));
      }

      return head - tail + 1;
   }
}
