package mix.client.mixui.injection.interfaces;

import net.minecraft.client.multiplayer.WorldClient;

public interface IMixinRenderGlobal {
	WorldClient getWorldClient();
}
