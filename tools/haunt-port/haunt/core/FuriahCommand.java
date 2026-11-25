package mix.client.mixui.integration.haunt.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.core.TestDummy;
import mix.client.mixui.integration.haunt.gui.GuiCheck;
import mix.client.mixui.integration.haunt.gui.GuiColors;
import mix.client.mixui.integration.haunt.gui.UiProbe;
import mix.client.mixui.integration.haunt.haunt.HitReg;
import mix.client.mixui.integration.haunt.haunt.Jarvis;
import mix.client.mixui.integration.haunt.hook.Hooks;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.fps.FpsOptimizer;
import mix.client.mixui.integration.haunt.module.watermark.IslandCheck;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

final class FuriahCommand extends CommandBase {
   public String getCommandName() {
      return "haunt";
   }

   public String getCommandUsage(ICommandSender sender) {
      return "/haunt [jarvis <pedido> | fps [optimize|stop|restore] | latency | movehud | dummy [remove|jump|run|kill [crit]] | weapon [reload|play <clip>] | hooks | gui [check|overlay|colors]]";
   }

   public List<String> getCommandAliases() {
      return Arrays.asList(new String[]{"hauntvisuals", "furiah", "furiahsvisuals"});
   }

   public boolean canCommandSenderUseCommand(ICommandSender sender) {
      return true;
   }

   public void processCommand(ICommandSender sender, String[] args) throws CommandException {
      if(FuriahsVisuals.instance != null && FuriahsVisuals.instance.isRunning()) {
         Minecraft mc = Minecraft.getMinecraft();
         if(args.length == 0) {
            FuriahsVisuals.instance.openMenuNextTick();
         } else if(!"movehud".equalsIgnoreCase(args[0]) && !"hud".equalsIgnoreCase(args[0])) {
            if("jarvis".equalsIgnoreCase(args[0])) {
               Jarvis.handle(String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 1, args.length)));
            } else if("fps".equalsIgnoreCase(args[0])) {
               String a = args.length > 1?args[1].toLowerCase(Locale.ROOT):"";
               FpsOptimizer o = FpsOptimizer.INSTANCE;
               if(!a.equals("optimize") && !a.equals("start")) {
                  if(a.equals("stop")) {
                     o.stop();
                  } else if(a.equals("restore")) {
                     o.restoreLast();
                  } else {
                     FuriahsVisuals.chat(String.format(Locale.ROOT, "%.0f FPS now, %.0f average, %.0f 1%% low. %s", new Object[]{Float.valueOf(o.liveFps()), Float.valueOf(o.fps()), Float.valueOf(o.low1()), o.status()}));
                  }
               } else {
                  o.start();
               }
            } else if("latency".equalsIgnoreCase(args[0]) || "ping".equalsIgnoreCase(args[0])) {
               if(args.length > 1 && "reset".equalsIgnoreCase(args[1])) {
                  HitReg.INSTANCE.reset();
                  FuriahsVisuals.chat("Latency measurements cleared.");
               } else {
                  for(String line : HitReg.INSTANCE.report()) {
                     FuriahsVisuals.chat(line);
                  }
               }
            } else if("hooks".equalsIgnoreCase(args[0])) {
               List<String> on = new ArrayList(Hooks.INSTALLED);
               Collections.sort(on);
               FuriahsVisuals.chat("Hooks active (" + on.size() + "): " + (on.isEmpty()?"none":String.join(", ", on)));
            } else if("gui".equalsIgnoreCase(args[0])) {
               String what = args.length > 1?args[1].toLowerCase(Locale.ROOT):"";
               if(what.equals("check") && args.length > 2 && args[2].equalsIgnoreCase("island")) {
                  if(!IslandCheck.start("the menu was not checked", 0)) {
                     FuriahsVisuals.chat("The island\'s check needs a world and the WaterMark on.");
                  }
               } else if(what.equals("check")) {
                  GuiCheck.request();
               } else if(what.equals("overlay")) {
                  UiProbe.overlay = !UiProbe.overlay;
                  FuriahsVisuals.chat(UiProbe.overlay?"GUI overlay on: the menu outlines every control and text, overlapping ones in red.":"GUI overlay off.");
               } else if(!what.equals("colors") && !what.equals("colours")) {
                  FuriahsVisuals.chat("Usage: /haunt gui check [island] | overlay | colors");
               } else {
                  GuiColors.request();
               }
            } else if(!"weapon".equalsIgnoreCase(args[0]) && !"weapons".equalsIgnoreCase(args[0])) {
               if(!"dummy".equalsIgnoreCase(args[0])) {
                  FuriahsVisuals.chat("Usage: " + this.getCommandUsage(sender));
               } else {
                  String action = args.length > 1?args[1].toLowerCase(Locale.ROOT):"";
                  if(!action.isEmpty() && !action.equals("spawn")) {
                     if(action.equals("remove")) {
                        TestDummy.remove(mc, true);
                     } else if(action.equals("run")) {
                        if(TestDummy.runAround(mc, Long.MAX_VALUE, true)) {
                           FuriahsVisuals.chat("The test dummy runs circles around you to try Trails; /haunt dummy remove stops it.");
                        }
                     } else if(action.equals("kill")) {
                        boolean crit = args.length > 2 && "crit".equalsIgnoreCase(args[2]);
                        if(!mc.isSingleplayer()) {
                           FuriahsVisuals.chat("The test dummy only works in singleplayer.");
                        } else if(TestDummy.isDead()) {
                           FuriahsVisuals.chat("The test dummy is already dead; it comes back in a moment.");
                        } else if(!FuriahsVisuals.instance.getKillEffects().isEnabled()) {
                           FuriahsVisuals.chat("Kill Effects is switched off.");
                        } else {
                           FuriahsVisuals.instance.getKillEffects().testKill(mc, crit);
                        }
                     } else if(action.equals("jump")) {
                        BooleanSetting jumps = dummyJumpSetting();
                        boolean on = !TestDummy.isJumping();
                        if(jumps != null) {
                           jumps.set(on);
                        } else {
                           TestDummy.setJumping(on);
                        }

                        FuriahsVisuals.chat(on?"The test dummy now jumps every 2 seconds.":"The test dummy stopped jumping.");
                     } else {
                        FuriahsVisuals.chat("Usage: " + this.getCommandUsage(sender));
                     }
                  } else {
                     TestDummy.spawn(mc);
                  }
               }
            } else {
               Module module = FuriahsVisuals.instance.getModules().get("weapons");
               if(module instanceof WeaponsModule) {
                  FuriahsVisuals.chat(((WeaponsModule)module).command((String[])Arrays.copyOfRange(args, 1, args.length)));
               }
            }
         } else if(mc.theWorld != null) {
            FuriahsVisuals.instance.openMoveHudNextTick();
         }
      } else {
         FuriahsVisuals.chat("HAUN’T VISUALS is not initialized.");
      }

   }

   private static BooleanSetting dummyJumpSetting() {
      Module module = FuriahsVisuals.instance.getModules().get("landing_impact");
      if(module == null) {
         return null;
      } else {
         for(Setting setting : module.getSettings()) {
            if(setting instanceof BooleanSetting && setting.getId().equals("dummyJumps")) {
               return (BooleanSetting)setting;
            }
         }

         return null;
      }
   }
}
