package mix.client.mixui.integration.haunt.module.fps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.Display;

public final class FpsOptimizer {
   public static final FpsOptimizer INSTANCE = new FpsOptimizer();
   private static final int FRAMES = 4096;
   private final long[] frameNanos = new long[4096];
   private int frameNext;
   private int frameCount;
   private long lastFrame;
   private final List<FpsOptimizer.Step> plan = new ArrayList();
   private int stepIndex = -1;
   private FpsOptimizer.Phase phase = FpsOptimizer.Phase.IDLE;
   private long phaseStart;
   private float baseline;
   private float before;
   private JsonObject snapshot;
   private final List<String> log = new ArrayList();
   private String status = "Ready. Press OPTIMIZE NOW, then close the menu and play normally for about half a minute.";
   private boolean allowVisual;
   private float targetFps = 300.0F;
   private File file;
   private JsonObject learned = new JsonObject();
   private final List<JsonObject> history = new ArrayList();
   private JsonObject current;
   private boolean loaded;

   public void frame() {
      long now = System.nanoTime();
      if(this.lastFrame != 0L) {
         long d = now - this.lastFrame;
         if(d > 0L && d < 2000000000L) {
            this.frameNanos[this.frameNext] = d;
            this.frameNext = (this.frameNext + 1) % 4096;
            this.frameCount = Math.min(4096, this.frameCount + 1);
         }
      }

      this.lastFrame = now;
   }

   private void clearFrames() {
      this.frameCount = 0;
      this.frameNext = 0;
   }

   public float fps() {
      if(this.frameCount == 0) {
         return 0.0F;
      } else {
         double s = 0.0D;

         for(int i = 0; i < this.frameCount; ++i) {
            s += (double)this.frameNanos[i];
         }

         return (float)(1.0E9D * (double)this.frameCount / s);
      }
   }

   public float low1() {
      if(this.frameCount < 50) {
         return this.fps();
      } else {
         long[] c = Arrays.copyOf(this.frameNanos, this.frameCount);
         Arrays.sort(c);
         int n = Math.max(1, this.frameCount / 100);
         double s = 0.0D;

         for(int i = c.length - n; i < c.length; ++i) {
            s += (double)c[i];
         }

         return (float)(1.0E9D * (double)n / s);
      }
   }

   public float liveFps() {
      int n = Math.min(this.frameCount, 120);
      if(n == 0) {
         return 0.0F;
      } else {
         double s = 0.0D;

         for(int i = 1; i <= n; ++i) {
            s += (double)this.frameNanos[(this.frameNext - i + 4096) % 4096];
         }

         return (float)(1.0E9D * (double)n / s);
      }
   }

   private static JsonObject capture(Minecraft mc) {
      GameSettings g = mc.gameSettings;
      JsonObject o = new JsonObject();
      o.addProperty("limitFramerate", Integer.valueOf(g.limitFramerate));
      o.addProperty("vsync", Boolean.valueOf(g.enableVsync));
      o.addProperty("vbo", Boolean.valueOf(g.useVbo));
      o.addProperty("particles", Integer.valueOf(g.particleSetting));
      o.addProperty("smoothLighting", Integer.valueOf(g.ambientOcclusion));
      o.addProperty("renderDistance", Integer.valueOf(g.renderDistanceChunks));
      o.addProperty("fancy", Boolean.valueOf(g.fancyGraphics));
      o.addProperty("time", Long.valueOf(System.currentTimeMillis()));
      return o;
   }

   private static void restore(Minecraft mc, JsonObject o) {
      GameSettings g = mc.gameSettings;
      boolean reload = false;
      g.limitFramerate = o.get("limitFramerate").getAsInt();
      if(g.enableVsync != o.get("vsync").getAsBoolean()) {
         g.enableVsync = o.get("vsync").getAsBoolean();
         Display.setVSyncEnabled(g.enableVsync);
      }

      reload = reload | g.useVbo != o.get("vbo").getAsBoolean();
      g.useVbo = o.get("vbo").getAsBoolean();
      g.particleSetting = o.get("particles").getAsInt();
      reload = reload | g.ambientOcclusion != o.get("smoothLighting").getAsInt();
      g.ambientOcclusion = o.get("smoothLighting").getAsInt();
      reload = reload | g.renderDistanceChunks != o.get("renderDistance").getAsInt();
      g.renderDistanceChunks = o.get("renderDistance").getAsInt();
      reload = reload | g.fancyGraphics != o.get("fancy").getAsBoolean();
      g.fancyGraphics = o.get("fancy").getAsBoolean();
      if(reload && mc.renderGlobal != null) {
         mc.renderGlobal.loadRenderers();
      }

      g.saveOptions();
   }

   private void buildPlan(Minecraft mc) {
      this.plan.clear();
      this.plan.add(new FpsOptimizer.Step("uncap", "Frame rate limit off and VSync off", false) {
         boolean applies(Minecraft mc) {
            return mc.gameSettings.limitFramerate < 260 || mc.gameSettings.enableVsync;
         }

         void apply(Minecraft mc) {
            mc.gameSettings.limitFramerate = 260;
            if(mc.gameSettings.enableVsync) {
               mc.gameSettings.enableVsync = false;
               Display.setVSyncEnabled(false);
            }

         }
      });
      this.plan.add(new FpsOptimizer.Step("vbo", "Vertex buffers (VBOs) on", false) {
         boolean applies(Minecraft mc) {
            return !mc.gameSettings.useVbo;
         }

         void apply(Minecraft mc) {
            mc.gameSettings.useVbo = true;
            mc.renderGlobal.loadRenderers();
         }
      });
      this.plan.add(new FpsOptimizer.Step("particles", "Particles: Decreased", true) {
         boolean applies(Minecraft mc) {
            return mc.gameSettings.particleSetting == 0;
         }

         void apply(Minecraft mc) {
            mc.gameSettings.particleSetting = 1;
         }
      });
      this.plan.add(new FpsOptimizer.Step("smooth", "Smooth Lighting: Minimum", true) {
         boolean applies(Minecraft mc) {
            return mc.gameSettings.ambientOcclusion == 2;
         }

         void apply(Minecraft mc) {
            mc.gameSettings.ambientOcclusion = 1;
            mc.renderGlobal.loadRenderers();
         }
      });
      this.plan.add(new FpsOptimizer.Step("distance", "Render distance 2 chunks shorter", true) {
         boolean applies(Minecraft mc) {
            return mc.gameSettings.renderDistanceChunks > 10 && FpsOptimizer.this.before < FpsOptimizer.this.targetFps;
         }

         void apply(Minecraft mc) {
            mc.gameSettings.renderDistanceChunks -= 2;
            mc.renderGlobal.loadRenderers();
         }
      });
      this.plan.add(new FpsOptimizer.Step("fast", "Graphics: Fast", true) {
         boolean applies(Minecraft mc) {
            return mc.gameSettings.fancyGraphics && FpsOptimizer.this.before < FpsOptimizer.this.targetFps;
         }

         void apply(Minecraft mc) {
            mc.gameSettings.fancyGraphics = false;
            mc.renderGlobal.loadRenderers();
         }
      });
   }

   public boolean running() {
      return this.phase != FpsOptimizer.Phase.IDLE;
   }

   public String status() {
      return this.status;
   }

   public List<String> log() {
      return this.log;
   }

   public int historySize() {
      return this.history.size();
   }

   public void configure(boolean allowVisual, float targetFps) {
      this.allowVisual = allowVisual;
      this.targetFps = targetFps;
   }

   public void start() {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.theWorld != null && mc.gameSettings != null) {
         if(!this.running()) {
            this.load();
            this.snapshot = capture(mc);
            this.buildPlan(mc);
            this.log.clear();
            this.stepIndex = -1;
            this.enter(FpsOptimizer.Phase.BASELINE);
            this.status = "Measuring your current frame rate... close the menu and play normally.";
            FuriahsVisuals.chat("FPS Optimization: measuring. Close the menu and play normally; it takes about half a minute. /haunt fps stop ends it.");
         }
      } else {
         this.status = "Join a world first: the optimizer measures the game while you play.";
      }

   }

   public void stop() {
      if(this.running()) {
         Minecraft mc = Minecraft.getMinecraft();
         if(this.phase != FpsOptimizer.Phase.BASELINE && this.stepIndex >= 0 && this.current != null) {
            restore(mc, this.current);
         }

         this.log.add("Stopped by you; the change being tested was put back.");
         this.finish(mc, "Stopped.");
      }

   }

   public void restoreLast() {
      Minecraft mc = Minecraft.getMinecraft();
      this.load();
      if(this.running()) {
         this.stop();
      }

      if(this.history.isEmpty()) {
         this.status = "Nothing to restore: no optimization has been run yet.";
      } else if(mc.gameSettings != null) {
         JsonObject last = (JsonObject)this.history.remove(this.history.size() - 1);
         restore(mc, last.getAsJsonObject("before"));
         this.save();
         this.status = "Restored the settings you had before the run of " + new Date(last.get("time").getAsLong()) + ".";
         FuriahsVisuals.chat("FPS Optimization: " + this.status);
      }

   }

   private void enter(FpsOptimizer.Phase p) {
      this.phase = p;
      this.phaseStart = System.nanoTime();
      this.clearFrames();
   }

   private float secondsIn() {
      return (float)(System.nanoTime() - this.phaseStart) / 1.0E9F;
   }

   public void tick(Minecraft mc) {
      if(this.running()) {
         if(mc.theWorld == null) {
            this.stop();
         } else if(mc.currentScreen != null) {
            this.phaseStart = System.nanoTime();
            this.clearFrames();
         } else {
            switch(this.phase.ordinal()) {
            case 1:
               if(this.secondsIn() > 5.0F && this.frameCount > 60) {
                  this.baseline = this.before = this.fps();
                  this.log.add(String.format(Locale.ROOT, "Start: %.0f FPS (1%% low %.0f).", new Object[]{Float.valueOf(this.baseline), Float.valueOf(this.low1())}));
                  this.next(mc);
               }
               break;
            case 2:
               if(this.secondsIn() > 2.0F) {
                  this.enter(FpsOptimizer.Phase.MEASURE);
               }
               break;
            case 3:
               if(this.secondsIn() > 4.0F && this.frameCount > 60) {
                  this.judge(mc);
               }
            }
         }
      }

   }

   private void next(Minecraft mc) {
      while(true) {
         if(++this.stepIndex < this.plan.size()) {
            FpsOptimizer.Step s = (FpsOptimizer.Step)this.plan.get(this.stepIndex);
            if(s.visual && !this.allowVisual) {
               continue;
            }

            if(this.learned.has(s.id) && this.learned.get(s.id).getAsFloat() < -0.02F) {
               this.log.add(s.label + ": skipped, it made this game slower last time.");
               continue;
            }

            if(!s.applies(mc)) {
               continue;
            }

            this.current = capture(mc);
            s.apply(mc);
            this.status = "Testing: " + s.label + "...";
            this.enter(FpsOptimizer.Phase.SETTLE);
            return;
         }

         this.finish(mc, (String)null);
         return;
      }
   }

   private void judge(Minecraft mc) {
      FpsOptimizer.Step s = (FpsOptimizer.Step)this.plan.get(this.stepIndex);
      float after = this.fps();
      float gain = this.before > 0.0F?after / this.before - 1.0F:0.0F;
      boolean keep = s.visual?gain >= 0.03F:gain >= -0.03F;
      this.learned.addProperty(s.id, Float.valueOf(gain));
      if(keep) {
         this.log.add(String.format(Locale.ROOT, "%s: kept (%.0f -> %.0f FPS, %+.0f%%).", new Object[]{s.label, Float.valueOf(this.before), Float.valueOf(after), Float.valueOf(gain * 100.0F)}));
         this.before = Math.max(this.before, after);
      } else {
         restore(mc, this.current);
         this.log.add(String.format(Locale.ROOT, "%s: put back, it did not help (%.0f -> %.0f FPS).", new Object[]{s.label, Float.valueOf(this.before), Float.valueOf(after)}));
      }

      this.next(mc);
   }

   private void finish(Minecraft mc, String why) {
      boolean changed = false;
      if(this.snapshot != null && mc.gameSettings != null) {
         JsonObject now = capture(mc);

         for(String k : new String[]{"limitFramerate", "vsync", "vbo", "particles", "smoothLighting", "renderDistance", "fancy"}) {
            changed |= !now.get(k).equals(this.snapshot.get(k));
         }

         if(changed) {
            mc.gameSettings.saveOptions();
            JsonObject h = new JsonObject();
            h.addProperty("time", Long.valueOf(System.currentTimeMillis()));
            h.add("before", this.snapshot);
            h.add("after", now);
            JsonArray lines = new JsonArray();

            for(String l : this.log) {
               lines.add(new JsonPrimitive(l));
            }

            h.add("report", lines);
            this.history.add(h);

            while(this.history.size() > 10) {
               this.history.remove(0);
            }
         }
      }

      this.phase = FpsOptimizer.Phase.IDLE;
      this.stepIndex = -1;
      this.current = null;
      String result = String.format(Locale.ROOT, "%.0f -> %.0f FPS", new Object[]{Float.valueOf(this.baseline), Float.valueOf(this.before)});
      if(why == null) {
         this.log.add(changed?"Done: " + result + ". Restore puts everything back.":"Done: nothing measurably faster was found, nothing was changed.");
         this.status = changed?"Done: " + result + ".":"Done: no change helped, nothing was changed.";
      } else {
         this.status = why;
      }

      this.save();

      for(String l : this.log) {
         FuriahsVisuals.chat("FPS Optimization: " + l);
      }

   }

   private File file() {
      if(this.file == null) {
         this.file = new File(FuriahsVisuals.instance.getDataDir(), "fps-optimizer.json");
      }

      return this.file;
   }

   private void load() {
      if(!this.loaded) {
         this.loaded = true;
         File f = this.file();
         if(f.isFile()) {
            try {
               Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);

               try {
                  JsonElement e = (new JsonParser()).parse(r);
                  if(e != null && e.isJsonObject()) {
                     JsonObject o = e.getAsJsonObject();
                     if(o.has("learned") && o.get("learned").isJsonObject()) {
                        this.learned = o.getAsJsonObject("learned");
                     }

                     if(o.has("history") && o.get("history").isJsonArray()) {
                        for(Object ho : o.getAsJsonArray("history")) {
                           JsonElement h = (JsonElement)ho;
                           if(h.isJsonObject() && h.getAsJsonObject().has("before")) {
                              this.history.add(h.getAsJsonObject());
                           }
                        }
                     }
                  }
               } catch (Throwable var9) {
                  try {
                     r.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               r.close();
            } catch (Exception var10) {
               FuriahsVisuals.LOG.warn("FPS Optimization: its history could not be read; it starts fresh (your game settings are untouched)");
            }
         }
      }

   }

   private void save() {
      try {
         File f = this.file();
         f.getParentFile().mkdirs();
         JsonObject o = new JsonObject();
         o.add("learned", this.learned);
         JsonArray h = new JsonArray();

         for(JsonObject x : this.history) {
            h.add(x);
         }

         o.add("history", h);
         File tmp = new File(f.getPath() + ".tmp");
         Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);

         try {
            w.write(o.toString());
         } catch (Throwable var9) {
            try {
               w.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         w.close();
         if(!tmp.renameTo(f)) {
            f.delete();
            tmp.renameTo(f);
         }
      } catch (Exception var10) {
         FuriahsVisuals.LOG.warn("FPS Optimization: its history could not be saved");
      }

   }

   static enum Phase {
      IDLE,
      BASELINE,
      SETTLE,
      MEASURE;

      // $FF: synthetic method
      private static FpsOptimizer.Phase[] $values() {
         return new FpsOptimizer.Phase[]{IDLE, BASELINE, SETTLE, MEASURE};
      }
   }

   abstract static class Step {
      final String id;
      final String label;
      final boolean visual;

      Step(String id, String label, boolean visual) {
         this.id = id;
         this.label = label;
         this.visual = visual;
      }

      abstract boolean applies(Minecraft var1);

      abstract void apply(Minecraft var1);
   }
}
