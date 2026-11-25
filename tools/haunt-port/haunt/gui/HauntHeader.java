package mix.client.mixui.integration.haunt.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Desktop.Action;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.UUID;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.gui.Theme;
import mix.client.mixui.integration.haunt.gui.Ui;
import mix.client.mixui.integration.haunt.module.watermark.HauntIconBridge;
import mix.client.mixui.integration.haunt.render.BodyProxy;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.sound.ModSounds;
import mix.client.mixui.integration.haunt.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager.SkinAvailableCallback;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.Sys;

public final class HauntHeader {
   public static final float DISCORD_H = 18.0F;
   public static final float DISCORD_GAP = 6.0F;
   private static final float PROFILE_H = 18.0F;
   private static int discordTex = -1;
   private static boolean discordTried;
   private static float discordHover;
   private static float discordPress;
   private static float discordX = -10000.0F;
   private static float discordY;
   private static float discordW;
   private static float discordHgt;
   private static float rippleT = -1.0F;
   private static float rippleX;
   private static float rippleY;
   private static long openedAt = System.nanoTime();
   private static float profileAppear;
   private static float profileX = -10000.0F;
   private static float profileY;
   private static float profileW;
   private static String profileName;
   private static ResourceLocation profileSkin;
   private static String skinFor;
   private static boolean skinRequested;
   private static float lookX;
   private static float lookY;
   private static final float[] BAT_T = new float[]{-1.0F, -1.0F};
   private static final float[] BAT_Y = new float[2];
   private static final float[] BAT_DIR = new float[2];
   private static float batTimer = 4.0F;

   public static void menuOpened() {
      openedAt = System.nanoTime();
      profileAppear = 0.0F;
   }

   private static float time() {
      return (float)((double)(System.nanoTime() - openedAt) / 1.0E9D);
   }

   public static boolean hauntTheme() {
      try {
         return FuriahsVisuals.instance != null && FuriahsVisuals.instance.getTheme().get() == Theme.Style.HAUNT;
      } catch (Throwable var1) {
         return false;
      }
   }

   public static void ghost(float cx, float cy, float size, float alpha, float mouseX, float mouseY, float dt) {
      float t = time();
      float appear = Easing.outBack(Easing.clamp01(t / 0.6F));
      float a = alpha * Easing.clamp01(t / 0.35F);
      if(a > 0.01F) {
         float bob = (float)Math.sin((double)t * 2.1D) * size * 0.07F;
         float sway = (float)Math.sin((double)t * 1.3D) * 4.0F;
         float y = cy + bob;
         float s = size * (0.85F + 0.15F * appear);
         float dx = mouseX - cx;
         float dy = mouseY - y;
         float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
         float tx = len > 1.0F?dx / len:0.0F;
         float ty = len > 1.0F?dy / len:0.0F;
         lookX = Easing.approach(lookX, tx, 6.0F, dt);
         lookY = Easing.approach(lookY, ty, 6.0F, dt);
         int glow = ColorUtil.withAlpha(-10802554, 0.55F * a);
         Ui.glow(cx, y + s * 0.05F, s * 3.2F, glow);
         Ui.glow(cx, y + s * 0.1F, s * 1.6F, ColorUtil.withAlpha(-2529761, 0.16F * a));
         GlStateManager.pushMatrix();
         GlStateManager.translate(cx, y, 0.0F);
         GlStateManager.rotate(sway, 0.0F, 0.0F, 1.0F);
         int body = ColorUtil.withAlpha(-1054469, a);
         float r = s * 0.5F;
         Ui.circle(0.0F, -s * 0.12F, r, body);
         Ui.rect(-r, -s * 0.12F, r * 2.0F, s * 0.52F, body);

         for(int i = 0; i < 3; ++i) {
            float wave = (float)Math.sin((double)(t * 4.2F) + (double)i * 1.9D) * s * 0.035F;
            float bx = -r + r / 3.0F + (float)i * (r * 2.0F / 3.0F);
            Ui.circle(bx, s * 0.4F + wave, r / 3.0F + 0.15F, body);
         }

         float blink = blink(t);
         float ex = lookX * s * 0.07F;
         float ey = lookY * s * 0.05F;
         int eye = ColorUtil.withAlpha(-15069654, a);
         float ew = s * 0.11F;
         float eh = s * 0.17F * blink + 0.6F;
         Ui.roundRect(-s * 0.2F - ew / 2.0F + ex, -s * 0.12F - eh / 2.0F + ey, ew, eh, ew / 2.0F, eye);
         Ui.roundRect(s * 0.2F - ew / 2.0F + ex, -s * 0.12F - eh / 2.0F + ey, ew, eh, ew / 2.0F, eye);
         if(blink > 0.5F) {
            int shine = ColorUtil.withAlpha(-1, 0.85F * a);
            Ui.circle(-s * 0.18F + ex, -s * 0.16F + ey, s * 0.025F + 0.3F, shine);
            Ui.circle(s * 0.22F + ex, -s * 0.16F + ey, s * 0.025F + 0.3F, shine);
         }

         Ui.circle(ex * 0.6F, s * 0.06F + ey * 0.6F, s * 0.045F + 0.3F, ColorUtil.withAlpha(-12967600, a));
         Ui.circle(-s * 0.32F, -s * 0.0F, s * 0.06F, ColorUtil.withAlpha(-1010036, 0.42F * a));
         Ui.circle(s * 0.32F, -s * 0.0F, s * 0.06F, ColorUtil.withAlpha(-1010036, 0.42F * a));
         GlStateManager.popMatrix();
      }

   }

   private static float blink(float t) {
      float period = 3.7F;
      float p = t % period;
      if(p > period - 0.14F) {
         float k = (p - (period - 0.14F)) / 0.14F;
         return Math.abs(1.0F - 2.0F * k);
      } else {
         return 1.0F;
      }
   }

   public static float title(float x, float y, int accent, boolean icons, float mouseX, float mouseY, float dt) {
      if(icons) {
         ghost(x + 7.0F, y + 10.0F, 15.0F, Ui.alpha(), mouseX, mouseY, dt);
         return 24.0F;
      } else {
         ghost(x + 7.0F, y + 9.0F, 15.0F, Ui.alpha(), mouseX, mouseY, dt);
         int orange = -2529761;
         int head = accent | -16777216;
         int titleColor = hauntTheme()?orange:head;
         Ui.textHeavy("HAUN’T.", x + 21.0F, y - 2.0F, 9.6F, titleColor);
         Ui.textBold("VISUALS", x + 21.0F, y + 8.5F, 11.5F, Theme.TEXT);
         Ui.text("A DARKER WAY TO SEE MINECRAFT.", x + 1.0F, y + 22.0F, 4.6F, ColorUtil.withAlpha(Theme.TEXT_DIM, 0.85F));
         return 30.0F;
      }
   }

   public static void discord(float x, float y, float w, float h, boolean icons, float mouseX, float mouseY, float dt) {
      discordX = x;
      discordY = y;
      discordW = w;
      discordHgt = h;
      boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
      discordHover = Easing.approach(discordHover, over?1.0F:0.0F, 14.0F, dt);
      discordPress = Easing.approach(discordPress, 0.0F, 9.0F, dt);
      Ui.registerControl(x, y, w, h, "Discord");
      float lift = discordHover * 1.0F - discordPress * 1.2F;
      float yy = y - lift;
      int blurple = -10983950;
      int base = ColorUtil.lerp(ColorUtil.withAlpha(blurple, 0.22F), ColorUtil.withAlpha(blurple, 0.42F), discordHover);
      float r = Math.min(h / 2.0F, 7.0F);
      if(discordHover > 0.01F) {
         Ui.shadow(x, yy + 1.5F, w, h, r, 8.0F, ColorUtil.withAlpha(blurple, 0.35F * discordHover));
      }

      Ui.roundRect(x, yy, w, h, r, base);
      Ui.outline(x, yy, w, h, r, 0.8F, ColorUtil.withAlpha(ColorUtil.brighten(blurple, 0.35F), 0.35F + 0.4F * discordHover));
      if(rippleT >= 0.0F) {
         rippleT += dt;
         float k = rippleT / 0.45F;
         if(k >= 1.0F) {
            rippleT = -1.0F;
         } else {
            Ui.pushScissor(x, yy, w, h);
            Ui.circle(rippleX, rippleY - lift, 4.0F + k * w * 0.7F, ColorUtil.withAlpha(-1, 0.18F * (1.0F - k)));
            Ui.popScissor();
         }
      }

      int tex = discordTexture();
      float icon = Math.min(h - 6.0F, 11.0F);
      int white = ColorUtil.lerp(-2302209, -1, discordHover);
      if(icons) {
         drawIcon(tex, x + (w - icon) / 2.0F, yy + (h - icon) / 2.0F, icon, white);
      } else {
         String label = "Discord";
         float ts = 8.2F;
         float tw = Ui.widthBold(label, ts);
         float total = icon + 5.0F + tw;
         float sx = x + (w - total) / 2.0F;
         drawIcon(tex, sx, yy + (h - icon) / 2.0F, icon, white);
         Ui.textBold(label, sx + icon + 5.0F, yy + (h - Ui.lineHeight(ts)) / 2.0F + 0.5F, ts, white);
      }

   }

   private static void drawIcon(int tex, float x, float y, float s, int color) {
      if(tex > 0) {
         Ui.image(tex, x, y, s, s, 0.0F, 0.0F, 1.0F, 1.0F, color);
      } else {
         Ui.roundRect(x + s * 0.1F, y + s * 0.2F, s * 0.8F, s * 0.6F, s * 0.25F, color);
      }

   }

   private static int discordTexture() {
      if(!discordTried) {
         discordTried = true;

         try {
            int size = 64;
            Shape shape = HauntIconBridge.discord(size);
            if(shape != null) {
               BufferedImage img = new BufferedImage(size, size, 2);
               Graphics2D g = img.createGraphics();
               g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
               g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
               g.setColor(Color.WHITE);
               g.fill(shape);
               g.dispose();
               int[] argb = img.getRGB(0, 0, size, size, (int[])null, 0, size);
               discordTex = Textures.uploadImage(argb, size, size, true, false);
            }
         } catch (Throwable var5) {
            FuriahsVisuals.LOG.warn("Header: could not make the Discord icon", var5);
            discordTex = -1;
         }
      }

      return discordTex;
   }

   public static boolean overDiscord(float mx, float my) {
      return discordX > -5000.0F && mx >= discordX && mx < discordX + discordW && my >= discordY - 1.0F && my < discordY + discordHgt;
   }

   public static String tooltip(float mx, float my) {
      return overDiscord(mx, my)?"Join the HAUN’T VISUALS Discord (opens discord.gg/ej8eVhgwGZ in your browser)":(overProfile(mx, my)?"Signed in as " + profileName:null);
   }

   public static boolean mouseDown(float mx, float my, int button) {
      if(button == 0 && overDiscord(mx, my)) {
         discordPress = 1.0F;
         rippleT = 0.0F;
         rippleX = mx;
         rippleY = my;
         ModSounds.gui(Sfx.CONFIRM);
         openLink("https://discord.gg/ej8eVhgwGZ");
         return true;
      } else {
         return false;
      }
   }

   public static void openLink(final String url) {
      Thread t = new Thread(new Runnable() {
         public void run() {
            if(!HauntHeader.tryDesktop(url) && !HauntHeader.tryLwjgl(url) && !HauntHeader.tryCommand(url)) {
               FuriahsVisuals.LOG.warn("Could not open {} in a browser", new Object[]{url});
            }

         }
      }, "HAUN\'T VISUALS link");
      t.setDaemon(true);
      t.start();
   }

   private static boolean tryDesktop(String url) {
      try {
         if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
            return true;
         }
      } catch (Throwable var2) {
         ;
      }

      return false;
   }

   private static boolean tryLwjgl(String url) {
      try {
         return Sys.openURL(url);
      } catch (Throwable var2) {
         return false;
      }
   }

   private static boolean tryCommand(String url) {
      String os = System.getProperty("os.name", "").toLowerCase();

      try {
         if(os.contains("win")) {
            (new ProcessBuilder(new String[]{"rundll32", "url.dll,FileProtocolHandler", url})).start();
         } else if(os.contains("mac")) {
            (new ProcessBuilder(new String[]{"open", url})).start();
         } else {
            (new ProcessBuilder(new String[]{"xdg-open", url})).start();
         }

         return true;
      } catch (Throwable var3) {
         return false;
      }
   }

   private static void refreshProfile() {
      Minecraft mc = Minecraft.getMinecraft();
      String name = null;
      ResourceLocation skin = null;

      try {
         if(mc.thePlayer != null) {
            name = mc.thePlayer.getName();
            skin = mc.thePlayer.getLocationSkin();
         }
      } catch (Throwable var7) {
         ;
      }

      if(name == null) {
         try {
            name = mc.getSession().getUsername();
         } catch (Throwable var61) {
            name = "Player";
         }
      }

      if(!name.equals(skinFor)) {
         skinFor = name;
         skinRequested = false;
         profileSkin = null;
      }

      if(skin != null) {
         profileSkin = skin;
      } else if(!skinRequested) {
         skinRequested = true;

         try {
            final GameProfile profile = mc.getSession().getProfile();
            UUID id = profile.getId();
            profileSkin = id != null?DefaultPlayerSkin.getDefaultSkin(id):null;
            mc.getSkinManager().loadProfileTextures(profile, new SkinAvailableCallback() {
               public void skinAvailable(Type type, ResourceLocation location, MinecraftProfileTexture texture) {
                  if(type == Type.SKIN && profile.getName() != null && profile.getName().equals(HauntHeader.skinFor)) {
                     HauntHeader.profileSkin = location;
                  }

               }
            }, true);
         } catch (Throwable var5) {
            ;
         }
      }

      profileName = name;
   }

   public static float profileWidth() {
      refreshProfile();
      return 26.0F + Ui.widthBold(profileName == null?"":profileName, 8.0F);
   }

   public static void profile(float x, float y, float mouseX, float mouseY, float dt) {
      refreshProfile();
      profileAppear = Easing.approach(profileAppear, 1.0F, 5.0F, dt);
      float a = Easing.outCubic(profileAppear);
      float w = profileWidth();
      float h = 18.0F;
      float yy = y + (1.0F - a) * -6.0F;
      profileX = x;
      profileY = yy;
      profileW = w;
      Ui.pushAlpha(a);

      try {
         boolean over = overProfile(mouseX, mouseY);
         Ui.roundRect(x, yy, w, h, h / 2.0F, ColorUtil.withAlpha(-16054000, over?0.86F:0.72F));
         Ui.outline(x, yy, w, h, h / 2.0F, 0.8F, ColorUtil.withAlpha(-2529761, over?0.55F:0.28F));
         float hs = h - 4.0F;
         float hx = x + 2.0F;
         float hy = yy + 2.0F;
         ResourceLocation skin = profileSkin;
         int tex = skin != null?BodyProxy.texture(skin):-1;
         if(tex > 0) {
            float u = 0.125F;
            Ui.roundedImage(tex, hx, hy, hs, hs, 3.0F, u, u, u * 2.0F, u * 2.0F, -1);
            float pad = hs * 0.06F;
            Ui.roundedImage(tex, hx - pad, hy - pad, hs + pad * 2.0F, hs + pad * 2.0F, 3.4F, u * 5.0F, u, u * 6.0F, u * 2.0F, -1);
         } else {
            Ui.roundRect(hx, hy, hs, hs, 3.0F, -10802554);
            ghost(hx + hs / 2.0F, hy + hs / 2.0F - 1.0F, hs * 0.7F, a, mouseX, mouseY, dt);
         }

         Ui.textBold(profileName, x + 22.0F, yy + (h - Ui.lineHeight(8.0F)) / 2.0F + 0.5F, 8.0F, Theme.TEXT);
         Ui.registerText(x, yy, w, h, "profile " + profileName);
      } finally {
         Ui.popAlpha();
      }

   }

   public static boolean overProfile(float mx, float my) {
      return profileX > -5000.0F && mx >= profileX && mx < profileX + profileW && my >= profileY && my < profileY + 18.0F;
   }

   public static float profileHeight() {
      return 18.0F;
   }

   public static void ambience(float vw, float vh, float dt) {
      if(hauntTheme()) {
         float t = time();
         float a = Easing.clamp01(t / 0.8F) * Ui.alpha();

         for(int i = 0; i < 4; ++i) {
            float px = vw * (0.15F + 0.23F * (float)i) + (float)Math.sin((double)(t * 0.13F) + (double)i * 2.1D) * vw * 0.06F;
            float py = vh * (0.82F - 0.05F * (float)(i % 2)) + (float)Math.cos((double)(t * 0.11F) + (double)i) * vh * 0.04F;
            int c = i % 2 == 0?-10802554:-7717358;
            Ui.glow(px, py, vh * 0.65F, ColorUtil.withAlpha(c, 0.13F * a));
         }

         for(int i = 0; i < 14; ++i) {
            float seed = (float)i * 12.9898F;
            float sx = (float)((Math.sin((double)seed) * 43758.5453D % 1.0D + 1.0D) % 1.0D);
            float speed = 0.012F + 0.01F * (float)(i % 3);
            float py = (1.0F - (t * speed + sx * 3.1F) % 1.0F) * vh;
            float px = sx * vw + (float)Math.sin((double)(t * 0.7F + (float)i)) * 6.0F;
            float tw = 0.4F + 0.6F * (float)Math.abs(Math.sin((double)(t * 1.3F + (float)i)));
            Ui.circle(px, py, 0.7F + (float)(i % 3) * 0.25F, ColorUtil.withAlpha(i % 4 == 0?-2529761:-3558145, 0.35F * tw * a));
         }

         batTimer -= dt;
         if(batTimer <= 0.0F) {
            batTimer = 9.0F + (float)Math.random() * 10.0F;

            for(int i = 0; i < BAT_T.length; ++i) {
               if(BAT_T[i] < 0.0F) {
                  BAT_T[i] = 0.0F;
                  BAT_Y[i] = vh * (0.08F + (float)Math.random() * 0.25F);
                  BAT_DIR[i] = Math.random() < 0.5D?1.0F:-1.0F;
                  break;
               }
            }
         }

         for(int ix = 0; ix < BAT_T.length; ++ix) {
            if(BAT_T[ix] >= 0.0F) {
               BAT_T[ix] += dt;
               float k = BAT_T[ix] / 6.5F;
               if(k >= 1.0F) {
                  BAT_T[ix] = -1.0F;
               } else {
                  float bx = BAT_DIR[ix] > 0.0F?-20.0F + k * (vw + 40.0F):vw + 20.0F - k * (vw + 40.0F);
                  float by = BAT_Y[ix] + (float)Math.sin((double)(k * 9.0F)) * 8.0F;
                  bat(bx, by, 5.0F, BAT_T[ix], ColorUtil.withAlpha(-16448760, 0.55F * a));
               }
            }
         }
      }

   }

   private static void bat(float x, float y, float s, float t, int color) {
      float flap = (float)Math.sin((double)(t * 14.0F));
      float tipY = -flap * s * 0.9F;
      Ui.circle(x, y, s * 0.32F, color);
      Ui.line(x, y, x - s * 0.9F, y + tipY * 0.5F - s * 0.2F, s * 0.28F, true, color);
      Ui.line(x - s * 0.9F, y + tipY * 0.5F - s * 0.2F, x - s * 1.8F, y + tipY, s * 0.2F, true, color);
      Ui.line(x, y, x + s * 0.9F, y + tipY * 0.5F - s * 0.2F, s * 0.28F, true, color);
      Ui.line(x + s * 0.9F, y + tipY * 0.5F - s * 0.2F, x + s * 1.8F, y + tipY, s * 0.2F, true, color);
      Ui.circle(x - s * 0.15F, y - s * 0.32F, s * 0.1F, color);
      Ui.circle(x + s * 0.15F, y - s * 0.32F, s * 0.1F, color);
   }
}
