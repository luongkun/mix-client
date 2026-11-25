package mix.client.mixui.integration.haunt.standalone.proxy;

import java.io.File;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.standalone.ForgeClientEvents;
import mix.client.mixui.integration.haunt.standalone.proxy.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public final class ClientProxy extends CommonProxy {
   public void initialize() {
      MinecraftForge.EVENT_BUS.register(new ForgeClientEvents());
      FMLCommonHandler.instance().bus().register(new ForgeClientEvents());
      File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
      FuriahsVisuals.start(configDir).resume();
      FuriahsVisuals.LOG.info("HAUN\'T VISUALS {} initialized (Forge 1.8.9).", new Object[]{"2.0.0"});
   }
}
