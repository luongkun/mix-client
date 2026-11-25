package mix.client.mixui.integration.haunt.module.landing.styles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Ground;
import mix.client.mixui.integration.haunt.fx.kit.Comic;
import mix.client.mixui.integration.haunt.fx.kit.Halftone;
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
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.EnumSetSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import mix.client.mixui.integration.haunt.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;

public final class ThudStyle extends LandingStyleBase {
   private static final float SPLAT = 0.1F;
   private static final float WORD = 0.25F;
   private static final float HOLD = 0.65F;
   private static final float END = 0.9F;
   private static final int MAX_CUSTOM = 8;
   private static final int MAX_WORD = 8;
   private static final int LINES = 9;
   private static final int LINE_POINTS = 3;
   private static final double SPLAT_W = 1.7D;
   private static final double WORD_H = 0.42D;
   private static final double BESIDE = 0.95D;
   private static final double AHEAD = 2.2D;
   private static final String BIG_WORD = "BOOM!";
   private static final String JUMP_WORD = "HOP!";
   public final ColorSetting lettering;
   public final ColorSetting outline;
   public final ModeSetting<ThudStyle.Words> words;
   public final EnumSetSetting<ThudStyle.Word> wordList;
   public final TextSetting custom;
   public final BooleanSetting wordInFirstPerson;
   public final BooleanSetting halftone;
   public final BooleanSetting puffs;
   public final SliderSetting size;
   private final List<String> pool = new ArrayList();
   private int poolVersion = -1;
   private String poolCustom = null;
   private final float[] uv = new float[4];
   private final double[] ws = new double[2];

   public ThudStyle(LandingImpactModule module) {
      super(module, LandingStyle.THUD, 0.9F, 2.0F, new ColorSetting("thud.color", "Burst", "The splat\'s fill (its dots a darker shade).", ColorSetting.Mode.STATIC, -11713, -30147, 1.0D));
      this.signature(this.color, -11713);
      this.lettering = (ColorSetting)this.setting((new ColorSetting("thud.lettering", "Lettering", "The word\'s letters.", ColorSetting.Mode.STATIC, -1689274, -1, 1.0D)).secondary());
      this.signature(this.lettering, -1689274);
      this.outline = (ColorSetting)this.setting((new ColorSetting("thud.outline", "Outline", "The outlines and the word\'s shadow.", ColorSetting.Mode.STATIC, -15658735, -14017974, 1.0D)).secondary());
      this.signature(this.outline, -15658735);
      this.words = (ModeSetting)this.setting(new ModeSetting("thud.words", "Words", "A word from the list each landing, or always the list\'s first.", ThudStyle.Words.class, ThudStyle.Words.RANDOM));
      this.wordList = (EnumSetSetting)this.setting(new EnumSetSetting("thud.wordList", "Word List", "The words it says.", ThudStyle.Word.class, false, new ThudStyle.Word[]{ThudStyle.Word.THUD, ThudStyle.Word.WHUMP}));
      this.custom = (TextSetting)this.setting(new TextSetting("thud.custom", "Custom Words", "Your own words for the list, separated by commas, up to 8 characters each.", "", 72));
      this.wordInFirstPerson = (BooleanSetting)this.setting(new BooleanSetting("thud.wordFirstPerson", "Word In First Person", "Show the word for your own landings while you look through your own eyes.", false));
      this.halftone = (BooleanSetting)this.setting(new BooleanSetting("thud.halftone", "Halftone", "Printed halftone dots in the splat\'s shade.", true));
      this.puffs = (BooleanSetting)this.setting(new BooleanSetting("thud.puffs", "Puffs", "Two comic puffs pop out to the sides.", true));
      this.size = this.look((SliderSetting)this.setting((new SliderSetting("thud.size", "Size", "How big the splat and the word are.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
   }

   public int passes(LandingFx fx) {
      return FxPass.TRANSLUCENT.bit();
   }

   private List<String> pool() {
      String c = this.custom.get();
      if(this.poolVersion != this.wordList.version() || !c.equals(this.poolCustom)) {
         this.poolVersion = this.wordList.version();
         this.poolCustom = c;
         this.pool.clear();

         for(ThudStyle.Word w : ThudStyle.Word.values()) {
            if(this.wordList.includes(w)) {
               this.pool.add(w.text);
            }
         }

         for(String part : c.split(",")) {
            String t = part.trim();
            if(!t.isEmpty() && this.pool.size() < ThudStyle.Word.values().length + 8) {
               this.pool.add(t.length() > 8?t.substring(0, 8):t);
            }
         }

         if(this.pool.isEmpty()) {
            this.pool.add(ThudStyle.Word.THUD.text);
         }

         for(String wx : this.pool) {
            Comic.word(wx);
         }
      }

      return this.pool;
   }

   public void start(LandingFx fx) {
      ThudStyle.State s = new ThudStyle.State();
      s.jump = fx.land.jump;
      s.scale = (double)(this.size.getFloat() / 100.0F * fx.size);
      s.turn = (double)fx.rand(1) * 6.283185307179586D;
      s.variant = (int)(fx.rand(2) * 4.0F);
      s.seed = fx.seed;
      if(s.jump) {
         s.word = "HOP!";
      } else {
         List<String> list = this.pool();
         boolean big = this.words.get() == ThudStyle.Words.RANDOM && fx.land.fallHeight >= 5.0F;
         s.word = big?"BOOM!":(this.words.get() == ThudStyle.Words.ONE?(String)list.get(0):(String)list.get((int)(fx.rand(3) * (float)list.size()) % list.size()));
      }

      Comic.word(s.word);
      Minecraft mc = Minecraft.getMinecraft();
      boolean firstPerson = fx.land.self && mc.gameSettings.thirdPersonView == 0 && !fx.preview;
      s.showWord = !firstPerson || this.wordInFirstPerson.get();
      Entity eye = mc.getRenderViewEntity();
      if(firstPerson && s.showWord && eye != null) {
         Vec3 look = eye.getLook(1.0F);
         double flat = Math.sqrt(look.xCoord * look.xCoord + look.zCoord * look.zCoord);
         double rx = flat > 0.001D?-look.zCoord / flat:1.0D;
         double rz = flat > 0.001D?look.xCoord / flat:0.0D;
         s.ahead = true;
         s.wx = fx.x() + look.xCoord * 2.2D + rx * 0.45D;
         s.wz = fx.z() + look.zCoord * 2.2D + rz * 0.45D;
         s.wy = Math.max(fx.y() + 0.65D, fx.y() + (double)eye.getEyeHeight() + look.yCoord * 2.2D + 0.1D);
      }

      fx.data = s;
   }

   private static double splat(LandingFx fx) {
      float t = fx.styleTime();
      return (double)Easing.outBack(Easing.clamp01(t / 0.1F), 1.6F) * (1.0D - (double)Easing.inCubic(Easing.clamp01((t - 0.65F) / 0.25F)));
   }

   private static void wordScale(LandingFx fx, double[] out) {
      float t = fx.styleTime();
      if(t < 0.1F) {
         out[1] = 0.0D;
         out[0] = 0.0D;
      } else {
         float in = Easing.clamp01((t - 0.1F) / 0.15F);
         if(t < 0.25F) {
            double e = (double)Easing.outBack(in, 2.4F);
            out[0] = e * (1.0D - 0.25D * Math.sin(3.141592653589793D * (double)in));
            out[1] = e * (1.0D + 0.3D * Math.sin(3.141592653589793D * (double)in));
         } else if(t < 0.65F) {
            double bob = 0.04D * Math.sin((double)(t - 0.25F) * 14.0D) * Math.exp((double)(-(t - 0.25F)) * 4.0D);
            out[0] = 1.0D - bob;
            out[1] = 1.0D + bob;
         } else {
            float o = Easing.clamp01((t - 0.65F) / 0.25F);
            out[0] = (1.0D + 0.35D * (double)Easing.outCubic(o)) * (1.0D - (double)Easing.inCubic(o));
            out[1] = (1.0D - (double)Easing.inCubic(o)) * (1.0D - 0.6D * (double)o);
         }
      }

   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
      ThudStyle.State s = (ThudStyle.State)fx.data;
      if(s != null) {
         Camera cam = Vfx.camera();
         float alpha = fx.alpha();
         int burst = this.color.getColor(0.5F) | -16777216;
         int line = this.outline.getColor(0.5F) | -16777216;
         int dots = ColorUtil.shade(burst, 0.62F) | -16777216;
         double k = splat(fx);
         RenderState.translucent(false);
         if(!s.jump && k > 0.01D) {
            RenderState.decal(true);

            try {
               this.splatDecal(fx, f, s, k, burst, dots, line, alpha);
               this.lines(fx, f, s, k, line, alpha);
            } finally {
               RenderState.decal(false);
            }
         }

         if(this.puffs.get() && k > 0.01D && (!fx.lod || s.jump)) {
            this.puffPair(fx, f, s, cam, k, line, alpha);
         }

         if(s.showWord) {
            this.word(fx, f, s, cam, line, alpha);
         }

         RenderState.translucent(true);
      }

   }

   private void splatDecal(LandingFx fx, FxFrame f, ThudStyle.State s, double k, int fill, int dots, int line, float alpha) {
      Comic.balloonCell(0, s.variant, this.uv);
      double half = 0.85D * s.scale * k;
      int a = ColorUtil.mulAlpha(fill, alpha * 0.95F);
      if(Halftone.usable()) {
         Vfx.flush();
         float period = Math.max(3.0F, (float)Minecraft.getMinecraft().displayHeight / 180.0F);
         Halftone.begin(period, this.halftone.get()?dots:0, 1.0F);

         try {
            Ground.decal(f, fx.ground, Comic.balloonShade(), this.uv[0], this.uv[1], this.uv[2], this.uv[3], fx.x(), fx.z(), half, s.turn, a, 6);
            Vfx.flush();
         } finally {
            Halftone.end();
         }
      } else {
         Ground.decal(f, fx.ground, this.halftone.get()?Comic.balloonFill():Comic.balloonShade(), this.uv[0], this.uv[1], this.uv[2], this.uv[3], fx.x(), fx.z(), half, s.turn, a, 6);
      }

      Ground.decal(f, fx.ground, Comic.balloonOutline(), this.uv[0], this.uv[1], this.uv[2], this.uv[3], fx.x(), fx.z(), half, s.turn, ColorUtil.mulAlpha(line, alpha), 6);
   }

   private void lines(LandingFx fx, FxFrame f, ThudStyle.State s, double k, int line, float alpha) {
      double inner = 0.935D * s.scale * Math.min(1.0D, k);
      double outer = 1.7D * (0.8D + 0.55D * k) * s.scale;
      int col = ColorUtil.mulAlpha(line, alpha);

      for(int i = 0; i < 9; ++i) {
         double a = s.turn + 6.283185307179586D * ((double)i + 0.5D * (double)fx.rand(50 + i)) / 9.0D;
         double ca = Math.cos(a);
         double sa = Math.sin(a);
         double r0 = inner * (0.95D + 0.2D * (double)fx.rand(70 + i));
         double r1 = outer * (0.8D + 0.3D * (double)fx.rand(90 + i));
         double w = 0.07D * s.scale * (0.7D + 0.6D * (double)fx.rand(110 + i));
         int n = Ground.linePoints(f, fx.ground, 0, fx.x() + ca * r0, fx.z() + sa * r0, fx.x() + ca * r1, fx.z() + sa * r1, w, col, col, 2);

         for(int j = 0; j < n; ++j) {
            Vfx.W[j] = (float)(w * (j == 0?0.05D:(double)j / (double)(n - 1)));
         }

         Vfx.flatRibbon(Textures.LINE_CORE, 0, n);
      }

   }

   private void puffPair(LandingFx fx, FxFrame f, ThudStyle.State s, Camera cam, double k, int line, float alpha) {
      double side = (s.jump?0.5D:0.95D) * s.scale * Math.min(1.0D, k);
      double w = (s.jump?0.5D:0.75D) * s.scale * k;
      double h = w * 0.62D;

      for(int d = -1; d <= 1; d += 2) {
         double x = fx.x() + cam.rightX * side * (double)d;
         double z = fx.z() + cam.rightZ * side * (double)d;
         double y = Ground.y(fx.ground, x, z) + h * 0.42D;
         double rx = f.rx(x);
         double ry = f.ry(y);
         double rz = f.rz(z);
         float a = alpha * NearCamera.fade(cam, rx, ry, rz);
         Comic.puff(s.seed + d, rx, ry, rz, cam.rightX, cam.rightY, cam.rightZ, cam.upX, cam.upY, cam.upZ, w, h, (double)d * 0.15D, ColorUtil.mulAlpha(-1, a), ColorUtil.mulAlpha(line, a), 1.0F);
      }

   }

   private void word(LandingFx fx, FxFrame f, ThudStyle.State s, Camera cam, int line, float alpha) {
      Comic.Word w = Comic.word(s.word);
      wordScale(fx, this.ws);
      if(w.ready() && this.ws[0] > 0.01D && this.ws[1] > 0.01D) {
         double h = 0.42D * s.scale * (s.jump?0.6D:1.0D);
         double x = fx.x() + cam.rightX * 0.95D * s.scale;
         double z = fx.z() + cam.rightZ * 0.95D * s.scale;
         double y = fx.y() + (s.jump?0.7D:1.35D) * Math.sqrt(s.scale);
         if(s.ahead) {
            x = s.wx;
            y = s.wy;
            z = s.wz;
         }

         double rz;
         double ry;
         double rx;
         float a;
         if((a = alpha * NearCamera.fade(cam, rx = f.rx(x), ry = f.ry(y), rz = f.rz(z))) > 0.003F) {
            int letters = ColorUtil.mulAlpha(this.lettering.getColor(0.5F) | -16777216, a);
            double tilt = -0.12D;
            double sx = this.ws[0];
            double sy = this.ws[1];
            Comic.lettering(w, rx, ry, rz, cam.rightX * sx, cam.rightY * sx, cam.rightZ * sx, cam.upX * sy, cam.upY * sy, cam.upZ * sy, h, tilt, letters, ColorUtil.mulAlpha(line, a), 1.2F);
         }
      }

   }

   public String stateForTest(LandingFx fx) {
      ThudStyle.State s = (ThudStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.styleTime();
         String phase = t < 0.1F?"splat":(t < 0.25F?"the word popping up":(t < 0.65F?"holding":(t < 0.9F?"popping away":"gone")));
         Comic.Word w = Comic.word(s.word);
         Object[] objectArray = new Object[13];
         objectArray[0] = phase;
         objectArray[1] = s.word;
         Object object = objectArray[2] = w.ready()?"ready":"painting";
         objectArray[3] = !s.showWord?", hidden in first person":(s.ahead?", ahead of you":"");
         objectArray[4] = Double.valueOf(splat(fx));
         objectArray[5] = this.puffs.get()?(fx.lod && !s.jump?"on (not far away)":"on"):"off";
         objectArray[6] = this.halftone.get()?"on":"off";
         objectArray[7] = Halftone.usable()?"shader":"printed";
         objectArray[8] = Float.valueOf(fx.land.fallHeight);
         objectArray[9] = Float.valueOf(fx.land.fallScale);
         objectArray[10] = s.jump?", a jump":"";
         objectArray[11] = fx.lod?"far":"near";
         objectArray[12] = Float.valueOf(fx.light);
         return String.format(Locale.ROOT, "%s; \"%s\" (%s%s), splat %.2f, puffs %s, halftone %s (%s); fall %.1f (x%.2f)%s; %s, light %.2f", objectArray);
      }
   }

   private static final class State {
      String word;
      boolean jump;
      boolean showWord;
      double scale;
      double turn;
      int variant;
      int seed;
      boolean ahead;
      double wx;
      double wy;
      double wz;

      private State() {
      }
   }

   public static enum Word implements Displayable {
      THUD("THUD!"),
      WHUMP("WHUMP!");

      final String text;

      private Word(String text) {
         this.text = text;
      }

      public String displayName() {
         return this.text;
      }

      public String description() {
         return "Says \"" + this.text + "\"";
      }
   }

   public static enum Words implements Displayable {
      RANDOM("Random from the list", "A word from the list each landing"),
      ONE("One word", "Always the list\'s first word");

      private final String displayName;
      private final String description;

      private Words(String displayName, String description) {
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
