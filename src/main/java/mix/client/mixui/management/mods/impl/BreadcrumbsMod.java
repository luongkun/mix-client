package mix.client.mixui.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventMotionUpdate;
import mix.client.mixui.management.event.impl.EventRender3D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.ColorSetting;
import mix.client.mixui.management.mods.settings.impl.NumberSetting;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.Render3DUtils;
import net.minecraft.util.Vec3;

public class BreadcrumbsMod extends Mod {

	private List<Vec3> path = new ArrayList<>();
	
	private BooleanSetting customColorSetting = new BooleanSetting(TranslateText.CUSTOM_COLOR, this, false);
	private ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, Color.RED, false);
    
    private BooleanSetting timeoutSetting = new BooleanSetting(TranslateText.TIMEOUT, this, true);
    private NumberSetting timeSetting = new NumberSetting(TranslateText.TIME, this, 15, 1, 150, true);
    
	public BreadcrumbsMod() {
		super(TranslateText.BREADCRUMBS, TranslateText.BREADCRUMBS_DESCRIPTION, ModCategory.RENDER);
	}
	
	@EventTarget
	public void onRender3D(EventRender3D event) {
		
		AccentColor currentColor = Mix.getInstance().getColorManager().getCurrentColor();
		
		Render3DUtils.renderBreadCrumbs(path, customColorSetting.isToggled() ? ColorUtils.applyAlpha(colorSetting.getColor(), 255) : currentColor.getInterpolateColor());
	}
	
	@EventTarget
	public void onMotionUpdate(EventMotionUpdate event) {
		
        if (mc.thePlayer.lastTickPosX != mc.thePlayer.posX || mc.thePlayer.lastTickPosY != mc.thePlayer.posY || mc.thePlayer.lastTickPosZ != mc.thePlayer.posZ) {
            path.add(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        }
        
        if (timeoutSetting.isToggled()) {
            while (path.size() > timeSetting.getValueInt()) {
                path.remove(0);
            }
        }
	}
}
