package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Comic;
import mix.client.mixui.integration.haunt.fx.kit.Halftone;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.PlayerModel;
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
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class KnockoutStyle extends KillStyleBase {
   private static final float SWEPT = 0.2F;
   private static final float SLAMMED = 0.35F;
   private static final float SHAKEN = 0.65F;
   private static final float LAUNCHED = 1.45F;
   private static final float TWINKLED = 1.65F;
   private static final float END = 1.8F;
   private static final float FLOPPED = 1.0F;
   private static final double AWAY = 6.5D;
   private static final double UP = 8.5D;
   private static final double GRAVITY = 5.0D;
   private static final double TURNS = 3.0D;
   private static final double SMALLEST = 0.08D;
   private static final double BURST_W = 1.25D;
   private static final double BURST_H = 1.05D;
   private static final double BESIDE = 0.95D;
   private static final int MAX_WORD = 8;
   private static final int RING_STARS = 6;
   private static final int DIZZY_STARS = 4;
   public final ColorSetting lettering;
   public final ColorSetting outline;
   public final ModeSetting<KnockoutStyle.Word> word;
   public final TextSetting custom;
   public final BooleanSetting launch;
   public final BooleanSetting halftone;
   public final SliderSetting outlineWidth;
   public final BooleanSetting twinkle;
   private static final double[] ROT = new double[9];
   private final double[] at = new double[3];

   public KnockoutStyle(KillEffectsModule module) {
      super(module, KillStyle.KNOCKOUT, new ColorSetting("knockout.color", "Burst", "The starburst\'s fill (its dots a darker shade).", ColorSetting.Mode.STATIC, -11713, -30147, 1.0D));
      this.signature(this.color, -11713);
      this.lettering = (ColorSetting)this.setting((new ColorSetting("knockout.lettering", "Lettering", "The word\'s letters.", ColorSetting.Mode.STATIC, -1689274, -1, 1.0D)).secondary());
      this.signature(this.lettering, -1689274);
      this.outline = (ColorSetting)this.setting((new ColorSetting("knockout.outline", "Outline", "The body\'s and the burst\'s outlines, and the word\'s shadow.", ColorSetting.Mode.STATIC, -15658735, -14017974, 1.0D)).secondary());
      this.signature(this.outline, -15658735);
      this.word = (ModeSetting)this.setting((new ModeSetting("knockout.word", "Word", "What the burst says: K.O.! or your own word.", KnockoutStyle.Word.class, KnockoutStyle.Word.KO)).style(ModeSetting.Style.CHIPS));
      this.custom = (TextSetting)this.setting((new TextSetting("knockout.custom", "Custom Word", "Your own word, up to 8 characters.", "BOOM!", 8)).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return KnockoutStyle.this.word.get() == KnockoutStyle.Word.CUSTOM;
         }
      }));
      this.launch = (BooleanSetting)this.setting(new BooleanSetting("knockout.launch", "Launch", "The body is launched into the sky (off: it flops flat, seeing stars, and stays like any body).", true));
      this.halftone = (BooleanSetting)this.setting(new BooleanSetting("knockout.halftone", "Halftone", "Printed halftone dots in the burst\'s shade.", true));
      this.outlineWidth = this.look((SliderSetting)this.setting((new SliderSetting("knockout.outlineWidth", "Outline Width", "How bold the body\'s outlines are.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%")));
      this.twinkle = (BooleanSetting)this.setting(new BooleanSetting("knockout.twinkle", "Twinkle", "The tiny star twinkles once before it\'s gone.", true));
   }

   public float seconds(KillFx fx) {
      return 1.8F;
   }

   public boolean leavesBody() {
      return !this.launch.get();
   }

   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   public void start(KillFx fx) {
      KnockoutStyle.State s = new KnockoutStyle.State();
      s.launch = this.launch.get();
      String text = this.word.get() == KnockoutStyle.Word.CUSTOM?this.custom.get().trim():"K.O.!";
      if(text.isEmpty()) {
         text = "K.O.!";
      }

      if(text.length() > 8) {
         text = text.substring(0, 8);
      }

      s.text = fx.crit?text + "!":text;
      Comic.word(s.text);
      double ax = fx.ctx.dirX;
      double az = fx.ctx.dirZ;
      double l = Math.sqrt(ax * ax + az * az);
      if(l < 1.0E-4D) {
         ax = 0.0D;
         az = 1.0D;
         l = 1.0D;
      }

      s.awayX = ax / l;
      s.awayZ = az / l;
      double sx = -s.awayZ;
      double sz = s.awayX;
      double tilt = (hash(fx.seed, 1) - 0.5D) * 0.6D;
      s.spinX = sx + s.awayX * tilt;
      s.spinY = 0.25D * (hash(fx.seed, 2) - 0.5D);
      s.spinZ = sz + s.awayZ * tilt;
      double sl = Math.sqrt(s.spinX * s.spinX + s.spinY * s.spinY + s.spinZ * s.spinZ);
      s.spinX /= sl;
      s.spinY /= sl;
      s.spinZ /= sl;
      s.side = hash(fx.seed, 3) < 0.5D?-1:1;
      BodyProxy.Part b = fx.rest[1];
      s.pivotX = b.x;
      s.pivotY = b.y;
      s.pivotZ = b.z;
      s.ceiling = !fx.preview && ceilingAbove(Minecraft.getMinecraft().theWorld, fx.ctx.feetX, fx.ctx.feetY, fx.ctx.feetZ);
      fx.data = s;
   }

   private static boolean ceilingAbove(World w, double x, double y, double z) {
      if(w == null) {
         return false;
      } else {
         for(int dy = 2; dy <= 5; ++dy) {
            for(int dx = -1; dx <= 1; ++dx) {
               for(int dz = -1; dz <= 1; ++dz) {
                  BlockPos p = new BlockPos(Math.floor(x + (double)dx * 0.3D), Math.floor(y) + (double)dy, Math.floor(z + (double)dz * 0.3D));
                  if(w.getBlockState(p).getBlock().getMaterial().blocksMovement()) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private static void rotateAbout(BodyProxy.Part q, double px, double py, double pz, double kx, double ky, double kz, double a) {
      double c = Math.cos(a);
      double s = Math.sin(a);
      double t = 1.0D - c;
      ROT[0] = c + kx * kx * t;
      ROT[1] = kx * ky * t - kz * s;
      ROT[2] = kx * kz * t + ky * s;
      ROT[3] = ky * kx * t + kz * s;
      ROT[4] = c + ky * ky * t;
      ROT[5] = ky * kz * t - kx * s;
      ROT[6] = kz * kx * t - ky * s;
      ROT[7] = kz * ky * t + kx * s;
      ROT[8] = c + kz * kz * t;
      double ox = q.x - px;
      double oy = q.y - py;
      double oz = q.z - pz;
      q.x = px + ROT[0] * ox + ROT[1] * oy + ROT[2] * oz;
      q.y = py + ROT[3] * ox + ROT[4] * oy + ROT[5] * oz;
      q.z = pz + ROT[6] * ox + ROT[7] * oy + ROT[8] * oz;
      PlayerModel.mul9(ROT, q.r, q.r);
   }

   private static double flight(KillFx fx) {
      return Math.max(0.0D, (double)(Math.min(fx.t, 1.45F) - 0.65F));
   }

   private double flightAt(KillFx fx, KnockoutStyle.State s, double tau, double[] out) {
      float k = (float)(tau / 0.8000000715255737D);
      double scale = 1.0D - 0.92D * (double)Easing.inCubic(k) - 0.0D;
      if(s.ceiling) {
         out[0] = s.pivotX;
         out[1] = s.pivotY + 0.35D * Math.sin(3.141592653589793D * Math.min(1.0D, (double)k * 1.6D));
         out[2] = s.pivotZ;
         return Math.max(0.02D, 1.0D - 0.98D * (double)Easing.inCubic(k));
      } else {
         double size = (double)fx.size;
         out[0] = s.pivotX + s.awayX * 6.5D * tau * size;
         out[1] = s.pivotY + (8.5D * tau - 2.5D * tau * tau) * size;
         out[2] = s.pivotZ + s.awayZ * 6.5D * tau * size;
         return scale;
      }
   }

   private void body(KillFx fx, FxFrame f, KnockoutStyle.State s) {
      float t = fx.t;
      if(!s.launch || t < 1.45F) {
         BodyProxy.Look look = fx.look;
         look.outlineColor = this.outline.getColor(0.5F) | -16777216;
         look.outlineWidth = 3.0F * this.outlineWidth.getFloat() / 100.0F;
         look.worldLight = fx.light;
         if(t < 0.2F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = BodyLook.TOON;
            look.sweep = Easing.inOutCubic(t / 0.2F);
            look.sweepMode = 2;
            look.sweepOX = fx.ctx.impactX;
            look.sweepOY = fx.ctx.impactY;
            look.sweepOZ = fx.ctx.impactZ;
            look.sweepSpan = 1.4F;
            look.sweepEdgeColor = -1;
         } else {
            look.kind = BodyLook.TOON;
            look.sweepTo = null;
         }

         look.dissolve = -1.0F;
         BodyProxy.Part[] parts = fx.parts;

         for(int i = 0; i < 6; ++i) {
            parts[i].copy(fx.rest[i]);
         }

         if(s.launch) {
            double tau = flight(fx);
            if(tau > 0.0D) {
               double scale = this.flightAt(fx, s, tau, this.at);
               double turn = 18.84955592153876D * (double)Easing.inQuad((float)(tau / 0.8000000715255737D)) * (fx.crit?1.3D:1.0D);

               for(int i = 0; i < 6; ++i) {
                  BodyProxy.Part p = parts[i];
                  if(s.ceiling) {
                     rotateAbout(p, s.pivotX, s.pivotY, s.pivotZ, 0.0D, 1.0D, 0.0D, turn);
                  } else {
                     rotateAbout(p, s.pivotX, s.pivotY, s.pivotZ, s.spinX, s.spinY, s.spinZ, turn);
                  }

                  p.x = this.at[0] + (p.x - s.pivotX) * scale;
                  p.y = this.at[1] + (p.y - s.pivotY) * scale;
                  p.z = this.at[2] + (p.z - s.pivotZ) * scale;
                  p.scale = (float)scale;
               }
            }
         } else {
            float k = fx.phase(0.65F, 1.0F);
            if(k > 0.0F) {
               double fall = 1.5707963267948966D * bounce(k);
               double ax = -s.awayZ;
               double az = s.awayX;

               for(int i = 0; i < 6; ++i) {
                  rotateAbout(parts[i], fx.ctx.feetX, fx.ctx.feetY + 0.12D, fx.ctx.feetZ, ax, 0.0D, az, -fall);
               }
            }
         }

         fx.drawBody(f);
         RenderState.translucent(true);
      }

   }

   private static double bounce(float k) {
      if(k < 0.7F) {
         float q = k / 0.7F;
         return (double)(q * q);
      } else {
         float q = (k - 0.7F) / 0.3F;
         return 1.0D - 0.08D * Math.sin(3.141592653589793D * (double)q);
      }
   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      KnockoutStyle.State s = (KnockoutStyle.State)fx.data;
      if(s != null) {
         if(pass == FxPass.SOLID) {
            this.body(fx, f, s);
         } else if(pass == FxPass.TRANSLUCENT) {
            Camera cam = Vfx.camera();
            this.burst(fx, f, s, cam);
            this.stars(fx, f, s, cam);
         } else if(pass == FxPass.GLOW) {
            this.glow(fx, f, s);
         }
      }

   }

   private void burst(KillFx fx, FxFrame f, KnockoutStyle.State s, Camera cam) {
      float t = fx.t;
      float alpha = fx.alpha();
      float in = fx.phase(0.2F, 0.35F);
      float away = fx.phase(0.9F, 1.0999999F);
      if(in > 0.0F && away < 1.0F) {
         double rx = cam.rightX;
         double ry = cam.rightY;
         double rz = cam.rightZ;
         double ux = cam.upX;
         double uy = cam.upY;
         double uz = cam.upZ;
         double sc = (double)fx.size * (fx.crit?1.15D:1.0D);
         BodyProxy.Part b = fx.rest[1];
         double side = 0.95D * sc * (double)s.side;
         double x = f.rx(b.x) + rx * side;
         double y = f.ry(b.y + 0.45D) + ry * side;
         double z = f.rz(b.z) + rz * side;
         double by;
         double bx;
         if(t < 0.35F) {
            double e = (double)Easing.outBack(in, 2.4F);
            bx = e * (1.0D - 0.25D * Math.sin(3.141592653589793D * (double)in));
            by = e * (1.0D + 0.3D * Math.sin(3.141592653589793D * (double)in));
         } else if(away <= 0.0F) {
            double wob = 0.05D * Math.exp((double)(-(t - 0.35F)) * 9.0D) * Math.sin((double)(t - 0.35F) * 70.0D);
            bx = 1.0D + wob;
            by = 1.0D - wob;
         } else if(away < 0.45F) {
            double q = (double)Easing.outCubic(away / 0.45F);
            bx = 1.0D + 0.35D * q;
            by = 1.0D - 0.55D * q;
         } else {
            double q = (double)Easing.inCubic((away - 0.45F) / 0.55F);
            bx = 1.35D * (1.0D + 0.3D * q) * (1.0D - q);
            by = 0.45D * (1.0D - q);
         }

         double w = 1.25D * sc * bx;
         double h = 1.05D * sc * by;
         if(w > 0.001D && h > 0.001D) {
            int fill = this.color.getColor(0.5F);
            int line = this.outline.getColor(0.5F);
            int lineCol = ColorUtil.mulAlpha(line, alpha);
            int variant = (int)(hash(fx.seed, 7) * 4.0D) % 4;
            double turn = (double)s.side * Math.toRadians(-7.0D);
            double shake = t >= 0.35F && t < 0.65F?0.05D * sc * Math.exp((double)(-(t - 0.35F)) * 7.0D):0.0D;
            double sx = shake * Math.sin((double)t * 210.0D + (double)fx.seed);
            double sy = shake * Math.cos((double)t * 170.0D + (double)fx.seed * 0.7D);
            float lines = alpha * Easing.clamp01(in * 2.0F) * (1.0F - away);
            double var65;
            double var66;
            double var67;
            Comic.actionLines(var65 = x + rx * sx + ux * sy, var66 = y + ry * sx + uy * sy, var67 = z + rz * sx + uz * sy, rx, ry, rz, ux, uy, uz, 0.62D * sc, 1.0D * sc, 14, fx.seed, 0.02D * sc, ColorUtil.mulAlpha(line, lines));
            if(fx.crit) {
               Comic.balloon(0, variant, var65, var66, var67, rx, ry, rz, ux, uy, uz, w * 1.14D, h * 1.14D, turn, ColorUtil.mulAlpha(-1, alpha), 0, false, lineCol, 1.6F);
            }

            Comic.balloon(0, variant, var65, var66, var67, rx, ry, rz, ux, uy, uz, w, h, turn, ColorUtil.mulAlpha(fill, alpha * 0.92F), ColorUtil.mulAlpha(ColorUtil.shade(fill, 0.7F), alpha), this.halftone.get(), lineCol, 1.4F);
            Comic.Word wd = Comic.word(s.text);
            float slam = fx.phase(0.25F, 0.39F);
            if(slam > 0.0F) {
               double grow = 1.0D + 1.1D * (1.0D - (double)Easing.outCubic(slam));
               double wordH = 0.32D * sc * grow * Math.min(bx, 1.2D) * Math.min(by * 1.2D, 1.0D);
               double tilt = (double)s.side * Math.toRadians(-10.0D) + (t < 0.65F?Math.toRadians(4.0D) * Math.sin((double)t * 80.0D) * Math.exp((double)(-(t - 0.35F)) * 6.0D):0.0D);
               float wa = alpha * Easing.clamp01(slam * 3.0F) * (1.0F - away);
               Comic.lettering(wd, var65, var66, var67, rx, ry, rz, ux, uy, uz, wordH, tilt, ColorUtil.mulAlpha(this.lettering.getColor(0.5F), wa), ColorUtil.mulAlpha(line, wa), fx.crit?1.6F:1.0F);
            }
         }
      }

   }

   private void stars(KillFx fx, FxFrame f, KnockoutStyle.State s, Camera cam) {
      float t = fx.t;
      float alpha = fx.alpha();
      int fill = this.color.getColor(0.5F);
      int line = this.outline.getColor(0.5F);
      float k;
      if(fx.crit && s.launch && (k = fx.phase(0.2F, 0.35F) * (1.0F - fx.phase(0.65F, 0.77F))) > 0.01F) {
         BodyProxy.Part b = fx.rest[1];

         for(int i = 0; i < 6; ++i) {
            double a = 6.283185307179586D * (double)i / 6.0D + (double)t * 5.0D;
            double r = 0.75D * (double)fx.size;
            star(f.rx(b.x + Math.cos(a) * r), f.ry(b.y + 0.15D + 0.1D * Math.sin(a * 2.0D)), f.rz(b.z + Math.sin(a) * r), 0.16D * (double)fx.size, (float)(a * 2.0D), fill, line, alpha * k);
         }
      }

      if(!s.launch && (k = fx.phase(0.9F, 1.1F) * (1.0F - fx.endPhase())) > 0.01F) {
         BodyProxy.Part h = fx.parts[0];

         for(int i = 0; i < 4; ++i) {
            double a = 6.283185307179586D * (double)i / 4.0D + (double)t * 4.0D;
            double r = 0.3D * (double)fx.size;
            star(f.rx(h.x + Math.cos(a) * r), f.ry(h.y + 0.32D + 0.05D * Math.sin(a * 3.0D)), f.rz(h.z + Math.sin(a) * r), 0.11D * (double)fx.size, (float)(a * 1.5D), fill, line, alpha * k);
         }
      }

   }

   private static void star(double x, double y, double z, double size, float spin, int fill, int line, float a) {
      Vfx.sprite(Textures.STAR, x, y, z, size * 1.3D, spin, ColorUtil.mulAlpha(line, a));
      Vfx.sprite(Textures.STAR, x, y, z, size, spin, ColorUtil.mulAlpha(fill, a));
   }

   private void glow(KillFx fx, FxFrame f, KnockoutStyle.State s) {
      float alpha = fx.alpha();
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      int fill = this.color.getColor(0.5F);
      float flash;
      if(!calm && (flash = 1.0F - Easing.clamp01(Math.abs(fx.t - 0.35F) / 0.08F)) > 0.0F) {
         BodyProxy.Part b = fx.rest[1];
         Vfx.sprite(Textures.GLOW, f.rx(b.x), f.ry(b.y + 0.4D), f.rz(b.z), 2.4D * (double)fx.size, 0.0F, alpha(fill, 0.35F * flash * alpha));
      }

      if(s.launch && this.twinkle.get()) {
         float k = fx.phase(1.4000001F, 1.65F);
         if(k > 0.0F && k < 1.0F) {
            this.flightAt(fx, s, 0.8000000715255737D, this.at);
            double x = f.rx(this.at[0]);
            double y = f.ry(this.at[1]);
            double z = f.rz(this.at[2]);
            float bloom = (float)Math.sin(3.141592653589793D * (double)k);
            double size = (s.ceiling?0.45D:0.9D) * (double)fx.size * (double)bloom;
            float a = alpha * bloom * (calm?0.55F:1.0F);
            Vfx.sprite(Textures.STAR, x, y, z, size * 0.55D, (float)((double)k * 2.0D), core(fill, a));
            Vfx.sprite(Textures.FLARE, x, y, z, size * 1.4D, 0.3F, alpha(fill, 0.7F * a));
            Vfx.sprite(Textures.GLOW, x, y, z, size * 1.8D, 0.0F, alpha(fill, 0.35F * a));
         }
      }

   }

   public String stateForTest(KillFx fx) {
      KnockoutStyle.State s = (KnockoutStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.2F?"turning toon":(t < 0.35F?"the burst slams in":(t < 0.65F?"shaking":(!s.launch?(t < 1.0F?"flopping":"flat, seeing stars"):(t < 1.45F?(s.ceiling?"spinning under a ceiling":"launched"):(t < 1.65F?"twinkling":"gone")))));
         Comic.Word w = Comic.word(s.text);
         Object[] objectArray = new Object[]{phase, s.text, w.ready()?"ready":"painting", Float.valueOf(3.0F * this.outlineWidth.getFloat() / 100.0F), this.halftone.get()?(Halftone.usable()?"shader":"printed"):"off", s.launch?"on":"off", s.ceiling?", ceiling above":""};
         return String.format(Locale.ROOT, "%s; \"%s\" (%s), outline %.1f px, halftone %s, launch %s%s", objectArray);
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   private static final class State {
      String text;
      boolean ceiling;
      boolean launch;
      double awayX;
      double awayZ;
      double spinX;
      double spinY;
      double spinZ;
      int side;
      double pivotX;
      double pivotY;
      double pivotZ;

      private State() {
      }
   }

   public static enum Word implements Displayable {
      KO("K.O.!", "The classic"),
      CUSTOM("Custom", "Your own word, up to 8 characters");

      private final String displayName;
      private final String description;

      private Word(String displayName, String description) {
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
}
