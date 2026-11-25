package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import mix.client.mixui.integration.haunt.fx.FxParticles;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.trail.PathRecorder;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.fx.trail.TrailShape;
import mix.client.mixui.integration.haunt.fx.trail.TrailStrip;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class StardustStyle extends TrailStyleBase {
   public final ColorSetting stars = this.secondColor("stars", "Stars", "The stars\' color (white at first).", -1, true);
   public final SliderSetting nebula = (SliderSetting)this.setting((new SliderSetting("stardust.nebula", "Nebula Strength", "How thick the drifting clouds are.", 0.0D, 100.0D, 70.0D, 1.0D)).unit("%"));
   public final SliderSetting starCount = (SliderSetting)this.setting((new SliderSetting("stardust.starCount", "Star Count", "How many stars twinkle in the band.", 0.0D, 200.0D, 100.0D, 5.0D)).unit("%"));
   public final SliderSetting twinkle = (SliderSetting)this.setting((new SliderSetting("stardust.twinkle", "Twinkle Speed", "How fast the stars twinkle.", 25.0D, 300.0D, 100.0D, 5.0D)).unit("%"));
   public final SliderSetting drift = (SliderSetting)this.setting((new SliderSetting("stardust.drift", "Drift", "How fast the clouds drift along the band.", 0.0D, 100.0D, 40.0D, 1.0D)).unit("%"));
   private final Rng rng = new Rng();
   private static final float[] BASE_W = new float[1024];

   public StardustStyle(TrailsModule module) {
      super(module, TrailStyle.STARDUST, spec().color("Nebula", -6657025).size(0.1D, 1.5D, 0.55D, 0.01D).taper(55.0D).anchor(TrailAnchor.WAIST, false).animated());
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.nebula);
      out.add(this.starCount);
   }

   public void render(PlayerTrail p, TrailFrame f) {
      TrailShape s = p.shape;
      float neb = this.nebula.getFloat() / 100.0F;
      float glowAmount = this.glow.getFloat() / 100.0F;
      double flow = f.now * (0.15D + 0.85D * (double)this.drift.getFloat() / 100.0D) * (double)this.animSpeed.getFloat() / 100.0D;

      for(int k = 0; k < s.runs; ++k) {
         int n = this.fill(p, f, k, this.size.getFloat(), -1.0F, 0.0F);
         if(n >= 2) {
            int from = s.runStart[k];

            for(int j = 0; j < n; ++j) {
               BASE_W[j] = Vfx.W[j];
            }

            TrailStrip.sides(0, n, 0, f.cam.posX, f.cam.posY, f.cam.posZ, 0.0F, (float[])null, 0);

            for(int strand = 0; strand < 3; ++strand) {
               double off = (double)(strand - 1) * 0.33D;

               for(int j2 = 0; j2 < n; ++j2) {
                  float along = s.along[from + j2];
                  float cloud = noise((float)((double)along * 1.4D - flow + (double)strand * 7.31D), (float)strand * 3.7F);
                  float dense = 1.0F - neb + neb * cloud;
                  int c = this.colorAt(Math.min(1.0F, along / Math.max(0.001F, s.length)), 0.0F, s.speed[from + j2], f.colorTime, (float)(strand - 1) * 0.12F + (cloud - 0.5F) * 0.25F);
                  Vfx.C[j2] = ColorUtil.mulAlpha(c, POINT_ALPHA[j2] * dense);
                  Vfx.W[j2] = BASE_W[j2] * 0.55F;
               }

               RenderState.translucent(false);
               TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.2F, off / 0.55D, 0.32F);
               RenderState.additive();
               TrailStrip.draw(Textures.LINE_GLOW, 0, n, 1.6F + 1.4F * glowAmount, off / 0.55D, 0.38F * (0.5F + glowAmount));
            }

            for(int var20 = 0; var20 < n; ++var20) {
               Vfx.W[var20] = BASE_W[var20];
            }
         }
      }

      this.starsOf(p, f);
      this.drifting(p, f);
   }

   private static float noise(float x, float seed) {
      return 0.65F * value(x, seed) + 0.35F * value(x * 2.3F + 13.1F, seed + 5.7F);
   }

   private static float value(float x, float seed) {
      int i = (int)Math.floor((double)x);
      float t = x - (float)i;
      float a = Rng.hash(i, (int)(seed * 1000.0F));
      float b = Rng.hash(i + 1, (int)(seed * 1000.0F));
      float u = t * t * (3.0F - 2.0F * t);
      return a + (b - a) * u;
   }

   private void starsOf(PlayerTrail p, TrailFrame f) {
      PathRecorder r = p.path;
      if(r != null && !p.ghost) {
         float perPoint = 0.55F * this.starCount.getFloat() / 100.0F;
         if(perPoint > 0.0F) {
            float maxAge = Math.max(0.05F, p.maxAge);
            double tw = (double)(this.twinkle.getFloat() / 100.0F * this.animSpeed.getFloat() / 100.0F);
            float half = this.size.getFloat() * 0.5F * p.widthScale;
            float op = this.opacity.getFloat() / 100.0F * p.alpha;
            int count = r.count();

            for(int i = 1; i < count; ++i) {
               double dz;
               double dx;
               double dl;
               int seed;
               double age;
               if((r.flags(i) & 64) == 0 && r.time(i) >= p.style.since && (age = f.now - r.time(i)) <= (double)maxAge && age >= 0.0D && Rng.hash(seed = (int)(r.distance(i) * 1000.0D), 7) <= perPoint && (dl = Math.sqrt((dx = r.x(i) - r.x(i - 1)) * dx + (dz = r.z(i) - r.z(i - 1)) * dz)) >= 1.0E-6D) {
                  double across = (double)((Rng.hash(seed, 11) * 2.0F - 1.0F) * half);
                  double up = (double)((Rng.hash(seed, 13) * 2.0F - 1.0F) * half) * 0.6D;
                  double wx = r.x(i) - dz / dl * across;
                  double wy = r.y(i) + up;
                  double wz = r.z(i) + dx / dl * across;
                  float fade = this.fade((float)(age / (double)maxAge));
                  float a = op * fade * (0.45F + 0.55F * (float)Math.max(0.0D, Math.sin(f.now * tw * (6.0D + (double)Rng.hash(seed, 17) * 8.0D) + (double)Rng.hash(seed, 19) * 6.283D))) * f.nearFade(wx, wy, wz) * f.fog(wx, wy, wz);
                  if(a > 0.01F) {
                     float sz = (0.05F + 0.07F * Rng.hash(seed, 23)) * Math.max(0.5F, this.size.getFloat() / 0.55F);
                     int c = this.starColor(Rng.hash(seed, 29), f.colorTime);
                     Vfx.sprite(Textures.SPARKLE, wx - f.viewX, wy - f.viewY, wz - f.viewZ, (double)(sz * 2.0F), (float)(f.now * 0.6D), ColorUtil.mulAlpha(c, a));
                     Vfx.sprite(Textures.GLOW, wx - f.viewX, wy - f.viewY, wz - f.viewZ, (double)(sz * 3.0F), 0.0F, ColorUtil.mulAlpha(c, a * 0.35F));
                  }
               }
            }
         }
      }

   }

   private void drifting(PlayerTrail p, TrailFrame f) {
      int due = this.piecesDue(p, 1.2D * (double)this.starCount.getFloat() / 100.0D, 3);
      TrailShape s = p.shape;
      if(due != 0 && s.runs != 0) {
         int k = s.runs - 1;
         int from = s.runStart[k];
         int to = s.runEnd[k];

         for(int i = 0; i < due; ++i) {
            if(this.module.piecesFull(f)) {
               return;
            }

            this.rng.seed((int)(f.now * 997.0D) + i * 31 + p.entityId);
            int j = from + (int)(this.rng.next() * (float)(to - from));
            double x = s.x[j];
            double y = s.y[j];
            double z = s.z[j];
            if(f.nearFade(x, y, z) >= 0.5F) {
               float sz = 0.07F + 0.05F * this.rng.next();
               FxParticles pool = f.pieces(this.type);
               pool.spawn(5, x, y, z, this.rng.signed() * 0.45F, this.rng.signed() * 0.3F + 0.1F, this.rng.signed() * 0.45F, 0.8F + this.rng.next() * 0.7F, sz).drag(0.8F).alpha(this.opacity.getFloat() / 100.0F * p.alpha).flags(2).sway(0.0F, this.rng.next() * 10.0F).fade(0.15F, 0.5F).end(sz * 0.5F);
               if(this.tint == 0 && this.light >= 0.999F) {
                  pool.color(this.stars, this.rng.next());
               } else {
                  pool.color(this.starColor(this.rng.next(), f.colorTime));
               }
            }
         }
      }

   }

   private int starColor(float pos, double time) {
      int c = this.tint != 0?ColorUtil.lerp(this.tint, -1, 0.6F):this.stars.getColor(pos, time);
      return this.light < 0.999F?ColorUtil.shade(c, this.light):c;
   }
}
