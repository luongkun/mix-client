package mix.client.mixui.visuals.furiah.module.misc;

import net.minecraft.entity.EntityLivingBase;

public final class MiscHooks {
    public static boolean hideArmor(EntityLivingBase entity, int slot) {
        return false;
    }
}
