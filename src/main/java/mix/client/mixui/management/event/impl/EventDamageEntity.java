package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;
import net.minecraft.entity.Entity;

public class EventDamageEntity extends Event {

	private Entity entity;
	
	public EventDamageEntity(Entity entity) {
		this.entity = entity;
	}

	public Entity getEntity() {
		return entity;
	}
}
