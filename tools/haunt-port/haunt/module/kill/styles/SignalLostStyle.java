package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Glitch;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.look.BodyLook;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import mix.client.mixui.integration.haunt.setting.TextSetting;

public final class SignalLostStyle extends KillStyleBase {
   private static final float OFF = 0.25F;
   private static final float TEXT = 0.45F;
   private static final double BURSTS = 3.0D;
   private static final double RENOISE = 0.07D;
   private static final int MAX_TEXT = 12;
   private static final String[] GLYPHS = new String[]{"ERR", "0x00", "NULL", "404", "SYNC", "▓▒░", "LOST", "0xFF"};
   public final ColorSetting channelB;
   public final ColorSetting glyphColor;
   public final SliderSetting glitchTime;
   public final SliderSetting intensity;
   public final SliderSetting slices;
   public final BooleanSetting pixelSort;
   public final BooleanSetting glyphs;
   public final ModeSetting<SignalLostStyle.FinalText> finalText;
   public final TextSetting custom;
   private final Glitch.Plane plane = new Glitch.Plane();
   private final Glitch.Bursts bursts = new Glitch.Bursts();

   public SignalLostStyle(KillEffectsModule module) {
      super(module, KillStyle.SIGNAL_LOST, new ColorSetting("signallost.color", "Channel A", "One of the split\'s two channels.", ColorSetting.Mode.STATIC, -54314, -37990, 1.0D));
      this.signature(this.color, -54314);
      this.channelB = (ColorSetting)this.setting((new ColorSetting("signallost.channelB", "Channel B", "The split\'s other channel.", ColorSetting.Mode.STATIC, -16718337, -8585269, 1.0D)).secondary());
      this.signature(this.channelB, -16718337);
      this.glyphColor = (ColorSetting)this.setting((new ColorSetting("signallost.glyphs", "Glyphs and Line", "The error glyphs, the switch-off\'s line and dot, and the words.", ColorSetting.Mode.STATIC, -657931, -1, 1.0D)).secondary());
      this.signature(this.glyphColor, -657931);
      this.glitchTime = this.look((SliderSetting)this.setting((new SliderSetting("signallost.glitchTime", "Glitch Time", "How long it glitches before it switches off.", 0.2D, 1.5D, 0.6D, 0.1D)).unit("s")));
      this.intensity = this.look((SliderSetting)this.setting((new SliderSetting("signallost.intensity", "Intensity", "How hard it glitches: the split, the jumps, the blocks.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%")));
      this.slices = (SliderSetting)this.setting(new SliderSetting("signallost.slices", "Slices", "How many bands the body jumps sideways in.", 0.0D, 10.0D, 6.0D, 1.0D));
      this.pixelSort = (BooleanSetting)this.setting(new BooleanSetting("signallost.pixelSort", "Pixel Sort", "Blocks of the body drip downward in streaks.", true));
      this.glyphs = (BooleanSetting)this.setting(new BooleanSetting("signallost.glyphsOn", "Glyphs", "Error glyphs flicker round the body.", true));
      this.finalText = (ModeSetting)this.setting((new ModeSetting("signallost.finalText", "Final Text", "The words left flickering in the air.", SignalLostStyle.FinalText.class, SignalLostStyle.FinalText.NO_SIGNAL)).style(ModeSetting.Style.CHIPS));
      this.custom = (TextSetting)this.setting((new TextSetting("signallost.custom", "Custom Text", "Your own words, up to 12 characters.", "GAME OVER", 12)).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return SignalLostStyle.this.finalText.get() == SignalLostStyle.FinalText.CUSTOM;
         }
      }));
   }

   private float glitch() {
      return this.glitchTime.getFloat();
   }

   public float seconds(KillFx fx) {
      return this.glitch() + 0.25F + (this.finalText.get() == SignalLostStyle.FinalText.NONE?0.05F:0.45F);
   }

   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   public void start(KillFx fx) {
      SignalLostStyle.State s = new SignalLostStyle.State();
      switch((SignalLostStyle.FinalText)this.finalText.get()) {
      case NO_SIGNAL:
         s.text = "NO SIGNAL";
         break;
      case ERROR:
         s.text = "ERROR";
         break;
      case CUSTOM:
         String c = this.custom.get().trim();
         s.text = c.length() > 12?c.substring(0, 12):c;
         break;
      default:
         s.text = "";
      }

      fx.data = s;
   }

   private static void aim(KillFx fx, SignalLostStyle.State s, Camera cam) {
      s.aimed = true;
      double dx = fx.ctx.dirX;
      double dy = fx.ctx.dirY;
      double dz = fx.ctx.dirZ;
      double along;
      double l;
      if((l = Math.sqrt((dx = dx - cam.forwardX * (along = dx * cam.forwardX + dy * cam.forwardY + dz * cam.forwardZ)) * dx + (dy = dy - cam.forwardY * along) * dy + (dz = dz - cam.forwardZ * along) * dz)) < 0.2D) {
         dx = cam.rightX;
         dy = cam.rightY;
         dz = cam.rightZ;
         l = 1.0D;
      }

      s.sx = dx / l;
      s.sy = dy / l;
      s.sz = dz / l;
   }

   private float off(KillFx fx) {
      return fx.phase(this.glitch(), this.glitch() + 0.25F);
   }

   private float envelope(KillFx fx) {
      float k = this.intensity.getFloat() / 100.0F;
      float burst = this.bursts.at((double)fx.t, 3.0D, fx.seed, Glitch.reduceFlashing());
      float in = Easing.clamp01(fx.t / 0.08F);
      return k * in * (0.35F + 0.65F * burst);
   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      SignalLostStyle.State s = (SignalLostStyle.State)fx.data;
      if(s != null) {
         Camera cam = Vfx.camera();
         if(!s.aimed) {
            aim(fx, s, cam);
         }

         if(pass == FxPass.SOLID) {
            this.body(fx, f, s);
         } else if(pass == FxPass.TRANSLUCENT) {
            this.around(fx, f, s, cam);
         } else {
            this.light(fx, f, s, cam);
         }
      }

   }

   private void body(KillFx fx, FxFrame f, SignalLostStyle.State s) {
      float out = this.off(fx);
      boolean calm = Glitch.reduceFlashing();
      float f2 = calm?1.0F:0.55F;
      if(out < f2) {
         BodyProxy.Look look = fx.look;
         BodyProxy.Part[] parts = fx.parts;

         for(int i = 0; i < 6; ++i) {
            parts[i].copy(fx.rest[i]);
         }

         float before = look.alpha;
         if(out > 0.0F) {
            if(calm) {
               look.kind = BodyLook.SKIN;
               look.glowAmount = 0.0F;
               look.alpha = 1.0F - out;
            } else {
               double mid = fx.ctx.feetY + 0.95D * (double)fx.size;

               for(BodyProxy.Part p : parts) {
                  p.stretch = Math.max(0.02F, Glitch.crtHeight(out));
                  p.sx = 0.0D;
                  p.sy = 1.0D;
                  p.sz = 0.0D;
                  p.ox = fx.ctx.feetX;
                  p.oy = mid;
                  p.oz = fx.ctx.feetZ;
               }

               look.kind = BodyLook.SKIN;
               look.glow = this.glyphColor.getColor(0.5F) | -16777216;
               look.glowAmount = Math.min(1.0F, 0.3F + out * 2.0F);
            }

            look.sweepTo = null;
            look.dissolve = -1.0F;
            fx.drawBody(f);
            look.glowAmount = 0.0F;
            look.alpha = before;
            RenderState.translucent(true);
         } else {
            float e = this.envelope(fx);
            double d = (0.03D + 0.12D * (double)e) * (double)fx.size;
            look.kind = BodyLook.GLITCH;
            look.sweepTo = null;
            look.dissolve = -1.0F;
            look.glitchA = this.color.getColor(0.5F) | -16777216;
            look.glitchB = this.channelB.getColor(0.5F) | -16777216;
            look.glitchX = s.sx * d;
            look.glitchY = s.sy * d;
            look.glitchZ = s.sz * d;
            look.slices = this.slices.getInt();
            look.sliceSeed = fx.seed + (int)Math.floor((double)fx.t / 0.07D) * 7919;
            look.sliceShift = (float)((0.04D + 0.16D * (double)e) * (double)fx.size);
            if(fx.crit && fx.t > this.glitch() - 0.15F) {
               double gap = 0.32D * (double)fx.size * (double)Easing.outCubic(fx.phase(this.glitch() - 0.15F, this.glitch() - 0.05F));

               for(int c = -1; c <= 1; ++c) {
                  for(int i = 0; i < 6; ++i) {
                     parts[i].copy(fx.rest[i]);
                     parts[i].x += s.sx * gap * (double)c;
                     parts[i].y += s.sy * gap * (double)c;
                     parts[i].z += s.sz * gap * (double)c;
                  }

                  look.alpha = c == 0?1.0F:0.75F;
                  fx.drawBody(f);
               }

               look.alpha = before;
            } else {
               fx.drawBody(f);
            }

            RenderState.translucent(true);
         }
      }

   }

   private void around(KillFx fx, FxFrame f, SignalLostStyle.State s, Camera cam) {
      float alpha = fx.alpha();
      int ca = this.color.getColor(0.5F);
      int cb = this.channelB.getColor(0.5F);
      int cg = this.glyphColor.getColor(0.5F);
      BodyProxy.Part b = fx.rest[1];
      double sc = (double)fx.size;
      double px = f.rx(b.x) - cam.forwardX * 0.35D;
      double py = f.ry(b.y + 0.05D) - cam.forwardY * 0.35D;
      double pz = f.rz(b.z) - cam.forwardZ * 0.35D;
      this.plane.set(px, py, pz, cam.rightX, cam.rightY, cam.rightZ, cam.upX, cam.upY, cam.upZ);
      float out = this.off(fx);
      if(out <= 0.0F) {
         float e = this.envelope(fx);
         double d = (0.02D + 0.05D * (double)e) * sc;
         float peak = Glitch.reduceFlashing()?0.6F:0.92F;
         Glitch.blocks(this.plane, this.bursts.seed, Easing.clamp01(e * 1.4F), 0.45D * sc, 1.0D * sc, d, alpha, ca, cb, cg, peak);
         Glitch.scanlines(this.plane, (double)fx.t, 1.5D, 0.5D * sc, 1.05D * sc, alpha, e, cg, ca, cb);
         if(this.pixelSort.get()) {
            float k2 = Easing.clamp01(fx.t / 0.25F) * (0.4F + 0.6F * e);
            Glitch.streaks(this.plane, fx.seed + (int)Math.floor((double)fx.t / 0.12D), k2, 7, 0.38D * sc, 0.3D * sc, 1.1D * sc, alpha, ca, cb, cg);
         }

         if(this.glyphs.get()) {
            this.glyphs(fx, sc, d, alpha, ca, cb, cg);
         }
      }

      float k;
      if(!s.text.isEmpty() && (k = fx.phase(this.glitch() + 0.25F, this.glitch() + 0.25F + 0.45F)) > 0.0F && k < 1.0F) {
         boolean on = Glitch.hash((int)Math.floor((double)fx.t / 0.045D), fx.seed) > 0.22F || k > 0.25F && k < 0.75F;
         if(on) {
            double gh = 0.26D * sc;
            double w = Glitch.textWidth(s.text) * gh;
            float a = alpha * (1.0F - Easing.smoothstep(0.75F, 1.0F, k));
            double dx = 0.012D * sc * (1.0D + 2.0D * (double)Glitch.hash((int)Math.floor((double)fx.t / 0.06D), fx.seed + 3));
            this.plane.splitText(Glitch.atlas(), s.text, -w * 0.5D, -gh * 0.3D, gh, dx, Glitch.alphaOf(ca, a * 0.7F), Glitch.alphaOf(cb, a * 0.7F), Glitch.alphaOf(cg, a));
         }
      }

   }

   private void glyphs(KillFx fx, double sc, double d, float alpha, int ca, int cb, int cg) {
      int n = fx.lod?1:3;
      int slot = (int)Math.floor((double)fx.t / 0.15D);

      for(int i = 0; i < n; ++i) {
         float h = Glitch.hash(slot * 31 + i, fx.seed);
         if(h >= 0.3F) {
            String g = GLYPHS[(int)(Glitch.hash(slot + i * 7, fx.seed + 1) * (float)GLYPHS.length) % GLYPHS.length];
            double gh = (0.1D + 0.06D * (double)Glitch.hash(i, slot)) * sc;
            double u = (double)((double)Glitch.hash(slot, i * 3 + fx.seed) < 0.5D?-1:1) * (0.42D + 0.3D * (double)Glitch.hash(i, slot + 9)) * sc;
            double v = ((double)Glitch.hash(slot + 5, i) * 1.8D - 0.8D) * sc;
            this.plane.splitText(Glitch.atlas(), g, u - Glitch.textWidth(g) * gh * 0.5D, v, gh, d, Glitch.alphaOf(ca, alpha * 0.6F), Glitch.alphaOf(cb, alpha * 0.6F), Glitch.alphaOf(cg, alpha * 0.9F));
         }
      }

   }

   private void light(KillFx fx, FxFrame f, SignalLostStyle.State s, Camera cam) {
      float out = this.off(fx);
      if(out > 0.45F && out < 1.0F && !Glitch.reduceFlashing()) {
         int cg = this.glyphColor.getColor(0.5F);
         float alpha = fx.alpha();
         BodyProxy.Part b = fx.rest[1];
         double x = f.rx(fx.ctx.feetX) - cam.forwardX * 0.3D;
         double y = f.ry(fx.ctx.feetY + 0.95D * (double)fx.size) - cam.forwardY * 0.3D;
         double z = f.rz(fx.ctx.feetZ) - cam.forwardZ * 0.3D;
         this.plane.set(x, y, z, cam.rightX, cam.rightY, cam.rightZ, cam.upX, cam.upY, cam.upZ);
         double w = 0.55D * (double)fx.size * (double)Glitch.crtWidth(out);
         double h = 0.012D * (double)fx.size;
         if(w > 0.004D) {
            this.plane.rect(-w, -h, w, h, Glitch.alphaOf(cg, alpha));
            Vfx.sprite(Textures.GLOW, x, y, z, w * 2.2D, 0.0F, alpha(cg, 0.35F * alpha));
         }

         float dot;
         if((dot = Glitch.crtDot(out)) > 0.01F) {
            Vfx.sprite(Textures.GLOW, x, y, z, 0.35D * (double)fx.size, 0.0F, core(cg, dot * alpha));
            Vfx.sprite(Textures.FLARE, x, y, z, 0.5D * (double)fx.size, 0.0F, alpha(cg, 0.6F * dot * alpha));
         }
      }

   }

   public String stateForTest(KillFx fx) {
      SignalLostStyle.State s = (SignalLostStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float out = this.off(fx);
         String phase = out <= 0.0F?"glitching":(out < 0.55F?"down to a line":(out < 1.0F?"a dot":"the words"));
         return String.format(Locale.ROOT, "%s; glitch %.1f s, envelope %.2f, %d slices, pixel sort %s, glyphs %s, words \"%s\"%s%s", new Object[]{phase, Float.valueOf(this.glitch()), Float.valueOf(this.envelope(fx)), Integer.valueOf(this.slices.getInt()), this.pixelSort.get()?"on":"off", this.glyphs.get()?"on":"off", s.text, fx.crit?", crit copies":"", Glitch.reduceFlashing()?", Reduce Flashing":""});
      }
   }

   public static enum FinalText implements Displayable {
      NO_SIGNAL("No signal", "NO SIGNAL"),
      ERROR("Error", "ERROR"),
      CUSTOM("Custom text", "Your own, up to 12 characters"),
      NONE("None", "Nothing");

      private final String displayName;
      private final String description;

      private FinalText(String displayName, String description) {
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
      String text;
      double sx;
      double sy;
      double sz;
      boolean aimed;

      private State() {
      }
   }
}
