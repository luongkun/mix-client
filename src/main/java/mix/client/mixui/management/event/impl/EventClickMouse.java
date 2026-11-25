package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventClickMouse extends Event {

	private int button;
	
	public EventClickMouse(int button) {
		this.button = button;
	}

	public int getButton() {
		return button;
	}
}
