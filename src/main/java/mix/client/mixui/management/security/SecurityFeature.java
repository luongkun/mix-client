package mix.client.mixui.management.security;

import mix.client.mixui.Mix;

public class SecurityFeature {

	public SecurityFeature() {
		Mix.getInstance().getEventManager().register(this);
	}
}
