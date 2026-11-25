package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.physics.Ragdoll;
import mix.client.mixui.integration.haunt.fx.physics.RigidBox;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class RagdollStyle extends KillStyleBase {
   private final SliderSetting fling = (SliderSetting)this.setting((new SliderSetting("ragdoll.fling", "Fling Strength", "How hard the body is thrown.", 0.2D, 3.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting stiffness = (SliderSetting)this.setting(new SliderSetting("ragdoll.stiffness", "Stiffness", "Limp (0) to stiff (1): how much the body keeps the pose it died in.", 0.0D, 1.0D, 0.15D, 0.05D));
   private final SliderSetting bounce = (SliderSetting)this.setting(new SliderSetting("ragdoll.bounce", "Bounciness", "How much it bounces off the ground and walls.", 0.0D, 0.8D, 0.3D, 0.05D));
   private final SliderSetting gravity = (SliderSetting)this.setting((new SliderSetting("ragdoll.gravity", "Gravity", "How heavy the body falls.", 0.4D, 2.0D, 1.0D, 0.05D)).unit("x"));

   public RagdollStyle(KillEffectsModule module) {
      super(module, KillStyle.RAGDOLL, new ColorSetting("ragdoll.color", "Color", "Color of the impact sparks and the flash.", -45206));
   }

   public float seconds(KillFx fx) {
      return 2.4F;
   }

   public boolean leavesBody() {
      return true;
   }

   public void start(KillFx fx) {
      Ragdoll r = fx.ragdoll();
      r.gravity = 26.0F * this.gravity.getFloat();
      r.stiffness = this.stiffness.getFloat();

      for(RigidBox b : r.parts) {
         b.restitution = this.bounce.getFloat();
         b.friction = 0.55F;
      }

      double dx = fx.ctx.dirX;
      double dy = fx.ctx.dirY;
      double dz = fx.ctx.dirZ;
      if(dy < -0.45D) {
         double h = Math.sqrt(dx * dx + dz * dz);
         double k = h < 1.0E-4D?0.0D:Math.sqrt(0.7975D) / h;
         dx *= k;
         dz *= k;
         dy = -0.45D;
      }

      double strength = 6.5D * (double)this.fling.getFloat() * (fx.crit?1.55D:1.0D) * Math.sqrt((double)fx.size) * (fx.preview?0.35D:1.0D);
      r.fling(fx.ctx.impactX, fx.ctx.impactY, fx.ctx.impactZ, fx.ctx.body.vx * 0.6D, fx.ctx.body.vy * 0.3D, fx.ctx.body.vz * 0.6D, dx, dy, dz, strength, 5.0D * (double)this.fling.getFloat() * (fx.crit?1.4D:1.0D));
      if(!fx.lod) {
         fx.burst(fx.ctx.impactX, fx.ctx.impactY, fx.ctx.impactZ, dx, dy, dz, fx.count(fx.crit?26.0F:16.0F, false), 5.5F, 0.55F);
      }

   }

   public void render(KillFx fx, FxFrame f) {
      float flash = 1.0F - Easing.smoothstep(0.0F, 0.18F, fx.t);
      fx.look.glow = fx.color(0.5F);
      fx.look.glowAmount = flash * 0.85F * Math.min(1.0F, 0.4F + fx.glow);
      fx.drawBody(f);
   }
}
