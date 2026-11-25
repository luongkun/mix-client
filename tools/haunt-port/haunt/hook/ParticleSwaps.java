package mix.client.mixui.integration.haunt.hook;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.hook.Bridge;
import mix.client.mixui.integration.haunt.hook.Hooks;
import mix.client.mixui.integration.haunt.hook.Swap;
import mix.client.mixui.integration.haunt.module.kill.KillHooks;
import mix.client.mixui.integration.haunt.module.particles.ParticleHooks;
import mix.client.mixui.integration.haunt.module.particles.ParticleTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityParticleEmitter;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;

public final class ParticleSwaps {
   static final String[] HOOKS = new String[]{"spawnHead", "spawnTail", "emitHead", "drawParticle", "drawStart", "drawEnd", "emitterSpawn", "critPrediction", "sharpPrediction", "critPacket"};
   static final String[] WORLD_HOOKS = new String[]{"bypassSetting", "poof"};
   private static final Field FX_LAYERS = Hooks.field(EffectRenderer.class, new String[]{"fxLayers", "fxLayers"});
   private static final Field RENDERER = Hooks.field(EffectRenderer.class, new String[]{"renderer", "renderer"});
   private static final Field EMITTER_ENTITY = Hooks.field(EntityParticleEmitter.class, new String[]{"attachedEntity", "attachedEntity"});
   private static final Field EMITTER_AGE = Hooks.field(EntityParticleEmitter.class, new String[]{"age", "age"});
   private static final Field EMITTER_LIFETIME = Hooks.field(EntityParticleEmitter.class, new String[]{"lifetime", "lifetime"});
   private static final Field EMITTER_TYPE = Hooks.field(EntityParticleEmitter.class, new String[]{"particleTypes", "particleTypes"});
   private static final Field WORLD_ACCESSES = Hooks.field(World.class, new String[]{"worldAccesses", "worldAccesses"});
   private static final ResourceLocation PARTICLES = new ResourceLocation("textures/particle/particles.png");
   private static final int EXPLOSION_ID = EnumParticleTypes.EXPLOSION_NORMAL.getParticleID();
   private static boolean installed;
   private static boolean attacking;
   private static int depth;
   private static boolean routing;
   private static World listenedWorld;
   private static int savedSetting = -1;
   private static boolean failed;

   static void install() {
      if(FX_LAYERS != null && EMITTER_ENTITY != null && EMITTER_AGE != null && EMITTER_LIFETIME != null && EMITTER_TYPE != null) {
         Minecraft mc = Minecraft.getMinecraft();
         ParticleSwaps.FvEffectRenderer f = (ParticleSwaps.FvEffectRenderer)Swap.upgrade(mc.effectRenderer, EffectRenderer.class, ParticleSwaps.FvEffectRenderer.class);
         if(f != null) {
            mc.effectRenderer = f;
            installed = true;
            Hooks.on(HOOKS);
         }
      } else {
         FuriahsVisuals.LOG.warn("Hooks: EffectRenderer\'s fields were not found; the particle hooks stay off");
      }

   }

   static void uninstall() {
      Minecraft mc = Minecraft.getMinecraft();
      syncWorld(false);
      if(Swap.ours(mc.effectRenderer, ParticleSwaps.FvEffectRenderer.class)) {
         mc.effectRenderer = (EffectRenderer)Swap.downgrade(mc.effectRenderer, EffectRenderer.class);
      }

      installed = false;
      attacking = false;
      Hooks.off(HOOKS);
   }

   static void attack() {
      attacking = true;
   }

   static void tickEnd() {
      attacking = false;
   }

   static void syncWorld(boolean on) {
      Minecraft mc = Minecraft.getMinecraft();
      World w = mc.theWorld;
      if(listenedWorld != null && (!on || listenedWorld != w)) {
         List<IWorldAccess> list = accesses(listenedWorld);
         if(list != null) {
            for(int i = list.size() - 1; i >= 0; --i) {
               if(list.get(i) instanceof ParticleSwaps.Listener) {
                  list.remove(i);
               }
            }
         }

         listenedWorld = null;
         Hooks.off(WORLD_HOOKS);
      }

      if(on && w != null && w != listenedWorld) {
         List<IWorldAccess> list = accesses(w);
         if(list != null) {
            list.add(0, new ParticleSwaps.Listener(true));
            list.add(new ParticleSwaps.Listener(false));
            listenedWorld = w;
            Hooks.on(WORLD_HOOKS);
         }
      }

   }

   private static List<IWorldAccess> accesses(World w) {
      Object o = WORLD_ACCESSES == null?null:Swap.get(WORLD_ACCESSES, w);
      return o instanceof List?(List)o:null;
   }

   private static void fail(Throwable t) {
      if(!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Hooks: a particle hook threw; it is logged once", t);
      }

   }

   private static boolean route(EffectRenderer self, Entity entity, EnumParticleTypes type) {
      if(!routing && (type == EnumParticleTypes.CRIT || type == EnumParticleTypes.CRIT_MAGIC)) {
         if(attacking) {
            return ParticleHooks.prediction(entity);
         } else {
            routing = true;

            try {
               ParticleHooks.critPacket(self, entity, type);
            } finally {
               routing = false;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean poof(double x, double y, double z) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.theWorld == null) {
         return false;
      } else {
         for(EntityPlayer p : mc.theWorld.playerEntities) {
            if(p.deathTime == 20 && p.isDead) {
               double w = (double)p.width;
               if(x >= p.posX - w && x <= p.posX + w && z >= p.posZ - w && z <= p.posZ + w && y >= p.posY && y <= p.posY + (double)p.height) {
                  return KillHooks.skipPoof(p);
               }
            }
         }

         return false;
      }
   }

   // $FF: synthetic method
   static boolean access$1100(double x0, double x1, double x2) {
      return poof(x0, x1, x2);
   }

   public static class FvEffectRenderer extends EffectRenderer {
      public FvEffectRenderer(World world, TextureManager tm) {
         super(world, tm);
      }

      public EntityFX spawnEffectParticle(int particleId, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
         if(ParticleHooks.spawnHead(this, particleId, x, y, z, xSpeed, ySpeed, zSpeed, parameters)) {
            return ParticleHooks.takeSpawnResult();
         } else {
            EntityFX fx = super.spawnEffectParticle(particleId, x, y, z, xSpeed, ySpeed, zSpeed, parameters);
            ParticleHooks.spawnTail(particleId, fx);
            return fx;
         }
      }

      public void emitParticleAtEntity(Entity entity, EnumParticleTypes type) {
         ++ParticleSwaps.depth;

         try {
            if(ParticleSwaps.depth == 1 && ParticleSwaps.route(this, entity, type)) {
               return;
            }

            if(!ParticleHooks.emitHead(this, entity, type)) {
               this.addEffect(new ParticleSwaps.FvParticleEmitter(this.worldObj, entity, type));
               return;
            }
         } finally {
            --ParticleSwaps.depth;
         }

      }

      public void renderParticles(Entity entity, float partialTicks) {
         ParticleHooks.drawStart(partialTicks);
         List<EntityFX>[][] layers = (List[][])((List[][])Swap.get(ParticleSwaps.FX_LAYERS, this));
         List<EntityFX>[][] saved = (List[][])((List[][])null);
         List<EntityFX>[][] pulled = (List[][])((List[][])null);
         boolean var16 = false;
         boolean var17 = false;

         try {
            var17 = true;
            var16 = true;
            if(layers != null) {
               for(int i = 0; i < 3; ++i) {
                  for(int k = 0; k < 2; ++k) {
                     List<EntityFX> list = layers[i][k];
                     List<EntityFX> mine = null;

                     for(int n = 0; n < list.size(); ++n) {
                        EntityFX fx = (EntityFX)list.get(n);
                        if((ParticleTag.tagOf(fx) & 3) != 0) {
                           if(mine == null) {
                              mine = new ArrayList();
                           }

                           mine.add(fx);
                        }
                     }

                     if(mine != null) {
                        if(saved == null) {
                           saved = new List[3][2];
                           pulled = new List[3][2];
                        }

                        saved[i][k] = new ArrayList(list);
                        pulled[i][k] = mine;
                        list.removeAll(mine);
                     }
                  }
               }
            }

            super.renderParticles(entity, partialTicks);
            if(pulled != null) {
               this.drawPulled(pulled, entity, partialTicks);
               var16 = false;
               var17 = false;
            } else {
               var16 = false;
               var17 = false;
            }
         } finally {
            if(var17) {
               if(var16) {
                  if(saved != null) {
                     for(int i = 0; i < 3; ++i) {
                        for(int k = 0; k < 2; ++k) {
                           if(saved[i][k] != null) {
                              layers[i][k].clear();
                              layers[i][k].addAll(saved[i][k]);
                           }
                        }
                     }
                  }

                  ParticleHooks.drawEnd();
               }

            }
         }

         if(var16) {
            if(saved != null) {
               for(int i = 0; i < 3; ++i) {
                  for(int k = 0; k < 2; ++k) {
                     if(saved[i][k] != null) {
                        layers[i][k].clear();
                        layers[i][k].addAll(saved[i][k]);
                     }
                  }
               }
            }

            ParticleHooks.drawEnd();
         }

         if(saved != null) {
            for(int i = 0; i < 3; ++i) {
               for(int kx = 0; kx < 2; ++kx) {
                  if(saved[i][kx] != null) {
                     layers[i][kx].clear();
                     layers[i][kx].addAll(saved[i][kx]);
                  }
               }
            }
         }

         ParticleHooks.drawEnd();
      }

      private void drawPulled(List<EntityFX>[][] pulled, Entity entity, float partialTicks) {
         float rx = ActiveRenderInfo.getRotationX();
         float rz = ActiveRenderInfo.getRotationZ();
         float ryz = ActiveRenderInfo.getRotationYZ();
         float rxy = ActiveRenderInfo.getRotationXY();
         float rxz = ActiveRenderInfo.getRotationXZ();
         TextureManager tm = (TextureManager)Swap.get(ParticleSwaps.RENDERER, this);
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(770, 771);
         GlStateManager.alphaFunc(516, 0.003921569F);

         for(int i = 0; i < 3; ++i) {
            for(int k = 0; k < 2; ++k) {
               List<EntityFX> mine = pulled[i][k];
               if(mine != null) {
                  GlStateManager.depthMask(k == 1);
                  if(tm != null) {
                     tm.bindTexture(i == 1?TextureMap.locationBlocksTexture:ParticleSwaps.PARTICLES);
                  }

                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                  Tessellator tessellator = Tessellator.getInstance();
                  WorldRenderer wr = tessellator.getWorldRenderer();
                  wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);

                  for(EntityFX fx : mine) {
                     ParticleHooks.drawParticle(fx, wr, entity, partialTicks, rx, rxz, rz, ryz, rxy);
                  }

                  tessellator.draw();
               }
            }
         }

         GlStateManager.depthMask(true);
         GlStateManager.disableBlend();
         GlStateManager.alphaFunc(516, 0.1F);
      }
   }

   public static class FvParticleEmitter extends EntityParticleEmitter {
      public FvParticleEmitter(World world, Entity entity, EnumParticleTypes type) {
         super(world, entity, type);
      }

      public void onUpdate() {
         Entity e = (Entity)Swap.get(ParticleSwaps.EMITTER_ENTITY, this);
         EnumParticleTypes type = (EnumParticleTypes)Swap.get(ParticleSwaps.EMITTER_TYPE, this);

         for(int i = 0; i < 16; ++i) {
            double d0 = (double)(this.rand.nextFloat() * 2.0F - 1.0F);
            double d1 = (double)(this.rand.nextFloat() * 2.0F - 1.0F);
            double d2 = (double)(this.rand.nextFloat() * 2.0F - 1.0F);
            if(d0 * d0 + d1 * d1 + d2 * d2 <= 1.0D) {
               double x = e.posX + d0 * (double)e.width / 4.0D;
               double y = e.getEntityBoundingBox().minY + (double)(e.height / 2.0F) + d1 * (double)e.height / 4.0D;
               double z = e.posZ + d2 * (double)e.width / 4.0D;
               ParticleHooks.emitterSpawn(this, this.worldObj, type, false, x, y, z, d0, d1 + 0.2D, d2, new int[0]);
            }
         }

         int age = Swap.getInt(ParticleSwaps.EMITTER_AGE, this) + 1;
         Swap.setInt(ParticleSwaps.EMITTER_AGE, this, age);
         if(age >= Swap.getInt(ParticleSwaps.EMITTER_LIFETIME, this)) {
            this.setDead();
         }

      }
   }

   static final class Listener implements IWorldAccess {
      private final boolean first;

      Listener(boolean first) {
         this.first = first;
      }

      public void spawnParticle(int particleID, boolean ignoreRange, double x, double y, double z, double xOffset, double yOffset, double zOffset, int... parameters) {
         Minecraft mc = Minecraft.getMinecraft();
         if(!this.first) {
            if(ParticleSwaps.savedSetting >= 0) {
               mc.gameSettings.particleSetting = ParticleSwaps.savedSetting;
               ParticleSwaps.savedSetting = -1;
            }
         } else if(Bridge.active()) {
            try {
               boolean handled = particleID == ParticleSwaps.EXPLOSION_ID && ParticleSwaps.access$1100(x, y, z);
               if(!handled && ParticleHooks.bypassSetting(particleID, ignoreRange, x, y, z, xOffset, yOffset, zOffset, parameters)) {
                  ParticleHooks.takeSpawnResult();
                  handled = true;
               }

               if(handled && ParticleSwaps.savedSetting < 0) {
                  ParticleSwaps.savedSetting = mc.gameSettings.particleSetting;
                  mc.gameSettings.particleSetting = 2;
               }
            } catch (Throwable var18) {
               ParticleSwaps.fail(var18);
            }
         }

      }

      public void markBlockForUpdate(BlockPos pos) {
      }

      public void notifyLightSet(BlockPos pos) {
      }

      public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
      }

      public void playSound(String soundName, double x, double y, double z, float volume, float pitch) {
      }

      public void playSoundToNearExcept(EntityPlayer except, String soundName, double x, double y, double z, float volume, float pitch) {
      }

      public void onEntityAdded(Entity entityIn) {
      }

      public void onEntityRemoved(Entity entityIn) {
      }

      public void playRecord(String recordName, BlockPos blockPosIn) {
      }

      public void broadcastSound(int soundID, BlockPos pos, int data) {
      }

      public void playAuxSFX(EntityPlayer player, int sfxType, BlockPos blockPosIn, int data) {
      }

      public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
      }
   }
}
