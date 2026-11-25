package mix.client.mixui.integration.haunt.module.kill;

import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.Anchor;
import mix.client.mixui.integration.haunt.fx.EffectInstance;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.FxParticles;
import mix.client.mixui.integration.haunt.fx.Lifecycle;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.pass.PassItem;
import mix.client.mixui.integration.haunt.fx.physics.BlockCollider;
import mix.client.mixui.integration.haunt.fx.physics.PhysicsClock;
import mix.client.mixui.integration.haunt.fx.physics.Ragdoll;
import mix.client.mixui.integration.haunt.fx.physics.RigidBox;
import mix.client.mixui.integration.haunt.module.kill.KillContext;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.DissolveShader;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.SkinSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public final class KillFx extends EffectInstance implements PassItem {
   static final float SLOWMO_SECONDS = 0.3F;
   static final float SLOWMO_SPEED = 0.35F;
   static final float END_SECONDS = 1.0F;
   public final KillStyleBase style;
   public final KillContext ctx;
   public final BodyProxy.Part[] parts = BodyProxy.newParts();
   public final BodyProxy.Part[] rest = BodyProxy.newParts();
   public final BodyProxy.Look look = new BodyProxy.Look();
   public final FxParticles particles;
   public final float size;
   public final float speed;
   public final float intensity;
   public final float opacity;
   public final float glow;
   public final boolean crit;
   public final boolean slowmo;
   public final boolean classicRed;
   public final float linger;
   public final KillEffectsModule.BodyEnd bodyEnd;
   public final boolean preview;
   public float t;
   public final float styleSeconds;
   public final float total;
   public Object data;
   public final Rng rng = new Rng();
   private Ragdoll ragdoll;
   private final PhysicsClock clock = new PhysicsClock();
   private final BlockCollider collider = new BlockCollider((World)null);
   private boolean dustSpawned;
   public float settledAt = -1.0F;
   public boolean trace;
   public static boolean traceNext;
   private int traceSteps;
   FxFrame passFrame;
   public float light = 1.0F;
   public float night;
   private long sparkleFrame = -1L;
   private float sparklesDue;
   private float dustDue;
   private final float[] sx = new float[512];
   private final float[] sy = new float[512];
   private final float[] sz = new float[512];
   private final int[] sc = new int[512];

   KillFx(KillStyleBase style, KillContext ctx, int seed, FxParticles particles, KillEffectsModule m, boolean preview) {
      super(Anchor.fixed(ctx.feetX, ctx.feetY, ctx.feetZ, 0.0D, 0.0D, 0.0D), Lifecycle.ANIMATION, 1000000.0F, 1.0F, seed, ctx.body.entityId);
      this.style = style;
      this.ctx = ctx;
      this.particles = particles;
      this.preview = preview;
      this.size = m.size.getFloat();
      this.speed = m.speed.getFloat();
      this.intensity = m.intensity.getFloat();
      this.opacity = m.opacity.getFloat();
      this.glow = m.glow.get()?m.glowIntensity.getFloat():0.0F;
      this.crit = ctx.crit && m.critBoost.get();
      this.slowmo = m.slowmo.get();
      this.classicRed = m.particleStyle.get() == KillEffectsModule.ParticleStyle.CLASSIC_RED;
      this.linger = preview?Math.min(0.8F, m.linger.getFloat()):m.linger.getFloat();
      this.bodyEnd = (KillEffectsModule.BodyEnd)m.bodyEnd.get();
      this.rng.seed(seed);
      BodyProxy.rest(ctx.body, this.rest);

      for(int i = 0; i < 6; ++i) {
         this.parts[i].copy(this.rest[i]);
      }

      this.look.reset();
      this.look.armor = m.showArmor.get();
      this.look.heldItem = m.showItem.get() && !preview;
      this.look.cape = m.showCape.get();
      this.styleSeconds = style.seconds(this);
      this.total = this.styleSeconds + (style.leavesBody()?this.linger + 1.0F:0.0F);
      if(preview) {
         this.collider.setFloor(0.0D);
      } else {
         this.collider.world = Minecraft.getMinecraft().theWorld;
         this.trace = traceNext;
         traceNext = false;
         if(this.trace) {
            FuriahsVisuals.LOG.info(String.format(Locale.ROOT, "[ragdoll] kill: impact (%.2f %.2f %.2f) from the feet, dir (%.2f %.2f %.2f), crit %s, pose yaw %.1f limbs %.2f/%.2f swing %.2f, v (%.2f %.2f %.2f)", new Object[]{Double.valueOf(ctx.impactX - ctx.feetX), Double.valueOf(ctx.impactY - ctx.feetY), Double.valueOf(ctx.impactZ - ctx.feetZ), Double.valueOf(ctx.dirX), Double.valueOf(ctx.dirY), Double.valueOf(ctx.dirZ), Boolean.valueOf(this.crit), Float.valueOf(ctx.body.pose.bodyYaw), Float.valueOf(ctx.body.pose.limbSwing), Float.valueOf(ctx.body.pose.limbSwingAmount), Float.valueOf(ctx.body.pose.swingProgress), Double.valueOf(ctx.body.vx), Double.valueOf(ctx.body.vy), Double.valueOf(ctx.body.vz)}));
         }
      }

   }

   public void start() {
      this.style.start(this);
   }

   public void update(float dt) {
      this.age += dt;
      float k = this.speed;
      if(this.slowmo && this.t < 0.3F) {
         k *= 0.35F;
      }

      float sdt = dt * k;
      this.t += sdt;
      this.style.update(this, sdt);
      if(this.ragdoll != null) {
         int steps = this.clock.advance((double)sdt);
         if(!this.preview) {
            this.collider.world = Minecraft.getMinecraft().theWorld;
         }

         for(int i = 0; i < steps; ++i) {
            this.ragdoll.step(this.collider);
            if(this.trace && ++this.traceSteps % 12 == 0) {
               RigidBox b = this.ragdoll.parts[1];
               StringBuilder speeds = new StringBuilder();

               for(RigidBox p : this.ragdoll.parts) {
                  speeds.append(String.format(Locale.ROOT, " %.2f", new Object[]{Double.valueOf(Math.sqrt(p.rigidStepSq()) * 120.0D)}));
               }

               FuriahsVisuals.LOG.info(String.format(Locale.ROOT, "[ragdoll] t=%.2f body c=(%.2f %.2f %.2f) axisY=(%.2f %.2f %.2f) sleeping=%s speeds%s", new Object[]{Double.valueOf((double)this.traceSteps / 120.0D), Double.valueOf(b.cx - this.ctx.feetX), Double.valueOf(b.cy - this.ctx.feetY), Double.valueOf(b.cz - this.ctx.feetZ), Double.valueOf(b.rot[1]), Double.valueOf(b.rot[4]), Double.valueOf(b.rot[7]), Boolean.valueOf(b.sleeping), speeds}));
            }
         }

         if(this.settledAt < 0.0F && this.ragdoll.asleep()) {
            this.settledAt = this.t;
         }
      }

   }

   public float realSeconds() {
      float slow = this.slowmo?Math.min(0.3F, this.total):0.0F;
      return (slow / 0.35F + (this.total - slow)) / this.speed;
   }

   public boolean isFinished() {
      return this.t >= this.total || this.age >= this.life;
   }

   public void render(FxFrame f) {
      this.style.render(this, f);
   }

   public int passes() {
      return this.style.passes(this);
   }

   public void drawPass(FxPass pass) {
      if(this.passFrame != null) {
         this.style.renderPass(this, this.passFrame, pass);
      }

   }

   public double passDistanceSq() {
      return this.passFrame == null?0.0D:this.passFrame.distanceSqToEye(this.ctx.feetX, this.ctx.feetY + 0.9D, this.ctx.feetZ);
   }

   public void passFailed() {
      this.age = this.life;
   }

   public float p() {
      return Easing.clamp01(this.t / this.styleSeconds);
   }

   public float phase(float from, float to) {
      return Easing.clamp01((this.t - from) / Math.max(1.0E-4F, to - from));
   }

   public float alpha() {
      return this.opacity;
   }

   public int color(float at) {
      return this.style.color.getColor(at);
   }

   public int count(float base, boolean critDoubles) {
      float n = base * this.intensity * (critDoubles && this.crit?1.8F:1.0F) * (this.lod?0.35F:1.0F);
      return Math.max(1, Math.round(n));
   }

   public int goreColor(int i) {
      if(this.classicRed) {
         float k = Rng.hash(this.seed, 9000 + i);
         return ColorUtil.lerp(-8779254, -2875874, k);
      } else {
         return this.color(Rng.hash(this.seed, 9100 + i));
      }
   }

   public void burst(double x, double y, double z, double dx, double dy, double dz, int n, float speed, float spread) {
      for(int i = 0; i < n; ++i) {
         double ux = dx + (double)(this.rng.signed() * spread);
         double uz;
         double uy;
         double ul = Math.sqrt(ux * ux + (uy = dy + (double)(this.rng.signed() * spread) + 0.15D) * uy + (uz = dz + (double)(this.rng.signed() * spread)) * uz);
         if(ul >= 1.0E-6D) {
            float v = speed * (0.35F + this.rng.next() * 0.9F) * this.size;
            float vx = (float)(ux / ul * (double)v);
            float vy = (float)(uy / ul * (double)v);
            float vz = (float)(uz / ul * (double)v);
            if(this.classicRed) {
               this.particles.spawn(12, x, y, z, vx, vy, vz, 1.4F + this.rng.next() * 0.8F, (0.035F + this.rng.next() * 0.045F) * this.size).gravity(17.0F).drag(0.5F).ground(this.ctx.groundY + 0.01D, 0, 0.05F).flags(4).color(this.goreColor(i)).alpha(this.opacity).fade(0.0F, 0.75F);
            } else if(i % 3 == 0) {
               this.particles.spawn(10, x, y, z, vx, vy, vz, 0.8F + this.rng.next() * 0.5F, (0.05F + this.rng.next() * 0.05F) * this.size).gravity(12.0F).drag(1.2F).ground(this.ctx.groundY + 0.02D, 2, 0.35F).spin(this.rng.next() * 6.0F, this.rng.signed() * 9.0F).axis(this.rng.signed(), this.rng.signed(), this.rng.signed()).color(this.style.color, this.rng.next()).alpha(this.opacity).hot(0.3F);
            } else {
               this.particles.spawn(1, x, y, z, vx, vy, vz, 0.45F + this.rng.next() * 0.4F, (0.05F + this.rng.next() * 0.04F) * this.size).gravity(9.0F).drag(2.2F).ground(this.ctx.groundY + 0.02D, 1, 0.3F).color(this.style.color, this.rng.next()).alpha(this.opacity).hot(0.7F).end(0.01F);
            }
         }
      }

   }

   public Ragdoll ragdoll() {
      if(this.ragdoll == null) {
         this.ragdoll = new Ragdoll();
         this.ragdoll.build(this.parts, this.ctx.body.slim);
      }

      return this.ragdoll;
   }

   public boolean hasRagdoll() {
      return this.ragdoll != null;
   }

   public boolean focus(double[] out) {
      if(this.ragdoll != null) {
         out[0] = this.ragdoll.parts[1].cx;
         out[1] = this.ragdoll.parts[1].cz;
         return true;
      } else {
         return this.style.focus(this, out);
      }
   }

   public void applyRagdoll() {
      if(this.ragdoll != null) {
         this.ragdoll.apply(this.parts, this.clock.alpha());
      }

   }

   public BlockCollider collider() {
      return this.collider;
   }

   public float endPhase() {
      return !this.style.leavesBody()?0.0F:Easing.clamp01((this.t - this.styleSeconds - this.linger) / 1.0F);
   }

   public void drawBody(FxFrame f) {
      this.applyRagdoll();
      this.drawParts(f, this.parts);
   }

   public void drawParts(FxFrame f, BodyProxy.Part[] parts) {
      float end = this.endPhase();
      float alpha = this.look.alpha;
      if(end > 0.0F) {
         switch(this.bodyEnd) {
         case SINK:
            double down = (double)Easing.inCubic(end) * 1.1D;

            for(BodyProxy.Part p : parts) {
               p.y -= down;
            }

            this.look.alpha = alpha * (1.0F - Easing.smoothstep(0.7F, 1.0F, end));
            if(Rng.hash(this.seed, (int)(this.t * 20.0F)) < 0.35F && !this.lod) {
               this.dustDue += f.dt * 60.0F;

               while(this.dustDue >= 1.0F) {
                  --this.dustDue;
                  this.dustPuff(end);
               }
            }
            break;
         case DUST:
            if(!this.dustSpawned) {
               this.dustSpawned = true;
               this.spawnSkinDust(this.count(160.0F, false), 0.6F, 1.3F);
            }

            if(DissolveShader.available()) {
               this.look.dissolve = end;
               this.look.dissolveNoise = 0.55F;
               this.look.sweepX = 0.0D;
               this.look.sweepY = -1.0D;
               this.look.sweepZ = 0.0D;
               this.look.sweepFrom = -(this.ctx.feetY + 2.0D);
               this.look.sweepLength = 2.0D;
               this.look.edgeColor = -5201776;
               this.look.edgeWidth = 0.06F;
            } else {
               this.look.alpha = alpha * (1.0F - end);
            }
            break;
         default:
            this.look.alpha = alpha * (1.0F - Easing.smoothstep(0.0F, 1.0F, end));
         }
      }

      this.look.alpha *= this.opacity;
      this.look.lightmap = !this.preview;
      this.look.crossfade = this.lod;
      if(this.look.sweepTo != null && this.look.sweep > 0.0F && this.look.sweep < 1.0F && !this.lod && BodyProxy.sweepCrossfades() && f.frameId != this.sparkleFrame) {
         this.sparkleFrame = f.frameId;
         this.sweepSparkles(f.dt);
      }

      this.look.now = f.now;
      BodyProxy.draw(this.ctx.body, parts, this.look, f.viewX, f.viewY, f.viewZ);
      this.look.alpha = alpha;
      if(end > 0.0F && this.bodyEnd == KillEffectsModule.BodyEnd.SINK) {
         double down = (double)Easing.inCubic(end) * 1.1D;

         for(BodyProxy.Part p : parts) {
            p.y += down;
         }
      }

   }

   private void sweepSparkles(float dt) {
      this.sparklesDue += dt * 45.0F * this.intensity;
      BodyProxy.Part b = this.parts[1];

      while(true) {
         double z;
         double y;
         double x;
         while(true) {
            if(this.sparklesDue < 1.0F) {
               return;
            }

            --this.sparklesDue;
            if(this.look.sweepMode == 2) {
               double u = (double)this.rng.signed();
               double w;
               double v;
               double l = Math.sqrt(u * u + (v = (double)this.rng.signed()) * v + (w = (double)this.rng.signed()) * w);
               if(l < 0.001D) {
                  continue;
               }

               double r = (double)(this.look.sweep * this.look.sweepSpan) / l;
               x = this.look.sweepOX + u * r;
               y = this.look.sweepOY + v * r;
               z = this.look.sweepOZ + w * r;
               if(Math.abs(x - b.x) > 0.45D || Math.abs(z - b.z) > 0.45D || y < this.ctx.feetY - 0.1D || y > this.ctx.feetY + 2.0D) {
                  continue;
               }
               break;
            }

            double up = this.look.sweepMode == 1?1.0D - (double)this.look.sweep:(double)this.look.sweep;
            double a = (double)this.rng.next() * 3.141592653589793D * 2.0D;
            double r = 0.22D + 0.12D * (double)this.rng.next();
            x = b.x + Math.cos(a) * r;
            y = this.look.sweepOY + up * (double)this.look.sweepSpan;
            z = b.z + Math.sin(a) * r;
            break;
         }

         this.particles.spawn(5, x, y, z, this.rng.signed() * 0.15F, 0.15F + this.rng.next() * 0.25F, this.rng.signed() * 0.15F, 0.35F + this.rng.next() * 0.25F, (0.06F + this.rng.next() * 0.04F) * this.size).color(this.look.sweepEdgeColor | -16777216).alpha(this.opacity).end(0.01F);
      }
   }

   private void dustPuff(float end) {
      BodyProxy.Part b = this.parts[1];
      double gx = b.x + (double)this.rng.signed() * 0.5D;
      double gz = b.z + (double)this.rng.signed() * 0.5D;
      this.particles.spawn(8, gx, this.ctx.groundY + 0.05D, gz, this.rng.signed() * 0.3F, 0.4F + this.rng.next() * 0.3F, this.rng.signed() * 0.3F, 0.9F, 0.35F).end(0.7F).drag(1.5F).color(-7701398);
   }

   public void spawnSkinDust(int n, float lift, float life) {
      n = Math.min(n, this.sc.length);
      int per = Math.max(1, n / 6);
      int k = 0;

      for(int part = 0; part < 6; ++part) {
         if(this.parts[part].visible) {
            k += SkinSampler.sample(this.ctx.body, part, per, this.rng, this.sx, this.sy, this.sz, this.sc, k, this.color(0.5F));
         }
      }

      for(int i = 0; i < k; ++i) {
         int partx = Math.min(5, i / per);
         BodyProxy.Part p = this.parts[partx];
         double wx = p.x + p.r[0] * (double)this.sx[i] + p.r[1] * (double)this.sy[i] + p.r[2] * (double)this.sz[i];
         double wy = p.y + p.r[3] * (double)this.sx[i] + p.r[4] * (double)this.sy[i] + p.r[5] * (double)this.sz[i];
         double wz = p.z + p.r[6] * (double)this.sx[i] + p.r[7] * (double)this.sy[i] + p.r[8] * (double)this.sz[i];
         float delay = this.rng.next() * 0.8F;
         this.particles.spawn(12, wx, wy, wz, this.rng.signed() * 0.25F, lift * (0.5F + this.rng.next()), this.rng.signed() * 0.25F, life, 0.04F * this.size).drag(0.8F).color(this.sc[i]).spin(this.rng.next() * 6.0F, this.rng.signed() * 4.0F).end(0.01F).delay(delay);
      }

   }
}
