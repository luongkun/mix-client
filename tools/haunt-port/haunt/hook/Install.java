package mix.client.mixui.integration.haunt.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.hook.Bridge;
import mix.client.mixui.integration.haunt.hook.CameraHooks;
import mix.client.mixui.integration.haunt.hook.FvProfiler;
import mix.client.mixui.integration.haunt.hook.Hooks;
import mix.client.mixui.integration.haunt.hook.HudHooks;
import mix.client.mixui.integration.haunt.hook.InputHooks;
import mix.client.mixui.integration.haunt.hook.ParticleSwaps;
import mix.client.mixui.integration.haunt.hook.RenderSwaps;
import mix.client.mixui.integration.haunt.hook.ResourceHooks;
import mix.client.mixui.integration.haunt.hook.SkinSwap;
import mix.client.mixui.integration.haunt.hook.Swap;
import mix.client.mixui.integration.haunt.hook.WorldHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IChatComponent;

public final class Install {
   static final String[] PROFILER_HOOKS = new String[]{"beforeHand", "beforeHud", "lightmapStart", "lightmapEnd"};
   private static final Field PROFILER = profilerField();
   private static boolean installed;
   private static boolean pending;

   private static Field profilerField() {
      Field f = Hooks.field(Minecraft.class, new String[]{"mcProfiler"});
      if(f != null) {
         return f;
      } else {
         for(Field c : Minecraft.class.getDeclaredFields()) {
            if(c.getType() == Profiler.class && !Modifier.isStatic(c.getModifiers())) {
               c.setAccessible(true);
               return c;
            }
         }

         return null;
      }
   }

   public static boolean installed() {
      return installed;
   }

   public static synchronized void install() {
      if(!installed) {
         Minecraft mc = Minecraft.getMinecraft();
         if(mc.ingameGUI != null && mc.getRenderManager() != null && mc.effectRenderer != null && mc.mouseHelper != null && mc.getItemRenderer() != null) {
            pending = false;
            installed = true;
            Bridge.setActive(true);
            InputHooks.reset();
            step("resources", new Runnable() {
               public void run() {
                  ResourceHooks.install();
               }
            });
            step("profiler", new Runnable() {
               public void run() {
                  FvProfiler f = (FvProfiler)Swap.upgrade(Minecraft.getMinecraft().mcProfiler, Profiler.class, FvProfiler.class);
                  if(f != null && Install.PROFILER != null) {
                     Swap.set(Install.PROFILER, Minecraft.getMinecraft(), f);
                     Hooks.on(Install.PROFILER_HOOKS);
                  } else {
                     FuriahsVisuals.LOG.warn("Hooks: the profiler could not be swapped; the frame hooks (sky, fog, weather, HUD order, hand) stay off");
                  }
               }
            });
            step("world", new Runnable() {
               public void run() {
                  WorldHooks.sync(true);
                  ParticleSwaps.syncWorld(true);
               }
            });
            step("HUD", new Runnable() {
               public void run() {
                  HudHooks.install();
               }
            });
            step("particles", new Runnable() {
               public void run() {
                  ParticleSwaps.install();
               }
            });
            step("rendering", new Runnable() {
               public void run() {
                  RenderSwaps.install();
               }
            });
            step("camera", new Runnable() {
               public void run() {
                  CameraHooks.install();
               }
            });
            step("skins", new Runnable() {
               public void run() {
                  SkinSwap.install();
               }
            });
            FuriahsVisuals.LOG.info("Hooks in: {}", new Object[]{Hooks.INSTALLED});
         } else {
            pending = true;
         }
      }
   }

   public static synchronized void uninstall() {
      pending = false;
      if(installed) {
         installed = false;
         Bridge.setActive(false);
         step("skins", new Runnable() {
            public void run() {
               SkinSwap.uninstall();
            }
         });
         step("camera", new Runnable() {
            public void run() {
               CameraHooks.uninstall();
            }
         });
         step("rendering", new Runnable() {
            public void run() {
               RenderSwaps.uninstall();
            }
         });
         step("particles", new Runnable() {
            public void run() {
               ParticleSwaps.uninstall();
            }
         });
         step("HUD", new Runnable() {
            public void run() {
               HudHooks.uninstall();
            }
         });
         step("world", new Runnable() {
            public void run() {
               WorldHooks.restoreWeather();
               WorldHooks.restoreTemperature();
               WorldHooks.sync(false);
            }
         });
         step("profiler", new Runnable() {
            public void run() {
               Minecraft mc = Minecraft.getMinecraft();
               if(Swap.ours(mc.mcProfiler, FvProfiler.class) && Install.PROFILER != null) {
                  Swap.set(Install.PROFILER, mc, Swap.downgrade(mc.mcProfiler, Profiler.class));
               }

               Hooks.off(Install.PROFILER_HOOKS);
            }
         });
         step("resources", new Runnable() {
            public void run() {
               ResourceHooks.uninstall();
            }
         });
         Hooks.INSTALLED.clear();
      }
   }

   private static void step(String what, Runnable r) {
      try {
         r.run();
      } catch (Throwable var3) {
         FuriahsVisuals.LOG.error("Hooks: the {} step failed", new Object[]{what, var3});
      }

   }

   static void sync() {
      if(installed) {
         WorldHooks.sync(true);
         ParticleSwaps.syncWorld(true);
         SkinSwap.sync();
         InputHooks.checkScreen();
         HudHooks.syncScreen();
      }
   }

   public static void tickEnd() {
      if(pending) {
         install();
      }

      if(installed) {
         ParticleSwaps.tickEnd();

         try {
            sync();
            if(Bridge.active()) {
               Bridge.events().onClientTick();
            }
         } catch (Throwable var1) {
            FuriahsVisuals.LOG.error("Hooks: the client tick threw", var1);
         }

      }
   }

   public static boolean profilerHooks() {
      return Hooks.has("beforeHud");
   }

   public static void renderTickStart(float partialTicks) {
      if(installed && !profilerHooks()) {
         try {
            Bridge.frameStart();
         } catch (Throwable var2) {
            FuriahsVisuals.LOG.error("Hooks: renderTickStart threw", var2);
         }

      }
   }

   public static void renderTickEnd() {
      if(installed && !profilerHooks()) {
         try {
            Bridge.frameEnd();
         } catch (Throwable var1) {
            FuriahsVisuals.LOG.error("Hooks: renderTickEnd threw", var1);
         }

      }
   }

   public static void renderWorldLast(float partialTicks) {
      if(installed && Bridge.active()) {
         try {
            Bridge.events().onRenderWorldLast(partialTicks);
            Bridge.events().onRenderWorldLastFinal();
         } catch (Throwable var2) {
            FuriahsVisuals.LOG.error("Hooks: Forge RenderWorldLast event threw", var2);
         }

      }
   }

   public static boolean guiOpen(GuiScreen gui) {
      if(installed && Bridge.active()) {
         boolean cancel = false;

         try {
            cancel = Bridge.events().onGuiOpen(gui);
         } catch (Throwable var3) {
            FuriahsVisuals.LOG.error("Hooks: onGuiOpen threw", var3);
         }

         if(!cancel) {
            InputHooks.screenHandled(gui);
         }

         return cancel;
      } else {
         return false;
      }
   }

   public static boolean mouse(int button, boolean pressed, int dwheel) {
      if(installed && Bridge.active() && (!profilerHooks() || !InputHooks.mouseAvailable())) {
         try {
            return Bridge.events().onMouse(button, pressed, dwheel);
         } catch (Throwable var4) {
            FuriahsVisuals.LOG.error("Hooks: onMouse threw", var4);
            return false;
         }
      } else {
         return false;
      }
   }

   public static void attack(EntityPlayer player, Entity target) {
      if(installed) {
         ParticleSwaps.attack();
         if(Bridge.active()) {
            Bridge.events().onAttack(player, target);
         }

      }
   }

   public static boolean overlayPre(String element) {
      if(installed && Bridge.active()) {
         try {
            return HudHooks.forgePre(element);
         } catch (Throwable var2) {
            FuriahsVisuals.LOG.error("Hooks: overlay pre threw", var2);
            return false;
         }
      } else {
         return false;
      }
   }

   public static void overlayPost(String element) {
      if(installed && Bridge.active()) {
         try {
            HudHooks.forgePost(element);
         } catch (Throwable var2) {
            FuriahsVisuals.LOG.error("Hooks: overlay post threw", var2);
         }

      }
   }

   public static void livingDeath(EntityPlayer player) {
      if(installed && Bridge.active()) {
         try {
            Bridge.events().onLivingDeath(player);
         } catch (Throwable var2) {
            FuriahsVisuals.LOG.error("Hooks: onLivingDeath threw", var2);
         }
      }

   }

   public static void chat(IChatComponent message, byte type) {
      if(installed && Bridge.active()) {
         Bridge.events().onChat(message, type);
      }

   }

   public static void keyInput() {
      if(installed) {
         InputHooks.keyInput();
      }

   }
}
