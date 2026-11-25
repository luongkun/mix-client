package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.module.particles.LibraryImageSetting;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.EnumSetSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class IconsStyle extends TrailStyleBase {
   public final EnumSetSetting<IconsStyle.Icon> icons = (EnumSetSetting)this.setting(new EnumSetSetting("icons.set", "Icon Set", "Which icons pop out (each is a random one of these).", IconsStyle.Icon.class, true, new IconsStyle.Icon[]{IconsStyle.Icon.HEARTS}));
   public final LibraryImageSetting image = (LibraryImageSetting)this.setting(new LibraryImageSetting("icons.image", "Icon Image", "An image from the Particles section\'s library (a missing file is left out)."));
   public final SliderSetting floatUp;
   public final SliderSetting spin;
   public final SliderSetting bounce;
   public final BooleanSetting tintIcons;
   private final Rng rng = new Rng();
   private final int[] picked = new int[IconsStyle.Icon.values().length];
   private int pickedCount;

   public IconsStyle(TrailsModule module) {
      super(module, TrailStyle.ICONS, spec().color("Icons", -36944).size(0.1D, 0.8D, 0.3D, 0.01D).length(1.6D).anchor(TrailAnchor.BACK, false).animated().windy().along(TrailStyleBase.ColorAlong.HEAD_TO_TAIL));
      this.image.visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return IconsStyle.this.icons.includes(IconsStyle.Icon.CUSTOM);
         }
      });
      this.floatUp = (SliderSetting)this.setting((new SliderSetting("icons.floatUp", "Float Up", "How fast the icons float up.", 0.0D, 100.0D, 50.0D, 1.0D)).unit("%"));
      this.spin = (SliderSetting)this.setting((new SliderSetting("icons.spin", "Spin", "How much the icons turn as they float.", 0.0D, 100.0D, 40.0D, 1.0D)).unit("%"));
      this.bounce = (SliderSetting)this.setting((new SliderSetting("icons.bounce", "Bounce", "How bouncy their pop-in is.", 0.0D, 100.0D, 60.0D, 1.0D)).unit("%"));
      this.tintIcons = (BooleanSetting)this.setting(new BooleanSetting("icons.tint", "Tint", "Tint the icons with the color; off, images keep their own colors.", true));
   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.floatUp);
   }

   protected TrailStyleBase.Store newStore() {
      return new IconsStyle.Icons();
   }

   private void pick() {
      this.pickedCount = 0;

      for(IconsStyle.Icon i : IconsStyle.Icon.values()) {
         int t;
         if(this.icons.includes(i) && (t = this.texture(i)) > 0) {
            this.picked[this.pickedCount++] = t;
         }
      }

      if(this.pickedCount == 0) {
         this.picked[this.pickedCount++] = Textures.HEART;
      }

   }

   private int texture(IconsStyle.Icon i) {
      switch(i) {
      case STARS:
         return Textures.STAR;
      case SPARKLES:
         return Textures.SPARKLE;
      case NOTES:
         return Textures.NOTE;
      case PETALS:
         return Textures.PETAL;
      case SNOWFLAKES:
         return Textures.SNOWFLAKE;
      case SKULLS:
         return Textures.SKULL;
      case CUSTOM:
         int t = this.image.texture();
         if(t <= 0) {
            this.image.warnIfMissing("Trails › Icons", "it is left out of the icon set");
         }

         return t;
      default:
         return Textures.HEART;
      }
   }

   public void render(PlayerTrail p, TrailFrame f) {
      if(!p.ghost) {
         int due = this.piecesDue(p, 1.8D, 6);
         if(due != 0) {
            Textures.ensureTrailsLoaded();
            this.pick();
            IconsStyle.Icons ic = (IconsStyle.Icons)this.store(f);
            double yaw = Math.toRadians((double)p.pose.bodyYaw);
            float fx = (float)(-Math.sin(yaw));
            float fz = (float)Math.cos(yaw);
            float[] w = this.wind(f, p.headX, p.headY, p.headZ);
            float windX = w[0];
            float windY = w[1];
            float windZ = w[2];
            float up = this.floatUp.getFloat() / 100.0F;
            float sp = this.spin.getFloat() / 100.0F;
            float sz = this.size.getFloat() * (float)Math.sqrt((double)Math.max(0.25F, 1.0F + this.speedBoost(p.speed))) * p.widthScale;

            for(int i = 0; i < due; ++i) {
               if(this.module.piecesFull(f)) {
                  return;
               }

               this.rng.seed((int)(f.now * 5003.0D) + i * 211 + p.entityId * 17);
               float side = this.rng.signed();
               double x = p.headX - (double)fx * 0.25D - (double)(fz * side) * 0.25D;
               double y = p.headY + (double)this.rng.signed() * 0.15D;
               double z = p.headZ - (double)fz * 0.25D + (double)(fx * side) * 0.25D;
               if(f.nearFade(x, y, z) >= 0.5F) {
                  int k = ic.add();
                  ic.x[k] = x;
                  ic.y[k] = y;
                  ic.z[k] = z;
                  float out = 0.5F + 0.6F * this.rng.next();
                  ic.vx[k] = windX - fx * out - fz * side * 0.6F;
                  ic.vz[k] = windZ - fz * out + fx * side * 0.6F;
                  ic.vy[k] = windY + 0.6F + 0.6F * this.rng.next();
                  ic.rise[k] = windY + (0.15F + 1.1F * up) * (0.8F + 0.4F * this.rng.next());
                  ic.born[k] = ic.clock;
                  ic.life[k] = this.lengthNow(p.speed) * (0.8F + 0.4F * this.rng.next());
                  ic.size[k] = sz * (0.8F + 0.4F * this.rng.next());
                  ic.rot[k] = this.rng.signed() * 0.35F;
                  ic.spin[k] = this.rng.signed() * (0.3F + 2.2F * sp) * (float)Math.sqrt((double)(this.animSpeed.getFloat() / 100.0F));
                  ic.alpha[k] = p.alpha;
                  ic.spot[k] = this.rng.next();
                  ic.speed[k] = p.speed;
                  ic.phase[k] = this.rng.next() * 6.283F;
                  ic.tex[k] = this.picked[Math.min(this.pickedCount - 1, (int)(this.rng.next() * (float)this.pickedCount))];
                  ic.tint[k] = this.tint;
               }
            }
         }
      }

   }

   protected void drawStore(TrailFrame f, TrailStyleBase.Store store) {
      IconsStyle.Icons ic = (IconsStyle.Icons)store;

      int j;
      for(int i = 0; i < ic.n; ic.order[j] = i++) {
         double dx = ic.x[i] - f.eyeX;
         double dy = ic.y[i] - f.eyeY;
         double dz = ic.z[i] - f.eyeZ;
         ic.dist[i] = dx * dx + dy * dy + dz * dz;

         for(j = i; j > 0 && ic.dist[ic.order[j - 1]] < ic.dist[i]; --j) {
            ic.order[j] = ic.order[j - 1];
         }
      }

      float op = this.opacity.getFloat() / 100.0F;
      float g = this.glow.getFloat() / 100.0F;
      float bn = this.bounce.getFloat() / 100.0F;
      boolean tinted = this.tintIcons.get();
      if(g > 0.0F) {
         RenderState.additive();

         for(int o = 0; o < ic.n; ++o) {
            int i2 = ic.order[o];
            float a = this.strength(ic, i2, f, op);
            if(a > 0.004F) {
               int c = this.iconColor(ic, i2, f, tinted);
               Vfx.sprite(Textures.GLOW, ic.x[i2] - f.viewX, ic.y[i2] - f.viewY, ic.z[i2] - f.viewZ, (double)(scale(ic, i2, bn) * (1.4F + 1.2F * g)), 0.0F, ColorUtil.mulAlpha(c, a * 0.35F * g));
            }
         }
      }

      RenderState.translucent(false);

      for(int ox = 0; ox < ic.n; ++ox) {
         int i3 = ic.order[ox];
         float a = this.strength(ic, i3, f, op);
         if(a > 0.004F) {
            int c = this.iconColor(ic, i3, f, tinted);
            Vfx.sprite(ic.tex[i3], ic.x[i3] - f.viewX, ic.y[i3] - f.viewY, ic.z[i3] - f.viewZ, (double)scale(ic, i3, bn), ic.rot[i3], ColorUtil.mulAlpha(c, a));
         }
      }

      RenderState.additive();
   }

   private int iconColor(IconsStyle.Icons ic, int i, TrailFrame f, boolean tinted) {
      int c = !tinted?-1:(ic.tint[i] != 0?ic.tint[i]:this.colorAt(ic.spot[i], age(ic, i), ic.speed[i], f.colorTime, 0.0F));
      return lit(c, f, ic.x[i], ic.y[i], ic.z[i]);
   }

   private static float age(IconsStyle.Icons ic, int i) {
      return Math.min(1.0F, (ic.clock - ic.born[i]) / Math.max(0.05F, ic.life[i]));
   }

   private float strength(IconsStyle.Icons ic, int i, TrailFrame f, float op) {
      double wx = ic.x[i];
      double wy = ic.y[i];
      double wz = ic.z[i];
      return op * ic.alpha[i] * this.fade(age(ic, i)) * f.nearFade(wx, wy, wz) * f.fog(wx, wy, wz);
   }

   private static float scale(IconsStyle.Icons ic, int i, float bn) {
      float t = Easing.clamp01((ic.clock - ic.born[i]) / 0.28F);
      float s = bn > 0.0F?Easing.outBack(t, 0.6F + 3.4F * bn):Easing.outCubic(t);
      return ic.size[i] * Math.max(0.0F, s);
   }

   public static enum Icon implements Displayable {
      HEARTS("Hearts", "Hearts"),
      STARS("Stars", "Five-point stars"),
      SPARKLES("Sparkles", "Four-point sparkles"),
      NOTES("Music notes", "Music notes"),
      PETALS("Petals", "Flower petals"),
      SNOWFLAKES("Snowflakes", "Snowflakes"),
      SKULLS("Skulls", "Skulls"),
      CUSTOM("Custom image", "An image from the Particles section\'s library");

      private final String name;
      private final String description;

      private Icon(String name, String description) {
         this.name = name;
         this.description = description;
      }

      public String displayName() {
         return this.name;
      }

      public String description() {
         return this.description;
      }
   }

   static final class Icons implements TrailStyleBase.Store {
      static final int CAP = 200;
      final double[] x = new double[200];
      final double[] y = new double[200];
      final double[] z = new double[200];
      final float[] vx = new float[200];
      final float[] vy = new float[200];
      final float[] vz = new float[200];
      final float[] rise = new float[200];
      final float[] born = new float[200];
      final float[] life = new float[200];
      final float[] size = new float[200];
      final float[] rot = new float[200];
      final float[] spin = new float[200];
      final float[] alpha = new float[200];
      final float[] spot = new float[200];
      final float[] speed = new float[200];
      final float[] phase = new float[200];
      final int[] tex = new int[200];
      final int[] order = new int[200];
      final int[] tint = new int[200];
      final double[] dist = new double[200];
      int n;
      int oldest;
      float clock;

      int add() {
         if(this.n < 200) {
            return this.n++;
         } else {
            int i = this.oldest;
            this.oldest = (this.oldest + 1) % 200;
            return i;
         }
      }

      public void update(float dt) {
         this.clock += dt;
         if(dt > 0.0F) {
            float damp = (float)Math.exp(-1.6D * (double)dt);
            int w = 0;

            for(int i = 0; i < this.n; ++i) {
               float age = this.clock - this.born[i];
               if(age < this.life[i]) {
                  this.vx[i] *= damp;
                  this.vz[i] *= damp;
                  this.vy[i] = this.rise[i] + (this.vy[i] - this.rise[i]) * damp;
                  float sway = (float)Math.sin((double)(age * 2.3F + this.phase[i])) * 0.12F;
                  this.x[i] += (double)((this.vx[i] + sway * (float)Math.cos((double)this.phase[i])) * dt);
                  this.y[i] += (double)(this.vy[i] * dt);
                  this.z[i] += (double)((this.vz[i] + sway * (float)Math.sin((double)this.phase[i])) * dt);
                  this.rot[i] += this.spin[i] * dt;
                  if(w != i) {
                     this.copy(i, w);
                  }

                  ++w;
               }
            }

            this.n = w;
            this.oldest = 0;
         }

      }

      private void copy(int a, int b) {
         this.x[b] = this.x[a];
         this.y[b] = this.y[a];
         this.z[b] = this.z[a];
         this.vx[b] = this.vx[a];
         this.vy[b] = this.vy[a];
         this.vz[b] = this.vz[a];
         this.rise[b] = this.rise[a];
         this.born[b] = this.born[a];
         this.life[b] = this.life[a];
         this.size[b] = this.size[a];
         this.rot[b] = this.rot[a];
         this.spin[b] = this.spin[a];
         this.alpha[b] = this.alpha[a];
         this.spot[b] = this.spot[a];
         this.speed[b] = this.speed[a];
         this.phase[b] = this.phase[a];
         this.tex[b] = this.tex[a];
         this.tint[b] = this.tint[a];
      }

      public int size() {
         return this.n;
      }

      public void clear() {
         this.n = 0;
         this.oldest = 0;
      }
   }
}
