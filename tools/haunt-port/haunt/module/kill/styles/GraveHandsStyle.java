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

public final class GraveHandsStyle extends GraveyardBase {
   private static final int MAX = 9;
   private final SliderSetting count = (SliderSetting)this.setting(new SliderSetting("gravehands.count", "Hands", "How many hands come out of the ground.", 3.0D, 9.0D, 6.0D, 1.0D));
   private final SliderSetting radius = (SliderSetting)this.setting((new SliderSetting("gravehands.radius", "Ring Size", "How far from the stone the hands come out.", 0.7D, 2.0D, 1.15D, 0.05D)).unit(" blocks"));
   private final SliderSetting speed = (SliderSetting)this.setting((new SliderSetting("gravehands.speed", "Speed", "How fast the hands move (lower = slower and creepier).", 0.6D, 1.6D, 1.0D, 0.05D)).unit("x"));
   private final BooleanSetting cross = (BooleanSetting)this.setting(new BooleanSetting("gravehands.cross", "Stone Cross", "A stone cross rises in the middle.", true));
   private final ColorSetting stoneColor = (ColorSetting)this.setting((new ColorSetting("gravehands.stone", "Stone", "Color of the stone cross.", -5197648)).secondary());

   public GraveHandsStyle(KillEffectsModule module) {
      super(module, KillStyle.GRAVE_HANDS, new ColorSetting("gravehands.color", "Grave Light", "Color of the light under the ring and the smoke glow.", -7539256));
   }

   private float k() {
      return 1.0F / this.speed.getFloat();
   }

   private int hands() {
      return Math.max(3, Math.min(9, this.count.getInt()));
   }

   public float seconds(KillFx fx) {
      return 0.7F + 3.6F * this.k() + 0.5F;
   }

   public void start(KillFx fx) {
      HauntTextures.warmUp();
      GraveHandsStyle.State s = new GraveHandsStyle.State();
      fx.data = s;
      s.frame = frame(fx);
      s.seed = fx.rng.next() * 40.0F;
      float k = this.k();
      int n = this.hands();
      double R = (double)this.radius.getFloat();
      float a0 = fx.rng.next() * 6.2832F;

      for(int i = 0; i < n; ++i) {
         GraveHandsStyle.Hand h = new GraveHandsStyle.Hand();
         double ang = (double)(a0 + 6.2832F * (float)i / (float)n + fx.rng.signed() * 0.35F);
         double r = R * (double)(0.8F + fx.rng.next() * 0.4F);
         h.lx = Math.cos(ang) * r;
         h.lz = Math.sin(ang) * r;
         h.yaw = Math.atan2(-h.lx, -h.lz) + (double)(fx.rng.signed() * 0.6F);
         h.delay = 0.35F + fx.rng.next() * 0.9F * k;
         h.rise = (0.45F + fx.rng.next() * 0.35F) * k;
         h.hold = (0.9F + fx.rng.next() * 0.9F) * k;
         h.exit = (0.6F + fx.rng.next() * 0.4F) * k;
         h.height = 0.12D + (double)(fx.rng.next() * 0.3F);
         h.scale = 0.85F + fx.rng.next() * 0.35F;
         h.seed = fx.rng.next() * 100.0F;
         h.smoke = i % 3 == 1 || fx.rng.next() < 0.2F;
         h.lean = 0.25F + fx.rng.next() * 0.3F;
         float last = this.seconds(fx) - 0.4F;
         float total = h.delay + h.rise + h.hold + (h.smoke?0.35F:h.exit);
         if(total > last) {
            h.hold = Math.max(0.3F, h.hold - (total - last));
         }

         s.hands[i] = h;
      }

      s.n = n;
   }

   public void update(KillFx fx, float dt) {
      GraveHandsStyle.State s = (GraveHandsStyle.State)fx.data;
      float t = fx.t;
      GraveyardBase.Frame fr = s.frame;
      if(!s.buried && t > 0.05F) {
         s.buried = true;
         soilBurst(fx, fx.ctx.feetX, fx.ctx.feetZ, fx.count(14.0F, false), 0.7F);
         fog(fx, fr.ox, fr.oz, 10, (float)this.radius.get() + 0.3F, -10264707, 0.3F);
      }

      if(t < this.seconds(fx) - 0.6F && fx.rng.next() < dt * 5.0F) {
         fog(fx, fr.ox, fr.oz, 1, (float)this.radius.get() + 0.4F, -10264707, 0.28F);
      }

      for(int i = 0; i < s.n; ++i) {
         GraveHandsStyle.Hand h = s.hands[i];
         double x = fr.wx(h.lx, h.lz);
         double z = fr.wz(h.lx, h.lz);
         if(!h.broke && t > h.delay) {
            h.broke = true;
            soilBurst(fx, x, z, fx.count(12.0F, false), 0.75F * h.scale);
         }

         float leave = h.delay + h.rise + h.hold;
         if(!h.left && t > leave) {
            h.left = true;
            if(h.smoke) {
               this.puff(fx, h, x, z);
            } else {
               soilBurst(fx, x, z, fx.count(5.0F, false), 0.4F);
            }
         }
      }

   }

   private void puff(KillFx fx, GraveHandsStyle.Hand h, double x, double z) {
      if(!fx.lod) {
         double y = fx.ctx.groundY + h.height * (double)fx.size + 0.15D * (double)fx.size;

         for(int i = 0; i < 9; ++i) {
            fx.particles.spawn(8, x + (double)(fx.rng.signed() * 0.12F), y + (double)(fx.rng.next() * 0.25F), z + (double)(fx.rng.signed() * 0.12F), fx.rng.signed() * 0.35F, 0.25F + fx.rng.next() * 0.45F, fx.rng.signed() * 0.35F, 1.0F + fx.rng.next() * 0.6F, (0.2F + fx.rng.next() * 0.15F) * fx.size).end(0.55F * fx.size).drag(1.4F).color(ColorUtil.lerp(-13684944, this.color.getColor(0.5F), 0.15F)).alpha(0.6F * fx.opacity);
         }

         for(int i = 0; i < 6; ++i) {
            fx.particles.spawn(3, x + (double)(fx.rng.signed() * 0.1F), y, z + (double)(fx.rng.signed() * 0.1F), fx.rng.signed() * 0.2F, 0.4F + fx.rng.next() * 0.6F, fx.rng.signed() * 0.2F, 0.9F + fx.rng.next() * 0.5F, (0.03F + fx.rng.next() * 0.02F) * fx.size).drag(1.0F).color(this.color.getColor(0.5F)).alpha(0.8F * fx.opacity).flags(2);
         }
      }

   }

   public void render(KillFx fx, FxFrame f) {
      GraveHandsStyle.State s = (GraveHandsStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      GraveyardBase.Frame fr = s.frame;
      bury(fx, f, t, 0.0F, 0.75F);
      if(HauntTextures.ensure()) {
         double sz = (double)fx.size;
         float end = this.seconds(fx);
         int glow = this.color.getColor(0.5F) | -16777216;
         float ringOn = Easing.smoothstep(0.2F, 1.0F, t) * (1.0F - Easing.smoothstep(end - 0.7F, end, t));
         if(ringOn > 0.01F) {
            RenderState.additive();
            Vfx.flatSprite(Textures.GROUND_GLOW, f.rx(fr.ox), f.ry(fx.ctx.groundY + 0.015D), f.rz(fr.oz), ((double)this.radius.getFloat() + 0.5D) * sz, (double)(t * 0.2F), alpha(glow, 0.2F * a * ringOn * Math.max(0.4F, fx.glow)));
            Vfx.flush();
         }

         for(int i = 0; i < s.n; ++i) {
            GraveHandsStyle.Hand h = s.hands[i];
            float hEnd = h.delay + h.rise + h.hold + (h.smoke?0.35F:h.exit);
            float mark = Easing.smoothstep(h.delay - 0.15F, h.delay + 0.3F, t) * (1.0F - Easing.smoothstep(hEnd, hEnd + 0.8F, t));
            groundMark(f, fx, fr.wx(h.lx, h.lz), fr.wz(h.lx, h.lz), 0.42D * sz * (double)h.scale, (double)h.seed, mark * a);
         }

         HauntKit.clipBelow(fx.ctx.groundY);

         try {
            if(this.cross.get()) {
               float rise = Easing.outBack(Easing.clamp01((t - 0.1F) / 0.75F), 1.3F);
               float sink = Easing.inCubic(Easing.clamp01((t - (end - 0.65F)) / 0.65F));
               double H = stoneHeight(GraveyardBase.Stone.CROSS, sz);
               double lift = ((double)Math.max(0.0F, rise) - 1.0D) * (H + 0.1D) - (double)sink * (H + 0.1D);
               HauntKit.Xf st = fr.base(new HauntKit.Xf(), 0.0D, 0.0D, lift);
               stone(f, st, GraveyardBase.Stone.CROSS, sz, this.stoneColor.getColor(0.5F) | -16777216, fr.light, a);
            }

            for(int i = 0; i < s.n; ++i) {
               this.drawHand(fx, f, s, s.hands[i], t, a);
            }
         } finally {
            HauntKit.noClip();
         }
      }

   }

   private void drawHand(KillFx fx, FxFrame f, GraveHandsStyle.State s, GraveHandsStyle.Hand h, float t, float a) {
      float t1 = h.delay + h.rise;
      float t2 = t1 + h.hold;
      float t3 = t2 + (h.smoke?0.35F:h.exit);
      if(t >= h.delay && t <= t3) {
         GraveyardBase.Frame fr = s.frame;
         double sz = (double)fx.size * (double)h.scale;
         HauntKit.HandPose p = new HauntKit.HandPose();
         float alpha = a;
         float scale = 1.0F;
         double top = h.height;
         double y;
         if(t < t1) {
            float u = (t - h.delay) / h.rise;
            float steps = Easing.outBack(Easing.clamp01(u * 1.6F), 1.8F) * 0.55F + Easing.outBack(Easing.clamp01(u * 1.6F - 0.6F), 2.0F) * 0.45F;
            y = -0.45D + (top + 0.45D) * (double)steps;

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.3F + 0.5F * wander(t, h.seed + (float)i * 2.3F, 5.0F + (float)i);
            }

            p.spread = 0.3F;
         } else if(t < t2) {
            float u = (t - t1) / h.hold;
            y = top + 0.035D * Math.sin((double)(t * 2.5F + h.seed));
            float grab = (float)Math.max(0.0D, Math.sin((double)(u * 8.0F + h.seed)));

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.1F + 0.7F * grab + 0.12F * wander(t, h.seed + (float)i * 3.7F, 6.5F);
            }

            p.thumb = 0.25F + 0.6F * grab;
            p.spread = 0.4F - 0.25F * grab;
         } else if(h.smoke) {
            float u = Easing.clamp01((t - t2) / 0.35F);
            y = top + 0.12D * (double)u;
            alpha = a * (1.0F - Easing.smoothstep(0.0F, 1.0F, u));
            scale = 1.0F - 0.35F * u;

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.1F;
            }

            p.spread = 0.5F;
         } else {
            float u = Easing.clamp01((t - t2) / h.exit);
            y = top - (top + 0.75D) * (double)Easing.inCubic(u);

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = Math.min(1.0F, 0.25F + 0.8F * u + 0.1F * (float)Math.sin((double)(t * 20.0F + (float)i)));
            }

            p.thumb = 0.5F + 0.4F * u;
         }

         if(alpha > 0.01F) {
            p.wristBend = (float)(0.3D * Math.sin((double)(t * 2.1F + h.seed)) - 0.1D);
            p.wristTwist = 0.3F * (float)Math.sin((double)(t * 3.1F + h.seed));
            double sway = 0.1D * Math.sin((double)(t * 1.7F + h.seed));
            HauntKit.Xf base = fr.base(new HauntKit.Xf(), h.lx, h.lz, y * sz);
            base.rotY(h.yaw).rotX((double)(-h.lean) + sway).rotZ(0.15D * Math.sin((double)(t * 1.3F + h.seed * 0.7F)));
            base.scale(1.3D * sz * (double)scale);
            RenderState.translucent(true);
            int skin = ColorUtil.withAlpha(h.smoke && t > t2?ColorUtil.lerp(-9532351, -13684944, Easing.clamp01((t - t2) / 0.35F)):-9532351, alpha);
            HauntKit.hand(f, base, p, HauntTextures.ZOMBIE, skin, ColorUtil.withAlpha(-7570098, alpha), fr.light, true, 0.6D);
            Vfx.flush();
         }
      }

   }

   private static final class Hand {
      double lx;
      double lz;
      double yaw;
      float delay;
      float rise;
      float hold;
      float exit;
      double height;
      float scale;
      float seed;
      float lean;
      boolean smoke;
      boolean broke;
      boolean left;

      private Hand() {
      }
   }

   private static final class State {
      GraveyardBase.Frame frame;
      float seed;
      int n;
      boolean buried;
      final GraveHandsStyle.Hand[] hands;

      private State() {
         this.hands = new GraveHandsStyle.Hand[9];
      }
   }
}
