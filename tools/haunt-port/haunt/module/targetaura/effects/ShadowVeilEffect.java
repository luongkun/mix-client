package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.kit.Smoke;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ShadowVeilEffect extends AuraEffect {
   private static final int TENDRILS = 5;
   private static final int MAX_PUFFS = 80;
   private static final int MIST = 6;
   private static final int CRACKS = 3;
   private static final int CRACK_POINTS = 6;
   private static final double STACK = 5.0D;
   public final ColorSetting emberColor;
   public final ColorSetting edgeColor;
   public final SliderSetting density;
   public final SliderSetting height;
   public final SliderSetting flowSpeed;
   public final SliderSetting embers;
   public final BooleanSetting groundMist;
   public final ModeSetting<Smoke.EdgeLight> edgeLight;
   private final Smoke.Puffs cloud = new Smoke.Puffs(80);
   private final double[] px;
   private final double[] py;
   private final double[] pz;
   private final float[] psize;
   private final float[] palpha;
   private final float[] pturn;
   private final int[] pcell;
   private int puffs;

   public ShadowVeilEffect(TargetAuraModule module) {
      super(module, EffectType.SHADOW_VEIL, "shadowveil", new ColorSetting("shadowveil.color", "Smoke", "The smoke\'s color (dark).", ColorSetting.Mode.STATIC, -15593964, -14019536, 1.0D), true);
      this.px = this.cloud.x;
      this.py = this.cloud.y;
      this.pz = this.cloud.z;
      this.psize = this.cloud.size;
      this.palpha = this.cloud.alpha;
      this.pturn = this.cloud.turn;
      this.pcell = this.cloud.cell;
      this.emberColor = (ColorSetting)this.setting((new ColorSetting("shadowveil.embers", "Embers", "The embers glowing in the darkest smoke.", ColorSetting.Mode.STATIC, -45778, -19641, 1.0D)).secondary());
      this.edgeColor = (ColorSetting)this.setting(new ColorSetting("shadowveil.edge", "Edge Light", "The faint rim of light round the smoke.", ColorSetting.Mode.STATIC, -4124129, -45715, 1.0D));
      this.signature(this.emberColor, -45778);
      this.signature(this.edgeColor, -4124129);
      this.density = this.look((SliderSetting)this.setting((new SliderSetting("shadowveil.density", "Density", "How thick the smoke is.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.height = (SliderSetting)this.setting((new SliderSetting("shadowveil.height", "Height", "How high the smoke rises, as a share of the target\'s height.", 50.0D, 120.0D, 90.0D, 5.0D)).unit("%"));
      this.flowSpeed = this.look((SliderSetting)this.setting((new SliderSetting("shadowveil.flow", "Flow Speed", "How fast the smoke flows up.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.embers = (SliderSetting)this.setting((new SliderSetting("shadowveil.emberAmount", "Embers", "How many embers glow in the smoke.", 0.0D, 100.0D, 40.0D, 5.0D)).unit("%"));
      this.groundMist = (BooleanSetting)this.setting(new BooleanSetting("shadowveil.mist", "Ground Mist", "Dark mist pooling on the ground.", true));
      this.edgeLight = (ModeSetting)this.setting(new ModeSetting("shadowveil.edgeLight", "Edge Light", "A faint red rim round the smoke: on Auto when it is dark round the target.", Smoke.EdgeLight.class, Smoke.EdgeLight.AUTO));
   }

   public void render(EffectContext c, FxPass pass) {
      float alpha = this.intensity(c);
      if(alpha > 0.003F) {
         if(pass == FxPass.TRANSLUCENT) {
            this.layout(c);
            if(this.groundMist.get()) {
               this.mist(c, alpha);
            }

            this.smoke(c, alpha);
         } else if(pass == FxPass.GLOW) {
            this.layout(c);
            this.embers(c, alpha);
            this.edge(c, alpha);
         }
      }

   }

   private void layout(EffectContext c) {
      double s = (double)this.size(c);
      double t = c.time;
      double flow = (double)this.flowSpeed.getFloat() / 100.0D * (double)this.speed();
      float dens = this.density.getFloat() / 100.0F;
      this.puffs = Math.min(80, 20 + Math.round(60.0F * dens));
      double top = (double)(c.height * this.height.getFloat()) / 100.0D;
      double ground = Math.max(c.groundY, c.y - 0.6D);
      float in = Easing.outCubic(Easing.clamp01((float)(t / 0.3D)));
      float out = c.outro;
      Camera cam = c.cam != null?c.cam:Vfx.camera();

      for(int k = 0; k < this.puffs; ++k) {
         int m = k % 5;
         double phase = t * flow * (0.32D + 0.08D * (double)hash(k, 3)) + (double)k / (double)this.puffs + (double)hash(k, 5) * 0.3D;
         phase = phase - Math.floor(phase);
         double dir = (m & 1) == 0?1.0D:-1.0D;
         double base = 6.283185307179586D * (double)m / 5.0D + (double)hash(m, 7) * 0.6D;
         double angle = base + dir * phase * 2.4D + 0.45D * Math.sin(t * 0.7D * flow + (double)m * 1.9D) + (double)hash(k, 9) * 0.5D;
         double radius = s * (0.52D + 0.18D * Math.sin(phase * 3.141592653589793D) - 0.12D * phase + 0.08D * (double)hash(k, 11));
         double y = ground + 0.02D + (c.y + top - ground) * Math.pow(phase, 0.85D);
         double swirl = 0.09D * s;
         double dx = swirl * Math.sin(1.7D * y + 1.3D * t * flow + (double)m) + 0.04D * Math.sin(3.1D * y - 2.0D * t * flow + (double)k);
         double dz = swirl * Math.cos(1.5D * y - 1.1D * t * flow + (double)(m * 2)) + 0.04D * Math.cos(2.7D * y + 1.7D * t * flow + (double)k);
         y = ground + (y - ground) * (0.2D + 0.8D * (double)in) + 0.4D * (double)out * phase;
         double var42;
         double var43;
         double x = c.x + Math.cos(var42 = angle + (double)(1.0F - in) * 3.0D * dir) * (var43 = radius * (1.0D + 1.6D * (double)(1.0F - in) + 1.8D * (double)out)) + dx;
         double z = c.z + Math.sin(var42) * var43 + dz;
         this.px[k] = x;
         this.py[k] = y;
         this.pz[k] = z;
         this.psize[k] = (float)(s * (0.72D - 0.32D * phase) * (0.8D + 0.4D * (double)hash(k, 13)) * (1.0D + 0.6D * (double)out));
         float a = (float)(Math.pow(1.0D - phase, 1.1D) * (double)Easing.smoothstep(0.0F, 0.08F, (float)phase));
         this.palpha[k] = a * (0.6F + 0.4F * dens) * (float)Math.pow((double)(1.0F - out), 1.5D);
         this.pturn[k] = (float)((double)hash(k, 17) * 6.283185307179586D + t * ((double)hash(k, 19) - 0.5D) * 0.8D);
         this.pcell[k] = (int)(hash(k, 23) * 4.0F);
      }

      this.cloud.count = this.puffs;
      this.cloud.sort(cam);
   }

   private static float readable(EffectContext c, double x, double y, double z, double half, float a) {
      Camera cam = c.cam != null?c.cam:Vfx.camera();
      return Smoke.readable(c.keep, cam, c.x, c.z, x, y, z, half, a, 5.0D);
   }

   public String stateForTest(EffectContext c) {
      this.layout(c);
      float alpha = this.intensity(c);
      Camera cam = c.cam != null?c.cam:Vfx.camera();
      double cx = c.x - cam.posX;
      double cy = c.y + (double)c.height * 0.5D - cam.posY;
      double cz = c.z - cam.posZ;
      double bodyDepth = cx * cam.forwardX + cy * cam.forwardY + cz * cam.forwardZ;
      double front = bodyDepth - (double)c.width * 0.5D;
      double darkest = 0.0D;

      for(int gy = 0; gy <= 16; ++gy) {
         for(int gx = 0; gx <= 8; ++gx) {
            double u = ((double)gx / 8.0D - 0.5D) * 0.9D;
            double v = ((double)gy / 16.0D - 0.5D) * (double)c.height;
            double sx = (cx * cam.rightX + cy * cam.rightY + cz * cam.rightZ + u) / bodyDepth;
            double sy = (cx * cam.upX + cy * cam.upY + cz * cam.upZ + v) / bodyDepth;
            double clear = 1.0D;

            for(int k = 0; k < this.puffs; ++k) {
               double dx = this.px[k] - cam.posX;
               double dy = this.py[k] - cam.posY;
               double dz = this.pz[k] - cam.posZ;
               double d = dx * cam.forwardX + dy * cam.forwardY + dz * cam.forwardZ;
               double oy;
               double ox;
               double r;
               if(d > 0.05D && d < front && (r = Math.sqrt((ox = (dx * cam.rightX + dy * cam.rightY + dz * cam.rightZ) / d - sx) * ox + (oy = (dx * cam.upX + dy * cam.upY + dz * cam.upZ) / d - sy) * oy) / ((double)this.psize[k] * 0.5D / d)) < 1.0D) {
                  double cover = (1.0D - r) * (1.0D - r) * 0.9D;
                  clear *= 1.0D - (double)readable(c, this.px[k], this.py[k], this.pz[k], (double)this.psize[k] * 0.5D, alpha * this.palpha[k]) * cover;
               }
            }

            darkest = Math.max(darkest, 1.0D - clear);
         }
      }

      return String.format(Locale.ROOT, "%d puffs; the body at most %.2f dark (cap %.2f)", new Object[]{Integer.valueOf(this.puffs), Double.valueOf(darkest), Float.valueOf(c.keep.darkCap())});
   }

   private void smoke(EffectContext c, float alpha) {
      Camera cam = c.cam != null?c.cam:Vfx.camera();
      this.cloud.draw(this.color(c, 0.5F), alpha, c.keep, cam, c.x, c.z, 5.0D);
   }

   private void mist(EffectContext c, float alpha) {
      double s = (double)this.size(c);
      double t = c.time;
      float in = Easing.smoothstep(0.05F, 0.35F, (float)t);
      float out = 1.0F - Easing.smoothstep(0.0F, 0.7F, c.outro);
      float a = alpha * 0.42F * (0.5F + 0.5F * this.density.getFloat() / 100.0F) * in * out * c.groundAlpha;
      if(a > 0.004F) {
         int smoke = this.color(c, 0.2F);
         double y = c.groundY + 0.015D;

         for(int i = 0; i < 6; ++i) {
            double ang = 6.283185307179586D * (double)i / 6.0D + t * 0.08D * (double)(i % 2 == 0?1:-1);
            double r = s * (0.25D + 0.45D * (double)hash(i, 29)) * (1.0D + 0.8D * (double)c.outro);
            double cx = c.x + Math.cos(ang) * r;
            double cz = c.z + Math.sin(ang) * r;
            double half = s * (0.6D + 0.35D * (double)hash(i, 31)) * (1.0D + 0.5D * (double)c.outro);
            Smoke.mist(cx, y, cz, half, t * 0.1D + (double)i, i % 4, alphaOf(smoke, readable(c, cx, y, cz, half, a)));
         }
      }

   }

   private void embers(EffectContext c, float alpha) {
      float amount = this.embers.getFloat() / 100.0F;
      if(amount > 0.0F) {
         double t = c.time;
         double s = (double)this.size(c);
         int col = this.emberColor.getColor(0.5F);
         float fade = (float)Math.pow((double)(1.0F - c.outro), 1.5D) * Easing.smoothstep(0.1F, 0.4F, (float)t);
         int n = Math.round(amount * 14.0F);

         for(int i = 0; i < n; ++i) {
            int k = (i * 7 + 3) % Math.max(1, this.puffs);
            float flick = 0.45F + 0.55F * (float)(0.5D + 0.5D * Math.sin(t * 1.8D + (double)i * 2.3D));
            float a = alpha * fade * flick * Math.min(1.0F, this.palpha[k] * 2.2F);
            double ox = ((double)hash(i, 37) - 0.5D) * 0.12D;
            double oz = ((double)hash(i, 41) - 0.5D) * 0.12D;
            Smoke.ember(this.px[k] + ox, this.py[k], this.pz[k] + oz, 0.16F * (float)s, 0.045F * (float)s, col, a);
         }

         float ca;
         if(this.groundMist.get() && (ca = alpha * fade * amount * c.groundAlpha * (0.4F + 0.3F * (float)Math.sin(t * 1.1D))) > 0.004F) {
            double y = c.groundY + 0.02D;

            for(int k = 0; k < 3; ++k) {
               double ang = 6.283185307179586D * ((double)k + 0.3D * (double)hash(k, 43)) / 3.0D;
               double r = s * 0.15D;
               double x0 = c.x + Math.cos(ang) * r;
               double z0 = c.z + Math.sin(ang) * r;

               for(int p = 0; p < 5; ++p) {
                  double a0 = ang + ((double)hash(k * 11 + p, 47) - 0.5D) * 0.8D;
                  double len = s * (0.1D + 0.08D * (double)hash(k * 11 + p, 53));
                  double x1 = x0 + Math.cos(a0) * len;
                  double z1 = z0 + Math.sin(a0) * len;
                  float fadeOut = 1.0F - (float)p / 5.0F;
                  Smoke.crack(x0, y, z0, x1, z1, 0.035D * s, alpha(col, ca * fadeOut));
                  x0 = x1;
                  z0 = z1;
               }
            }
         }
      }

   }

   private void edge(EffectContext c, float alpha) {
      float amount;
      switch((Smoke.EdgeLight)this.edgeLight.get()) {
      case ON:
         amount = 1.0F;
         break;
      case OFF:
         amount = 0.0F;
         break;
      default:
         amount = c.night;
      }

      if(amount > 0.01F) {
         int col = this.edgeColor.getColor(0.5F);

         for(int k = 0; k < this.puffs; k += 3) {
            float a = alpha * amount * 0.13F * this.palpha[k];
            if(a > 0.003F) {
               Vfx.sprite(Textures.GLOW, this.px[k], this.py[k], this.pz[k], (double)(this.psize[k] * 1.35F), 0.0F, alpha(col, a));
            }
         }
      }

   }

   private static int alphaOf(int color, float a) {
      return Math.round(255.0F * Easing.clamp01(a) * ((float)(color >>> 24) / 255.0F)) << 24 | color & 16777215;
   }
}
