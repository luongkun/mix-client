package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.physics.Ragdoll;
import mix.client.mixui.integration.haunt.fx.physics.RigidBox;
import mix.client.mixui.integration.haunt.haunt.fx.HauntKit;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.PlayerModel;
import mix.client.mixui.integration.haunt.render.PoseSnapshot;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class FrozenDesperationStyle extends KillStyleBase {
   private static final float STAGE2 = 2.6F;
   private final SliderSetting hold = (SliderSetting)this.setting((new SliderSetting("frozen.hold", "Hold Time", "How long it stays frozen in the air if nobody hits it.", 2.0D, 15.0D, 6.0D, 0.5D)).unit(" s"));
   private final SliderSetting hover = (SliderSetting)this.setting((new SliderSetting("frozen.hover", "Height", "How high above the ground it hangs.", 0.6D, 2.5D, 1.5D, 0.05D)).unit(" blocks"));
   private final SliderSetting panic = (SliderSetting)this.setting((new SliderSetting("frozen.panic", "Desperation", "How wildly the arms and legs fight.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting animSpeed = (SliderSetting)this.setting((new SliderSetting("frozen.speed", "Animation Speed", "How fast it flails.", 0.4D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting launch = (SliderSetting)this.setting((new SliderSetting("frozen.launch", "Launch", "How hard the second hit throws the body.", 0.3D, 2.5D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting shardCount = (SliderSetting)this.setting((new SliderSetting("frozen.shards", "Ice Shards", "How many shards the frost breaks into.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting anyClick = (BooleanSetting)this.setting(new BooleanSetting("frozen.anyClick", "Break On Any Hit", "Off: only a click that lands on the frozen body breaks it.", false));
   private final ColorSetting frost = (ColorSetting)this.setting((new ColorSetting("frozen.frost", "Frost", "Color of the frost glints and the shards.", -6750209)).secondary());
   private final PoseSnapshot pose = new PoseSnapshot();
   private final double[] fcx = new double[6];
   private final double[] fcy = new double[6];
   private final double[] fcz = new double[6];
   private final double[] frot = new double[54];

   public FrozenDesperationStyle(KillEffectsModule module) {
      super(module, KillStyle.FROZEN_DESPERATION, new ColorSetting("frozen.color", "Ice", "Color of the ice covering the body.", -8402689));
   }

   private float holdTime(KillFx fx) {
      return fx.preview?2.2F:this.hold.getFloat();
   }

   public float seconds(KillFx fx) {
      return this.holdTime(fx) + 2.6F;
   }

   public boolean leavesBody() {
      return true;
   }

   public void start(KillFx fx) {
      FrozenDesperationStyle.State s = new FrozenDesperationStyle.State();
      fx.data = s;
      s.seed = fx.rng.next() * 100.0F;
      s.lift = (double)this.hover.getFloat();
      HauntKit.newSwing();
   }

   private float stage2(KillFx fx) {
      FrozenDesperationStyle.State s = (FrozenDesperationStyle.State)fx.data;
      return s.brokeAt >= 0.0F?s.brokeAt:this.holdTime(fx);
   }

   public void update(KillFx fx, float dt) {
      FrozenDesperationStyle.State s = (FrozenDesperationStyle.State)fx.data;
      float t = fx.t;
      if(s.brokeAt < 0.0F) {
         s.anim += dt * this.animSpeed.getFloat();
         boolean swung = !fx.preview && HauntKit.newSwing();
         if(swung && t > 0.45F) {
            double cx = fx.ctx.feetX;
            double cy = fx.ctx.feetY + s.lift;
            double cz = fx.ctx.feetZ;
            boolean hit = this.anyClick.get() || HauntKit.lookHits(cx - 0.55D, cy - 0.15D, cz - 0.55D, cx + 0.55D, cy + 2.0D, cz + 0.55D, 5.0D);
            if(hit) {
               double[] d = HauntKit.lookDir();
               this.breakFree(fx, s, d[0], d[1], d[2]);
               fx.t = this.holdTime(fx);
            }
         }

         if(s.brokeAt < 0.0F && fx.t >= this.holdTime(fx)) {
            this.breakFree(fx, s, fx.ctx.dirX, 0.2D, fx.ctx.dirZ);
         }

         if(!fx.lod && fx.rng.next() < dt * 18.0F) {
            int i = fx.rng.nextInt(6);
            BodyProxy.Part p = fx.parts[i];
            fx.particles.spawn(5, p.x + (double)(fx.rng.signed() * 0.2F), p.y + (double)(fx.rng.signed() * 0.25F), p.z + (double)(fx.rng.signed() * 0.2F), fx.rng.signed() * 0.2F, 0.15F, fx.rng.signed() * 0.2F, 0.6F, (0.04F + fx.rng.next() * 0.03F) * fx.size).drag(1.5F).color(this.frost, fx.rng.next()).alpha(fx.opacity).flags(2);
         }

         if(!fx.lod && fx.rng.next() < dt * 6.0F) {
            fx.particles.spawn(8, fx.ctx.feetX + (double)(fx.rng.signed() * 0.4F), fx.ctx.feetY + s.lift + 0.1D, fx.ctx.feetZ + (double)(fx.rng.signed() * 0.4F), fx.rng.signed() * 0.1F, -0.4F, fx.rng.signed() * 0.1F, 1.2F, 0.18F * fx.size).end(0.45F * fx.size).color(ColorUtil.lerp(-1, this.color.getColor(0.5F), 0.4F)).alpha(0.25F * fx.opacity);
         }
      }

   }

   private void breakFree(KillFx fx, FrozenDesperationStyle.State s, double dx, double dy, double dz) {
      s.brokeAt = Math.max(fx.t, 0.0F);
      double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if(l < 1.0E-4D) {
         dx = 0.0D;
         dy = 0.2D;
         dz = 1.0D;
         l = Math.sqrt(1.04D);
      }

      dx = dx / l;
      dy = dy / l;
      dz = dz / l;
      Ragdoll r = fx.ragdoll();
      r.gravity = 26.0F;
      r.stiffness = 0.12F;

      for(RigidBox b : r.parts) {
         b.restitution = 0.3F;
         b.friction = 0.55F;
      }

      double strength = 7.5D * (double)this.launch.getFloat() * (fx.crit?1.4D:1.0D) * (fx.preview?0.4D:1.0D);
      BodyProxy.Part body = fx.parts[1];
      r.fling(body.x - dx * 0.3D, body.y, body.z - dz * 0.3D, 0.0D, 0.0D, 0.0D, dx, Math.max(0.15D, dy + 0.35D), dz, strength, 7.0D * (double)this.launch.getFloat());
      float amt = this.shardCount.getFloat();
      int n = fx.count(60.0F * amt, true);

      for(int i = 0; i < n; ++i) {
         BodyProxy.Part p = fx.parts[fx.rng.nextInt(6)];
         double px = p.x + (double)(fx.rng.signed() * 0.22F);
         double py = p.y + (double)(fx.rng.signed() * 0.3F);
         double pz = p.z + (double)(fx.rng.signed() * 0.22F);
         double sp = 2.5D + (double)fx.rng.next() * 4.5D;
         double ox = (double)fx.rng.signed();
         double oy = (double)fx.rng.signed() * 0.6D + 0.4D;
         double oz = (double)fx.rng.signed();
         float sz = (0.05F + fx.rng.next() * 0.08F) * fx.size;
         double ax = (double)fx.rng.signed();
         double ay = (double)fx.rng.signed();
         double az = (double)fx.rng.signed();
         double al = Math.max(1.0E-4D, Math.sqrt(ax * ax + ay * ay + az * az));
         ax = ax / al;
         ay = ay / al;
         az = az / al;
         double bx = ay * 0.0D - az * 1.0D;
         double by = az * 0.0D - ax * 0.0D;
         double bz = ax * 1.0D - ay * 0.0D;
         double bl = Math.sqrt(bx * bx + by * by + bz * bz);
         if(bl < 1.0E-4D) {
            bx = 1.0D;
            by = 0.0D;
            bz = 0.0D;
            bl = 1.0D;
         }

         bx = bx / bl;
         by = by / bl;
         bz = bz / bl;
         double ux = ay * bz - az * by;
         double uy = az * bx - ax * bz;
         double uz = ax * by - ay * bx;
         fx.particles.spawn(11, px, py, pz, (float)((dx * 0.8D + ox * 0.6D) * sp), (float)((dy * 0.5D + oy) * sp), (float)((dz * 0.8D + oz * 0.6D) * sp), 0.9F + fx.rng.next() * 0.6F, 1.0F).end(0.6F).gravity(14.0F).drag(0.8F).ground(fx.ctx.groundY + 0.01D, 2, 0.25F).axis(fx.rng.signed(), fx.rng.signed(), fx.rng.signed()).spin(0.0F, fx.rng.range(6.0F, 16.0F) * (fx.rng.next() < 0.5F?-1.0F:1.0F)).shard(-sz, -sz * 0.6F, sz, -sz * 0.4F * fx.rng.next(), sz * 0.2F * fx.rng.signed(), sz * (1.0F + fx.rng.next()), bx, by, bz, ux, uy, uz).color(fx.rng.next() < 0.6F?this.color.getColor(fx.rng.next()):this.frost.getColor(fx.rng.next())).alpha(fx.opacity).fade(0.01F, 0.6F);
      }

      for(int i = 0; i < fx.count(30.0F * amt, true); ++i) {
         fx.particles.spawn(5, body.x + (double)(fx.rng.signed() * 0.4F), body.y + (double)(fx.rng.signed() * 0.6F), body.z + (double)(fx.rng.signed() * 0.4F), (float)(dx * 3.0D + (double)(fx.rng.signed() * 3.0F)), fx.rng.next() * 3.0F, (float)(dz * 3.0D + (double)(fx.rng.signed() * 3.0F)), 0.5F + fx.rng.next() * 0.4F, (0.06F + fx.rng.next() * 0.05F) * fx.size).drag(2.0F).color(this.frost, fx.rng.next()).alpha(fx.opacity).flags(2);
      }

      for(int i = 0; i < 8; ++i) {
         fx.particles.spawn(8, body.x + (double)(fx.rng.signed() * 0.3F), body.y + (double)(fx.rng.signed() * 0.6F), body.z + (double)(fx.rng.signed() * 0.3F), (float)(dx * 1.4D + (double)(fx.rng.signed() * 0.5F)), 0.3F + fx.rng.next() * 0.5F, (float)(dz * 1.4D + (double)(fx.rng.signed() * 0.5F)), 0.9F, (0.3F + fx.rng.next() * 0.3F) * fx.size).end(0.8F * fx.size).drag(1.5F).color(ColorUtil.lerp(-1, this.color.getColor(0.5F), 0.4F)).alpha(0.5F * fx.opacity);
      }

   }

   private void flail(KillFx fx, FrozenDesperationStyle.State s, float t) {
      float k = this.panic.getFloat();
      float a = s.anim;
      float freeze = Easing.smoothstep(0.0F, 0.35F, t);
      this.pose.copy(fx.ctx.body.pose);
      this.pose.y = fx.ctx.feetY + s.lift * (double)Easing.outBack(Easing.clamp01(t / 0.35F), 1.4F);
      this.pose.sneaking = false;
      this.pose.limbSwing = fx.ctx.body.pose.limbSwing + a * 21.0F;
      this.pose.limbSwingAmount = (1.05F + 0.35F * (float)Math.sin((double)(a * 3.1F + s.seed))) * k * freeze + fx.ctx.body.pose.limbSwingAmount * (1.0F - freeze);
      this.pose.swingProgress = a * 2.3F % 1.0F * Math.min(1.0F, k);
      this.pose.netHeadYaw = 55.0F * (float)Math.sin((double)(a * 2.2F + s.seed)) * Math.min(1.0F, k);
      this.pose.headPitch = -25.0F + 30.0F * (float)Math.sin((double)(a * 3.7F + s.seed * 0.5F));
      this.pose.deathAngle = 14.0F * k * (float)Math.sin((double)(a * 1.6F + s.seed)) + 6.0F * (float)Math.sin((double)(a * 4.3F));
      this.pose.bodyYaw = fx.ctx.body.pose.bodyYaw + 12.0F * (float)Math.sin((double)(a * 1.1F + s.seed * 2.0F));
      PlayerModel.frames(this.pose, this.fcx, this.fcy, this.fcz, this.frot);

      for(int i = 0; i < 6; ++i) {
         BodyProxy.Part p = fx.parts[i];
         p.copy(fx.rest[i]);
         p.x = this.fcx[i];
         p.y = this.fcy[i];
         p.z = this.fcz[i];
         System.arraycopy(this.frot, i * 9, p.r, 0, 9);
      }

      boolean slim = fx.ctx.body.slim;

      for(int arm = 2; arm <= 3; ++arm) {
         BodyProxy.Part p = fx.parts[arm];
         double[] pv = HauntKit.pivot(p, arm, slim);
         BodyProxy.Part body = fx.parts[1];
         double side = arm == 2?1.0D:-1.0D;
         double raise = (0.9D + 0.7D * Math.sin((double)(a * (arm == 2?5.3F:4.7F) + s.seed))) * (double)k * (double)freeze;
         double[] q = HauntKit.axisAngle(body.r[2], body.r[5], body.r[8], side * raise);
         HauntKit.rotateAbout(p, pv[0], pv[1], pv[2], q);
      }

      for(int leg = 4; leg <= 5; ++leg) {
         BodyProxy.Part p = fx.parts[leg];
         double[] pv = HauntKit.pivot(p, leg, slim);
         BodyProxy.Part body = fx.parts[1];
         double kick = 0.35D * Math.sin((double)(a * (leg == 4?7.1F:6.4F) + s.seed)) * (double)k * (double)freeze;
         double[] q = HauntKit.axisAngle(body.r[2], body.r[5], body.r[8], kick);
         HauntKit.rotateAbout(p, pv[0], pv[1], pv[2], q);
      }

   }

   public void render(KillFx fx, FxFrame f) {
      FrozenDesperationStyle.State s = (FrozenDesperationStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      int ice = this.color.getColor(0.5F) | -16777216;
      if(s.brokeAt < 0.0F) {
         this.flail(fx, s, t);
         float cover = Easing.smoothstep(0.1F, 1.6F, t);
         fx.look.tint = ice;
         fx.look.tintAmount = 0.82F * cover;
         fx.look.glow = ColorUtil.lerp(-1, ice, 0.4F);
         fx.look.glowAmount = 0.25F * cover + 0.1F * (float)Math.sin((double)(t * 3.0F)) * cover;
         fx.drawParts(f, fx.parts);
         fx.look.tintAmount = 0.0F;
         fx.look.glowAmount = 0.0F;
         RenderState.additive();
         this.frostGlints(fx, f, s, t, a, cover);
         double gy = f.ry(fx.ctx.groundY + 0.02D);
         Vfx.flatSprite(Textures.GROUND_GLOW, f.rx(fx.ctx.feetX), gy, f.rz(fx.ctx.feetZ), 0.9D * (double)fx.size, 0.0D, alpha(ice, 0.25F * a * cover));
         Vfx.flush();
      } else {
         float k = (t - s.brokeAt) / 2.6F;
         float thaw = 1.0F - Easing.smoothstep(0.0F, 0.8F, k);
         fx.look.tint = ice;
         fx.look.tintAmount = 0.82F * thaw;
         fx.look.glow = -1;
         fx.look.glowAmount = 0.9F * (1.0F - Easing.clamp01(k / 0.08F));
         fx.drawBody(f);
         fx.look.tintAmount = 0.0F;
         fx.look.glowAmount = 0.0F;
         RenderState.additive();
         float fl = 1.0F - Easing.clamp01(k / 0.25F);
         if(fl > 0.0F) {
            BodyProxy.Part body = fx.parts[1];
            double cx = f.rx(body.x);
            double cy = f.ry(body.y);
            double cz = f.rz(body.z);
            Vfx.sprite(Textures.GLOW, cx, cy, cz, 3.2D * (double)fx.size, 0.0F, alpha(ice, 0.7F * a * fl * fl));
            Vfx.sprite(Textures.FLARE, cx, cy, cz, 2.4D * (double)fx.size, k * 3.0F, alpha(-1, a * fl));
            double rr = (0.3D + 2.4D * (double)Easing.outCubic(1.0F - fl)) * (double)fx.size;
            HauntKit.ring(Textures.LINE_GLOW, cx, cy, cz, rr, 0.25D * (double)fx.size * (double)fl, alpha(this.frost.getColor(0.5F), 0.8F * a * fl));
         }

         Vfx.flush();
      }

   }

   private void frostGlints(KillFx fx, FxFrame f, FrozenDesperationStyle.State s, float t, float a, float cover) {
      if(!fx.lod) {
         for(int i = 0; i < 14; ++i) {
            BodyProxy.Part p = fx.parts[i % 6];
            double ox = Math.sin((double)((float)i * 12.9898F + s.seed)) * 0.18D;
            double oy = Math.cos((double)((float)i * 78.233F + s.seed)) * 0.28D;
            double oz = Math.sin((double)((float)i * 37.719F)) * 0.18D;
            float tw = 0.5F + 0.5F * (float)Math.sin((double)(t * (3.0F + (float)(i % 4))) + (double)i);
            float on = Easing.smoothstep((float)i / 14.0F * 1.2F, (float)i / 14.0F * 1.2F + 0.4F, t);
            Vfx.sprite(Textures.SPARKLE, f.rx(p.x + ox), f.ry(p.y + oy), f.rz(p.z + oz), (0.12D + 0.1D * (double)tw) * (double)fx.size, t + (float)i, alpha(this.frost.getColor((float)i / 14.0F), 0.9F * a * tw * on * cover));
         }
      }

   }

   public boolean focus(KillFx fx, double[] out) {
      if(fx.hasRagdoll()) {
         return false;
      } else {
         out[0] = fx.ctx.feetX;
         out[1] = fx.ctx.feetZ;
         return true;
      }
   }

   public String stateForTest(KillFx fx) {
      FrozenDesperationStyle.State s = (FrozenDesperationStyle.State)fx.data;
      return s == null?null:(s.brokeAt < 0.0F?"frozen":"ragdoll");
   }

   static final class State {
      float seed;
      float anim;
      double lift;
      float brokeAt = -1.0F;
   }
}
