package mix.client.mixui.integration.haunt.module.landing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.Lifecycle;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.style.Signature;
import mix.client.mixui.integration.haunt.fx.style.StyleMaterials;
import mix.client.mixui.integration.haunt.module.landing.LandingFx;
import mix.client.mixui.integration.haunt.module.landing.LandingImpactModule;
import mix.client.mixui.integration.haunt.module.landing.LandingStyle;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public abstract class LandingStyleBase implements SettingStyle {
   protected static final double TWO_PI = 6.283185307179586D;
   protected final LandingImpactModule module;
   protected final LandingStyle type;
   public final ColorSetting color;
   private final float baseSeconds;
   private final float reach;
   private final List<Setting> settings = new ArrayList();
   private final Signature signature = new Signature();
   private final List<SliderSetting> lookTargets = new ArrayList();

   protected LandingStyleBase(LandingImpactModule module, LandingStyle type, float baseSeconds, float reach, ColorSetting color) {
      this.module = module;
      this.type = type;
      this.baseSeconds = baseSeconds;
      this.reach = reach;
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

   public final LandingStyle getType() {
      return this.type;
   }

   public final Lifecycle lifecycle() {
      return this.type.lifecycle();
   }

   public float baseSeconds() {
      return this.baseSeconds;
   }

   public final float reach() {
      return this.reach;
   }

   public void start(LandingFx fx) {
   }

   public void render(LandingFx fx, FxFrame f) {
   }

   public int passes(LandingFx fx) {
      return 0;
   }

   public void renderPass(LandingFx fx, FxFrame f, FxPass pass) {
   }

   public String stateForTest(LandingFx fx) {
      return null;
   }

   protected static int alpha(int color, float alpha) {
      return ColorUtil.mulAlpha(color, alpha);
   }

   protected static int core(int color, float alpha) {
      return ColorUtil.mulAlpha(ColorUtil.core(color), alpha);
   }

   protected static float loop(float p) {
      p = p - (float)Math.floor((double)p);
      return 1.0F - Math.abs(2.0F * p - 1.0F);
   }
}
