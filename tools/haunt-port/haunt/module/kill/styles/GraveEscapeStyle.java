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
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class GraveEscapeStyle extends GraveyardBase {
   private final SliderSetting stay = (SliderSetting)this.setting((new SliderSetting("graveescape.speed", "Speed", "How fast the hand moves (lower = a slower, creepier scene).", 0.6D, 1.6D, 1.0D, 0.05D)).unit("x"));
   private final SliderSetting dirt = (SliderSetting)this.setting((new SliderSetting("graveescape.dirt", "Dirt", "How much soil, dust and leaves fly.", 0.3D, 2.0D, 1.0D, 0.05D)).unit("x"));
   private final ColorSetting stoneColor = (ColorSetting)this.setting((new ColorSetting("graveescape.stone", "Stone", "Color of the headstone.", -5197648)).secondary());

   public GraveEscapeStyle(KillEffectsModule module) {
      super(module, KillStyle.GRAVE_ESCAPE, new ColorSetting("graveescape.color", "Glow", "Color of the faint light around the grave.", -7539256));
   }

   private float k(KillFx fx) {
      return 1.0F / this.stay.getFloat();
   }

   public float seconds(KillFx fx) {
      return 0.9F + 3.3F * this.k(fx);
   }

   public void start(KillFx fx) {
      HauntTextures.warmUp();
      GraveEscapeStyle.State s = new GraveEscapeStyle.State();
      fx.data = s;
      s.frame = frame(fx);
      s.handX = (double)((fx.rng.next() < 0.5F?-1.0F:1.0F) * (0.24F + fx.rng.next() * 0.05F) * fx.size);
      s.seed = fx.rng.next() * 50.0F;
   }

   public void update(KillFx fx, float dt) {
      GraveEscapeStyle.State s = (GraveEscapeStyle.State)fx.data;
      float t = fx.t;
      float k = this.k(fx);
      float d = this.dirt.getFloat();
      GraveyardBase.Frame fr = s.frame;
      if(!s.buried && t > 0.05F) {
         s.buried = true;
         soilBurst(fx, fx.ctx.feetX, fx.ctx.feetZ, fx.count(16.0F * d, false), 0.8F);
      }

      if(!s.stoneUp && t > 0.25F) {
         s.stoneUp = true;
         soilBurst(fx, fr.wx(0.0D, 0.0D), fr.wz(0.0D, 0.0D), fx.count(10.0F * d, false), 0.6F);
         fog(fx, fr.ox, fr.oz, 6, 0.9F, -10264707, 0.35F);
      }

      if(!s.handOut && t > 0.9F) {
         s.handOut = true;
         soilBurst(fx, fr.wx(s.handX, -0.35D), fr.wz(s.handX, -0.35D), fx.count(24.0F * d, false), 1.0F);
      }

      if(!s.slipped && t > 0.9F + 2.1F * k) {
         s.slipped = true;
         soilBurst(fx, fr.wx(s.handX, -0.3D), fr.wz(s.handX, -0.3D), fx.count(10.0F * d, false), 0.5F);
         leaves(fx, fr.ox, fx.ctx.groundY + 1.4D, fr.oz, fx.count(9.0F * d, false));
      }

   }

   public void render(KillFx fx, FxFrame f) {
      GraveEscapeStyle.State s = (GraveEscapeStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float k = this.k(fx);
      GraveyardBase.Frame fr = s.frame;
      bury(fx, f, t, 0.0F, 0.85F);
      if(HauntTextures.ensure()) {
         double sz = (double)fx.size;
         float rise = Easing.outBack(Easing.clamp01((t - 0.15F) / 0.7F), 1.15F);
         float sinkStone = Easing.inCubic(Easing.clamp01((t - (this.seconds(fx) - 0.6F)) / 0.6F));
         double stoneLift = ((double)Math.max(0.0F, rise) - 1.0D) * stoneHeight(GraveyardBase.Stone.GOTHIC, sz) - (double)sinkStone * stoneHeight(GraveyardBase.Stone.GOTHIC, sz);
         float shake = 0.0F;
         float hs = 0.9F;
         float h1 = hs + 0.7F * k;
         float h2 = h1 + 0.8F * k;
         float h3 = h2 + 0.6F * k;
         float h4 = h3 + 0.7F * k;
         if(t > h2 && t < h3) {
            shake = (float)Math.sin((double)(t * 60.0F)) * 0.012F;
         }

         HauntKit.Xf st = fr.base(new HauntKit.Xf(), (double)shake, 0.0D, stoneLift);
         float mark = Easing.smoothstep(0.6F, 1.0F, t) * (1.0F - Easing.smoothstep(this.seconds(fx) - 0.5F, this.seconds(fx), t));
         groundMark(f, fx, fr.wx(s.handX, -0.33D), fr.wz(s.handX, -0.33D), 0.55D * sz, (double)s.seed, mark * a);
         HauntKit.clipBelow(fx.ctx.groundY);

         try {
            stone(f, st, GraveyardBase.Stone.GOTHIC, sz, this.stoneColor.getColor(0.5F) | -16777216, fr.light, a);
            if(t > hs && t < h4 + 0.3F) {
               this.drawHand(fx, f, s, t, a, hs, h1, h2, h3, h4, stoneLift);
            }
         } finally {
            HauntKit.noClip();
         }

         RenderState.additive();
         float glow = a * Easing.smoothstep(0.3F, 1.0F, t) * (1.0F - sinkStone) * Math.max(0.3F, fx.glow);
         Vfx.sprite(Textures.GLOW, f.rx(fr.ox), f.ry(fr.oy + 0.7D * sz + stoneLift), f.rz(fr.oz), 2.2D * sz, 0.0F, alpha(this.color.getColor(0.5F), 0.16F * glow));
         Vfx.flush();
      }

   }

   private void drawHand(KillFx fx, FxFrame f, GraveEscapeStyle.State s, float t, float a, float hs, float h1, float h2, float h3, float h4, double stoneLift) {
      GraveyardBase.Frame fr = s.frame;
      double sz = (double)fx.size;
      double top = 0.92D * sz + stoneLift;
      HauntKit.HandPose p = new HauntKit.HandPose();
      float alpha = a;

      for(int i = 0; i < 4; ++i) {
         p.curl[i] = 0.15F + 0.55F * wander(t, s.seed + (float)i * 3.1F, 3.0F + (float)i * 0.4F);
      }

      p.thumb = 0.3F + 0.4F * wander(t, s.seed + 9.0F, 2.6F);
      p.spread = 0.25F + 0.2F * wander(t, s.seed + 5.0F, 1.7F);
      double y;
      double z;
      double bend;
      if(t < h1) {
         float u = Easing.outCubic((t - hs) / (h1 - hs));
         y = -0.3D + 0.55D * (double)u;
         z = -0.38D;
         bend = -0.15D + 0.1D * Math.sin((double)(t * 9.0F));
         p.wristTwist = 0.3F * (float)Math.sin((double)(t * 4.0F + s.seed));
      } else if(t < h2) {
         float u = Easing.inOutSine((t - h1) / (h2 - h1));
         y = 0.25D + (top - 0.1D - 0.25D) * (double)u;
         z = -0.38D + 0.2D * (double)u;
         bend = -0.1D + 0.2D * (double)u;
         p.wristTwist = 0.25F * (float)Math.sin((double)(t * 3.0F + s.seed)) * (1.0F - u);

         for(int i = 0; i < 4; ++i) {
            p.curl[i] *= 1.0F - 0.6F * u;
         }
      } else if(t < h3) {
         float u = (t - h2) / (h3 - h2);
         float grip = Easing.outCubic(Easing.clamp01(u / 0.3F));
         double strain = Math.sin((double)(t * 22.0F)) * 0.02D * (double)grip;
         y = top - 0.1D + 0.07D * (double)grip + strain;
         z = -0.18D + 0.03D * (double)grip;
         bend = 0.1D + 1.25D * (double)grip;

         for(int i = 0; i < 4; ++i) {
            p.curl[i] = 0.12F + 0.7F * grip + 0.05F * (float)Math.sin((double)(t * 18.0F + (float)i));
         }

         p.thumb = 0.5F + 0.4F * grip;
         p.spread = 0.12F;
      } else {
         float u = Easing.inCubic(Easing.clamp01((t - h3) / (h4 - h3)));
         y = top - 0.03D - (top + 0.5D) * (double)u;
         z = -0.15D - 0.22D * (double)u;
         bend = 1.35D - 1.2D * (double)Math.min(1.0F, u * 2.0F);

         for(int i = 0; i < 4; ++i) {
            p.curl[i] = Math.max(0.05F, 0.8F - u * 1.4F + 0.1F * (float)Math.sin((double)(t * 25.0F + (float)i)));
         }

         alpha = a * (1.0F - Easing.smoothstep(0.7F, 1.0F, u));
      }

      p.wristBend = (float)bend;
      HauntKit.Xf t0 = fr.base(new HauntKit.Xf(), s.handX, z, y);
      t0.rotZ((double)(0.08F * (float)Math.sin((double)(t * 2.0F + s.seed))));
      t0.scale(1.75D * sz);
      RenderState.translucent(true);
      int skin = ColorUtil.withAlpha(-9532351, alpha);
      HauntKit.hand(f, t0, p, HauntTextures.ZOMBIE, skin, ColorUtil.withAlpha(-7570098, alpha), fr.light, true, 0.55D);
      Vfx.flush();
   }

   private static final class State {
      GraveyardBase.Frame frame;
      double handX;
      float seed;
      boolean buried;
      boolean stoneUp;
      boolean handOut;
      boolean slipped;

      private State() {
      }
   }
}
