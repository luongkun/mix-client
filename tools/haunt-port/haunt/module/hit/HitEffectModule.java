package mix.client.mixui.integration.haunt.module.hit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import mix.client.mixui.integration.haunt.core.ClientClock;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.core.GameClock;
import mix.client.mixui.integration.haunt.core.HitConfirmer;
import mix.client.mixui.integration.haunt.core.PlayerFilter;
import mix.client.mixui.integration.haunt.core.TestDummy;
import mix.client.mixui.integration.haunt.fx.Anchor;
import mix.client.mixui.integration.haunt.fx.Basis;
import mix.client.mixui.integration.haunt.fx.EffectEngine;
import mix.client.mixui.integration.haunt.fx.FxFrame;
import mix.client.mixui.integration.haunt.fx.FxParticles;
import mix.client.mixui.integration.haunt.fx.Lifecycle;
import mix.client.mixui.integration.haunt.fx.Rng;
import mix.client.mixui.integration.haunt.fx.Terrain;
import mix.client.mixui.integration.haunt.fx.look.WorldLight;
import mix.client.mixui.integration.haunt.fx.pass.FxPasses;
import mix.client.mixui.integration.haunt.fx.style.Signature;
import mix.client.mixui.integration.haunt.gui.widget.SignatureColorsWidget;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.hit.HitContext;
import mix.client.mixui.integration.haunt.module.hit.HitFx;
import mix.client.mixui.integration.haunt.module.hit.HitStyle;
import mix.client.mixui.integration.haunt.module.hit.HitStyleBase;
import mix.client.mixui.integration.haunt.render.Camera;
import mix.client.mixui.integration.haunt.render.Easing;
import mix.client.mixui.integration.haunt.render.PoseSnapshot;
import mix.client.mixui.integration.haunt.render.RenderState;
import mix.client.mixui.integration.haunt.render.Silhouette;
import mix.client.mixui.integration.haunt.render.Textures;
import mix.client.mixui.integration.haunt.render.Vfx;
import mix.client.mixui.integration.haunt.setting.BooleanSetting;
import mix.client.mixui.integration.haunt.setting.ColorSetting;
import mix.client.mixui.integration.haunt.setting.Displayable;
import mix.client.mixui.integration.haunt.setting.ModeSetting;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public final class HitEffectModule extends Module {
   private static final long EVERY_ATTACK_GAP_NANOS = 100000000L;
   private static final long SWIPE_NANOS = 60000000L;
   private static final float TINT_SECONDS = 0.25F;
   private static final double LOD_DISTANCE = 16.0D;
   public final ModeSetting<HitStyle> style = (ModeSetting)this.add((new ModeSetting("style", "Style", "The look of the effect.", HitStyle.class, HitStyle.VORTEX)).style(ModeSetting.Style.CARDS).group("Style"));
   public final ModeSetting<HitEffectModule.Trigger> trigger;
   public final BooleanSetting critBoost;
   public final BooleanSetting ignoreTeammates;
   public final BooleanSetting alsoOnMobs;
   public final BooleanSetting bodyTint;
   public final ColorSetting tintColor;
   public final SliderSetting tintIntensity;
   public final SliderSetting size;
   public final SliderSetting duration;
   public final SliderSetting speed;
   public final SliderSetting opacity;
   public final SliderSetting density;
   public final SliderSetting closeRange;
   public final BooleanSetting follow;
   public final ModeSetting<HitEffectModule.Again> again;
   public final SliderSetting maxPerTarget;
   public final BooleanSetting keepVisible;
   public final SliderSetting inWayOpacity;
   public final WorldLight lighting;
   public static final float DARK_CAP = 0.35F;
   private final HitStyleBase[] styles;
   private final EffectEngine<HitFx> engine = new EffectEngine("Hit Effect");
   private final FxParticles particles = new FxParticles(2048);
   private final Camera camera = new Camera();
   private final FxFrame frame = new FxFrame();
   private final Basis scratchBasis = new Basis();
   private final Rng rng = new Rng();
   private int seedCounter = 1;
   private int spawnCount;
   private long lastFrameNanos;
   private float renderMillis;
   private final Map<HitConfirmer.Attack, HitContext> waiting = new IdentityHashMap();
   private final Map<Integer, Long> lastSpawn = new HashMap();
   private static final int SWIPE_SAMPLES = 32;
   private final long[] lookTime = new long[32];
   private final float[] lookYaw = new float[32];
   private final float[] lookPitch = new float[32];
   private int lookHead;
   private final List<HitEffectModule.Tint> tints = new ArrayList();
   private final PoseSnapshot tintPose = new PoseSnapshot();
   private final Silhouette.Look tintLook = new Silhouette.Look();
   private long testAt;
   private final EffectEngine<HitFx> previewEngine = new EffectEngine("Hit Effect preview");
   private final FxParticles previewParticles = new FxParticles(768, true);
   private final FxFrame previewFrame = new FxFrame();
   private float previewClock;
   private float nextPreviewHit;
   private int previewIndex;
   private float previewTint = -1.0F;
   private final Module.StartingLooks looks = styleLooks(new Supplier<List<SliderSetting>>() {
      public List<SliderSetting> get() {
         return HitEffectModule.this.getSelectedStyle().lookTargets();
      }
   });
   private float frameDt;
   private long frameNow;
   private final FxPasses.Clock passClock = new FxPasses.Clock();
   private static final double[][] PREVIEW_EYES = new double[][]{{0.0D, 1.62D, 3.0D}, {-2.5D, 1.6D, 1.8D}, {2.5D, 1.6D, 1.8D}, {0.25D, 3.3D, 2.1D}, {-0.5D, 0.35D, 2.6D}, {1.8D, 2.3D, 2.3D}};
   private static final double[][] PREVIEW_SWIPES = new double[][]{{1.0D, 0.25D}, {-1.0D, 0.35D}, {0.8D, -0.6D}, {0.2D, -1.0D}, {-0.6D, 0.8D}, {1.0D, -0.4D}};

   public HitEffectModule() {
      super("hit_effect", "Hit Effect", "An effect on the players you hit, shaped by where and how you hit them.", true);
      HitStyle[] types = HitStyle.values();
      this.styles = new HitStyleBase[types.length];

      for(HitStyle type : types) {
         HitStyleBase s;
         this.styles[type.ordinal()] = s = type.create(this);

         for(Setting setting : s.getSettings()) {
            this.add(setting);
         }
      }

      BooleanSupplier fade = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return ((HitStyle)HitEffectModule.this.style.get()).lifecycle() == Lifecycle.FADE;
         }
      };
      BooleanSupplier animation = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return ((HitStyle)HitEffectModule.this.style.get()).lifecycle() == Lifecycle.ANIMATION;
         }
      };
      BooleanSupplier tintOn = new BooleanSupplier() {
         public boolean getAsBoolean() {
            return HitEffectModule.this.bodyTint.get();
         }
      };
      this.trigger = (ModeSetting)this.add((new ModeSetting("trigger", "Trigger", "Confirmed shows exactly one effect per real hit; Every Attack reacts to each click.", HitEffectModule.Trigger.class, HitEffectModule.Trigger.CONFIRMED)).group("Trigger"));
      this.critBoost = (BooleanSetting)this.add((new BooleanSetting("critBoost", "Critical Boost", "Critical hits (while falling) get a bigger, brighter version.", true)).group("Trigger"));
      this.ignoreTeammates = (BooleanSetting)this.add((new BooleanSetting("ignoreTeammates", "Ignore Teammates", "No effect on players of your team or name color.", false)).group("Trigger"));
      this.alsoOnMobs = (BooleanSetting)this.add((new BooleanSetting("alsoOnMobs", "Also On Mobs", "Mobs and armor stands too (for testing).", false)).group("Trigger"));
      this.bodyTint = (BooleanSetting)this.add((new BooleanSetting("bodyTint", "Body Tint", "Flashes the target\'s whole body with a color for a moment. Works with every style.", false)).group("Body Tint"));
      this.tintColor = (ColorSetting)this.add(((ColorSetting)(new ColorSetting("tintColor", "Tint Color", "Color of the flash.", -50066)).group("Body Tint")).visibleWhen(tintOn));
      this.tintIntensity = (SliderSetting)this.add(((SliderSetting)(new SliderSetting("tintIntensity", "Tint Intensity", "How strong the flash is.", 0.1D, 1.0D, 0.6D, 0.05D)).group("Body Tint")).visibleWhen(tintOn));
      this.size = (SliderSetting)this.add((new SliderSetting("size", "Size", "Scales the whole effect.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x").group("Look"));
      this.duration = (SliderSetting)this.add(((SliderSetting)(new SliderSetting("duration", "Duration", "How long the effect lasts before it fades.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x").group("Look")).visibleWhen(fade));
      this.speed = (SliderSetting)this.add(((SliderSetting)(new SliderSetting("speed", "Speed", "How fast the animation plays. It always plays to the end.", 0.5D, 2.0D, 1.0D, 0.05D)).unit("x").group("Look")).visibleWhen(animation));
      this.opacity = (SliderSetting)this.add((new SliderSetting("opacity", "Opacity", "How strong the effect is.", 0.1D, 1.0D, 1.0D, 0.05D)).group("Look"));
      this.density = (SliderSetting)this.add((new SliderSetting("density", "Density", "How many particles, pieces and sparks.", 0.25D, 2.0D, 1.0D, 0.05D)).unit("x").group("Look"));
      this.closeRange = (SliderSetting)this.add((new SliderSetting("closeRange", "Close-Range Opacity", "When the target is within 1.5 blocks, effects fade to this, so they never block your view in combos.", 0.1D, 1.0D, 0.6D, 0.05D)).group("Look"));
      this.follow = (BooleanSetting)this.add((new BooleanSetting("follow", "Follow Target", "The effect moves with the target; off, it stays where the hit happened.", true)).group("Behavior"));
      this.again = (ModeSetting)this.add((new ModeSetting("again", "When Hit Again", "A new hit while an animation still plays: another one on top, or none.", HitEffectModule.Again.class, HitEffectModule.Again.STACK)).group("Behavior"));
      this.maxPerTarget = (SliderSetting)this.add((new SliderSetting("maxPerTarget", "Max Per Target", "Effects at once on one target.", 1.0D, 6.0D, 3.0D, 1.0D)).group("Behavior"));
      final BooleanSetting keep = this.keepVisible = (BooleanSetting)this.add(((BooleanSetting)(new BooleanSetting("keepVisible", "Keep the Target Visible", "The effect never hides the player you hit: solid, glass and dark parts fade while they cover their body, and dark smoke stays see-through there. Close-Range Opacity still applies on top.", true)).group("Advanced")).keywords(new String[]{"visible", "hide", "hidden", "see", "fade", "readability", "transparent", "block view"}));
      this.inWayOpacity = (SliderSetting)this.add(((SliderSetting)(new SliderSetting("keepVisible.opacity", "In-the-Way Opacity", "How much of a solid, glass or dark part still shows where it covers the target\'s body.", 20.0D, 100.0D, 50.0D, 5.0D)).unit("%").group("Advanced")).visibleWhen(new BooleanSupplier() {
         public boolean getAsBoolean() {
            return keep.get();
         }
      }));
      this.lighting = new WorldLight("Solid materials, paper and drawn strokes", "Thorn Lash, Ink Slash, Shadow Burst, and Scribble with Pencil");
      this.add(this.lighting.match);
      this.add(this.lighting.minimum);
      this.add(this.lighting.night);
      this.add(SignatureColorsWidget.setting(new SignatureColorsWidget.Source() {
         public Signature selectedSignature() {
            return HitEffectModule.this.getSelectedStyle().signature();
         }

         public String selectedStyleName() {
            return ((HitStyle)HitEffectModule.this.style.get()).displayName();
         }
      }));
      this.style.addListener(new Runnable() {
         public void run() {
            HitEffectModule.this.restartPreview();
         }
      });
      FuriahsVisuals.instance.getHitConfirmer().addListener(new HitConfirmer.Listener() {
         public void onConfirmedHit(HitConfirmer.Attack attack) {
            HitEffectModule.this.confirmed(attack);
         }
      });
   }

   public HitStyleBase getStyle(HitStyle type) {
      return this.styles[type.ordinal()];
   }

   public Module.StartingLooks startingLooks() {
      return this.looks;
   }

   public HitStyleBase getSelectedStyle() {
      return this.styles[((HitStyle)this.style.get()).ordinal()];
   }

   public void onAttack(EntityPlayer attacker, Entity target) {
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP me = mc.thePlayer;
      if(attacker == me && me != null && target != null && target != me && this.isValidTarget(mc, target)) {
         long now = GameClock.nanos();
         this.recordLook(me, now);
         HitContext hit = this.measure(mc, me, target, now);
         int id = target.getEntityId();
         if(this.trigger.get() == HitEffectModule.Trigger.EVERY_ATTACK) {
            Long last = (Long)this.lastSpawn.get(Integer.valueOf(id));
            if(last == null || now - last.longValue() >= 100000000L) {
               this.lastSpawn.put(Integer.valueOf(id), Long.valueOf(now));
               this.spawn(hit, false);
            }
         } else {
            HitConfirmer.Attack attack = FuriahsVisuals.instance.getHitConfirmer().lastAttack();
            if(attack != null && attack.target == target) {
               this.waiting.put(attack, hit);
            }
         }
      }

   }

   private void confirmed(HitConfirmer.Attack attack) {
      if(this.isEnabled() && this.trigger.get() == HitEffectModule.Trigger.CONFIRMED) {
         HitContext hit = (HitContext)this.waiting.remove(attack);
         Iterator<HitConfirmer.Attack> it = this.waiting.keySet().iterator();

         while(it.hasNext()) {
            if(((HitConfirmer.Attack)it.next()).targetId == attack.targetId) {
               it.remove();
            }
         }

         if(hit != null) {
            this.spawn(hit, false);
         }
      }

   }

   private boolean isValidTarget(Minecraft mc, Entity target) {
      if(target instanceof EntityLivingBase && !target.isDead && !target.isInvisible()) {
         if(!(target instanceof EntityPlayer)) {
            return this.alsoOnMobs.get();
         } else {
            EntityPlayer p = (EntityPlayer)target;
            return p.isSpectator()?false:!this.ignoreTeammates.get() || !PlayerFilter.isTeammate(mc, p);
         }
      } else {
         return false;
      }
   }

   private HitContext measure(Minecraft mc, EntityPlayerSP me, Entity target, long now) {
      HitContext hit = new HitContext();
      hit.target = target;
      hit.targetId = target.getEntityId();
      hit.time = now;
      hit.feetX = target.posX;
      hit.feetY = target.posY;
      hit.feetZ = target.posZ;
      hit.width = target.width;
      hit.height = target.height;
      Vec3 eye = me.getPositionEyes(1.0F);
      Vec3 look = me.getLook(1.0F);
      double border = (double)target.getCollisionBorderSize();
      AxisAlignedBB box = target.getEntityBoundingBox().expand(border, border, border);
      Vec3 impact = null;
      MovingObjectPosition over = mc.objectMouseOver;
      if(over != null && over.entityHit == target && over.hitVec != null && !box.isVecInside(eye)) {
         impact = over.hitVec;
      }

      if(impact == null) {
         MovingObjectPosition m = box.calculateIntercept(eye, eye.addVector(look.xCoord * 8.0D, look.yCoord * 8.0D, look.zCoord * 8.0D));
         impact = m != null && m.hitVec != null?m.hitVec:closestPointOnBox(box, eye, look);
      }

      hit.offX = impact.xCoord - hit.feetX;
      hit.offY = impact.yCoord - hit.feetY;
      hit.offZ = impact.zCoord - hit.feetZ;
      this.direction(hit, eye.xCoord, eye.yCoord, eye.zCoord, look.xCoord, look.yCoord, look.zCoord);
      this.swipe(hit, now, me.ticksExisted);
      hit.crit = isCrit(me, target);
      double ground = Terrain.surfaceAt(mc.theWorld, target.posX, target.posZ, target.posY, 0.3D, 6.0D);
      hit.groundY = Double.isNaN(ground)?target.posY - 6.0D:ground;
      return hit;
   }

   private static Vec3 closestPointOnBox(AxisAlignedBB box, Vec3 eye, Vec3 look) {
      double cx = (box.minX + box.maxX) / 2.0D;
      double cy = (box.minY + box.maxY) / 2.0D;
      double cz = (box.minZ + box.maxZ) / 2.0D;
      double t = Math.max(0.0D, (cx - eye.xCoord) * look.xCoord + (cy - eye.yCoord) * look.yCoord + (cz - eye.zCoord) * look.zCoord);
      double px = eye.xCoord + look.xCoord * t;
      double py = eye.yCoord + look.yCoord * t;
      double pz = eye.zCoord + look.zCoord * t;
      return new Vec3(clamp(px, box.minX, box.maxX), clamp(py, box.minY, box.maxY), clamp(pz, box.minZ, box.maxZ));
   }

   private static double clamp(double v, double min, double max) {
      return v < min?min:(v > max?max:v);
   }

   private void direction(HitContext hit, double ex, double ey, double ez, double lookX, double lookY, double lookZ) {
      double dx = hit.impactX() - ex;
      double dz;
      double dy;
      double len = Math.sqrt(dx * dx + (dy = hit.impactY() - ey) * dy + (dz = hit.impactZ() - ez) * dz);
      if(len < 1.0E-4D) {
         dx = lookX;
         dy = lookY;
         dz = lookZ;
         len = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
      }

      hit.dirX = dx / len;
      hit.dirY = dy / len;
      hit.dirZ = dz / len;
      Basis b = this.scratchBasis.set(hit.dirX, hit.dirY, hit.dirZ);
      double across = hit.offX * b.rx + hit.offZ * b.rz;
      hit.side = across >= 0.0D?1:-1;
   }

   private static boolean isCrit(EntityPlayerSP me, Entity target) {
      return me.fallDistance > 0.0F && !me.onGround && !me.isOnLadder() && !me.isInWater() && !me.isPotionActive(Potion.blindness) && me.ridingEntity == null && target instanceof EntityLivingBase;
   }

   private void recordLook(EntityPlayerSP me, long now) {
      if(now - this.lookTime[this.lookHead] < 4000000L) {
         this.lookYaw[this.lookHead] = me.rotationYaw;
         this.lookPitch[this.lookHead] = me.rotationPitch;
      } else {
         this.lookHead = (this.lookHead + 1) % 32;
         this.lookTime[this.lookHead] = now;
         this.lookYaw[this.lookHead] = me.rotationYaw;
         this.lookPitch[this.lookHead] = me.rotationPitch;
      }

   }

   private void swipe(HitContext hit, long now, int tick) {
      float yawNow = this.lookYaw[this.lookHead];
      float pitchNow = this.lookPitch[this.lookHead];
      float dYaw = 0.0F;
      float dPitch = 0.0F;

      int j;
      for(int k = 1; k < 32 && this.lookTime[j = (this.lookHead - k + 32) % 32] != 0L; ++k) {
         dYaw = MathHelper.wrapAngleTo180_float(yawNow - this.lookYaw[j]);
         dPitch = pitchNow - this.lookPitch[j];
         if(now - this.lookTime[j] >= 60000000L) {
            break;
         }
      }

      double sx;
      double len;
      double sy;
      if((len = Math.sqrt((sx = (double)dYaw) * sx + (sy = (double)(-dPitch)) * sy)) < 0.35D) {
         this.rng.seed(tick, hit.targetId);
         sx = this.rng.next() < 0.5F?1.0D:-1.0D;
         sy = this.rng.next() < 0.5F?0.75D:-0.75D;
         len = Math.sqrt(sx * sx + sy * sy);
      }

      double var18;
      hit.screenSwipeX = (float)(var18 = sx / len);
      double var17;
      hit.screenSwipeY = (float)(var17 = sy / len);
      Basis b = this.scratchBasis.set(hit.dirX, hit.dirY, hit.dirZ);
      hit.swipeX = b.rx * var18 + b.ux * var17;
      hit.swipeY = b.ry * var18 + b.uy * var17;
      hit.swipeZ = b.rz * var18 + b.uz * var17;
   }

   public void onTick() {
      Minecraft mc = Minecraft.getMinecraft();
      long now = GameClock.nanos();
      if(mc.thePlayer != null) {
         this.recordLook(mc.thePlayer, now);
      }

      if(this.testAt != 0L && GameClock.millis() >= this.testAt && mc.currentScreen == null) {
         this.testAt = 0L;
         this.playTest(mc);
      }

      if(!this.waiting.isEmpty()) {
         HitConfirmer confirmer = FuriahsVisuals.instance.getHitConfirmer();
         Iterator<HitConfirmer.Attack> it = this.waiting.keySet().iterator();

         while(it.hasNext()) {
            if(!confirmer.isPending((HitConfirmer.Attack)it.next())) {
               it.remove();
            }
         }
      }

      if(this.lastSpawn.size() > 64) {
         this.lastSpawn.clear();
      }

   }

   private void spawn(HitContext hit, boolean preview) {
      EffectEngine<HitFx> target = preview?this.previewEngine:this.engine;
      FxParticles pool = preview?this.previewParticles:this.particles;
      HitStyleBase s = this.getSelectedStyle();
      Lifecycle lifecycle = s.lifecycle();
      if(preview || this.again.get() != HitEffectModule.Again.IGNORE || !target.isRunning(hit.targetId, Lifecycle.ANIMATION)) {
         target.setLimits(32, this.maxPerTarget.getInt());
         float timeScale = lifecycle == Lifecycle.FADE?this.duration.getFloat():1.0F / this.speed.getFloat();
         boolean crit = hit.crit && this.critBoost.get();
         Anchor anchor = !preview && this.follow.get() && hit.target != null?Anchor.entity(hit.target, hit.offX, hit.offY, hit.offZ, true, 1.0F):Anchor.fixed(hit.feetX, hit.feetY, hit.feetZ, hit.offX, hit.offY, hit.offZ);
         if(hit.target instanceof EntityLivingBase) {
            hit.pose.capture((EntityLivingBase)hit.target, 1.0F);
         } else if(preview) {
            hit.pose.copy(PoseSnapshot.PREVIEW_MANNEQUIN);
         }

         int seed = Rng.mix(this.seedCounter++ * -1640531535 ^ (int)System.nanoTime());
         HitFx fx = new HitFx(s, hit, anchor, s.baseSeconds() * timeScale, timeScale, seed, pool, this.size.getFloat(), this.opacity.getFloat(), this.density.getFloat(), crit, preview);
         Entity viewer;
         if(!preview && (viewer = Minecraft.getMinecraft().getRenderViewEntity()) != null && viewer.getDistanceSq(hit.impactX(), hit.impactY(), hit.impactZ()) > 256.0D) {
            fx.lod = true;
         }

         if(target.add(fx)) {
            if(!preview) {
               ++this.spawnCount;
            }

            if(this.bodyTint.get()) {
               if(preview) {
                  this.previewTint = 0.0F;
               } else if(hit.target instanceof EntityPlayer) {
                  this.startTint((EntityLivingBase)hit.target);
               }
            }
         }
      }

   }

   private void startTint(EntityLivingBase e) {
      long now = GameClock.nanos();

      for(int i = 0; i < this.tints.size(); ++i) {
         if(((HitEffectModule.Tint)this.tints.get(i)).entity == e) {
            ((HitEffectModule.Tint)this.tints.get(i)).start = now;
            return;
         }
      }

      this.tints.add(new HitEffectModule.Tint(e, now));
   }

   public void onPrepareWorld(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      long now = GameClock.nanos();
      float dt = this.lastFrameNanos == 0L?0.0F:Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = now;
      this.frameDt = dt;
      this.frameNow = now;
      if(mc.thePlayer != null) {
         this.recordLook(mc.thePlayer, now);
      }

      this.engine.update(dt, partialTicks, mc.theWorld);
      this.particles.update(dt);

      for(int i = this.tints.size() - 1; i >= 0; --i) {
         HitEffectModule.Tint t = (HitEffectModule.Tint)this.tints.get(i);
         if((float)(now - t.start) / 1.0E9F > 0.25F || t.entity.isDead) {
            this.tints.remove(i);
         }
      }

      boolean passes = false;

      for(int ix = 0; ix < this.engine.size() && !passes; ++ix) {
         passes = ((HitFx)this.engine.list().get(ix)).passes() != 0;
      }

      if(passes) {
         long start = System.nanoTime();
         this.camera.captureWorld();
         this.fillFrame(mc, partialTicks, dt, now);
         this.fades();
         this.lighting.frame(mc.theWorld, partialTicks);
         float inWay = this.inWayOpacity.getFloat() / 100.0F;

         for(int ix = 0; ix < this.engine.size(); ++ix) {
            HitFx fx = (HitFx)this.engine.list().get(ix);
            if(fx.passes() != 0) {
               fx.passFrame = this.frame;
               float level = this.lighting.level(mc.theWorld, fx.cx(), fx.cy(), fx.cz());
               fx.light = this.lighting.light(level);
               fx.night = this.lighting.night(level);
               fx.keep.set(this.camera, fx.anchor.feetX() - this.frame.viewX, fx.anchor.feetY() - this.frame.viewY, fx.anchor.feetZ() - this.frame.viewZ, fx.hit.width, fx.hit.height, this.keepVisible.get(), inWay, 0.35F);
               FxPasses.submit(fx, this.passClock);
            }
         }

         this.passClock.nanos += System.nanoTime() - start;
      }

   }

   private void fades() {
      for(int i = 0; i < this.engine.size(); ++i) {
         HitFx fx = (HitFx)this.engine.list().get(i);
         float d = (float)Math.sqrt(this.frame.distanceSqToEye(fx.cx(), fx.cy(), fx.cz()));
         fx.fade = Easing.lerp(this.closeRange.getFloat(), 1.0F, Easing.smoothstep(1.5F, 2.3F, d));
      }

   }

   public void onRenderWorld(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      long now = this.frameNow;
      float dt = this.frameDt;
      float passMillis = this.passClock.takeMillis();
      if(!this.engine.isEmpty() || !this.particles.isEmpty() || !this.tints.isEmpty()) {
         long start = System.nanoTime();
         Textures.ensureLoaded();
         RenderState.begin();

         try {
            this.camera.captureModelView();
            Vfx.setCamera(this.camera);
            this.fillFrame(mc, partialTicks, dt, now);
            this.fades();

            for(int i = 0; i < this.tints.size(); ++i) {
               HitEffectModule.Tint t = (HitEffectModule.Tint)this.tints.get(i);
               if(!t.entity.isInvisible() && (!(t.entity instanceof EntityPlayer) || !FuriahsVisuals.instance.getModules().hidesPlayer((EntityPlayer)t.entity))) {
                  this.drawTint(this.tintPose.capture(t.entity, partialTicks), (float)(now - t.start) / 1.0E9F, this.frame.viewX, this.frame.viewY, this.frame.viewZ);
               }
            }

            this.engine.render(this.frame);
            this.particles.render(this.frame.viewX, this.frame.viewY, this.frame.viewZ);
         } finally {
            RenderState.end();
         }

         float var14 = (float)(System.nanoTime() - start) / 1000000.0F + passMillis;
         this.renderMillis = this.renderMillis == 0.0F?var14:this.renderMillis + (var14 - this.renderMillis) * 0.05F;
      }

   }

   private void fillFrame(Minecraft mc, float pt, float dt, long now) {
      FxFrame f = this.frame;
      f.cam = this.camera;
      f.viewX = mc.getRenderManager().viewerPosX;
      f.viewY = mc.getRenderManager().viewerPosY;
      f.viewZ = mc.getRenderManager().viewerPosZ;
      Entity v = (Entity)(mc.getRenderViewEntity() != null?mc.getRenderViewEntity():mc.thePlayer);
      if(v != null) {
         f.eyeX = v.lastTickPosX + (v.posX - v.lastTickPosX) * (double)pt;
         f.eyeY = v.lastTickPosY + (v.posY - v.lastTickPosY) * (double)pt + (double)v.getEyeHeight();
         f.eyeZ = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * (double)pt;
      }

      f.partialTicks = pt;
      f.dt = dt;
      f.now = now;
      f.frameId = FxFrame.currentFrame();
      f.preview = false;
      f.world = mc.theWorld;
      f.firstPerson = mc.gameSettings.thirdPersonView == 0;
   }

   private void drawTint(PoseSnapshot pose, float t, double offX, double offY, double offZ) {
      float k = t < 0.04F?t / 0.04F:(float)Math.pow((double)Math.max(0.0F, 1.0F - (t - 0.04F) / 0.21000001F), 1.6D);
      float strength = this.tintIntensity.getFloat() * k;
      if(strength > 0.003F) {
         this.tintLook.reset();
         this.tintLook.color = this.tintColor;
         this.tintLook.fill = 0.85F * strength;
         this.tintLook.rim = 0.7F * strength;
         this.tintLook.inflate = 1.1F;
         this.tintLook.rimWidth = 0.6F;
         Silhouette.draw(pose, offX, offY, offZ, this.tintLook);
      }

   }

   public void onWorldChange() {
      this.engine.clear();
      this.particles.clear();
      this.waiting.clear();
      this.lastSpawn.clear();
      this.tints.clear();
      this.lastFrameNanos = 0L;
   }

   protected void onDisable() {
      this.onWorldChange();
   }

   public float getRenderMillis() {
      return this.renderMillis;
   }

   public int getActiveEffectCount() {
      return this.engine.size();
   }

   public HitFx newestForTest() {
      return this.engine.isEmpty()?null:(HitFx)this.engine.list().get(this.engine.size() - 1);
   }

   public int runningForTest() {
      return this.engine.size();
   }

   public int cutShort(int targetId) {
      int n = 0;

      for(int i = 0; i < this.engine.size(); ++i) {
         HitFx fx = (HitFx)this.engine.list().get(i);
         if(fx.entityId == targetId && fx.cut < 0.0F && fx.age < 0.6F) {
            fx.cut = 0.0F;
            ++n;
         }
      }

      for(int var5 = this.tints.size() - 1; var5 >= 0; --var5) {
         if(((HitEffectModule.Tint)this.tints.get(var5)).entity.getEntityId() == targetId) {
            this.tints.remove(var5);
         }
      }

      return n;
   }

   public boolean hasTestAction() {
      return true;
   }

   public String getTestTooltip() {
      return "Closes the menu, then plays the style on the player you look at, or on your test dummy";
   }

   public void runTest() {
      this.testAt = GameClock.millis() + 250L;
   }

   public void playTest(Minecraft mc) {
      this.playTest(mc, false);
   }

   public void playTest(Minecraft mc, boolean forceCrit) {
      if(mc.thePlayer != null && mc.theWorld != null) {
         EntityLivingBase target = null;
         MovingObjectPosition over = mc.objectMouseOver;
         if(over != null && over.entityHit instanceof EntityLivingBase && !over.entityHit.isInvisible()) {
            target = (EntityLivingBase)over.entityHit;
         }

         if(target == null) {
            target = TestDummy.get();
         }

         if(target == null) {
            FuriahsVisuals.chat("Look at a player, or spawn a test dummy with /haunt dummy.");
         } else {
            long now = GameClock.nanos();
            this.recordLook(mc.thePlayer, now);
            HitContext hit = this.measure(mc, mc.thePlayer, target, now);
            hit.crit |= forceCrit;
            this.spawn(hit, false);
         }
      }

   }

   public int getSpawnCount() {
      return this.spawnCount;
   }

   public int getAccentColor(double time) {
      return this.getSelectedStyle().color.getDisplayColor();
   }

   public int getOptionColor(Setting setting, Enum<?> option, double time) {
      return setting == this.style && option instanceof HitStyle?this.getStyle((HitStyle)option).color.getDisplayColor():0;
   }

   public boolean hasPreview() {
      return true;
   }

   public String getPreviewLabel() {
      return ((HitStyle)this.style.get()).displayName();
   }

   public String getPreviewInfo() {
      return this.renderMillis <= 0.0F?"Hits come from changing sides":String.format(Locale.ROOT, "%.2f ms / frame", new Object[]{Float.valueOf(this.renderMillis)});
   }

   public void restartPreview() {
      this.previewEngine.clear();
      this.previewParticles.clear();
      this.previewClock = 0.0F;
      this.nextPreviewHit = 0.25F;
      this.previewTint = -1.0F;
   }

   private void spawnPreviewHit() {
      int k = this.previewIndex++ % PREVIEW_EYES.length;
      double ex = PREVIEW_EYES[k][0];
      double ey = PREVIEW_EYES[k][1];
      double ez = PREVIEW_EYES[k][2];
      this.rng.seed(this.previewIndex, 77);
      double ax = (double)this.rng.signed() * 0.12D;
      double ay = 1.2D + (double)this.rng.signed() * 0.2D;
      AxisAlignedBB box = new AxisAlignedBB(-0.4D, -0.1D, -0.4D, 0.4D, 1.9D, 0.4D);
      Vec3 eye = new Vec3(ex, ey, ez);
      double lx = ax - ex;
      double ly = ay - ey;
      double lz = -ez;
      double ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
      Vec3 look = new Vec3(lx / ll, ly / ll, lz / ll);
      MovingObjectPosition m = box.calculateIntercept(eye, eye.addVector(look.xCoord * 8.0D, look.yCoord * 8.0D, look.zCoord * 8.0D));
      Vec3 impact = m != null && m.hitVec != null?m.hitVec:closestPointOnBox(box, eye, look);
      HitContext hit = new HitContext();
      hit.targetId = -2;
      hit.time = ClientClock.nanos();
      hit.offX = impact.xCoord;
      hit.offY = impact.yCoord;
      hit.offZ = impact.zCoord;
      this.direction(hit, ex, ey, ez, look.xCoord, look.yCoord, look.zCoord);
      Basis b = this.scratchBasis.set(hit.dirX, hit.dirY, hit.dirZ);
      double sx = PREVIEW_SWIPES[k][0];
      double sy = PREVIEW_SWIPES[k][1];
      double sl = Math.sqrt(sx * sx + sy * sy);
      hit.swipeX = (b.rx * sx + b.ux * sy) / sl;
      hit.swipeY = (b.ry * sx + b.uy * sy) / sl;
      hit.swipeZ = (b.rz * sx + b.uz * sy) / sl;
      hit.crit = k == 3;
      hit.groundY = 0.0D;
      this.spawn(hit, true);
   }

   public void renderPreview(float dt) {
      this.previewClock += dt;
      if(this.previewClock >= this.nextPreviewHit) {
         this.spawnPreviewHit();
         HitStyleBase s = this.getSelectedStyle();
         float scale = s.lifecycle() == Lifecycle.FADE?this.duration.getFloat():1.0F / this.speed.getFloat();
         this.nextPreviewHit = this.previewClock + Math.max(1.0F, s.baseSeconds() * scale + 0.35F);
      }

      this.previewEngine.update(dt, 1.0F, (World)null);
      this.previewParticles.update(dt);
      Textures.ensureLoaded();
      RenderState.begin();

      try {
         this.camera.captureModelView();
         Vfx.setCamera(this.camera);
         FxFrame f = this.previewFrame;
         f.cam = this.camera;
         f.viewZ = 0.0D;
         f.viewY = 0.0D;
         f.viewX = 0.0D;
         f.eyeX = this.camera.posX;
         f.eyeY = this.camera.posY;
         f.eyeZ = this.camera.posZ;
         f.partialTicks = 1.0F;
         f.dt = dt;
         f.now = ClientClock.nanos();
         f.frameId = FxFrame.currentFrame();
         f.preview = true;
         f.world = null;
         f.firstPerson = false;
         if(this.previewTint >= 0.0F) {
            this.previewTint += dt;
            if(this.previewTint > 0.25F) {
               this.previewTint = -1.0F;
            } else {
               this.drawTint(PoseSnapshot.PREVIEW_MANNEQUIN, this.previewTint, 0.0D, 0.0D, 0.0D);
            }
         }

         this.previewEngine.render(f);

         for(int i = 0; i < this.previewEngine.size(); ++i) {
            HitFx fx = (HitFx)this.previewEngine.list().get(i);
            fx.passFrame = f;
            fx.light = 1.0F;
            fx.night = this.lighting.previewNight();
            fx.keep.set(this.camera, 0.0D, 0.0D, 0.0D, fx.hit.width, fx.hit.height, this.keepVisible.get(), this.inWayOpacity.getFloat() / 100.0F, 0.35F);
         }

         FxPasses.drawNow(this.previewEngine.list());
         this.previewParticles.render(0.0D, 0.0D, 0.0D);
      } finally {
         RenderState.end();
      }

   }

   public static enum Again implements Displayable {
      STACK("Stack", "Play another one on top, up to the limit"),
      IGNORE("Ignore", "Skip hits until the animation has finished");

      private final String displayName;
      private final String description;

      private Again(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }

   private static final class Tint {
      final EntityLivingBase entity;
      long start;

      Tint(EntityLivingBase entity, long start) {
         this.entity = entity;
         this.start = start;
      }
   }

   public static enum Trigger implements Displayable {
      CONFIRMED("Confirmed Hit", "When the server confirms the damage"),
      EVERY_ATTACK("Every Attack", "On every attack that connects");

      private final String displayName;
      private final String description;

      private Trigger(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      public String displayName() {
         return this.displayName;
      }

      public String description() {
         return this.description;
      }
   }
}
