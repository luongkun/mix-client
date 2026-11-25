package mix.client.mixui.visuals.furiah.module;

import mix.client.mixui.visuals.furiah.module.watermark.IslandIcons;
import mix.client.mixui.visuals.furiah.setting.BooleanSetting;
import mix.client.mixui.visuals.furiah.setting.SettingStyle;

public final class MiniSection implements SettingStyle {
    private final MiniSections owner;
    private final String id;
    private final String name;
    private final IslandIcons.Icon icon;
    private final boolean dividerBefore;
    private BooleanSetting enabled;

    MiniSection(MiniSections owner, String id, String name, IslandIcons.Icon icon, boolean dividerBefore) {
        this.owner = owner;
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.dividerBefore = dividerBefore;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public IslandIcons.Icon getIcon() { return icon; }
    public boolean dividerBefore() { return dividerBefore; }
    public BooleanSetting getSwitch() { return enabled; }

    public void setSwitch(BooleanSetting enabled) {
        this.enabled = enabled == null ? null : enabled.sectionSwitch();
    }

    public Module module() { return owner.module; }
    public boolean on() { return owner.module.isEnabled() && (enabled == null || enabled.get()); }

    @Override public String styleName() { return name; }
    @Override public boolean isShown() { return owner.current() == this; }
    @Override public boolean isActive() { return enabled == null || enabled.get(); }
    @Override public boolean isMultiple() { return true; }
    @Override public void show() { owner.select(this); }
    @Override public String kind() { return "mini section"; }
    @Override public String kinds() { return "mini sections"; }
    @Override public String toString() { return id; }
}
