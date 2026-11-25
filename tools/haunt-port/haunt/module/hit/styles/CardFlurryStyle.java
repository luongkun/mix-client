package mix.client.mixui.integration.haunt.module.hit.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.Cards;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.hit.HitEffectModule;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class CardFlurryStyle extends HitStyleBase {
   private static final float FAN = 0.16F;
   private static final float EXIT = 0.6666667F;
   private static final int MAX_CARDS = 7;
   private static final float RIM = 0.14F;
   private static final int EMBER = -15809;
   public final ColorSetting symbolColor;
   public final ModeSetting<Cards.Foil> foil;
   public final ColorSetting foilTint;
   public final SliderSetting cards;
   public final ModeSetting<Cards.Design> design;
   public final ModeSetting<Cards.Symbols> symbols;
   public final SliderSetting spin;
   public final ModeSetting<CardFlurryStyle.Exit> exit;
   private final Cards.Card[] deck = new Cards.Card[7];
   private int count;

   public CardFlurryStyle(HitEffectModule module) {
      super(module, HitStyle.CARD_FLURRY, 0.75F, new ColorSetting("cardflurry.color", "Card", "The cards\' deep background.", ColorSetting.Mode.STATIC, -12903590, -14996902, 1.0D));
      this.signature(this.color, -12903590);
      this.symbolColor = (ColorSetting)this.setting((new ColorSetting("cardflurry.symbols", "Symbols", "The line art on the faces and the backs.", ColorSetting.Mode.STATIC, -1185802, -1, 1.0D)).secondary());
      this.signature(this.symbolColor, -1185802);
      this.foil = (ModeSetting)this.setting((new ModeSetting("cardflurry.material", "Foil", "The borders\' metal leaf: Gold, Silver, Rose gold, or Custom (the Foil Tint).", Cards.Foil.class, Cards.Foil.GOLD)).style(ModeSetting.Style.CHIPS).keywords(new String[]{"foil", "gold", "silver", "rose gold", "material", "metal"}));
      this.foilTint = (ColorSetting)this.setting((new ColorSetting("cardflurry.foilTint", "Foil Tint", "The foil\'s color with Custom.", ColorSetting.Mode.STATIC, -1851025, -597080, 1.0D)).secondary().visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CardFlurryStyle.this.foil.get() == Cards.Foil.CUSTOM;
         }
      }));
      this.signature(this.foilTint, -1851025);
      this.cards = this.look((SliderSetting)this.setting(new SliderSetting("cardflurry.cards", "Cards", "How many cards fan out.", 2.0D, 6.0D, 3.0D, 1.0D)));
      this.design = (ModeSetting)this.setting((new ModeSetting("cardflurry.design", "Card Design", "The frames on the faces and the pattern on the backs.", Cards.Design.class, Cards.Design.CELESTIAL)).style(ModeSetting.Style.CHIPS));
      this.symbols = (ModeSetting)this.setting((new ModeSetting("cardflurry.symbolSet", "Symbols", "Which symbols the faces show.", Cards.Symbols.class, Cards.Symbols.SKY)).style(ModeSetting.Style.CHIPS));
      this.spin = (SliderSetting)this.setting((new SliderSetting("cardflurry.spin", "Spin", "How fast the cards spin and flip as they fly.", 0.0D, 100.0D, 60.0D, 5.0D)).unit("%"));
      this.exit = (ModeSetting)this.setting((new ModeSetting("cardflurry.exit", "Exit", "How the cards go: burning away from their edges, in sparkles, or fading.", CardFlurryStyle.Exit.class, CardFlurryStyle.Exit.BURN)).style(ModeSetting.Style.CHIPS));

      for(int k = 0; k < 7; ++k) {
         this.deck[k] = new Cards.Card();
      }

   }

   public int passes(HitFx fx) {
      return FxPass.SOLID.bit() | FxPass.GLOW.bit();
   }

   private static float exitOf(float p) {
      return Easing.clamp01((p - 0.6666667F) / 0.3333333F);
   }

   private void layout(HitFx fx, FxFrame f, float p) {
      Basis b = fx.basis;
      double s = scale(fx);
      int n = this.cards.getInt();
      double half = 0.12D * s;
      double ox = fx.ix() - b.fx * 0.15D;
      double oy = fx.iy() - b.fy * 0.15D;
      double oz = fx.iz() - b.fz * 0.15D;
      double reach = half * 3.2D;
      double px = ox - b.ux * reach;
      double py = oy - b.uy * reach;
      double pz = oz - b.uz * reach;
      double spread = Math.toRadians(n <= 3?24.0D:(n == 4?18.0D:(n == 5?15.0D:13.0D)));
      float fan = Easing.outBack(Easing.clamp01(p / 0.16F), 1.6F);
      double tau = Math.max(0.0D, (double)((p - 0.16F) * this.baseSeconds()));
      double spinAmount = (double)this.spin.getFloat() / 100.0D;
      int[] set = ((Cards.Symbols)this.symbols.get()).set;
      float e = exitOf(p);
      CardFlurryStyle.Exit how = (CardFlurryStyle.Exit)this.exit.get();
      this.count = 0;
      int total = fx.crit?n + 1:n;

      for(int k = 0; k < total; ++k) {
         boolean big = fx.crit && k == n;
         int id = fx.seed + k * 37;
         double phi = big?0.0D:((double)k - (double)(n - 1) * 0.5D) * spread;
         double a = phi * (double)fan;
         double ca = Math.cos(a);
         double sa = Math.sin(a);
         double upx = b.ux * ca + b.rx * sa;
         double upy = b.uy * ca + b.ry * sa;
         double upz = b.uz * ca + b.rz * sa;
         double acx = b.rx * ca - b.ux * sa;
         double acy = b.ry * ca - b.uy * sa;
         double acz = b.rz * ca - b.uz * sa;
         double cx = px + upx * reach;
         double cy = py + upy * reach;
         double cz = pz + upz * reach;
         double along = big?1.6D:0.9D + 0.5D * hash(id, 1);
         double out = big?1.5D:1.7D + 0.6D * hash(id, 2);
         double travel = (1.0D - Math.exp(-2.4D * tau)) / 2.4D;
         double wide = phi * 3.0D;
         double fdx = Math.cos(wide) * b.ux + Math.sin(wide) * b.rx;
         double fdy = Math.cos(wide) * b.uy + Math.sin(wide) * b.ry;
         double fdz = Math.cos(wide) * b.uz + Math.sin(wide) * b.rz;
         cx = cx + (b.fx * along + fdx * out) * travel;
         cy = cy + ((b.fy * along + fdy * out) * travel - 0.6D * tau * tau);
         cz = cz + (b.fz * along + fdz * out) * travel;
         double psi = big?0.0D:(double)(hash(id, 3) < 0.5D?-1:1) * spinAmount * (5.0D + 4.0D * hash(id, 4)) * tau;
         double theta = big?0.0D:spinAmount * 3.141592653589793D * (2.0D + 1.5D * hash(id, 5)) * tau;
         double cps = Math.cos(psi);
         double sps = Math.sin(psi);
         double rx = acx * cps + upx * sps;
         double ry = acy * cps + upy * sps;
         double rz = acz * cps + upz * sps;
         double ux = upx * cps - acx * sps;
         double uy = upy * cps - acy * sps;
         double uz = upz * cps - acz * sps;
         double nx0 = -b.fx;
         double ny0 = -b.fy;
         double nz0 = -b.fz;
         double ct = Math.cos(theta);
         double st = Math.sin(theta);
         double fx2 = rx * ct + nx0 * st;
         double fy2 = ry * ct + ny0 * st;
         double fz2 = rz * ct + nz0 * st;
         double nx = nx0 * ct - rx * st;
         double ny = ny0 * ct - ry * st;
         double nz = nz0 * ct - rz * st;
         Cards.Card c = this.deck[this.count++];
         c.x = f.rx(cx);
         c.y = f.ry(cy);
         c.z = f.rz(cz);
         c.rx = fx2;
         c.ry = fy2;
         c.rz = fz2;
         c.ux = ux;
         c.uy = uy;
         c.uz = uz;
         c.nx = nx;
         c.ny = ny;
         c.nz = nz;
         double size = half * (big?1.5D:1.0D) * (0.55D + 0.45D * (double)Easing.clamp01(p / 0.16F));
         if(how == CardFlurryStyle.Exit.SPARKLE && e > 0.0F) {
            size *= Math.pow(1.0D - (double)e, 0.7D);
         }

         c.half = size;
         c.symbol = big?3:set[k % set.length];
         float a8 = fx.alpha() * fx.keep.cover(c.x, c.y, c.z);
         if(how == CardFlurryStyle.Exit.FADE || how == CardFlurryStyle.Exit.BURN && !Cards.burnWorks()) {
            a8 *= 1.0F - e;
         }

         c.alpha = a8;
      }

   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
      float p = fx.progress();
      if(fx.alpha() > 0.003F) {
         this.layout(fx, f, p);
         Camera cam = Vfx.camera();
         float e = exitOf(p);
         boolean burning = this.exit.get() == CardFlurryStyle.Exit.BURN && Cards.burnWorks() && e > 0.0F;
         if(pass == FxPass.SOLID) {
            if(burning) {
               for(int k = 0; k < this.count; ++k) {
                  Cards.burnHoles(this.deck[k], e, k % 4, cam);
               }
            }

            MeshBatch.begin(Material.CUSTOM, Finish.SATIN, fx.light, 0.14F, -1, true, true);
            int body = this.color.getColor(0.5F);

            for(int k = 0; k < this.count; ++k) {
               Cards.body(this.deck[k], Cards.withAlpha(body, this.deck[k].alpha));
            }

            MeshBatch.end();
            Cards.Foil fl = (Cards.Foil)this.foil.get();
            MeshBatch.begin(fl.material, Finish.METALLIC, fx.light, 0.14F, -1, true, true);
            int tint = fl == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):-1;

            for(int k = 0; k < this.count; ++k) {
               double sweep = ((double)p * 2.2D + (double)k * 0.37D) % 1.8D - 0.4D;
               Cards.foil(this.deck[k], Cards.withAlpha(tint, this.deck[k].alpha), sweep, true);
            }

            MeshBatch.end();
            int ink = ColorUtil.shade(this.symbolColor.getColor(0.5F), fx.light);
            RenderState.decal(true);
            RenderState.translucent(false);

            try {
               Cards.art(this.deck, this.count, ((Cards.Design)this.design.get()).ordinal(), ink, 0.9F);
            } finally {
               RenderState.decal(false);
               RenderState.translucent(true);
            }
         } else if(pass == FxPass.GLOW) {
            this.glow(fx, cam, p, e, burning);
         }
      }

   }

   private void glow(HitFx fx, Camera cam, float p, float e, boolean burning) {
      if(burning) {
         for(int k = 0; k < this.count; ++k) {
            Cards.burnEdge(this.deck[k], e, 0.07F, k % 4, -15809, this.deck[k].alpha, cam);
         }
      } else if(e > 0.0F && this.exit.get() != CardFlurryStyle.Exit.FADE && !fx.lod) {
         int col = this.foil.get() == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):((Cards.Foil)this.foil.get()).material.swatch();

         for(int k = 0; k < this.count; ++k) {
            Cards.Card c = this.deck[k];
            float k8 = (float)Math.sin(3.141592653589793D * (double)e) * fx.alpha();

            for(int i = 0; i < 5; ++i) {
               double a = ((double)i + (double)k * 0.7D) * 1.2566D + (double)e * 3.0D;
               double r = c.half * (1.2D + 0.8D * (double)e);
               double x = c.x + (c.rx * Math.cos(a) + c.ux * Math.sin(a)) * r;
               double y = c.y + (c.ry * Math.cos(a) + c.uy * Math.sin(a)) * r;
               double z = c.z + (c.rz * Math.cos(a) + c.uz * Math.sin(a)) * r;
               Vfx.sprite(Textures.SPARK, x, y, z, c.half * 0.5D, 0.0F, core(col, 0.8F * k8));
            }
         }
      }

      float flash;
      if(fx.crit && this.count > 0 && (flash = 1.0F - Easing.clamp01(Math.abs(p - 0.16F) / 0.08F)) > 0.0F && !FuriahsVisuals.instance.isReduceFlashing()) {
         Cards.Card c = this.deck[this.count - 1];
         int col = this.foil.get() == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):((Cards.Foil)this.foil.get()).material.swatch();
         Vfx.sprite(Textures.FLARE, c.x, c.y, c.z, c.half * 4.0D, 0.4F, core(col, 0.85F * flash * fx.alpha()));
         Vfx.sprite(Textures.GLOW, c.x, c.y, c.z, c.half * 5.0D, 0.0F, alpha(col, 0.4F * flash * fx.alpha()));
      }

   }

   public String stateForTest(HitFx fx) {
      float p = fx.progress();
      String phase = p < 0.16F?"fanning out":(p < 0.6666667F?"flying":((CardFlurryStyle.Exit)this.exit.get()).displayName().toLowerCase(Locale.ROOT));
      return String.format(Locale.ROOT, "%s: %d card(s)%s, %s foil, %s, %s, spin %d%%; exit %.2f, burn %s", new Object[]{phase, Integer.valueOf(this.cards.getInt()), fx.crit?" and the eye":"", ((Cards.Foil)this.foil.get()).displayName(), ((Cards.Design)this.design.get()).displayName(), ((Cards.Symbols)this.symbols.get()).displayName(), Integer.valueOf(this.spin.getInt()), Float.valueOf(exitOf(p)), Cards.burnWorks()?"with holes":"as a fade"});
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      return (double)(h >>> 8 & 16777215) / 1.6777216E7D;
   }

   public static enum Exit implements Displayable {
      BURN("Burn away", "Each card burns away from its edges"),
      SPARKLE("Sparkle out", "The cards shrink away in sparkles"),
      FADE("Fade", "The cards fade out");

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
