package mix.client.mixui.injection.mixin.mixins.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import mix.client.mixui.injection.interfaces.ICachedHashcode;
import net.minecraft.block.properties.PropertyInteger;

@Mixin(PropertyInteger.class)
public class MixinPropertyInteger {

    @Overwrite
    public int hashCode() {
        return ((ICachedHashcode)((Object)this)).getCachedHashcode();
    }
}