package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;
import net.minecraft.entity.EntityLivingBase;

public class EventLivingUpdate extends Event {
	
	private EntityLivingBase entity;
	
	public EventLivingUpdate(EntityLivingBase entity) {
		this.entity = entity;
	}

	public EntityLivingBase getEntity() {
		return entity;
	}
}