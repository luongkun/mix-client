package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.haunt.fx.HauntKit;
import mix.client.mixui.integration.haunt.haunt.fx.HauntTextures;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.GraveyardBase;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class ZombieAwakeningStyle extends GraveyardBase {
   private static final double FORE = 0.62D;
   private static final double SCALE = 1.35D;
   private static final float[] PUSH_AT = new float[]{0.0F, 0.2F, 0.38F, 0.62F, 0.84F};
   private static final float[] PUSH_H = new float[]{0.22F, 0.18F, 0.26F, 0.4F, 0.26F};
   private final SliderSetting speed = (SliderSetting)this.setting((new SliderSetting("zombieawake.speed", "Speed", "How fast the arm moves (lower = slower and creepier).", 0.6D, 1.6D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting dirt = (SliderSetting)this.setting((new SliderSetting("zombieawake.dirt", "Dirt", "How much soil, dust and crumbs fly.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting second = (BooleanSetting)this.setting(new BooleanSetting("zombieawake.second", "Second Hand", "A second, smaller hand claws out beside the first one.", false));
   private final ColorSetting skin = (ColorSetting)this.setting((new ColorSetting("zombieawake.skin", "Skin", "Color of the rotten skin.", -9532351)).secondary());

   public ZombieAwakeningStyle(KillEffectsModule module) {
      super(module, KillStyle.ZOMBIE_AWAKENING, new ColorSetting("zombieawake.color", "Crack Light", "Color of the light leaking out of the cracks.", -7539256));
   }

   private float k() {
      return 1.0F / this.speed.getFloat();
   }

   public float seconds(KillFx fx) {
      return 0.6F + 3.4F * this.k() + 0.6F;
   }

   public void start(KillFx fx) {
      HauntTextures.warmUp();
      ZombieAwakeningStyle.State s = new ZombieAwakeningStyle.State();
      fx.data = s;
      s.frame = frame(fx);
      s.seed = fx.rng.next() * 60.0F;
      s.side = fx.rng.next() < 0.5F?-1:1;
      s.arms[0] = new ZombieAwakeningStyle.Arm(0.0D, 0.08D, 0.0F, 1.0F, 1.0F, s.seed);
      s.arms[1] = new ZombieAwakeningStyle.Arm((double)s.side * 0.62D, -0.18D, 0.45F, 0.82F, 0.62F, s.seed + 17.0F);
   }

   private int arms() {
      return this.second.get()?2:1;
   }

   public void update(KillFx fx, float dt) {
      ZombieAwakeningStyle.State s = (ZombieAwakeningStyle.State)fx.data;
      float t = fx.t;
      float k = this.k();
      float d = this.dirt.getFloat();
      GraveyardBase.Frame fr = s.frame;
      if(!s.buried && t > 0.05F) {
         s.buried = true;
         soilBurst(fx, fx.ctx.feetX, fx.ctx.feetZ, fx.count(14.0F * d, false), 0.7F);
      }

      for(int i = 0; i < this.arms(); ++i) {
         ZombieAwakeningStyle.Arm arm = s.arms[i];
         double x = fr.wx(arm.lx, arm.lz);
         double z = fr.wz(arm.lx, arm.lz);
         float rs = 0.55F + arm.delay * k;
         if(!fx.lod && t > rs - 0.45F && t < rs && fx.rng.next() < dt * 20.0F) {
            fx.particles.spawn(12, x + (double)(fx.rng.signed() * 0.25F), fx.ctx.groundY + 0.03D, z + (double)(fx.rng.signed() * 0.25F), fx.rng.signed() * 0.3F, 0.6F + fx.rng.next() * 0.8F, fx.rng.signed() * 0.3F, 0.5F, 0.025F * fx.size).gravity(12.0F).ground(fx.ctx.groundY + 0.01D, 0, 0.0F).color(-12175310).alpha(fx.opacity);
         }

         for(int p = 0; p < PUSH_AT.length; ++p) {
            float at = rs + PUSH_AT[p] * 1.25F * k;
            if((arm.pushed & 1 << p) == 0 && t > at) {
               arm.pushed |= 1 << p;
               float strength = p == 0?1.0F:(p == 3?1.25F:0.55F);
               soilBurst(fx, x, z, fx.count((p == 3?22.0F:9.0F) * d * arm.scale, false), strength * arm.scale);
               if(p == 0 || p == 3) {
                  fog(fx, x, z, 4, 0.6F, -10264707, 0.3F);
               }
            }
         }

         float out0 = rs + 1.25F * k;
         float out1 = out0 + 1.3F * k * arm.hold;
         if(!fx.lod && t > out0 && t < out1 && fx.rng.next() < dt * 9.0F * d && arm.last != null) {
            double along = (double)fx.rng.next() * 0.62D * 0.9D;
            HauntKit.Xf xf = arm.last;
            fx.particles.spawn(12, xf.px(0.06D * (double)fx.rng.signed(), -along, 0.05D), xf.py(0.0D, -along, 0.0D), xf.pz(0.06D * (double)fx.rng.signed(), -along, 0.05D), fx.rng.signed() * 0.2F, 0.0F, fx.rng.signed() * 0.2F, 0.8F, 0.022F * fx.size).gravity(10.0F).ground(fx.ctx.groundY + 0.01D, 0, 0.0F).color(-12702953).alpha(fx.opacity);
         }

         if(!arm.gone && t > out1 + 0.85F * k) {
            arm.gone = true;
            soilBurst(fx, x, z, fx.count(8.0F * d, false), 0.45F);
            leaves(fx, x, fx.ctx.groundY + 1.1D, z, fx.count(4.0F * d, false));
         }
      }

   }

   public void render(KillFx fx, FxFrame f) {
      ZombieAwakeningStyle.State s = (ZombieAwakeningStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float k = this.k();
      GraveyardBase.Frame fr = s.frame;
      bury(fx, f, t, 0.0F, 0.6F);
      if(HauntTextures.ensure()) {
         double sz = (double)fx.size;
         float end = this.seconds(fx);
         int skinColor = this.skin.getColor(0.5F) | -16777216;
         int glow = this.color.getColor(0.5F) | -16777216;

         for(int i = 0; i < this.arms(); ++i) {
            ZombieAwakeningStyle.Arm arm = s.arms[i];
            float rs = 0.55F + arm.delay * k;
            double x = fr.wx(arm.lx, arm.lz);
            double z = fr.wz(arm.lx, arm.lz);
            float crack = Easing.smoothstep(rs - 0.5F, rs + 1.3F * k, t) * (1.0F - Easing.smoothstep(end - 0.8F, end, t));
            groundMark(f, fx, x, z, (0.35D + 0.4D * (double)crack) * sz * (double)arm.scale, (double)(s.seed + (float)i), Math.min(1.0F, crack * 1.6F) * a);
            float light = crack * (0.6F + 0.4F * (float)Math.sin((double)(t * 7.0F + (float)i)));
            if(light > 0.01F) {
               RenderState.additive();
               double gy = f.ry(fx.ctx.groundY + 0.016D);
               Vfx.flatSprite(HauntTextures.CRACKS, f.rx(x), gy, f.rz(z), (0.3D + 0.4D * (double)crack) * sz * (double)arm.scale, (double)(s.seed + (float)i), alpha(glow, 0.55F * a * light * Math.max(0.35F, fx.glow)));
               Vfx.flatSprite(Textures.GROUND_GLOW, f.rx(x), gy + 0.002D, f.rz(z), 0.5D * sz * (double)arm.scale, 0.0D, alpha(glow, 0.22F * a * light));
               Vfx.flush();
            }

            HauntKit.clipBelow(fx.ctx.groundY);

            try {
               this.drawArm(fx, f, s, arm, t, k, a, skinColor, rs);
            } finally {
               HauntKit.noClip();
            }
         }
      }

   }

   private static double rise(ZombieAwakeningStyle.Arm arm, float u) {
      double h = -0.42D;

      for(int p = 0; p < PUSH_AT.length; ++p) {
         float e = Easing.clamp01((u - PUSH_AT[p]) / 0.13F);
         h += (double)(PUSH_H[p] * Easing.outBack(e, 2.2F));
      }

      return h * (double)arm.reach + (1.0D - (double)arm.reach) * -0.42D;
   }

   private void drawArm(KillFx fx, FxFrame f, ZombieAwakeningStyle.State s, ZombieAwakeningStyle.Arm arm, float t, float k, float a, int skinColor, float rs) {
      float riseDur = 1.25F * k;
      float hold = 1.3F * k * arm.hold;
      float sinkDur = 0.85F * k;
      float out0 = rs + riseDur;
      float out1 = out0 + hold;
      if(t >= rs && t <= out1 + sinkDur) {
         double sz = (double)fx.size * (double)arm.scale;
         GraveyardBase.Frame fr = s.frame;
         HauntKit.HandPose p = new HauntKit.HandPose();
         float tw = twitch(t, arm.seed);
         double y;
         double lean;
         double twist;
         if(t < out0) {
            float u = (t - rs) / riseDur;
            y = rise(arm, u);
            lean = 0.1D + 0.2D * Math.sin((double)(t * 5.0F + arm.seed)) + (double)(tw * 0.5F);
            twist = (double)(0.5F * (float)Math.sin((double)(t * 3.2F + arm.seed)) + tw);

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.25F + 0.6F * wander(t, arm.seed + (float)i * 2.7F, 4.5F + (float)i * 0.6F);
            }

            p.thumb = 0.3F + 0.5F * wander(t, arm.seed + 8.0F, 3.4F);
            p.spread = 0.3F + 0.25F * wander(t, arm.seed + 3.0F, 2.2F);
         } else if(t < out1) {
            float u = (t - out0) / hold;
            y = rise(arm, 1.0F) + 0.03D * Math.sin((double)(t * 2.2F + arm.seed));
            lean = 0.2D + 0.28D * Math.sin((double)(t * 1.6F + arm.seed)) + (double)(tw * 0.7F);
            twist = (double)(0.7F * (float)Math.sin((double)(t * 1.3F + arm.seed)) + tw * 1.2F);
            float grab = (float)Math.max(0.0D, Math.sin((double)(u * 9.4F + arm.seed)));

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.1F + 0.75F * grab * (0.75F + 0.25F * wander(t, arm.seed + (float)i, 6.0F)) + 0.1F * wander(t, arm.seed + (float)i * 4.1F, 7.0F);
            }

            p.thumb = 0.2F + 0.7F * grab;
            p.spread = 0.45F - 0.3F * grab;
         } else {
            float u = Easing.clamp01((t - out1) / sinkDur);
            float e = Easing.inCubic(u);
            y = rise(arm, 1.0F) - (rise(arm, 1.0F) + 0.75D) * (double)e + 0.04D * Math.sin((double)(t * 30.0F)) * (double)(1.0F - u);
            lean = 0.15D + (double)(tw * 0.6F) * (double)(1.0F - u);
            twist = (double)(0.4F * (float)Math.sin((double)(t * 2.0F + arm.seed)));

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = Math.min(1.0F, 0.3F + u * 0.8F + 0.15F * (float)Math.sin((double)(t * 22.0F + (float)i)));
            }

            p.thumb = 0.4F + 0.5F * u;
            p.spread = 0.2F;
         }

         p.wristBend = (float)(0.35D * Math.sin((double)(t * 2.6F + arm.seed)) - 0.15D);
         p.wristTwist = 0.25F * (float)Math.sin((double)(t * 3.7F + arm.seed * 0.5F));
         HauntKit.Xf base = fr.base(new HauntKit.Xf(), arm.lx, arm.lz, y * sz);
         base.rotY(twist).rotX(lean).rotZ((double)(0.12F * (float)Math.sin((double)(t * 1.9F + arm.seed))));
         base.scale(1.35D * sz);
         arm.last = (new HauntKit.Xf()).set(base);
         RenderState.translucent(true);
         int skin = ColorUtil.withAlpha(skinColor, a);
         HauntKit.hand(f, base, p, HauntTextures.ZOMBIE, skin, ColorUtil.withAlpha(-7570098, a), fr.light, true, 0.62D);
         int cloth = ColorUtil.withAlpha(-14013388, a);
         HauntKit.box(f, base, HauntTextures.ZOMBIE, cloth, fr.light, 1.0D, -0.066D, -0.62D, -0.058D, 0.066D, -0.3844D, 0.058D);
         HauntKit.box(f, base, HauntTextures.ZOMBIE, HauntKit.darker(cloth, 0.8F), fr.light, 1.0D, -0.07D, -0.434D, -0.062D, 0.04D, -0.372D, 0.062D);
         HauntKit.Xf elbow = (new HauntKit.Xf()).set(base).translate(0.0D, -0.62D, 0.0D);
         HauntKit.box(f, elbow, HauntTextures.ZOMBIE, HauntKit.darker(skin, 0.78F), fr.light, 3.0D, -0.064D, -0.05D, -0.058D, 0.064D, 0.035D, 0.058D);
         elbow.rotX(1.05D);
         HauntKit.box(f, elbow, HauntTextures.ZOMBIE, cloth, fr.light, 1.0D, -0.068D, -0.7D, -0.06D, 0.068D, 0.0D, 0.06D);
         Vfx.flush();
      }

   }

   private static float twitch(float t, float seed) {
      float step = t * 2.6F + seed;
      int n = (int)Math.floor((double)step);
      float ph = step - (float)n;
      float h = hash(n, seed) - 0.5F;
      return h * 0.7F * (float)Math.exp((double)(-ph * 9.0F)) * (float)Math.sin((double)(ph * 40.0F));
   }

   private static float hash(int n, float seed) {
      int h = n * 374761393 + Float.floatToIntBits(seed) * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      h = h ^ h >>> 16;
      return (float)(h & 16777215) / 1.6777216E7F;
   }

   private static final class Arm {
      final double lx;
      final double lz;
      final float delay;
      final float scale;
      final float reach;
      final float hold;
      final float seed;
      int pushed;
      boolean gone;
      HauntKit.Xf last;

      Arm(double lx, double lz, float delay, float scale, float reach, float seed) {
         this.lx = lx;
         this.lz = lz;
         this.delay = delay;
         this.scale = scale;
         this.reach = reach;
         this.hold = reach < 1.0F?0.7F:1.0F;
         this.seed = seed;
      }
   }

   private static final class State {
      GraveyardBase.Frame frame;
      float seed;
      int side;
      boolean buried;
      final ZombieAwakeningStyle.Arm[] arms;

      private State() {
         this.arms = new ZombieAwakeningStyle.Arm[2];
      }
   }
}
