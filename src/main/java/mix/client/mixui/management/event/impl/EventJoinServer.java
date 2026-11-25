package mix.client.mixui.management.event.impl;

import mix.client.mixui.management.event.Event;

public class EventJoinServer extends Event {

	private String ip;
	
	public EventJoinServer(String ip) {
		this.ip = ip;
	}

	public String getIp() {
		return ip;
	}
}
