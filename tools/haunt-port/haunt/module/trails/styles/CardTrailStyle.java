package mix.client.mixui.integration.haunt.module.trails.styles;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxParticles;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.kit.Cards;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.trail.TrailAnchor;
import mix.client.mixui.integration.haunt.module.trails.PlayerTrail;
import mix.client.mixui.integration.haunt.module.trails.TrailFrame;
import mix.client.mixui.integration.haunt.module.trails.TrailStyle;
import mix.client.mixui.integration.haunt.module.trails.TrailStyleBase;
import mix.client.mixui.integration.haunt.module.trails.TrailsModule;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CardTrailStyle extends TrailStyleBase {
   private static final int MAX_CARDS = 96;
   private static final float PEEL = 0.28F;
   private static final float EXIT = 0.6F;
   private static final float FLIP_SECONDS = 1.2F;
   private static final int EMBER = -24512;
   private static final float RIM = 0.2F;
   public final ColorSetting symbolColor = this.signatureColor("symbols", "Symbols", "The line art on the faces and the backs.", -1185802, -1);
   public final ColorSetting foilTint;
   public final ModeSetting<Cards.Foil> foil = (ModeSetting)this.setting((new ModeSetting("card_trail.foil", "Foil", "The borders\' metal leaf: Gold, Silver, Rose gold, or Custom (the Foil Tint).", Cards.Foil.class, Cards.Foil.GOLD)).style(ModeSetting.Style.CHIPS).keywords(new String[]{"foil", "gold", "silver", "rose gold", "material", "metal"}));
   public final SliderSetting spacing;
   public final BooleanSetting flip;
   public final ModeSetting<Cards.Design> design;
   public final ModeSetting<Cards.Symbols> symbols;
   public final ModeSetting<CardTrailStyle.Exit> exit;
   private final Rng rng = new Rng();
   private final Cards.Card[] drawn = new Cards.Card[96];
   private final int[] drawnIndex = new int[96];

   public CardTrailStyle(TrailsModule module) {
      super(module, TrailStyle.CARD_TRAIL, spec().color("Card", -12903590).signature(-14996902).size(0.15D, 0.6D, 0.34D, 0.01D).length(2.4D).anchor(TrailAnchor.BACK, false));
      this.foil.group("Colors");
      this.foilTint = (ColorSetting)this.setting((new ColorSetting("card_trail.foilTint", "Foil Tint", "The foil\'s color with Custom.", ColorSetting.Mode.STATIC, -1851025, -597080, 1.0D)).secondary().visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CardTrailStyle.this.foil.get() == Cards.Foil.CUSTOM;
         }
      }));
      this.foilTint.group("Colors");
      this.signature(this.foilTint, -1851025);
      this.doesNotApply(new Setting[]{this.density, this.glow});
      this.spacing = (SliderSetting)this.setting((new SliderSetting("card_trail.spacing", "Spacing", "How far you travel between two cards.", 1.0D, 4.0D, 2.0D, 0.25D)).unit(" blocks"));
      this.flip = (BooleanSetting)this.setting(new BooleanSetting("card_trail.flip", "Flip", "The cards turn over and over, their backs and faces coming round in turn.", true));
      this.design = (ModeSetting)this.setting((new ModeSetting("card_trail.design", "Card Design", "The frames on the faces and the pattern on the backs.", Cards.Design.class, Cards.Design.CELESTIAL)).style(ModeSetting.Style.CHIPS));
      this.symbols = (ModeSetting)this.setting((new ModeSetting("card_trail.symbolSet", "Symbols", "Which symbols the faces show.", Cards.Symbols.class, Cards.Symbols.SKY)).style(ModeSetting.Style.CHIPS));
      this.exit = (ModeSetting)this.setting((new ModeSetting("card_trail.exit", "Exit", "How each card leaves.", CardTrailStyle.Exit.class, CardTrailStyle.Exit.BURN)).style(ModeSetting.Style.CHIPS));

      for(int k = 0; k < 96; ++k) {
         this.drawn[k] = new Cards.Card();
      }

   }

   protected void addLookTargets(List<SliderSetting> out) {
      out.add(this.spacing);
   }

   protected TrailStyleBase.Store newStore() {
      return new CardTrailStyle.Deck();
   }

   public int passes() {
      return FxPass.SOLID.bit() | FxPass.GLOW.bit();
   }

   public void renderPass(PlayerTrail p, TrailFrame f, FxPass pass) {
      if(!p.ghost && this.spawnPass(pass)) {
         int due = this.piecesDue(p, 1.0D / (double)this.spacing.getFloat(), 2);
         if(due != 0) {
            CardTrailStyle.Deck d = (CardTrailStyle.Deck)this.store(f);
            double yawR = Math.toRadians((double)p.pose.bodyYaw);
            float fx = (float)(-Math.sin(yawR));
            float fz = (float)Math.cos(yawR);
            int[] set = ((Cards.Symbols)this.symbols.get()).set;
            float life = this.lengthNow(p.speed);

            for(int i = 0; i < due && !this.module.piecesFull(f); ++i) {
               this.rng.seed((int)(f.now * 3803.0D) + i * 151 + p.entityId * 17);
               double x = p.headX - (double)fx * 0.15D;
               double y = p.headY + 0.05D;
               double z = p.headZ - (double)fz * 0.15D;
               if(f.nearFade(x, y, z) >= 0.3F) {
                  int k = d.add();
                  d.x[k] = x;
                  d.y[k] = y;
                  d.z[k] = z;
                  float side = this.rng.signed() * 0.35F;
                  d.vx[k] = -fx * (1.3F + this.rng.next() * 0.5F) + fz * side;
                  d.vz[k] = -fz * (1.3F + this.rng.next() * 0.5F) - fx * side;
                  d.vy[k] = 0.45F + this.rng.next() * 0.35F;
                  d.born[k] = d.clock;
                  d.life[k] = life * (0.85F + this.rng.next() * 0.3F);
                  d.half[k] = this.size.getFloat() * 0.5F * p.widthScale;
                  d.alpha[k] = this.opacity.getFloat() / 100.0F * p.alpha;
                  d.yaw[k] = (float)(yawR + (this.rng.next() < 0.5F?0.5D:-0.5D) * 3.141592653589793D + (double)this.rng.signed() * 0.4D);
                  d.spin[k] = this.rng.signed() * 0.45F;
                  d.flipAt[k] = this.rng.next();
                  d.bob[k] = this.rng.next() * 6.283F;
                  d.tumble[k] = this.rng.signed();
                  d.symbol[k] = set[d.dealt++ % set.length];
                  d.tint[k] = this.tint;
                  d.far[k] = p.reduced;
                  d.burst[k] = false;
               }
            }
         }
      }

   }

   private static float exitOf(CardTrailStyle.Deck d, int i) {
      float age = d.clock - d.born[i];
      return Easing.clamp01((age - (d.life[i] - 0.6F)) / 0.6F);
   }

   private int layout(TrailFrame f, CardTrailStyle.Deck d, Camera cam) {
      int m = 0;
      CardTrailStyle.Exit how = (CardTrailStyle.Exit)this.exit.get();

      for(int i = 0; i < d.n && m < 96; ++i) {
         float age = d.clock - d.born[i];
         float e = exitOf(d, i);
         boolean far = d.far[i];
         float peel = Easing.outCubic(Easing.clamp01(age / 0.28F));
         double x = d.x[i];
         double y = d.y[i] + Math.sin((double)(age * 2.2F + d.bob[i])) * 0.035D * (double)peel;
         double z = d.z[i];
         double half = (double)d.half[i] * (0.4D + 0.6D * (double)peel);
         float a = d.alpha[i] * Easing.clamp01(age / 0.08F) * f.nearFade(x, y, z) * f.fog(x, y, z);
         double tilt = 0.0D;
         if(!far && (how != CardTrailStyle.Exit.BURN || Cards.burnWorks())) {
            if(how == CardTrailStyle.Exit.SPARKLE) {
               half *= 1.0D - 0.75D * (double)Easing.inCubic(e);
               a *= 1.0F - Easing.smoothstep(0.6F, 1.0F, e);
            } else if(how == CardTrailStyle.Exit.FALL) {
               double drop = 1.4D * (double)e * (double)e;
               y -= drop;
               tilt = (double)(d.tumble[i] * e) * 2.2D;
               a *= 1.0F - e;
            }
         } else {
            a *= 1.0F - e;
         }

         if(a > 0.01F && half > 1.0E-4D) {
            double turn = (double)d.yaw[i];
            if(this.flip.get() && !far) {
               turn += (double)(age / 1.2F + d.flipAt[i]) * 3.141592653589793D;
            }

            double nx = Math.cos(turn);
            double nz = Math.sin(turn);
            Cards.Card c = this.drawn[m];
            double ct = Math.cos(tilt);
            double st = Math.sin(tilt);
            double rx0 = -nz;
            c.ux = rx0 * st;
            c.uy = ct;
            c.uz = nx * st;
            c.nx = nx;
            c.ny = 0.0D;
            c.nz = nz;
            c.rx = c.uy * c.nz - c.uz * c.ny;
            c.ry = c.uz * c.nx - c.ux * c.nz;
            c.rz = c.ux * c.ny - c.uy * c.nx;
            c.x = x - f.viewX;
            c.y = y - f.viewY;
            c.z = z - f.viewZ;
            c.half = half;
            c.alpha = a;
            c.symbol = d.symbol[i];
            c.uncut();
            this.drawnIndex[m] = i;
            ++m;
         }
      }

      return m;
   }

   public void renderPiecesPass(TrailFrame f, FxPass pass) {
      CardTrailStyle.Deck d = (CardTrailStyle.Deck)f.store(this.type);
      if(d != null && d.n != 0) {
         Camera cam = Vfx.camera();
         int m = this.layout(f, d, cam);
         if(m != 0) {
            boolean burning = this.exit.get() == CardTrailStyle.Exit.BURN && Cards.burnWorks();
            if(pass == FxPass.GLOW) {
               this.glow(f, d, m, cam, burning);
            } else {
               if(burning) {
                  for(int k = 0; k < m; ++k) {
                     int i = this.drawnIndex[k];
                     float e = exitOf(d, i);
                     if(e > 0.0F && !d.far[i]) {
                        Cards.burnHoles(this.drawn[k], e, i % 4, cam);
                     }
                  }
               }

               MeshBatch.begin(Material.CUSTOM, Finish.SATIN, this.matLight, 0.2F + 0.3F * this.night, -1, true, true);

               try {
                  int body = this.color.getColor(0.5F);

                  for(int kx = 0; kx < m; ++kx) {
                     int i = this.drawnIndex[kx];
                     Cards.body(this.drawn[kx], Cards.withAlpha(d.tint[i] != 0?d.tint[i]:body, this.drawn[kx].alpha));
                  }
               } finally {
                  MeshBatch.end();
               }

               Cards.Foil fl = (Cards.Foil)this.foil.get();
               MeshBatch.begin(fl.material, Finish.METALLIC, this.matLight, 0.2F, -1, true, true);

               try {
                  int tintCol = fl == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):-1;

                  for(int kx = 0; kx < m; ++kx) {
                     int i = this.drawnIndex[kx];
                     double sweep = ((double)(d.clock - d.born[i]) * 0.7D + (double)d.flipAt[i]) % 2.2D * 1.8D - 0.4D;
                     Cards.foil(this.drawn[kx], Cards.withAlpha(tintCol, this.drawn[kx].alpha), sweep, true);
                  }
               } finally {
                  MeshBatch.end();
               }

               int var32 = ColorUtil.shade(this.symbolColor.getColor(0.5F), Math.max(0.45F, this.matLight)) | -16777216;
               RenderState.decal(true);
               RenderState.translucent(false);

               try {
                  Cards.art(this.drawn, m, ((Cards.Design)this.design.get()).ordinal(), var32, 0.9F);
               } finally {
                  RenderState.decal(false);
                  RenderState.translucent(true);
               }
            }
         }
      }

   }

   private void glow(TrailFrame f, CardTrailStyle.Deck d, int m, Camera cam, boolean burning) {
      CardTrailStyle.Exit how = (CardTrailStyle.Exit)this.exit.get();

      for(int k = 0; k < m; ++k) {
         int i = this.drawnIndex[k];
         float e = exitOf(d, i);
         if(e > 0.0F && !d.far[i]) {
            Cards.Card c = this.drawn[k];
            if(how == CardTrailStyle.Exit.BURN && burning) {
               Cards.burnEdge(c, e, 0.07F, i % 4, -24512, c.alpha, cam);
            } else if((how == CardTrailStyle.Exit.SPARKLE || how == CardTrailStyle.Exit.BURN && !burning) && !d.burst[i]) {
               d.burst[i] = true;
               this.sparkles(f, d, i);
            }
         }
      }

      FxParticles pool = f.piecesIfAny(this.type);
      if(pool != null) {
         pool.render(f.viewX, f.viewY, f.viewZ);
      }

   }

   private void sparkles(TrailFrame f, CardTrailStyle.Deck d, int i) {
      FxParticles pool = f.pieces(this.type);
      Cards.Foil fl = (Cards.Foil)this.foil.get();
      int col = fl == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):fl.material.swatch();

      for(int s = 0; s < 7 && !this.module.piecesFull(f); ++s) {
         this.rng.seed(i * 97 + s * 13 + d.dealt);
         pool.spawn(s % 3 == 0?5:1, d.x[i] + (double)(this.rng.signed() * d.half[i]), d.y[i] + (double)(this.rng.signed() * d.half[i]) * 1.5D, d.z[i] + (double)(this.rng.signed() * d.half[i]), this.rng.signed() * 0.6F, 0.2F + this.rng.next() * 0.6F, this.rng.signed() * 0.6F, 0.45F + this.rng.next() * 0.3F, 0.03F + this.rng.next() * 0.025F).drag(1.5F).color(col).alpha(d.alpha[i]).end(0.01F);
      }

   }

   public String stateForTest() {
      return String.format(Locale.ROOT, "spacing %.2f, flip %s, %s, %s, %s foil, exit %s (burn %s)", new Object[]{Float.valueOf(this.spacing.getFloat()), this.flip.get()?"on":"off", ((Cards.Design)this.design.get()).displayName(), ((Cards.Symbols)this.symbols.get()).displayName(), ((Cards.Foil)this.foil.get()).displayName(), ((CardTrailStyle.Exit)this.exit.get()).displayName(), Cards.burnWorks()?"with holes":"as a fade"});
   }

   static final class Deck implements TrailStyleBase.Store {
      final double[] x = new double[96];
      final double[] y = new double[96];
      final double[] z = new double[96];
      final float[] vx = new float[96];
      final float[] vy = new float[96];
      final float[] vz = new float[96];
      final float[] born = new float[96];
      final float[] life = new float[96];
      final float[] half = new float[96];
      final float[] alpha = new float[96];
      final float[] yaw = new float[96];
      final float[] spin = new float[96];
      final float[] flipAt = new float[96];
      final float[] bob = new float[96];
      final float[] tumble = new float[96];
      final int[] symbol = new int[96];
      final int[] tint = new int[96];
      final boolean[] far = new boolean[96];
      final boolean[] burst = new boolean[96];
      int n;
      int dealt;
      float clock;

      int add() {
         if(this.n < 96) {
            return this.n++;
         } else {
            int oldest = 0;

            for(int i = 1; i < this.n; ++i) {
               if(this.born[i] < this.born[oldest]) {
                  oldest = i;
               }
            }

            return oldest;
         }
      }

      public void update(float dt) {
         this.clock += dt;
         if(dt > 0.0F) {
            float damp = (float)Math.exp(-2.4D * (double)dt);
            int w = 0;

            for(int i = 0; i < this.n; ++i) {
               float age = this.clock - this.born[i];
               if(age < this.life[i]) {
                  this.vx[i] *= damp;
                  this.vz[i] *= damp;
                  this.vy[i] *= damp;
                  this.x[i] += (double)(this.vx[i] * dt);
                  this.y[i] += (double)(this.vy[i] * dt);
                  this.z[i] += (double)(this.vz[i] * dt);
                  this.yaw[i] += this.spin[i] * dt;
                  if(w != i) {
                     this.copy(i, w);
                  }

                  ++w;
               }
            }

            this.n = w;
         }

      }

      private void copy(int a, int b) {
         this.x[b] = this.x[a];
         this.y[b] = this.y[a];
         this.z[b] = this.z[a];
         this.vx[b] = this.vx[a];
         this.vy[b] = this.vy[a];
         this.vz[b] = this.vz[a];
         this.born[b] = this.born[a];
         this.life[b] = this.life[a];
         this.half[b] = this.half[a];
         this.alpha[b] = this.alpha[a];
         this.yaw[b] = this.yaw[a];
         this.spin[b] = this.spin[a];
         this.flipAt[b] = this.flipAt[a];
         this.bob[b] = this.bob[a];
         this.tumble[b] = this.tumble[a];
         this.symbol[b] = this.symbol[a];
         this.tint[b] = this.tint[a];
         this.far[b] = this.far[a];
         this.burst[b] = this.burst[a];
      }

      public int size() {
         return this.n;
      }

      public void clear() {
         this.n = 0;
      }
   }

   public static enum Exit implements Displayable {
      BURN("Burn away", "Eaten away from its edges with a thin ember line"),
      SPARKLE("Sparkle out", "A burst of gold sparkles as it shrinks away"),
      FALL("Fall and fade", "Tumbling down as it fades");

      private final String displayName;
      private final String description;

      private Exit(String displayName, String description) {
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
