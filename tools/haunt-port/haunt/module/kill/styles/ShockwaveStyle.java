package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.Terrain;
import mix.client.mixui.integration.haunt.fx.WarpShell;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Distortion;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockPos;

public final class ShockwaveStyle extends KillStyleBase {
   private static final int MAX_DEBRIS = 72;
   private static final float WAVE_SECONDS = 0.75F;
   private final SliderSetting radius = (SliderSetting)this.setting((new SliderSetting("shockwave.radius", "Radius", "How far the ring races out.", 3.0D, 10.0D, 6.0D, 0.5D)).unit(" blocks"));
   private final SliderSetting strength = (SliderSetting)this.setting((new SliderSetting("shockwave.strength", "Strength", "How hard the body is thrown and the debris lifted.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting debris = (SliderSetting)this.setting((new SliderSetting("shockwave.debris", "Debris", "How much ground is lifted.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting distortion = (BooleanSetting)this.setting(new BooleanSetting("shockwave.distortion", "Distortion", "The swelling shell bends the scene behind it.", true));
   private final double[] m = new double[9];
   private static final int[][] FACES = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
   private final double[] px = new double[8];
   private final double[] py = new double[8];
   private final double[] pz = new double[8];

   public ShockwaveStyle(KillEffectsModule module) {
      super(module, KillStyle.SHOCKWAVE, new ColorSetting("shockwave.color", "Color", "Color of the flash and the ring.", -18340));
   }

   public float seconds(KillFx fx) {
      return 1.9F;
   }

   public boolean leavesBody() {
      return true;
   }

   private double reach(KillFx fx) {
      return Math.min(fx.preview?2.4D:1.0E9D, (double)(this.radius.getFloat() * fx.size) * (fx.crit?1.3D:1.0D));
   }

   public void start(KillFx fx) {
      ShockwaveStyle.State s = new ShockwaveStyle.State();
      fx.data = s;
      Minecraft mc = Minecraft.getMinecraft();
      WorldClient world = fx.preview?null:mc.theWorld;
      double r = this.reach(fx);
      s.ground = world == null?Terrain.HeightField.flat(fx.ctx.groundY):Terrain.HeightField.sample(world, fx.ctx.feetX, fx.ctx.groundY, fx.ctx.feetZ, r + 1.0D, 1);
      s.texture = Textures.STONE;
      s.u0 = 0.0F;
      s.v0 = 0.0F;
      s.du = 1.0F;
      s.dv = 1.0F;
      if(world != null) {
         try {
            BlockPos below = new BlockPos(fx.ctx.feetX, fx.ctx.groundY - 0.5D, fx.ctx.feetZ);
            IBlockState state = world.getBlockState(below);
            TextureAtlasSprite sprite;
            if(state.getBlock().getMaterial().isSolid() && (sprite = mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(state)) != null) {
               s.texture = BodyProxy.texture(TextureMap.locationBlocksTexture);
               s.u0 = sprite.getMinU();
               s.v0 = sprite.getMinV();
               s.du = sprite.getMaxU() - sprite.getMinU();
               s.dv = sprite.getMaxV() - sprite.getMinV();
            }
         } catch (RuntimeException var14) {
            ;
         }
      }

      Rng rng = fx.rng;
      s.count = Math.min(72, fx.count(30.0F * this.debris.getFloat() * (float)(r / 6.0D), true));
      if(this.debris.getFloat() <= 0.0F) {
         s.count = 0;
      }

      for(int i = 0; i < s.count; ++i) {
         s.ringAngle[i] = rng.next() * 6.2832F;
         float dist = (float)(r * (0.18D + 0.8D * Math.sqrt((double)rng.next())));
         s.born[i] = 0.75F * (float)Math.pow((double)dist / r, 1.5D);
         s.x[i] = fx.ctx.feetX + Math.cos((double)s.ringAngle[i]) * (double)dist;
         s.z[i] = fx.ctx.feetZ + Math.sin((double)s.ringAngle[i]) * (double)dist;
         double g = s.ground.at(s.x[i], s.z[i]);
         s.y[i] = Double.isNaN(g)?fx.ctx.groundY:g;
         float lift = this.strength.getFloat() * (1.0F - 0.5F * dist / (float)r);
         s.vx[i] = (float)Math.cos((double)s.ringAngle[i]) * (1.5F + rng.next() * 2.0F) * lift;
         s.vz[i] = (float)Math.sin((double)s.ringAngle[i]) * (1.5F + rng.next() * 2.0F) * lift;
         s.vy[i] = (3.5F + rng.next() * 3.5F) * lift;

         float axl;
         while(true) {
            s.ax[i] = rng.signed();
            s.ay[i] = rng.signed();
            s.az[i] = rng.signed();
            if((axl = (float)Math.sqrt((double)(s.ax[i] * s.ax[i] + s.ay[i] * s.ay[i] + s.az[i] * s.az[i]))) >= 0.1F) {
               break;
            }
         }

         s.ax[i] /= axl;
         s.ay[i] /= axl;
         s.az[i] /= axl;
         s.spin[i] = rng.signed() * 10.0F;
         s.angle[i] = rng.next() * 6.2832F;
         s.size[i] = (0.07F + rng.next() * 0.09F) * fx.size;
         s.u[i] = rng.next() * 0.75F;
         s.v[i] = rng.next() * 0.75F;
         s.landed[i] = -1.0F;
      }

      fx.ragdoll().fling(fx.ctx.impactX, fx.ctx.impactY, fx.ctx.impactZ, fx.ctx.body.vx * 0.5D, 0.0D, fx.ctx.body.vz * 0.5D, fx.ctx.dirX, Math.max(-0.3D, fx.ctx.dirY) + 0.25D, fx.ctx.dirZ, 7.5D * (double)this.strength.getFloat() * (fx.crit?1.4D:1.0D) * (fx.preview?0.35D:1.0D), 4.5D * (double)this.strength.getFloat());
      if(!fx.lod) {
         fx.burst(fx.ctx.midX(), fx.ctx.midY(), fx.ctx.midZ(), fx.ctx.dirX, fx.ctx.dirY, fx.ctx.dirZ, fx.count(14.0F, true), 6.0F, 0.9F);
      }

   }

   public void update(KillFx param1, float param2) {
   }

   public void render(KillFx fx, FxFrame f) {
      ShockwaveStyle.State s = (ShockwaveStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      double r = this.reach(fx);
      double cx = f.rx(fx.ctx.midX());
      double cy = f.ry(fx.ctx.midY());
      double cz = f.rz(fx.ctx.midZ());
      float flash = 1.0F - Easing.smoothstep(0.0F, 0.25F, t);
      fx.look.glow = this.color.getColor(0.5F);
      fx.look.glowAmount = 0.8F * flash;
      fx.drawBody(f);
      RenderState.additive();
      if(flash > 0.01F) {
         float g = Math.max(0.35F, fx.glow);
         Vfx.sprite(Textures.GLOW, cx, cy, cz, 5.5D * (double)fx.size * (fx.crit?1.3D:1.0D), 0.0F, alpha(this.color.getColor(0.5F), 0.6F * a * flash * g));
         Vfx.sprite(Textures.FLARE, cx, cy, cz, 3.2D * (double)fx.size, t * 2.0F, core(this.color.getColor(0.5F), 0.9F * a * flash));
      }

      float shellT;
      if((shellT = Easing.clamp01(t / 0.6F)) < 1.0F && !fx.lod) {
         double sr = 0.4D + r * 0.55D * (double)Easing.outCubic(shellT);
         float sa = a * (1.0F - shellT);
         if(this.distortion.get()) {
            WarpShell.draw(f, fx.ctx.midX(), fx.ctx.midY(), fx.ctx.midZ(), sr, 0.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.0D, 1.0D, 0.0D, 0.0D, 1.0D, false, 1.4F, this.color.getColor(0.5F), 0.6F, sa, Distortion.refracts() && !f.preview);
            RenderState.additive();
         } else {
            Vfx.sprite(Textures.BUBBLE, cx, cy, cz, sr * 2.0D, 0.0F, alpha(this.color.getColor(0.5F), 0.5F * sa));
         }
      }

      float waveT;
      if((waveT = Easing.clamp01(t / 0.75F)) < 1.0F || t < 1.15F) {
         double wr = r * Math.pow((double)waveT, 0.66D);
         float wa = a * (1.0F - Easing.smoothstep(0.55F, 1.0F, Easing.clamp01(t / 1.15F)));
         if(wa > 0.01F && wr > 0.05D) {
            Neon.ring(f, s.ground, fx.ctx.feetX, fx.ctx.feetZ, wr, 0.12D * (double)fx.size, 72, this.color, 0, wa, t * 0.2F);
            RenderState.decal(true);
            double band = 0.9D * (double)fx.size;
            Vfx.flatBand(Textures.WAVE, f.rx(fx.ctx.feetX), f.ry(fx.ctx.groundY) + 0.03D, f.rz(fx.ctx.feetZ), Math.max(0.0D, wr - band), wr, 0.0D, 6.283185307179586D, 72, alpha(this.color.getColor(0.3F), 0.55F * wa));
            RenderState.decal(false);
         }
      }

      this.drawDebris(fx, f, s, a);
   }

   private void drawDebris(KillFx fx, FxFrame f, ShockwaveStyle.State s, float a) {
      if(s.count != 0) {
         RenderState.translucent(true);

         for(int i = 0; i < s.count; ++i) {
            if(fx.t >= s.born[i]) {
               float fade = s.landed[i] < 0.0F?1.0F:1.0F - Easing.smoothstep(0.35F, 0.9F, fx.t - s.landed[i]);
               float grow = Easing.outCubic(Easing.clamp01((fx.t - s.born[i]) / 0.08F));
               float alpha = a * fade * (1.0F - Easing.smoothstep(fx.styleSeconds - 0.3F, fx.styleSeconds, fx.t));
               if(alpha > 0.01F) {
                  axisAngle((double)s.ax[i], (double)s.ay[i], (double)s.az[i], (double)s.angle[i], this.m);
                  double h = (double)(s.size[i] * grow);
                  float u0 = s.u0 + s.u[i] * s.du;
                  float v0 = s.v0 + s.v[i] * s.dv;
                  float u1 = u0 + s.du * 0.25F;
                  float v1 = v0 + s.dv * 0.25F;
                  this.cube(f, s.texture, s.x[i], s.y[i], s.z[i], h, u0, v0, u1, v1, alpha);
               }
            }
         }

         Vfx.flush();
         RenderState.additive();
      }

   }

   private static void axisAngle(double x, double y, double z, double angle, double[] out) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      double t = 1.0D - c;
      out[0] = t * x * x + c;
      out[1] = t * x * y - s * z;
      out[2] = t * x * z + s * y;
      out[3] = t * x * y + s * z;
      out[4] = t * y * y + c;
      out[5] = t * y * z - s * x;
      out[6] = t * x * z - s * y;
      out[7] = t * y * z + s * x;
      out[8] = t * z * z + c;
   }

   private void cube(FxFrame f, int texture, double x, double y, double z, double h, float u0, float v0, float u1, float v1, float alpha) {
      for(int c = 0; c < 8; ++c) {
         double lx = (c & 1) != 0?h:-h;
         double ly = (c & 2) != 0?h:-h;
         double lz = (c & 4) != 0?h:-h;
         this.px[c] = f.rx(x) + this.m[0] * lx + this.m[1] * ly + this.m[2] * lz;
         this.py[c] = f.ry(y) + this.m[3] * lx + this.m[4] * ly + this.m[5] * lz;
         this.pz[c] = f.rz(z) + this.m[6] * lx + this.m[7] * ly + this.m[8] * lz;
      }

      for(int[] face : FACES) {
         double ay = this.py[face[1]] - this.py[face[0]];
         double bz = this.pz[face[3]] - this.pz[face[0]];
         double az = this.pz[face[1]] - this.pz[face[0]];
         double by = this.py[face[3]] - this.py[face[0]];
         double nx = ay * bz - az * by;
         double bx = this.px[face[3]] - this.px[face[0]];
         double ax = this.px[face[1]] - this.px[face[0]];
         double ny = az * bx - ax * bz;
         double nz = ax * by - ay * bx;
         double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
         if(nl >= 1.0E-12D) {
            double mx = (this.px[face[0]] + this.px[face[2]]) * 0.5D - f.rx(x);
            if(nx * mx + ny * ((this.py[face[0]] + this.py[face[2]]) * 0.5D - f.ry(y)) + nz * ((this.pz[face[0]] + this.pz[face[2]]) * 0.5D - f.rz(z)) < 0.0D) {
               nx = -nx;
               ny = -ny;
               nz = -nz;
            }

            if(nx * (this.px[face[0]] - Vfx.camera().posX) + ny * (this.py[face[0]] - Vfx.camera().posY) + nz * (this.pz[face[0]] - Vfx.camera().posZ) <= 0.0D) {
               float shade = (float)(0.62D + 0.38D * Math.max(0.0D, ny / nl) + 0.12D * Math.abs(nx / nl));
               int g = Math.min(255, Math.round(255.0F * shade));
               int c = Math.max(0, Math.min(255, Math.round(alpha * 255.0F))) << 24 | g << 16 | g << 8 | g;
               Vfx.quadUV(texture, this.px[face[0]], this.py[face[0]], this.pz[face[0]], (double)u0, (double)v0, c, this.px[face[1]], this.py[face[1]], this.pz[face[1]], (double)u1, (double)v0, c, this.px[face[2]], this.py[face[2]], this.pz[face[2]], (double)u1, (double)v1, c, this.px[face[3]], this.py[face[3]], this.pz[face[3]], (double)u0, (double)v1, c);
            }
         }
      }

   }

   private static final class State {
      final double[] x;
      final double[] y;
      final double[] z;
      final float[] vx;
      final float[] vy;
      final float[] vz;
      final float[] ax;
      final float[] ay;
      final float[] az;
      final float[] angle;
      final float[] spin;
      final float[] size;
      final float[] born;
      final float[] landed;
      final float[] u;
      final float[] v;
      final float[] ringAngle;
      int count;
      int spawned;
      Terrain.HeightField ground;
      int texture;
      float u0;
      float v0;
      float du;
      float dv;
      int dustDone;

      private State() {
         this.x = new double[72];
         this.y = new double[72];
         this.z = new double[72];
         this.vx = new float[72];
         this.vy = new float[72];
         this.vz = new float[72];
         this.ax = new float[72];
         this.ay = new float[72];
         this.az = new float[72];
         this.angle = new float[72];
         this.spin = new float[72];
         this.size = new float[72];
         this.born = new float[72];
         this.landed = new float[72];
         this.u = new float[72];
         this.v = new float[72];
         this.ringAngle = new float[72];
      }
   }
}
