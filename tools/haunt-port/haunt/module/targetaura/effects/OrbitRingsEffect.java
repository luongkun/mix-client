package mix.client.mixui.integration.haunt.module.targetaura.effects;

import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class OrbitRingsEffect extends AuraEffect {
   private static final int SEGMENTS = 96;
   private static final int MAX_RINGS = 4;
   private static final double[] TILT = new double[]{0.5D, 0.5D, 1.05D, 0.82D};
   private static final double[] AZIMUTH = new double[]{0.0D, 3.141592653589793D, 1.9D, 4.2D};
   public final SliderSetting rings = (SliderSetting)this.setting(new SliderSetting("rings.count", "Rings", "Number of rings.", 1.0D, 4.0D, 3.0D, 1.0D));
   public final SliderSetting thickness = (SliderSetting)this.setting((new SliderSetting("rings.thickness", "Thickness", "Width of the rings and their glow.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   public final BooleanSetting comet = (BooleanSetting)this.setting(new BooleanSetting("rings.comet", "Comet Heads", "A bright spot travels along each ring.", true));
   private final double[] headX = new double[4];
   private final double[] headY = new double[4];
   private final double[] headZ = new double[4];
   private final float[] ringGrow = new float[4];

   public OrbitRingsEffect(TargetAuraModule module) {
      super(module, EffectType.ORBIT_RINGS, "rings", new ColorSetting("rings.color", "Color", "Ring color. The bright core is derived from it.", -41035));
   }

   public void render(EffectContext c) {
      float sz = this.size(c);
      float inten = this.intensity(c);
      float thick = this.thickness.getFloat();
      float spd = this.speed();
      int n = this.rings.getInt();
      boolean heads = this.comet.get();
      double cx = c.x;
      double cy = c.y + (double)c.height * 0.55D;
      double cz = c.z;

      for(int i = 0; i < n; ++i) {
         float in = Easing.clamp01((c.intro - (float)i * 0.1F) / 0.75F);
         float grow;
         this.ringGrow[i] = grow = Easing.outBack(in, 1.3F);
         double radius = (0.6D + 0.075D * (double)i) * (double)sz * (double)Math.max(0.0F, grow);
         double dir = i % 2 == 0?1.0D:-1.0D;
         double bob = 0.035D * Math.sin(c.time * 1.3D + (double)i * 2.0D);
         double az = AZIMUTH[i] + c.time * 0.3D * (double)spd * dir;
         double tilt = TILT[i];
         double hx = -Math.sin(az);
         double hz = Math.cos(az);
         double dx = Math.cos(az) * Math.cos(tilt);
         double dy = Math.sin(tilt);
         double dz = Math.sin(az) * Math.cos(tilt);
         double head = dir * c.time * 6.283185307179586D * 0.55D * (double)spd + (double)i * 2.1D;
         this.headX[i] = cx + radius * (hx * Math.cos(head) + dx * Math.sin(head));
         this.headY[i] = cy + bob + radius * dy * Math.sin(head);
         this.headZ[i] = cz + radius * (hz * Math.cos(head) + dz * Math.sin(head));
         int base = i * 96;

         for(int k = 0; k < 96; ++k) {
            double a = 6.283185307179586D * (double)k / 96.0D;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            Vfx.X[base + k] = cx + radius * (hx * cos + dx * sin);
            Vfx.Y[base + k] = cy + bob + radius * dy * sin;
            Vfx.Z[base + k] = cz + radius * (hz * cos + dz * sin);
         }
      }

      for(int pass = 0; pass < 2; ++pass) {
         boolean bloom = pass == 0;

         for(int i2 = 0; i2 < n; ++i2) {
            if(this.ringGrow[i2] > 0.001F) {
               double dir = i2 % 2 == 0?1.0D:-1.0D;
               double head = dir * c.time * 6.283185307179586D * 0.55D * (double)spd + (double)i2 * 2.1D;
               float widthGrow = Math.min(1.0F, this.ringGrow[i2] * 1.5F);
               int base = i2 * 96;

               for(int k = 0; k < 96; ++k) {
                  double a = 6.283185307179586D * (double)k / 96.0D;
                  float glow = 1.0F;
                  if(heads) {
                     double behind = dir * (head - a);
                     behind = behind - Math.floor(behind / 6.283185307179586D) * 6.283185307179586D;
                     glow = 0.3F + 0.7F * (float)Math.exp(-behind * 1.25D);
                  }

                  int col = this.color(c, loop((float)k / 96.0F + (float)i2 * 0.33F));
                  if(bloom) {
                     Vfx.W[base + k] = 0.2F * sz * thick * widthGrow;
                     Vfx.C[base + k] = alpha(col, inten * glow * 0.5F);
                  } else {
                     Vfx.W[base + k] = 0.042F * sz * thick * widthGrow;
                     Vfx.C[base + k] = core(col, inten * glow);
                  }
               }

               Vfx.ribbon(bloom?Textures.LINE_GLOW:Textures.LINE_CORE, base, 96, true);
            }
         }
      }

      if(heads) {
         for(int var47 = 0; var47 < n; ++var47) {
            if(this.ringGrow[var47] > 0.001F) {
               int col = this.color(c, (float)var47 * 0.33F);
               Vfx.sprite(Textures.GLOW, this.headX[var47], this.headY[var47], this.headZ[var47], (double)(0.6F * sz), 0.0F, alpha(col, 0.6F * inten));
            }
         }

         for(int var48 = 0; var48 < n; ++var48) {
            if(this.ringGrow[var48] > 0.001F) {
               Vfx.sprite(Textures.FLARE, this.headX[var48], this.headY[var48], this.headZ[var48], (double)(0.42F * sz), (float)(c.time * 1.4D + (double)var48), core(this.color(c, (float)var48 * 0.33F), 0.5F * inten));
            }
         }

         for(int var49 = 0; var49 < n; ++var49) {
            if(this.ringGrow[var49] > 0.001F) {
               Vfx.sprite(Textures.SPARK, this.headX[var49], this.headY[var49], this.headZ[var49], (double)(0.2F * sz), 0.0F, core(this.color(c, (float)var49 * 0.33F), inten));
            }
         }
      }

   }
}
