package mix.client.mixui.integration.haunt.module.kill.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.haunt.fx.HauntKit;
import mix.client.mixui.integration.haunt.haunt.fx.HauntTextures;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.styles.GraveyardBase;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class RestInPeaceStyle extends GraveyardBase {
   private final SliderSetting stay = (SliderSetting)this.setting((new SliderSetting("rip.stay", "Stay Time", "How long the scene stays before it fades.", 1.5D, 6.0D, 2.6D, 0.1D)).unit(" s"));
   private final BooleanSetting shadow = (BooleanSetting)this.setting(new BooleanSetting("rip.shadow", "Shadow", "A tall shadow looms behind the stone.", true));
   private final ColorSetting stoneColor = (ColorSetting)this.setting((new ColorSetting("rip.stone", "Stone", "Color of the headstone.", -4605511)).secondary());

   public RestInPeaceStyle(KillEffectsModule module) {
      super(module, KillStyle.REST_IN_PEACE, new ColorSetting("rip.color", "Ghost Light", "Color of the pulsing light and the eyes.", -6990132));
   }

   public float seconds(KillFx fx) {
      return 1.0F + (fx.preview?Math.min(1.6F, this.stay.getFloat()):this.stay.getFloat()) + 0.9F;
   }

   public void start(KillFx fx) {
      HauntTextures.warmUp();
      RestInPeaceStyle.State s = new RestInPeaceStyle.State();
      fx.data = s;
      s.frame = frame(fx);
      s.seed = fx.rng.next() * 40.0F;
   }

   public void update(KillFx fx, float dt) {
      RestInPeaceStyle.State s = (RestInPeaceStyle.State)fx.data;
      float t = fx.t;
      GraveyardBase.Frame fr = s.frame;
      if(!s.burst && t > 0.2F) {
         s.burst = true;
         soilBurst(fx, fr.ox, fr.oz, fx.count(26.0F, false), 1.1F);
         fog(fx, fr.ox, fr.oz, 8, 1.1F, -11447725, 0.32F);
      }

      float end = this.seconds(fx) - 0.9F;
      if(this.shadow.get() && !fx.lod && t > 0.8F && t < end && fx.rng.next() < dt * 16.0F) {
         double lx = (double)(fx.rng.signed() * 0.35F);
         fx.particles.spawn(8, fr.wx(lx, -0.5D), fx.ctx.groundY + 0.3D + (double)(fx.rng.next() * 1.8F), fr.wz(lx, -0.5D), fx.rng.signed() * 0.1F, 0.12F + fx.rng.next() * 0.15F, fx.rng.signed() * 0.1F, 1.4F + fx.rng.next() * 0.6F, (0.35F + fx.rng.next() * 0.3F) * fx.size).end(0.7F * fx.size).drag(0.8F).color(-15461340).alpha(0.55F * fx.opacity);
      }

      if(!s.faded && t > end) {
         s.faded = true;
         fog(fx, fr.ox, fr.oz, 10, 1.3F, -11447725, 0.3F);
         leaves(fx, fr.ox, fx.ctx.groundY + 1.6D, fr.oz, fx.count(8.0F, false));
      }

   }

   public void render(KillFx fx, FxFrame f) {
      RestInPeaceStyle.State s = (RestInPeaceStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      GraveyardBase.Frame fr = s.frame;
      bury(fx, f, t, 0.0F, 0.9F);
      if(HauntTextures.ensure()) {
         double sz = (double)fx.size;
         double H = stoneHeight(GraveyardBase.Stone.ROUNDED, sz);
         float end = this.seconds(fx) - 0.9F;
         float rise = Easing.outBack(Easing.clamp01((t - 0.2F) / 0.8F), 1.5F);
         float sink = Easing.inCubic(Easing.clamp01((t - end) / 0.9F));
         double lift = ((double)Math.max(0.0F, rise) - 1.0D) * (H + 0.2D) - (double)sink * (H + 0.2D);
         float tilt = (1.0F - Easing.clamp01((t - 0.2F) / 0.8F)) * 0.25F * (float)Math.sin((double)(t * 18.0F));
         HauntKit.Xf st = fr.base(new HauntKit.Xf(), 0.0D, 0.0D, lift).rotZ((double)tilt);
         float mark = Easing.smoothstep(0.15F, 0.6F, t) * (1.0F - Easing.smoothstep(end, end + 0.9F, t));
         groundMark(f, fx, fr.ox, fr.oz, 0.75D * sz, (double)s.seed, mark * a);
         HauntKit.clipBelow(fx.ctx.groundY);

         try {
            stone(f, st, GraveyardBase.Stone.ROUNDED, sz, this.stoneColor.getColor(0.5F) | -16777216, fr.light, a);
         } finally {
            HauntKit.noClip();
         }

         int ghost = this.color.getColor(0.5F) | -16777216;
         float pulse = 0.55F + 0.45F * (float)Math.sin((double)(t * 4.2F));
         float lit = Easing.smoothstep(0.7F, 1.3F, t) * (1.0F - sink);
         RenderState.additive();
         double[] face = faceOf(GraveyardBase.Stone.ROUNDED, sz);
         HauntKit.Xf glowT = (new HauntKit.Xf()).set(st);
         Vfx.sprite(Textures.GLOW, f.rx(glowT.px(0.0D, face[0], face[1] + 0.02D)), f.ry(glowT.py(0.0D, face[0], face[1] + 0.02D)), f.rz(glowT.pz(0.0D, face[0], face[1] + 0.02D)), 1.6D * sz, 0.0F, alpha(ghost, 0.35F * a * lit * pulse));
         Vfx.sprite(Textures.GLOW, f.rx(fr.ox), f.ry(fr.oy + H * 0.5D + lift), f.rz(fr.oz), 2.8D * sz, 0.0F, alpha(ghost, 0.12F * a * lit));
         Vfx.flatSprite(Textures.GROUND_GLOW, f.rx(fr.ox), f.ry(fx.ctx.groundY + 0.02D), f.rz(fr.oz), 1.3D * sz, (double)(t * 0.4F), alpha(ghost, 0.25F * a * lit * pulse));
         Vfx.flush();
         if(this.shadow.get()) {
            this.drawShadow(fx, f, s, t, a, end, H);
         }

         RenderState.additive();
         this.engraveGlow(fx, f, st, t, a * lit, ghost);
         Vfx.flush();
      }

   }

   private void drawShadow(KillFx fx, FxFrame f, RestInPeaceStyle.State s, float t, float a, float end, double H) {
      GraveyardBase.Frame fr = s.frame;
      double sz = (double)fx.size;
      float on = Easing.smoothstep(0.8F, 1.6F, t) * (1.0F - Easing.smoothstep(end - 0.2F, end + 0.6F, t));
      if(on > 0.01F) {
         double bx = fr.wx(0.0D, -0.55D);
         double bz = fr.wz(0.0D, -0.55D);
         RenderState.translucent(false);

         for(int i = 0; i < 7; ++i) {
            double y = fr.oy + 0.35D * sz + (double)i * 0.3D * sz;
            double sway = Math.sin((double)(t * 1.4F + (float)i * 0.8F + s.seed)) * 0.06D * sz;
            double w = (0.62D - Math.abs((double)i - 4.5D) * 0.06D) * sz;
            Vfx.sprite(Textures.SMOKE, f.rx(bx + sway * fr.rx), f.ry(y), f.rz(bz + sway * fr.rz), w * 1.6D, t * 0.2F + (float)i, alpha(-15527660, 0.42F * a * on));
         }

         Vfx.sprite(Textures.SMOKE, f.rx(bx), f.ry(fr.oy + 2.25D * sz), f.rz(bz), 0.9D * sz, t * 0.3F, alpha(-15527660, 0.5F * a * on));
         Vfx.flush();
         float eyes = Easing.smoothstep(1.5F, 1.8F, t) * (1.0F - Easing.smoothstep(end - 0.6F, end - 0.2F, t));
         float blink = (t * 0.7F + s.seed) % 3.0F > 2.85F?0.1F:1.0F;
         if(eyes > 0.01F) {
            RenderState.additive();
            int glow = this.color.getColor(0.5F) | -16777216;
            double ey = fr.oy + 2.2D * sz;

            for(int side = -1; side <= 1; side += 2) {
               double ex = fr.wx((double)side * 0.13D * sz, -0.5D);
               double ez = fr.wz((double)side * 0.13D * sz, -0.5D);
               Vfx.sprite(HauntTextures.EYE, f.rx(ex), f.ry(ey), f.rz(ez), 0.22D * sz, 0.0F, alpha(glow, a * eyes * blink));
               Vfx.sprite(Textures.GLOW, f.rx(ex), f.ry(ey), f.rz(ez), 0.6D * sz, 0.0F, alpha(glow, 0.5F * a * eyes * blink));
            }

            Vfx.flush();
         }
      }

   }

   private void engraveGlow(KillFx fx, FxFrame f, HauntKit.Xf st, float t, float a, int color) {
      if(a > 0.01F) {
         double sz = (double)fx.size;
         double[] face = faceOf(GraveyardBase.Stone.ROUNDED, sz);

         for(int i = 0; i < 3; ++i) {
            float on = Easing.smoothstep(0.9F + (float)i * 0.2F, 1.2F + (float)i * 0.2F, t);
            rune(f, st, (i * 3 + 1) % 8, ((double)i - 1.0D) * 0.2D * sz, face[0] + 0.12D * sz, face[1] + 0.006D, 0.16D * sz, alpha(color, 0.85F * a * on));
         }
      }

   }

   private static final class State {
      GraveyardBase.Frame frame;
      float seed;
      boolean burst;
      boolean faded;

      private State() {
      }
   }
}
