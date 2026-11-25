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

public final class CursedTombstoneStyle extends GraveyardBase {
   private static final GraveyardBase.Stone SHAPE = GraveyardBase.Stone.OBELISK;
   private final SliderSetting stay = (SliderSetting)this.setting((new SliderSetting("cursed.stay", "Stay Time", "How long the runes stay lit.", 2.0D, 7.0D, 3.0D, 0.1D)).unit(" s"));
   private final SliderSetting runes = (SliderSetting)this.setting(new SliderSetting("cursed.runes", "Runes", "How many runes ignite on the stone.", 3.0D, 7.0D, 5.0D, 1.0D));
   private final BooleanSetting hand = (BooleanSetting)this.setting(new BooleanSetting("cursed.hand", "Shadow Hand", "A shadow hand slides around the stone for a moment.", true));
   private final ColorSetting stoneColor = (ColorSetting)this.setting((new ColorSetting("cursed.stone", "Stone", "Color of the cursed stone.", -13619136)).secondary());

   public CursedTombstoneStyle(KillEffectsModule module) {
      super(module, KillStyle.CURSED_TOMBSTONE, new ColorSetting("cursed.color", "Runes", "Color of the cursed runes and their light.", -5223169));
   }

   private float stayTime(KillFx fx) {
      return fx.preview?Math.min(1.8F, this.stay.getFloat()):this.stay.getFloat();
   }

   private int count() {
      return Math.max(3, Math.min(7, this.runes.getInt()));
   }

   private float onAt(KillFx fx, int i) {
      float S = this.stayTime(fx);
      float step = Math.min(0.28F, (S - 0.3F) / (float)this.count());
      return 1.15F + step * (float)i;
   }

   private float holdEnd(KillFx fx) {
      return 1.1F + this.stayTime(fx);
   }

   private float offAt(KillFx fx, int i) {
      return this.holdEnd(fx) + 0.17F * (float)i;
   }

   private float sinkAt(KillFx fx) {
      return this.offAt(fx, this.count()) + 0.1F;
   }

   public float seconds(KillFx fx) {
      return this.sinkAt(fx) + 0.8F;
   }

   public void start(KillFx fx) {
      HauntTextures.warmUp();
      CursedTombstoneStyle.State s = new CursedTombstoneStyle.State();
      fx.data = s;
      s.frame = frame(fx);
      s.seed = fx.rng.next() * 30.0F;
      s.side = fx.rng.next() < 0.5F?-1:1;

      for(int i = 0; i < s.glyph.length; ++i) {
         s.glyph[i] = (int)(fx.rng.next() * 8.0F) % 8;
      }

   }

   private float handAt(KillFx fx) {
      return this.onAt(fx, this.count() - 1) + 0.25F;
   }

   public void update(KillFx fx, float dt) {
      CursedTombstoneStyle.State s = (CursedTombstoneStyle.State)fx.data;
      float t = fx.t;
      GraveyardBase.Frame fr = s.frame;
      double sz = (double)fx.size;
      if(!s.burst && t > 0.12F) {
         s.burst = true;
         soilBurst(fx, fr.ox, fr.oz, fx.count(18.0F, false), 0.7F);
         fog(fx, fr.ox, fr.oz, 8, 1.0F, -14540254, 0.45F);
      }

      if(t < this.sinkAt(fx) && fx.rng.next() < dt * 6.0F) {
         fog(fx, fr.ox, fr.oz, 1, 1.2F, ColorUtil.lerp(-14540254, this.color.getColor(0.5F), 0.12F), 0.4F);
      }

      if(!fx.lod && t > 0.15F && t < 1.1F && fx.rng.next() < dt * 25.0F) {
         double a = (double)fx.rng.next() * 6.2832D;
         fx.particles.spawn(8, fr.ox + Math.cos(a) * 0.3D * sz, fx.ctx.groundY + 0.05D, fr.oz + Math.sin(a) * 0.3D * sz, (float)Math.cos(a) * 0.1F, 0.5F + fx.rng.next() * 0.4F, (float)Math.sin(a) * 0.1F, 1.2F, (0.3F + fx.rng.next() * 0.2F) * fx.size).end(0.6F * fx.size).drag(1.0F).color(-15329770).alpha(0.5F * fx.opacity);
      }

      int n = this.count();
      double[] face = faceOf(SHAPE, sz);

      for(int i = 0; i < n; ++i) {
         if((s.lit & 1 << i) == 0 && t > this.onAt(fx, i)) {
            s.lit |= 1 << i;
            double ry = this.runeY(i, n, sz);
            double rx = fr.wx(0.0D, face[1] + 0.03D);
            double rz = fr.wz(0.0D, face[1] + 0.03D);
            this.sparks(fx, rx, fx.ctx.groundY + ry, rz, fr, fx.count(7.0F, false));
         }

         if((s.out & 1 << i) == 0 && t > this.offAt(fx, i)) {
            s.out |= 1 << i;
            double ry = this.runeY(i, n, sz);
            if(!fx.lod) {
               fx.particles.spawn(8, fr.wx(0.0D, face[1] + 0.05D), fx.ctx.groundY + ry, fr.wz(0.0D, face[1] + 0.05D), 0.0F, 0.25F, 0.0F, 0.9F, 0.16F * fx.size).end(0.4F * fx.size).drag(1.0F).color(-14540254).alpha(0.5F * fx.opacity);
            }
         }
      }

      if(!s.sunk && t > this.sinkAt(fx)) {
         s.sunk = true;
         soilBurst(fx, fr.ox, fr.oz, fx.count(12.0F, false), 0.5F);
         fog(fx, fr.ox, fr.oz, 6, 0.8F, -14540254, 0.4F);
      }

   }

   private void sparks(KillFx fx, double x, double y, double z, GraveyardBase.Frame fr, int n) {
      if(!fx.lod) {
         int c = this.color.getColor(0.5F);

         for(int i = 0; i < n; ++i) {
            float sp = 0.4F + fx.rng.next() * 0.9F;
            fx.particles.spawn(1, x, y, z, (float)fr.fx * sp + fx.rng.signed() * 0.5F, fx.rng.signed() * 0.6F + 0.3F, (float)fr.fz * sp + fx.rng.signed() * 0.5F, 0.45F + fx.rng.next() * 0.3F, (0.04F + fx.rng.next() * 0.03F) * fx.size).drag(2.5F).color(c).alpha(fx.opacity).flags(1);
         }
      }

   }

   private double runeY(int i, int n, double sz) {
      double top = 1.38D * sz;
      double bottom = 0.38D * sz;
      return n <= 1?(top + bottom) * 0.5D:top - (top - bottom) * (double)i / (double)(n - 1);
   }

   public void render(KillFx fx, FxFrame f) {
      CursedTombstoneStyle.State s = (CursedTombstoneStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      GraveyardBase.Frame fr = s.frame;
      bury(fx, f, t, 0.0F, 0.8F);
      if(HauntTextures.ensure()) {
         double sz = (double)fx.size;
         double H = stoneHeight(SHAPE, sz);
         float riseU = Easing.clamp01((t - 0.12F) / 1.0F);
         float rise = Easing.outCubic(riseU);
         float sink = Easing.inCubic(Easing.clamp01((t - this.sinkAt(fx)) / 0.8F));
         double shudder = riseU < 1.0F?Math.sin((double)(t * 47.0F)) * 0.012D * (double)(1.0F - riseU):0.0D;
         double lift = ((double)rise - 1.0D) * (H + 0.15D) - (double)sink * (H + 0.15D);
         HauntKit.Xf st = fr.base(new HauntKit.Xf(), shudder, 0.0D, lift);
         int n = this.count();
         float litSum = 0.0F;

         for(int i = 0; i < n; ++i) {
            litSum += this.runeOn(fx, t, i);
         }

         float charge = litSum / (float)n;
         float mark = Easing.smoothstep(0.1F, 0.7F, t) * (1.0F - Easing.smoothstep(this.sinkAt(fx), this.seconds(fx), t));
         groundMark(f, fx, fr.ox, fr.oz, 0.9D * sz, (double)s.seed, mark * a);
         int glow = this.color.getColor(0.5F) | -16777216;
         int stoneC = ColorUtil.lerp(this.stoneColor.getColor(0.5F) | -16777216, HauntKit.darker(glow, 0.35F), 0.25F * charge);
         HauntKit.clipBelow(fx.ctx.groundY);

         try {
            stone(f, st, SHAPE, sz, stoneC, fr.light, a);
            if(this.hand.get()) {
               this.drawShadowHand(fx, f, s, t, a, lift);
            }
         } finally {
            HauntKit.noClip();
         }

         double[] face = faceOf(SHAPE, sz);
         RenderState.additive();

         for(int i = 0; i < n; ++i) {
            float on = this.runeOn(fx, t, i) * (1.0F - sink);
            if(on > 0.01F) {
               double y = this.runeY(i, n, sz);
               float flick = 0.85F + 0.15F * (float)Math.sin((double)(t * 13.0F + (float)i * 2.1F + s.seed));
               rune(f, st, s.glyph[i], 0.0D, y, face[1] + 0.004D, 0.2D * sz, alpha(glow, a * on * flick));
               double gx = st.px(0.0D, y, face[1] + 0.04D);
               double gy = st.py(0.0D, y, face[1] + 0.04D);
               double gz = st.pz(0.0D, y, face[1] + 0.04D);
               Vfx.sprite(Textures.GLOW, f.rx(gx), f.ry(gy), f.rz(gz), 0.55D * sz, 0.0F, alpha(glow, 0.45F * a * on * flick * Math.max(0.4F, fx.glow)));
               float flash = 1.0F - Easing.clamp01((t - this.onAt(fx, i)) / 0.25F);
               if(flash > 0.0F && t >= this.onAt(fx, i)) {
                  Vfx.sprite(Textures.FLARE, f.rx(gx), f.ry(gy), f.rz(gz), 0.9D * sz * (double)(0.6F + flash), t * 2.0F, alpha(glow, 0.6F * a * flash));
               }
            }
         }

         float aura = charge * (1.0F - sink);
         if(aura > 0.01F) {
            Vfx.sprite(Textures.GLOW, f.rx(fr.ox), f.ry(fr.oy + H * 0.55D + lift), f.rz(fr.oz), 3.0D * sz, 0.0F, alpha(glow, 0.12F * a * aura));
            Vfx.flatSprite(Textures.GROUND_GLOW, f.rx(fr.ox), f.ry(fx.ctx.groundY + 0.02D), f.rz(fr.oz), 1.4D * sz, (double)(t * 0.3F), alpha(glow, 0.22F * a * aura));
         }

         Vfx.flush();
      }

   }

   private float runeOn(KillFx fx, float t, int i) {
      return Easing.smoothstep(this.onAt(fx, i), this.onAt(fx, i) + 0.18F, t) * (1.0F - Easing.smoothstep(this.offAt(fx, i), this.offAt(fx, i) + 0.2F, t));
   }

   private void drawShadowHand(KillFx fx, FxFrame f, CursedTombstoneStyle.State s, float t, float a, double lift) {
      float t0 = this.handAt(fx);
      float reach = 0.4F;
      float grip = 0.35F;
      float back = 0.22F;
      if(t >= t0 && t <= t0 + reach + grip + back) {
         GraveyardBase.Frame fr = s.frame;
         double sz = (double)fx.size;
         HauntKit.HandPose p = new HauntKit.HandPose();
         double z;
         float alpha;
         if(t < t0 + reach) {
            float u = Easing.outCubic((t - t0) / reach);
            z = -0.5D + 0.48D * (double)u;
            alpha = Easing.clamp01(u * 2.5F);

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.05F + 0.1F * (float)i * 0.25F;
            }

            p.spread = 0.35F;
         } else if(t < t0 + reach + grip) {
            float u = (t - t0 - reach) / grip;
            float g = Easing.outCubic(Easing.clamp01(u / 0.45F));
            z = -0.02D + 0.015D * Math.sin((double)(t * 30.0F)) * (double)g;
            alpha = 1.0F;

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.1F + 0.8F * g;
            }

            p.thumb = 0.2F + 0.4F * g;
            p.spread = 0.2F;
         } else {
            float u = Easing.inCubic((t - t0 - reach - grip) / back);
            z = -0.02D - 0.55D * (double)u;
            alpha = 1.0F - u;

            for(int i = 0; i < 4; ++i) {
               p.curl[i] = 0.9F - 0.6F * u;
            }

            p.spread = 0.2F + 0.2F * u;
         }

         double x = (double)s.side * (0.2D * sz + 0.035D * sz);
         double y = 1.02D * sz + lift;
         HauntKit.Xf base = fr.base(new HauntKit.Xf(), x, z * sz, y);
         base.rotX(1.5707963267948966D).rotY((double)(-s.side) * 1.5707963267948966D).rotZ(0.12D * (double)s.side);
         base.scale(1.3D * sz);
         int shadow = ColorUtil.withAlpha(-15594984, 0.92F * alpha * a);
         RenderState.translucent(true);
         HauntKit.hand(f, base, p, Textures.WHITE, shadow, shadow, 1.0F, true, 0.7D);
         Vfx.flush();
         RenderState.translucent(false);

         for(int i = 0; i < 4; ++i) {
            double back0 = -0.15D - 0.18D * (double)i;
            double wx = base.px(0.0D, back0, 0.0D);
            double wy = base.py(0.0D, back0, 0.0D);
            double wz = base.pz(0.0D, back0, 0.0D);
            Vfx.sprite(Textures.SMOKE, f.rx(wx), f.ry(wy), f.rz(wz), (0.22D + 0.08D * (double)i) * sz, t * 0.8F + (float)i, alpha(-15594984, 0.4F * alpha * a));
         }

         Vfx.flush();
      }

   }

   private static final class State {
      GraveyardBase.Frame frame;
      float seed;
      int side;
      int lit;
      int out;
      boolean burst;
      boolean sunk;
      final int[] glyph;

      private State() {
         this.glyph = new int[7];
      }
   }
}
