package mix.client.mixui.integration.haunt.standalone;

import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.haunt.HitReg;
import mix.client.mixui.integration.haunt.hook.Bridge;
import mix.client.mixui.integration.haunt.hook.Install;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public final class ForgeClientEvents {
   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if(event.phase == Phase.START) {
         HitReg.syncLook(Minecraft.getMinecraft());
      }

      if(event.phase == Phase.END && FuriahsVisuals.instance != null) {
         Install.tickEnd();
      }

   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent event) {
      if(FuriahsVisuals.instance != null) {
         if(event.phase == Phase.START) {
            Install.renderTickStart(event.renderTickTime);
         } else if(event.phase == Phase.END) {
            Install.renderTickEnd();
            HitReg.syncLook(Minecraft.getMinecraft());
         }
      }

   }

   @SubscribeEvent
   public void onRenderWorldLast(RenderWorldLastEvent event) {
      if(FuriahsVisuals.instance != null) {
         Install.renderWorldLast(event.partialTicks);
      }

   }

   @SubscribeEvent
   public void onOverlayPre(Pre event) {
      if(FuriahsVisuals.instance != null && event.type != null && Install.overlayPre(event.type.name())) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onOverlayPost(Post event) {
      if(FuriahsVisuals.instance != null && event.type != null) {
         Install.overlayPost(event.type.name());
      }

   }

   @SubscribeEvent
   public void onAttack(AttackEntityEvent event) {
      if(FuriahsVisuals.instance != null && event.entityPlayer != null) {
         Install.attack(event.entityPlayer, event.target);
      }

   }

   @SubscribeEvent
   public void onLivingDeath(LivingDeathEvent event) {
      if(FuriahsVisuals.instance != null && event.entityLiving instanceof EntityPlayer && event.entityLiving.worldObj != null && event.entityLiving.worldObj.isRemote) {
         Install.livingDeath((EntityPlayer)event.entityLiving);
      }

   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent event) {
      if(FuriahsVisuals.instance != null && Bridge.active()) {
         Install.chat(event.message, event.type);
      }

   }

   @SubscribeEvent
   public void onKeyInput(KeyInputEvent event) {
      if(FuriahsVisuals.instance != null) {
         Install.keyInput();
      }

   }

   @SubscribeEvent
   public void onGuiOpen(GuiOpenEvent event) {
      if(FuriahsVisuals.instance != null && Install.guiOpen(event.gui)) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onMouse(MouseEvent event) {
      if(FuriahsVisuals.instance != null && Install.mouse(event.button, event.buttonstate, event.dwheel)) {
         event.setCanceled(true);
      }

   }
}
