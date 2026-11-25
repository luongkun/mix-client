package mix.client.mixui.integration.haunt.module.targetaura.effects;

import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CrystalHaloEffect extends AuraEffect {
   private static final int MAX_GEMS = 13;
   private static final double LX = 0.3578D;
   private static final double LY = 0.8944D;
   private static final double LZ = 0.2683D;
   public final SliderSetting shards = (SliderSetting)this.setting(new SliderSetting("crystal.shards", "Shards", "Diamond shards orbiting the body.", 0.0D, 12.0D, 8.0D, 1.0D));
   public final SliderSetting motes = (SliderSetting)this.setting(new SliderSetting("crystal.motes", "Motes", "Small glowing specks drifting around.", 0.0D, 30.0D, 14.0D, 1.0D));
   public final SliderSetting height = (SliderSetting)this.setting((new SliderSetting("crystal.height", "Height", "Crystal height above the head (clear of nametags).", 0.3D, 1.5D, 0.85D, 0.05D)).unit(" blocks"));
   private final double[] gx = new double[78];
   private final double[] gy = new double[78];
   private final double[] gz = new double[78];
   private final double[] centerX = new double[13];
   private final double[] centerY = new double[13];
   private final double[] centerZ = new double[13];
   private final float[] gemAlpha = new float[13];
   private final int[] gemColor = new int[13];
   private final float[] gemEdge = new float[13];
   private int gemCount;

   public CrystalHaloEffect(TargetAuraModule module) {
      super(module, EffectType.CRYSTAL_HALO, "crystal", new ColorSetting("crystal.color", "Color", "Crystal color.", -12590849));
   }

   public void render(EffectContext c) {
      float sz = this.size(c);
      float inten = this.intensity(c);
      float spd = this.speed();
      this.gemCount = 0;
      float pop = Easing.outBack(Easing.clamp01(c.intro / 0.8F), 1.6F);
      double cx = c.x;
      double cz = c.z;
      double cy = c.y + (double)c.height + this.height.get() * (double)sz + 0.07D * (double)sz * Math.sin(c.time * 1.9D * (double)spd);
      int mainColor = this.color(c, 0.2F);
      if(pop > 0.001F) {
         double yaw = c.time * 0.9D * (double)spd;
         double wobble = 0.12D * Math.sin(c.time * 0.7D);
         double cp = Math.cos(yaw);
         double sp = Math.sin(yaw);
         double cw = Math.cos(wobble);
         double sw = Math.sin(wobble);
         this.addGem(cx, cy, cz, cp, 0.0D, -sp, sw * sp, cw, sw * cp, cw * sp, -sw, cw * cp, 0.24D * (double)sz * (double)pop, 0.3D * (double)sz * (double)pop, 0.13D * (double)sz * (double)pop, mainColor, inten, 0.014F * sz);
         Vfx.sprite(Textures.GLOW, cx, cy, cz, (double)(1.7F * sz * pop), 0.0F, alpha(mainColor, 0.55F * inten * (0.85F + 0.15F * (float)Math.sin(c.time * 3.0D))));
      }

      int n = this.shards.getInt();
      float arrive = Easing.outCubic(Easing.clamp01(c.intro / 0.9F));
      float shardAlpha = inten * Easing.smoothstep(0.0F, 0.6F, c.intro) * 0.85F;

      for(int i = 0; i < n && this.gemCount < 13; ++i) {
         double dir = i % 2 == 0?1.0D:-1.0D;
         double orbit = ((double)c.width * 0.5D + 0.38D + 0.12D * (double)hash(i, 1)) * (double)sz * (1.0D + 1.3D * (1.0D - (double)arrive));
         double baseY = c.y + (double)c.height * (0.15D + 0.7D * ((double)i * 0.618034D % 1.0D));
         double w = (0.45D + 0.3D * (double)hash(i, 2)) * (double)spd * dir;
         double phi = w * c.time + (double)i * 6.283185307179586D / (double)n;
         double tilt = 0.12D + 0.18D * (double)hash(i, 3);
         double px = cx + orbit * Math.cos(phi);
         double py = baseY + tilt * Math.sin(phi + (double)i);
         double pz = cz + orbit * Math.sin(phi);
         double tx = -Math.sin(phi) * dir;
         double tz = Math.cos(phi) * dir;
         double ax = tx * 0.6D;
         double ay = 1.0D;
         double az = tz * 0.6D;
         double len = Math.sqrt(ax * ax + ay * ay + az * az);
         double rx = Math.cos(phi);
         double rz = Math.sin(phi);
         double var96;
         double xx = (var96 = ay / len) * rz;
         double var95;
         double var97;
         double xy = (var97 = az / len) * rx - (var95 = ax / len) * rz;
         double xz = -var96 * rx;
         len = Math.sqrt(xx * xx + xy * xy + xz * xz);
         double var100;
         double var101;
         double zx = (var100 = xy / len) * var97 - (var101 = xz / len) * var96;
         double var99;
         double zy = var101 * var95 - (var99 = xx / len) * var97;
         double zz = var99 * var96 - var100 * var95;
         double spin = c.time * 2.2D * (double)spd + (double)i;
         double cs = Math.cos(spin);
         double ss = Math.sin(spin);
         double sxx = var99 * cs + zx * ss;
         double sxy = var100 * cs + zy * ss;
         double sxz = var101 * cs + zz * ss;
         double szx = zx * cs - var99 * ss;
         double szy = zy * cs - var100 * ss;
         double szz = zz * cs - var101 * ss;
         double scale = (0.8D + 0.4D * (double)hash(i, 4)) * (double)sz;
         this.addGem(px, py, pz, sxx, sxy, sxz, var95, var96, var97, szx, szy, szz, 0.11D * scale, 0.11D * scale, 0.032D * scale, this.color(c, (float)i / (float)Math.max(1, n - 1)), shardAlpha, 0.008F * sz);
      }

      this.drawGems(c.cam);
      if(pop > 0.001F) {
         Vfx.sprite(Textures.FLARE, this.gx[0], this.gy[0], this.gz[0], (double)(0.42F * sz * pop), (float)(c.time * 0.8D), core(mainColor, 0.55F * inten * (0.5F + 0.5F * (float)Math.sin(c.time * 2.3D))));
      }

      int m = this.motes.getInt();
      float appear = Easing.smoothstep(0.2F, 1.0F, c.intro) * inten;
      if(m > 0 && appear > 0.003F) {
         for(int pass = 0; pass < 2; ++pass) {
            for(int i = 0; i < m; ++i) {
               double r = (0.35D + 0.55D * (double)hash(i, 11)) * (double)sz;
               double w = (0.25D + 0.5D * (double)hash(i, 12)) * (double)(hash(i, 13) > 0.5F?1:-1) * (double)spd;
               double ang = (double)hash(i, 14) * 6.283185307179586D + c.time * w;
               double y = c.y + 0.1D + (double)hash(i, 15) * ((double)c.height + 0.5D) + 0.12D * Math.sin(c.time * (0.8D + (double)hash(i, 16)) + (double)hash(i, 17) * 6.28D);
               float flick = 0.55F + 0.45F * (float)Math.sin(c.time * (2.0D + 3.0D * (double)hash(i, 18)) + (double)hash(i, 19) * 6.28D);
               double x = cx + Math.cos(ang) * r;
               double z = cz + Math.sin(ang) * r;
               int col = this.color(c, hash(i, 20));
               if(pass == 0) {
                  Vfx.sprite(Textures.GLOW, x, y, z, (double)(0.24F * sz), 0.0F, alpha(col, 0.32F * appear * flick));
               } else {
                  Vfx.sprite(Textures.SPARK, x, y, z, (double)((0.06F + 0.035F * hash(i, 21)) * sz), 0.0F, core(col, appear * flick));
               }
            }
         }
      }

   }

   private void addGem(double cx, double cy, double cz, double xx, double xy, double xz, double yx, double yy, double yz, double zx, double zy, double zz, double top, double bottom, double radius, int color, float alpha, float edge) {
      int g = this.gemCount++;
      int o = g * 6;
      this.gx[o] = cx + yx * top;
      this.gy[o] = cy + yy * top;
      this.gz[o] = cz + yz * top;
      this.gx[o + 1] = cx - yx * bottom;
      this.gy[o + 1] = cy - yy * bottom;
      this.gz[o + 1] = cz - yz * bottom;

      for(int k = 0; k < 4; ++k) {
         double a = (double)k * 3.141592653589793D / 2.0D;
         double cos = Math.cos(a) * radius;
         double sin = Math.sin(a) * radius;
         this.gx[o + 2 + k] = cx + xx * cos + zx * sin;
         this.gy[o + 2 + k] = cy + xy * cos + zy * sin;
         this.gz[o + 2 + k] = cz + xz * cos + zz * sin;
      }

      this.centerX[g] = cx;
      this.centerY[g] = cy;
      this.centerZ[g] = cz;
      this.gemAlpha[g] = alpha;
      this.gemColor[g] = color;
      this.gemEdge[g] = edge;
   }

   private void drawGems(Camera cam) {
      for(int g = 0; g < this.gemCount; ++g) {
         int o = g * 6;

         for(int k = 0; k < 4; ++k) {
            int e1 = o + 2 + k;
            int e2 = o + 2 + (k + 1) % 4;
            this.face(cam, g, o, e1, e2);
            this.face(cam, g, o + 1, e2, e1);
         }
      }

      for(int pass = 0; pass < 2; ++pass) {
         for(int g = 0; g < this.gemCount; ++g) {
            int o = g * 6;
            float a = this.gemAlpha[g];
            int col = pass == 0?alpha(this.gemColor[g], 0.28F * a):core(this.gemColor[g], 0.9F * a);
            double width = pass == 0?(double)this.gemEdge[g] * 4.0D:(double)this.gemEdge[g];
            int tex = pass == 0?Textures.LINE_GLOW:Textures.LINE_CORE;

            for(int k = 0; k < 4; ++k) {
               int e1 = o + 2 + k;
               int e2 = o + 2 + (k + 1) % 4;
               Vfx.line(tex, this.gx[o], this.gy[o], this.gz[o], this.gx[e1], this.gy[e1], this.gz[e1], width, col, col);
               Vfx.line(tex, this.gx[o + 1], this.gy[o + 1], this.gz[o + 1], this.gx[e1], this.gy[e1], this.gz[e1], width, col, col);
               Vfx.line(tex, this.gx[e1], this.gy[e1], this.gz[e1], this.gx[e2], this.gy[e2], this.gz[e2], width, col, col);
            }
         }
      }

   }

   private void face(Camera cam, int g, int a, int b, int c) {
      double uy = this.gy[b] - this.gy[a];
      double vz = this.gz[c] - this.gz[a];
      double uz = this.gz[b] - this.gz[a];
      double vy = this.gy[c] - this.gy[a];
      double nx = uy * vz - uz * vy;
      double vx = this.gx[c] - this.gx[a];
      double ux = this.gx[b] - this.gx[a];
      double ny = uz * vx - ux * vz;
      double nz = ux * vy - uy * vx;
      double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
      if(len >= 1.0E-12D) {
         double fx = (this.gx[a] + this.gx[b] + this.gx[c]) / 3.0D;
         double fy = (this.gy[a] + this.gy[b] + this.gy[c]) / 3.0D;
         double fz = (this.gz[a] + this.gz[b] + this.gz[c]) / 3.0D;
         if((nx = nx / len) * (fx - this.centerX[g]) + (ny = ny / len) * (fy - this.centerY[g]) + (nz = nz / len) * (fz - this.centerZ[g]) < 0.0D) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
         }

         double wz;
         double wy;
         double wx;
         double wl;
         double facing = (wl = Math.sqrt((wx = cam.posX - fx) * wx + (wy = cam.posY - fy) * wy + (wz = cam.posZ - fz) * wz)) < 1.0E-9D?1.0D:Math.abs((nx * wx + ny * wy + nz * wz) / wl);
         double diffuse = Math.max(0.0D, nx * 0.3578D + ny * 0.8944D + nz * 0.2683D);
         double rim = (1.0D - facing) * (1.0D - facing);
         float light = (float)(0.3D + 0.45D * diffuse + 0.55D * rim);
         float alpha = this.gemAlpha[g];
         int base = this.gemColor[g];
         int apex = ColorUtil.mulAlpha(ColorUtil.lerp(base, ColorUtil.core(base), Math.min(1.0F, 0.55F + 0.35F * light)), alpha * (0.4F + 0.35F * light));
         int rimColor = ColorUtil.mulAlpha(base, alpha * (0.22F + 0.33F * light));
         Vfx.triangle(this.gx[a], this.gy[a], this.gz[a], this.gx[b], this.gy[b], this.gz[b], this.gx[c], this.gy[c], this.gz[c], apex, rimColor, rimColor);
      }

   }
}
