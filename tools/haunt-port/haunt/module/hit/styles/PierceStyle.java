package mix.client.mixui.integration.haunt.module.hit.styles;

import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.DepthPull;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class PierceStyle extends HitStyleBase {
   private static final int POINTS = 16;
   private static final float STAB = 0.2F;
   public final SliderSetting reach = (SliderSetting)this.setting((new SliderSetting("pierce.reach", "Reach", "How far past the target the lance goes.", 0.5D, 3.0D, 1.5D, 0.1D)).unit(" blocks"));
   public final SliderSetting width = (SliderSetting)this.setting((new SliderSetting("pierce.width", "Width", "How thick the lance is.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final Rng rng = new Rng();

   public PierceStyle(HitEffectModule module) {
      super(module, HitStyle.PIERCE, 0.3F, new ColorSetting("pierce.color", "Color", "Lance glow (gradient: tip to tail).", -8090));
   }

   private static double exitDistance(HitFx fx) {
      Basis b = fx.basis;
      double half = (double)fx.hit.width * 0.5D + 0.1D;
      double minX = fx.anchor.feetX() - half;
      double maxX = fx.anchor.feetX() + half;
      double minY = fx.anchor.feetY() - 0.1D;
      double maxY = fx.anchor.feetY() + (double)fx.hit.height + 0.1D;
      double minZ = fx.anchor.feetZ() - half;
      double maxZ = fx.anchor.feetZ() + half;
      double far = 4.0D;
      far = Math.min(far, slab(fx.ix(), b.fx, minX, maxX));
      far = Math.min(far, slab(fx.iy(), b.fy, minY, maxY));
      far = Math.min(far, slab(fx.iz(), b.fz, minZ, maxZ));
      return Math.max(0.15D, far);
   }

   private static double slab(double o, double d, double min, double max) {
      return Math.abs(d) < 1.0E-9D?Double.MAX_VALUE:Math.max((min - o) / d, (max - o) / d);
   }

   public void start(HitFx fx) {
      if(!fx.lod) {
         Basis b = fx.basis;
         double exit = exitDistance(fx);
         double total = exit + this.reach.get() * (double)fx.size;
         double ratio = Math.min(0.99D, exit / total);
         float q = (float)(1.0D - Math.cbrt(1.0D - ratio));
         float delay = fx.life * 0.2F * q;
         this.rng.seed(fx.seed);
         double ex = fx.ix() + b.fx * exit;
         double ey = fx.iy() + b.fy * exit;
         double ez = fx.iz() + b.fz * exit;
         int n = fx.count(9.0F, true);

         for(int i = 0; i < n; ++i) {
            float speed = this.rng.range(0.8F, 2.2F);
            fx.particles.spawn(3, ex, ey, ez, (float)(b.fx * (double)speed + (double)this.rng.signed() * 0.9D), (float)(b.fy * (double)speed + (double)this.rng.signed() * 0.9D) + 0.3F, (float)(b.fz * (double)speed + (double)this.rng.signed() * 0.9D), fx.seconds(this.rng.range(0.25F, 0.4F)), this.rng.range(0.04F, 0.07F) * fx.size).end(0.01F).drag(3.5F).color(this.color, this.rng.next()).alpha(fx.opacity).delay(delay).follow(fx.anchor, 0.5F);
         }
      }

   }

   public void render(HitFx fx, FxFrame f) {
      float p = fx.progress();
      float a = fx.alpha() * (1.0F - Easing.smoothstep(0.4F, 1.0F, p));
      if(a > 0.003F) {
         Basis b = fx.basis;
         double exit = exitDistance(fx);
         double total = exit + this.reach.get() * (double)fx.size;
         float stab = Easing.outCubic(Easing.clamp01(p / 0.2F));
         double tipD = total * (double)stab;
         double tailD = -0.3D * (double)fx.size + tipD * 0.25D * (double)Easing.smoothstep(0.3F, 1.0F, p);
         float thin = 1.0F - 0.8F * Easing.smoothstep(0.25F, 1.0F, p);
         double w = 0.075D * (double)fx.size * (double)this.width.getFloat() * (fx.crit?1.4D:1.0D) * (double)thin;
         double ix = fx.ix();
         double iy = fx.iy();
         double iz = fx.iz();
         double limit = DepthPull.distance(f, ix, iy, iz) - 0.12D;
         if(tipD > tailD + 0.02D) {
            for(int i = 0; i < 16; ++i) {
               float s = (float)i / 15.0F;
               double d = tailD + (tipD - tailD) * (double)s;
               Vfx.X[i] = f.rx(ix + b.fx * d);
               Vfx.Y[i] = f.ry(iy + b.fy * d);
               Vfx.Z[i] = f.rz(iz + b.fz * d);
               float profile = (float)(Math.pow((double)s, 0.45D) * Math.pow((double)(1.0F - s), 0.35D) * 1.9D);
               Vfx.W[i] = (float)(w * 1.6D * (double)profile);
               Vfx.C[i] = alpha(this.color.getColor(1.0F - s), a);
            }

            DepthPull.apply(0, 16, limit);
            Neon.add(0, 16, false);
            Neon.flush();
         }

         float entry;
         if((entry = 1.0F - Easing.smoothstep(0.0F, 0.3F, p)) > 0.003F) {
            double x = f.rx(ix - b.fx * 0.1D);
            double y = f.ry(iy - b.fy * 0.1D);
            double z = f.rz(iz - b.fz * 0.1D);
            int c = this.color.getColor(1.0F);
            RenderState.translucent(false);
            Vfx.sprite(Textures.FLARE, x, y, z, 0.7D * (double)fx.size, 0.8F, alpha(c, 0.75F * entry * a));
            RenderState.additive();
            Vfx.sprite(Textures.GLOW, x, y, z, 0.7D * (double)fx.size, 0.0F, alpha(c, 0.45F * entry * a));
            Vfx.sprite(Textures.FLARE, x, y, z, 0.45D * (double)fx.size, 0.8F, alpha(Neon.tint(c), entry * a));
         }

         if(tipD >= exit) {
            float since = Easing.clamp01((float)((tipD - exit) / Math.max(0.01D, total - exit)));
            float flare = (1.0F - Easing.smoothstep(0.2F, 1.0F, p)) * Easing.smoothstep(0.0F, 0.15F, since + 0.1F);
            double ex = f.rx(ix + b.fx * exit);
            double ey = f.ry(iy + b.fy * exit);
            double ez = f.rz(iz + b.fz * exit);
            double k = DepthPull.factor(ex, ey, ez, limit);
            Camera cam = Vfx.camera();
            ex = cam.posX + (ex - cam.posX) * k;
            ey = cam.posY + (ey - cam.posY) * k;
            ez = cam.posZ + (ez - cam.posZ) * k;
            int c = this.color.getColor(0.4F);
            Vfx.sprite(Textures.GLOW, ex, ey, ez, 0.9D * (double)fx.size * k, 0.0F, alpha(c, 0.5F * flare * a));
            Vfx.sprite(Textures.FLARE, ex, ey, ez, 0.8D * (double)fx.size * k, 0.3F, alpha(Neon.tint(c), flare * a));
         }
      }

   }
}
