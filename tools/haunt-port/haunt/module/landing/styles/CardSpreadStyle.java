package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.kit.CardArt;
import mix.client.mixui.integration.haunt.fx.kit.Cards;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
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

public final class CardSpreadStyle extends LandingStyleBase {
   private static final float SLID = 0.3F;
   private static final float FLIPPED = 0.8F;
   private static final float HELD = 1.1F;
   private static final float END = 1.7F;
   private static final float JUMP_OUT = 0.25F;
   private static final float JUMP_IN = 0.6F;
   private static final int MAX_CARDS = 16;
   private static final double HALF = 0.17D;
   private static final double RADIUS = 1.15D;
   private static final float RIM = 0.14F;
   private static final float DUST_WIND = 0.3F;
   public final ColorSetting symbolColor;
   public final ModeSetting<Cards.Foil> foil;
   public final ColorSetting foilTint;
   public final SliderSetting cards;
   public final ModeSetting<CardSpreadStyle.Spread> spread;
   public final ModeSetting<Cards.Design> design;
   public final ModeSetting<Cards.Symbols> symbols;
   public final BooleanSetting flip;
   private final Cards.Card[] deck = new Cards.Card[16];

   public CardSpreadStyle(LandingImpactModule module) {
      super(module, LandingStyle.CARD_SPREAD, 1.7F, 2.2F, new ColorSetting("cardspread.color", "Card", "The cards\' deep background.", ColorSetting.Mode.STATIC, -12903590, -14996902, 1.0D));
      this.signature(this.color, -12903590);
      this.symbolColor = (ColorSetting)this.setting((new ColorSetting("cardspread.symbols", "Symbols", "The line art on the faces and the backs.", ColorSetting.Mode.STATIC, -1185802, -1, 1.0D)).secondary());
      this.signature(this.symbolColor, -1185802);
      this.foil = (ModeSetting)this.setting((new ModeSetting("cardspread.material", "Foil", "The borders\' metal leaf: Gold, Silver, Rose gold, or Custom (the Foil Tint).", Cards.Foil.class, Cards.Foil.GOLD)).style(ModeSetting.Style.CHIPS).keywords(new String[]{"foil", "gold", "silver", "rose gold", "material", "metal"}));
      this.foilTint = (ColorSetting)this.setting((new ColorSetting("cardspread.foilTint", "Foil Tint", "The foil\'s color with Custom.", ColorSetting.Mode.STATIC, -1851025, -597080, 1.0D)).secondary().visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return CardSpreadStyle.this.foil.get() == Cards.Foil.CUSTOM;
         }
      }));
      this.signature(this.foilTint, -1851025);
      this.cards = this.look((SliderSetting)this.setting(new SliderSetting("cardspread.cards", "Cards", "How many cards are dealt.", 4.0D, 12.0D, 8.0D, 1.0D)));
      this.spread = (ModeSetting)this.setting((new ModeSetting("cardspread.spread", "Spread", "How the cards are laid out.", CardSpreadStyle.Spread.class, CardSpreadStyle.Spread.CIRCLE)).style(ModeSetting.Style.CHIPS));
      this.design = (ModeSetting)this.setting((new ModeSetting("cardspread.design", "Card Design", "The frames on the faces and the pattern on the backs.", Cards.Design.class, Cards.Design.CELESTIAL)).style(ModeSetting.Style.CHIPS));
      this.symbols = (ModeSetting)this.setting((new ModeSetting("cardspread.symbolSet", "Symbols", "Which symbols the faces show.", Cards.Symbols.class, Cards.Symbols.SKY)).style(ModeSetting.Style.CHIPS));
      this.flip = (BooleanSetting)this.setting(new BooleanSetting("cardspread.flip", "Flip", "The cards turn face up in a wave round the spread.", true));

      for(int k = 0; k < 16; ++k) {
         this.deck[k] = new Cards.Card();
      }

   }

   public int passes(LandingFx fx) {
      return FxPass.SOLID.bit() | (fx.lod?0:FxPass.GLOW.bit());
   }

   public void start(LandingFx fx) {
      CardSpreadStyle.State s = new CardSpreadStyle.State();
      s.jump = fx.land.jump;
      s.half = 0.17D * Math.sqrt((double)fx.size) * (s.jump?0.8D:1.0D);
      int n = this.cards.getInt() + (s.jump?0:Math.round(Math.max(0.0F, fx.land.fallScale - 1.0F) * 4.0F));
      if(fx.lod) {
         n = Math.max(4, n / 2);
      }

      s.n = Math.max(2, Math.min(16, n));
      double r = 1.15D * (double)fx.size * (s.jump?0.55D:1.0D);
      double yaw = Math.toRadians((double)fx.land.yaw);
      double fwdX = -Math.sin(yaw);
      double fwdZ = Math.cos(yaw);
      double facing = Math.atan2(fwdZ, fwdX);
      double turn = (double)fx.rand(1) * 6.283185307179586D;
      int[] set = ((Cards.Symbols)this.symbols.get()).set;
      CardSpreadStyle.Spread sp = (CardSpreadStyle.Spread)this.spread.get();

      for(int k = 0; k < s.n; ++k) {
         double dist;
         double ang;
         if(sp == CardSpreadStyle.Spread.ARC) {
            ang = facing - 1.5707963267948966D + 3.141592653589793D * ((double)k + 0.5D) / (double)s.n;
            dist = r;
         } else if(sp == CardSpreadStyle.Spread.CROSS) {
            int arm = k % 4;
            int step = k / 4;
            int perArm = (s.n + 3) / 4;
            ang = facing + (double)arm * 3.141592653589793D / 2.0D;
            dist = r * (0.55D + 0.75D * (double)step / (double)Math.max(1, perArm - 1 + (perArm == 1?1:0)));
         } else {
            ang = turn + 6.283185307179586D * (double)k / (double)s.n;
            dist = r;
         }

         s.ax[k] = Math.cos(ang);
         s.az[k] = Math.sin(ang);
         s.px[k] = s.ax[k] * dist;
         s.pz[k] = s.az[k] * dist;
         s.delay[k] = (s.jump?0.05F:0.09F) * (float)k / (float)Math.max(1, s.n - 1);
         s.spinIn[k] = fx.srand(20 + k) * 1.6F;
         s.symbol[k] = set[k % set.length];
      }

      fx.data = s;
   }

   private static float slid(LandingFx fx, CardSpreadStyle.State s, int k) {
      float t = fx.styleTime() - s.delay[k];
      float out = Easing.outCubic(Easing.clamp01(t / (s.jump?0.25F:0.21000001F)));
      if(s.jump) {
         out *= 1.0F - Easing.inCubic(Easing.clamp01((fx.styleTime() - 0.25F - 0.08F) / 0.27000004F));
      }

      return out;
   }

   private float flipped(LandingFx fx, CardSpreadStyle.State s, int k) {
      if(!s.jump && this.flip.get()) {
         float at = 0.3F + 0.34F * (float)k / (float)Math.max(1, s.n - 1);
         return Easing.inOutCubic(Easing.clamp01((fx.styleTime() - at) / 0.16F));
      } else {
         return 0.0F;
      }
   }

   private static float sunk(LandingFx fx, CardSpreadStyle.State s) {
      return s.jump?0.0F:Easing.clamp01((fx.styleTime() - 1.1F) / 0.6F);
   }

   private int layout(LandingFx fx, FxFrame f, CardSpreadStyle.State s, Camera cam) {
      float sink = sunk(fx, s);

      for(int k = 0; k < s.n; ++k) {
         Cards.Card c = this.deck[k];
         float out = slid(fx, s, k);
         if(out <= 0.0F) {
            c.half = 0.0D;
         } else {
            double x = fx.x() + s.px[k] * (double)out;
            double z = fx.z() + s.pz[k] * (double)out;
            double spin = (double)s.spinIn[k] * (1.0D - (double)out);
            double ux0 = s.ax[k] * Math.cos(spin) - s.az[k] * Math.sin(spin);
            double uz0;
            double sx = uz0 = s.ax[k] * Math.sin(spin) + s.az[k] * Math.cos(spin);
            double sz = -ux0;
            double phi = 3.141592653589793D * (double)this.flipped(fx, s, k);
            double cp = Math.cos(phi);
            double sp = Math.sin(phi);
            c.nx = sx * sp;
            c.ny = -cp;
            c.nz = sz * sp;
            c.ux = ux0;
            c.uy = 0.0D;
            c.uz = uz0;
            c.rx = c.uy * c.nz - c.uz * c.ny;
            c.ry = c.uz * c.nx - c.ux * c.nz;
            c.rz = c.ux * c.ny - c.uy * c.nx;
            double ground = Ground.y(fx.ground, x, z);
            boolean on = Ground.lastValid();
            double lift = s.half * 1.05D * sp + s.half * 0.05D;
            double down = (double)Easing.inCubic(sink) * s.half * 0.6D;
            c.x = f.rx(x);
            c.y = f.ry(ground + lift - down);
            c.z = f.rz(z);
            c.half = s.half * (0.55D + 0.45D * (double)out);
            c.alpha = fx.alpha() * (on?1.0F:0.0F) * (1.0F - Easing.smoothstep(0.35F, 1.0F, sink)) * NearCamera.fade(cam, c.x, c.y, c.z);
            c.symbol = s.symbol[k];
            c.uncut();
         }
      }

      return s.n;
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      CardSpreadStyle.State s = (CardSpreadStyle.State)fx.data;
      if(s != null) {
         Camera cam = Vfx.camera();
         int n = this.layout(fx, f, s, cam);
         if(pass == FxPass.GLOW) {
            this.glow(fx, f, s, n);
         } else {
            MeshBatch.begin(Material.CUSTOM, Finish.SATIN, fx.light, 0.14F, -1, true, true);
            int body = this.color.getColor(0.5F);

            for(int k = 0; k < n; ++k) {
               Cards.body(this.deck[k], Cards.withAlpha(body, this.deck[k].alpha));
            }

            MeshBatch.end();
            Cards.Foil fl = (Cards.Foil)this.foil.get();
            MeshBatch.begin(fl.material, Finish.METALLIC, fx.light, 0.14F, -1, true, true);
            int tint = fl == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):-1;

            for(int k = 0; k < n; ++k) {
               double sweep = (double)(fx.styleTime() - (0.3F + 0.34F * (float)k / (float)Math.max(1, s.n - 1))) * 6.0D - 0.4D;
               Cards.foil(this.deck[k], Cards.withAlpha(tint, this.deck[k].alpha), sweep, true);
            }

            MeshBatch.end();
            int ink = ColorUtil.shade(this.symbolColor.getColor(0.5F), Math.max(0.45F, fx.light)) | -16777216;
            RenderState.decal(true);
            RenderState.translucent(false);

            try {
               Cards.art(this.deck, n, ((Cards.Design)this.design.get()).ordinal(), ink, 0.9F);
            } finally {
               RenderState.decal(false);
               RenderState.translucent(true);
            }

            this.dust(fx, s);
         }
      }

   }

   private void dust(LandingFx fx, CardSpreadStyle.State s) {
      if(!s.dusted && !s.jump && !fx.lod && sunk(fx, s) > 0.0F) {
         s.dusted = true;
         int col = this.foil.get() == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):((Cards.Foil)this.foil.get()).material.swatch();
         int per = Math.max(2, fx.count(5.0F));

         for(int k = 0; k < s.n; ++k) {
            for(int i = 0; i < per; ++i) {
               double x = fx.x() + s.px[k] + ((double)fx.rand(300 + k * 13 + i) - 0.5D) * s.half * 2.0D;
               double z = fx.z() + s.pz[k] + ((double)fx.rand(400 + k * 13 + i) - 0.5D) * s.half * 2.0D;
               double y = Ground.y(fx.ground, x, z) + 0.05D;
               fx.particles.spawn(i % 3 == 0?5:1, x, y, z, (fx.rand(500 + k * 13 + i) - 0.5F) * 0.3F, 0.5F + fx.rand(600 + k * 13 + i) * 0.7F, (fx.rand(700 + k * 13 + i) - 0.5F) * 0.3F, fx.seconds(0.7F + fx.rand(800 + i) * 0.4F), (0.035F + fx.rand(900 + i) * 0.03F) * (float)Math.sqrt((double)fx.size)).drag(1.5F).color(col).alpha(fx.alpha()).end(0.01F).wind(0.3F);
            }
         }
      }

   }

   private void glow(LandingFx fx, FxFrame f, CardSpreadStyle.State s, int n) {
      int col = this.foil.get() == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):((Cards.Foil)this.foil.get()).material.swatch();

      for(int k = 0; k < n; ++k) {
         Cards.Card c = this.deck[k];
         float fl = this.flipped(fx, s, k);
         float k2 = (float)Math.sin(3.141592653589793D * (double)fl) * c.alpha;
         if(k2 > 0.01F && c.half > 1.0E-4D) {
            Vfx.sprite(Textures.GLOW, c.x, c.y + c.half * 0.3D, c.z, c.half * 3.5D, 0.0F, alpha(col, 0.35F * k2));
         }
      }

   }

   public String stateForTest(LandingFx fx) {
      CardSpreadStyle.State s = (CardSpreadStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         int up = 0;

         for(int k = 0; k < s.n; ++k) {
            up += this.flipped(fx, s, k) >= 1.0F?1:0;
         }

         String phase = s.jump?(t < 0.25F?"sliding out":(t < 0.6F?"sliding back in":"gone")):(t < 0.3F?"sliding out":(t < 0.8F?"the flip wave":(t < 1.1F?"holding":(t < 1.7F?"sinking into gold dust":"gone"))));
         return String.format(Locale.ROOT, "%s; %d cards (%s), %d face up, %s, %s, %s, %s; fall %.1f (x%.2f)%s; %s, light %.2f", new Object[]{phase, Integer.valueOf(s.n), ((CardSpreadStyle.Spread)this.spread.get()).displayName(), Integer.valueOf(up), ((Cards.Design)this.design.get()).displayName(), ((Cards.Symbols)this.symbols.get()).displayName(), ((Cards.Foil)this.foil.get()).displayName(), CardArt.stateForTest(), Float.valueOf(fx.land.fallHeight), Float.valueOf(fx.land.fallScale), s.jump?", a jump":"", fx.lod?"far":"near", Float.valueOf(fx.light)});
      }
   }

   public static enum Spread implements Displayable {
      CIRCLE("Circle", "All round the feet"),
      ARC("Arc", "A half circle in front"),
      CROSS("Cross", "Four arms");

      private final String displayName;
      private final String description;

      private Spread(String displayName, String description) {
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
      int n;
      boolean jump;
      double half;
      final double[] px;
      final double[] pz;
      final double[] ax;
      final double[] az;
      final float[] delay;
      final float[] spinIn;
      final int[] symbol;
      boolean dusted;

      private State() {
         this.px = new double[16];
         this.pz = new double[16];
         this.ax = new double[16];
         this.az = new double[16];
         this.delay = new float[16];
         this.spinIn = new float[16];
         this.symbol = new int[16];
      }
   }
}
