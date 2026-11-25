package mix.client.mixui.visuals.furiah.module.kill;

import net.minecraft.entity.player.EntityPlayer;

public final class KillHooks {
    public static boolean skipPoof(EntityPlayer player) {
        return false;
    }
}
