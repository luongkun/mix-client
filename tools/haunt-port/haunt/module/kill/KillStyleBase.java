package mix.client.mixui.integration.haunt.module.kill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.style.Signature;
import mix.client.mixui.integration.haunt.fx.style.StyleMaterials;
import mix.client.mixui.integration.haunt.module.kill.KillEffectsModule;
import mix.client.mixui.integration.haunt.module.kill.KillFx;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public abstract class KillStyleBase implements SettingStyle {
   protected static final double TWO_PI = 6.283185307179586D;
   protected final KillEffectsModule module;
   protected final KillStyle type;
   public final ColorSetting color;
   private final List<Setting> settings = new ArrayList();
   private final Signature signature = new Signature();
   private final List<SliderSetting> lookTargets = new ArrayList();

   protected KillStyleBase(KillEffectsModule module, KillStyle type, ColorSetting color) {
      this.module = module;
      this.type = type;
      this.color = color;
      color.group(type.displayName()).ownedBy(this);
   }

   protected final <S extends Setting> S setting(S setting) {
      setting.group(this.type.displayName());
      setting.ownedBy(this);
      this.settings.add(setting);
      return (S)setting;
   }

   public final Signature signature() {
      return this.signature;
   }

   protected final void signature(ColorSetting slot, int color) {
      this.signature.add(slot, color);
   }

   protected final <S extends SliderSetting> S look(S slider) {
      this.lookTargets.add(slider);
      return (S)slider;
   }

   public final List<SliderSetting> lookTargets() {
      return Collections.unmodifiableList(this.lookTargets);
   }

   protected final ModeSetting<Material> materialSetting(String key, Material made, String description) {
      ModeSetting<Material> m = (ModeSetting)this.setting(StyleMaterials.material(key, made, description));
      this.signature.material(m, made);
      return m;
   }

   protected final ModeSetting<Finish> finishSetting(String key, Finish finish, ModeSetting<Material> material) {
      return (ModeSetting)this.setting(StyleMaterials.finish(key, finish, material));
   }

   public final String styleName() {
      return this.type.displayName();
   }

   public final boolean isShown() {
      return this.module.style.get() == this.type;
   }

   public final boolean isActive() {
      return this.isShown();
   }

   public final boolean isMultiple() {
      return false;
   }

   public final void show() {
      this.module.style.set(this.type);
   }

   public final List<Setting> getSettings() {
      ArrayList<Setting> all = new ArrayList();
      all.add(this.color);
      all.addAll(this.settings);
      return Collections.unmodifiableList(all);
   }

   public final KillStyle getType() {
      return this.type;
   }

   public abstract float seconds(KillFx var1);

   public boolean leavesBody() {
      return false;
   }

   public void start(KillFx fx) {
   }

   public void update(KillFx fx, float dt) {
   }

   public void render(KillFx fx, FxFrame f) {
   }

   public int passes(KillFx fx) {
      return 0;
   }

   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
   }

   public boolean focus(KillFx fx, double[] out) {
      return false;
   }

   public String stateForTest(KillFx fx) {
      return null;
   }

   protected static int alpha(int color, float alpha) {
      return ColorUtil.mulAlpha(color, alpha);
   }

   protected static int core(int color, float alpha) {
      return ColorUtil.mulAlpha(ColorUtil.core(color), alpha);
   }
}
