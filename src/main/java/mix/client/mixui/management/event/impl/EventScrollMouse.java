package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventScrollMouse extends Event {

	private int amount;
	
	public EventScrollMouse(int amount) {
		this.amount = amount;
	}

	public int getAmount() {
		return amount;
	}
}