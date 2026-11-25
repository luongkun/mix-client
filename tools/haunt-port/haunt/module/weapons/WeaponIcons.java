package mix.client.mixui.integration.haunt.module.weapons;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.module.weapons.Mat4;
import mix.client.mixui.integration.haunt.module.weapons.PixelIcons;
import mix.client.mixui.integration.haunt.module.weapons.WeaponAssets;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponOptions;
import mix.client.mixui.integration.haunt.module.weapons.WeaponRenderer;
import mix.client.mixui.integration.haunt.module.weapons.WeaponSettings;
import mix.client.mixui.integration.haunt.module.weapons.WeaponShader;
import mix.client.mixui.integration.haunt.module.weapons.WeaponStudio;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.render.GlSave;
import mix.client.mixui.integration.haunt.render.GlTarget;
import mix.client.mixui.integration.haunt.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class WeaponIcons {
   private static final int CELL = 128;
   private static final int COLS = 8;
   private static final int CELLS = 64;
   private final WeaponsModule module;
   private final WeaponRenderer renderer;
   private final WeaponStudio studio;
   private final GlSave save = new GlSave();
   private GlTarget atlas;
   private final Map<String, Integer> cells = new LinkedHashMap(16, 0.75F, true);
   private final Mat4 turn = new Mat4();
   private final Mat4 model = new Mat4();
   private boolean failed;
   private final PixelIcons pixels = new PixelIcons();

   WeaponIcons(WeaponsModule module, WeaponRenderer renderer) {
      this.module = module;
      this.renderer = renderer;
      this.studio = new WeaponStudio(renderer);
      this.pixels.moduleForLook = module;
   }

   boolean draw(ItemStack stack, int x, int y, float zLevel) {
      if(!this.failed && this.module.icons.get()) {
         WeaponsModule m = this.module;
         float scale = m.iconSize.getFloat();
         float dx = m.iconX.getFloat();
         float dy = m.iconY.getFloat();
         float rot = m.iconRotation.getFloat();
         boolean moved = scale != 1.0F || dx != 0.0F || dy != 0.0F || rot != 0.0F;
         if(moved) {
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)x + 8.0F + dx, (float)y + 8.0F + dy, 0.0F);
            GlStateManager.rotate(rot, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            GlStateManager.translate((float)(-x) - 8.0F, (float)(-y) - 8.0F, 0.0F);
         }

         boolean var11;
         try {
            var11 = this.drawPlaced(stack, x, y, zLevel);
         } finally {
            if(moved) {
               GlStateManager.popMatrix();
            }

         }

         return var11;
      } else {
         return false;
      }
   }

   static void iconLook(WeaponShader.Program p, WeaponsModule m) {
      p.set1f("uFade", 1.0F - m.iconOpacity.getFloat());
      p.set1f("uBrightAdd", m.iconBrightness.getFloat() - 1.0F);
   }

   private boolean drawPlaced(ItemStack stack, int x, int y, float zLevel) {
      if(!this.failed) {
         WeaponDef def = this.module.weaponFor(stack);
         if(def != null && !def.isVanilla()) {
            WeaponShader.Program icon = this.renderer.shaders().icon();
            if(icon != null && this.renderer.shaders().composite() != null) {
               if(this.module.pixelFor(stack, def) && this.pixels.draw(icon, def, this.module.settingsOf(def), this.module.ruleFinish(stack, def), x, y, zLevel, this.module.enchantMode(stack), this.module.clientTime())) {
                  return true;
               } else {
                  WeaponAssets a = WeaponAssets.of(def, WeaponAssets.Tier.CARD);
                  a.request(256);
                  if(!a.ready()) {
                     WeaponAssets.pump();
                     if(!a.ready()) {
                        return false;
                     }
                  }

                  if(a.pixelImport()) {
                     return false;
                  } else {
                     WeaponSettings s = this.module.settingsOf(def);
                     WeaponOptions.Finish finish = this.module.ruleFinish(stack, def);
                     String key = def.id + "|" + System.identityHashCode(a) + "|" + lookKey(s) + "|" + finish;
                     Integer cell = (Integer)this.cells.get(key);
                     if(cell == null && (cell = this.render(a, s, finish, key)) == null) {
                        return false;
                     } else {
                        this.drawCell(icon, cell.intValue(), x, y, zLevel, this.module.enchantMode(stack));
                        return true;
                     }
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   float[] iconOf(WeaponDef def) {
      return this.iconOf(def, (WeaponOptions.Finish)null);
   }

   float[] iconOf(WeaponDef def, WeaponOptions.Finish finish) {
      if(!this.failed && !def.isVanilla() && this.renderer.shaders().composite() != null) {
         float[] uv;
         if(this.module.pixelOf(def, WeaponOptions.StyleChoice.SECTION) && (uv = this.pixels.iconOf(def, this.module.settingsOf(def), finish)) != null) {
            return uv;
         } else {
            WeaponAssets a = WeaponAssets.of(def, WeaponAssets.Tier.CARD);
            a.request(256);
            if(!a.ready()) {
               WeaponAssets.pump();
               if(!a.ready()) {
                  return null;
               }
            }

            if(a.pixelImport()) {
               return null;
            } else {
               WeaponSettings s = this.module.settingsOf(def);
               String key = def.id + "|" + System.identityHashCode(a) + "|" + lookKey(s) + "|" + finish;
               Integer cell = (Integer)this.cells.get(key);
               if(cell == null && (cell = this.render(a, s, finish, key)) == null) {
                  return null;
               } else {
                  int cx = cell.intValue() % 8 * 128;
                  int cy = cell.intValue() / 8 * 128;
                  float size = 1024.0F;
                  return new float[]{(float)this.atlas.texture, (float)cx / size, (float)(cy + 128) / size, (float)(cx + 128) / size, (float)cy / size};
               }
            }
         }
      } else {
         return null;
      }
   }

   private static String lookKey(WeaponSettings s) {
      StringBuilder sb = new StringBuilder();

      for(Setting x : s.all) {
         String g = x.getGroup();
         if("Look".equals(g)) {
            sb.append(x.toJson()).append(';');
         }
      }

      return Integer.toHexString(sb.toString().hashCode());
   }

   private Integer render(WeaponAssets a, WeaponSettings s, WeaponOptions.Finish finish, String key) {
      int cell;
      if(this.cells.size() >= 64) {
         Iterator<Entry<String, Integer>> it = this.cells.entrySet().iterator();
         Entry<String, Integer> oldest = (Entry)it.next();
         cell = ((Integer)oldest.getValue()).intValue();
         it.remove();
      } else {
         cell = this.cells.size();
      }

      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      this.save.save();

      try {
         GL11.glDisable(3089);
         if(this.atlas == null) {
            this.atlas = GlTarget.create(1024, 1024, '聘', 6408, true, false);
            if(this.atlas == null) {
               this.failed = true;
               Object var14 = null;
               return (Integer)var14;
            }

            this.atlas.bind();
            GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager.clear(16384);
         }

         int w = 256;
         if(!this.studio.begin(256, w)) {
            this.failed = true;
            Object var20 = null;
            return (Integer)var20;
         } else {
            this.turn.identity();
            this.turn.rotate(-45.0F, 0.0F, 0.0F, 1.0F);
            this.turn.rotate(18.0F, 0.0F, 1.0F, 0.0F);
            this.turn.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
            this.studio.fit(a.def, a.mesh(1), this.turn, 12.0F, 0.92F, this.model);
            this.studio.light.studio(0.0F, 0.0F);
            WeaponRenderer.Look look = (new WeaponRenderer.Look()).set(s);
            look.finish = finish;
            this.studio.draw(a, look, 1, this.model, 12.0F, w, w, 0.0F);
            int cx = cell % 8 * 128;
            int cy = cell / 8 * 128;
            this.studio.finish(this.atlas, cx, cy, 128, 128, w, w, 6.0F);
            this.cells.put(key, Integer.valueOf(cell));
            Integer var10 = Integer.valueOf(cell);
            return var10;
         }
      } catch (RuntimeException | LinkageError var14) {
         this.failed = true;
         FuriahsVisuals.LOG.warn("Weapon icons are off: they could not be drawn", var14);
         Object look = null;
         return (Integer)look;
      } finally {
         this.save.restore();
         GlStateManager.matrixMode(5889);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
      }
   }

   private void drawCell(WeaponShader.Program p, int cell, int x, int y, float zLevel, int enchant) {
      int cx = cell % 8 * 128;
      int cy = cell / 8 * 128;
      float size = 1024.0F;
      float u0 = (float)cx / size;
      float u1 = (float)(cx + 128) / size;
      float v0 = (float)(cy + 128) / size;
      float v1 = (float)cy / size;
      int previous = GL11.glGetInteger('讍');
      GlStateManager.bindTexture(this.atlas.texture);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
      GlStateManager.disableLighting();
      GlStateManager.disableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      p.use();
      iconLook(p, this.module);
      p.set4f("uCell", u0, v1, u1, v0);
      p.set1f("uSheen", (float)enchant);
      p.set1f("uTime", this.module.clientTime());
      p.set3f("uSheenColor", 0.55F, 0.32F, 1.0F);

      try {
         Tessellator t = Tessellator.getInstance();
         WorldRenderer wr = t.getWorldRenderer();
         float z = zLevel + 50.0F;
         wr.begin(7, DefaultVertexFormats.POSITION_TEX);
         wr.pos((double)x, (double)(y + 16), (double)z).tex((double)u0, (double)v1).endVertex();
         wr.pos((double)(x + 16), (double)(y + 16), (double)z).tex((double)u1, (double)v1).endVertex();
         wr.pos((double)(x + 16), (double)y, (double)z).tex((double)u1, (double)v0).endVertex();
         wr.pos((double)x, (double)y, (double)z).tex((double)u0, (double)v0).endVertex();
         t.draw();
      } finally {
         GL20.glUseProgram(previous);
         GlStateManager.blendFunc(770, 771);
         Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
      }

   }

   void clear() {
      this.cells.clear();
      this.pixels.clear();
   }

   void saveForTest(File file) throws IOException {
      if(this.atlas != null) {
         int n = 1024;
         ByteBuffer px = BufferUtils.createByteBuffer(n * n * 4);
         GlStateManager.bindTexture(this.atlas.texture);
         GL11.glGetTexImage(3553, 0, 6408, 5121, px);
         BufferedImage img = new BufferedImage(n, n, 2);

         for(int y = 0; y < n; ++y) {
            for(int x = 0; x < n; ++x) {
               int i = ((n - 1 - y) * n + x) * 4;
               int r = px.get(i) & 255;
               int g = px.get(i + 1) & 255;
               int b = px.get(i + 2) & 255;
               int a = px.get(i + 3) & 255;
               img.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
            }
         }

         ImageIO.write(img, "png", file);
      }

   }

   void release() {
      GlTarget.delete(this.atlas);
      this.atlas = null;
      this.studio.release();
      this.cells.clear();
      this.pixels.release();
   }
}
