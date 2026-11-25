package mix.client.mixui.visuals;

import java.util.ArrayList;
import java.util.List;

import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.TextSetting;
import mix.client.mixui.visuals.furiah.core.FuriahRuntime;
import mix.client.mixui.visuals.furiah.module.Module;
import mix.client.mixui.visuals.furiah.setting.Setting;
import mix.client.mixui.visuals.furiah.setting.WidgetSetting;

public abstract class FuriahVisualMod extends Mod {
    protected final Module effect;
    private final TextSetting settingsStore;

    protected FuriahVisualMod(TranslateText name, TranslateText description, Module effect) {
        super(name, description, ModCategory.RENDER);
        this.effect = effect;
        setHide(true);
        FuriahRuntime.get().init();
        settingsStore = new TextSetting(TranslateText.FURIAH_SETTINGS, this, "{}");
        settingsStore.addListener(new Runnable() {
            @Override
            public void run() {
                loadSettings();
            }
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        loadSettings();
        effect.setEnabled(true);
    }

    @Override
    public void onDisable() {
        effect.setEnabled(false);
        saveSettings();
        super.onDisable();
    }

    public Module getEffect() {
        return effect;
    }

    public List<Setting> getFuriahSettings() {
        List<Setting> result = new ArrayList<Setting>();
        for (Setting setting : effect.getSettings()) {
            if (!"enabled".equals(setting.getId()) && !(setting instanceof WidgetSetting)
                    && setting.showInMenu() && setting.isVisible()) {
                result.add(setting);
            }
        }
        return result;
    }

    public void saveSettings() {
        settingsStore.setText(FuriahRuntime.save(effect).toString());
    }

    public void resetSettings() {
        for (Setting setting : effect.getSettings()) {
            if (!"enabled".equals(setting.getId())) {
                setting.reset();
            }
        }
        saveSettings();
    }

    private void loadSettings() {
        FuriahRuntime.load(effect, settingsStore.getText());
    }
}
