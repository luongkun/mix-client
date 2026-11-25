package mix.client.mixui.mobends.animation;

import mix.client.mixui.mobends.data.MoBends_EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;

public abstract class MoBendsAnimation {
	public abstract void animate(EntityLivingBase argEntity, ModelBase argModel, MoBends_EntityData argData);
	public abstract String getName();
}
