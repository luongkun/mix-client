package mix.client.mixui.integration.haunt.module.targetaura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mix.client.mixui.integration.haunt.fx.pass.FxPass;
import mix.client.mixui.integration.haunt.fx.style.Signature;
import mix.client.mixui.integration.haunt.fx.style.StyleMaterials;
import mix.client.mixui.integration.haunt.module.targetaura.EffectContext;
import mix.client.mixui.integration.haunt.module.targetaura.EffectType;
import mix.client.mixui.integration.haunt.module.targetaura.TargetAuraModule;
import mix.client.mixui.integration.haunt.render.ColorUtil;
import mix.client.mixui.integration.haunt.render.material.Finish;
import mix.client.mixui.integration.haunt.render.material.Material;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingStyle;
import mix.client.mixui.integration.haunt.setting.SliderSetting;

public abstract class AuraEffect implements SettingStyle {
   protected static final double TWO_PI = 6.283185307179586D;
   protected final TargetAuraModule module;
   protected final EffectType type;
   public final SliderSetting size;
   public final SliderSetting speed;
   public final SliderSetting intensity;
   public final ColorSetting color;
   private final List<Setting> settings;
   private final Signature signature;
   private final List<SliderSetting> lookTargets;
   private final boolean percent;

   protected AuraEffect(TargetAuraModule module, EffectType type, String key, ColorSetting color) {
      this(module, type, key, color, false);
   }

   protected AuraEffect(TargetAuraModule module, EffectType type, String key, ColorSetting color, boolean percent) {
      this.settings = new ArrayList();
      this.signature = new Signature();
      this.lookTargets = new ArrayList();
      this.module = module;
      this.type = type;
      this.percent = percent;
      if(percent) {
         this.size = (SliderSetting)this.setting((new SliderSetting(key + ".size", "Size", "Scales the whole effect.", 50.0D, 150.0D, 100.0D, 5.0D)).unit("%"));
         this.speed = (SliderSetting)this.setting((new SliderSetting(key + ".speed", "Speed", "How fast it moves.", 50.0D, 200.0D, 100.0D, 5.0D)).unit("%"));
         this.intensity = (SliderSetting)this.setting((new SliderSetting(key + ".intensity", "Intensity", "Brightness and opacity.", 10.0D, 100.0D, 100.0D, 5.0D)).unit("%"));
      } else {
         this.size = (SliderSetting)this.setting((new SliderSetting(key + ".size", "Size", "Scales the whole effect.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x"));
         this.speed = (SliderSetting)this.setting((new SliderSetting(key + ".speed", "Speed", "How fast the effect animates.", 0.25D, 3.0D, 1.0D, 0.05D)).unit("x"));
         this.intensity = (SliderSetting)this.setting(new SliderSetting(key + ".intensity", "Intensity", "Brightness and opacity.", 0.1D, 1.0D, 0.85D, 0.05D));
      }

      this.color = color;
      color.group(type.displayName()).ownedBy(this);
      this.signature(color, color.getDefaultColorA());
      this.lookTargets.add(this.size);
      this.lookTargets.add(this.intensity);
      this.lookTargets.add(this.speed);
   }

   protected final <S extends SliderSetting> S look(S slider) {
      this.lookTargets.add(slider);
      return (S)slider;
   }

   public final List<SliderSetting> lookTargets() {
      return Collections.unmodifiableList(this.lookTargets);
   }

   protected final void signature(ColorSetting slot, int signatureColor) {
      this.signature.add(slot, signatureColor);
   }

   public final Signature signature() {
      return this.signature;
   }

   public final void useSignatureColors() {
      this.signature.use();
   }

   public final boolean hasSignatureColors() {
      return this.signature.inUse();
   }

   public final int[] signatureColors() {
      return this.signature.slotColors();
   }

   protected final ModeSetting<Material> materialSetting(String key, Material defaultMaterial, String description) {
      ModeSetting<Material> m = (ModeSetting)this.setting(StyleMaterials.material(key, defaultMaterial, description));
      this.signature.material(m, defaultMaterial);
      return m;
   }

   protected final ModeSetting<Finish> finishSetting(String key, Finish defaultFinish, ModeSetting<Material> material) {
      return (ModeSetting)this.setting(StyleMaterials.finish(key, defaultFinish, material));
   }

   protected final <S extends Setting> S setting(S setting) {
      setting.group(this.type.displayName());
      setting.ownedBy(this);
      this.settings.add(setting);
      return (S)setting;
   }

   public final String styleName() {
      return this.type.displayName();
   }

   public final boolean isShown() {
      return this.module.effect.get() == this.type;
   }

   public final boolean isActive() {
      return this.isShown();
   }

   public final boolean isMultiple() {
      return false;
   }

   public final void show() {
      this.module.effect.set(this.type);
   }

   public final List<Setting> getSettings() {
      ArrayList<Setting> all = new ArrayList(this.settings);
      all.add(this.color);
      return Collections.unmodifiableList(all);
   }

   public final EffectType getType() {
      return this.type;
   }

   public void render(EffectContext c, FxPass pass) {
      if(pass == FxPass.GLOW) {
         this.render(c);
      }

   }

   public void render(EffectContext c) {
   }

   protected final float size(EffectContext c) {
      return this.percent?this.size.getFloat() / 100.0F:this.size.getFloat() * c.scale;
   }

   protected final float sizeNoShrink() {
      return this.percent?this.size.getFloat() / 100.0F:this.size.getFloat();
   }

   protected final float speed() {
      return this.percent?this.speed.getFloat() / 100.0F:this.speed.getFloat();
   }

   protected final float intensity(EffectContext c) {
      return (this.percent?this.intensity.getFloat() / 100.0F:this.intensity.getFloat()) * c.alpha;
   }

   protected final int color(EffectContext c, float position) {
      return this.color.getColor(position);
   }

   protected static float loop(float p) {
      p = p - (float)Math.floor((double)p);
      return 1.0F - Math.abs(2.0F * p - 1.0F);
   }

   protected static int alpha(int color, float alpha) {
      return ColorUtil.mulAlpha(color, alpha);
   }

   protected static int core(int color, float alpha) {
      return ColorUtil.mulAlpha(ColorUtil.core(color), alpha);
   }

   protected static float hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h = h ^ h >>> 15;
      h = h * -2048144789;
      h = h ^ h >>> 13;
      h = h * -1028477387;
      h = h ^ h >>> 16;
      return (float)(h >>> 8) / 1.6777216E7F;
   }
}
