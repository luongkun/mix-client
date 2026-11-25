package mix.client.mixui.integration.haunt.render.look;

import mix.client.mixui.integration.haunt.asset.AssetSource;
import mix.client.mixui.integration.haunt.asset.ShaderFiles;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.render.GlSave;
import mix.client.mixui.integration.haunt.render.OptiFineCompat;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL20;

public final class LineArt {
   private static final String VERT = "look/lineart.vsh";
   private static final String FRAG = "look/lineart.fsh";
   private static boolean compiled;
   private static boolean broken;
   private static boolean forceFallback;
   private static int filesGeneration = -1;
   private static int program;
   private static int uTexel;
   private static int uLevels;
   private static int uFill;
   private static int uInner;
   private static int previous;

   public static boolean usable() {
      if(!forceFallback && !OptiFineCompat.shadersActive()) {
         if(!compiled || filesGeneration != AssetSource.generation()) {
            compile();
         }

         return program != 0 && !broken;
      } else {
         return false;
      }
   }

   public static void begin(int levels, float fill, float inner) {
      previous = GlSave.boundProgram();
      GL20.glUseProgram(program);
      GL20.glUniform2f(uTexel, 0.001953125F, 0.0013020834F);
      GL20.glUniform1f(uLevels, (float)Math.max(1, levels));
      GL20.glUniform1f(uFill, Math.max(0.0F, Math.min(1.0F, fill)));
      GL20.glUniform1f(uInner, Math.max(0.05F, inner));
   }

   public static void end() {
      GL20.glUseProgram(previous);
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
      } else {
         String vertex = ShaderFiles.text("look/lineart.vsh");
         String fragment = ShaderFiles.text("look/lineart.fsh");
         if(vertex != null && fragment != null) {
            int before = GlSave.boundProgram();

            try {
               int vs = shader('謱', vertex);
               int fs = shader('謰', fragment);
               if(vs != 0 && fs != 0) {
                  int p = GL20.glCreateProgram();
                  GL20.glAttachShader(p, vs);
                  GL20.glAttachShader(p, fs);
                  GL20.glLinkProgram(p);
                  if(GL20.glGetProgrami(p, '讂') == 0) {
                     FuriahsVisuals.LOG.warn("Line art shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                     fail("the shader did not link");
                     return;
                  }

                  program = p;
                  uTexel = GL20.glGetUniformLocation(p, "uTexel");
                  uLevels = GL20.glGetUniformLocation(p, "uLevels");
                  uFill = GL20.glGetUniformLocation(p, "uFill");
                  uInner = GL20.glGetUniformLocation(p, "uInner");
                  GL20.glUseProgram(p);
                  GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0);
                  return;
               }

               fail("the shader did not compile");
            } catch (LinkageError | RuntimeException var9) {
               FuriahsVisuals.LOG.warn("Line art shader setup failed", var9);
               fail("shader setup failed");
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
         FuriahsVisuals.LOG.warn("Line art shader failed to compile: {}", new Object[]{GL20.glGetShaderInfoLog(s, 2048)});
         return 0;
      } else {
         return s;
      }
   }

   private static void fail(String reason) {
      if(!broken) {
         broken = true;
         program = 0;
         FuriahsVisuals.LOG.warn("Line art shader switched off ({}); portraits show in full color instead", new Object[]{reason});
      }

   }

   public static String stateForTest() {
      if(forceFallback) {
         return "fallback (asked)";
      } else if(OptiFineCompat.shadersActive()) {
         return "fallback (shader pack)";
      } else {
         if(!compiled || filesGeneration != AssetSource.generation()) {
            compile();
         }

         return program != 0 && !broken?"shader":"fallback (no shader)";
      }
   }

   public static void forceFallbackForTest(boolean on) {
      forceFallback = on;
   }
}
