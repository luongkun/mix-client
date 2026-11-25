package probe;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Throwaway mod used to prove that a jar in mods/ is actually loaded by FML. */
@Mod(modid = "mixprobe", name = "Mix Probe", version = "1.0")
public class MixProbe {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("[MixProbe] MOD_LOADED mods=" + event.getModConfigurationDirectory());
    }
}
