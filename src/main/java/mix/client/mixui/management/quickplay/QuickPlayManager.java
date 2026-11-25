package mix.client.mixui.management.quickplay;

import java.util.ArrayList;

import mix.client.mixui.management.quickplay.impl.ArcadeQuickPlay;
import mix.client.mixui.management.quickplay.impl.BedwarsQuickPlay;
import mix.client.mixui.management.quickplay.impl.DuelsQuickPlay;
import mix.client.mixui.management.quickplay.impl.MainLobbyQuickPlay;
import mix.client.mixui.management.quickplay.impl.MurderMysteryQuickPlay;
import mix.client.mixui.management.quickplay.impl.SkywarsQuickPlay;
import mix.client.mixui.management.quickplay.impl.TNTQuickPlay;
import mix.client.mixui.management.quickplay.impl.UHCQuickPlay;

public class QuickPlayManager {

	private ArrayList<QuickPlay> quickPlays = new ArrayList<QuickPlay>();
	
	public QuickPlayManager() {
		quickPlays.add(new ArcadeQuickPlay());
		quickPlays.add(new BedwarsQuickPlay());
		quickPlays.add(new DuelsQuickPlay());
		quickPlays.add(new MainLobbyQuickPlay());
		quickPlays.add(new MurderMysteryQuickPlay());
		quickPlays.add(new SkywarsQuickPlay());
		quickPlays.add(new TNTQuickPlay());
		quickPlays.add(new UHCQuickPlay());
	}

	public ArrayList<QuickPlay> getQuickPlays() {
		return quickPlays;
	}
}
