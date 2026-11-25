package mix.client.mixui.integration.haunt.module.targetaura.effects;

import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.kit.CardArt;
import mix.client.mixui.integration.haunt.fx.kit.Cards;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.targetaura.AuraEffect;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class TarotEffect extends AuraEffect {
   private static final double FLIP = 0.7D;
   private static final float RIM = 0.14F;
   public final ColorSetting symbolColor = (ColorSetting)this.setting((new ColorSetting("tarot.symbols", "Symbols", "The line art on the faces and the backs.", ColorSetting.Mode.STATIC, -1185802, -1, 1.0D)).secondary());
   public final ColorSetting foilTint = (ColorSetting)this.setting((new ColorSetting("tarot.foilTint", "Foil Tint", "The foil\'s color with Custom.", ColorSetting.Mode.STATIC, -1851025, -597080, 1.0D)).visibleWhen(new BooleanSupplier() {
      public boolean getAsBoolean() {
         return TarotEffect.this.foil.get() == Cards.Foil.CUSTOM;
      }
   }));
   public final ModeSetting<Cards.Foil> foil = (ModeSetting)this.setting((new ModeSetting("tarot.material", "Foil", "The borders\' metal leaf: Gold, Silver, Rose gold, or Custom (the Foil Tint).", Cards.Foil.class, Cards.Foil.GOLD)).style(ModeSetting.Style.CHIPS).keywords(new String[]{"foil", "gold", "silver", "rose gold", "material", "metal"}));
   public final SliderSetting cards;
   public final ModeSetting<TarotEffect.Arrangement> arrangement;
   public final ModeSetting<Cards.Design> design;
   public final ModeSetting<Cards.Symbols> symbols;
   public final BooleanSetting flip;
   public final SliderSetting flipEvery;
   public final BooleanSetting glint;
   private static final int MAX_CARDS = 5;
   private final Cards.Card[] cardsNow = new Cards.Card[5];
   private final float[] cardAlpha = new float[5];
   private int count;
   private double deckX;
   private double deckY;
   private double deckZ;
   private double puff;

   public TarotEffect(TargetAuraModule module) {
      super(module, EffectType.TAROT, "tarot", new ColorSetting("tarot.color", "Card", "The cards\' deep background.", ColorSetting.Mode.STATIC, -12903590, -14996902, 1.0D), true);
      this.signature(this.symbolColor, -1185802);
      this.signature(this.foilTint, -1851025);
      this.cards = this.look((SliderSetting)this.setting(new SliderSetting("tarot.cards", "Cards", "How many cards float round the target.", 3.0D, 5.0D, 3.0D, 1.0D)));
      this.arrangement = (ModeSetting)this.setting(new ModeSetting("tarot.arrangement", "Arrangement", "An arc round the chest, all the way round, or a small crown of cards above the head.", TarotEffect.Arrangement.class, TarotEffect.Arrangement.ARC));
      this.design = (ModeSetting)this.setting((new ModeSetting("tarot.design", "Card Design", "The frames on the faces and the pattern on the backs.", Cards.Design.class, Cards.Design.CELESTIAL)).style(ModeSetting.Style.CHIPS));
      this.symbols = (ModeSetting)this.setting((new ModeSetting("tarot.symbolSet", "Symbols", "Which symbols the faces show.", Cards.Symbols.class, Cards.Symbols.SKY)).style(ModeSetting.Style.CHIPS));
      this.flip = (BooleanSetting)this.setting(new BooleanSetting("tarot.flip", "Flip", "Now and then a card flips over to show a new symbol; off: never.", true));
      this.flipEvery = this.look((SliderSetting)this.setting((new SliderSetting("tarot.flipEvery", "Flip Every", "Seconds between flips.", 3.0D, 15.0D, 5.0D, 1.0D)).unit("s").visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return TarotEffect.this.flip.get();
         }
      })));
      this.glint = (BooleanSetting)this.setting(new BooleanSetting("tarot.glint", "Glint", "A glint sweeps over the foil now and then.", true));

      for(int k = 0; k < 5; ++k) {
         this.cardsNow[k] = new Cards.Card();
      }

   }

   public void render(EffectContext c, FxPass pass) {
      if(pass == FxPass.SOLID) {
         float alpha = this.intensity(c);
         if(alpha <= 0.003F) {
            return;
         }

         this.layout(c);
         if(this.count == 0) {
            return;
         }

         this.drawBodies(c);
         this.drawFoil(c);
         this.drawArt(c);
      } else if(pass == FxPass.GLOW) {
         this.layout(c);
         if(this.puff > 0.0D) {
            this.drawPuff(c);
         }
      }

   }

   private void layout(EffectContext c) {
      double s = (double)this.size(c);
      double t = c.time;
      double spd = (double)this.speed();
      int n = this.cards.getInt();
      TarotEffect.Arrangement arr = (TarotEffect.Arrangement)this.arrangement.get();
      boolean crown = arr == TarotEffect.Arrangement.CROWN;
      double scale = crown?0.55D:1.0D;
      double cardHalf = 0.15D * s * scale;
      double radius = crown?(0.3D + 0.03D * (double)n) * s:0.8D * (0.8D + 0.2D * s);
      double cy = crown?c.y + (double)c.nameTop + 0.1D + cardHalf * 1.5D + 0.05D:c.y + (double)c.height * 0.64D;
      this.deckX = c.x;
      this.deckY = c.y + (double)c.nameTop + 0.32D;
      this.deckZ = c.z;
      float out = c.outro;
      float fan = Easing.outCubic(Easing.clamp01(t / 0.3D));
      float gather = Easing.inOutCubic(Easing.clamp01(out / 0.55F));
      float vanish = Easing.smoothstep(0.55F, 0.85F, out);
      this.puff = out > 0.75F?(double)Easing.clamp01((out - 0.75F) / 0.25F):0.0D;
      double deckSpin = (double)Easing.inCubic(vanish) * 3.141592653589793D * 6.0D;
      double turnRate = Math.toRadians(arr == TarotEffect.Arrangement.ARC?8.0D:(arr == TarotEffect.Arrangement.ORBIT?12.0D:10.0D)) * spd;
      double base = t * turnRate;
      double spread = n <= 3?Math.toRadians(48.0D):(n == 4?Math.toRadians(40.0D):Math.toRadians(32.0D));
      this.count = n;
      Cards.Symbols set = (Cards.Symbols)this.symbols.get();

      for(int k = 0; k < n; ++k) {
         double a = arr == TarotEffect.Arrangement.ARC?base + ((double)k - (double)(n - 1) / 2.0D) * spread:base + 6.283185307179586D * (double)k / (double)n;
         double ca = Math.cos(a);
         double sa = Math.sin(a);
         double bob = 0.035D * s * Math.sin(t * 1.6D * spd + (double)k * 1.9D);
         double x = c.x + ca * radius;
         double y = cy + bob;
         double z = c.z + sa * radius;
         double lean = crown?Math.toRadians(18.0D):Math.toRadians(5.0D);
         double outx = ca * Math.cos(lean);
         double outy = -Math.sin(lean);
         double outz = sa * Math.cos(lean);
         double rgx = -sa;
         double rgy = 0.0D;
         double upx = outy * ca - outz * rgy;
         double upy = outz * rgx - outx * ca;
         double upz = outx * rgy - outy * rgx;
         if(upy < 0.0D) {
            upx = -upx;
            upy = -upy;
            upz = -upz;
         }

         double roll = Math.toRadians(4.0D) * Math.sin(t * 0.9D * spd + (double)k * 2.3D);
         double cr = Math.cos(roll);
         double sr = Math.sin(roll);
         double r1x = rgx * cr + upx * sr;
         double r1y = rgy * cr + upy * sr;
         double r1z = ca * cr + upz * sr;
         double u1x = upx * cr - rgx * sr;
         double u1y = upy * cr - rgy * sr;
         double u1z = upz * cr - ca * sr;
         double turn = 0.0D;
         double introFlip = (double)Easing.inOutCubic(Easing.clamp01((t - 0.2D - 0.07D * (double)k) / 0.22D));
         turn = turn + 3.141592653589793D * (1.0D - introFlip);
         int flips = 0;
         if(this.flip.get() && t > 1.0D) {
            double every = (double)this.flipEvery.getFloat() / spd;
            long started = (long)Math.floor(t / every);
            long passed = (long)Math.floor((t - 0.35D) / every);
            if(passed >= 1L && passed - 1L >= (long)k) {
               flips = (int)((passed - 1L - (long)k) / (long)n + 1L);
            }

            double f;
            if(started >= 1L && (started - 1L) % (long)n == (long)k && (f = (t - (double)started * every) / 0.7D) < 1.0D) {
               turn += 6.283185307179586D * (double)Easing.inOutCubic((float)f);
            }
         }

         double var127;
         double ct = Math.cos(var127 = turn + 3.141592653589793D * (double)gather);
         double st = Math.sin(var127);
         double fx = r1x * ct + outx * st;
         double fy = r1y * ct + outy * st;
         double fz = r1z * ct + outz * st;
         double gx = outx * ct - r1x * st;
         double gy = outy * ct - r1y * st;
         double gz = outz * ct - r1z * st;
         double from = 1.0D - (double)fan * (1.0D - (double)gather);
         double qx = x + (this.deckX - x) * from;
         double qy = y + (this.deckY - y) * from;
         double qz = z + (this.deckZ - z) * from;
         if(vanish > 0.0F) {
            double cs = Math.cos(deckSpin);
            double ss = Math.sin(deckSpin);
            double tx2 = fx * cs - gx * ss;
            double tz2 = fz * cs - gz * ss;
            double gx2 = gx * cs + fx * ss;
            double gz2 = gz * cs + fz * ss;
            fx = tx2;
            fz = tz2;
            gx = gx2;
            gz = gz2;
         }

         Cards.Card card = this.cardsNow[k];
         double var132;
         card.x = var132 = qx + gx * 0.004D * (double)k * from;
         card.y = qy;
         double var133;
         card.z = var133 = qz + gz * 0.004D * (double)k * from;
         card.rx = fx;
         card.ry = fy;
         card.rz = fz;
         card.ux = u1x;
         card.uy = u1y;
         card.uz = u1z;
         card.nx = gx;
         card.ny = gy;
         card.nz = gz;
         card.half = cardHalf * (0.7D + 0.3D * (double)fan) * (1.0D - 0.3D * (double)gather) * (1.0D - (double)vanish);
         this.cardAlpha[k] = c.keep.solid(var132, qy, var133);
         int[] symbolsOf = set.set;
         int step = symbolsOf.length > n?n:1;
         card.symbol = symbolsOf[(k + flips * step) % symbolsOf.length];
      }

   }

   private void drawBodies(EffectContext c) {
      float alpha = this.intensity(c);
      MeshBatch.begin(Material.CUSTOM, Finish.SATIN, c.light, 0.14F, -1, true, true);
      int body = this.color(c, 0.5F);

      for(int k = 0; k < this.count; ++k) {
         Cards.body(this.cardsNow[k], withAlpha(body, alpha * this.cardAlpha[k]));
      }

      MeshBatch.end();
   }

   private void drawFoil(EffectContext c) {
      float alpha = this.intensity(c);
      Cards.Foil f = (Cards.Foil)this.foil.get();
      boolean custom = f == Cards.Foil.CUSTOM;
      MeshBatch.begin(f.material, Finish.METALLIC, c.light, 0.14F, -1, true, true);
      int tint = custom?this.foilTint.getColor(0.5F):-1;
      boolean shine = this.glint.get();

      for(int k = 0; k < this.count; ++k) {
         double sweep = (c.time * 0.45D + (double)k * 0.37D) % 1.8D - 0.4D;
         Cards.foil(this.cardsNow[k], withAlpha(tint, alpha * this.cardAlpha[k]), sweep, shine);
      }

      MeshBatch.end();
   }

   private void drawArt(EffectContext c) {
      float alpha = this.intensity(c);
      int ink = this.symbolColor.getColor(0.5F);
      ink = ColorUtil.shade(ink, c.light);

      for(int k = 0; k < this.count; ++k) {
         this.cardsNow[k].alpha = alpha * this.cardAlpha[k];
      }

      RenderState.decal(true);

      try {
         Cards.art(this.cardsNow, this.count, ((Cards.Design)this.design.get()).ordinal(), ink, 0.9F);
      } finally {
         RenderState.decal(false);
      }

   }

   private void drawPuff(EffectContext c) {
      float a = this.intensity.getFloat() / 100.0F * (float)(1.0D - this.puff) * Easing.smoothstep(0.0F, 0.15F, (float)this.puff * 4.0F);
      int light = ColorUtil.lerp(this.symbolColor.getColor(0.5F), this.foil.get() == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):-6488, 0.5F);
      float size = (float)(0.2D + 0.5D * this.puff) * this.size(c);
      Vfx.sprite(Textures.GLOW, this.deckX, this.deckY, this.deckZ, (double)size, 0.0F, alpha(light, 0.7F * a));
      Vfx.sprite(Textures.SPARK, this.deckX, this.deckY, this.deckZ, (double)(size * 0.4F), 0.0F, core(light, a));

      for(int i = 0; i < 6; ++i) {
         double ang = (double)i * 6.283185307179586D / 6.0D + 0.3D;
         double r = 0.08D + 0.35D * this.puff;
         Vfx.sprite(Textures.SPARK, this.deckX + Math.cos(ang) * r, this.deckY + Math.sin(ang * 2.0D) * 0.05D + 0.1D * this.puff, this.deckZ + Math.sin(ang) * r, 0.05999999865889549D, 0.0F, core(light, a * 0.8F));
      }

   }

   private static int withAlpha(int color, float a) {
      return Cards.withAlpha(color, a);
   }

   public String stateForTest() {
      StringBuilder sb = (new StringBuilder("art ")).append(CardArt.stateForTest()).append(", symbols");

      for(int k = 0; k < this.count; ++k) {
         sb.append(' ').append(this.cardsNow[k].symbol);
      }

      return sb.toString();
   }

   public static void paintArtForTest() {
      CardArt.paintAllForTest();
   }

   public static enum Arrangement implements Displayable {
      ARC("Arc", "An arc round the chest"),
      ORBIT("Orbit", "All the way round"),
      CROWN("Crown above head", "A small ring above the head");

      private final String displayName;
      private final String description;

      private Arrangement(String displayName, String description) {
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
