package mix.client.mixui.integration.haunt.render.material;

import mix.client.mixui.integration.haunt.asset.AssetSource;
import mix.client.mixui.integration.haunt.asset.ShaderFiles;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.render.GlSave;
import mix.client.mixui.integration.haunt.render.OptiFineCompat;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class MaterialShader {
   private static final String VERT = "material/material.vsh";
   private static final String FRAG = "material/material.fsh";
   private static boolean compiled;
   private static boolean broken;
   private static int filesGeneration = -1;
   private static int program;
   private static int uTintable;
   private static int uLight;
   private static int uRim;
   private static int uGlass;
   private static int previous;
   private static boolean checked;
   private static boolean askDriver;
   private static Boolean forceFallback;

   static void forceFallbackForTest(Boolean on) {
      forceFallback = on;
   }

   static boolean usable() {
      if(forceFallback != null && forceFallback.booleanValue()) {
         return false;
      } else if(OptiFineCompat.shadersActive()) {
         return false;
      } else {
         if(!compiled || filesGeneration != AssetSource.generation()) {
            compile();
         }

         return program != 0 && !broken;
      }
   }

   static void begin(boolean tintable, float light, int rimColor, float rim, boolean glass) {
      previous = boundProgram();
      GL20.glUseProgram(program);
      GL20.glUniform1f(uTintable, tintable?1.0F:0.0F);
      GL20.glUniform1f(uLight, light);
      GL20.glUniform4f(uRim, (float)(rimColor >> 16 & 255) / 255.0F, (float)(rimColor >> 8 & 255) / 255.0F, (float)(rimColor & 255) / 255.0F, rim);
      GL20.glUniform1f(uGlass, glass?1.0F:0.0F);
   }

   static void end() {
      GL20.glUseProgram(previous);
   }

   private static int boundProgram() {
      int tracked = GlSave.trackedProgram();
      if(tracked >= 0) {
         return tracked;
      } else if(checked && !askDriver) {
         return 0;
      } else {
         int p = GL11.glGetInteger('讍');
         if(!checked) {
            checked = true;
            askDriver = p != 0;
         }

         return p;
      }
   }

   private static void compile() {
      compiled = true;
      filesGeneration = AssetSource.generation();
      if(program != 0) {
         GL20.glDeleteProgram(program);
         program = 0;
      }

      broken = false;
      if(!OpenGlHelper.shadersSupported) {
         broken = true;
         FuriahsVisuals.LOG.info("Materials: no shader support here; the fallback shades them");
      } else {
         String vertex = ShaderFiles.text("material/material.vsh");
         String fragment = ShaderFiles.text("material/material.fsh");
         if(vertex != null && fragment != null) {
            int before = boundProgram();

            try {
               int vs = shader('謱', vertex);
               int fs = shader('謰', fragment);
               if(vs != 0 && fs != 0) {
                  int p = GL20.glCreateProgram();
                  GL20.glAttachShader(p, vs);
                  GL20.glAttachShader(p, fs);
                  GL20.glLinkProgram(p);
                  GL20.glDeleteShader(vs);
                  GL20.glDeleteShader(fs);
                  if(GL20.glGetProgrami(p, '讂') == 0) {
                     FuriahsVisuals.LOG.warn("Material shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                     GL20.glDeleteProgram(p);
                     fail("it did not link");
                     return;
                  }

                  program = p;
                  uTintable = GL20.glGetUniformLocation(p, "uTintable");
                  uLight = GL20.glGetUniformLocation(p, "uLight");
                  uRim = GL20.glGetUniformLocation(p, "uRim");
                  uGlass = GL20.glGetUniformLocation(p, "uGlass");
                  GL20.glUseProgram(p);
                  GL20.glUniform1i(GL20.glGetUniformLocation(p, "uMatcap"), 0);
                  return;
               }

               fail("it did not compile");
            } catch (LinkageError | RuntimeException var9) {
               FuriahsVisuals.LOG.warn("Material shader setup failed", var9);
               fail("its setup failed");
               return;
            } finally {
               GL20.glUseProgram(before);
            }

            return;
         }
      }

   }

   private static int shader(int type, String source) {
      int s = GL20.glCreateShader(type);
      GL20.glShaderSource(s, source);
      GL20.glCompileShader(s);
      if(GL20.glGetShaderi(s, '讁') == 0) {
         FuriahsVisuals.LOG.warn("Material shader failed to compile: {}", new Object[]{GL20.glGetShaderInfoLog(s, 2048)});
         GL20.glDeleteShader(s);
         return 0;
      } else {
         return s;
      }
   }

   private static void fail(String why) {
      broken = true;
      program = 0;
      FuriahsVisuals.LOG.warn("Material shader switched off ({}); the fallback shades materials", new Object[]{why});
   }

   static String stateForTest() {
      usable();
      return forceFallback != null && forceFallback.booleanValue()?"fallback (asked)":(OptiFineCompat.shadersActive()?"fallback (shader pack)":(program != 0 && !broken?"shader":"fallback (no shader)"));
   }
}
