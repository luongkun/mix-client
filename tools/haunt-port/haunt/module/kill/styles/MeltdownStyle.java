package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Liquid;
import mix.client.mixui.integration.haunt.fx.look.KeepVisible;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.look.BodyLook;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class MeltdownStyle extends KillStyleBase {
   private static final float SWEPT = 0.4F;
   private static final float STATUE = 0.7F;
   private static final float MELT = 0.7F;
   private static final float RIPPLE = 0.4F;
   private static final float SPHERE = 0.4F;
   private static final float FADE = 0.08F;
   private static final float GLINT_FROM = 0.45F;
   private static final int BALLS = 11;
   private static final int MAX_DROPLETS = 8;
   private static final int CROWN = 8;
   private static final int MAX = 28;
   private static final double PUDDLE = 0.85D;
   private static final double SPHERE_R = 0.3D;
   private static final double SAG = 0.3D;
   private static final float RIM = 0.22F;
   public final ModeSetting<Material> material = this.materialSetting("meltdown", Material.CHROME, "What the body melts into. Custom takes the Tint, with a finish.");
   public final ModeSetting<Finish> finish;
   public final SliderSetting meltSpeed;
   public final SliderSetting puddleSize;
   public final SliderSetting droplets;
   public final ModeSetting<MeltdownStyle.Quality> quality;
   private static final int[] PER_PART = new int[]{1, 2, 2, 2, 2, 2};
   private static final double[] SPACING = new double[]{0.0D, 0.18D, 0.17D, 0.17D, 0.17D, 0.17D};
   private static final double[] RADIUS = new double[]{0.26D, 0.21D, 0.12D, 0.12D, 0.13D, 0.13D};

   public MeltdownStyle(KillEffectsModule module) {
      super(module, KillStyle.MELTDOWN, new ColorSetting("meltdown.color", "Tint", "The metal\'s color with the Custom material.", ColorSetting.Mode.STATIC, -2300950, -6310712, 1.0D));
      this.finish = this.finishSetting("meltdown", Finish.METALLIC, this.material);
      this.color.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return MeltdownStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.signature(this.color, -2300950);
      this.meltSpeed = this.look((SliderSetting)this.setting((new SliderSetting("meltdown.meltSpeed", "Melt Speed", "How fast the statue melts into a puddle.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.puddleSize = this.look((SliderSetting)this.setting((new SliderSetting("meltdown.puddleSize", "Puddle Size", "How far the puddle spreads.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
      this.droplets = (SliderSetting)this.setting(new SliderSetting("meltdown.droplets", "Droplets", "How many droplets jump out of the puddle.", 0.0D, 8.0D, 4.0D, 1.0D));
      this.quality = (ModeSetting)this.setting(new ModeSetting("meltdown.quality", "Quality", "How fine the liquid\'s surface is: Low, Medium or High (a little more work each frame).", MeltdownStyle.Quality.class, MeltdownStyle.Quality.MEDIUM));
   }

   private float melt() {
      return 70.0F / this.meltSpeed.getFloat();
   }

   public float seconds(KillFx fx) {
      return 0.7F + this.melt() + 0.4F + 0.4F;
   }

   public int passes(KillFx fx) {
      int body = ((Material)this.material.get()).translucent((Finish)this.finish.get())?FxPass.TRANSLUCENT.bit():FxPass.SOLID.bit();
      return body | FxPass.SOLID.bit() | FxPass.GLOW.bit();
   }

   public void start(KillFx fx) {
      MeltdownStyle.State s = new MeltdownStyle.State();
      double ax = fx.ctx.dirX;
      double az = fx.ctx.dirZ;
      double l = Math.sqrt(ax * ax + az * az);
      if(l < 1.0E-4D) {
         ax = 0.0D;
         az = 1.0D;
         l = 1.0D;
      }

      s.awayX = ax / l;
      s.awayZ = az / l;
      double size = (double)fx.size;
      double puddle = 0.85D * (double)this.puddleSize.getFloat() / 100.0D * size;
      s.cx = fx.ctx.feetX + s.awayX * 0.3D * size;
      s.cz = fx.ctx.feetZ + s.awayZ * 0.3D * size;
      int b = 0;

      for(int part = 0; part < 6; ++part) {
         BodyProxy.Part q = fx.rest[part];

         for(int k = 0; k < PER_PART[part]; ++k) {
            double along = PER_PART[part] == 1?0.0D:(double)(k == 0?-1:1) * SPACING[part];
            s.sx[b] = q.x + q.r[1] * along;
            s.sy[b] = q.y + q.r[4] * along;
            s.sz[b] = q.z + q.r[7] * along;
            s.r[b] = RADIUS[part] * size;
            double ox = s.sx[b] - fx.ctx.feetX;
            double oz = s.sz[b] - fx.ctx.feetZ;
            double angle = Math.atan2(oz, ox) + (hash(fx.seed, b * 3 + 1) - 0.5D) * 1.2D;
            if(ox * ox + oz * oz < 1.0E-4D) {
               angle = 6.283185307179586D * hash(fx.seed, b * 3 + 2);
            }

            double reach = puddle * (0.25D + 0.6D * hash(fx.seed, b * 3 + 3));
            s.px[b] = s.cx + Math.cos(angle) * reach;
            s.pz[b] = s.cz + Math.sin(angle) * reach;
            ++b;
         }
      }

      s.droplets = fx.lod?this.droplets.getInt() / 2:this.droplets.getInt();

      for(int i = 0; i < s.droplets; ++i) {
         double a = 6.283185307179586D * hash(fx.seed, 100 + i);
         double r = puddle * (0.2D + 0.55D * hash(fx.seed, 101 + i));
         s.dx[i] = s.cx + Math.cos(a) * r;
         s.dz[i] = s.cz + Math.sin(a) * r;
         s.dUp[i] = (1.1D + 0.7D * hash(fx.seed, 102 + i)) * Math.sqrt(size);
         s.dAt[i] = (float)(0.05D + 0.5D * hash(fx.seed, 103 + i));
      }

      fx.data = s;
   }

   private float meltEnd() {
      return 0.7F + this.melt();
   }

   private float rippleEnd() {
      return this.meltEnd() + 0.4F;
   }

   private void balls(KillFx fx, FxFrame f, MeltdownStyle.State s) {
      Liquid.Field fl = s.field;
      float t = fx.t;
      fl.begin(1.9D);
      if(t >= 0.68F) {
         double ground = fx.ctx.groundY;
         float melt = fx.phase(0.7F, this.meltEnd());
         float ripple = fx.phase(this.meltEnd(), this.rippleEnd());
         float sphere = fx.phase(this.rippleEnd(), this.rippleEnd() + 0.4F);
         double size = (double)fx.size;
         float gather = Easing.inOutCubic(Easing.clamp01(sphere / 0.4F));
         double sphereR = 0.3D * Math.cbrt((double)this.puddleSize.getFloat() / 100.0D) * size;
         double hop = sphere > 0.4F && sphere < 0.8F?Math.sin(3.141592653589793D * (double)(sphere - 0.4F) / 0.4000000059604645D) * 0.35D * size:0.0D;
         double vanish = 1.0D - (double)Easing.inCubic(Easing.clamp01((sphere - 0.75F) / 0.25F));

         for(int b = 0; b < 11; ++b) {
            double h = s.sy[b] - ground;
            double q = (double)Easing.inQuad(melt);
            double flat = 1.0D + 1.6D * q;
            double r = s.r[b] * (1.0D + 0.45D * q);
            double y = ground + r / flat * 0.9D + (h - r / flat * 0.9D) * (1.0D - q);
            double spread = (double)Easing.inOutCubic(melt);
            double x = s.sx[b] + (s.px[b] - s.sx[b]) * spread + s.awayX * 0.3D * size * 0.3D * Math.sin(3.141592653589793D * (double)melt);
            double z = s.sz[b] + (s.pz[b] - s.sz[b]) * spread + s.awayZ * 0.3D * size * 0.3D * Math.sin(3.141592653589793D * (double)melt);
            if(ripple > 0.0F && sphere <= 0.0F) {
               double d = Math.sqrt((x - s.cx) * (x - s.cx) + (z - s.cz) * (z - s.cz));
               y += 0.012D * size * Math.sin(d * 18.0D - (double)ripple * 20.0D) * (1.0D - (double)ripple);
            }

            if(sphere > 0.0F) {
               x += (s.cx - x) * (double)gather;
               z += (s.cz - z) * (double)gather;
               y += (ground + sphereR + hop - y) * (double)gather;
               flat += (1.0D - flat) * (double)gather;
               r = r + (sphereR * 0.62D - r) * (double)gather;
               r = r * vanish;
            }

            if(r >= 0.004D) {
               fl.ball(f.rx(x), f.ry(y), f.rz(z), r, 1.0D, 0.0D, 1.0D, 0.0D, flat);
            }
         }

         if(ripple > 0.0F && sphere < 0.3F) {
            double radius = 0.85D * (double)this.puddleSize.getFloat() / 100.0D * size * 0.75D * (1.0D - (double)gather);
            double w = (double)ripple * 30.0D;
            fl.ring(f.rx(s.cx), f.ry(ground + 0.03D * size), f.rz(s.cz), radius, 0.05D * size, 0.8D * (1.0D - (double)gather), 0.025D * size * (1.0D - (double)ripple), Math.cos(w), Math.sin(w));
         }

         float rt = t - this.meltEnd();

         for(int i = 0; i < s.droplets; ++i) {
            double air = (double)(rt - s.dAt[i] * 0.4F);
            double up = s.dUp[i] * air - 4.5D * air * air;
            if(air > 0.0D && up > 0.0D) {
               fl.ball(f.rx(s.dx[i]), f.ry(ground + 0.04D + up), f.rz(s.dz[i]), 0.045D * size, 1.0D, 0.0D, 1.0D, 0.0D, 0.85D);
            }
         }

         float c;
         if(fx.crit && (c = fx.phase(this.meltEnd() - 0.05F, this.meltEnd() + 0.3F)) > 0.0F && c < 1.0F) {
            double radius = 0.85D * (double)this.puddleSize.getFloat() / 100.0D * size * 0.55D;
            double up = Math.sin(3.141592653589793D * (double)c) * 0.28D * size;

            for(int ix = 0; ix < 8; ++ix) {
               double a = 6.283185307179586D * (double)ix / 8.0D + (double)fx.seed;
               fl.ball(f.rx(s.cx + Math.cos(a) * radius * (1.0D + 0.3D * (double)c)), f.ry(ground + 0.05D + up), f.rz(s.cz + Math.sin(a) * radius * (1.0D + 0.3D * (double)c)), 0.04D * size, 1.0D, 0.0D, 1.0D, 0.0D, 0.7D);
            }
         }
      }

   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      MeltdownStyle.State s = (MeltdownStyle.State)fx.data;
      if(s != null) {
         if(pass == FxPass.GLOW) {
            this.glow(fx, f, s);
         } else {
            boolean translucent = ((Material)this.material.get()).translucent((Finish)this.finish.get());
            if(pass == FxPass.SOLID) {
               this.statue(fx, f);
            }

            if(pass == FxPass.TRANSLUCENT == translucent) {
               this.balls(fx, f, s);
               if(s.field.balls() != 0) {
                  s.field.plain = fx.lod;
                  boolean custom = this.material.get() == Material.CUSTOM;
                  int tint = custom?this.color.getColor(0.5F):-1;
                  float a = (custom?fx.alpha() * (float)(tint >>> 24) / 255.0F:fx.alpha()) * fx.phase(0.68F, 0.74F);
                  double cells = (double)((MeltdownStyle.Quality)this.quality.get()).cells;
                  double cell = 1.9D * (double)fx.size / cells;
                  MeshBatch.begin((Material)this.material.get(), (Finish)this.finish.get(), fx.light, 0.22F, -1, true, pass == FxPass.SOLID);
                  s.field.mesh(cell, f.rx(s.cx), f.ry(fx.ctx.groundY), f.rz(s.cz), f.distanceSqToEye(s.cx, fx.ctx.groundY, s.cz), a, tint, (KeepVisible)null);
                  MeshBatch.end();
               }
            }
         }
      }

   }

   private void statue(KillFx fx, FxFrame f) {
      float t = fx.t;
      float gone = fx.phase(0.7F, 0.78F);
      if(gone < 1.0F) {
         BodyProxy.Look look = fx.look;
         look.material = (Material)this.material.get();
         look.finish = (Finish)this.finish.get();
         look.materialTint = this.color.getColor(0.5F) | -16777216;
         look.worldLight = fx.light;
         look.glint = fx.crit?0.1F + 0.45F * (float)Math.sin(3.141592653589793D * (double)fx.phase(0.45F, 0.7F)):0.1F;
         look.roundness = 0.9F;
         look.reflectTilt = 0.35F;
         if(t < 0.4F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = BodyLook.MATERIAL;
            look.sweep = Easing.inOutCubic(t / 0.4F);
            look.sweepMode = 0;
            look.sweepOX = fx.ctx.feetX;
            look.sweepOY = fx.ctx.feetY;
            look.sweepOZ = fx.ctx.feetZ;
            look.sweepSpan = 1.95F;
            look.sweepEdgeColor = -1;
         } else {
            look.kind = BodyLook.MATERIAL;
            look.sweepTo = null;
         }

         look.dissolve = -1.0F;
         float before = look.alpha;
         look.alpha = 1.0F - gone;
         fx.drawBody(f);
         look.alpha = before;
         RenderState.translucent(true);
      }

   }

   private void glow(KillFx fx, FxFrame f, MeltdownStyle.State s) {
      float alpha = fx.alpha();
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      BodyProxy.Part b = fx.rest[1];
      float g;
      if(fx.crit && (g = fx.phase(0.45F, 0.7F)) > 0.0F && g < 1.0F) {
         Camera cam = Vfx.camera();
         double y = fx.ctx.feetY + 1.95D * (double)fx.size * (double)g;
         double x = f.rx(b.x);
         double yy = f.ry(y);
         double z = f.rz(b.z);
         double tx = cam.posX - x;
         double ty = cam.posY - yy;
         double tz = cam.posZ - z;
         double tl = Math.max(0.001D, Math.sqrt(tx * tx + ty * ty + tz * tz));
         double pull = Math.min(0.4D, tl * 0.5D) / tl;
         Vfx.sprite(Textures.FLARE, x + tx * pull, yy + ty * pull, z + tz * pull, 0.9D * (double)fx.size, 0.2F, core(-1, (calm?0.35F:0.75F) * alpha));
      }

      float flash;
      if((flash = 1.0F - Easing.clamp01(Math.abs(fx.t - 0.74F) / 0.09F)) > 0.0F) {
         Vfx.sprite(Textures.GLOW, f.rx(b.x), f.ry(b.y), f.rz(b.z), 2.4D * (double)fx.size, 0.0F, alpha(-1, (calm?0.2F:0.55F) * flash * alpha));
      }

      float sphere;
      if((sphere = fx.phase(this.rippleEnd(), this.rippleEnd() + 0.4F)) > 0.75F && sphere < 1.0F) {
         float k = (float)Math.sin(3.141592653589793D * (double)(sphere - 0.75F) / 0.25D);
         double y = fx.ctx.groundY + 0.3D * (double)fx.size;
         Vfx.sprite(Textures.SPARKLE, f.rx(s.cx), f.ry(y), f.rz(s.cz), 0.6D * (double)fx.size, 0.1F, core(-1, 0.6F * k * alpha));
      }

   }

   public String stateForTest(KillFx fx) {
      MeltdownStyle.State s = (MeltdownStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.4F?"turning to metal":(t < 0.7F?"a statue":(t < this.meltEnd()?"melting":(t < this.rippleEnd()?"the puddle ripples":"the sphere")));
         Liquid.Field fl = s.field;
         String how = fl.lastMode == 0?"meshed":(fl.lastMode == 1?"its last mesh":(fl.lastMode == 2?"spheres":"nothing"));
         return String.format(Locale.ROOT, "%s; %s, %d balls (%s: %d vertices, %d points), %d droplets, %s quality%s", new Object[]{phase, ((Material)this.material.get()).displayName(), Integer.valueOf(fl.balls()), how, Integer.valueOf(fl.lastVertices), Integer.valueOf(fl.lastPoints), Integer.valueOf(s.droplets), ((MeltdownStyle.Quality)this.quality.get()).displayName(), fx.crit?", crit":""});
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   public static enum Quality implements Displayable {
      LOW("Low", "16 cells across the body", 16),
      MEDIUM("Medium", "24 cells across the body", 24),
      HIGH("High", "32 cells across the body", 32);

      private final String displayName;
      private final String description;
      final int cells;

      private Quality(String displayName, String description, int cells) {
         this.displayName = displayName;
         this.description = description;
         this.cells = cells;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }

   private static final class State {
      final Liquid.Field field;
      final double[] sx;
      final double[] sy;
      final double[] sz;
      final double[] r;
      final double[] px;
      final double[] pz;
      double cx;
      double cz;
      double awayX;
      double awayZ;
      int droplets;
      final double[] dx;
      final double[] dz;
      final double[] dUp;
      final float[] dAt;

      private State() {
         this.field = new Liquid.Field(28);
         this.sx = new double[11];
         this.sy = new double[11];
         this.sz = new double[11];
         this.r = new double[11];
         this.px = new double[11];
         this.pz = new double[11];
         this.dx = new double[8];
         this.dz = new double[8];
         this.dUp = new double[8];
         this.dAt = new float[8];
      }
   }
}
