package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.look.LightBoost;
import mix.client.mixui.integration.haunt.haunt.fx.HauntKit;
import mix.client.mixui.integration.haunt.haunt.fx.HauntTextures;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

abstract class GraveyardBase extends KillStyleBase {
   static final int ZOMBIE_SKIN = -9532351;
   static final int ZOMBIE_NAIL = -7570098;
   static final int SOIL = -12702953;
   static final int STONE = -5197648;

   GraveyardBase(KillEffectsModule module, KillStyle type, ColorSetting color) {
      super(module, type, color);
   }

   static GraveyardBase.Frame frame(KillFx fx) {
      GraveyardBase.Frame fr = new GraveyardBase.Frame();
      double f0x = -fx.ctx.dirX;
      double f0z = -fx.ctx.dirZ;
      double l = Math.sqrt(f0x * f0x + f0z * f0z);
      if(l < 1.0E-4D) {
         f0x = 0.0D;
         f0z = 1.0D;
         l = 1.0D;
      }

      fr.fx = f0x / l;
      fr.fz = f0z / l;
      fr.rx = fr.fz;
      fr.rz = -fr.fx;
      fr.ox = fx.ctx.feetX;
      fr.oy = fx.ctx.groundY;
      fr.oz = fx.ctx.feetZ;
      if(!fx.preview && Minecraft.getMinecraft().theWorld != null) {
         try {
            BlockPos pos = new BlockPos(fx.ctx.feetX, fx.ctx.groundY + 0.5D, fx.ctx.feetZ);
            fr.light = Math.max(0.3F, LightBoost.lift(Minecraft.getMinecraft().theWorld.getLightBrightness(pos)));
         } catch (RuntimeException var9) {
            fr.light = 1.0F;
         }
      }

      return fr;
   }

   static void bury(KillFx fx, FxFrame f, float t, float from, float dur) {
      float sink = Easing.inCubic(Easing.clamp01((t - from) / dur));
      if(sink < 1.0F) {
         double down = (double)sink * 2.1D;

         for(int i = 0; i < 6; ++i) {
            BodyProxy.Part p = fx.parts[i];
            p.copy(fx.rest[i]);
            p.y -= down;
            p.clip = true;
            p.cnx = 0.0D;
            p.cny = 1.0D;
            p.cnz = 0.0D;
            p.cd = fx.ctx.groundY + 0.002D;
            p.capColor = 0;
         }

         fx.look.tint = -14741219;
         fx.look.tintAmount = 0.35F * sink;
         fx.drawBody(f);
         fx.look.tintAmount = 0.0F;
      }

   }

   static void soilBurst(KillFx fx, double x, double z, int n, float strength) {
      if(!fx.lod) {
         for(int i = 0; i < n; ++i) {
            double a = (double)fx.rng.next() * 6.2832D;
            float sp = (0.6F + fx.rng.next() * 1.4F) * strength;
            fx.particles.spawn(10, x + Math.cos(a) * 0.15D, fx.ctx.groundY + 0.05D, z + Math.sin(a) * 0.15D, (float)Math.cos(a) * sp, (1.5F + fx.rng.next() * 2.5F) * strength, (float)Math.sin(a) * sp, 0.9F + fx.rng.next() * 0.6F, (0.04F + fx.rng.next() * 0.05F) * fx.size).gravity(16.0F).ground(fx.ctx.groundY + 0.01D, 1, 0.2F).axis(fx.rng.signed(), fx.rng.signed(), fx.rng.signed()).spin(0.0F, fx.rng.signed() * 10.0F).color(ColorUtil.lerp(-12702953, -10070731, fx.rng.next())).alpha(fx.opacity).flags(8);
         }

         for(int i = 0; i < Math.max(2, n / 3); ++i) {
            double a = (double)fx.rng.next() * 6.2832D;
            fx.particles.spawn(8, x + Math.cos(a) * 0.2D, fx.ctx.groundY + 0.1D, z + Math.sin(a) * 0.2D, (float)Math.cos(a) * 0.7F * strength, 0.4F + fx.rng.next() * 0.5F, (float)Math.sin(a) * 0.7F * strength, 1.1F + fx.rng.next() * 0.6F, (0.25F + fx.rng.next() * 0.2F) * fx.size).end(0.7F * fx.size).drag(1.6F).color(-8686733).alpha(0.45F * fx.opacity);
         }

         for(int i = 0; i < n / 2; ++i) {
            double a = (double)fx.rng.next() * 6.2832D;
            fx.particles.spawn(12, x, fx.ctx.groundY + 0.05D, z, (float)Math.cos(a) * (1.0F + fx.rng.next() * 2.0F) * strength, 1.0F + fx.rng.next() * 2.5F, (float)Math.sin(a) * (1.0F + fx.rng.next() * 2.0F) * strength, 0.7F + fx.rng.next() * 0.4F, 0.03F * fx.size).gravity(14.0F).ground(fx.ctx.groundY + 0.01D, 0, 0.0F).color(-12175310).alpha(fx.opacity);
         }
      }

   }

   static void leaves(KillFx fx, double x, double y, double z, int n) {
      if(!fx.lod) {
         for(int i = 0; i < n; ++i) {
            fx.particles.spawn(9, x + (double)(fx.rng.signed() * 0.8F), y + (double)(fx.rng.next() * 0.6F), z + (double)(fx.rng.signed() * 0.8F), fx.rng.signed() * 0.4F, 0.2F + fx.rng.next() * 0.4F, fx.rng.signed() * 0.4F, 2.2F + fx.rng.next() * 1.2F, (0.08F + fx.rng.next() * 0.06F) * fx.size).gravity(0.9F).drag(1.2F).sway(0.6F, fx.rng.next() * 6.28F).texture(Textures.LEAF).ground(fx.ctx.groundY + 0.01D, 0, 0.0F).color(ColorUtil.lerp(-7779274, -11252706, fx.rng.next())).alpha(fx.opacity);
         }
      }

   }

   static void fog(KillFx fx, double x, double z, int n, float spread, int color, float alpha) {
      if(!fx.lod) {
         for(int i = 0; i < n; ++i) {
            double a = (double)fx.rng.next() * 6.2832D;
            double r = (double)(spread * (0.3F + fx.rng.next() * 0.7F));
            fx.particles.spawn(8, x + Math.cos(a) * r, fx.ctx.groundY + 0.08D + (double)(fx.rng.next() * 0.2F), z + Math.sin(a) * r, (float)Math.cos(a) * 0.15F, 0.03F + fx.rng.next() * 0.05F, (float)Math.sin(a) * 0.15F, 1.8F + fx.rng.next() * 1.0F, (0.5F + fx.rng.next() * 0.4F) * fx.size).end(1.0F * fx.size).drag(0.6F).color(color).alpha(alpha * fx.opacity);
         }
      }

   }

   static double stoneHeight(GraveyardBase.Stone kind, double k) {
      switch(kind.ordinal()) {
      case 1:
         return 1.5D * k;
      case 2:
         return 1.25D * k;
      case 3:
         return 0.78D * k;
      case 4:
         return 1.8D * k;
      default:
         return 1.05D * k;
      }
   }

   static void stone(FxFrame f, HauntKit.Xf t, GraveyardBase.Stone kind, double k, int color, float bright, float alpha) {
      int c = ColorUtil.withAlpha(color, alpha);
      int tex = Textures.STONE;
      double uv = 0.9D;
      RenderState.translucent(true);
      switch(kind.ordinal()) {
      case 1:
         HauntKit.box(f, t, tex, c, bright, uv, -0.3D * k, 0.0D, -0.2D * k, 0.3D * k, 0.18D * k, 0.2D * k);
         HauntKit.box(f, t, tex, c, bright, uv, -0.11D * k, 0.18D * k, -0.09D * k, 0.11D * k, 1.5D * k, 0.09D * k);
         HauntKit.box(f, t, tex, c, bright, uv, -0.42D * k, 0.92D * k, -0.09D * k, 0.42D * k, 1.13D * k, 0.09D * k);
         break;
      case 2:
         HauntKit.box(f, t, tex, c, bright, uv, -0.48D * k, 0.0D, -0.2D * k, 0.48D * k, 0.12D * k, 0.2D * k);
         HauntKit.box(f, t, tex, c, bright, uv, -0.38D * k, 0.12D * k, -0.1D * k, 0.38D * k, 0.85D * k, 0.1D * k);

         for(int i = 0; i < 6; ++i) {
            double y0 = 0.85D * k + (double)i * 0.065D * k;
            double w = 0.38D * k * (1.0D - (double)(i + 1) / 7.0D);
            HauntKit.box(f, t, tex, c, bright, uv, -w, y0, -0.1D * k, w, y0 + 0.065D * k, 0.1D * k);
         }

         HauntKit.box(f, t, tex, ColorUtil.withAlpha(HauntKit.darker(color, 0.8F), alpha), bright, uv, -0.18D * k, 0.4D * k, 0.1D * k, 0.18D * k, 0.72D * k, 0.11D * k);
         break;
      case 3:
         HauntKit.box(f, t, tex, c, bright, uv, -0.46D * k, 0.0D, -0.2D * k, 0.46D * k, 0.12D * k, 0.2D * k);
         HauntKit.box(f, t, tex, c, bright, uv, -0.38D * k, 0.12D * k, -0.09D * k, 0.38D * k, 0.55D * k, 0.09D * k);
         HauntKit.Xf tilt = (new HauntKit.Xf()).set(t).translate(0.06D * k, 0.55D * k, 0.0D).rotZ(-0.32D);
         HauntKit.box(f, tilt, tex, c, bright, uv, -0.3D * k, 0.0D, -0.09D * k, 0.22D * k, 0.22D * k, 0.09D * k);
         break;
      case 4:
         HauntKit.box(f, t, tex, c, bright, uv, -0.36D * k, 0.0D, -0.36D * k, 0.36D * k, 0.16D * k, 0.36D * k);
         HauntKit.box(f, t, tex, c, bright, uv, -0.2D * k, 0.16D * k, -0.2D * k, 0.2D * k, 1.5D * k, 0.2D * k);
         HauntKit.box(f, t, tex, c, bright, uv, -0.13D * k, 1.5D * k, -0.13D * k, 0.13D * k, 1.8D * k, 0.13D * k);
         break;
      default:
         HauntKit.box(f, t, tex, c, bright, uv, -0.5D * k, 0.0D, -0.22D * k, 0.5D * k, 0.14D * k, 0.22D * k);
         HauntKit.box(f, t, tex, c, bright, uv, -0.4D * k, 0.14D * k, -0.1D * k, 0.4D * k, 0.62D * k, 0.1D * k);
         HauntKit.roundTop(f, t, tex, c, bright, uv, 0.62D * k, 0.4D * k, 0.1D * k, 12);
      }

      Vfx.flush();
   }

   static double[] faceOf(GraveyardBase.Stone kind, double k) {
      switch(kind.ordinal()) {
      case 1:
         return new double[]{1.02D * k, 0.09D * k, 0.7D * k};
      case 2:
         return new double[]{0.62D * k, 0.1D * k, 0.6D * k};
      case 3:
         return new double[]{0.34D * k, 0.09D * k, 0.6D * k};
      case 4:
         return new double[]{0.85D * k, 0.2D * k, 0.32D * k};
      default:
         return new double[]{0.5D * k, 0.1D * k, 0.66D * k};
      }
   }

   static void rune(FxFrame f, HauntKit.Xf t, int index, double x, double y, double z, double size, int color) {
      float u0 = (float)(index % 8) / 8.0F;
      float u1 = u0 + 0.125F;
      double h = size * 0.5D;
      Vfx.quadUV(Textures.RUNES, f.rx(t.px(x - h, y - h, z)), f.ry(t.py(x - h, y - h, z)), f.rz(t.pz(x - h, y - h, z)), (double)u0, 1.0D, color, f.rx(t.px(x + h, y - h, z)), f.ry(t.py(x + h, y - h, z)), f.rz(t.pz(x + h, y - h, z)), (double)u1, 1.0D, color, f.rx(t.px(x + h, y + h, z)), f.ry(t.py(x + h, y + h, z)), f.rz(t.pz(x + h, y + h, z)), (double)u1, 0.0D, color, f.rx(t.px(x - h, y + h, z)), f.ry(t.py(x - h, y + h, z)), f.rz(t.pz(x - h, y + h, z)), (double)u0, 0.0D, color);
   }

   static void groundMark(FxFrame f, KillFx fx, double x, double z, double r, double rot, float alpha) {
      if(alpha > 0.01F && HauntTextures.ensure()) {
         RenderState.translucent(false);
         double gy = f.ry(fx.ctx.groundY + 0.012D);
         Vfx.flatSprite(Textures.GROUND_GLOW, f.rx(x), gy, f.rz(z), r * 1.1D, rot, ColorUtil.withAlpha(-15725298, 0.55F * alpha));
         Vfx.flatSprite(HauntTextures.CRACKS, f.rx(x), gy + 0.002D, f.rz(z), r, rot, ColorUtil.withAlpha(-15466480, 0.8F * alpha));
         Vfx.flush();
      }

   }

   static float wander(float t, float seed, float speed) {
      double v = Math.sin((double)(t * speed + seed)) * 0.5D + Math.sin((double)(t * speed * 2.31F + seed * 1.7F)) * 0.3D + Math.sin((double)(t * speed * 4.13F + seed * 0.4F)) * 0.2D;
      return (float)(v * 0.5D + 0.5D);
   }

   static final class Frame {
      double ox;
      double oy;
      double oz;
      double rx;
      double rz;
      double fx;
      double fz;
      float light = 1.0F;

      HauntKit.Xf base(HauntKit.Xf t, double localX, double localZ, double y) {
         t.identity();
         t.a = this.rx;
         t.c = this.fx;
         t.g = this.rz;
         t.i = this.fz;
         t.tx = this.ox + this.rx * localX + this.fx * localZ;
         t.ty = this.oy + y;
         t.tz = this.oz + this.rz * localX + this.fz * localZ;
         return t;
      }

      double wx(double lx, double lz) {
         return this.ox + this.rx * lx + this.fx * lz;
      }

      double wz(double lx, double lz) {
         return this.oz + this.rz * lx + this.fz * lz;
      }
   }

   static enum Stone {
      ROUNDED,
      CROSS,
      GOTHIC,
      BROKEN,
      OBELISK;

      // $FF: synthetic method
      private static GraveyardBase.Stone[] $values() {
         return new GraveyardBase.Stone[]{ROUNDED, CROSS, GOTHIC, BROKEN, OBELISK};
      }
   }
}
