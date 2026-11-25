package mix.client.mixui.injection.interfaces;

import mix.client.mixui.management.mods.impl.skin3d.layers.BodyLayerFeatureRenderer;
import mix.client.mixui.management.mods.impl.skin3d.layers.HeadLayerFeatureRenderer;

public interface IMixinRenderPlayer {
	public boolean hasThinArms();
	public HeadLayerFeatureRenderer getHeadLayer();
	public BodyLayerFeatureRenderer getBodyLayer();
}