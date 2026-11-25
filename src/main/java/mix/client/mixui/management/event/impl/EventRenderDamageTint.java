package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventRenderDamageTint extends Event {

	private float partialTicks;
	
	public EventRenderDamageTint(float partialTicks) {
		this.partialTicks = partialTicks;
	}

	public float getPartialTicks() {
		return partialTicks;
	}
}
