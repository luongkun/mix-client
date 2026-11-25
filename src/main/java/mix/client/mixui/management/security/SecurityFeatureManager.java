package mix.client.mixui.management.security;

import java.util.ArrayList;

import mix.client.mixui.management.security.impl.DemoSecurity;
import mix.client.mixui.management.security.impl.ExplosionSecurity;
import mix.client.mixui.management.security.impl.Log4jSecurity;
import mix.client.mixui.management.security.impl.ParticleSecurity;
import mix.client.mixui.management.security.impl.ResourcePackSecurity;
import mix.client.mixui.management.security.impl.TeleportSecurity;

public class SecurityFeatureManager {

	private ArrayList<SecurityFeature> features = new ArrayList<SecurityFeature>();
	
	public SecurityFeatureManager() {
		features.add(new DemoSecurity());
		features.add(new ExplosionSecurity());
		features.add(new Log4jSecurity());
		features.add(new ParticleSecurity());
		features.add(new ResourcePackSecurity());
		features.add(new TeleportSecurity());
	}

	public ArrayList<SecurityFeature> getFeatures() {
		return features;
	}
}
