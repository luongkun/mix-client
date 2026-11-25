package mix.client.mixui.integration.haunt.module.landing.styles;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.module.landing.LandingStyleBase;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class FeatherFallStyle extends LandingStyleBase {
   public final ModeSetting<FeatherFallStyle.Piece> piece = (ModeSetting)this.setting(new ModeSetting("feathers.piece", "Pieces", "What flutters down.", FeatherFallStyle.Piece.class, FeatherFallStyle.Piece.FEATHERS));
   public final SliderSetting count = (SliderSetting)this.setting(new SliderSetting("feathers.count", "Count", "How many pieces.", 6.0D, 20.0D, 11.0D, 1.0D));
   private final Rng rng = new Rng();

   public FeatherFallStyle(LandingImpactModule module) {
      super(module, LandingStyle.FEATHER_FALL, 1.8F, 2.0F, new ColorSetting("feathers.color", "Color", "Color of the pieces (gradient: a random mix per piece).", -792065));
   }

   private int texture() {
      switch((FeatherFallStyle.Piece)this.piece.get()) {
      case PETALS:
         return Textures.PETAL;
      case LEAVES:
         return Textures.LEAF;
      case SNOWFLAKES:
         return Textures.SNOWFLAKE;
      default:
         return Textures.FEATHER;
      }
   }

   public void start(LandingFx fx) {
      this.rng.seed(fx.seed);
      int n = fx.count(this.count.getFloat() * (fx.lod?0.5F:1.0F));
      int tex = this.texture();

      for(int i = 0; i < n; ++i) {
         double ang = 6.283185307179586D * ((double)i + (double)this.rng.next() * 0.8D) / (double)n;
         float out = this.rng.range(1.4F, 2.8F) * (float)Math.sqrt((double)fx.size);
         float s = this.rng.range(0.34F, 0.46F) * (float)Math.sqrt((double)fx.size);
         double ax = -Math.sin(ang);
         double az = Math.cos(ang);
         fx.particles.spawn(9, fx.x() + Math.cos(ang) * 0.2D, fx.y() + (double)this.rng.range(0.15F, 0.45F), fx.z() + Math.sin(ang) * 0.2D, (float)Math.cos(ang) * out, this.rng.range(2.2F, 3.6F), (float)Math.sin(ang) * out, fx.seconds(this.rng.range(1.6F, 1.9F)), s).gravity(2.6F).drag(2.9F).axis((float)ax, 0.0F, (float)az).spin(this.rng.next() * 6.28F, this.rng.signed() * 2.0F).sway(this.rng.range(0.35F, 0.6F), this.rng.next() * 6.28F).texture(tex).color(this.color, this.rng.next()).alpha(0.95F * fx.opacity).fade(0.02F, 0.72F).flags(4).ground(fx.y() + 0.02D, 0, 0.0F);
      }

   }

   public void render(LandingFx fx, FxFrame f) {
   }

   public static enum Piece implements Displayable {
      FEATHERS("Feathers", "Soft feathers"),
      PETALS("Petals", "Blossom petals"),
      LEAVES("Leaves", "Leaves"),
      SNOWFLAKES("Snowflakes", "Snowflakes");

      private final String displayName;
      private final String description;

      private Piece(String displayName, String description) {
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
