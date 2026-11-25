package mix.client.mixui.integration.haunt.module.targetaura.effects;

import mix.client.mixui.integration.haunt.fx.kit.Chains;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ChainBindEffect extends AuraEffect {
   private static final double PITCH = 0.72D;
   private static final double[] CHAIN_TILT = new double[]{0.5D, -0.5D, 0.1D};
   private static final double[] CHAIN_AZIMUTH = new double[]{0.0D, 0.0D, 1.3D};
   private static final double[] CHAIN_HEIGHT = new double[]{0.52D, 0.52D, 0.3D};
   private static final double[] CHAIN_RADIUS = new double[]{1.0D, 1.07D, 0.96D};
   private static final float RIM = 0.16F;
   public final SliderSetting chains = (SliderSetting)this.setting(new SliderSetting("chains.count", "Chains", "Number of chains.", 1.0D, 3.0D, 2.0D, 1.0D));
   public final BooleanSetting glint = (BooleanSetting)this.setting(new BooleanSetting("chains.glint", "Glint", "A shine sweeping along the chains.", true));
   public final ModeSetting<Material> material = this.materialSetting("chains", Material.CUSTOM, "What the chains are made of. Custom takes the chain color above, with a finish.");
   public final ModeSetting<Finish> finish;

   public ChainBindEffect(TargetAuraModule module) {
      super(module, EffectType.CHAIN_BIND, "chains", new ColorSetting("chains.color", "Color", "Metal color (the Custom material\'s).", -15809));
      this.finish = this.finishSetting("chains", Finish.METALLIC, this.material);
   }

   public void render(EffectContext c, FxPass pass) {
      Material m = (Material)this.material.get();
      Finish f;
      FxPass linkPass = m.translucent(f = (Finish)this.finish.get())?FxPass.TRANSLUCENT:FxPass.SOLID;
      if(pass == linkPass) {
         this.links(c, m, f, pass == FxPass.SOLID);
      } else if(pass == FxPass.GLOW) {
         this.sparkles(c);
      }

   }

   private void links(EffectContext c, Material m, Finish f, boolean solid) {
      float sz = this.size(c);
      float inten = this.intensity(c);
      float spd = this.speed();
      int n = this.chains.getInt();
      double link = 0.17D * (double)sz;
      float reveal = Easing.outCubic(Easing.clamp01(c.intro / 0.9F));
      double tighten = 1.0D + 0.6D * (1.0D - (double)Easing.outCubic(Easing.clamp01(c.intro / 0.7F)));
      double glintPos = c.time * 0.3D * (double)spd % 1.0D;
      boolean shine = this.glint.get();
      boolean custom = m == Material.CUSTOM;
      MeshBatch.begin(m, f, c.light, 0.16F, -1, true, solid);

      for(int ch = 0; ch < n; ++ch) {
         double az = CHAIN_AZIMUTH[ch] + c.time * 0.3D * (double)spd;
         double tilt = CHAIN_TILT[ch];
         double hx = -Math.sin(az);
         double hz = Math.cos(az);
         double dx = Math.cos(az) * Math.cos(tilt);
         double dy = Math.sin(tilt);
         double dz = Math.sin(az) * Math.cos(tilt);
         double nx = dy * hz;
         double ny = dz * hx - dx * hz;
         double nz = -dy * hx;
         double radius = ((double)c.width * 0.5D + 0.22D) * (double)sz * CHAIN_RADIUS[ch] * tighten;
         double cx = c.x;
         double cy = c.y + (double)c.height * CHAIN_HEIGHT[ch];
         double cz = c.z;
         int links = (int)Math.max(8L, Math.round(6.283185307179586D * radius / (0.72D * link)));
         links = links + (links & 1);
         double step = 6.283185307179586D / (double)links;
         double slide = c.time * 0.25D * (double)spd + (double)ch * 0.37D;

         float kf;
         for(int k = 0; k < links && (kf = (float)k / (float)links) <= reveal; ++k) {
            float frontier = Easing.clamp01((reveal - kf) * (float)links);
            double th = slide + (double)k * step;
            double cos = Math.cos(th);
            double sin = Math.sin(th);
            double px = cx + radius * (hx * cos + dx * sin);
            double py = cy + radius * dy * sin;
            double pz = cz + radius * (hz * cos + dz * sin);
            double ax = -hx * sin + dx * cos;
            double ay = dy * cos;
            double az2 = -hz * sin + dz * cos;
            double bz;
            double by;
            double bx;
            if((k & 1) == 0) {
               bx = nx;
               by = ny;
               bz = nz;
            } else {
               bx = hx * cos + dx * sin;
               by = dy * sin;
               bz = hz * cos + dz * sin;
            }

            double qx = ay * bz - az2 * by;
            double qy = az2 * bx - ax * bz;
            double qz = ax * by - ay * bx;
            float g = 0.0F;
            if(shine) {
               double d = Math.abs((double)kf - glintPos);
               d = Math.min(d, 1.0D - d);
               g = (float)Math.exp(-(d / 0.05D) * (d / 0.05D));
            }

            float alpha;
            if((alpha = inten * frontier * c.keep.solid(px, py, pz)) > 0.003F) {
               int tint = custom?this.color(c, loop(kf + (float)ch * 0.25F)):-1;
               int a = Math.round(alpha * (float)(custom?tint >>> 24:255));
               Chains.link(px, py, pz, ax, ay, az2, bx, by, bz, qx, qy, qz, link, a << 24 | tint & 16777215, g * 0.9F);
            }
         }
      }

      MeshBatch.end();
   }

   private void sparkles(EffectContext c) {
      if(this.glint.get()) {
         float sz = this.size(c);
         float inten = this.intensity(c);
         float spd = this.speed();
         int n = this.chains.getInt();
         float reveal = Easing.outCubic(Easing.clamp01(c.intro / 0.9F));
         double tighten = 1.0D + 0.6D * (1.0D - (double)Easing.outCubic(Easing.clamp01(c.intro / 0.7F)));
         double glintPos = c.time * 0.3D * (double)spd % 1.0D;
         if(glintPos <= (double)reveal) {
            for(int ch = 0; ch < n; ++ch) {
               double az = CHAIN_AZIMUTH[ch] + c.time * 0.3D * (double)spd;
               double tilt = CHAIN_TILT[ch];
               double hx = -Math.sin(az);
               double hz = Math.cos(az);
               double dx = Math.cos(az) * Math.cos(tilt);
               double dy = Math.sin(tilt);
               double dz = Math.sin(az) * Math.cos(tilt);
               double radius = ((double)c.width * 0.5D + 0.22D) * (double)sz * CHAIN_RADIUS[ch] * tighten;
               double th = c.time * 0.25D * (double)spd + (double)ch * 0.37D + glintPos * 6.283185307179586D;
               double px = c.x + radius * (hx * Math.cos(th) + dx * Math.sin(th));
               double py = c.y + (double)c.height * CHAIN_HEIGHT[ch] + radius * dy * Math.sin(th);
               double pz = c.z + radius * (hz * Math.cos(th) + dz * Math.sin(th));
               int col = this.material.get() == Material.CUSTOM?this.color(c, loop((float)glintPos + (float)ch * 0.25F)):((Material)this.material.get()).swatch();
               Vfx.sprite(Textures.GLOW, px, py, pz, (double)(0.35F * sz), 0.0F, alpha(col, 0.35F * inten));
               Vfx.sprite(Textures.FLARE, px, py, pz, (double)(0.45F * sz), (float)(c.time * 1.2D + (double)ch), core(col, 0.8F * inten));
            }
         }
      }

   }
}
