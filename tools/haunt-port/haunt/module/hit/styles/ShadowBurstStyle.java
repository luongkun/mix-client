package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.Breeze;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Smoke;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ShadowBurstStyle extends HitStyleBase {
   private static final int MAX_PUFFS = 48;
   private static final int RING = 16;
   private static final int CRACKS = 4;
   private static final double STACK = 4.0D;
   private static final float SMOKE_WIND = 0.4F;
   public final ColorSetting emberColor;
   public final ColorSetting edgeColor;
   public final SliderSetting density;
   public final SliderSetting puffs;
   public final SliderSetting spread;
   public final SliderSetting embers;
   public final ModeSetting<Smoke.EdgeLight> edgeLight;
   private final Smoke.Puffs cloud = new Smoke.Puffs(64);
   private final double[] flow = new double[3];
   private final float[] wind = new float[3];

   public ShadowBurstStyle(HitEffectModule module) {
      super(module, HitStyle.SHADOW_BURST, 0.7F, new ColorSetting("shadowburst.color", "Smoke", "The smoke\'s color (dark).", ColorSetting.Mode.STATIC, -15593964, -14019536, 1.0D));
      this.signature(this.color, -15593964);
      this.emberColor = (ColorSetting)this.setting((new ColorSetting("shadowburst.embers", "Embers", "The embers and the cracks in the thickest smoke.", ColorSetting.Mode.STATIC, -45778, -19641, 1.0D)).secondary());
      this.signature(this.emberColor, -45778);
      this.edgeColor = (ColorSetting)this.setting((new ColorSetting("shadowburst.edge", "Edge Light", "The thin rim of light round the smoke.", ColorSetting.Mode.STATIC, -4124129, -45715, 1.0D)).secondary());
      this.signature(this.edgeColor, -4124129);
      this.density = this.look((SliderSetting)this.setting((new SliderSetting("shadowburst.density", "Density", "How thick the smoke is.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.puffs = this.look((SliderSetting)this.setting(new SliderSetting("shadowburst.puffs", "Puffs", "How many puffs of smoke burst out.", 6.0D, 24.0D, 12.0D, 1.0D)));
      this.spread = (SliderSetting)this.setting((new SliderSetting("shadowburst.spread", "Spread", "How wide the cone of smoke opens.", 30.0D, 120.0D, 70.0D, 5.0D)).unit("°"));
      this.embers = (SliderSetting)this.setting((new SliderSetting("shadowburst.emberAmount", "Embers", "How many embers drift out of the smoke.", 0.0D, 100.0D, 40.0D, 5.0D)).unit("%"));
      this.edgeLight = (ModeSetting)this.setting(new ModeSetting("shadowburst.edgeLight", "Edge Light", "A thin red rim round the smoke: on Auto when it is dark round the target.", Smoke.EdgeLight.class, Smoke.EdgeLight.AUTO));
   }

   public int passes(HitFx fx) {
      return FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   private void layout(HitFx fx, FxFrame f, Camera cam) {
      Basis b = fx.basis;
      double s = scale(fx);
      double t = (double)fx.styleTime();
      float p = fx.progress();
      int n = Math.min(48, Math.round((float)this.puffs.getInt() * fx.density * 1.5F));
      double half = Math.toRadians((double)this.spread.getFloat()) * 0.5D;
      float dens = this.density.getFloat() / 100.0F;
      float life = Easing.smoothstep(0.0F, 0.06F, p) * (1.0F - Easing.smoothstep(0.45F, 1.0F, p));
      double ground = fx.hit.groundY + 0.05D;
      Breeze.at(fx.ix(), fx.iy(), fx.iz(), fx.preview, this.wind);
      double windX = (double)(this.wind[0] * 0.4F) * t;
      double windY = (double)(this.wind[1] * 0.4F) * t;
      double windZ = (double)(this.wind[2] * 0.4F) * t;
      int count = 0;

      for(int k = 0; k < n; ++k) {
         int id = fx.seed + k * 13;
         double cone = half * Math.sqrt(hash(id, 1));
         double around = hash(id, 2) * 6.283185307179586D;
         double sc = Math.sin(cone);
         double cc = Math.cos(cone);
         double ca = Math.cos(around);
         double sa = Math.sin(around);
         double dx = b.fx * cc + (b.rx * ca + b.ux * sa) * sc;
         double dy = b.fy * cc + (b.ry * ca + b.uy * sa) * sc;
         double dz = b.fz * cc + (b.rz * ca + b.uz * sa) * sc;
         double speed = (1.6D + 1.6D * hash(id, 3)) * s;
         double travel = speed * (1.0D - Math.exp(-3.2D * t)) / 3.2D;
         double spill = (1.4D + 1.0D * hash(id, 9)) * s * (1.0D - Math.exp(-2.8D * t)) / 2.8D;
         double lx = b.rx * ca + b.ux * sa;
         double ly = b.ry * ca + b.uy * sa;
         double lz = b.rz * ca + b.uz * sa;
         double x = fx.ix() + dx * travel + lx * spill;
         double y = fx.iy() + dy * travel + ly * spill;
         double z = fx.iz() + dz * travel + lz * spill;
         Smoke.curl(x, y, z, t + (double)k * 0.37D, 2.2D, this.flow);
         double curl = 0.22D * s * (double)Easing.clamp01((float)(t / 0.25D));
         x = x + this.flow[0] * curl + windX;
         y = y + this.flow[1] * curl + 0.12D * t + windY;
         z = z + this.flow[2] * curl + windZ;
         double size = s * (0.3D + 0.55D * (double)Easing.outCubic(Easing.clamp01((float)(t / 0.45D)))) * (0.75D + 0.5D * hash(id, 4));
         double floor = ground + size * 0.3D;
         if(y < floor) {
            double under = floor - y;
            double hx = dx;
            double hz = dz;
            double hl = Math.sqrt(dx * dx + dz * dz);
            if(hl < 0.001D) {
               hx = Math.cos(around);
               hz = Math.sin(around);
               hl = 1.0D;
            }

            x += hx / hl * under;
            z += hz / hl * under;
            y = floor;
         }

         this.cloud.x[count] = f.rx(x);
         this.cloud.y[count] = f.ry(y);
         this.cloud.z[count] = f.rz(z);
         this.cloud.size[count] = (float)size;
         this.cloud.alpha[count] = life * (0.55F + 0.45F * dens) * (float)(0.8D + 0.2D * hash(id, 5));
         this.cloud.turn[count] = (float)(hash(id, 6) * 6.283185307179586D + t * (hash(id, 7) - 0.5D) * 1.5D);
         this.cloud.cell[count] = (int)(hash(id, 8) * 4.0D) % 4;
         ++count;
      }

      if(fx.crit) {
         double rt = (double)Easing.outCubic(Easing.clamp01((float)(t / 0.55D)));
         double dist = (0.2D + 1.6D * rt) * s;
         double radius = (0.25D + 0.75D * rt) * s;
         float ringLife = Easing.smoothstep(0.0F, 0.05F, p) * (1.0F - Easing.smoothstep(0.35F, 0.9F, p));

         for(int k = 0; k < 16; ++k) {
            double a = 6.283185307179586D * (double)k / 16.0D + t * 1.4D;
            double ca = Math.cos(a);
            double sa = Math.sin(a);
            double x = fx.ix() + b.fx * dist + (b.rx * ca + b.ux * sa) * radius;
            double y = fx.iy() + b.fy * dist + (b.ry * ca + b.uy * sa) * radius;
            double z = fx.iz() + b.fz * dist + (b.rz * ca + b.uz * sa) * radius;
            this.cloud.x[count] = f.rx(x);
            this.cloud.y[count] = f.ry(y);
            this.cloud.z[count] = f.rz(z);
            this.cloud.size[count] = (float)(s * (0.3D + 0.2D * rt));
            this.cloud.alpha[count] = ringLife * (0.6F + 0.35F * dens);
            this.cloud.turn[count] = (float)(a * 2.0D);
            this.cloud.cell[count] = k % 4;
            ++count;
         }
      }

      this.cloud.count = count;
      this.cloud.sort(cam);
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      float alpha = fx.alpha();
      if(alpha > 0.003F) {
         Camera cam = Vfx.camera();
         this.layout(fx, f, cam);
         if(pass == FxPass.TRANSLUCENT) {
            this.smoke(fx, f, cam, alpha);
         } else {
            this.light(fx, cam, alpha);
         }
      }

   }

   private void smoke(HitFx fx, FxFrame f, Camera cam, float alpha) {
      int tex = Smoke.sheet();
      int col = this.color.getColor(0.5F);
      double bx = f.rx(fx.cx());
      double bz = f.rz(fx.cz());

      for(int i = 0; i < this.cloud.count; ++i) {
         int k = this.cloud.order(i);
         double x = this.cloud.x[k];
         double y = this.cloud.y[k];
         double z = this.cloud.z[k];
         double h = (double)this.cloud.size[k] * 0.5D;
         float a = alpha * this.cloud.alpha[k];
         a = Smoke.readable(fx.keep, cam, bx, bz, x, y, z, h, a, 4.0D);
         if((a = NearCamera.dark(cam, x, y, z, a)) > 0.004F) {
            float u0 = (float)this.cloud.cell[k] / 4.0F;
            float u1 = (float)(this.cloud.cell[k] + 1) / 4.0F;
            Vfx.sprite(tex, x, y, z, h, h, this.cloud.turn[k], u0, 0.0F, u1, 1.0F, Smoke.alphaOf(col, a));
         }
      }

   }

   private void light(HitFx fx, Camera cam, float alpha) {
      double s = scale(fx);
      double t = (double)fx.styleTime();
      float p = fx.progress();
      int ember = this.emberColor.getColor(0.5F);
      float amount = this.embers.getFloat() / 100.0F;
      if(amount > 0.0F && !fx.lod) {
         int n = Math.round(amount * 12.0F * fx.density);

         for(int i = 0; i < n && this.cloud.count > 0; ++i) {
            int k = (i * 5 + 2) % this.cloud.count;
            double up = 0.25D * t + 0.08D * Math.sin(t * 6.0D + (double)i);
            double side = 0.1D * Math.sin(t * 3.0D + (double)i * 2.1D);
            float flick = 0.5F + 0.5F * (float)Math.sin(t * 22.0D + (double)i * 1.7D);
            float a = alpha * Easing.smoothstep(0.05F, 0.2F, p) * (1.0F - Easing.smoothstep(0.6F, 1.0F, p)) * (0.4F + 0.6F * flick);
            Smoke.ember(this.cloud.x[k] + cam.rightX * side, this.cloud.y[k] + up, this.cloud.z[k] + cam.rightZ * side, 0.12F * (float)s, 0.035F * (float)s, ember, a);
         }

         float ca = alpha * amount * (1.0F - Easing.smoothstep(0.3F, 0.7F, p)) * Easing.smoothstep(0.04F, 0.12F, p);

         for(int c = 0; c < 4 && c < this.cloud.count; ++c) {
            float flick = 0.35F + 0.65F * (float)Math.abs(Math.sin(t * 17.0D + (double)c * 2.9D));
            double x = this.cloud.x[c];
            double y = this.cloud.y[c];
            double z = this.cloud.z[c];
            double len = (double)this.cloud.size[c] * 0.18D;

            for(int seg = 0; seg < 3; ++seg) {
               double a0 = (hash(fx.seed + c * 7, seg) - 0.5D) * 2.4D + (double)c;
               double x1 = x + (cam.rightX * Math.cos(a0) + cam.upX * Math.sin(a0)) * len;
               double y1 = y + (cam.rightY * Math.cos(a0) + cam.upY * Math.sin(a0)) * len;
               double z1 = z + (cam.rightZ * Math.cos(a0) + cam.upZ * Math.sin(a0)) * len;
               crackSegment(cam, x, y, z, x1, y1, z1, 0.016D * s, Smoke.alphaOf(ember, ca * flick * (1.0F - (float)seg * 0.25F)));
               x = x1;
               y = y1;
               z = z1;
            }
         }
      }

      float edge;
      switch((Smoke.EdgeLight)this.edgeLight.get()) {
      case ON:
         edge = 1.0F;
         break;
      case OFF:
         edge = 0.0F;
         break;
      default:
         edge = fx.night;
      }

      if(edge > 0.01F) {
         int col = this.edgeColor.getColor(0.5F);

         for(int k = 0; k < this.cloud.count; k += 2) {
            float a = alpha * edge * 0.14F * this.cloud.alpha[k];
            if(a > 0.003F) {
               Vfx.sprite(Textures.GLOW, this.cloud.x[k], this.cloud.y[k], this.cloud.z[k], (double)(this.cloud.size[k] * 1.3F), 0.0F, alpha(col, a));
            }
         }
      }

   }

   private static void crackSegment(Camera cam, double x0, double y0, double z0, double x1, double y1, double z1, double w, int col) {
      if(col >>> 24 != 0) {
         double dz = z1 - z0;
         double dy = y1 - y0;
         double vx = (y0 - cam.posY) * dz - (z0 - cam.posZ) * dy;
         double dx = x1 - x0;
         double vy = (z0 - cam.posZ) * dx - (x0 - cam.posX) * dz;
         double vz = (x0 - cam.posX) * dy - (y0 - cam.posY) * dx;
         double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
         if(vl >= 1.0E-9D) {
            double var30;
            double var31;
            double var32;
            Vfx.quadUV(Textures.LINE_GLOW, x0 - (var30 = vx * (w * 0.5D / vl)), y0 - (var31 = vy * (w * 0.5D / vl)), z0 - (var32 = vz * (w * 0.5D / vl)), 0.0D, 0.0D, col, x1 - var30, y1 - var31, z1 - var32, 1.0D, 0.0D, col, x1 + var30, y1 + var31, z1 + var32, 1.0D, 1.0D, col, x0 + var30, y0 + var31, z0 + var32, 0.0D, 1.0D, col);
         }
      }

   }

   public String stateForTest(HitFx fx) {
      return String.format(Locale.ROOT, "%d puffs%s, spread %.0f deg, edge light %s (night %.2f), light %.2f", new Object[]{Integer.valueOf(this.cloud.count), fx.crit?" (with the ring)":"", Double.valueOf(this.spread.get()), ((Smoke.EdgeLight)this.edgeLight.get()).displayName(), Float.valueOf(fx.night), Float.valueOf(fx.light)});
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }
}
