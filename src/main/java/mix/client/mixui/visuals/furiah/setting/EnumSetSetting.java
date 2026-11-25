package mix.client.mixui.visuals.furiah.setting;

import com.google.gson.JsonElement;
import mix.client.mixui.visuals.furiah.gui.widget.Widget;

public class EnumSetSetting<E extends Enum<E>> extends StringListSetting implements HasWidget, EnumChoices {
    private final E[] values;
    private final boolean atLeastOne;
    private int cachedVersion = -1;
    private final boolean[] cached;

    @SafeVarargs
    public EnumSetSetting(String id, String name, String description, Class<E> type, boolean atLeastOne, E... defaults) {
        super(id, name, description, names(defaults));
        this.values = type.getEnumConstants();
        this.atLeastOne = atLeastOne;
        this.cached = new boolean[this.values.length];
    }

    private static String[] names(Enum<?>[] defaults) {
        String[] out = new String[defaults.length];
        for (int i = 0; i < defaults.length; i++) {
            out[i] = defaults[i].name();
        }
        return out;
    }

    public E[] values() {
        return values;
    }

    @Override
    public Enum<?>[] choices() {
        return values;
    }

    public boolean includes(E value) {
        if (cachedVersion != version()) {
            cachedVersion = version();
            for (int i = 0; i < values.length; i++) {
                cached[i] = contains(values[i].name());
            }
        }
        return cached[value.ordinal()];
    }

    public int count() {
        int n = 0;
        for (E value : values) {
            if (includes(value)) {
                n++;
            }
        }
        return n;
    }

    public boolean toggle(E value) {
        if (includes(value)) {
            if (atLeastOne && count() <= 1) {
                return false;
            }
            remove(value.name());
        } else {
            add(value.name());
        }
        return true;
    }

    @Override
    public boolean showInMenu() {
        return true;
    }

    @Override
    public Widget createWidget() {
        return new Widget();
    }
}
