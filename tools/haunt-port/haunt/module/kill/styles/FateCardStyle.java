package mix.client.mixui.integration.haunt.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.kit.CardArt;
import mix.client.mixui.integration.haunt.fx.kit.Cards;
import mix.client.mixui.integration.haunt.fx.kit.Glitch;
import mix.client.mixui.integration.haunt.fx.look.NearCamera;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.kill.KillStyleBase;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.render.look.BodyLook;
import mix.client.mixui.integration.haunt.render.look.LineArt;
import mix.client.mixui.integration.haunt.render.look.Portrait;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.render.material.MeshBatch;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public final class FateCardStyle extends KillStyleBase {
   private static final float RISEN = 0.3F;
   private static final float DRAWN = 0.7F;
   private static final float TURNED = 1.3F;
   private static final float FLIPPED = 1.5F;
   private static final float EXIT = 0.5F;
   private static final float PICTURE_FROM = 0.55F;
   private static final float TITLE_FROM = 0.62F;
   private static final float TITLE_TO = 0.82F;
   private static final float GOLD_FROM = 1.02F;
   private static final float GOLD_TO = 1.27F;
   private static final double WIDTH = 1.6D;
   private static final double LIFT = 0.04D;
   private static final float CLEAR = 0.18F;
   private static final double GA = 0.6D;
   private static final double GB = 0.8D;
   private static final float RIM = 0.14F;
   private static final int EMBER = -15809;
   private static final int DECK = 6;
   public final ColorSetting lines;
   public final ModeSetting<Cards.Foil> foil;
   public final ColorSetting foilTint;
   public final ModeSetting<Cards.Design> design;
   public final ModeSetting<FateCardStyle.Title> title;
   public final ModeSetting<FateCardStyle.Picture> picture;
   public final ModeSetting<FateCardStyle.Exit> exit;
   public final SliderSetting cardSize;
   private final Glitch.Plane plane = new Glitch.Plane();
   private final float[] uv = new float[4];
   private final double[] top = new double[4];

   public FateCardStyle(KillEffectsModule module) {
      super(module, KillStyle.FATE_CARD, new ColorSetting("fatecard.color", "Card", "The card\'s deep background.", ColorSetting.Mode.STATIC, -12903590, -14996902, 1.0D));
      this.signature(this.color, -12903590);
      this.lines = (ColorSetting)this.setting((new ColorSetting("fatecard.lines", "Lines", "The portrait\'s line art, the title and the card\'s art.", ColorSetting.Mode.STATIC, -1185802, -1, 1.0D)).secondary());
      this.signature(this.lines, -1185802);
      this.foil = (ModeSetting)this.setting((new ModeSetting("fatecard.material", "Foil", "The border\'s metal leaf: Gold, Silver, Rose gold, or Custom (the Foil Tint).", Cards.Foil.class, Cards.Foil.GOLD)).style(ModeSetting.Style.CHIPS).keywords(new String[]{"foil", "gold", "silver", "rose gold", "material", "metal"}));
      this.foilTint = (ColorSetting)this.setting((new ColorSetting("fatecard.foilTint", "Foil Tint", "The foil\'s color with Custom.", ColorSetting.Mode.STATIC, -1851025, -597080, 1.0D)).secondary().visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return FateCardStyle.this.foil.get() == Cards.Foil.CUSTOM;
         }
      }));
      this.signature(this.foilTint, -1851025);
      this.design = (ModeSetting)this.setting((new ModeSetting("fatecard.design", "Card Design", "The frame on the face and the pattern on the back.", Cards.Design.class, Cards.Design.CELESTIAL)).style(ModeSetting.Style.CHIPS));
      this.title = (ModeSetting)this.setting((new ModeSetting("fatecard.title", "Title", "Along the bottom of the face: the victim\'s name, the kill\'s number in Roman numerals, or nothing.", FateCardStyle.Title.class, FateCardStyle.Title.NAME)).style(ModeSetting.Style.CHIPS));
      this.picture = (ModeSetting)this.setting((new ModeSetting("fatecard.portrait", "Portrait", "The body on the card: in fine line art, or in full color.", FateCardStyle.Picture.class, FateCardStyle.Picture.LINE_ART)).style(ModeSetting.Style.CHIPS));
      this.exit = (ModeSetting)this.setting((new ModeSetting("fatecard.exit", "Exit", "How the card goes: burning away from its edges, flying up and shrinking, or shuffled into a deck.", FateCardStyle.Exit.class, FateCardStyle.Exit.BURN)).style(ModeSetting.Style.CHIPS));
      this.cardSize = this.look((SliderSetting)this.setting((new SliderSetting("fatecard.size", "Card Size", "How big the card is.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%")));
   }

   public float seconds(KillFx fx) {
      return 2.0F;
   }

   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.GLOW.bit();
   }

   public void start(KillFx fx) {
      FateCardStyle.State s = new FateCardStyle.State();
      switch((FateCardStyle.Title)this.title.get()) {
      case NAME:
         s.title = fx.ctx.victimName == null?"":fx.ctx.victimName.trim();
         break;
      case NUMERAL:
         s.title = roman(this.module.getKillCount() + 1);
         break;
      default:
         s.title = "";
      }

      aim(s, -fx.ctx.dirX, -fx.ctx.dirZ);
      s.width = 1.6D * (double)this.cardSize.getFloat() / 100.0D * (double)fx.size;
      s.cx = fx.ctx.feetX + s.nX * 0.6D;
      s.cz = fx.ctx.feetZ + s.nZ * 0.6D;
      s.cy = fx.ctx.groundY + s.width * 0.75D + 0.04D;
      fx.data = s;
   }

   private static void aim(FateCardStyle.State s, double dx, double dz) {
      double l = Math.sqrt(dx * dx + dz * dz);
      if(l < 1.0E-4D) {
         if(s.nX == 0.0D && s.nZ == 0.0D) {
            s.nZ = 1.0D;
            s.rX = 1.0D;
         }
      } else {
         s.nX = dx / l;
         s.rX = s.nZ = dz / l;
         s.rZ = -s.nX;
      }

   }

   private static void place(KillFx fx, FxFrame f, FateCardStyle.State s, Camera cam) {
      s.placed = true;
      double dx = f.viewX + cam.posX - fx.ctx.feetX;
      double dz = f.viewZ + cam.posZ - fx.ctx.feetZ;
      aim(s, dx, dz);
      double dist = Math.max(0.4D, Math.min(0.75D, Math.sqrt(dx * dx + dz * dz) - 1.8D));
      s.cx = fx.ctx.feetX + s.nX * dist;
      s.cz = fx.ctx.feetZ + s.nZ * dist;
   }

   private static void capture(KillFx fx, FateCardStyle.State s) {
      s.tried = true;
      if(Portrait.available()) {
         long t0 = System.nanoTime();
         BodyProxy.Look look = new BodyProxy.Look();
         look.reset();
         look.armor = fx.look.armor;
         look.heldItem = fx.look.heldItem;
         look.cape = fx.look.cape;
         float yaw = (float)Math.toDegrees(Math.atan2(-s.nX, s.nZ));
         s.picture = Portrait.capture(fx.ctx.body, fx.rest, look, fx.ctx.feetX, fx.ctx.feetY, fx.ctx.feetZ, yaw);
         s.pictureMillis = (float)(System.nanoTime() - t0) / 1000000.0F;
         RenderState.translucent(true);
      }

   }

   public void update(KillFx fx, float dt) {
      FateCardStyle.State s = (FateCardStyle.State)fx.data;
      if(s != null && s.placed && !s.sparked && fx.t > 0.03F) {
         s.sparked = true;
         int col = this.foilColor();
         int n = fx.count(14.0F, false);

         for(int i = 0; i < n; ++i) {
            double u = ((double)fx.rng.next() - 0.5D) * s.width;
            fx.particles.spawn(1, s.cx + s.rX * u, fx.ctx.groundY + 0.03D, s.cz + s.rZ * u, (float)(s.nX * 0.4D) + fx.rng.signed() * 0.6F, 1.2F + fx.rng.next() * 1.6F, (float)(s.nZ * 0.4D) + fx.rng.signed() * 0.6F, 0.5F + fx.rng.next() * 0.4F, (0.035F + fx.rng.next() * 0.03F) * fx.size).gravity(6.0F).drag(1.8F).color(col).alpha(fx.alpha()).end(0.01F);
         }
      }

   }

   private int foilColor() {
      return this.foil.get() == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):((Cards.Foil)this.foil.get()).material.swatch();
   }

   private static float exitOf(KillFx fx) {
      return fx.phase(1.5F, 2.0F);
   }

   private static float goldOf(KillFx fx) {
      return fx.crit && fx.t >= 1.02F?fx.phase(1.02F, 1.27F):-1.0F;
   }

   private static double goldLine(double s, float gold) {
      return s * (-0.95D + 1.9D * (double)gold);
   }

   private void layout(KillFx fx, FxFrame f, FateCardStyle.State s, Camera cam) {
      Cards.Card c = s.card;
      double w = s.width;
      double half = w * 0.5D;
      float rise = Easing.outBack(fx.phase(0.0F, 0.3F), 1.3F);
      double y0 = fx.ctx.groundY - w * 0.75D - 0.02D;
      double cx = s.cx;
      double cy = y0 + (s.cy - y0) * (double)rise;
      double cz = s.cz;
      double phi = 6.283185307179586D * (double)Easing.inOutCubic(fx.phase(0.7F, 1.3F));
      double theta = 3.141592653589793D * (double)Easing.inOutCubic(fx.phase(1.3F, 1.5F));
      float alpha = fx.alpha();
      float e = exitOf(fx);
      FateCardStyle.Exit how = (FateCardStyle.Exit)this.exit.get();
      s.deckCount = 0;
      if(e > 0.0F) {
         if(how == FateCardStyle.Exit.FLY) {
            cy += 2.6D * w / 1.6D * (double)Easing.inCubic(e);
            phi += 9.42477796076938D * (double)Easing.inQuad(e);
            half *= Math.pow(1.0D - (double)e, 1.3D);
            alpha *= 1.0F - Easing.smoothstep(0.75F, 1.0F, e);
         } else if(how == FateCardStyle.Exit.SHUFFLE) {
            double[] top = this.deck(fx, f, s, e);
            float m = Easing.inOutCubic(Easing.clamp01(e / 0.45F));
            cx += (top[0] - cx) * (double)m;
            cy += (top[1] - cy) * (double)m;
            cz += (top[2] - cz) * (double)m;
            half += (top[3] - half) * (double)m;
         } else if(!Cards.burnWorks()) {
            alpha *= 1.0F - e;
         }
      }

      axes(c, s, phi, theta);
      c.x = f.rx(cx);
      c.y = f.ry(cy);
      c.z = f.rz(cz);
      c.half = half;
      c.alpha = alpha * nearFade(c, cam);
      c.symbol = 4;
      if(fx.t < 0.35000002F) {
         c.cutBelow(f.ry(fx.ctx.groundY));
      } else {
         c.uncut();
      }

   }

   private static void axes(Cards.Card c, FateCardStyle.State s, double phi, double theta) {
      double cp = Math.cos(phi);
      double sp = Math.sin(phi);
      double ct = Math.cos(theta);
      double st = Math.sin(theta);
      double n1x = s.nX * cp + s.rX * sp;
      double n1z = s.nZ * cp + s.rZ * sp;
      c.rx = s.rX * cp - s.nX * sp;
      c.ry = 0.0D;
      c.rz = s.rZ * cp - s.nZ * sp;
      c.ux = n1x * st;
      c.uy = ct;
      c.uz = n1z * st;
      c.nx = n1x * ct;
      c.ny = -st;
      c.nz = n1z * ct;
   }

   private static float nearFade(Cards.Card c, Camera cam) {
      double vx = cam.posX - c.x;
      double vy = cam.posY - c.y;
      double vz = cam.posZ - c.z;
      double lx = Math.max(-c.half, Math.min(c.half, vx * c.rx + vy * c.ry + vz * c.rz));
      double ly = Math.max(-c.half * 1.5D, Math.min(c.half * 1.5D, vx * c.ux + vy * c.uy + vz * c.uz));
      return NearCamera.fade(cam, c.x + c.rx * lx + c.ux * ly, c.y + c.ry * lx + c.uy * ly, c.z + c.rz * lx + c.uz * ly);
   }

   private double[] deck(KillFx fx, FxFrame f, FateCardStyle.State s, float e) {
      double w = s.width;
      double dh = w * 0.13D;
      float scale = Easing.outBack(Easing.clamp01(e / 0.3F), 1.4F) * (1.0F - Easing.inCubic(Easing.clamp01((e - 0.75F) / 0.25F)));
      double bx = s.cx + s.nX * w * 0.12D;
      double bz = s.cz + s.nZ * w * 0.12D;
      double by = fx.ctx.groundY + 0.04D + w * 0.42D - dh * 1.5D * (double)scale;
      float riffle = (float)Math.sin(3.141592653589793D * (double)Easing.clamp01((e - 0.35F) / 0.4F));
      double rx = -s.rX;
      double rz = -s.rZ;
      s.deckCount = scale > 0.01F?6:0;

      for(int k = 0; k < s.deckCount; ++k) {
         Cards.Card d = s.deck[k];
         double a = ((double)k - 2.5D) * 0.16D * (double)riffle;
         double ca = Math.cos(a);
         double sa = Math.sin(a);
         d.rx = rx * ca;
         d.ry = -sa;
         d.rz = rz * ca;
         d.ux = rx * sa;
         d.uy = ca;
         d.uz = rz * sa;
         d.nx = -s.nX;
         d.ny = 0.0D;
         d.nz = -s.nZ;
         double stack = 0.006D * w * (double)k;
         double reach = dh * 1.5D * (double)scale;
         d.x = f.rx(bx + s.nX * stack + d.ux * reach);
         d.y = f.ry(by + d.uy * reach);
         d.z = f.rz(bz + s.nZ * stack + d.uz * reach);
         d.half = dh * (double)scale;
         d.alpha = fx.alpha();
         d.uncut();
      }

      double stack = 0.006D * w * 6.0D;
      this.top[0] = bx + s.nX * stack;
      this.top[1] = by + dh * 1.5D * (double)scale;
      this.top[2] = bz + s.nZ * stack;
      this.top[3] = dh * (double)scale;
      return this.top;
   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      FateCardStyle.State s = (FateCardStyle.State)fx.data;
      if(s != null) {
         Camera cam = Vfx.camera();
         if(pass == FxPass.SOLID) {
            if(!s.placed) {
               place(fx, f, s, cam);
            }

            if(!s.tried && !fx.lod) {
               capture(fx, s);
            }

            this.layout(fx, f, s, cam);
            this.solid(fx, f, s, cam);
         } else if(pass == FxPass.GLOW && s.placed) {
            this.layout(fx, f, s, cam);
            this.glow(fx, f, s, cam);
         }
      }

   }

   private void solid(KillFx fx, FxFrame f, FateCardStyle.State s, Camera cam) {
      Cards.Card c = s.card;
      float drawn = Easing.inOutCubic(fx.phase(0.3F, 0.7F));
      boolean body = fx.t < 0.7F;
      boolean first = drawn < 0.5F;
      if(body && first) {
         this.drawIn(fx, f, s, drawn);
      }

      float e = exitOf(fx);
      if(this.exit.get() == FateCardStyle.Exit.BURN && e > 0.0F && Cards.burnWorks()) {
         Cards.burnHoles(c, e, 0, cam);
      }

      this.meshes(fx, s, c);
      this.art(fx, s, c);
      if(body && !first) {
         this.drawIn(fx, f, s, drawn);
      }

   }

   private void meshes(KillFx fx, FateCardStyle.State s, Cards.Card c) {
      float gold = goldOf(fx);
      Cards.Foil fl = (Cards.Foil)this.foil.get();
      int tint = fl == Cards.Foil.CUSTOM?this.foilTint.getColor(0.5F):-1;
      int body = this.color.getColor(0.5F);
      float face = 0.18F + 0.82F * Easing.smoothstep(0.42000002F, 0.7F, fx.t);
      boolean cut = c.cut;
      double ca = c.cutA;
      double cb = c.cutB;
      double cd = c.cutD;
      double sc = c.half * 2.0D;
      if(gold < 1.0F) {
         if(gold >= 0.0F) {
            c.cutLine(0.6D, 0.8D, goldLine(sc, gold));
         }

         MeshBatch.begin(Material.CUSTOM, Finish.SATIN, fx.light, 0.14F, -1, true, true);
         if(c.half > 1.0E-4D) {
            Cards.body(c, Cards.withAlpha(body, c.alpha * face));
         }

         for(int k = 0; k < s.deckCount; ++k) {
            Cards.body(s.deck[k], Cards.withAlpha(body, s.deck[k].alpha));
         }

         MeshBatch.end();
      }

      if(gold >= 0.0F && c.half > 1.0E-4D) {
         if(gold < 1.0F) {
            c.cutLine(-0.6D, -0.8D, -goldLine(sc, gold));
         } else {
            c.uncut();
         }

         MeshBatch.begin(fl.material, Finish.METALLIC, fx.light, 0.14F, -1, true, true);
         Cards.body(c, Cards.withAlpha(tint, c.alpha));
         MeshBatch.end();
      }

      restoreCut(c, cut, ca, cb, cd);
      MeshBatch.begin(fl.material, Finish.METALLIC, fx.light, 0.14F, -1, true, true);
      double sweep = (double)fx.t * 1.4D % 1.8D - 0.4D;
      if(c.half > 1.0E-4D) {
         Cards.foil(c, Cards.withAlpha(tint, c.alpha), sweep, true);
      }

      for(int k = 0; k < s.deckCount; ++k) {
         Cards.foil(s.deck[k], Cards.withAlpha(tint, s.deck[k].alpha), sweep + (double)k * 0.2D, true);
      }

      MeshBatch.end();
   }

   private static void restoreCut(Cards.Card c, boolean cut, double a, double b, double d) {
      if(cut) {
         c.cutLine(a, b, d);
      } else {
         c.uncut();
      }

   }

   private void art(KillFx fx, FateCardStyle.State s, Cards.Card c) {
      int d = ((Cards.Design)this.design.get()).ordinal();
      float gold = goldOf(fx);
      int ink = ColorUtil.shade(this.lines.getColor(0.5F), fx.light);
      int engraved = ColorUtil.shade(this.foilColor(), 0.42F * fx.light) | -16777216;
      boolean cut = c.cut;
      double ca = c.cutA;
      double cb = c.cutB;
      double cd = c.cutD;
      double sc = c.half * 2.0D;
      RenderState.decal(true);
      RenderState.translucent(false);

      try {
         if(c.half > 1.0E-4D) {
            if(gold < 1.0F) {
               if(gold >= 0.0F) {
                  c.cutLine(0.6D, 0.8D, goldLine(sc, gold));
               }

               this.faceArt(fx, s, c, d, ink, false);
            }

            if(gold >= 0.0F) {
               if(gold < 1.0F) {
                  c.cutLine(-0.6D, -0.8D, -goldLine(sc, gold));
               } else {
                  c.uncut();
               }

               this.faceArt(fx, s, c, d, engraved, true);
            }

            restoreCut(c, cut, ca, cb, cd);
            double titleAt = 0.8D * sc * -0.5234375D;
            boolean goldTitle = gold >= 0.0F && titleAt < goldLine(sc, gold);
            this.titleText(fx, s, c, goldTitle?engraved:ink);
         }

         int back;
         if((back = CardArt.back(d)) != 0) {
            for(int k = 0; k < s.deckCount; ++k) {
               Cards.Card dk = s.deck[k];
               Cards.decal(dk, back, 0.0F, 0.0F, 1.0F, 1.0F, 0.0D, 0.0D, 1.0D, 1.5D, -1, Cards.withAlpha(ink, dk.alpha * 0.9F));
            }
         }
      } finally {
         RenderState.decal(false);
         RenderState.translucent(true);
      }

   }

   private void faceArt(KillFx fx, FateCardStyle.State s, Cards.Card c, int d, int ink, boolean gold) {
      float a = c.alpha;
      int back = CardArt.back(d);
      if(back != 0) {
         Cards.decal(c, back, 0.0F, 0.0F, 1.0F, 1.0F, 0.0D, 0.0D, 1.0D, 1.5D, -1, Cards.withAlpha(ink, a * 0.9F));
      }

      float in = Easing.smoothstep(0.55F, 0.7F, fx.t);
      if(s.picture >= 0 && !fx.lod) {
         int face = CardArt.portraitFace(d);
         if(face != 0) {
            Cards.decal(c, face, 0.0F, 0.0F, 1.0F, 1.0F, 0.0D, 0.0D, 1.0D, 1.5D, 1, Cards.withAlpha(ink, a));
         }

         if(in > 0.003F) {
            boolean lineArt = this.picture.get() == FateCardStyle.Picture.LINE_ART && LineArt.usable();
            int col = lineArt?ink:(gold?ColorUtil.shade(this.foilColor(), fx.light) | -16777216:ColorUtil.shade(-1, fx.light));
            Vfx.flush();
            if(lineArt) {
               LineArt.begin(3, 0.3F, 0.45F);
            }

            try {
               Cards.decal(c, s.picture, 0.0F, 1.0F, 1.0F, 0.0F, 0.0D, 0.0517578125D, 0.6640625D, 0.99609375D, 1, Cards.withAlpha(col, a * in));
               Vfx.flush();
            } finally {
               if(lineArt) {
                  LineArt.end();
               }

            }
         }
      } else {
         int facex = CardArt.face(d);
         if(facex != 0) {
            Cards.decal(c, facex, 0.0F, 0.0F, 1.0F, 1.0F, 0.0D, 0.0D, 1.0D, 1.5D, 1, Cards.withAlpha(ink, a));
         }

         int sym;
         if((sym = CardArt.symbols()) != 0 && in > 0.003F) {
            CardArt.cell(c.symbol, this.uv);
            Cards.decal(c, sym, this.uv[0], this.uv[1], this.uv[2], this.uv[3], 0.0D, 0.1640625D, 0.62D, 0.62D, 1, Cards.withAlpha(ink, a * in));
         }
      }

   }

   private void titleText(KillFx fx, FateCardStyle.State s, Cards.Card c, int ink) {
      float a = c.alpha * fx.phase(0.62F, 0.82F);
      if(!s.title.isEmpty() && a > 0.003F && !fx.lod && s.picture >= 0) {
         int tex = Glitch.atlas();
         double sc = c.half * 2.0D;
         double z = sc * 0.0255D;
         this.plane.set(c.x + c.nx * z, c.y + c.ny * z, c.z + c.nz * z, c.rx, c.ry, c.rz, c.ux, c.uy, c.uz);
         double gh = sc * 0.1328125D * 0.4D / 0.52D;
         double wide = Glitch.textWidth(s.title) * gh;
         double room = sc * 0.5859375D;
         if(wide > room) {
            gh *= room / wide;
            wide = room;
         }

         this.plane.text(tex, s.title, -wide * 0.5D, sc * -0.5234375D - gh * 0.26D, gh, Cards.withAlpha(ink, a));
      }

   }

   private void drawIn(KillFx fx, FxFrame f, FateCardStyle.State s, float k) {
      BodyProxy.Look look = fx.look;
      look.kind = BodyLook.SKIN;
      look.sweepTo = null;
      look.dissolve = -1.0F;
      float before = look.alpha;
      look.alpha = 1.0F - Easing.smoothstep(0.55F, 0.7F, fx.t);
      double w = s.width;
      double scale = w * 0.99609375D / 2.0999999046325684D;
      double feet = w * -0.4462890625D + 0.07999999821186066D * scale;
      double face = w * 0.03D;
      double px = s.cx + s.nX * face;
      double pz = s.cz + s.nZ * face;

      for(int i = 0; i < 6; ++i) {
         BodyProxy.Part q = fx.rest[i];
         BodyProxy.Part p = fx.parts[i];
         p.copy(q);
         if(k > 0.0F) {
            double dx = q.x - fx.ctx.feetX;
            double dy = q.y - fx.ctx.feetY;
            double dz = q.z - fx.ctx.feetZ;
            double across = dx * s.rX + dz * s.rZ;
            double tx = px + s.rX * across * scale;
            double ty = s.cy + feet + dy * scale;
            double tz = pz + s.rZ * across * scale;
            p.x = q.x + (tx - q.x) * (double)k;
            p.y = q.y + (ty - q.y) * (double)k;
            p.z = q.z + (tz - q.z) * (double)k;
            p.scale = (float)(1.0D + (scale - 1.0D) * (double)k);
            p.stretch = (float)(1.0D - 0.97D * (double)k);
            p.sx = s.nX;
            p.sy = 0.0D;
            p.sz = s.nZ;
            p.ox = px;
            p.oy = s.cy;
            p.oz = pz;
         }
      }

      fx.drawBody(f);
      look.alpha = before;
      RenderState.translucent(true);
   }

   private void glow(KillFx fx, FxFrame f, FateCardStyle.State s, Camera cam) {
      Cards.Card c = s.card;
      float alpha = c.alpha;
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      int foilCol = this.foilColor();
      float out = fx.phase(0.0F, 0.06F) * (1.0F - fx.phase(0.25F, 0.5F));
      if(out > 0.01F) {
         double gy = f.ry(fx.ctx.groundY + 0.03D);

         for(int i = -3; i <= 3; ++i) {
            double u = (double)i / 3.0D * s.width * 0.5D;
            Vfx.sprite(Textures.GLOW, f.rx(s.cx + s.rX * u), gy, f.rz(s.cz + s.rZ * u), s.width * 0.35D, 0.0F, alpha(foilCol, 0.28F * out * fx.alpha()));
         }
      }

      if(c.half > 1.0E-4D) {
         double sc = c.half * 2.0D;
         float pull = (float)Math.sin(3.141592653589793D * (double)fx.phase(0.3F, 0.8F));
         if(pull > 0.01F) {
            double z = sc * 0.05D;
            double y = sc * 0.0517578125D;
            Vfx.sprite(Textures.GLOW, c.x + c.nx * z + c.ux * y, c.y + c.ny * z + c.uy * y, c.z + c.nz * z + c.uz * y, sc * 0.9D, 0.0F, alpha(this.lines.getColor(0.5F), (calm?0.12F:0.22F) * pull * alpha));
         }

         float gold;
         if((gold = goldOf(fx)) >= 0.0F && gold < 1.0F) {
            double d = goldLine(sc, gold);
            double z = sc * 0.05D;

            for(int i = -5; i <= 5; ++i) {
               double along = (double)i / 5.0D * sc * 0.9D;
               double lx = 0.6D * d - 0.8D * along;
               double ly = 0.8D * d + 0.6D * along;
               if(Math.abs(lx) <= sc * 0.5D && Math.abs(ly) <= sc * 0.75D) {
                  Vfx.sprite(Textures.GLOW, c.x + c.rx * lx + c.ux * ly + c.nx * z, c.y + c.ry * lx + c.uy * ly + c.ny * z, c.z + c.rz * lx + c.uz * ly + c.nz * z, sc * 0.16D, 0.0F, core(foilCol, (calm?0.25F:0.55F) * alpha));
               }
            }
         }

         float e;
         if((e = exitOf(fx)) > 0.0F) {
            FateCardStyle.Exit how = (FateCardStyle.Exit)this.exit.get();
            if(how == FateCardStyle.Exit.BURN && Cards.burnWorks()) {
               Cards.burnEdge(c, e, 0.07F, 0, -15809, alpha, cam);
            } else if(how != FateCardStyle.Exit.SHUFFLE || e > 0.7F) {
               Cards.Card at = how == FateCardStyle.Exit.SHUFFLE && s.deckCount > 0?s.deck[s.deckCount - 1]:c;
               float k = (float)Math.sin(3.141592653589793D * (double)(how == FateCardStyle.Exit.SHUFFLE?Easing.clamp01((e - 0.7F) / 0.3F):e)) * fx.alpha();
               double r0 = Math.max(at.half, s.width * 0.08D);

               for(int ix = 0; ix < 8; ++ix) {
                  double a = (double)ix * 0.785D + (double)e * 4.0D + (double)fx.seed;
                  double r = r0 * (1.2D + 0.6D * (double)e + 0.25D * Math.sin((double)ix * 2.3D + (double)e * 9.0D));
                  double x = at.x + (at.rx * Math.cos(a) + at.ux * Math.sin(a) * 1.4D) * r;
                  double y = at.y + (at.ry * Math.cos(a) + at.uy * Math.sin(a) * 1.4D) * r;
                  double z = at.z + (at.rz * Math.cos(a) + at.uz * Math.sin(a) * 1.4D) * r;
                  Vfx.sprite(Textures.SPARK, x, y, z, s.width * 0.06D, 0.0F, core(foilCol, 0.8F * k));
               }
            }
         }
      }

   }

   public String stateForTest(KillFx fx) {
      FateCardStyle.State s = (FateCardStyle.State)fx.data;
      if(s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.3F?"rising":(t < 0.7F?"drawing the body in":(t < 1.3F?"turning":(t < 1.5F?"flipping":((FateCardStyle.Exit)this.exit.get()).displayName().toLowerCase(Locale.ROOT))));
         String pic = fx.lod?"the symbol (far)":(!s.tried?"not yet":(s.picture < 0?"the symbol (no portrait)":(this.picture.get() == FateCardStyle.Picture.LINE_ART?"line art (" + LineArt.stateForTest() + ")":"full color")));
         float gold = goldOf(fx);
         return String.format(Locale.ROOT, "%s; card %.2f wide, %s, %s, portrait %s, drawn in %.2f ms; title \"%s\"; exit %.2f (burn %s)%s; light %.2f", new Object[]{phase, Double.valueOf(s.width), ((Cards.Design)this.design.get()).displayName(), ((Cards.Foil)this.foil.get()).displayName(), pic, Float.valueOf(s.pictureMillis), s.title, Float.valueOf(exitOf(fx)), Cards.burnWorks()?"with holes":"as a fade", gold < 0.0F?"":String.format(Locale.ROOT, ", gold %.2f", new Object[]{Float.valueOf(gold)}), Float.valueOf(fx.light)});
      }
   }

   static String roman(int n) {
      n = (Math.max(1, n) - 1) % 3999 + 1;
      int[] values = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
      String[] digits = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < values.length; ++i) {
         while(n >= values[i]) {
            sb.append(digits[i]);
            n -= values[i];
         }
      }

      return sb.toString();
   }

   public static enum Exit implements Displayable {
      BURN("Burn away", "The card burns away from its edges"),
      FLY("Fly up and shrink", "The card flies up, spinning, and shrinks away"),
      SHUFFLE("Shuffle into a deck", "The card shrinks into a deck that riffles and vanishes");

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

   public static enum Picture implements Displayable {
      LINE_ART("Line art", "Fine lines and a few flat tones"),
      FULL_COLOR("Full color", "The body as it was");

      private final String displayName;
      private final String description;

      private Picture(String displayName, String description) {
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
      boolean placed;
      double nX;
      double nZ;
      double rX;
      double rZ;
      double cx;
      double cy;
      double cz;
      double width;
      int picture = -1;
      boolean tried;
      float pictureMillis;
      String title;
      boolean sparked;
      final Cards.Card card = new Cards.Card();
      final Cards.Card[] deck = new Cards.Card[6];
      int deckCount;

      State() {
         for(int k = 0; k < 6; ++k) {
            this.deck[k] = new Cards.Card();
         }

      }
   }

   public static enum Title implements Displayable {
      NAME("Victim\'s name", "The player\'s name"),
      NUMERAL("Roman numeral", "The kill\'s number this session: I, II, III ..."),
      NONE("None", "No title");

      private final String displayName;
      private final String description;

      private Title(String displayName, String description) {
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
