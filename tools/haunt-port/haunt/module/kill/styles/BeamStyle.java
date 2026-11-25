package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Neon;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.module.kill.SkinDust;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.DissolveShader;
import mix.client.mixui.integration.haunt.render.Distortion;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Procedural;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class BeamStyle extends KillStyleBase {
   private final ModeSetting<BeamStyle.Variant> variant = (ModeSetting)this.setting((new ModeSetting("beam.variant", "Variant", "Which beam.", BeamStyle.Variant.class, BeamStyle.Variant.PILLAR)).style(ModeSetting.Style.CHIPS));
   private final SliderSetting width = (SliderSetting)this.setting((new SliderSetting("beam.width", "Width", "How thick the beam is.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting height = (SliderSetting)this.setting((new SliderSetting("beam.height", "Height", "How high up the beam starts.", 10.0D, 60.0D, 30.0D, 1.0D)).unit(" blocks"));
   private final SliderSetting duration = (SliderSetting)this.setting((new SliderSetting("beam.duration", "Duration", "How long the beam lasts.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private static final int DUST = 420;
   private final Rng boltRng = new Rng();
   private static final float[][] FLICKERS = new float[][]{{0.0F, 0.08F}, {0.13F, 0.2F}, {0.29F, 0.34F}, {0.42F, 0.46F}};

   public BeamStyle(KillEffectsModule module) {
      super(module, KillStyle.BEAM, new ColorSetting("beam.color", "Color", "Color of the beam\'s glow.", -8399617));
   }

   private BeamStyle.Variant variant(KillFx fx) {
      return (BeamStyle.Variant)this.variant.get();
   }

   public float seconds(KillFx fx) {
      float base = this.variant.get() == BeamStyle.Variant.LIGHTNING?2.2F:(this.variant.get() == BeamStyle.Variant.ORBITAL?2.6F:2.3F);
      return base * this.duration.getFloat();
   }

   private float time(KillFx fx) {
      return fx.t / this.duration.getFloat();
   }

   private double beamWidth(KillFx fx) {
      return 0.55D * (double)this.width.getFloat() * (double)fx.size * (fx.crit?1.5D:1.0D);
   }

   public void start(final KillFx fx) {
      BeamStyle.State s = new BeamStyle.State();
      fx.data = s;
      final boolean up = this.variant.get() != BeamStyle.Variant.LIGHTNING;
      final float share = this.noiseShare();
      s.dust.sample(fx, fx.count(this.variant.get() == BeamStyle.Variant.LIGHTNING?320.0F:260.0F, true), new SkinDust.Threshold() {
         public float at(double x, double y, double z) {
            float h = Easing.clamp01((float)((y - fx.ctx.feetY) / 2.0D));
            float noise = DissolveShader.noiseAt(x - fx.ctx.body.pose.x, y - fx.ctx.body.pose.y, z - fx.ctx.body.pose.z);
            return DissolveShader.thresholdAt(up?h:1.0F - h, noise, share, 0.08F);
         }
      });

      for(int i = 0; i < s.arc.length; ++i) {
         s.arc[i] = fx.rng.next() * 6.2832F;
      }

   }

   private float noiseShare() {
      return this.variant.get() == BeamStyle.Variant.LIGHTNING?0.7F:0.3F;
   }

   private float dissolve(KillFx fx) {
      float t = this.time(fx);
      switch(this.variant(fx)) {
      case LIGHTNING:
         return Easing.smoothstep(0.0F, 1.0F, Easing.clamp01((t - 0.62F) / 1.0F));
      case ORBITAL:
         return Easing.clamp01((t - 0.5F) / 0.55F);
      default:
         return Easing.clamp01((t - 0.35F) / 1.05F);
      }
   }

   public void update(KillFx fx, float dt) {
      BeamStyle.State s = (BeamStyle.State)fx.data;
      BeamStyle.Variant v = this.variant(fx);
      float t = this.time(fx);
      float p = this.dissolve(fx);
      Rng rng = fx.rng;
      double lift = v == BeamStyle.Variant.PILLAR?liftOf(t):0.0D;
      SkinDust d = s.dust;

      while(d.next < d.count && d.at[d.next] <= p) {
         int i = d.next++;
         double x = fx.ctx.feetX + (double)d.x[i];
         double y = fx.ctx.feetY + (double)d.y[i] + lift;
         double z = fx.ctx.feetZ + (double)d.z[i];
         switch(v) {
         case LIGHTNING:
            fx.particles.spawn(12, x, y, z, rng.signed() * 0.3F, 0.3F + rng.next() * 0.6F, rng.signed() * 0.3F, 1.2F + rng.next() * 0.8F, (0.03F + rng.next() * 0.03F) * fx.size).drag(0.8F).end(0.01F).color(ColorUtil.lerp(-14935528, -11909566, rng.next())).alpha(fx.opacity).fade(0.0F, 0.5F);
            if(rng.next() < 0.3F) {
               fx.particles.spawn(7, x, y, z, rng.signed() * 0.4F, 0.5F + rng.next() * 0.7F, rng.signed() * 0.4F, 0.6F + rng.next() * 0.6F, (0.04F + rng.next() * 0.03F) * fx.size).drag(1.0F).color(ColorUtil.lerp(-38374, -19641, rng.next())).alpha(fx.opacity).hot(0.9F);
            }
            break;
         case ORBITAL:
            fx.particles.spawn(7, x, y, z, rng.signed() * 0.8F, 2.0F + rng.next() * 3.0F, rng.signed() * 0.8F, 0.5F + rng.next() * 0.6F, (0.04F + rng.next() * 0.04F) * fx.size).drag(1.3F).color(ColorUtil.lerp(d.color[i], this.color.getColor(rng.next()), 0.6F)).alpha(fx.opacity).hot(0.8F);
            break;
         default:
            fx.particles.spawn(3, x, y, z, rng.signed() * 0.15F, 1.2F + rng.next() * 1.6F, rng.signed() * 0.15F, 0.8F + rng.next() * 0.8F, (0.05F + rng.next() * 0.04F) * fx.size).drag(0.4F).color(ColorUtil.lerp(d.color[i], this.color.getColor(rng.next()), 0.55F)).alpha(fx.opacity).hot(0.6F);
         }
      }

      if(v != BeamStyle.Variant.LIGHTNING && !fx.lod && this.beamOpen(fx) > 0.3F && rng.next() < dt * 40.0F) {
         double r = this.beamWidth(fx) * 0.45D * (double)rng.next();
         double a = (double)rng.next() * 6.2832D;
         fx.particles.spawn(3, fx.ctx.feetX + Math.cos(a) * r, fx.ctx.feetY + (double)rng.next() * 3.5D, fx.ctx.feetZ + Math.sin(a) * r, 0.0F, 1.5F + rng.next() * 2.0F, 0.0F, 0.9F, 0.05F * fx.size).color(this.color, rng.next()).alpha(fx.opacity).hot(0.7F).flags(2);
      }

      if(v == BeamStyle.Variant.LIGHTNING && !s.fired && t >= 0.0F) {
         s.fired = true;
         fx.burst(fx.ctx.feetX, fx.ctx.feetY + 1.8D, fx.ctx.feetZ, 0.0D, 1.0D, 0.0D, fx.count(18.0F, true), 5.0F, 1.0F);
      }

      if(v == BeamStyle.Variant.ORBITAL && !s.fired && t >= 0.45F) {
         s.fired = true;
         if(!fx.lod) {
            for(int i = 0; i < fx.count(26.0F, true); ++i) {
               double a = (double)rng.next() * 6.2832D;
               float sp = 3.0F + rng.next() * 4.0F;
               fx.particles.spawn(1, fx.ctx.feetX, fx.ctx.groundY + 0.05D, fx.ctx.feetZ, (float)Math.cos(a) * sp, 1.0F + rng.next() * 2.0F, (float)Math.sin(a) * sp, 0.5F + rng.next() * 0.4F, 0.06F * fx.size).gravity(9.0F).drag(1.5F).ground(fx.ctx.groundY + 0.02D, 1, 0.3F).color(this.color, rng.next()).alpha(fx.opacity).hot(0.8F);
            }
         }
      }

   }

   private static double liftOf(float t) {
      return 0.4D * (double)Easing.inOutSine(Easing.clamp01((t - 0.3F) / 1.2F));
   }

   private float beamOpen(KillFx fx) {
      float t = this.time(fx);
      return this.variant(fx) == BeamStyle.Variant.ORBITAL?(t < 0.45F?0.0F:Easing.outCubic(Easing.clamp01((t - 0.45F) / 0.08F)) * (1.0F - Easing.smoothstep(1.8F, 2.5F, t))):Easing.outCubic(Easing.clamp01((t - 0.08F) / 0.3F)) * (1.0F - Easing.smoothstep(1.45F, 2.2F, t));
   }

   public void render(KillFx fx, FxFrame f) {
      switch(this.variant(fx)) {
      case LIGHTNING:
         this.renderLightning(fx, f);
         break;
      case ORBITAL:
         this.renderOrbital(fx, f);
         break;
      default:
         this.renderPillar(fx, f);
      }

   }

   private void column(KillFx fx, FxFrame f, double w, float a, float pulse) {
      if(a > 0.003F && w > 0.002D) {
         double x = f.rx(fx.ctx.feetX);
         double z = f.rz(fx.ctx.feetZ);
         double y0 = f.ry(fx.ctx.groundY);
         double y1 = f.ry(fx.ctx.groundY + (double)this.height.getFloat());
         int c = this.color.getColor(0.3F);
         float glow = Math.max(0.35F, fx.glow);
         RenderState.additive();
         Vfx.line(Textures.LINE_GLOW, x, y0, z, x, y1, z, w * 3.4D, alpha(c, 0.3F * a * glow), alpha(c, 0.0F));
         Vfx.line(Textures.LINE_GLOW, x, y0, z, x, y1, z, w * 1.5D, alpha(this.color.getColor(0.6F), 0.75F * a * pulse), alpha(c, 0.1F * a));
         Vfx.line(Textures.LINE_CORE, x, y0, z, x, y1, z, w * 0.42D, core(c, 0.95F * a), core(c, 0.25F * a));
         double half = w * 0.6D;
         double rx = Vfx.camera().rightX;
         double rz = Vfx.camera().rightZ;
         double rl = Math.sqrt(rx * rx + rz * rz);
         if(rl > 1.0E-4D) {
            double scroll = (double)fx.t * 1.8D;
            int sc = alpha(ColorUtil.core(c), 0.35F * a);
            double var28;
            double var29;
            Vfx.quadUV(Textures.STREAKS, x - (var28 = rx / rl) * half, y0, z - (var29 = rz / rl) * half, 0.0D, scroll, sc, x + var28 * half, y0, z + var29 * half, 1.0D, scroll, sc, x + var28 * half, y1, z + var29 * half, 1.0D, scroll + (double)this.height.getFloat() * 0.15D, alpha(sc, 0.0F), x - var28 * half, y1, z - var29 * half, 0.0D, scroll + (double)this.height.getFloat() * 0.15D, alpha(sc, 0.0F));
         }
      }

   }

   private void shimmer(KillFx fx, FxFrame f, double radius, float strength, float a) {
      if(a > 0.02F && !fx.lod && !f.preview && Distortion.refracts()) {
         double x = fx.ctx.feetX - f.viewX;
         double z = fx.ctx.feetZ - f.viewZ;
         double y0 = fx.ctx.groundY - f.viewY;
         float wobble = 0.85F + 0.15F * (float)Math.sin((double)(fx.t * 23.0F));
         Vfx.flush();
         Distortion.begin(f.frameId, strength * wobble, -1, 0.0F, a * 0.6F, true);
         int segments = 16;
         int rings = 4;
         double top = (double)this.height.getFloat();

         for(int j = 0; j < rings; ++j) {
            double h0 = top * (double)j / (double)rings;
            double h1 = top * (double)(j + 1) / (double)rings;
            double r0 = radius * (1.0D - 0.6D * (double)j / (double)rings);
            double r1 = radius * (1.0D - 0.6D * (double)(j + 1) / (double)rings);

            for(int i = 0; i < segments; ++i) {
               double a0 = 6.283185307179586D * (double)i / (double)segments;
               double a1 = 6.283185307179586D * (double)(i + 1) / (double)segments;
               double c0 = Math.cos(a0);
               double s0 = Math.sin(a0);
               double c1 = Math.cos(a1);
               double s1 = Math.sin(a1);
               Distortion.vertex(x + c0 * r0, y0 + h0, z + s0 * r0, c0, 0.0D, s0);
               Distortion.vertex(x + c1 * r0, y0 + h0, z + s1 * r0, c1, 0.0D, s1);
               Distortion.vertex(x + c1 * r1, y0 + h1, z + s1 * r1, c1, 0.0D, s1);
               Distortion.vertex(x + c0 * r1, y0 + h1, z + s0 * r1, c0, 0.0D, s0);
            }
         }

         Distortion.end();
         RenderState.additive();
      }

   }

   private void dissolveBody(KillFx fx, FxFrame f, float p, boolean up, int edge, double lift) {
      if(p < 1.0F) {
         for(BodyProxy.Part part : fx.parts) {
            part.y += lift;
         }

         if(DissolveShader.available()) {
            fx.look.dissolve = p;
            fx.look.sweepX = 0.0D;
            fx.look.sweepY = up?1.0D:-1.0D;
            fx.look.sweepZ = 0.0D;
            fx.look.sweepFrom = up?fx.ctx.feetY:-(fx.ctx.feetY + 2.0D);
            fx.look.sweepLength = 2.0D;
            fx.look.dissolveNoise = this.noiseShare();
            fx.look.edgeColor = edge | -16777216;
            fx.look.edgeWidth = 0.08F;
         } else {
            fx.look.dissolve = -1.0F;
            fx.look.alpha = 1.0F - Easing.smoothstep(0.0F, 0.95F, p);
         }

         fx.drawBody(f);
         fx.look.alpha = 1.0F;

         for(BodyProxy.Part part : fx.parts) {
            part.y -= lift;
         }
      }

   }

   private void renderPillar(KillFx fx, FxFrame f) {
      float t = this.time(fx);
      float a = fx.alpha();
      float open = this.beamOpen(fx);
      double w = this.beamWidth(fx) * (0.04D + 0.96D * (double)open) * (double)(1.0F - Easing.smoothstep(1.5F, 2.2F, t) * 0.7F);
      float lineA = Easing.smoothstep(0.0F, 0.05F, t) * (1.0F - Easing.smoothstep(1.9F, 2.25F, t));
      float pulse = 0.85F + 0.15F * (float)Math.sin((double)(t * 17.0F));
      fx.look.glow = this.color.getColor(0.5F);
      fx.look.glowAmount = 0.45F * open * Math.max(0.3F, fx.glow);
      this.dissolveBody(fx, f, this.dissolve(fx), true, ColorUtil.core(this.color.getColor(0.5F)), liftOf(t));
      this.column(fx, f, w, a * lineA, pulse);
      double gx = f.rx(fx.ctx.feetX);
      double gy = f.ry(fx.ctx.groundY) + 0.02D;
      double gz = f.rz(fx.ctx.feetZ);
      float ringT = Easing.clamp01((t - 0.2F) / 0.55F);
      if(ringT > 0.0F && ringT < 1.0F) {
         double r = (0.5D + 2.6D * (double)Easing.outCubic(ringT)) * (double)fx.size * (fx.crit?1.6D:1.0D);
         RenderState.decal(true);
         Vfx.flatRing(Textures.LINE_GLOW, gx, gy, gz, r, 0.45D * (double)fx.size, 64, alpha(this.color.getColor(0.4F), a * (1.0F - ringT)));
         RenderState.decal(false);
      }

      float pool;
      if((pool = open * a) > 0.01F) {
         RenderState.decal(true);
         Vfx.flatSprite(Textures.GROUND_GLOW, gx, gy, gz, 1.6D * (double)fx.size * (fx.crit?1.4D:1.0D), (double)t * 0.4D, alpha(this.color.getColor(0.2F), 0.7F * pool));
         RenderState.decal(false);
      }

      Vfx.sprite(Textures.GLOW, gx, gy + 0.3D, gz, 2.4D * (double)fx.size, 0.0F, alpha(this.color.getColor(0.5F), 0.35F * pool * Math.max(0.3F, fx.glow)));
   }

   private int flicker(KillFx fx, float t) {
      int n = fx.crit?4:3;

      for(int i = 0; i < n; ++i) {
         if(t >= FLICKERS[i][0] && t < FLICKERS[i][1]) {
            return i;
         }
      }

      return -1;
   }

   private void renderLightning(KillFx fx, FxFrame f) {
      BeamStyle.State s = (BeamStyle.State)fx.data;
      float t = this.time(fx);
      float a = fx.alpha();
      float flash = this.flicker(fx, t) >= 0?1.0F:0.0F;
      fx.look.glow = -1;
      fx.look.glowAmount = flash * 0.9F;
      fx.look.tint = -15461614;
      fx.look.tintAmount = 0.93F * Easing.smoothstep(0.04F, 0.3F, t);
      this.dissolveBody(fx, f, this.dissolve(fx), false, -42478, 0.0D);
      fx.look.glowAmount = 0.0F;
      double gx = f.rx(fx.ctx.feetX);
      double gy = f.ry(fx.ctx.groundY) + 0.02D;
      double gz = f.rz(fx.ctx.feetZ);
      double scorch = (fx.crit?1.5D:1.1D) * (double)fx.size;
      float mark = a * (1.0F - Easing.smoothstep(1.4F, 2.2F, t)) * Easing.smoothstep(0.0F, 0.06F, t);
      if(mark > 0.01F) {
         RenderState.translucent(false);
         RenderState.decal(true);
         Vfx.flatSprite(Textures.GLOW, gx, gy, gz, scorch * 1.1D, (double)fx.seed, alpha(-16119544, 0.95F * mark));
         Vfx.flatSprite(Textures.GLOW, gx, gy, gz, scorch * 0.6D, (double)(fx.seed + 1), alpha(-16448508, 0.8F * mark));
         RenderState.additive();
         float heat = 1.0F - Easing.smoothstep(0.05F, 1.4F, t);
         int hot = ColorUtil.lerp(-5232120, -20400, heat * heat);
         Vfx.flatSprite(Textures.GLOW, gx, gy + 0.005D, gz, scorch * 0.55D, (double)fx.seed * 0.1D, alpha(hot, 0.85F * mark * heat));
         RenderState.decal(false);
      }

      float crawl;
      if((crawl = a * Easing.smoothstep(0.05F, 0.15F, t) * (1.0F - Easing.smoothstep(0.9F, 1.3F, t))) > 0.01F && !fx.lod) {
         int bucket = (int)(t / 0.05F);
         int arcs = fx.crit?6:4;

         for(int k2 = 0; k2 < arcs; ++k2) {
            this.boltRng.seed(fx.seed + k2 * 131, bucket);
            double ang = (double)s.arc[k2] + (double)this.boltRng.signed() * 0.4D;
            double r0 = scorch * (0.3D + 0.3D * (double)this.boltRng.next());
            double r1 = r0 + scorch * (0.4D + 0.6D * (double)this.boltRng.next());
            int n = Procedural.bolt(this.boltRng, 0, fx.ctx.feetX + Math.cos(ang) * r0, fx.ctx.groundY + 0.04D, fx.ctx.feetZ + Math.sin(ang) * r0, fx.ctx.feetX + Math.cos(ang) * r1, fx.ctx.groundY + 0.04D, fx.ctx.feetZ + Math.sin(ang) * r1, 3, 0.35D);

            for(int i = 0; i < n; ++i) {
               Vfx.X[i] = f.rx(Vfx.X[i]);
               Vfx.Y[i] = f.ry(fx.ctx.groundY + 0.04D);
               Vfx.Z[i] = f.rz(Vfx.Z[i]);
               Vfx.W[i] = (float)(0.05D * (double)fx.size * (1.0D - (double)i / (double)n));
               Vfx.C[i] = alpha(this.color.getColor((float)i / (float)n), crawl * (0.6F + 0.4F * this.boltRng.next()));
            }

            Neon.add(0, n, false);
            Neon.flush();
         }
      }

      int k;
      if((k = this.flicker(fx, t)) >= 0) {
         this.boltRng.seed(fx.seed, k * 7919);
         double topX = fx.ctx.feetX + (double)(this.boltRng.signed() * 3.0F);
         double topZ = fx.ctx.feetZ + (double)(this.boltRng.signed() * 3.0F);
         double topY = fx.ctx.groundY + (double)this.height.getFloat();
         int n;
         int offset = n = Procedural.bolt(this.boltRng, 0, topX, topY, topZ, fx.ctx.feetX, fx.ctx.feetY + 1.9D, fx.ctx.feetZ, 6, 0.18D);
         int forks = 2 + this.boltRng.nextInt(3);

         for(int j = 0; j < forks && n > 8; ++j) {
            int from = 2 + this.boltRng.nextInt(n / 2);
            double bx = Vfx.X[from];
            double by = Vfx.Y[from];
            double bz = Vfx.Z[from];
            double len = 2.5D + (double)this.boltRng.next() * 4.0D;
            int bp = Procedural.bolt(this.boltRng, offset, bx, by, bz, bx + (double)this.boltRng.signed() * len * 0.7D, by - len, bz + (double)this.boltRng.signed() * len * 0.7D, 3, 0.3D);
            this.boltPoints(f, offset, bp, 0.08D * (double)this.width.getFloat() * (double)fx.size, a * 0.7F);
            offset += bp;
         }

         this.boltPoints(f, 0, n, 0.2D * (double)this.width.getFloat() * (double)fx.size * (fx.crit?1.5D:1.0D), a);
         Neon.flush(1.2F, 1.0F, 1.2F);
         Vfx.sprite(Textures.GLOW, f.rx(fx.ctx.feetX), f.ry(fx.ctx.feetY + 1.2D), f.rz(fx.ctx.feetZ), 5.0D * (double)fx.size, 0.0F, alpha(-1, 0.55F * a * Math.max(0.4F, fx.glow)));
         Vfx.sprite(Textures.GLOW, f.rx(fx.ctx.feetX), f.ry(fx.ctx.groundY + 0.2D), f.rz(fx.ctx.feetZ), 3.0D * (double)fx.size, 0.0F, alpha(this.color.getColor(0.5F), 0.6F * a));
      }

   }

   private void boltPoints(FxFrame f, int offset, int n, double w, float a) {
      for(int i = offset; i < offset + n; ++i) {
         float s = (float)(i - offset) / (float)Math.max(1, n - 1);
         Vfx.X[i] = f.rx(Vfx.X[i]);
         Vfx.Y[i] = f.ry(Vfx.Y[i]);
         Vfx.Z[i] = f.rz(Vfx.Z[i]);
         Vfx.W[i] = (float)(w * (0.6D + 0.4D * (double)s));
         Vfx.C[i] = alpha(this.color.getColor(s), a);
      }

      Neon.add(offset, n, false);
   }

   private void renderOrbital(KillFx fx, FxFrame f) {
      float t = this.time(fx);
      float a = fx.alpha();
      double gx = f.rx(fx.ctx.feetX);
      double gy = f.ry(fx.ctx.groundY) + 0.02D;
      double gz = f.rz(fx.ctx.feetZ);
      float open = this.beamOpen(fx);
      fx.look.glow = this.color.getColor(0.5F);
      fx.look.glowAmount = 0.6F * open;
      this.dissolveBody(fx, f, this.dissolve(fx), true, ColorUtil.core(this.color.getColor(0.5F)), 0.0D);
      if(t < 0.55F) {
         float lock = Easing.smoothstep(0.0F, 0.4F, t);
         float wob = (1.0F - lock) * 1.6F;
         double topX = fx.ctx.feetX + (double)(Procedural.noise(t * 7.0F, fx.seed) * wob);
         double topZ = fx.ctx.feetZ + (double)(Procedural.noise(t * 7.0F, fx.seed + 5) * wob);
         double aimX = fx.ctx.feetX + (double)(Procedural.noise(t * 9.0F, fx.seed + 9) * wob) * 0.3D;
         double aimZ = fx.ctx.feetZ + (double)(Procedural.noise(t * 9.0F, fx.seed + 13) * wob) * 0.3D;
         float blink = t < 0.4F?1.0F:((int)(t * 30.0F) % 2 == 0?1.0F:0.4F);
         float la = a * Easing.smoothstep(0.0F, 0.05F, t) * blink;
         int c = this.color.getColor(0.8F);
         Vfx.line(Textures.LINE_GLOW, f.rx(topX), f.ry(fx.ctx.groundY + (double)this.height.getFloat()), f.rz(topZ), f.rx(aimX), f.ry(fx.ctx.feetY + 1.0D), f.rz(aimZ), 0.12D * (double)fx.size, alpha(c, 0.0F), alpha(c, 0.8F * la));
         Vfx.line(Textures.LINE_CORE, f.rx(topX), f.ry(fx.ctx.groundY + (double)this.height.getFloat()), f.rz(topZ), f.rx(aimX), f.ry(fx.ctx.feetY + 1.0D), f.rz(aimZ), 0.03D * (double)fx.size, core(c, 0.1F * la), core(c, 0.95F * la));
         double r = (1.7D - 1.0D * (double)lock) * (double)fx.size;
         RenderState.decal(true);
         Vfx.flatRing(Textures.LINE_CORE, gx, gy, gz, r, 0.05D * (double)fx.size, 64, alpha(c, 0.85F * la));
         Vfx.flatRing(Textures.LINE_GLOW, gx, gy, gz, r, 0.3D * (double)fx.size, 64, alpha(c, 0.35F * la));

         for(int i = 0; i < 4; ++i) {
            double ang = (double)i * 3.141592653589793D / 2.0D + (double)t * 3.0D;
            double cx = Math.cos(ang);
            double cz = Math.sin(ang);
            Vfx.flatLine(Textures.LINE_CORE, gx + cx * (r + 0.1D), gz + cz * (r + 0.1D), gx + cx * (r + 0.45D * (double)fx.size), gz + cz * (r + 0.45D * (double)fx.size), gy, 0.06D * (double)fx.size, alpha(c, 0.9F * la), alpha(c, 0.2F * la));
         }

         RenderState.decal(false);
      }

      if(open > 0.0F) {
         double w = this.beamWidth(fx) * 1.7D * (double)open;
         float pulse = 0.8F + 0.2F * (float)Math.sin((double)(t * 40.0F));
         this.shimmer(fx, f, w * 0.75D, 0.05F, a * open);
         this.column(fx, f, w, a, pulse);
         float ringT = Easing.clamp01((t - 0.45F) / 0.7F);
         if(ringT < 1.0F) {
            double r = (0.4D + 3.6D * (double)Easing.outCubic(ringT)) * (double)fx.size * (fx.crit?1.6D:1.0D);
            double band = 0.7D * (double)fx.size * (double)(1.0F - 0.5F * ringT);
            RenderState.decal(true);
            Vfx.flatBand(Textures.WAVE, gx, gy, gz, Math.max(0.0D, r - band), r, 0.0D, 6.283185307179586D, 64, alpha(this.color.getColor(0.4F), a * (1.0F - ringT)));
            RenderState.decal(false);
         }

         RenderState.decal(true);
         Vfx.flatSprite(Textures.GROUND_GLOW, gx, gy, gz, 2.2D * (double)fx.size * (fx.crit?1.4D:1.0D), (double)t, alpha(ColorUtil.core(this.color.getColor(0.3F)), 0.8F * a * open));
         RenderState.decal(false);
         Vfx.sprite(Textures.GLOW, gx, gy + 0.4D, gz, 4.0D * (double)fx.size, 0.0F, alpha(this.color.getColor(0.5F), 0.4F * a * open * Math.max(0.3F, fx.glow)));
      }

   }

   private static final class State {
      final SkinDust dust;
      final float[] arc;
      boolean fired;

      private State() {
         this.dust = new SkinDust(420);
         this.arc = new float[6];
      }
   }

   static enum Variant implements Displayable {
      PILLAR("Pillar of Light", "A beam from the sky lifts the body away"),
      LIGHTNING("Lightning", "A bolt strikes; the body chars and crumbles"),
      ORBITAL("Orbital Laser", "A targeting laser locks on, then the big one fires");

      private final String displayName;
      private final String description;

      private Variant(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
