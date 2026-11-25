package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.ambience.AmbienceModule;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.module.kill.SkinDust;
import mix.client.mixui.integration.haunt.render.DissolveShader;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class DisintegrateStyle extends KillStyleBase {
   private static final float FREEZE = 0.35F;
   private static final float SWEEP = 1.6F;
   private static final float DRIFT_OUT = 1.0F;
   private static final float NOISE = 0.5F;
   private static final int MAX_DUST = 1400;
   private final ModeSetting<DisintegrateStyle.Start> start = (ModeSetting)this.setting(new ModeSetting("disintegrate.start", "Start Point", "Where the crumbling begins.", DisintegrateStyle.Start.class, DisintegrateStyle.Start.HIT_POINT));
   private final SliderSetting sweepSpeed = (SliderSetting)this.setting((new SliderSetting("disintegrate.speed", "Crumble Speed", "How fast the edge sweeps across the body.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting dust = (SliderSetting)this.setting((new SliderSetting("disintegrate.dust", "Dust Amount", "How many specks the body turns into.", 0.25D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting drift = (SliderSetting)this.setting((new SliderSetting("disintegrate.drift", "Drift", "How far the dust drifts away.", 0.0D, 2.0D, 1.0D, 0.05D)).unit("x"));

   public DisintegrateStyle(KillEffectsModule module) {
      super(module, KillStyle.DISINTEGRATE, new ColorSetting("disintegrate.color", "Edge Glow", "Color of the burning edge and the embers.", -34257));
   }

   private float sweepSeconds(KillFx fx) {
      return 1.6F / this.sweepSpeed.getFloat() * (fx.crit?0.8F:1.0F);
   }

   public float seconds(KillFx fx) {
      return 0.35F + this.sweepSeconds(fx) + 1.0F;
   }

   private float edge(KillFx fx) {
      return fx.crit?0.12F:0.08F;
   }

   public void start(final KillFx fx) {
      final DisintegrateStyle.State s = new DisintegrateStyle.State();
      fx.data = s;
      switch((DisintegrateStyle.Start)this.start.get()) {
      case TOP_DOWN:
         s.sx = 0.0D;
         s.sy = -1.0D;
         s.sz = 0.0D;
         s.from = -(fx.ctx.feetY + 2.05D);
         s.length = 2.15D;
         break;
      case BOTTOM_UP:
         s.sx = 0.0D;
         s.sy = 1.0D;
         s.sz = 0.0D;
         s.from = fx.ctx.feetY - 0.1D;
         s.length = 2.15D;
         break;
      default:
         double l = Math.sqrt(fx.ctx.dirX * fx.ctx.dirX + fx.ctx.dirY * fx.ctx.dirY + fx.ctx.dirZ * fx.ctx.dirZ);
         s.sx = fx.ctx.dirX / l;
         s.sy = fx.ctx.dirY / l;
         s.sz = fx.ctx.dirZ / l;
         s.from = fx.ctx.impactX * s.sx + fx.ctx.impactY * s.sy + fx.ctx.impactZ * s.sz - 0.35D;
         s.length = 1.25D;
      }

      final float edge = this.edge(fx);
      s.dust.sample(fx, fx.count(900.0F * this.dust.getFloat(), true), new SkinDust.Threshold() {
         public float at(double x, double y, double z) {
            float sweep = Easing.clamp01((float)((x * s.sx + y * s.sy + z * s.sz - s.from) / s.length));
            float noise = DissolveShader.noiseAt(x - fx.ctx.body.pose.x, y - fx.ctx.body.pose.y, z - fx.ctx.body.pose.z);
            return DissolveShader.thresholdAt(sweep, noise, 0.5F, edge);
         }
      });
      AmbienceModule ambience = FuriahsVisuals.instance.getAmbience();
      if(fx.preview || ambience == null || !ambience.windAt(fx.ctx.midX(), fx.ctx.midY(), fx.ctx.midZ(), s.wind)) {
         s.wind[0] = (float)(fx.ctx.dirX * 1.2D);
         s.wind[1] = 0.2F;
         s.wind[2] = (float)(fx.ctx.dirZ * 1.2D);
      }

   }

   private float progress(KillFx fx) {
      return fx.phase(0.35F, 0.35F + this.sweepSeconds(fx));
   }

   public void update(KillFx fx, float dt) {
      DisintegrateStyle.State s = (DisintegrateStyle.State)fx.data;
      SkinDust d = s.dust;
      float p = this.progress(fx);
      float drifts = this.drift.getFloat();
      boolean shader = DissolveShader.available();
      Rng rng = fx.rng;

      while(d.next < d.count && d.at[d.next] <= p) {
         int i = d.next++;
         double x = fx.ctx.feetX + (double)d.x[i];
         double y = fx.ctx.feetY + (double)d.y[i];
         double z = fx.ctx.feetZ + (double)d.z[i];
         float vx = s.wind[0] * 0.45F * drifts + rng.signed() * 0.25F;
         float vy = s.wind[1] * 0.45F * drifts + (0.25F + rng.next() * 0.55F) * (0.5F + 0.5F * drifts);
         float vz = s.wind[2] * 0.45F * drifts + rng.signed() * 0.25F;
         fx.particles.spawn(12, x, y, z, vx, vy, vz, 1.1F + rng.next() * 0.9F, (0.03F + rng.next() * 0.025F) * fx.size).drag(0.7F).end(0.008F).color(d.color[i]).alpha(fx.opacity).fade(0.0F, 0.45F).spin(rng.next() * 6.3F, rng.signed() * 5.0F);
         float f = rng.next();
         float f2 = shader?0.07F:0.2F;
         float f3 = fx.lod?0.5F:1.0F;
         if(f < f2 * f3) {
            fx.particles.spawn(7, x, y, z, vx * 0.6F, vy * 0.8F + 0.3F, vz * 0.6F, 0.5F + rng.next() * 0.5F, (0.04F + rng.next() * 0.03F) * fx.size).drag(1.2F).color(this.color, rng.next()).alpha(fx.opacity).hot(0.8F);
         }
      }

   }

   public void render(KillFx fx, FxFrame f) {
      DisintegrateStyle.State s = (DisintegrateStyle.State)fx.data;
      float p = this.progress(fx);
      if(p < 1.0F) {
         fx.look.tint = -4607318;
         fx.look.tintAmount = 0.25F * Easing.smoothstep(0.0F, 0.35F, fx.t);
         fx.look.glow = this.color.getColor(0.5F);
         fx.look.glowAmount = 0.18F * Easing.smoothstep(0.1F, 0.35F, fx.t) * (1.0F - Easing.smoothstep(0.0F, 0.15F, p)) * Math.max(0.3F, fx.glow);
         if(DissolveShader.available()) {
            fx.look.dissolve = p;
            fx.look.sweepX = s.sx;
            fx.look.sweepY = s.sy;
            fx.look.sweepZ = s.sz;
            fx.look.sweepFrom = s.from;
            fx.look.sweepLength = s.length;
            fx.look.dissolveNoise = 0.5F;
            fx.look.edgeColor = this.color.getColor(0.5F) | -16777216;
            fx.look.edgeWidth = this.edge(fx);
         } else {
            fx.look.dissolve = -1.0F;
            fx.look.alpha = 1.0F - Easing.smoothstep(0.0F, 0.95F, p);
         }

         fx.drawBody(f);
         fx.look.alpha = 1.0F;
      }

   }

   static enum Start implements Displayable {
      HIT_POINT("Hit Point", "From where the killing blow landed"),
      TOP_DOWN("Top Down", "From the head down"),
      BOTTOM_UP("Bottom Up", "From the feet up");

      private final String displayName;
      private final String description;

      private Start(String displayName, String description) {
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

   private static final class State {
      final SkinDust dust;
      double sx;
      double sy;
      double sz;
      double from;
      double length;
      final float[] wind;

      private State() {
         this.dust = new SkinDust(1400);
         this.wind = new float[3];
      }
   }
}
